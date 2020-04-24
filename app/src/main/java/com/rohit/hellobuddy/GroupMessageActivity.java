package com.rohit.hellobuddy;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rohit.hellobuddy.Adapter.GroupMessageAdapter;
import com.rohit.hellobuddy.model.Group;
import com.rohit.hellobuddy.model.GroupChat;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupMessageActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    CircleImageView profile_image;
    TextView username,status;
    ImageButton btn_send;
    EditText text_send;
    ProgressBar progressBar;

    GroupMessageAdapter groupMessageAdapter;

    Intent intent;
    String groupId;

    FirebaseUser fuser;
    DatabaseReference reference,rootRef;

    List<GroupChat> mChat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        Toolbar toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        recyclerView=findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager=new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        profile_image=findViewById(R.id.profile_image);
        username=findViewById(R.id.username);
        status=findViewById(R.id.status);
        btn_send=findViewById(R.id.btn_send);
        text_send=findViewById(R.id.text_send);
        progressBar=findViewById(R.id.progressbar);

        fuser= FirebaseAuth.getInstance().getCurrentUser();
        rootRef=FirebaseDatabase.getInstance().getReference();

        intent=getIntent();

        groupId=intent.getStringExtra("id");

        readGroupInfo();

        profile_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent groupViewIntent=new Intent(GroupMessageActivity.this,GroupProfileView.class);
                groupViewIntent.putExtra("id",groupId);
                startActivity(groupViewIntent);
            }
        });

        username.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent groupViewIntent=new Intent(GroupMessageActivity.this,GroupProfileView.class);
                groupViewIntent.putExtra("id",groupId);
                startActivity(groupViewIntent);
            }
        });

        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String msg=text_send.getText().toString();
                if(!msg.equals("") && msg.trim().length()>0){

                    sendMessageToGroup(fuser.getUid(), msg);
                }
                text_send.setText("");
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.group_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){

            case R.id.add_contacts:
                Intent AddContactIntent=new Intent(GroupMessageActivity.this,AddParticipants.class);
                AddContactIntent.putExtra("status","old");
                AddContactIntent.putExtra("groupID",groupId);
                AddContactIntent.putExtra("groupName",username.getText().toString());
                startActivity(AddContactIntent);
                break;

            case R.id.exit_group:
                rootRef.child("GroupChatList").child(fuser.getUid()).child(groupId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            rootRef.child("GroupChats").child(groupId).child(fuser.getUid()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()){
                                        startActivity(new Intent(GroupMessageActivity.this,MainActivity.class));
                                    }
                                    else {
                                        Toast.makeText(GroupMessageActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                        else {
                            Toast.makeText(GroupMessageActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                break;

        }

        return false;
    }

    private void sendMessageToGroup(String sender, String msg) {

        final DatabaseReference groupRef=FirebaseDatabase.getInstance().getReference().child("GroupChats").child(groupId);

        final HashMap<String,Object>hashMap=new HashMap<>();

        SimpleDateFormat sdf=new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String currentDateTime=sdf.format(new Date());

        final long timeInMillis=ConvertDateTimeIntoMilis(currentDateTime);

        hashMap.put("sender",sender);
        hashMap.put("message",msg);
        hashMap.put("date",timeInMillis+"");

        groupRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                    groupRef.child(snapshot.getKey()).push().setValue(hashMap);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void readGroupMessage() {

        mChat=new ArrayList<>();

        reference=FirebaseDatabase.getInstance().getReference().child("GroupChats").child(groupId).child(fuser.getUid());

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                mChat.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()){

                    GroupChat groupChat=snapshot.getValue(GroupChat.class);

                    mChat.add(groupChat);


                }

                groupMessageAdapter=new GroupMessageAdapter(GroupMessageActivity.this,mChat);
                recyclerView.setAdapter(groupMessageAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
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

    private void status(String status){
        DatabaseReference reference1=FirebaseDatabase.getInstance().getReference("Users").child(fuser.getUid());

        SimpleDateFormat sdf=new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String currentDateTime=sdf.format(new Date());

        HashMap<String ,Object>hashMap=new HashMap<>();
        hashMap.put("currentStatus",status);
        hashMap.put("lastSeenDate",currentDateTime);

        reference1.updateChildren(hashMap);
    }

    @Override
    protected void onResume() {
        super.onResume();

        status("online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        status("offline");
    }

    private void readGroupInfo() {

        reference=FirebaseDatabase.getInstance().getReference().child("Groups");

        reference.child(groupId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                Group group=dataSnapshot.getValue(Group.class);

                username.setText(group.getName());
                status.setText(group.getStatus());

                if (group.getImage().equals("default")){

                    profile_image.setImageResource(R.drawable.group_icon);
                }
                else{
                    Picasso.get().load(group.getImage()).placeholder(R.drawable.group_icon).into(profile_image);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        readGroupMessage();
    }
}
