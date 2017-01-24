package com.example.vtec.beaconalarm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by VTEC on 12/13/2016.
 */

public class AdRecord {
    public static final int TYPE_FLAGS = 0x1;
    public static final int TYPE_UUID16_INC = 0x2;
    public static final int TYPE_UUID16 = 0x3;
    public static final int TYPE_UUID32_INC = 0x4;
    public static final int TYPE_UUID32 = 0x5;
    public static final int TYPE_UUID128_INC = 0x6;
    public static final int TYPE_UUID128 = 0x7;
    public static final int TYPE_NAME_SHORT = 0x8;
    public static final int TYPE_NAME = 0x9;
    public static final int TYPE_TRANSMITPOWER = 0xA;
    public static final int TYPE_CONNINTERVAL = 0x12;
    public static final int TYPE_SERVICEDATA = 0x16;
    public static final int TYPE_ManufacturerSpecific = -1; //(0xff)

    public static List<AdRecord> parseScanRecord(byte[] scanRecord){

        List<AdRecord> records = new ArrayList<AdRecord>();

        int index = 0;
        while(index < scanRecord.length){
            int length = scanRecord[index++];
            if(length ==0 ) break;

            int type = scanRecord[index];
            if(type == 0) break;

            byte[] data = Arrays.copyOfRange(scanRecord, index + 1, index + length);

            records.add(new AdRecord(length, type, data));

            index += length;
        }

        return records;
    }

    public static String getName(AdRecord nameRecord){
        return new String(nameRecord.mData);
    }

    public static int getServiceDataUuid(AdRecord serviceData){
        if(serviceData.mType != TYPE_SERVICEDATA) return -1;

        byte[] raw = serviceData.mData;
        int uuid = (raw[1] & 0xFF) << 8;
        uuid += (raw[0] & 0xFF);

        return uuid;
    }

    public static byte[] getServiceData(AdRecord serviceData){

        if(serviceData.mType != TYPE_SERVICEDATA) return null;

        byte[] raw = serviceData.mData;
        //System.out.println("getServicedata" + raw.length);
        return Arrays.copyOfRange(raw, 2, raw.length);
    }

    public static byte[] getServiceName(AdRecord serviceData){
        if(serviceData.mType != TYPE_NAME) return null;
        byte[] raw = serviceData.mData;
//        System.out.println("getServiceName" + raw.length);
        return Arrays.copyOfRange(raw, 2, raw.length);

    }

    public static byte[] getBeaconUUID(AdRecord beaconData){
        if (beaconData.mType != TYPE_ManufacturerSpecific) return null;

        byte[] raw = beaconData.mData;
        return Arrays.copyOfRange(raw, 4, raw.length - 5);
    }

    public static byte[] getBeaconMajor(AdRecord beaconData){
        if (beaconData.mType != TYPE_ManufacturerSpecific) return null;
        byte[] raw = beaconData.mData;
        return Arrays.copyOfRange(raw, raw.length - 5, raw.length - 3);
    }


    public static byte[] getBeaconMinor(AdRecord beaconData){
        if (beaconData.mType != TYPE_ManufacturerSpecific) return null;
        byte[] raw = beaconData.mData;
        return Arrays.copyOfRange(raw, raw.length - 3, raw.length - 1);
    }

    public static byte getBeaconTxPower(AdRecord beaconData){
        if (beaconData.mType != TYPE_ManufacturerSpecific) return 0;
        byte[] raw = beaconData.mData;
        return raw[raw.length - 1];
    }

    private int mLength;
    private int mType;
    private byte[] mData;

    public AdRecord(int length, int type, byte[] data){
        mLength = length;
        mType = type;
        mData = data;
    }

    public byte[] getData(){
        return mData;
    }

    public int getLength(){
        return mLength;
    }

    public int getType(){
        return mType;
    }

    public String toString(){
        switch(mType){
            case TYPE_FLAGS:
                return "Flags";
            case TYPE_NAME_SHORT:
            case TYPE_NAME:
                return "Name";
            case TYPE_UUID16:
            case TYPE_UUID16_INC:
                return "UUIDs";
            case TYPE_TRANSMITPOWER:
                return "Transmit Power";
            case TYPE_CONNINTERVAL:
                return "Connect Interval";
            case TYPE_SERVICEDATA:
                return "Service Data";
            default:
                return "Unknown Structure: "+mType;
        }
    }
}