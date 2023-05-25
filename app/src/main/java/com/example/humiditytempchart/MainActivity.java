package com.example.humiditytempchart;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private FirebaseService firebaseService;
    private Intent firebaseServiceIntent;
    private DatabaseReference jsonRef;
    private ImageView upBtn;
    private ImageView downBtn;
    private EditText editTextHumidity;
    private int setHumidityThreshold = 55;
    private String deviceMAC = "";

    private BroadcastReceiver firebaseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction() == "com.example.humiditytempchart.broadcast.FIREBASE_ACTION"){
                Bundle bundle = intent.getBundleExtra("bundle");
                float[] humidityArray = bundle.getFloatArray("humidityList");
                float[] tempArray = bundle.getFloatArray("tempList");
                //float[] timestampArray = bundle.getFloatArray("timestampList");

                TextView humidityTextView = (TextView) findViewById(R.id.currentHumidityTextView);
                if(humidityArray.length > 0)humidityTextView.setText(String.valueOf(humidityArray[humidityArray.length-1]));

                TextView tempTextView = (TextView) findViewById(R.id.currentTempTextView);
                if(tempArray.length > 0)tempTextView.setText(String.valueOf(tempArray[tempArray.length-1]));

            }
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            firebaseService = ((FirebaseService.LocalBinder) iBinder).getService();

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }

        @Override
        public void onBindingDied(ComponentName name) {
            ServiceConnection.super.onBindingDied(name);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);
        deviceMAC = getIntent().getStringExtra("deviceMAC").toLowerCase(Locale.ROOT).replaceAll(":", "");

        firebaseServiceIntent = new Intent(getApplicationContext(), FirebaseService.class);
        firebaseServiceIntent.putExtra("deviceMAC", deviceMAC);
        getApplicationContext().bindService(firebaseServiceIntent, mConnection, BIND_AUTO_CREATE);
        getApplicationContext().startService(firebaseServiceIntent);
        getApplicationContext().registerReceiver(firebaseReceiver, new IntentFilter("com.example.humiditytempchart.broadcast.FIREBASE_ACTION"));

        downBtn = (ImageView) findViewById(R.id.downBtn);
        upBtn = (ImageView) findViewById(R.id.upBtn);
        editTextHumidity = (EditText) findViewById(R.id.editTextHumidity);
        editTextHumidity.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                    Log.e("MainActivity", "Action ID = " + i + " Keyevent " + (keyEvent!=null?keyEvent.toString():"is null"));
                    if(keyEvent!=null?keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER:false){
                        int value = Integer.parseInt(String.valueOf(editTextHumidity.getText()));
                        if(value < 100 && value >=20 ){
                            setHumidityThreshold = value;
                            jsonRef.child("setHumidity/0").setValue(setHumidityThreshold);
                        }
                        return true;
                    }
                return false;
            }
        });

        jsonRef = FirebaseDatabase.getInstance().getReference("mac" + deviceMAC + "/input/");
        jsonRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                setHumidityThreshold=task.getResult().child("setHumidity/0").getValue(int.class);
                editTextHumidity.setText(Integer.valueOf(setHumidityThreshold).toString());
            }
        });


        downBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(setHumidityThreshold > 20){
                    setHumidityThreshold -= 1;
                    editTextHumidity.setText(Integer.valueOf(setHumidityThreshold).toString());
                    jsonRef.child("setHumidity/0").setValue(setHumidityThreshold);
                }
            }
        });

        upBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(setHumidityThreshold <99) {
                    setHumidityThreshold += 1;
                    editTextHumidity.setText(Integer.valueOf(setHumidityThreshold).toString());
                    jsonRef.child("setHumidity/0").setValue(setHumidityThreshold);
                }
            }
        });

    }

    public void goToCharts(View view) {
        Intent intent = new Intent(MainActivity.this,ChartActivity.class);
        intent.putExtra("deviceMAC", deviceMAC);
        startActivity(intent);
    }

    public void onBackPressed(){}
}