package com.rohit.hellobuddy;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    EditText editTextCountryCode,editTextNumber;
    Button buttonNext;

    FirebaseUser firebaseUser;

    @Override
    protected void onStart() {
        super.onStart();

        firebaseUser= FirebaseAuth.getInstance().getCurrentUser();

        //check if user is null
        if(firebaseUser!=null){
            Intent intent=new Intent(getApplicationContext(),MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editTextCountryCode=findViewById(R.id.input_country_code);
        editTextNumber=findViewById(R.id.input_number);
        buttonNext=findViewById(R.id.login_next);

        buttonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isNetworkAvailable(LoginActivity.this)) {

                    String countryCode = editTextCountryCode.getText().toString();
                    String number = editTextNumber.getText().toString();

                    if (countryCode.isEmpty()) {
                        editTextCountryCode.setError("Field is empty");
                        editTextCountryCode.requestFocus();
                        return;
                    }

                    if (number.isEmpty()) {
                        editTextNumber.setError("Field is empty");
                        editTextNumber.requestFocus();
                        return;
                    }

                    if (number.length() != 10) {
                        editTextNumber.setError("Invalid Number");
                        editTextNumber.requestFocus();
                        return;
                    }

                    if (countryCode.length() != 3) {
                        editTextCountryCode.setError("Invalid Code");
                        editTextCountryCode.requestFocus();
                        return;
                    }

                    String mobileNumber = countryCode + number;

                    Intent intent = new Intent(LoginActivity.this, OtpVerificationActivity.class);
                    intent.putExtra("mobileNumber", mobileNumber);
                    startActivity(intent);
                }
                else{
                    Snackbar.make(v,"No internet",Snackbar.LENGTH_LONG).show();
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
