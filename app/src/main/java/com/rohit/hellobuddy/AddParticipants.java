package com.rohit.hellobuddy;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.rohit.hellobuddy.model.User;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class AddParticipants extends AppCompatActivity {

    Toolbar toolbar;
    EditText editTextSearch;
    RecyclerView recyclerView;
    FloatingActionButton fab_create_group;

    DatabaseReference buddyRef, userRef, rootRef;
    String currentUserID;

    HashMap<String,String>hashMapContacts=new HashMap<>();

    ProgressDialog loadingBar;

    Intent intent;
    String groupIDByIntent,statusByIntent,groupNameByIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_participants);

        toolbar = findViewById(R.id.toolbar);
        editTextSearch = findViewById(R.id.search_participants);
        recyclerView = findViewById(R.id.recycler_view);
        fab_create_group=findViewById(R.id.fab_create_group);

        loadingBar=new ProgressDialog(AddParticipants.this);

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Add Participants");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AddParticipants.this,MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        });

        LinearLayoutManager linearLayoutManager=new LinearLayoutManager(AddParticipants.this);
        recyclerView.setLayoutManager(linearLayoutManager);

        intent=getIntent();
        statusByIntent=intent.getStringExtra("status");
        groupIDByIntent=intent.getStringExtra("groupID");
        groupNameByIntent=intent.getStringExtra("groupName");

        currentUserID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        buddyRef = FirebaseDatabase.getInstance().getReference().child("Buddies").child(currentUserID);
        userRef=FirebaseDatabase.getInstance().getReference().child("Users");
        rootRef=FirebaseDatabase.getInstance().getReference();

        fab_create_group.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (statusByIntent.equals("new")) {

                    final AlertDialog dialog = new AlertDialog.Builder(AddParticipants.this).create();
                    final LayoutInflater inflater = getLayoutInflater();
                    View dialogView = inflater.inflate(R.layout.dialog_for_add_groups, null);

                    final EditText editTextGroupName = dialogView.findViewById(R.id.input_group_name);
                    final EditText editTextGroupStatus = dialogView.findViewById(R.id.input_group_status);
                    Button submit_btn = dialogView.findViewById(R.id.submit_btn);
                    final ProgressBar progressBar = dialogView.findViewById(R.id.progressbar);

                    submit_btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            final String groupName = editTextGroupName.getText().toString();
                            String groupStatus = editTextGroupStatus.getText().toString();

                            if (groupName.isEmpty()) {

                                editTextGroupName.setError("Field is empty");
                                editTextGroupName.requestFocus();
                                return;
                            }

                            if (groupStatus.isEmpty()) {
                                editTextGroupStatus.setError("Field is empty");
                                editTextGroupStatus.requestFocus();
                                return;
                            }

                            progressBar.setVisibility(View.VISIBLE);

                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
                            String currentDateTime = sdf.format(new Date());

                            long dateTimeInMillis = ConvertDateTimeIntoMilis(currentDateTime);

                            final String groupkey = rootRef.child("Groups").push().getKey();

                            HashMap<String, String> hashMap = new HashMap<>();
                            hashMap.put("id", groupkey);
                            hashMap.put("name", groupName);
                            hashMap.put("status", groupStatus);
                            hashMap.put("image", "default");
                            hashMap.put("date", dateTimeInMillis + "");

                            rootRef.child("Groups").child(groupkey).setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    progressBar.setVisibility(View.GONE);

                                    if (task.isSuccessful()) {

                                        rootRef.child("GroupChatList").child(currentUserID).child(groupkey).child("name").setValue(groupName);

                                        HashMap<String, Object> hashMap2 = new HashMap<>();

                                        hashMap2.put("sender", "default");
                                        hashMap2.put("message", "default");
                                        hashMap2.put("date", "default");

                                        rootRef.child("GroupChats").child(groupkey).child(currentUserID).push().setValue(hashMap2);

                                        for (Map.Entry mapElement : hashMapContacts.entrySet()) {
                                            String key = (String) mapElement.getKey();
                                            String value = (String) mapElement.getValue();

                                            if (value.equals("true")) {

                                                rootRef.child("GroupChatList").child(key).child(groupkey).child("name").setValue(groupName);
                                                rootRef.child("GroupChats").child(groupkey).child(key).push().setValue(hashMap2);
                                            }
                                        }
                                        Toast.makeText(getApplicationContext(), "Success", Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();
                                        startActivity(new Intent(AddParticipants.this,MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                                    } else {
                                        Toast.makeText(getApplicationContext(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                        Log.i("Error", task.getException().getMessage());
                                        return;
                                    }
                                }
                            });


                        }
                    });

                    dialog.setView(dialogView);
                    dialog.show();
                }
                else {

                    rootRef.child("GroupChatList").child(currentUserID).child(groupIDByIntent).child("name").setValue(groupNameByIntent);

                    HashMap<String, Object> hashMap2 = new HashMap<>();

                    hashMap2.put("sender", "default");
                    hashMap2.put("message", "default");
                    hashMap2.put("date", "default");

                    rootRef.child("GroupChats").child(groupIDByIntent).child(currentUserID).push().setValue(hashMap2);

                    for (Map.Entry mapElement : hashMapContacts.entrySet()) {
                        String key = (String) mapElement.getKey();
                        String value = (String) mapElement.getValue();

                        if (value.equals("true")) {

                            rootRef.child("GroupChatList").child(key).child(groupIDByIntent).child("name").setValue(groupNameByIntent);
                            rootRef.child("GroupChats").child(groupIDByIntent).child(key).push().setValue(hashMap2);
                        }
                    }
                    Toast.makeText(getApplicationContext(), "Success", Toast.LENGTH_SHORT).show();

                    startActivity(new Intent(AddParticipants.this,MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));

                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        searchUser("");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_participants_menu,menu);
        return true;
    }

    MenuItem search_menu;

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        search_menu=menu.findItem(R.id.search);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){

            case R.id.search:

                if (editTextSearch.getVisibility()==View.VISIBLE){
                    toolbar.setBackgroundColor(ContextCompat.getColor(this,R.color.colorPrimary));
                    toolbar.setNavigationIcon(R.drawable.ic_arrow_white_24dp);
                    search_menu.setIcon(R.drawable.ic_search_black_24dp);
                    editTextSearch.setVisibility(View.GONE);
                }
                else{
                    toolbar.setBackgroundColor(ContextCompat.getColor(this,R.color.white));
                    toolbar.setNavigationIcon(R.drawable.ic_arrow_color_primary_24dp);
                    search_menu.setIcon(R.drawable.ic_search_color_primary_24dp);
                    editTextSearch.setVisibility(View.VISIBLE);
                }

        }

        return false;
    }


    public static class UserViewHolder extends RecyclerView.ViewHolder {

        TextView userName, userStatus;
        CircleImageView profileImage;


        public UserViewHolder(@NonNull View itemView) {
            super(itemView);

            userName = itemView.findViewById(R.id.username);
            userStatus = itemView.findViewById(R.id.userstatusOrlastmessage);
            profileImage = itemView.findViewById(R.id.profile_image);
        }
    }

    private void searchUser(String s) {

        FirebaseRecyclerOptions options;

        if (s.equals("")){

            options=new FirebaseRecyclerOptions.Builder<User>()
                    .setQuery(buddyRef,User.class)
                    .build();
        }
        else{

            options=new FirebaseRecyclerOptions.Builder<User>()
                    .setQuery(buddyRef.orderByChild("NameForSearch").startAt(s).endAt(s+"\uf8ff"),User.class)
                    .build();
        }

        FirebaseRecyclerAdapter<User,UserViewHolder>adapter=new FirebaseRecyclerAdapter<User, UserViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final UserViewHolder userViewHolder, int position, @NonNull final User user) {

                final String userIDs=getRef(position).getKey();

                Toast.makeText(getApplicationContext(),user.getName(),Toast.LENGTH_SHORT).show();

                userRef.child(userIDs).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        userViewHolder.userName.setText(dataSnapshot.child("name").getValue().toString());
                        userViewHolder.userStatus.setText(dataSnapshot.child("status").getValue().toString());

                        String imageUrl=dataSnapshot.child("image").getValue().toString();

                        if (!imageUrl.equals("default")){
                            Picasso.get().load(imageUrl).placeholder(R.drawable.ic_account_circle).into(userViewHolder.profileImage);
                        }

                        if (user.isSelected()){
                            userViewHolder.itemView.setBackgroundColor(Color.CYAN);
                            hashMapContacts.put(userIDs,"true");
                        }
                        else{
                            userViewHolder.itemView.setBackgroundColor(Color.WHITE);
                            hashMapContacts.put(userIDs,"false");
                        }

                        userViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                if (user.isSelected()){
                                    user.setSelected(false);
                                    userViewHolder.itemView.setBackgroundColor(Color.WHITE);
                                    hashMapContacts.put(userIDs,"false");
                                }
                                else{
                                    user.setSelected(true);
                                    userViewHolder.itemView.setBackgroundColor(Color.CYAN);
                                    hashMapContacts.put(userIDs,"true");
                                }
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @NonNull
            @Override
            public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

                View view=LayoutInflater.from(parent.getContext()).inflate(R.layout.buddies_display_layout,parent,false);
                UserViewHolder viewHolder=new UserViewHolder(view);
                return viewHolder;
            }
        };

        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }

    private long ConvertDateTimeIntoMilis(String currentDateTime){

        SimpleDateFormat sdf=new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date= null;
        try {
            date = sdf.parse(currentDateTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date.getTime();
    }

}
