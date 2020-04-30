package com.rohit.hellobuddy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskExecutors;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class OtpVerificationActivity extends AppCompatActivity {

    EditText editTextOTP;
    Button buttonSubmit;
    TextView textViewResendOTP, textViewWrongNumber;

    String mVerificationId;
    PhoneAuthProvider.ForceResendingToken mResendToken;

    FirebaseAuth mAuth;

    ProgressDialog loadingBar;
    String phoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        mAuth = FirebaseAuth.getInstance();

        editTextOTP = findViewById(R.id.input_otp);
        buttonSubmit = findViewById(R.id.otp_submit);
        textViewResendOTP = findViewById(R.id.text_resend);
        textViewWrongNumber = findViewById(R.id.text_wrong_number);

        final Intent intent = getIntent();
        phoneNumber = intent.getStringExtra("mobileNumber");

        textViewWrongNumber.setText("Wrong Number ? "+phoneNumber);

        loadingBar = new ProgressDialog(OtpVerificationActivity.this);

        loadingBar.setTitle("Phone verification");
        loadingBar.setMessage("Please wait, while we are authenticating your phone...");
        loadingBar.setCanceledOnTouchOutside(false);
        loadingBar.show();

        sendVerificationCode(phoneNumber);

        buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isNetworkAvailable(OtpVerificationActivity.this)) {

                    String verificationCode = editTextOTP.getText().toString();

                    if (verificationCode.isEmpty()) {
                        editTextOTP.setError("Field is empty");
                        editTextOTP.requestFocus();
                        return;
                    }

                    loadingBar.setTitle("Verification Code");
                    loadingBar.setMessage("Please wait, while we are verifying verification code...");
                    loadingBar.setCanceledOnTouchOutside(false);
                    loadingBar.show();

                    verifyVerificationCode(verificationCode);
                }
                else{
                    Snackbar.make(v,"No internet",Snackbar.LENGTH_LONG).show();
                }
            }
        });

        textViewResendOTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isNetworkAvailable(OtpVerificationActivity.this)) {

                    textViewResendOTP.setClickable(false);
                    sendVerificationCode(phoneNumber);
                }
                else{
                    Snackbar.make(v,"No internet",Snackbar.LENGTH_LONG).show();
                }
            }
        });

        textViewWrongNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent1=new Intent(OtpVerificationActivity.this,LoginActivity.class);
                intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent1);
                finish();
            }
        });
    }

    private void sendVerificationCode(String phoneNumber) {

        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,
                60,
                TimeUnit.SECONDS,
                TaskExecutors.MAIN_THREAD,
                callbacks
        );
    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks=new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override
        public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {

            String code=phoneAuthCredential.getSmsCode();

            if (code!=null){
                editTextOTP.setText(code);
                loadingBar.setTitle("Verification Code");
                loadingBar.setMessage("Please wait, while we are verifying verification code...");
                loadingBar.setCanceledOnTouchOutside(false);
                loadingBar.show();
                verifyVerificationCode(code);
            }
        }

        @Override
        public void onVerificationFailed(@NonNull FirebaseException e) {

            loadingBar.dismiss();
            textViewResendOTP.setClickable(true);
            Toast.makeText(getApplicationContext(),"Something Sent Wrong...",Toast.LENGTH_SHORT).show();
        }

        public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token){
            super.onCodeSent(verificationId,token);

            loadingBar.dismiss();
            Toast.makeText(OtpVerificationActivity.this, "Otp sent", Toast.LENGTH_SHORT).show();
            mVerificationId=verificationId;
            mResendToken=token;

            textViewResendOTP.setClickable(false);

            new CountDownTimer(60000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    textViewResendOTP.setText("Resend in "+(millisUntilFinished/1000));
                }

                @Override
                public void onFinish() {
                    textViewResendOTP.setText("Resend");
                    textViewResendOTP.setClickable(true);
                }
            }.start();
        }
    };

    private void verifyVerificationCode(String verificationCode) {

        PhoneAuthCredential credential=PhoneAuthProvider.getCredential(mVerificationId,verificationCode);

        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential){

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        loadingBar.dismiss();
                        if(task.isSuccessful()){

                            SharedPreferences.Editor editor=getSharedPreferences("Phone",MODE_PRIVATE).edit();
                            editor.putString("number",phoneNumber);
                            editor.apply();

                            Intent intent=new Intent(getApplicationContext(),MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();

                        } else {

                            String message="Something went wrong...";

                            if(task.getException() instanceof FirebaseAuthInvalidCredentialsException){
                                message="Invalid code entered...";
                            }

                            Toast.makeText(getApplicationContext(),message,Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public static boolean isNetworkAvailable(Context context){

        if (context==null) return false;

        ConnectivityManager connectivityManager=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager!=null){

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){

                NetworkCapabilities capabilities=connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                if (capabilities!=null){
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)){
                        return true;
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){
                        return true;
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)){
                        return true;
                    }
                }
            }

            else {

                try {
                    NetworkInfo activeNetworkInfo=connectivityManager.getActiveNetworkInfo();
                    if (activeNetworkInfo!=null && activeNetworkInfo.isConnected()){
                        Log.i("update_status","Network is available : true");
                        return true;
                    }
                } catch (Exception e){
                    Log.i("update_status",""+e.getMessage());
                }
            }
        }
        Log.i("update_status","Network is available : FALSE");
        return false;
    }

}
