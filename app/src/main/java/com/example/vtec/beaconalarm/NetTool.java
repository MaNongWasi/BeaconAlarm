package com.example.vtec.beaconalarm;

import android.content.ContentValues;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Message;
import android.widget.Toast;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by VTEC on 1/6/2017.
 */
public class NetTool {
    private Context context;

    public String local_IP, network_seg;
    private int ip_index;
    private volatile List<String> ip_list = new ArrayList<>();
    private Runtime runtime = Runtime.getRuntime();
    private WifiManager mWifiManager;
    private WifiInfo mWifiInfo;
    private List<ScanResult> mWifiList;
    private List<WifiConfiguration> MwifiConfig;
    WifiManager.WifiLock mWifiLock;
    private String ping = "ping -c 1 -w 0.5 " ;//-c count -w response time
    private Runtime run = Runtime.getRuntime();
    private Process proc = null;
    public boolean exit_ip = false;

    public boolean isExit_ip() {
        return exit_ip;
    }

    public void setExit_ip(boolean exit_ip) {
        this.exit_ip = exit_ip;
    }



    public NetTool(Context context){
        this.context = context;

        mWifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        mWifiInfo = mWifiManager.getConnectionInfo();

        mWifiManager.startScan();
        mWifiList = mWifiManager.getScanResults();

        //set local ip
        String ip = getLocalIP().toString();
        local_IP = ip.substring(1, ip.length());
        network_seg = this.local_IP.substring(0, this.local_IP.lastIndexOf(".")+1);
//        System.out.println("local IP " + local_IP);
//        System.out.println("network seg " + network_seg);
//        getOtherIP();
    }

    //get local IP
    public InetAddress getLocalIP(){
        int hostAddress = mWifiInfo.getIpAddress();
        byte[] addressBytes = {(byte)(0xff & hostAddress), (byte)(0xff & (hostAddress >> 8)),
                (byte)(0xff & (hostAddress >> 16)),
                (byte)(0xff & (hostAddress >> 24))};
        try{
            return InetAddress.getByAddress(addressBytes);
        }catch(UnknownHostException e){
            throw new AssertionError();
        }
    }

    public void checkIP(final String last_ip){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    InetAddress inetAddress = InetAddress.getByName(network_seg + last_ip);
                    if (inetAddress.isReachable(1000)){
                        setExit_ip(true);
                    }else {
                        setExit_ip(false);
                    }
                }catch (UnknownHostException e){
                    e.printStackTrace();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }).start();

    }


    public void getOtherIP(){

        for (int i = 0; i < 256; i++){
            try{
                InetAddress inetAddress = InetAddress.getByName(network_seg + String.valueOf(i));
                if (inetAddress.isReachable(1000)){
                    System.out.println(String.valueOf(i));
                    System.out.println(inetAddress.getHostName());
                }
            }catch (UnknownHostException e){
                e.printStackTrace();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        if (network_seg.equals("")){
            Toast.makeText(context, "Scan failed. Please check the Wifi Network", Toast.LENGTH_LONG).show();
            return;
        }


        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    String p = network_seg + "101";
                    InetAddress add = InetAddress.getByName(p);
                    System.out.println(add.getHostName());
                }catch (IOException e1){
                    e1.printStackTrace();
                }finally {

                }
            }

        }).start();



        /*for(int i = 0; i < 256; i++){
            j = i;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String p = NetTool.this.ping + network_seg + NetTool.this.j;
                    String current_ip = network_seg + NetTool.this.j;
                    try {
                        proc = run.exec(p);

                        int result = proc.waitFor();
                        if (result == 0) {
                            System.out.println("connect success " + current_ip);
                            InetAddress inetAddress = InetAddress.getByName(current_ip);
                            System.out.println("connect success " + inetAddress);
                            System.out.println("hostname " + inetAddress.getHostAddress());
                        }
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    } catch (InterruptedException e2) {
                        e2.printStackTrace();
                    } finally {
                        proc.destroy();
                    }
                }
                }).start();
        }*/

    }


}
