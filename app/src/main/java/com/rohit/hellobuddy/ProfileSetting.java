package com.rohit.hellobuddy;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
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
import com.sinch.android.rtc.ClientRegistration;
import com.sinch.android.rtc.PushTokenRegistrationCallback;
import com.sinch.android.rtc.Sinch;
import com.sinch.android.rtc.SinchError;
import com.sinch.android.rtc.UserController;
import com.sinch.android.rtc.UserRegistrationCallback;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.rohit.hellobuddy.SinchService.APP_KEY;
import static com.rohit.hellobuddy.SinchService.APP_SECRET;
import static com.rohit.hellobuddy.SinchService.ENVIRONMENT;


public class ProfileSetting extends BaseActivity implements SinchService.StartFailedListener, PushTokenRegistrationCallback, UserRegistrationCallback {

    CircleImageView profileImage;
    ImageButton setProfileImage;
    EditText userName,userStatus;
    Button buttonSubmit;

    String currentUserID;
    FirebaseAuth mAuth;
    DatabaseReference RootRef;
    ValueEventListener listener;

    StorageReference UserProfileImagesRef;

    Toolbar toolbar;

    ProgressDialog loadingBar;
    StorageTask uploadTask;

    ProgressBar imageProgressBar;

    private long mSigningSequence = 1;
    private static final int REQUEST_ID_MULTIPLE_PERMISSIONS=1;

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
                checkForPermission();
                registerSinch();
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

        listener=new ValueEventListener() {
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

            }
        };

        RootRef.child("Users").child(currentUserID).addValueEventListener(listener);
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

                Uri resultUri=result.getUri();

                Bitmap bmp=null;
                try {
                    bmp=MediaStore.Images.Media.getBitmap(getContentResolver(),resultUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ByteArrayOutputStream baos=new ByteArrayOutputStream();

                bmp.compress(Bitmap.CompressFormat.JPEG,25,baos);
                byte[] fileInBytes=baos.toByteArray();

                loadingBar.setTitle("Set Profile Image");
                loadingBar.setMessage("Please wait, your profile is updating...");
                loadingBar.setCanceledOnTouchOutside(false);
                loadingBar.show();

                final StorageReference filePath=UserProfileImagesRef.child(currentUserID + ".jpg");

                uploadTask=filePath.putBytes(fileInBytes);
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

        String setUserName = userName.getText().toString();
        String setStatus = userStatus.getText().toString();

        if (setUserName.isEmpty()) {
            userName.setError("Field is empty");
            userName.requestFocus();
            return;
        }

        if (setStatus.isEmpty()) {
            userStatus.setError("Field is empty");
            userStatus.requestFocus();
            return;
        }

        loadingBar.setTitle("Profile Setting");
        loadingBar.setMessage("Please wait, while we are updating your profile...");
        loadingBar.setCanceledOnTouchOutside(false);
        loadingBar.show();

        SharedPreferences prefs = getSharedPreferences("Phone", MODE_PRIVATE);
        String phoneNumber = prefs.getString("number", "");

        HashMap<String, Object> profileMap = new HashMap<>();
        profileMap.put("uid", currentUserID);
        profileMap.put("name", setUserName);
        profileMap.put("status", setStatus);
        profileMap.put("phone", phoneNumber);

        RootRef.child("Users").child(currentUserID).updateChildren(profileMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    loadingBar.dismiss();
                    Toast.makeText(getApplicationContext(), "Profile updated", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(ProfileSetting.this,MainActivity.class));
                    finish();
                } else {
                    String message = task.getException().toString();
                    Log.i("updateSettings", message);
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
        RootRef.child("Users").child(currentUserID).addValueEventListener(listener);
        status("online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        RootRef.child("Users").child(currentUserID).removeEventListener(listener);
        status("offline");
    }

    @Override
    protected void onServiceConnected() {
        getSinchServiceInterface().setStartListener(this);
    }

    @Override
    public void onStartFailed(SinchError error) {
        Toast.makeText(this, error.toString(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onStarted() {
        SharedPreferences.Editor ed=getSharedPreferences("sinch_service",MODE_PRIVATE).edit();
        ed.putBoolean("isLogin",true);
        ed.apply();
        UpdateSettings();
    }

    private void startClientAndFinishActivity() {
        // start Sinch Client, it'll result onStarted() callback from where the place call activity will be started
        if (!getSinchServiceInterface().isStarted()) {
            getSinchServiceInterface().startClient();
        }
    }

    private void registerSinch() {

        if (!currentUserID.equals(getSinchServiceInterface().getUsername())){
            getSinchServiceInterface().stopClient();
        }

        getSinchServiceInterface().setUsername(currentUserID);

        UserController uc = Sinch.getUserControllerBuilder()
                .context(getApplicationContext())
                .applicationKey(APP_KEY)
                .userId(currentUserID)
                .environmentHost(ENVIRONMENT)
                .build();
        uc.registerUser(this, this);
    }

    @Override
    public void tokenRegistered() {
        startClientAndFinishActivity();
    }

    @Override
    public void tokenRegistrationFailed(SinchError sinchError) {
        loadingBar.dismiss();
        Toast.makeText(this, "Push token registration failed - incoming calls can't be received!", Toast.LENGTH_LONG).show();
    }

    // The most secure way is to obtain the signature from the backend,
    // since storing APP_SECRET in the app is not secure.
    // Following code demonstrates how the signature is obtained provided
    // the UserId and the APP_KEY and APP_SECRET.
    @Override
    public void onCredentialsRequired(ClientRegistration clientRegistration) {
        String toSign = currentUserID + APP_KEY + mSigningSequence + APP_SECRET;
        String signature;
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-1");
            byte[] hash = messageDigest.digest(toSign.getBytes("UTF-8"));
            signature = Base64.encodeToString(hash, Base64.DEFAULT).trim();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e.getCause());
        }

        clientRegistration.register(signature, mSigningSequence++);
    }

    @Override
    public void onUserRegistered() {
        // Instance is registered, but we'll wait for another callback, assuring that the push token is
        // registered as well, meaning we can receive incoming calls.
    }

    @Override
    public void onUserRegistrationFailed(SinchError sinchError) {
        loadingBar.dismiss();
        Toast.makeText(this, "Registration failed!", Toast.LENGTH_LONG).show();
    }

    private void checkForPermission() {

        String[] permission_list=new String[3];
        permission_list[0]= Manifest.permission.CAMERA;
        permission_list[1]=Manifest.permission.RECORD_AUDIO;
        permission_list[2]=Manifest.permission.READ_PHONE_STATE;

        String[] granted_permissions = new String[3];
        int index=0;
        int grant;
        for(int i=0;i<3;i++) {
            grant= ContextCompat.checkSelfPermission(getApplicationContext(),permission_list[i]);
            if (grant!= PackageManager.PERMISSION_GRANTED) {
                granted_permissions[index++]=permission_list[i];
            }
        }
        if(index!=0)
            ActivityCompat.requestPermissions(ProfileSetting.this, granted_permissions, REQUEST_ID_MULTIPLE_PERMISSIONS);
    }
}
