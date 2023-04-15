package com.example.humiditytempchart;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.NonNull;

import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FirebaseService extends Service {
    private final IBinder mBinder = new LocalBinder();
    List<Float> humidityList = new ArrayList<Float>();
    List<Float> tempList = new ArrayList<Float>();
    List<String> formattedTimeList =  new ArrayList<String>();
    List<Float> timestampList = new ArrayList<Float>();
    int tankFull = 0;
    DatabaseReference jsonRef;
    long ref_ts;
    float prev_ts = 0;

    public FirebaseService() {
    }

    public class LocalBinder extends Binder {
        public FirebaseService getService() {
            return FirebaseService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {

        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        FirebaseDatabase.getInstance().setPersistenceCacheSizeBytes(1024*1024*100);
        jsonRef = FirebaseDatabase.getInstance().getReference("test/json/");
        jsonRef.addValueEventListener(listener);
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        broadcastUpdate("com.example.humiditytempchart.broadcast.FIREBASE_ACTION");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        return super.onUnbind(intent);
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        Bundle bundle = new Bundle();
        bundle.putStringArrayList("formatedTimeList", (ArrayList<String>) formattedTimeList);

        Float[] WrapperTimestampArray = timestampList.toArray(new Float[timestampList.size()]);
        float[] array = new float[WrapperTimestampArray.length];
        int index = 0;
        for(final Float value : WrapperTimestampArray ){
            array[index++] = value;
        }
        bundle.putFloatArray("timestampList", array);

        WrapperTimestampArray = humidityList.toArray(new Float[humidityList.size()]);
        index = 0;
        array = new float[WrapperTimestampArray.length];
        for(final Float value : WrapperTimestampArray ){
            array[index++] = value;
        }
        bundle.putFloatArray("humidityList", array);

        WrapperTimestampArray = tempList.toArray(new Float[tempList.size()]);
        index = 0;
        array = new float[WrapperTimestampArray.length];
        for(final Float value : WrapperTimestampArray ){
            array[index++] = value;
        }
        bundle.putFloatArray("tempList", array);

        bundle.putLong("ref_ts", ref_ts);
        bundle.putInt("tankFull", tankFull);
        intent.putExtra( "bundle", bundle);
        sendBroadcast(intent);
    }

    ValueEventListener listener = new ValueEventListener() {
        int count = 9;

        @Override
        public void onDataChange (@NonNull DataSnapshot DataSnapshot){

            if (formattedTimeList.isEmpty()) {

                for (int i = 8; i >= 0; i--) {

                    String ts = DataSnapshot.child("timestamp").child(Integer.toString(i)).getValue(String.class);
                    formattedTimeList.add(ts);

                    Integer parts[] = new Integer[3];

                    for (int j = 0; j < 3; j++) {

                        parts[j] = Integer.parseInt(ts.split(":")[j]);
                    }

                    if (i == 8) {

                        ref_ts = (parts[0] * 3600 + parts[1] * 60 + parts[2]);


                    }
                    float val = Float.valueOf(parts[0] * 3600 + parts[1] * 60 + parts[2]) - ref_ts;
                    if (prev_ts > val) val += (3600 * 24); //add a day at 00:00
                    timestampList.add(val);
                    prev_ts = val;


                }
            } else {

                String ts = DataSnapshot.child("timestamp/0").getValue(String.class);
                formattedTimeList.add(ts);

                Integer parts[] = new Integer[3];

                for (int j = 0; j < 3; j++) {

                    parts[j] = Integer.parseInt(ts.split(":")[j]);
                }
                float val = Float.valueOf(parts[0] * 3600 + parts[1] * 60 + parts[2]) - ref_ts;
                if (prev_ts > val) val += 3600 * 24; //add a day at 00:00
                timestampList.add(val);
                prev_ts = val;


            }

            if (humidityList.isEmpty()) {

                for (int i = 8; i >= 0; i--) {

                    Float humidity = DataSnapshot.child("humidity").child(Integer.toString(i)).getValue(Float.class);
                    humidityList.add(humidity);


                }
            } else {

                Float humidity = DataSnapshot.child("humidity/0").getValue(Float.class);
                humidityList.add(humidity);

            }

            if (tempList.isEmpty()) {
                for (int i = 8; i >= 0; i--) {

                    Float temp = DataSnapshot.child("temp").child(Integer.toString(i)).getValue(Float.class);
                    tempList.add(temp);


                }
            } else {

                Float temp = DataSnapshot.child("temp/0").getValue(Float.class);
                tempList.add(temp);
                count++;

            }

            tankFull = DataSnapshot.child("tankFull/0").getValue(Integer.class);

            broadcastUpdate("com.example.humiditytempchart.broadcast.FIREBASE_ACTION");

        }

        @Override
        public void onCancelled (@NonNull DatabaseError error){


        }
    };

}