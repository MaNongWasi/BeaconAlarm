package com.example.vtec.beaconalarm;

import android.app.Activity;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.text.format.DateFormat;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

/**
 * Created by Cecylia on 12/11/2016.
 */
public class TCPActivity extends Activity {
    private final int HANDLER_MSG_TELL_RECV = 0x124;
    String apihost = "";
    String start_url = "livedata";
    String disconnect_url = "disconnect";
    private RequestQueue requestQueue;
    private com.github.nkzawa.socketio.client.Socket mSocket;
    private JSONObject jsonObject;
    private double degree;
    private boolean notify = false;
    private NotificationManager manager;
    CharSequence sysTimeStr;
    long time;
    String alarm_time = null;
    private ImageView im, alarm_im;
    private TextView time_tv, alarm_tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tcp);

        ip_dialog();

        manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
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

        requestQueue = Volley.newRequestQueue(getApplicationContext());
    }

    private void send_request(String url) {
        StringRequest request = new StringRequest(Request.Method.GET, apihost+url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                System.out.println("response -> " + response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println(error.getMessage() + " " + error);
            }
        });
        requestQueue.add(request);
    }


    public CharSequence getCurrentTime() {
        time = System.currentTimeMillis();
        sysTimeStr = DateFormat.format("yy-MM-dd kk:mm:ss", time);
        return sysTimeStr;
    }

    public void ip_dialog() {
        final Dialog dialog = new Dialog(TCPActivity.this);
        dialog.setContentView(R.layout.edit_dialog);
        dialog.setTitle("IP Address");
        Button ok_bt = (Button) dialog.findViewById(R.id.ok);
        Button cancel_bt = (Button) dialog.findViewById(R.id.cancel);
        final EditText ip_et = (EditText) dialog.findViewById(R.id.input_et);
        ip_et.setInputType(InputType.TYPE_CLASS_NUMBER);
        ok_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NetTool netTool = new NetTool(TCPActivity.this);
                apihost = "http://" + netTool.network_seg + ip_et.getText().toString() + ":8000/";
                dialog.dismiss();
                init_sockt();
            }
        });
        cancel_bt.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void init_sockt() {
        try {
//            System.out.println(apihost);
            mSocket = IO.socket(apihost + "test");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        mSocket.connect();
        mSocket.on("connect", onConnectMsg);

        mSocket.on(com.github.nkzawa.socketio.client.Socket.EVENT_CONNECT, onConnect);
        mSocket.on(com.github.nkzawa.socketio.client.Socket.EVENT_DISCONNECT, onDisconnect);
        mSocket.on(com.github.nkzawa.socketio.client.Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.on(com.github.nkzawa.socketio.client.Socket.EVENT_CONNECT_TIMEOUT, onConnectTimeoutError);
        mSocket.on("live_data", onEventRecieved);
    }

    @Override
    public void onDestroy() {
        System.out.println("destory");
        socket_clear();
        super.onDestroy();
    }

    private void socket_clear() {
        mSocket.disconnect();
        mSocket.off(com.github.nkzawa.socketio.client.Socket.EVENT_CONNECT, onConnect);
        mSocket.off(com.github.nkzawa.socketio.client.Socket.EVENT_DISCONNECT, onDisconnect);
        mSocket.off(com.github.nkzawa.socketio.client.Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.off(com.github.nkzawa.socketio.client.Socket.EVENT_CONNECT_TIMEOUT, onConnectTimeoutError);
        mSocket.off("live_data", onEventRecieved);
    }


    private Emitter.Listener onConnectMsg = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            System.out.println("Emitter invoked");

            System.out.println("onConnect msg : " + args);
        }
    };
    private Emitter.Listener onEventRecieved = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {

//
//            for (int i = 0; i < args.length; i++) {
//                System.out.println("received masg : " + i + " " + args[i]);
//            }
            jsonObject = (JSONObject) args[0];
            try {
                degree = jsonObject.getDouble("degree");
                System.out.println("degree " + degree);
                if (Math.abs(degree) > 0.5 && !notify) {
//                    socket_clear();
                    notify = true;
                    alarm_time = getCurrentTime().toString();
//                    build_notification();

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }


        }
    };

    private static final int ALARM = 0;
    private static final int CON_FAILD = 1;
    private static final int CON_TIMEOUT = 2;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ALARM:
                    setContentView(R.layout.activity_alarm);
                    String info = (String) msg.obj;
                    if (info != null) {
                        time_tv = (TextView) findViewById(R.id.time_tv);
                        time_tv.setVisibility(View.VISIBLE);
                        time_tv.setText(info);
                    } else {
                        alarm_im = (ImageView) findViewById(R.id.alarm);
                        alarm_tv = (TextView) findViewById(R.id.alarm_tv);
                        alarm_im.setImageResource(R.drawable.alarm_clock);
                        alarm_tv.setText("NO INTRUDER");
                        alarm_tv.setTextColor(Color.BLUE);
                    }
                    break;

                case CON_FAILD:
                    Toast.makeText(TCPActivity.this, "Connetion Failed...", Toast.LENGTH_LONG).show();
                    break;
                case CON_TIMEOUT:
                    Toast.makeText(TCPActivity.this, "Connetion timeout...", Toast.LENGTH_LONG).show();
                    break;
            }

        }
    };

    private Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            System.out.println("disconnect ");
            send_request(disconnect_url);
//            isConnected = false;
        }
    };

    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            System.out.println("connect Successful ");
            send_request(start_url);

        }
    };

    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            System.out.println("Connection failed" + args[0]);
            Message msg = new Message();
            msg.what = CON_FAILD;
            handler.sendMessage(msg);

        }
    };

    private Emitter.Listener onConnectTimeoutError = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            System.out.println("connection timeout" + args[0]);
            Message msg = new Message();
            msg.what = CON_TIMEOUT;
            handler.sendMessage(msg);

        }
    };

    public void build_notification() {
//        RemoteViews rv = new RemoteViews(getPackageName(), R.layout.activity_notify);
        Bitmap burglar = BitmapFactory.decodeResource(getResources(), R.drawable.burglar);
        Intent intent = new Intent(TCPActivity.this, AlarmActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);

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

        manager.notify(1, myNotify); //NOTIFICATION_FLAG
    }
}
