package com.example.humiditytempchart;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;
import java.util.regex.Pattern;

public class RegistrationActivity extends AppCompatActivity {

    private static final String TAG  = RegistrationActivity.class.getSimpleName();
    final String formatMAC = "%02X:%02X:%02X:%02X:%02X:%02X";
    final String[] permissions = new String[] {Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.ACCESS_FINE_LOCATION};

    private EditText emailTextView, passwordTextView, macTextView;
    private ProgressBar progressbar;
    private WifiInfo mWiFiInfo;
    WifiManager wifiManager;
    private FirebaseAuth mAuth;
    private final FirebaseFirestore dbUsers = FirebaseFirestore.getInstance();



    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), isGranted -> {
                Log.e(TAG, isGranted.toString());
                if (!isGranted.containsValue(false)) {
                    // Permission is granted. Continue the action or workflow in your
                    // app.
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // feature requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.
                    Toast.makeText(getApplicationContext(),
                                    "You need to give permissions to scan!",
                                    Toast.LENGTH_LONG)
                            .show();

                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_registration);

        // taking FirebaseAuth instance
        mAuth = FirebaseAuth.getInstance();

        // initialising all views through id defined above
        emailTextView = findViewById(R.id.email);
        passwordTextView = findViewById(R.id.passwd);
        macTextView = findViewById(R.id.macEditText);
        Button btn = findViewById(R.id.btnregister);
        Button macScanBtn = findViewById(R.id.macScanBtn);
        progressbar = findViewById(R.id.progressBarReg);
        progressbar.setVisibility(View.GONE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            wifiManager = getSystemService(WifiManager.class);
        }


        // Set on Click Listener on Registration button
        btn.setOnClickListener(v -> registerNewUser());

        macScanBtn.setOnClickListener(view -> {
            mWiFiInfo = wifiManager.getConnectionInfo();
            Intent intent = new Intent(RegistrationActivity.this, DeviceConfigActivity.class);
            intent.putExtra("wifiInfo", mWiFiInfo);
            startActivityForResult(intent, 1);
        });

        if (!(ContextCompat.checkSelfPermission(
                getApplicationContext(), Manifest.permission_group.LOCATION) ==
                PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(getApplicationContext(),Manifest.permission_group.NEARBY_DEVICES) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                getApplicationContext(), Manifest.permission.CHANGE_WIFI_STATE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                getApplicationContext(), Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED)) {
            requestPermissionLauncher.launch(permissions);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1 && resultCode == RESULT_OK) {
            assert data != null;
            String bssid = data.getStringExtra("bssid");
            macTextView.setText(bssid);
        }

    }

    private void registerNewUser()
    {

        // show the visibility of progress bar to show loading
        progressbar.setVisibility(View.VISIBLE);

        // Take the value of two edit texts in Strings
        String email, password, mac;
        email = emailTextView.getText().toString();
        password = passwordTextView.getText().toString();
        mac = macTextView.getText().toString();

        // Validations for input email and password
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
        if (TextUtils.isEmpty(mac)) {
            Toast.makeText(getApplicationContext(),
                            "Please enter mac!!",
                            Toast.LENGTH_LONG)
                    .show();
            progressbar.setVisibility(View.GONE);
            return;
        } else if(!Pattern.matches(formatMAC, mac)){
            Toast.makeText(getApplicationContext(),
                            "Valid MAC format is: XX:XX:XX:XX:XX:XX",
                            Toast.LENGTH_LONG)
                    .show();
            progressbar.setVisibility(View.GONE);
        }

        // create new user or register new user
        mAuth
                .createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {

                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task)
                    {
                        if (task.isSuccessful()) {
                            Toast.makeText(getApplicationContext(),
                                            "Registration successful!",
                                            Toast.LENGTH_LONG)
                                    .show();

                            // hide the progress bar
                            progressbar.setVisibility(View.GONE);

                            dbUsers.collection("users")
                                    .add(new User(Objects.requireNonNull(task.getResult().getUser()).getUid(), email, mac))
                                    .addOnSuccessListener(documentReference -> {
                                        Log.d(TAG, "User added with action ID: " + documentReference.getId());
                                        Toast.makeText(getApplicationContext(),
                                                        "User added to database",
                                                        Toast.LENGTH_LONG)
                                                .show();
                                    })
                                    .addOnFailureListener(e -> Log.w(TAG, "Error adding User", e));

                            // if the user created intent to login activity
                            Intent intent
                                    = new Intent(RegistrationActivity.this,
                                    MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                        else {

                            // Registration failed
                            Toast.makeText(
                                            getApplicationContext(),
                                            "Registration failed!!"
                                                    + " Please try again later",
                                            Toast.LENGTH_LONG)
                                    .show();

                            // hide the progress bar
                            progressbar.setVisibility(View.GONE);
                            Log.e(this.getClass().getSimpleName(), task.getResult().toString());
                        }
                    }
                });
    }

}
