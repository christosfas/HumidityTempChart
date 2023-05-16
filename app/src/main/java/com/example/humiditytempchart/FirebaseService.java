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
import java.util.Locale;

public class FirebaseService extends Service {
    private final IBinder mBinder = new LocalBinder();
    List<Float> humidityList = new ArrayList<Float>();
    List<Float> tempList = new ArrayList<Float>();
    List<Float> timestampList = new ArrayList<Float>();
    private String deviceMAC = "";
    int tankFull = 0;
    DatabaseReference jsonRef;
    long ref_ts = 0;

    public FirebaseService() {
    }

    public class LocalBinder extends Binder {
        public FirebaseService getService() {
            return FirebaseService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {

//        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
//        FirebaseDatabase.getInstance().setPersistenceCacheSizeBytes(1024*1024*100);

        if(this.deviceMAC == "") this.deviceMAC = intent.getStringExtra("deviceMAC").toLowerCase(Locale.ROOT).replaceAll(":", "");

        jsonRef = FirebaseDatabase.getInstance().getReference("mac" + deviceMAC + "/output/");
        jsonRef.addValueEventListener(listener);
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        broadcastUpdate("com.example.humiditytempchart.broadcast.FIREBASE_ACTION");
        return START_STICKY;
    }

    @Override
    public boolean onUnbind(Intent intent) {

        return super.onUnbind(intent);
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        Bundle bundle = new Bundle();

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
        @Override
        public void onDataChange (@NonNull DataSnapshot DataSnapshot){

            for(int i =8; i >= 0; i--){                 //index 0 is the most recent entry
                Long ts = 1000L * DataSnapshot.child("timestamp").child(Integer.toString(i)).getValue(Long.class);
                if(timestampList.isEmpty() || timestampList.get(timestampList.size()-1).longValue() + ref_ts < ts.longValue()){
                    if(timestampList.isEmpty()) ref_ts = ts.longValue();
                    timestampList.add((float) (ts.longValue()-ref_ts));

                    Float humidity = DataSnapshot.child("humidity").child(Integer.toString(i)).getValue(Float.class);
                    humidityList.add(humidity);

                    Float temp = DataSnapshot.child("temp").child(Integer.toString(i)).getValue(Float.class);
                    tempList.add(temp);
                }
            }

            tankFull = DataSnapshot.child("tankFull/0").getValue(Integer.class);

            broadcastUpdate("com.example.humiditytempchart.broadcast.FIREBASE_ACTION");

        }

        @Override
        public void onCancelled (@NonNull DatabaseError error){


        }
    };

}