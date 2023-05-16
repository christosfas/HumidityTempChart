package com.example.humiditytempchart;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.content.Intent;
import android.os.PatternMatcher;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.Button;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.AuthResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class RegistrationActivity extends AppCompatActivity {

    private static final String TAG  = RegistrationActivity.class.getSimpleName();
    final String formatMAC = "%02X:%02X:%02X:%02X:%02X:%02X";
    final String[] permissions = new String[] {Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.ACCESS_FINE_LOCATION};

    private EditText emailTextView, passwordTextView, macTextView;
    private Button Btn, macScanBtn;
    private ProgressBar progressbar;
    private ListView scanResultView;
    WifiManager wifiManager;
    BroadcastReceiver wifiScanReceiver;
    private FirebaseAuth mAuth;
    private FirebaseFirestore dbUsers = FirebaseFirestore.getInstance();

    private void scanFailure() {
        Toast.makeText(getApplicationContext(),
                        "Scan failed!",
                        Toast.LENGTH_LONG)
                .show();
    }

    private void scanSuccess() {
        List<ScanResult> scanResults = wifiManager.getScanResults();
        List<String> scanResultStringList = new ArrayList<>();
        for (ScanResult result : scanResults) scanResultStringList.add(result.SSID + " \n" + "RSSI: " + result.level + " MAC: " + result.BSSID);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, scanResultStringList);
        scanResultView.setAdapter(adapter);
        scanResultView.setVisibility(View.VISIBLE);
        Log.i(TAG, scanResults.toString());
        scanResultView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String selectedSSID = adapterView.getItemAtPosition(i).toString().split(" ")[0];
                Log.e(TAG, selectedSSID);
                for (ScanResult result : scanResults){
                    if(result.SSID.equals(selectedSSID)){
                        //Log.e(TAG, "Found it!");
                        macTextView.setText(result.BSSID);
                        scanResultView.setVisibility(View.GONE);
                    }
                }
            }
        });
    }

    private ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), isGranted -> {
                Log.e(TAG, isGranted.toString());
                if (!isGranted.values().contains(false)) {
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
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_registration);

        // taking FirebaseAuth instance
        mAuth = FirebaseAuth.getInstance();

        // initialising all views through id defined above
        emailTextView = findViewById(R.id.email);
        passwordTextView = findViewById(R.id.passwd);
        macTextView = findViewById(R.id.macEditText);
        Btn = findViewById(R.id.btnregister);
        macScanBtn = findViewById(R.id.macScanBtn);
        progressbar = (ProgressBar) findViewById(R.id.progressBarReg);
        progressbar.setVisibility(View.GONE);
        scanResultView = (ListView) findViewById(R.id.scanResultView);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiScanReceiver = new WiFiReceiver();
        registerReceiver(wifiScanReceiver, new IntentFilter("android.net.wifi.SCAN_RESULTS"));



        // Set on Click Listener on Registration button
        Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                registerNewUser();
            }
        });

        macScanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(
                        getApplicationContext(), Manifest.permission_group.LOCATION) ==
                        PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(getApplicationContext(),Manifest.permission_group.NEARBY_DEVICES) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                        getApplicationContext(), Manifest.permission.CHANGE_WIFI_STATE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                        getApplicationContext(), Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED) {

                } else {
                    // You can directly ask for the permission.
                    // The registered ActivityResultCallback gets the result of this request.
                    requestPermissionLauncher.launch(permissions);
                }
                boolean startScan = wifiManager.startScan();
                Log.e(TAG, "Scan successful: " + startScan);
            }
        });
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
            return;
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(getApplicationContext(),
                            "Please enter password!!",
                            Toast.LENGTH_LONG)
                    .show();
            return;
        }
        if (TextUtils.isEmpty(mac)) {
            Toast.makeText(getApplicationContext(),
                            "Please enter mac!!",
                            Toast.LENGTH_LONG)
                    .show();
            return;
        } else if(!Pattern.matches(formatMAC, mac)){
            Toast.makeText(getApplicationContext(),
                            "Valid MAC format is: XX:XX:XX:XX:XX:XX",
                            Toast.LENGTH_LONG)
                    .show();
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
                                    .add(new User(task.getResult().getUser().getUid(), email, mac))
                                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                        @Override
                                        public void onSuccess(DocumentReference documentReference) {
                                            Log.d(TAG, "User added with action ID: " + documentReference.getId());
                                            Toast.makeText(getApplicationContext(),
                                                            "User added to database",
                                                            Toast.LENGTH_LONG)
                                                    .show();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.w(TAG, "Error adding User", e);
                                        }
                                    });

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

    class WiFiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context c, Intent intent) {
            boolean success = intent.getBooleanExtra(
                    WifiManager.EXTRA_RESULTS_UPDATED, false);
            if (success) {
                scanSuccess();
            } else {
                // scan failure handling
                scanFailure();
            }
        }
    }
}
