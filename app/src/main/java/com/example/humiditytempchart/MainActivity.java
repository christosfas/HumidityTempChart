package com.example.humiditytempchart;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.TextView;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

public class MainActivity extends AppCompatActivity {

    private FirebaseService firebaseService;
    private Intent firebaseServiceIntent;
    private BroadcastReceiver firebaseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction() == "com.example.humiditytempchart.broadcast.FIREBASE_ACTION"){
                Bundle bundle = intent.getBundleExtra("bundle");
                float[] humidityArray = bundle.getFloatArray("humidityList");
                float[] tempArray = bundle.getFloatArray("tempList");
                float[] timestampArray = bundle.getFloatArray("timestampList");

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
        setContentView(R.layout.activity_main);

        firebaseServiceIntent = new Intent(getApplicationContext(), FirebaseService.class);
        getApplicationContext().bindService(firebaseServiceIntent, mConnection, BIND_AUTO_CREATE);
        getApplicationContext().startService(firebaseServiceIntent);
        getApplicationContext().registerReceiver(firebaseReceiver, new IntentFilter("com.example.humiditytempchart.broadcast.FIREBASE_ACTION"));
    }

    public void goToCharts(View view) {
        Intent intent = new Intent(MainActivity.this,ChartActivity.class);
        startActivity(intent);
    }

    public void onBackPressed(){}
}