package com.example.humiditytempchart;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import com.example.humiditytempchart.FirebaseService;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.Window;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;


public class ChartActivity extends AppCompatActivity {

    private LineChart humidityChart;
    private LineChart tempChart;
    ArrayList<Entry> humidityList =  new ArrayList<Entry>();
    ArrayList<Entry> tempList =  new ArrayList<Entry>();
    List<Float> timestampList = new ArrayList<Float>();
    long ref_ts = 0;
    private FirebaseService firebaseService;
    private Intent firebaseServiceIntent;
    private BroadcastReceiver firebaseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction() == "com.example.humiditytempchart.broadcast.FIREBASE_ACTION"){
                Bundle bundle = intent.getBundleExtra("bundle");
                if(humidityList.isEmpty() && tempList.isEmpty() && timestampList.isEmpty() && ref_ts == 0){
                    ref_ts = bundle.getLong("ref_ts");
                    float[] humidityArray = bundle.getFloatArray("humidityList");
                    float[] tempArray = bundle.getFloatArray("tempList");
                    float[] timestampArray = bundle.getFloatArray("timestampList");
                    for(int i =0; i < timestampArray.length; i++ ){
                        humidityList.add(new Entry(timestampArray[i], humidityArray[i]));
                        tempList.add(new Entry(timestampArray[i], tempArray[i]));
                        ValueFormatter myAxisValueFormatter = new MyXAxisValueFormatter(/**timestampList,formattedTimeList**/ref_ts);
                        humidityChart.getXAxis().setValueFormatter(myAxisValueFormatter);
                        tempChart.getXAxis().setValueFormatter(myAxisValueFormatter);
                    }
                }else{
                    float[] humidityArray = bundle.getFloatArray("humidityList");
                    float[] tempArray = bundle.getFloatArray("tempList");
                    float[] timestampArray = bundle.getFloatArray("timestampList");
                    humidityList.add(new Entry(timestampArray[timestampArray.length-1], humidityArray[humidityArray.length-1]));
                    tempList.add(new Entry(timestampArray[timestampArray.length-1], tempArray[tempArray.length-1]));
                }

                LineDataSet humidityDataSet = new LineDataSet(humidityList,"% humidity");
                humidityDataSet.setDrawFilled(true);
                humidityDataSet.setFillColor(Color.parseColor("#03A9F4"));
                humidityDataSet.setLineWidth(3f);
                humidityDataSet.setColor(Color.parseColor("#03A9F4"));
                humidityDataSet.setCircleColor(Color.BLUE);
                humidityDataSet.setValueTextSize(10f);
                humidityDataSet.setValueTextColor(Color.WHITE);
                //humidityDataSet.enableDashedLine(8,8,0);
                LineData humidityData = new LineData(humidityDataSet);
                humidityChart.setData(humidityData);
                humidityChart.notifyDataSetChanged();
                humidityChart.invalidate();

                LineDataSet tempDataSet = new LineDataSet(tempList,"*C");
                tempDataSet.setDrawFilled(true);
                tempDataSet.setFillColor(Color.RED);
                tempDataSet.setLineWidth(3f);
                tempDataSet.setColor(Color.RED);
                tempDataSet.setCircleColor(Color.RED);
                tempDataSet.setValueTextSize(10f);
                tempDataSet.setValueTextColor(Color.WHITE);
                //tempDataSet.enableDashedLine(8,8,0);
                LineData tempData = new LineData(tempDataSet);
                tempChart.setData(tempData);
                tempChart.notifyDataSetChanged();
                tempChart.invalidate();

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
        setContentView(R.layout.activity_chart);

        firebaseServiceIntent = new Intent(getApplicationContext(), FirebaseService.class);
        getApplicationContext().bindService(firebaseServiceIntent, mConnection, BIND_AUTO_CREATE);
        getApplicationContext().startService(firebaseServiceIntent);
        getApplicationContext().registerReceiver(firebaseReceiver, new IntentFilter("com.example.humiditytempchart.broadcast.FIREBASE_ACTION"));

        humidityChart = (LineChart)findViewById(R.id.humidityChart);
        YAxis humidityYAxis = humidityChart.getAxisLeft();
        humidityYAxis.setAxisMinimum(0f);
        humidityYAxis.setAxisMaximum(100f);
        humidityYAxis.setAxisLineColor(Color.WHITE);
        humidityYAxis.setGridColor(Color.WHITE);
        humidityYAxis.setTextColor(Color.WHITE);
        humidityYAxis.enableGridDashedLine(8,8,0);
        humidityYAxis = humidityChart.getAxisRight();
        humidityYAxis.setAxisMinimum(0f);
        humidityYAxis.setAxisMaximum(100f);
        humidityYAxis.setAxisLineColor(Color.WHITE);
        humidityYAxis.setGridColor(Color.WHITE);
        humidityYAxis.setTextColor(Color.WHITE);
        humidityYAxis.enableGridDashedLine(8,8,0);
        XAxis humidityXAxis = humidityChart.getXAxis();
        humidityXAxis.setLabelRotationAngle(-45);
        humidityXAxis.setAxisLineColor(Color.WHITE);
        humidityXAxis.setGridColor(Color.WHITE);
        humidityXAxis.setTextColor(Color.WHITE);
        humidityXAxis.enableGridDashedLine(8,8,0);
        humidityChart.setNoDataTextColor(Color.YELLOW);
        humidityChart.getLegend().setTextColor(Color.WHITE);
        humidityChart.getDescription().setTextColor(Color.WHITE);
        humidityChart.getDescription().setText("Humidity Measurements Curve");

        tempChart = (LineChart)findViewById(R.id.tempChart);
        YAxis tempYAxis = tempChart.getAxisLeft();
        tempYAxis.setAxisMinimum(10f);
        tempYAxis.setAxisMaximum(45f);
        tempYAxis.setAxisLineColor(Color.WHITE);
        tempYAxis.setGridColor(Color.WHITE);
        tempYAxis.setTextColor(Color.WHITE);
        tempYAxis.enableGridDashedLine(8,8,0);
        tempYAxis = tempChart.getAxisRight();
        tempYAxis.setAxisMinimum(10f);
        tempYAxis.setAxisMaximum(45f);
        tempYAxis.setAxisLineColor(Color.WHITE);
        tempYAxis.setGridColor(Color.WHITE);
        tempYAxis.setTextColor(Color.WHITE);
        tempYAxis.enableGridDashedLine(8,8,0);
        XAxis tempXAxis = tempChart.getXAxis();
        tempXAxis.setAxisLineColor(Color.WHITE);
        tempXAxis.setLabelRotationAngle(-45);
        tempXAxis.setGridColor(Color.WHITE);
        tempXAxis.setTextColor(Color.WHITE);
        tempXAxis.enableGridDashedLine(8,8,0);
        tempChart.setNoDataTextColor(Color.YELLOW);
        tempChart.getLegend().setTextColor(Color.WHITE);
        tempChart.getDescription().setTextColor(Color.WHITE);
        tempChart.getDescription().setText("Temperature Measurements Curve");

    }

    public void goBack(View view) {
        Intent intent = new Intent(ChartActivity.this,MainActivity.class);
        startActivity(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}