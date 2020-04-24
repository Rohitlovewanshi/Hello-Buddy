package com.rohit.hellobuddy.Fragments;


import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rohit.hellobuddy.MessageActivity;
import com.rohit.hellobuddy.R;
import com.rohit.hellobuddy.model.User;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class BuddiesFragment extends Fragment {

    private RecyclerView myBuddyList;
    private View buddyView;

    private FloatingActionButton fab_add_buddy;

    private FirebaseAuth mAuth;
    private DatabaseReference buddyRef,userRef;

    private String currentUserID,currentUserName,currentUserPhone;

    private EditText editTextSearch;
    TextView textViewNothing;


    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        buddyView= inflater.inflate(R.layout.fragment_buddies, container, false);

        setHasOptionsMenu(true);

        myBuddyList=buddyView.findViewById(R.id.buddy_list);
        fab_add_buddy=buddyView.findViewById(R.id.fab_add_buddy);
        editTextSearch=buddyView.findViewById(R.id.search_user);
        textViewNothing=buddyView.findViewById(R.id.txt_nothing);
        myBuddyList.setLayoutManager(new LinearLayoutManager(getContext()));

        textViewNothing.setVisibility(View.VISIBLE);

        mAuth=FirebaseAuth.getInstance();
        currentUserID=mAuth.getCurrentUser().getUid();
        buddyRef= FirebaseDatabase.getInstance().getReference().child("Buddies");
        userRef=FirebaseDatabase.getInstance().getReference().child("Users");


        userRef.child(currentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                currentUserName=dataSnapshot.child("name").getValue().toString();
                currentUserPhone=dataSnapshot.child("phone").getValue().toString();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        searchUsers("");

        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchUsers(s.toString().toLowerCase());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });


        fab_add_buddy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final AlertDialog dialog=new AlertDialog.Builder(getContext()).create();
                final LayoutInflater inflater=getLayoutInflater();
                View dialogView=inflater.inflate(R.layout.dialog_add_buddy_layout,null);
                final EditText editTextInputNumber=dialogView.findViewById(R.id.input_number);
                final EditText editTextCountryCode=dialogView.findViewById(R.id.input_country_code);
                Button search_btn=dialogView.findViewById(R.id.btn_search);
                final RelativeLayout userDisplayLayout=dialogView.findViewById(R.id.user_display_layout);
                final CircleImageView profilePicture=dialogView.findViewById(R.id.profile_image);
                final TextView textViewUserName=dialogView.findViewById(R.id.username);
                final TextView textViewStatus=dialogView.findViewById(R.id.userstatusOrlastmessage);
                final TextView textViewBuddyStatus=dialogView.findViewById(R.id.buddy_status);
                final ProgressBar progressBar=dialogView.findViewById(R.id.progressbar);

                editTextCountryCode.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        userDisplayLayout.setVisibility(View.GONE);
                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                });

                editTextInputNumber.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        userDisplayLayout.setVisibility(View.GONE);
                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                });

                final String[] UserName = new String[1];
                final String[] userId = new String[1];

                search_btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        String countryCode=editTextCountryCode.getText().toString();
                        String number=editTextInputNumber.getText().toString();

                        if (countryCode.isEmpty()){
                            editTextCountryCode.setError("Field is empty");
                            editTextCountryCode.requestFocus();
                            return;
                        }

                        if (number.isEmpty()){
                            editTextInputNumber.setError("Field is empty");
                            editTextInputNumber.requestFocus();
                            return;
                        }

                        if (countryCode.length()!=3){
                            editTextCountryCode.setError("Invalid code");
                            editTextCountryCode.requestFocus();
                            return;
                        }

                        if (number.length()!=10){
                            editTextInputNumber.setError("Invalid number");
                            editTextInputNumber.requestFocus();
                            return;
                        }

                        final String phoneNumber=countryCode+number;

                        final Boolean[] flag = {false};

                        progressBar.setVisibility(View.VISIBLE);

                        userRef.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                                    User user=snapshot.getValue(User.class);

                                    if (phoneNumber.equals(user.getPhone())){

                                        if (!user.getImage().equals("default")) {
                                            Picasso.get().load(user.getImage()).into(profilePicture);
                                        }

                                        textViewUserName.setText(user.getName());
                                        UserName[0] =user.getName();
                                        textViewStatus.setText(user.getStatus());
                                        textViewBuddyStatus.setText("ADD");
                                        userId[0] =user.getUid();
                                        userDisplayLayout.setVisibility(View.VISIBLE);
                                        progressBar.setVisibility(View.GONE);
                                        flag[0] =true;
                                        checkForBuddy();
                                        break;

                                    }
                                }

                                if (!flag[0]){
                                    progressBar.setVisibility(View.GONE);
                                    textViewUserName.setText(phoneNumber);
                                    textViewStatus.setText("");
                                    textViewBuddyStatus.setText("Invite");
                                    userDisplayLayout.setVisibility(View.VISIBLE);
                                }
                            }

                            private void checkForBuddy() {

                                progressBar.setVisibility(View.VISIBLE);

                                buddyRef.child(currentUserID).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                        for (DataSnapshot snapshot : dataSnapshot.getChildren()){

                                            if (userId[0].equals(snapshot.getKey())){

                                                textViewBuddyStatus.setText("Message");
                                                break;
                                            }
                                        }
                                        progressBar.setVisibility(View.GONE);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                });

                textViewBuddyStatus.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (textViewBuddyStatus.getText().equals("ADD")){

                            if ((editTextCountryCode.getText().toString()+editTextInputNumber.getText().toString()).equals(currentUserPhone)){
                                Toast.makeText(getContext(),"You can't add your own account",Toast.LENGTH_SHORT).show();
                                return;
                            }

                            progressBar.setVisibility(View.VISIBLE);
                            buddyRef.child(currentUserID).child(userId[0]).child("NameForSearch").setValue(UserName[0]).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    progressBar.setVisibility(View.GONE);
                                    if (task.isSuccessful()){
                                        Toast.makeText(getContext(),"Buddy Added",Toast.LENGTH_SHORT).show();
                                        textViewBuddyStatus.setText("Message");
                                    }
                                    else{
                                        Log.i("BuddyAdd",task.getException().getMessage()+"");
                                    }
                                }
                            });
                        }
                        else if (textViewBuddyStatus.getText().equals("Message")){

                            dialog.dismiss();

                            Intent intent=new Intent(getContext(),MessageActivity.class);
                            intent.putExtra("id",userId[0]);
                            startActivity(intent);
                        }
                        else {

                            //Invite buddy code goes here
                        }
                    }
                });

                dialog.setView(dialogView);
                dialog.show();
            }
        });

        return buddyView;
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){

            case R.id.menu_search:
                if (editTextSearch.getVisibility()==View.GONE) {
                    editTextSearch.setVisibility(View.VISIBLE);
                }
                else{
                    editTextSearch.setVisibility(View.GONE);
                    if (!editTextSearch.getText().toString().isEmpty()){
                        editTextSearch.setText("");
                        searchUsers("");
                    }
                }

            default:
                break;
        }
        return false;
    }

    private void searchUsers(String s) {

        FirebaseRecyclerOptions options=new FirebaseRecyclerOptions.Builder<User>()
                .setQuery(buddyRef.child(currentUserID).orderByChild("NameForSearch").startAt(s).endAt(s+"\uf8ff"),User.class)
                .build();

        FirebaseRecyclerAdapter<User,ViewHolder>adapter=new FirebaseRecyclerAdapter<User, ViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final ViewHolder holder, int position, @NonNull User user) {

                final String usersIDs=getRef(position).getKey();

                userRef.child(usersIDs).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        if (dataSnapshot.exists()) {

                            textViewNothing.setVisibility(View.GONE);

                            String retName=dataSnapshot.child("name").getValue().toString();
                            String retImage=dataSnapshot.child("image").getValue().toString();
                            String retStatus=dataSnapshot.child("status").getValue().toString();

                            holder.userName.setText(retName);
                            if (!retImage.equals("default")) {
                                Picasso.get().load(retImage).placeholder(R.drawable.ic_account_circle).into(holder.profileImage);
                            }

                            holder.userStatus.setText(retStatus);
                            holder.img_on.setVisibility(View.GONE);
                            holder.img_off.setVisibility(View.GONE);

                            holder.itemView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent=new Intent(getContext(), MessageActivity.class);
                                    intent.putExtra("id",usersIDs);
                                    startActivity(intent);
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

            }

            @NonNull
            @Override
            public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.buddies_display_layout,parent,false);
                return new ViewHolder(view);
            }
        };

        myBuddyList.setAdapter(adapter);
        adapter.startListening();

    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        TextView userName,userStatus,textViewLastDate,textViewCountUnseenMsg;
        CircleImageView profileImage,img_on,img_off;

        public ViewHolder(View itemView){
            super(itemView);

            userName=itemView.findViewById(R.id.username);
            userStatus=itemView.findViewById(R.id.userstatusOrlastmessage);
            profileImage=itemView.findViewById(R.id.profile_image);
            textViewLastDate=itemView.findViewById(R.id.last_date);
            textViewCountUnseenMsg=itemView.findViewById(R.id.count_unseen_message);
            img_on=itemView.findViewById(R.id.img_on);
            img_off=itemView.findViewById(R.id.img_off);
        }
    }

}
