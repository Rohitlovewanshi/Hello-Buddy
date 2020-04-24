package com.rohit.hellobuddy.Fragments;

import android.content.Intent;
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

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rohit.hellobuddy.MessageActivity;
import com.rohit.hellobuddy.R;
import com.rohit.hellobuddy.model.ChatList;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatsFragment extends Fragment {

    private RecyclerView recyclerView;

    FirebaseUser fuser;
    DatabaseReference chatListRef,userRef;

    EditText editTextSearch;
    TextView textViewNothing;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view= inflater.inflate(R.layout.fragment_chats, container, false);

        setHasOptionsMenu(true);

        recyclerView=view.findViewById(R.id.recycler_view);
        editTextSearch=view.findViewById(R.id.search_user);
        textViewNothing=view.findViewById(R.id.txt_nothing);
        LinearLayoutManager linearLayoutManager=new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        textViewNothing.setVisibility(View.VISIBLE);

        fuser= FirebaseAuth.getInstance().getCurrentUser();
        chatListRef=FirebaseDatabase.getInstance().getReference().child("ChatList");
        userRef=FirebaseDatabase.getInstance().getReference().child("Users");

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

        return view;
    }

    private void searchUsers(String s) {

        FirebaseRecyclerOptions options;

        if(s.equals("")){

            options=new FirebaseRecyclerOptions.Builder<ChatList>()
                    .setQuery(chatListRef.child(fuser.getUid()).orderByChild("date"),ChatList.class)
                    .build();
        }
        else {

            options=new FirebaseRecyclerOptions.Builder<ChatList>()
                    .setQuery(chatListRef.child(fuser.getUid()).orderByChild("NameForSearch").startAt(s).endAt(s+"\uf8ff"),ChatList.class)
                    .build();
        }


        FirebaseRecyclerAdapter<ChatList,ViewHolder>adapter=new FirebaseRecyclerAdapter<ChatList, ViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final ViewHolder holder, int position, @NonNull ChatList chatList) {

                final String userIDs=getRef(position).getKey();

                userRef.child(userIDs).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        if (dataSnapshot.exists()) {

                            textViewNothing.setVisibility(View.GONE);

                            String retName=dataSnapshot.child("name").getValue().toString();
                            String retImage=dataSnapshot.child("image").getValue().toString();

                            holder.userName.setText(retName);

                            if(retImage.equals("default")){
                                holder.profileImage.setImageResource(R.drawable.ic_account_circle);
                            } else {
                                Picasso.get().load(retImage).placeholder(R.drawable.ic_account_circle).into(holder.profileImage);
                            }

                            if (dataSnapshot.child("currentStatus").getValue().toString().equals("online")){
                                holder.img_on.setVisibility(View.VISIBLE);
                                holder.img_off.setVisibility(View.GONE);
                            }
                            else {
                                holder.img_on.setVisibility(View.GONE);
                                holder.img_off.setVisibility(View.VISIBLE);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

                chatListRef.child(userIDs).child(fuser.getUid()).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        if (dataSnapshot.exists()) {

                            holder.userStatus.setText(dataSnapshot.child("lastMessage").getValue().toString());
                            holder.textViewLastDate.setText(ModifyDate(dataSnapshot.child("lastMessageDate").getValue().toString()));
                            if (!dataSnapshot.child("unseenMsgCount").getValue().toString().equals("0")) {
                                holder.textViewCountUnseenMsg.setText(dataSnapshot.child("unseenMsgCount").getValue().toString());
                                holder.textViewCountUnseenMsg.setVisibility(View.VISIBLE);
                            }
                            else
                                holder.textViewCountUnseenMsg.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent=new Intent(getContext(), MessageActivity.class);
                        intent.putExtra("id",userIDs);
                        startActivity(intent);
                    }
                });

            }

            @NonNull
            @Override
            public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

                View view=LayoutInflater.from(parent.getContext()).inflate(R.layout.buddies_display_layout,parent,false);
                return new ViewHolder(view);
            }
        };

        recyclerView.setAdapter(adapter);
        adapter.startListening();
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
                    }
                }

            default:
                break;
        }
        return false;
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


    private String ConvertMillisToDateTime(String millis){

        DateFormat formatter=new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        long milliseconds=Long.parseLong(millis);
        Calendar calendar=Calendar.getInstance();
        calendar.setTimeInMillis(milliseconds);
        return formatter.format(calendar.getTime());
    }

    private String ModifyDate(String millis){

        String dateTime = ConvertMillisToDateTime(millis);
        String modifiedDate = dateTime.substring(0, 10);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        String currentDateTime = sdf.format(new Date());

        if (modifiedDate.equals(currentDateTime)) {

            int hour = Integer.parseInt((dateTime.substring(11, 12) + dateTime.substring(12, 13)));
            if (hour >= 12) {
                if (hour > 12) {
                    hour = hour - 12;
                }
                modifiedDate = hour + ":" + dateTime.charAt(14) + dateTime.charAt(15) + " pm";
            } else {
                modifiedDate = hour + ":" + dateTime.charAt(14) + dateTime.charAt(15) + " am";
            }
        }
        return modifiedDate;
    }
}
