package com.rohit.hellobuddy;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
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
import com.rohit.hellobuddy.model.User;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupProfileView extends AppCompatActivity {

    Toolbar toolbar;
    CircleImageView group_profile_image;
    ImageButton edit_group_profile_picture;
    ProgressBar progressBar;
    TextView textViewGroupName,textViewGroupStatus,textViewParticipants;
    ImageView edit_group_name,edit_group_status;

    DatabaseReference groupRef,userRef;
    StorageReference groupProfileImagesRef;

    ProgressDialog loadingBar;
    StorageTask uploadTask;

    ProgressBar imageProgressBar;

    Intent intent;
    String groupID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_profile_view);

        toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Group Profile");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        group_profile_image=findViewById(R.id.group_profile_image);
        edit_group_profile_picture=findViewById(R.id.set_group_profile_button);
        progressBar=findViewById(R.id.progressbar);
        textViewGroupName=findViewById(R.id.display_group_name);
        textViewGroupStatus=findViewById(R.id.display_group_status);
        textViewParticipants=findViewById(R.id.txt_view_participants);
        edit_group_name=findViewById(R.id.edit_group_name_button);
        edit_group_status=findViewById(R.id.edit_group_status_button);
        imageProgressBar=findViewById(R.id.progressbar);

        loadingBar=new ProgressDialog(GroupProfileView.this);

        groupRef= FirebaseDatabase.getInstance().getReference().child("Groups");
        userRef=FirebaseDatabase.getInstance().getReference().child("Users");
        groupProfileImagesRef= FirebaseStorage.getInstance().getReference().child("Group Profile Images");

        intent=getIntent();
        groupID=intent.getStringExtra("id");

        updateGroupDetails();

        edit_group_profile_picture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StartCropActivity();
            }
        });

        textViewParticipants.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent viewParticipantsIntent=new Intent(GroupProfileView.this,ViewGroupParticipants.class);
                viewParticipantsIntent.putExtra("id",groupID);
                startActivity(viewParticipantsIntent);
            }
        });

        edit_group_name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final AlertDialog dialog=new AlertDialog.Builder(GroupProfileView.this).create();
                LayoutInflater inflater=getLayoutInflater();
                View dialogView=inflater.inflate(R.layout.dialog_for_change_info,null);
                final EditText editTextInputName=dialogView.findViewById(R.id.input_name);
                Button submit_btn=dialogView.findViewById(R.id.submit_btn);
                final ProgressBar progressBar=dialogView.findViewById(R.id.progressbar);

                editTextInputName.setText(textViewGroupName.getText().toString());

                submit_btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        String name=editTextInputName.getText().toString();

                        if (name.isEmpty()){
                            editTextInputName.setError("Field can't be empty");
                            editTextInputName.requestFocus();
                            return;
                        }

                        progressBar.setVisibility(View.VISIBLE);

                        groupRef.child(groupID).child("name").setValue(name).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                progressBar.setVisibility(View.GONE);
                                if (task.isSuccessful()){
                                    dialog.dismiss();
                                    Toast.makeText(GroupProfileView.this,"Name Changed",Toast.LENGTH_SHORT).show();
                                }
                                else{
                                    Toast.makeText(GroupProfileView.this,"Something went wrong",Toast.LENGTH_SHORT).show();
                                    Log.i("nameChanged",task.getException().getMessage());
                                }
                            }
                        });
                    }
                });

                dialog.setView(dialogView);
                dialog.show();
            }
        });

        edit_group_status.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final AlertDialog dialog=new AlertDialog.Builder(GroupProfileView.this).create();
                LayoutInflater inflater=getLayoutInflater();
                View dialogView=inflater.inflate(R.layout.dialog_for_change_info,null);
                final EditText editText=dialogView.findViewById(R.id.input_name);
                Button submit_btn=dialogView.findViewById(R.id.submit_btn);
                final ProgressBar progressBar=dialogView.findViewById(R.id.progressbar);

                editText.setText(textViewGroupStatus.getText().toString());

                submit_btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        String status=editText.getText().toString();

                        if (status.isEmpty()){
                            editText.setError("Field can't be empty");
                            editText.requestFocus();
                            return;
                        }

                        progressBar.setVisibility(View.VISIBLE);

                        groupRef.child(groupID).child("status").setValue(status).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                progressBar.setVisibility(View.GONE);
                                if (task.isSuccessful()){
                                    dialog.dismiss();
                                    Toast.makeText(GroupProfileView.this,"Status Changed",Toast.LENGTH_SHORT).show();
                                }
                                else{
                                    Toast.makeText(GroupProfileView.this,"Something went wrong",Toast.LENGTH_SHORT).show();
                                    Log.i("nameChanged",task.getException().getMessage());
                                }
                            }
                        });
                    }
                });

                dialog.setView(dialogView);
                dialog.show();
            }
        });
    }

    private void StartCropActivity() {
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(1,1)
                .start(GroupProfileView.this);
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
                    bmp= MediaStore.Images.Media.getBitmap(getContentResolver(),resultUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ByteArrayOutputStream baos=new ByteArrayOutputStream();

                bmp.compress(Bitmap.CompressFormat.JPEG,25,baos);
                byte[] fileInBytes=baos.toByteArray();

                loadingBar.setTitle("Set Profile Image");
                loadingBar.setMessage("Please wait, Group icon is updating...");
                loadingBar.setCanceledOnTouchOutside(false);
                loadingBar.show();

                final StorageReference filePath=groupProfileImagesRef.child(groupID + ".jpg");

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

                            groupRef.child(groupID).child("image")
                                    .setValue(downloadUrl)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            loadingBar.dismiss();
                                            if (task.isSuccessful()){
                                                Toast.makeText(getApplicationContext(),"Icon saved",Toast.LENGTH_SHORT).show();
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

    private void updateGroupDetails() {

        groupRef.child(groupID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {

                    String groupPicture=dataSnapshot.child("image").getValue().toString();
                    String groupName=dataSnapshot.child("name").getValue().toString();
                    String groupStatus=dataSnapshot.child("status").getValue().toString();

                    if (!groupPicture.equals("default")){
                        Picasso.get().load(groupPicture).placeholder(R.drawable.group_icon).into(group_profile_image);
                    }
                    textViewGroupName.setText(groupName);
                    textViewGroupStatus.setText(groupStatus);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
