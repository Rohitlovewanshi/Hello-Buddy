package com.rohit.hellobuddy.Adapter;

import android.content.Context;
import android.content.Intent;
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
import com.rohit.hellobuddy.MessageActivity;
import com.rohit.hellobuddy.R;
import com.rohit.hellobuddy.model.Chat;
import com.rohit.hellobuddy.model.User;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    private Context mContext;
    private List<User> mUsers;
    private boolean isChat;

    String theLastMessage;

    public UserAdapter(Context mContext, List<User>mUsers, boolean isChat){
        this.mUsers=mUsers;
        this.mContext=mContext;
        this.isChat=isChat;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(mContext).inflate(R.layout.buddies_display_layout,parent,false);
        return new UserAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        final User user=mUsers.get(position);
        holder.userName.setText(user.getName());
        holder.profileImage.setImageResource(R.drawable.ic_account_circle);
        /*if(user.getImage().equals("default")){
            holder.profileImage.setImageResource(R.drawable.ic_account_circle);
        } else {
            Glide.with(mContext).load(user.getImageURL()).into(holder.profile_image);
        }*/

        if(isChat){
            lastMessage(user.getUid(),holder.userStatus,holder.textViewLastDate,holder.textViewCountUnseenMsg);
        } else {
            holder.userStatus.setText(user.getStatus());
        }

        if(isChat){
            if(user.getCurrentStatus().equals("online")){
                holder.img_on.setVisibility(View.VISIBLE);
                holder.img_off.setVisibility(View.GONE);
            } else {
                holder.img_on.setVisibility(View.GONE);
                holder.img_off.setVisibility(View.VISIBLE);
            }
        } else {
            holder.img_on.setVisibility(View.GONE);
            holder.img_off.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(mContext, MessageActivity.class);
                intent.putExtra("id",user.getUid());
                mContext.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mUsers.size();
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

    private void lastMessage(final String userid, final TextView last_msg, final TextView lastDate, final TextView countUnseen){
        theLastMessage="default";
        final FirebaseUser firebaseUser= FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference reference= FirebaseDatabase.getInstance().getReference().child("Chats").child(firebaseUser.getUid());

        final String[] date = new String[1];
        final int[] count = {0};

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                count[0]=0;
                for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                    Chat chat=snapshot.getValue(Chat.class);
                    if(chat.getReceiver().equals(firebaseUser.getUid()) && chat.getSender().equals(userid) ||
                            chat.getReceiver().equals(userid) && chat.getSender().equals(firebaseUser.getUid())){

                        theLastMessage=chat.getMessage();
                        date[0] =chat.getDate();
                    }
                    if (chat.getReceiver().equals(firebaseUser.getUid()) && chat.getSender().equals(userid)) {
                        if (!chat.isIsseen()) {
                            count[0]++;
                        }
                    }
                }

                switch (theLastMessage){
                    case "default":
                        last_msg.setText("No Message");
                        break;

                        default:
                            last_msg.setText(theLastMessage);
                            lastDate.setText(ModifyDate(date[0]));
                            if (count[0]!=0) {
                                countUnseen.setText(count[0] + "");
                                countUnseen.setVisibility(View.VISIBLE);
                            }
                            else{
                                countUnseen.setVisibility(View.GONE);
                            }
                            break;
                }

                theLastMessage="default";
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
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
