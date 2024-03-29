package com.example.humiditytempchart;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

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
    List<Float> humidityList = new ArrayList<>();
    List<Float> tempList = new ArrayList<>();
    List<Float> timestampList = new ArrayList<>();
    private String deviceMAC = "";
    int tankFull = 0;
    DatabaseReference jsonRef;
    long ref_ts = 0;
    NotificationManagerCompat notificationManager;
    NotificationCompat.Builder tankNotificationBuilder;
    private Notification mNotification = null;
    private static final String CHANNEL_ID = "FirebaseNotificationChannel";

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

        if(this.deviceMAC.equals("")) this.deviceMAC = intent.getStringExtra("deviceMAC").toLowerCase(Locale.ROOT).replaceAll(":", "");
        notificationManager = NotificationManagerCompat.from(this);
        createNotificationChannel();

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

        Float[] WrapperTimestampArray = timestampList.toArray(new Float[0]);
        float[] array = new float[WrapperTimestampArray.length];
        int index = 0;
        for(final Float value : WrapperTimestampArray ){
            array[index++] = value;
        }
        bundle.putFloatArray("timestampList", array);

        WrapperTimestampArray = humidityList.toArray(new Float[0]);
        index = 0;
        array = new float[WrapperTimestampArray.length];
        for(final Float value : WrapperTimestampArray ){
            array[index++] = value;
        }
        bundle.putFloatArray("humidityList", array);

        WrapperTimestampArray = tempList.toArray(new Float[0]);
        index = 0;
        array = new float[WrapperTimestampArray.length];
        for(final Float value : WrapperTimestampArray ){
            array[index++] = value;
        }
        bundle.putFloatArray("tempList", array);
        bundle.putLong("ref_ts", ref_ts);
        bundle.putInt("tankFull", tankFull);
        if(tankFull == 1 && mNotification == null){
            mNotification = tankNotificationBuilder.build();
            notificationManager.notify(0, mNotification);
        }else if(tankFull == 0){
            notificationManager.cancel(0);
            mNotification = null;
        }
        intent.putExtra( "bundle", bundle);
        sendBroadcast(intent);
    }

    ValueEventListener listener = new ValueEventListener() {
        @Override
        public void onDataChange (@NonNull DataSnapshot DataSnapshot){

            for(int i = (int) DataSnapshot.child("timestamp").getChildrenCount()-1; i >= 0; i--){                 //index 0 is the most recent entry
                long ts = 1000L * DataSnapshot.child("timestamp").child(Integer.toString(i)).getValue(Long.class);
                if(timestampList.isEmpty() || timestampList.get(timestampList.size()-1).longValue() + ref_ts < ts){
                    if(timestampList.isEmpty()) ref_ts = ts;
                    timestampList.add((float) (ts-ref_ts));

                    Float humidity = DataSnapshot.child("humidity").child(Integer.toString(i)).getValue(Float.class);
                    humidityList.add(humidity);

                    Float temp = DataSnapshot.child("temp").child(Integer.toString(i)).getValue(Float.class);
                    tempList.add(temp);
                }
            }

            tankFull = DataSnapshot.child("tankFull/0").getValue(Integer.class);
            if(tankFull == 1 && mNotification == null){
                mNotification = tankNotificationBuilder.build();
                notificationManager.notify(0, mNotification);
            }else if(tankFull == 0){
                notificationManager.cancel(0);
                mNotification = null;
            }

            broadcastUpdate("com.example.humiditytempchart.broadcast.FIREBASE_ACTION");

        }

        @Override
        public void onCancelled (@NonNull DatabaseError error){


        }
    };

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.shouldShowLights();
            channel.shouldVibrate();
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
            tankNotificationBuilder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                    .setSmallIcon(R.drawable.waterdrop)
                    .setContentTitle("Dehumidifier Notification")
                    .setContentText("Tank is full or removed")
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        }
    }

}