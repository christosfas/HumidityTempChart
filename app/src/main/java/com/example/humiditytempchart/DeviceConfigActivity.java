package com.example.humiditytempchart;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.espressif.iot.esptouch.EsptouchTask;
import com.espressif.iot.esptouch.IEsptouchResult;
import com.espressif.iot.esptouch.IEsptouchTask;
import com.espressif.iot.esptouch.util.ByteUtil;
import com.espressif.iot.esptouch.util.TouchNetUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class DeviceConfigActivity extends AppCompatActivity {

    private WifiInfo mWiFiInfo;
    private EditText passwordEditTxt, devCountEdtTxt;
    private TextView resultTxt;
    private RadioGroup packageModeGroup;
    private ConstraintLayout progressView, content;
    private EsptouchAsyncTask mTask;
    private final String TAG = this.getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_device_config);

        mWiFiInfo = getIntent().getParcelableExtra("wifiInfo");

        TextView ssidTxt = findViewById(R.id.apSsidText);
        ssidTxt.setText(mWiFiInfo.getSSID().substring(1, mWiFiInfo.getSSID().length()-1));
        TextView bssidTxt = findViewById(R.id.apBssidText);
        bssidTxt.setText(mWiFiInfo.getBSSID());
        resultTxt =  findViewById(R.id.testResult);

        packageModeGroup = findViewById(R.id.packageModeGroup);
        passwordEditTxt = findViewById(R.id.apPasswordEdit);
        devCountEdtTxt = findViewById(R.id.deviceCountEdit);
        progressView = findViewById(R.id.progressView);
        content = findViewById(R.id.content);
        Button cancelBtn = findViewById(R.id.cancel_button);
        cancelBtn.setOnClickListener(view -> {
            if(mTask != null) mTask.cancelEsptouch();
        });
        Button btn = findViewById(R.id.confirmBtn);
        btn.setOnClickListener(view -> executeEspTouch());
    }

    private void executeEspTouch() {
        byte[] ssid = ByteUtil.getBytesByString(mWiFiInfo.getSSID().substring(1, mWiFiInfo.getSSID().length()-1));
        Log.e(TAG, mWiFiInfo.getSSID().substring(1, mWiFiInfo.getSSID().length()-1));
        CharSequence pwdStr = passwordEditTxt.getText();
        byte[] password = pwdStr == null ? null : ByteUtil.getBytesByString(pwdStr.toString());
        byte[] bssid = TouchNetUtil.parseBssid2bytes(mWiFiInfo.getBSSID());
        Log.e(TAG, mWiFiInfo.getBSSID());
        CharSequence devCountStr = devCountEdtTxt.getText();
        byte[] deviceCount = devCountStr == null ? new byte[0] : devCountStr.toString().getBytes();
        byte[] broadcast = {(byte) (packageModeGroup.getCheckedRadioButtonId() == R.id.packageBroadcast
                ? 1 : 0)};

        if (mTask != null) {
            mTask.cancelEsptouch();
        }
        mTask = new EsptouchAsyncTask(this);
        mTask.execute(ssid, bssid, password, deviceCount, broadcast);
    }

    private static class EsptouchAsyncTask extends AsyncTask<byte[], IEsptouchResult, List<IEsptouchResult>> {
        private final WeakReference<DeviceConfigActivity> mActivity;

        private final Object mLock = new Object();
        private AlertDialog mResultDialog;
        private IEsptouchTask mEsptouchTask;
        private final String TAG = this.getClass().getSimpleName();

        EsptouchAsyncTask(DeviceConfigActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        void cancelEsptouch() {
            cancel(true);
            DeviceConfigActivity activity = mActivity.get();
            if (activity != null) {
                activity.showProgress(false);
            }
            if (mResultDialog != null) {
                mResultDialog.dismiss();
            }
            if (mEsptouchTask != null) {
                mEsptouchTask.interrupt();
            }
        }

        @Override
        protected void onPreExecute() {
            DeviceConfigActivity activity = mActivity.get();
            activity.resultTxt.setText("");
            activity.showProgress(true);
        }

        @Override
        protected void onProgressUpdate(IEsptouchResult... values) {
            DeviceConfigActivity activity = mActivity.get();
            if (activity != null) {
                IEsptouchResult result = values[0];
                Log.i(TAG, "EspTouchResult: " + result);
                String text = result.getBssid() + " is connected to the wifi";
                Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();

                activity.resultTxt.append(String.format(
                        Locale.ENGLISH,
                        "%s,%s\n",
                        result.getInetAddress().getHostAddress(),
                        result.getBssid()
                ));
            }
        }

        @Override
        protected List<IEsptouchResult> doInBackground(byte[]... params) {
            DeviceConfigActivity activity = mActivity.get();
            int taskResultCount;
            synchronized (mLock) {
                byte[] apSsid = params[0];
                byte[] apBssid = params[1];
                byte[] apPassword = params[2];
                byte[] deviceCountData = params[3];
                byte[] broadcastData = params[4];
                taskResultCount = deviceCountData.length == 0 ? -1 : Integer.parseInt(new String(deviceCountData));
                Context context = activity.getApplicationContext();
                mEsptouchTask = new EsptouchTask(apSsid, apBssid, apPassword, context);
                mEsptouchTask.setPackageBroadcast(broadcastData[0] == 1);
                mEsptouchTask.setEsptouchListener(this::publishProgress);
            }
            return mEsptouchTask.executeForResults(taskResultCount);
        }

        @Override
        protected void onPostExecute(List<IEsptouchResult> result) {
            DeviceConfigActivity activity = mActivity.get();
            activity.mTask = null;
            activity.showProgress(false);
            if (result == null) {
                mResultDialog = new AlertDialog.Builder(activity)
                        .setMessage(R.string.esptouch1_configure_result_failed_port)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                mResultDialog.setCanceledOnTouchOutside(false);
                return;
            }

            // check whether the task is cancelled and no results received
            IEsptouchResult firstResult = result.get(0);
            if (firstResult.isCancelled()) {
                return;
            }
            // the task received some results including cancelled while
            // executing before receiving enough results

            if (!firstResult.isSuc()) {
                mResultDialog = new AlertDialog.Builder(activity)
                        .setMessage(R.string.esptouch1_configure_result_failed)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                mResultDialog.setCanceledOnTouchOutside(false);
                return;
            }

            ArrayList<CharSequence> resultMsgList = new ArrayList<>(result.size());
            for (IEsptouchResult touchResult : result) {
                String message = activity.getString(R.string.esptouch1_configure_result_success_item,
                        touchResult.getBssid(), touchResult.getInetAddress().getHostAddress());
                resultMsgList.add(message);
            }
//            CharSequence[] items = new CharSequence[resultMsgList.size()];
//            mResultDialog = new AlertDialog.Builder(activity)
//                    .setTitle(R.string.esptouch1_configure_result_success)
//                    .setItems(resultMsgList.toArray(items), null)
//                    .setPositiveButton(android.R.string.ok, null)
//                    .show();
//            mResultDialog.setCanceledOnTouchOutside(false);
            String bssid = result.get(0).getBssid();
            Log.e(TAG, bssid);
            Intent intent = new Intent();
            intent.putExtra("bssid", bssid);
            activity.setResult(RESULT_OK, intent);
            activity.finish();
        }
    }

    private void showProgress(boolean show) {
        if (show) {
            content.setVisibility(View.INVISIBLE);
            progressView.setVisibility(View.VISIBLE);
        } else {
            content.setVisibility(View.VISIBLE);
            progressView.setVisibility(View.GONE);
        }
    }

}