package com.rohit.hellobuddy.Fragments;


import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rohit.hellobuddy.AddParticipants;
import com.rohit.hellobuddy.GroupMessageActivity;
import com.rohit.hellobuddy.MessageActivity;
import com.rohit.hellobuddy.R;
import com.rohit.hellobuddy.model.Group;
import com.rohit.hellobuddy.model.User;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupsFragment extends Fragment {

    RecyclerView groups_recyclerView;
    FloatingActionButton create_group_actionBar;
    EditText editTextSearch;

    DatabaseReference rootRef;
    String currentUserId;
    TextView textViewNothing;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view= inflater.inflate(R.layout.fragment_groups, container, false);

        setHasOptionsMenu(true);

        groups_recyclerView=view.findViewById(R.id.group_list);
        create_group_actionBar=view.findViewById(R.id.create_group);
        editTextSearch=view.findViewById(R.id.search_groups);
        textViewNothing=view.findViewById(R.id.txt_nothing);

        textViewNothing.setVisibility(View.VISIBLE);

        groups_recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager=new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        groups_recyclerView.setLayoutManager(linearLayoutManager);

        rootRef= FirebaseDatabase.getInstance().getReference();
        currentUserId= FirebaseAuth.getInstance().getCurrentUser().getUid();

        create_group_actionBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent AddContactIntent =new Intent(getContext(),AddParticipants.class);
                AddContactIntent.putExtra("status","new");
                AddContactIntent.putExtra("groupID","new");
                AddContactIntent.putExtra("groupName","new");
                startActivity(AddContactIntent);
            }
        });

        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                SearchGroups(s.toString().toLowerCase());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        return view;
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
                        SearchGroups("");
                    }
                }

            default:
                break;
        }
        return false;
    }

    @Override
    public void onStart() {
        super.onStart();

        SearchGroups("");
    }

    public static class GroupViewHolder extends RecyclerView.ViewHolder {

        TextView groupName,groupStatus,textViewLastDate,textViewCountUnseenMsg;
        CircleImageView groupImage,img_on,img_off;

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);

            groupName = itemView.findViewById(R.id.username);
            groupStatus = itemView.findViewById(R.id.userstatusOrlastmessage);
            groupImage = itemView.findViewById(R.id.profile_image);
            textViewLastDate = itemView.findViewById(R.id.last_date);
            textViewCountUnseenMsg = itemView.findViewById(R.id.count_unseen_message);
            img_on = itemView.findViewById(R.id.img_on);
            img_off = itemView.findViewById(R.id.img_off);
        }
    }

    private void SearchGroups(String s) {

        FirebaseRecyclerOptions options;

        options=new FirebaseRecyclerOptions.Builder<Group>()
                .setQuery(rootRef.child("GroupChatList").child(currentUserId).orderByChild("name").startAt(s).endAt(s+"\uf8ff"),Group.class)
                .build();

        FirebaseRecyclerAdapter<Group, GroupViewHolder> adapter=new FirebaseRecyclerAdapter<Group, GroupViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final GroupViewHolder groupViewHolder, int position, @NonNull final Group group) {

                final String groupID=getRef(position).getKey();

                rootRef.child("Groups").child(groupID).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        textViewNothing.setVisibility(View.GONE);

                        final Group group1=dataSnapshot.getValue(Group.class);

                        groupViewHolder.groupName.setText(group1.getName());
                        groupViewHolder.groupStatus.setText(group1.getStatus());

                        if (group1.getImage().equals("default")){

                            groupViewHolder.groupImage.setImageResource(R.drawable.group_icon);
                        }
                        else{
                            Picasso.get().load(group1.getImage()).placeholder(R.drawable.group_icon).into(groupViewHolder.groupImage);
                        }

                        groupViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                Intent intent=new Intent(getContext(), GroupMessageActivity.class);
                                intent.putExtra("id",group1.getId());
                                startActivity(intent);

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
            public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

                View view=LayoutInflater.from(parent.getContext()).inflate(R.layout.buddies_display_layout,parent,false);
                GroupViewHolder viewHolder=new GroupViewHolder(view);
                return viewHolder;
            }
        };

        groups_recyclerView.setAdapter(adapter);
        adapter.startListening();
    }
}
