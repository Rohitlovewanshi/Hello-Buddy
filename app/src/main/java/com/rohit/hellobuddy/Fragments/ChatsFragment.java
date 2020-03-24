package com.rohit.hellobuddy.Fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.rohit.hellobuddy.Adapter.UserAdapter;
import com.rohit.hellobuddy.R;
import com.rohit.hellobuddy.model.ChatList;
import com.rohit.hellobuddy.model.User;
import java.util.ArrayList;
import java.util.List;

public class ChatsFragment extends Fragment {

    private RecyclerView recyclerView;

    FirebaseUser fuser;
    DatabaseReference reference;

    EditText editTextSearch;

    private UserAdapter userAdapter;
    private List<User>mUsers;
    private List<ChatList>usersList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view= inflater.inflate(R.layout.fragment_chats, container, false);

        setHasOptionsMenu(true);

        recyclerView=view.findViewById(R.id.recycler_view);
        editTextSearch=view.findViewById(R.id.search_user);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager=new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        fuser= FirebaseAuth.getInstance().getCurrentUser();

        usersList=new ArrayList<>();

        Query query =FirebaseDatabase.getInstance().getReference().child("ChatList").child(fuser.getUid()).orderByChild("date");
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                usersList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    ChatList chatList=snapshot.getValue(ChatList.class);
                    usersList.add(chatList);
                }
                chatList();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

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
                    }
                }

            default:
                break;
        }
        return false;
    }

    private void chatList() {
        mUsers=new ArrayList<>();
        reference=FirebaseDatabase.getInstance().getReference().child("Users");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mUsers.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    User user=snapshot.getValue(User.class);
                    for (ChatList chatlist : usersList){
                        if(user.getUid().equals(chatlist.getId())){
                            mUsers.add(user);
                        }
                    }
                }

                userAdapter=new UserAdapter(getContext(),mUsers,true);
                recyclerView.setAdapter(userAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
