package com.example.humiditytempchart;

import static java.lang.Math.abs;
import static java.lang.Math.floor;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.DefaultValueFormatter;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.text.*;
import java.util.Calendar;
import java.util.Date;
import java.lang.Object;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class MyXAxisValueFormatter extends ValueFormatter  {
    private int days, hours, minutes, seconds;
    private long ref;

    public MyXAxisValueFormatter (long ref){

        this.ref = ref;
        //days = 0;
    }

    @Override
    public String getFormattedValue(float value) {
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis((long)value + ref - (60*60*3*1000));
        hours = cal.getTime().getHours();
        minutes = cal.getTime().getMinutes();
        seconds = cal.getTime().getSeconds();

        String sHours = (hours<10)?"0"+hours:""+hours;
        String sMinutes = (minutes<10)?"0"+minutes:""+minutes;
        String sSeconds = (seconds<10)?"0"+seconds:""+seconds;

        return sHours+":"+sMinutes+":"+sSeconds;
    }

//    @Override
//    public String getFormattedValue(float value) {
//
//        String hours_str, minutes_str, seconds_str;
//
//        this.days = (int) (((long)(value+ref) -((long)(value+ref) % (3600*24)))/3600*24);
//        this.hours = (int) (((long)(value+ref) -(long)(days*3600*24) -(((long)(value+ref) -(long)(days*3600*24)) % 3600))/3600);
//        this.minutes = (int) (((long)(value+ref) -(long)(days*3600*24) -(long) (hours*60*60) -(((long)(value+ref) -(long)(days*3600*24) -(long)(hours*60*60)) % 60))/60);
//        this.seconds = (int) (((long)(value+ref) -(long)(days*3600*24) -(long)(hours*60*60) -(long)(minutes*60) -(((long)(value+ref) -(long)(days*3600*24) -(long)(hours*60*60) -(long)(minutes*60)) % 1))/1);
//
//        if(seconds < 0 ){
//            minutes-=1;
//            seconds = 60 +seconds;
//        }
//         if(minutes < 0){
//            hours -= 1;
//            minutes = 60 + minutes;
//
//        }
//         if(hours < 0 ){
//             days -=1;
//             hours = 24 + hours;
//         }
//
//         //String time = this.ref_ts.get((int) (value-(prev_val%10))/10);
//        if((hours / 10)<1){
//            hours_str = "0" + hours;
//        }
//        else hours_str = Integer.valueOf(hours).toString();
//
//        if((minutes / 10)<1){
//            minutes_str = "0" + minutes;
//        }
//        else minutes_str = Integer.valueOf(minutes).toString();
//
//        if((seconds / 10)<1){
//            seconds_str = "0" + seconds;
//        }
//        else seconds_str = Integer.valueOf(seconds).toString();
//
//        String time = hours_str + ":" + minutes_str + ":" + seconds_str;
//         //this.prev_val = value;
//
//         return time ;
//    }

 }
