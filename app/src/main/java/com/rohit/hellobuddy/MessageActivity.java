package com.rohit.hellobuddy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rohit.hellobuddy.Adapter.MessageAdapter;
import com.rohit.hellobuddy.model.Chat;
import com.rohit.hellobuddy.model.User;
import com.squareup.picasso.Picasso;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    CircleImageView profile_image;
    TextView username,status;
    ImageButton btn_send;
    EditText text_send;
    ProgressBar progressBar;

    MessageAdapter messageAdapter;

    Intent intent;
    String userid;

    ValueEventListener seenListener;

    FirebaseUser fuser;
    DatabaseReference reference,userRef,chatRef,chatListRef;

    List<Chat>mChat;

    String userNameForSearch,currentUserName;

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
                startActivity(new Intent(MessageActivity.this,MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
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
        userRef=FirebaseDatabase.getInstance().getReference().child("Users");
        chatRef=FirebaseDatabase.getInstance().getReference().child("Chats");
        chatListRef=FirebaseDatabase.getInstance().getReference().child("ChatList");

        intent=getIntent();

        userid=intent.getStringExtra("id");

        userRef.child(fuser.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                currentUserName=dataSnapshot.child("name").getValue().toString();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        reference=chatListRef.child(userid).child(fuser.getUid());
        seenListener=reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                chatListRef.child(userid).child(fuser.getUid()).child("messageSeen").setValue("true");
                chatListRef.child(userid).child(fuser.getUid()).child("unseenMsgCount").setValue("0");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        readUserInfo();

        profile_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent=new Intent(MessageActivity.this,ProfileView.class);
                intent.putExtra("userID",userid);
                startActivity(intent);
            }
        });

        username.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent=new Intent(MessageActivity.this,ProfileView.class);
                intent.putExtra("userID",userid);
                startActivity(intent);
            }
        });

        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String msg=text_send.getText().toString();
                if(!msg.equals("") && msg.trim().length()>0){

                    sendMessage(fuser.getUid(), userid, msg);
                }
                text_send.setText("");
            }
        });

        text_send.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                FirebaseDatabase.getInstance().getReference().child("Users").child(fuser.getUid()).child("currentStatus").setValue("typing...");
            }

            @Override
            public void afterTextChanged(Editable s) {
                status.post(new Runnable() {
                    int i = 0;

                    @Override
                    public void run() {
                        if (i == 1) {
                            FirebaseDatabase.getInstance().getReference().child("Users").child(fuser.getUid()).child("currentStatus").setValue("online");
                        } else {
                            i++;
                            status.postDelayed(this, 2000);
                        }
                    }
                });
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){

            case R.id.clear_conversation:

                progressBar.setVisibility(View.VISIBLE);

                final DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference().child("Chats");

                chatsRef.child(fuser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                            Chat chat = snapshot.getValue(Chat.class);

                            if (chat.getReceiver().equals(fuser.getUid()) && chat.getSender().equals(userid) ||
                                    chat.getReceiver().equals(userid) && chat.getSender().equals(fuser.getUid())) {

                                progressBar.setVisibility(View.VISIBLE);

                                chatsRef.child(snapshot.getKey()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        progressBar.setVisibility(View.GONE);
                                        if (!task.isSuccessful()) {
                                            Log.i("DeleteError", task.getException().getMessage());
                                        }
                                    }
                                });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

                chatsRef.child(userid).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                            Chat chat = snapshot.getValue(Chat.class);

                            if (chat.getReceiver().equals(fuser.getUid()) && chat.getSender().equals(userid) ||
                                    chat.getReceiver().equals(userid) && chat.getSender().equals(fuser.getUid())) {

                                progressBar.setVisibility(View.VISIBLE);

                                chatsRef.child(snapshot.getKey()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        progressBar.setVisibility(View.GONE);
                                        if (!task.isSuccessful()) {
                                            Log.i("DeleteError", task.getException().getMessage());
                                        }
                                    }
                                });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

                progressBar.setVisibility(View.VISIBLE);

                FirebaseDatabase.getInstance().getReference().child("ChatList").child(fuser.getUid()).child(userid).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        progressBar.setVisibility(View.GONE);
                        if (!task.isSuccessful()) {
                            Log.i("DeleteError", task.getException().getMessage());
                        } else {
                            progressBar.setVisibility(View.VISIBLE);

                            FirebaseDatabase.getInstance().getReference().child("ChatList").child(userid).child(fuser.getUid()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    progressBar.setVisibility(View.GONE);
                                    if (!task.isSuccessful()) {
                                        Log.i("DeleteError", task.getException().getMessage());
                                    } else {
                                        startActivity(new Intent(MessageActivity.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                                    }
                                }
                            });
                        }
                    }
                });
        }

        return false;
    }

    private void sendMessage(final String sender, final String receiver, String message) {

        SimpleDateFormat sdf=new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());

        String currentDateTime=sdf.format(new Date());

        final long timeInMillis=ConvertDateTimeIntoMilis(currentDateTime);

        chatListRef.child(sender).child(receiver).child("lastMessage").setValue(message);
        chatListRef.child(receiver).child(sender).child("lastMessage").setValue(message);
        chatListRef.child(sender).child(receiver).child("lastMessageDate").setValue(timeInMillis+"");
        chatListRef.child(receiver).child(sender).child("lastMessageDate").setValue(timeInMillis+"");
        chatListRef.child(sender).child(receiver).child("messageSeen").setValue("false");

        chatListRef.child(sender).child(receiver).child("unseenMsgCount").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (!dataSnapshot.exists()){
                    chatListRef.child(sender).child(receiver).child("unseenMsgCount").setValue("1");
                }
                else {
                    String total=dataSnapshot.getValue().toString();
                    int temp=Integer.parseInt(total)+1;
                    chatListRef.child(sender).child(receiver).child("unseenMsgCount").setValue(String.valueOf(temp));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        HashMap<String,Object>hashMap=new HashMap<>();
        hashMap.put("sender",sender);
        hashMap.put("receiver",receiver);
        hashMap.put("message",message);
        hashMap.put("date",timeInMillis+"");

        chatRef.child(sender).push().setValue(hashMap);
        chatRef.child(receiver).push().setValue(hashMap);

        chatListRef.child(fuser.getUid()).child(userid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(!dataSnapshot.exists()){
                    chatListRef.child(fuser.getUid()).child(userid).child("id").setValue(userid);
                    chatListRef.child(fuser.getUid()).child(userid).child("NameForSearch").setValue(userNameForSearch.toLowerCase());
                }
                chatListRef.child(fuser.getUid()).child(userid).child("date").setValue(timeInMillis+"");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        chatListRef.child(userid).child(fuser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()) {
                    chatListRef.child(userid).child(fuser.getUid()).child("id").setValue(fuser.getUid());
                    chatListRef.child(userid).child(fuser.getUid()).child("NameForSearch").setValue(currentUserName);

                }
                chatListRef.child(userid).child(fuser.getUid()).child("date").setValue(timeInMillis+"");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void readMessage(final String myid, final String userid) {

        mChat=new ArrayList<>();

        chatRef.child(myid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                mChat.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()){

                    Chat chat=snapshot.getValue(Chat.class);

                    if (chat.getReceiver().equals(myid) && chat.getSender().equals(userid) ||
                    chat.getReceiver().equals(userid) && chat.getSender().equals(myid)){

                        mChat.add(chat);
                    }

                    messageAdapter=new MessageAdapter(MessageActivity.this,mChat);
                    recyclerView.setAdapter(messageAdapter);
                }
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

        reference.removeEventListener(seenListener);

        status("offline");
    }

    void readUserInfo(){

        userRef.child(userid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                User user=dataSnapshot.getValue(User.class);

                userNameForSearch=user.getName();

                username.setText(userNameForSearch);

                if (!user.getImage().equals("default")) {
                   Picasso.get().load(user.getImage()).placeholder(R.drawable.ic_account_circle).into(profile_image);
                }

                if (user.getCurrentStatus().equals("online")){
                    status.setText("Online");
                }
                else if (user.getCurrentStatus().equals("typing...")){
                    status.setText("typing...");
                }
                else{

                    String dateWithTime=user.getLastSeenDate();

                    String day=dateWithTime.substring(0,2);
                    String month=dateWithTime.substring(3,5);
                    String year=dateWithTime.substring(6,10);
                    String hour=dateWithTime.substring(11,13);
                    String min=dateWithTime.substring(14,16);
                    String sec=dateWithTime.substring(17,19);

                    SimpleDateFormat sdf=new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
                    String currentDateTime=sdf.format(new Date());

                    if (year.equals(currentDateTime.substring(6,10))){

                        if (month.equals(currentDateTime.substring(3,5))){

                            if (day.equals(currentDateTime.substring(0,2))){

                                if (hour.equals(currentDateTime.substring(11,13))){

                                    if (min.equals(currentDateTime.substring(14,16))){

                                        int intSec= Integer.parseInt(sec);
                                        int intCurrentSec=Integer.parseInt(currentDateTime.substring(17,19));

                                        status.setText("Active "+(intCurrentSec-intSec)+"sec ago");
                                    }
                                    else{

                                        int intMin= Integer.parseInt(min);
                                        int intCurrentMin=Integer.parseInt(currentDateTime.substring(14,16));

                                        status.setText("Active "+(intCurrentMin-intMin)+"min ago");
                                    }
                                }
                                else{

                                    int intHour= Integer.parseInt(hour);
                                    int intCurrentHour=Integer.parseInt(currentDateTime.substring(11,13));

                                    status.setText("Active "+(intCurrentHour-intHour)+"hour ago");
                                }
                            }
                            else{

                                int intDay= Integer.parseInt(day);
                                int intCurrentDay=Integer.parseInt(currentDateTime.substring(0,2));

                                status.setText("Active "+(intCurrentDay-intDay)+"day ago");
                            }
                        }
                        else{

                            int intMonth= Integer.parseInt(month);
                            int intCurrentMonth=Integer.parseInt(currentDateTime.substring(3,5));

                            status.setText("Active "+(intCurrentMonth-intMonth)+"month ago");
                        }
                    }
                    else{

                        int intYear= Integer.parseInt(year);
                        int intCurrentYear=Integer.parseInt(currentDateTime.substring(6,10));

                        status.setText("Active "+(intCurrentYear-intYear)+"year ago");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        readMessage(fuser.getUid(),userid);
    }
}
