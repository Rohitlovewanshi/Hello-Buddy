package com.rohit.hellobuddy;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rohit.hellobuddy.Fragments.BuddiesFragment;
import com.rohit.hellobuddy.Fragments.ChatsFragment;
import com.rohit.hellobuddy.Fragments.GroupsFragment;
import com.rohit.hellobuddy.model.User;
import com.squareup.picasso.Picasso;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ID_MULTIPLE_PERMISSIONS=1;

    CircleImageView profile_image;
    TextView username;

    FirebaseUser currentUser;
    private FirebaseAuth mAuth;

    DatabaseReference rootRef,buddiesRef;

    String currentUserID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth=FirebaseAuth.getInstance();
        currentUser=mAuth.getCurrentUser();
        currentUserID=mAuth.getCurrentUser().getUid();
        rootRef= FirebaseDatabase.getInstance().getReference();
        buddiesRef=FirebaseDatabase.getInstance().getReference().child("Buddies").child(currentUserID);

        Toolbar toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");

        profile_image=findViewById(R.id.profile_image);
        username=findViewById(R.id.username);

        rootRef.child("Users").child(currentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.hasChild("name")) {
                    username.setText(dataSnapshot.child("name").getValue().toString());
                }

                if (dataSnapshot.hasChild("image") && !dataSnapshot.child("image").getValue().toString().equals("default")){
                    Picasso.get().load(dataSnapshot.child("image").getValue().toString()).placeholder(R.drawable.ic_account_circle).into(profile_image);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        profile_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent=new Intent(MainActivity.this,ProfileView.class);
                intent.putExtra("userID",currentUserID);
                startActivity(intent);
            }
        });

        username.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent=new Intent(MainActivity.this,ProfileView.class);
                intent.putExtra("userID",currentUserID);
                startActivity(intent);
            }
        });

        TabLayout tabLayout=findViewById(R.id.tab_layout);
        ViewPager viewPager=findViewById(R.id.view_paper);

        ViewPagerAdapter viewPagerAdapter=new ViewPagerAdapter(getSupportFragmentManager());
        viewPagerAdapter.addFragment(new ChatsFragment(),"Chats");
        viewPagerAdapter.addFragment(new BuddiesFragment(),"Buddies");
        viewPagerAdapter.addFragment(new GroupsFragment(),"Groups");

        viewPager.setAdapter(viewPagerAdapter);

        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (currentUser!=null){
            VerifyUserExistance();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){

            case R.id.logout:
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(getApplicationContext(),LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                return true;

            case R.id.sync_buddies:
                String permission_contact= Manifest.permission.READ_CONTACTS;
                int grant_contact= ContextCompat.checkSelfPermission(getApplicationContext(),permission_contact);
                if (grant_contact!= PackageManager.PERMISSION_GRANTED){
                    String[] permission_list=new String[1];
                    permission_list[0]=permission_contact;
                    ActivityCompat.requestPermissions(MainActivity.this,permission_list,REQUEST_ID_MULTIPLE_PERMISSIONS);
                    Toast.makeText(getApplicationContext(),"Click again to sync after giving permission",Toast.LENGTH_SHORT).show();
                }
                else{
                    new LongOperation().execute("");
                }

        }

        return false;
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {

        private ArrayList<Fragment> fragments;
        private ArrayList<String>titles;

        ViewPagerAdapter(FragmentManager fm){
            super(fm);
            this.fragments=new ArrayList<>();
            this.titles=new ArrayList<>();
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        public void addFragment(Fragment fragment,String title){
            fragments.add(fragment);
            titles.add(title);
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return titles.get(position);
        }
    }

    private void VerifyUserExistance() {

        String currentUserID= Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        rootRef.child("Users").child(currentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(!(dataSnapshot.child("name").exists())){
                    Intent intent=new Intent(getApplicationContext(),ProfileSetting.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private class LongOperation extends AsyncTask<String,Void,String >{

        ProgressDialog progress;

        @Override
        protected void onPreExecute() {
            progress=new ProgressDialog(MainActivity.this);
            progress.setMessage("Loading...");
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.setIndeterminate(true);
            progress.setProgress(0);
            progress.show();
        }

        @Override
        protected String doInBackground(String... strings) {

            final HashMap<String, String> hashMap = new HashMap<>();

            String[] projection = new String[]{
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
            };

            Cursor cursor = null;
            try {
                cursor = getApplicationContext().getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, null, null, null);
            } catch (SecurityException e1) {
                Log.i("ContactError", e1.getMessage() + "");
            }

            if (cursor != null) {
                try {
                    HashSet<String> normalizedNumbersAlreadyFound = new HashSet<>();
                    int indexOfNormalizedNumber = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER);
                    int indexOfDisplayNumber = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

                    while (cursor.moveToNext()) {
                        String normalizedNumber = cursor.getString(indexOfNormalizedNumber);
                        if (normalizedNumbersAlreadyFound.add(normalizedNumber)) {

                            final String displayNumber = cursor.getString(indexOfDisplayNumber);

                            String phoneNumber = "";
                            for (int i = 0; i < displayNumber.length(); i++) {
                                if (i == 0) {

                                    if (displayNumber.charAt(i) != '+') {
                                        phoneNumber += "+91"+displayNumber.charAt(i);
                                    } else {
                                        phoneNumber += "+";
                                    }
                                } else {
                                    if (displayNumber.charAt(i) != ' ') {
                                        phoneNumber += displayNumber.charAt(i);
                                    }
                                }
                            }
                            hashMap.put(phoneNumber, "value");
                        }

                    }
                } finally {
                    cursor.close();
                }
            }


            SharedPreferences pref=getSharedPreferences("Phone",MODE_PRIVATE);
            final String currentUserPhoneNumber=pref.getString("number","");

            rootRef.child("Users").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                        User user = snapshot.getValue(User.class);

                        if (hashMap.containsKey(user.getPhone())) {
                            if (!currentUserPhoneNumber.equals(user.getPhone())) {
                                buddiesRef.child(user.getUid()).child("NameForSearch").setValue(user.getName().toLowerCase());
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            progress.dismiss();
        }
    }

    private void status(String status){
        DatabaseReference reference=FirebaseDatabase.getInstance().getReference("Users").child(currentUserID);

        SimpleDateFormat sdf=new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String currentDateTime=sdf.format(new Date());

        HashMap<String ,Object>hashMap=new HashMap<>();
        hashMap.put("currentStatus",status);
        hashMap.put("lastSeenDate",currentDateTime);

        reference.updateChildren(hashMap);
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
}
