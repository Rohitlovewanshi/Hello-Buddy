package com.rohit.hellobuddy.Adapter;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
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
import com.rohit.hellobuddy.model.GroupChat;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GroupMessageAdapter extends RecyclerView.Adapter<GroupMessageAdapter.ViewHolder> {

    private static final int MSG_TYPE_LEFT=0;
    private static final int MSG_TYPE_RIGHT=1;

    private Context mContext;
    private List<GroupChat> mChat;

    FirebaseUser fuser;
    DatabaseReference ref;

    public GroupMessageAdapter(Context mContext,List<GroupChat>mChat){
        this.mChat=mChat;
        this.mContext=mContext;
    }

    @NonNull
    @Override
    public GroupMessageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType==MSG_TYPE_RIGHT) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.chat_item_right, parent, false);
            return new GroupMessageAdapter.ViewHolder(view);
        } else {
            View view = LayoutInflater.from(mContext).inflate(R.layout.chat_item_left, parent, false);
            return new GroupMessageAdapter.ViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final GroupMessageAdapter.ViewHolder holder, int position) {

        final GroupChat groupChat = mChat.get(position);


        holder.show_message.setText(groupChat.getMessage());

        if (!groupChat.getDate().equals("default")) {

            String dateTime = ConvertMillisToDateTime(groupChat.getDate());

            String modifiedDateTime = dateTime.substring(0, 10);

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

            String currentDateTime = sdf.format(new Date());

            if (modifiedDateTime.equals(currentDateTime)) {
                modifiedDateTime = "Today";
            }

            int hour = Integer.parseInt((dateTime.substring(11, 13)));
            if (hour >= 12) {
                if (hour > 12) {
                    hour = hour - 12;
                }
                modifiedDateTime = modifiedDateTime + " " + hour + ":" + dateTime.charAt(14) + dateTime.charAt(15) + " pm";
            } else {
                modifiedDateTime = modifiedDateTime + " " + hour + ":" + dateTime.charAt(14) + dateTime.charAt(15) + " am";
            }

            holder.show_time.setText(modifiedDateTime);

            fuser=FirebaseAuth.getInstance().getCurrentUser();

            if (!fuser.getUid().equals(groupChat.getSender())){

                ref= FirebaseDatabase.getInstance().getReference().child("Users").child(groupChat.getSender());

                ref.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        holder.sender_number.setText(dataSnapshot.child("phone").getValue().toString());
                        holder.sender_number.setVisibility(View.VISIBLE);
                        holder.sender_name.setText("~"+dataSnapshot.child("name").getValue().toString());
                        holder.sender_name.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        }
        else {

            holder.sender_name.setVisibility(View.GONE);
            holder.show_message.setVisibility(View.GONE);
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

        public TextView show_message,txt_seen,show_time,sender_name,sender_number;
        private View view;

        public ViewHolder(View itemView){
            super(itemView);
            view=itemView;
            show_message=itemView.findViewById(R.id.show_message);
            txt_seen=itemView.findViewById(R.id.txt_seen);
            show_time=itemView.findViewById(R.id.show_time);
            sender_name=itemView.findViewById(R.id.sender_name);
            sender_number=itemView.findViewById(R.id.sender_number);
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
