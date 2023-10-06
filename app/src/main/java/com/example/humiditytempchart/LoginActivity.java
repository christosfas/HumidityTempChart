package com.example.humiditytempchart;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Button;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.AuthResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class LoginActivity extends AppCompatActivity {

    private EditText emailTextView, passwordTextView;
    private ProgressBar progressbar;
    private TextView loginTxt;

    private FirebaseAuth mAuth;
    private final FirebaseFirestore dbUsers = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_login);
        // taking instance of FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        // initialising all views through id defined above
        emailTextView = findViewById(R.id.email);
        passwordTextView = findViewById(R.id.password);
        Button btn = findViewById(R.id.login);
        progressbar = findViewById(R.id.progressBarLogIn);
        TextInputLayout txtLayout = findViewById(R.id.apPasswordLayoutLogin);
        loginTxt = findViewById(R.id.loginTXT);
        TextView goToRegisterTxt = findViewById(R.id.textView3);

        // Set on Click Listener on Sign-in button
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                loginUserAccount();
            }
        });

        //check for saved login credentials
        SharedPreferences sp1=this.getSharedPreferences("Login", MODE_PRIVATE);
        if(sp1!=null) {
            emailTextView.setText(sp1.getString("Unm", null));
            passwordTextView.setText(sp1.getString("Psw", null));
            if(!(TextUtils.isEmpty(emailTextView.getText()) || TextUtils.isEmpty(passwordTextView.getText()))) loginUserAccount();
        }
    }

    private void loginUserAccount()
    {
        progressbar.setVisibility(View.VISIBLE);
        SharedPreferences sp=getSharedPreferences("Login", MODE_PRIVATE);
        SharedPreferences.Editor Ed=sp.edit();

        // Take the value of two edit texts in Strings
        String email, password;
        email = emailTextView.getText().toString();
        password = passwordTextView.getText().toString();

        // validations for input email and password
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(getApplicationContext(),
                            "Please enter email!!",
                            Toast.LENGTH_LONG)
                    .show();
            progressbar.setVisibility(View.GONE);
            return;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(getApplicationContext(),
                            "Please enter password!!",
                            Toast.LENGTH_LONG)
                    .show();
            progressbar.setVisibility(View.GONE);
            return;
        }

        // signin existing user
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(
                        new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(
                                    @NonNull Task<AuthResult> task)
                            {
                                if (task.isSuccessful()) {
                                    Toast.makeText(getApplicationContext(),
                                                    "Login successful!!",
                                                    Toast.LENGTH_LONG)
                                            .show();


                                    Ed.putString("Unm",email ); //write user to shared preferences
                                    Ed.putString("Psw",password);
                                    Ed.apply();

                                    //find user from firestore to get associated mac address / device
                                    Query firestoreUserQuery = dbUsers.collection("users").whereEqualTo("email", email);
                                    firestoreUserQuery.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {

                                            if(task.isSuccessful()){

                                                User loggedInUser = task.getResult().getDocuments().get(0).toObject(User.class); //get user entry from firestore to associate with device MAC
                                                assert loggedInUser != null;
                                                Log.e(this.getClass().getSimpleName(),loggedInUser.mac.get(0));

                                                // if sign-in is successful
                                                // intent to home activity
                                                Intent intent
                                                        = new Intent(LoginActivity.this,
                                                        MainActivity.class);
                                                intent.putExtra("deviceMAC", loggedInUser.mac.get(0)); //every user can have multiple devices assigned so mac attr. is list
                                                progressbar.setVisibility(View.GONE);
                                                startActivity(intent);
                                                finish();

                                            }else{
                                                progressbar.setVisibility(View.GONE);
                                                Toast.makeText(getApplicationContext(), "User association with device not found in database", Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    });

                                }

                                else {

                                    // sign-in failed
                                    Toast.makeText(getApplicationContext(),
                                                    "Login failed!!",
                                                    Toast.LENGTH_LONG)
                                            .show();

                                    // hide the progress bar
                                    progressbar.setVisibility(View.GONE);
                                }
                            }
                        });
    }

    public void goToRegister(android.view.View View){

        Intent intent
                = new Intent(LoginActivity.this,
                RegistrationActivity.class);
        startActivity(intent);

    }
}