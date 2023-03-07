package com.example.humiditytempchart;


import static java.lang.Math.abs;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import java.util.ArrayList;
import java.util.List;


public class ChartActivity extends AppCompatActivity {

    DatabaseReference jsonRef;
    private LineChart humidityChart;
    private LineChart tempChart;

    ArrayList<Entry> humidityList =  new ArrayList<Entry>();
    ArrayList<Entry> tempList =  new ArrayList<Entry>();
    List<String> formattedTimeList =  new ArrayList<String>();
    List<Float> timestampList = new ArrayList<Float>();
    ValueEventListener listener;
    long ref_ts;
    float prev_ts = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_chart);

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

        jsonRef = FirebaseDatabase.getInstance().getReference("test/json/");
        jsonRef.addValueEventListener(listener = new ValueEventListener() {
            int count = 9;

            @Override
            public void onDataChange(@NonNull DataSnapshot DataSnapshot) {

                if(formattedTimeList.isEmpty()){

                    for (int i = 8; i >= 0; i--) {

                        String ts = DataSnapshot.child("timestamp").child(Integer.toString(i)).getValue(String.class);
                        formattedTimeList.add(ts);

                        Integer parts[] = new Integer[3];

                        for(int j = 0; j < 3; j++ ){

                            parts[j]=Integer.parseInt(ts.split(":")[j]);
                        }

                        if(i == 8) {

                            ref_ts = (parts[0] * 3600 + parts[1] * 60 + parts[2]);
                            ValueFormatter myAxisValueFormatter = new MyXAxisValueFormatter(/**timestampList,formattedTimeList**/ref_ts);
                            tempXAxis.setValueFormatter(myAxisValueFormatter);
                            humidityXAxis.setValueFormatter(myAxisValueFormatter);

                        }
                        float val = Float.valueOf(parts[0]*3600 + parts[1]*60 + parts[2]) -ref_ts;
                        if (prev_ts > val) val += (3600*24); //add a day at 00:00
                        timestampList.add(val);
                        prev_ts = val;


                    }
                }

                else{

                    String ts = DataSnapshot.child("timestamp/0").getValue(String.class);
                    formattedTimeList.add(ts);

                    Integer parts[] = new Integer[3];

                    for(int j = 0; j < 3; j++ ){

                        parts[j]=Integer.parseInt(ts.split(":")[j]);
                    }
                    float val = Float.valueOf(parts[0]*3600 + parts[1]*60 + parts[2])-ref_ts;
                    if (prev_ts > val) val += 3600*24; //add a day at 00:00
                    timestampList.add(val);
                    prev_ts = val;


                }

                 if(humidityList.isEmpty()){

                    for (int i = 8; i >= 0; i--) {

                        Float humidity = DataSnapshot.child("humidity").child(Integer.toString(i)).getValue(Float.class);
                        humidityList.add(new Entry(timestampList.get(8-i), humidity));

                    }
                }

                else{

                    Float humidity = DataSnapshot.child("humidity/0").getValue(Float.class);
                    humidityList.add(new Entry(timestampList.get(count), humidity));

                }

                if(tempList.isEmpty()){
                    for (int i = 8; i >= 0; i--) {

                        Float temp = DataSnapshot.child("temp").child(Integer.toString(i)).getValue(Float.class);
                        tempList.add(new Entry(timestampList.get(8-i), temp));

                    }
                }

                else{

                    Float temp = DataSnapshot.child("temp/0").getValue(Float.class);
                    tempList.add(new Entry(timestampList.get(count), temp));
                    count++;

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

            @Override
            public void onCancelled(@NonNull DatabaseError error) {



            }
        });

    }

    public void goBack(View view) {
        Intent intent = new Intent(ChartActivity.this,MainActivity.class);
        startActivity(intent);
    }

}