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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rohit.hellobuddy.model.User;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupProfileView extends AppCompatActivity {


    private RecyclerView groupList;

    Toolbar toolbar;
    CircleImageView group_profile_image;
    ImageButton edit_group_profile_picture;
    ProgressBar progressBar;
    TextView textViewGroupName,textViewGroupStatus;
    ImageView edit_group_name,edit_group_status;

    DatabaseReference rootRef,userRef;

    Intent intent;
    String groupID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_profile_view);

        toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Group Profile");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(GroupProfileView.this,MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        });

        group_profile_image=findViewById(R.id.group_profile_image);
        edit_group_profile_picture=findViewById(R.id.set_group_profile_button);
        progressBar=findViewById(R.id.progressbar);
        textViewGroupName=findViewById(R.id.display_group_name);
        textViewGroupStatus=findViewById(R.id.display_group_status);
        edit_group_name=findViewById(R.id.edit_group_name_button);
        edit_group_status=findViewById(R.id.edit_group_status_button);
        groupList=findViewById(R.id.group_list);

        groupList.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        rootRef= FirebaseDatabase.getInstance().getReference();
        userRef=FirebaseDatabase.getInstance().getReference().child("Users");

        intent=getIntent();
        groupID=intent.getStringExtra("id");

        updateGroupDetails();

        updateParticipants();
    }

    private void updateGroupDetails() {

        rootRef.child("Groups").child(groupID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {

                    String groupPicture=dataSnapshot.child("image").getValue().toString();
                    String groupName=dataSnapshot.child("name").getValue().toString();
                    String groupStatus=dataSnapshot.child("status").getValue().toString();

                    if (!groupPicture.equals("default")){
                        Picasso.get().load(groupPicture).placeholder(R.drawable.group_icon).into(group_profile_image);
                    }
                    textViewGroupName.setText(groupName);
                    textViewGroupStatus.setText(groupStatus);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void updateParticipants() {

        FirebaseRecyclerOptions options=new FirebaseRecyclerOptions.Builder<User>()
                .setQuery(rootRef.child("GroupChats").child(groupID),User.class)
                .build();

        FirebaseRecyclerAdapter<User,ViewHolder>adapter=new FirebaseRecyclerAdapter<User, ViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final ViewHolder holder, int position, @NonNull User user) {

                final String usersIDs=getRef(position).getKey();

                userRef.child(usersIDs).addValueEventListener(new ValueEventListener() {
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
}
