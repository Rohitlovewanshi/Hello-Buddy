package com.rohit.hellobuddy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rohit.hellobuddy.model.User;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ViewGroupParticipants extends AppCompatActivity {

    private RecyclerView groupList;
    Toolbar toolbar;

    DatabaseReference rootRef,userRef;

    Intent intent;
    String groupID,currentUserID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_group_participants);

        toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Participants");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        groupList=findViewById(R.id.group_list);
        groupList.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        rootRef= FirebaseDatabase.getInstance().getReference();
        userRef=FirebaseDatabase.getInstance().getReference().child("Users");
        currentUserID= FirebaseAuth.getInstance().getCurrentUser().getUid();

        intent=getIntent();
        groupID=intent.getStringExtra("id");

        updateParticipants();
    }

    private void updateParticipants() {

        FirebaseRecyclerOptions options=new FirebaseRecyclerOptions.Builder<User>()
                .setQuery(rootRef.child("GroupChats").child(groupID),User.class)
                .build();

        FirebaseRecyclerAdapter<User,ViewHolder> adapter=new FirebaseRecyclerAdapter<User, ViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final ViewHolder holder, int position, @NonNull User user) {

                final String usersIDs=getRef(position).getKey();

                userRef.child(usersIDs).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        if (dataSnapshot.exists()) {

                            String retName=dataSnapshot.child("name").getValue().toString();
                            String retImage=dataSnapshot.child("image").getValue().toString();
                            String retStatus=dataSnapshot.child("status").getValue().toString();

                            holder.userName.setText(retName);
                            if (!retImage.equals("default")) {
                                Picasso.get().load(retImage).placeholder(R.drawable.ic_account_circle).into(holder.profileImage);
                            }

                            holder.userStatus.setText(retStatus);

                            holder.itemView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent=new Intent(ViewGroupParticipants.this, MessageActivity.class);
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

        groupList.setAdapter(adapter);
        adapter.startListening();

    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        TextView userName,userStatus;
        CircleImageView profileImage;

        public ViewHolder(View itemView){
            super(itemView);

            userName=itemView.findViewById(R.id.username);
            userStatus=itemView.findViewById(R.id.userstatusOrlastmessage);
            profileImage=itemView.findViewById(R.id.profile_image);
        }
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

    private void status(String status){
        DatabaseReference reference=FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserID);

        SimpleDateFormat sdf=new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String currentDateTime=sdf.format(new Date());

        HashMap<String ,Object> hashMap=new HashMap<>();
        hashMap.put("currentStatus",status);
        hashMap.put("lastSeenDate",currentDateTime);

        reference.updateChildren(hashMap);
    }
}
