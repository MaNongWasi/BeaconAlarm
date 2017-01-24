package com.example.vtec.beaconalarm;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.util.List;

/**
 * Created by VTEC on 12/13/2016.
 */
public class EMBeacon {
    public double accx; //, accy, accz;

    public EMBeacon(List<AdRecord> records){
        for (AdRecord packet : records){
            if (packet.getType() == AdRecord.TYPE_ManufacturerSpecific){
                byte[] data = packet.getData();
                accx = extractAcc(data[3]);
//                accy = extractAcc(data[5]);
//                accy = extractAcc(data[7]);

                System.out.println("degree " + accx);
            }
        }
    }

    public static double extractAcc(byte data) {
        return data * 0.015625;
    }

}
