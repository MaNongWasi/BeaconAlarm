package com.example.vtec.beaconalarm;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by VTEC on 12/12/2016.
 */
public class BTActivity extends Activity{
    private BluetoothAdapter mBluetoothAdapter;
    private int REQUEST_ENABLE_BT = 1;
    private Handler mHandler;
    private static final long STOP_PERIOD = 4000;
    private static final long SCAN_PERIOD = 4100;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt mGatt;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private NotificationManager manager;
    private boolean notify = false;
    CharSequence sysTimeStr;
    long time;
    String alarm_time = null;
    private ImageView im, alarm_im;
    private TextView time_tv, alarm_tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bt);

        mHandler = new Handler();

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        permission_ask();

        im = (ImageView) findViewById(R.id.alarm);
        im.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Message msg = new Message();
                msg.obj = alarm_time;
                msg.what = ALARM;
                handler.sendMessage(msg);

                alarm_time = null;
            }
        });
    }

    @Override
    protected void onResume(){
        super.onResume();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }else {
            if (Build.VERSION.SDK_INT >= 21){
                mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
                filters = new ArrayList<ScanFilter>();
                scanLeDevice(true);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mLEScanner.stopScan(mScanCallback);
        mHandler.removeCallbacks(mStartRunnable);
        mHandler.removeCallbacks(mStopRunnable);
        if (mGatt == null) {
            return;
        }

        mGatt.close();
        mGatt = null;

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                //Bluetooth not enabled.
                finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private static final int ALARM = 0;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == ALARM) {
                setContentView(R.layout.activity_alarm);
                String info = (String) msg.obj;
                if (info != null){
                    time_tv = (TextView) findViewById(R.id.time_tv);
                    time_tv.setVisibility(View.VISIBLE);
                    time_tv.setText(info);
                }else{
                    alarm_im = (ImageView)findViewById(R.id.alarm);
                    alarm_tv = (TextView)findViewById(R.id.alarm_tv);
                    alarm_im.setImageResource(R.drawable.alarm_clock);
                    alarm_tv.setTextColor(Color.BLUE);
                    alarm_tv.setText("NO INTRUDE");
                }
            }
        }
    };

    public CharSequence getCurrentTime() {
        time = System.currentTimeMillis();
        sysTimeStr = DateFormat.format("yy-MM-dd kk:mm:ss", time);
        return sysTimeStr;
    }

    private Runnable mStopRunnable = new Runnable() {
        @Override
        public void run() {
            if (Build.VERSION.SDK_INT < 21) {
//                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
            } else {
                mLEScanner.stopScan(mScanCallback);
                //                        scanLeDevice(true);
            }
        }
    };


    private Runnable mStartRunnable = new Runnable() {
        @Override
        public void run() {
            scanLeDevice(true);
        }
    };

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(mStopRunnable, STOP_PERIOD);
            mHandler.postDelayed(mStartRunnable, SCAN_PERIOD);
            if (Build.VERSION.SDK_INT > 21) {
//                mBluetoothAdapter.startLeScan(mLeScanCallback);
//            } else {
                mLEScanner.startScan(filters, settings, mScanCallback);
            }
        } else {
            if (Build.VERSION.SDK_INT > 21) {
                mLEScanner.stopScan(mScanCallback);
//            } else {
//                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {

            if (result.getDevice().getName() != null){
                if (result.getDevice().getName().equals("EMBeacon02587")){
                    byte[] results = result.getScanRecord().getBytes();
                    List<AdRecord> records = AdRecord.parseScanRecord(results);
                    EMBeacon emBeacon = new EMBeacon(records);
//                    BeaconTag
                    if(Math.abs(emBeacon.accx) > 0.5 && !notify){
                        scanLeDevice(false);
                        notify = true;
                        alarm_time = getCurrentTime().toString();
//                        build_notification();
                    }
                }
            }
        }




        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    public void build_notification(){
//        RemoteViews rv = new RemoteViews(getPackageName(), R.layout.activity_notify);
        Bitmap burglar = BitmapFactory.decodeResource(getResources(), R.drawable.burglar);
        Intent intent = new Intent(BTActivity.this, AlarmActivity.class);
        PendingIntent contentIntent =PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        Notification myNotify = new Notification.Builder(this)
                .setLargeIcon(burglar)
                .setSmallIcon(R.drawable.thief)
//                .setTicker("Your door is open!!!")
                .setContentIntent(contentIntent)
                .setContentTitle("Alarm!!!")
                .setContentText("Your door is open!!!")
//                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
//                .setContent(rv)
                .build();

        manager.notify(1 , myNotify); //NOTIFICATION_FLAG
    }

    private void permission_ask(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                   // Log.d(TAG, "coarse location permission granted");
                    System.out.println("coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }
}
