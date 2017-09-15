package com.nordicsemi.MultiLinkDemo;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Created by too1 on 16-Aug-17.
 */

public class BleLinkManager {

    private Map<Integer, BleDevice> mDeviceMap;
    private Context                 mAppContext;
    private BleListViewAdapter      mBleListViewAdapter;

    public BleLinkManager(Context appContext){
        mAppContext = appContext;
        mDeviceMap = new HashMap<Integer, BleDevice>();
        ArrayList<BleDevice> bleDeviceList = new ArrayList<BleDevice>();
        mBleListViewAdapter = new BleListViewAdapter(appContext, bleDeviceList);
    }

    public void processBlePacket(byte []packet){
        String hexString = "";
        for(int i = 0; i < packet.length; i++){
            hexString += String.valueOf((int)packet[i]) + " ";
        }
        Log.d("BleLinkManager", "New packet: " + hexString);

        int connHandle;
        if(packet.length >= 1){
            switch(packet[0]){
                // Device connected
                case 1:
                    connHandle = packet[1] << 8 | packet[2];
                    addBleDevice(connHandle);
                    if(!mDeviceMap.containsKey(connHandle)){
                        mDeviceMap.put(connHandle, null);
                    }
                    break;
                // Device disconnected
                case 2:
                    connHandle = packet[1] << 8 | packet[2];
                    removeBleDevice(connHandle);
                    if(mDeviceMap.containsKey(connHandle)){
                        mDeviceMap.remove(connHandle);
                    }
                    break;
            }
        }
    }

    public ArrayAdapter getListAdapter(){
        return mBleListViewAdapter;
    }

    public void addDebugItem(){
        mBleListViewAdapter.add(new BleDevice());
    }

    public void addBleDevice(int connHandle){
        mBleListViewAdapter.add(new BleDevice(connHandle));
    }

    public void removeBleDevice(int connHandle){
        mBleListViewAdapter.removeByConnHandle(connHandle);
    }

    public void clearBleDevices(){
        mBleListViewAdapter.clear();
    }

    private class BleDevice{
        public String  Name;
        public String  Type;
        public boolean ButtonState;
        public int     Color;
        private int    mConnHandle;

        public BleDevice(){
            Name = "Noname";
            Type = "None";
            ButtonState = false;
            Color = 0x00FFFFFF;
        }

        public BleDevice(int connHandle){
            mConnHandle = connHandle;
            Name = "Noname";
            Type = "None";
            ButtonState = false;
            Color = 0x00FFFFFF;
        }

        public boolean connHandleMatch(int connHandle) {
            return mConnHandle == connHandle;
        }

        public int getConnHandle(){
            return mConnHandle;
        }
    }

    private class BleListViewAdapter extends ArrayAdapter {
        public BleListViewAdapter(Context context, ArrayList<BleDevice> bleDeviceList){
            super(context, 0, bleDeviceList);
        }

        public boolean removeByConnHandle(int connHandle){
            BleDevice foundDevice = null;
            for(int i = 0; i < getCount(); i++){
                if(((BleDevice)getItem(i)).connHandleMatch(connHandle)){
                    foundDevice = (BleDevice)getItem(i);
                    break;
                }
            }
            if(foundDevice != null){
                mBleListViewAdapter.remove(foundDevice);
                return true;
            }
            else return false;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup viewGroup) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.ble_link_view, viewGroup, false);
            }
            BleDevice currentDevice = (BleDevice)getItem(position);
            ((TextView)convertView.findViewById(R.id.name)).setText(currentDevice.Name);
            ((TextView)convertView.findViewById(R.id.type)).setText(currentDevice.Type);
            ((TextView)convertView.findViewById(R.id.connHandle)).setText(String.valueOf(currentDevice.getConnHandle()));
            return convertView;
        }
    }
}
