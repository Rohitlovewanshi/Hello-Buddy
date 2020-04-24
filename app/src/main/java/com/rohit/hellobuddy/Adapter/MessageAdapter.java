package com.rohit.hellobuddy.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rohit.hellobuddy.R;
import com.rohit.hellobuddy.model.Chat;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    private static final int MSG_TYPE_LEFT=0;
    private static final int MSG_TYPE_RIGHT=1;

    private Context mContext;
    private List<Chat> mChat;

    FirebaseUser fuser;

    public MessageAdapter(Context mContext,List<Chat>mChat){
        this.mChat=mChat;
        this.mContext=mContext;
    }

    @NonNull
    @Override
    public MessageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType==MSG_TYPE_RIGHT) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.chat_item_right, parent, false);
            return new MessageAdapter.ViewHolder(view);
        } else {
            View view = LayoutInflater.from(mContext).inflate(R.layout.chat_item_left, parent, false);
            return new MessageAdapter.ViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final MessageAdapter.ViewHolder holder, int position) {

        final Chat chat = mChat.get(position);

        fuser= FirebaseAuth.getInstance().getCurrentUser();


        holder.show_message.setText(chat.getMessage());

        String dateTime=ConvertMillisToDateTime(chat.getDate());

        String modifiedDateTime=dateTime.substring(0,10);

        SimpleDateFormat sdf=new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        String currentDateTime=sdf.format(new Date());

        if (modifiedDateTime.equals(currentDateTime)){
            modifiedDateTime="Today";
        }

        int hour= Integer.parseInt((dateTime.substring(11,13)));
        if (hour>=12){
            if (hour>12){
                hour=hour-12;
            }
            modifiedDateTime=modifiedDateTime+" "+hour+":"+dateTime.charAt(14)+dateTime.charAt(15)+" pm";
        }
        else{
            modifiedDateTime=modifiedDateTime+" "+hour+":"+dateTime.charAt(14)+dateTime.charAt(15)+" am";
        }

        holder.show_time.setText(modifiedDateTime);

        if(position==mChat.size()-1 && chat.getSender().equals(fuser.getUid())){

            DatabaseReference chatListRef= FirebaseDatabase.getInstance().getReference().child("ChatList");
            chatListRef.child(chat.getSender()).child(chat.getReceiver()).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.hasChild("messageSeen")){

                        if (dataSnapshot.child("messageSeen").getValue().toString().equals("true")){
                            holder.txt_seen.setText("seen");
                        }
                        else {
                            holder.txt_seen.setText("Delivered");
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            holder.txt_seen.setVisibility(View.VISIBLE);

        } else {
            holder.txt_seen.setVisibility(View.GONE);
        }

        holder.show_message.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (holder.show_time.getVisibility()==View.GONE) {
                    holder.show_time.setVisibility(View.VISIBLE);
                }
                else {
                    holder.show_time.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mChat.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        public TextView show_message;
        public TextView txt_seen;
        public TextView show_time;
        private View view;

        public ViewHolder(View itemView){
            super(itemView);
            view=itemView;
            show_message=itemView.findViewById(R.id.show_message);
            txt_seen=itemView.findViewById(R.id.txt_seen);
            show_time=itemView.findViewById(R.id.show_time);
        }
    }

    @Override
    public int getItemViewType(int position) {
        fuser= FirebaseAuth.getInstance().getCurrentUser();
        if(mChat.get(position).getSender().equals(fuser.getUid())){
            return MSG_TYPE_RIGHT;
        } else {
            return MSG_TYPE_LEFT;
        }
    }

    private String ConvertMillisToDateTime(String millis){

        DateFormat formatter=new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        long milliseconds=Long.parseLong(millis);
        Calendar calendar=Calendar.getInstance();
        calendar.setTimeInMillis(milliseconds);
        return formatter.format(calendar.getTime());
    }
}
