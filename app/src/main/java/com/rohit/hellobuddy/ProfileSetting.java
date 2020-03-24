package com.rohit.hellobuddy;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileSetting extends AppCompatActivity {

    CircleImageView profileImage;
    ImageButton setProfileImage;
    EditText userName,userStatus;
    Button buttonSubmit;

    String currentUserID;
    FirebaseAuth mAuth;
    DatabaseReference RootRef;

    StorageReference UserProfileImagesRef;

    Toolbar toolbar;

    ProgressDialog loadingBar;
    StorageTask uploadTask;

    ProgressBar imageProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_setting);

        profileImage=findViewById(R.id.profile_image);
        setProfileImage=findViewById(R.id.set_profile_button);
        userName=findViewById(R.id.input_name);
        userStatus=findViewById(R.id.input_about);
        buttonSubmit=findViewById(R.id.button_submit);
        imageProgressBar=findViewById(R.id.imageProgressBar);

        mAuth=FirebaseAuth.getInstance();
        currentUserID=mAuth.getCurrentUser().getUid();
        RootRef= FirebaseDatabase.getInstance().getReference();
        UserProfileImagesRef= FirebaseStorage.getInstance().getReference().child("Profile Images");

        toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Profile Settings");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        loadingBar=new ProgressDialog(ProfileSetting.this);

        loadProfileImage();

        buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UpdateSettings();
            }
        });

        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                StartCropActivity();
            }
        });

        setProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                StartCropActivity();
            }
        });
    }

    private void loadProfileImage() {

        imageProgressBar.setVisibility(View.VISIBLE);

        RootRef.child("Users").child(currentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if((dataSnapshot.child("image").exists())){

                    String profileImageUrl=dataSnapshot.child("image").getValue().toString();

                    if (!profileImageUrl.equals("default")) {

                        Picasso.get().load(profileImageUrl).into(profileImage, new Callback() {
                            @Override
                            public void onSuccess() {
                                imageProgressBar.setVisibility(View.GONE);
                            }

                            @Override
                            public void onError(Exception e) {
                                imageProgressBar.setVisibility(View.GONE);
                                Log.i("ProfileSettings Picasso", e.getMessage() + "");
                            }
                        });
                    }
                    else{
                        imageProgressBar.setVisibility(View.INVISIBLE);
                    }
                }
                else{
                    RootRef.child("Users").child(currentUserID).child("image").setValue("default");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.i("DatabaseError",databaseError.getMessage());
            }
        });
    }

    private void StartCropActivity() {
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(1,1)
                .start(ProfileSetting.this);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            CropImage.ActivityResult result=CropImage.getActivityResult(data);

            if (resultCode==RESULT_OK){

                loadingBar.setTitle("Set Profile Image");
                loadingBar.setMessage("Please wait, your profile is updating...");
                loadingBar.setCanceledOnTouchOutside(false);
                loadingBar.show();

                Uri resultUri=result.getUri();

                final StorageReference filePath=UserProfileImagesRef.child(currentUserID + ".jpg");

                uploadTask=filePath.putFile(resultUri);
                uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                        if (!task.isSuccessful()){
                            throw task.getException();
                        }

                        return filePath.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        loadingBar.dismiss();
                        if (task.isSuccessful()){
                            Uri downloadUri=task.getResult();
                            String downloadUrl=downloadUri.toString();

                            RootRef.child("Users").child(currentUserID).child("image")
                                    .setValue(downloadUrl)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            loadingBar.dismiss();
                                            if (task.isSuccessful()){
                                                Toast.makeText(getApplicationContext(),"Image saved",Toast.LENGTH_SHORT).show();
                                            } else {
                                                Log.i("savingImage",task.getException().toString());
                                            }
                                        }
                                    });
                        } else {
                            Log.i("referenceChild",task.getException().toString());
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        loadingBar.dismiss();
                        Log.i("uploadingImage",e.getMessage()+"");
                    }
                });
            }
        }
    }

    private void UpdateSettings() {

        String setUserName=userName.getText().toString();
        String setStatus=userStatus.getText().toString();

        if(setUserName.isEmpty()){
            userName.setError("Field is empty");
            userName.requestFocus();
            return;
        }

        if(setStatus.isEmpty()){
            userStatus.setError("Field is empty");
            userStatus.requestFocus();
            return;
        }

        SharedPreferences prefs=getSharedPreferences("Phone",MODE_PRIVATE);
        String phoneNumber=prefs.getString("number","");

        HashMap<String ,Object > profileMap=new HashMap<>();
        profileMap.put("uid",currentUserID);
        profileMap.put("name",setUserName);
        profileMap.put("status",setStatus);
        profileMap.put("phone",phoneNumber);

        RootRef.child("Users").child(currentUserID).updateChildren(profileMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Toast.makeText(getApplicationContext(),"Profile updated",Toast.LENGTH_SHORT).show();
                    Intent intent=new Intent(getApplicationContext(),MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    String message=task.getException().toString();
                    Log.i("updateSettings",message);
                }
            }
        });
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
