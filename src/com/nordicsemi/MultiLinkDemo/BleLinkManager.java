package com.nordicsemi.MultiLinkDemo;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by too1 on 16-Aug-17.
 */

public class BleLinkManager {
    private BleListViewAdapter      mBleListViewAdapter;
    private LedButtonService        mService;
    private float                   mSelColorR = 1.0f, mSelColorG = 1.0f, mSelColorB = 1.0f, mSelColorInt = 1.0f;
    private boolean                 mInThingySelectMode = false;

    private enum BLE_TX_COMMANDS {INVALID, LINK_CONNECTED, LINK_DISCONNECTED, LINK_DATA_UPDATE, LED_BUTTON_PRESSED};

    public interface BleLinkListener {
        public void onListChanged();
    }
    private BleLinkListener mListener = null;

    public BleLinkManager(Context appContext){
        ArrayList<BleDevice> bleDeviceList = new ArrayList<BleDevice>();
        mBleListViewAdapter = new BleListViewAdapter(appContext, bleDeviceList);
    }

    public void setBleLinkListener(BleLinkListener listener){
        mListener = listener;
    }

    public int getNumberOfLinks(){
        return mBleListViewAdapter.getCount();
    }

    public void setOutgoingService(LedButtonService service){
        mService = service;
    }

    public void processBlePacket(byte []packet){
        String hexString = "";
        for(int i = 0; i < packet.length; i++){
            hexString += String.valueOf((int)packet[i]) + " ";
        }
        Log.d("BleLinkManager", "New packet: " + hexString);

        int connHandle = 0xFFFF;
        if(packet.length >= 3) {
            connHandle = packet[1] << 8 | packet[2];
        }
        switch(BLE_TX_COMMANDS.values()[packet[0]]){
            // Device connected
            case LINK_CONNECTED:
                addBleDevice(connHandle, Arrays.copyOfRange(packet, 3, packet.length));
                if(mListener != null){
                    mListener.onListChanged();
                }
                break;
            // Device disconnected
            case LINK_DISCONNECTED:
                removeBleDevice(connHandle);
                if(mListener != null){
                    mListener.onListChanged();
                }
                break;
            // Data update received
            case LINK_DATA_UPDATE:
                Log.d("BleLinkManager", "Data updated");
                bleDeviceDataUpdate(connHandle, Arrays.copyOfRange(packet, 4, 4 + packet[3]));
                break;

            case LED_BUTTON_PRESSED:
                bleDevicesLedUpdate(packet[1]);
                break;
        }
    }

    public ArrayAdapter getListAdapter(){
        return mBleListViewAdapter;
    }

    private enum OutgoingCommand {
        ERROR, SetLedColorAll, SetLedStateAll, PostConnectMessage, DisconnectAllPeripherals, DisconnectCentral, SetLedPulse
    }

    public void addDebugItem(){
        mBleListViewAdapter.add(new BleDevice());
    }

    public void addBleDevice(int connHandle, int type, int buttonState, int ledState, int rssi, String deviceName){
        BleDevice newBleDevice = new BleDevice(connHandle, type);
        newBleDevice.setName(deviceName);
        newBleDevice.ButtonState = (buttonState != 0);
        newBleDevice.setColorIntensity(ledState != 0, 1.0f);
        newBleDevice.setRssi(rssi);
        mBleListViewAdapter.add(newBleDevice);

        // Send welcome message to the new device
        /*byte []newCmd = new byte[2];
        newCmd[0] = (byte)OutgoingCommand.PostConnectMessage.ordinal();
        newCmd[1] = (byte)connHandle;
        sendOutgoingCommand(newCmd);*/
    }

    public void addBleDevice(int connHandle, byte []bleDeviceData){
        String deviceName;
        if(bleDeviceData.length > 7){
            deviceName = new String(Arrays.copyOfRange(bleDeviceData, 7, bleDeviceData.length));
        }
        else deviceName = "No name";

        BleDevice newBleDevice = null;
        for(int i = 0; i < mBleListViewAdapter.getCount(); i++){
            if(((BleDevice)mBleListViewAdapter.getItem(i)).getConnHandle() == connHandle)
            {
                newBleDevice = (BleDevice)mBleListViewAdapter.getItem(i);
                break;
            }
        }
        //TODO: Figure out why the BLE device can some times be found in the list (list should be cleared on disconnect)
        boolean addNewDevice = false;
        if(newBleDevice == null) {
            newBleDevice = new BleDevice(connHandle, bleDeviceData[0]);
            addNewDevice = true;
        }
        newBleDevice.setName(deviceName);
        newBleDevice.ButtonState = (bleDeviceData[1] != 0);
        newBleDevice.setColorIntensity((bleDeviceData[2] != 0), 1.0f);
        newBleDevice.setColor(bleDeviceData[3], bleDeviceData[4], bleDeviceData[5]);
        newBleDevice.setRssi((int) bleDeviceData[6]);
        newBleDevice.setPhy((int) bleDeviceData[7]);
        if(addNewDevice) mBleListViewAdapter.add(newBleDevice);

        // Send welcome message to the new device
        /*byte []newCmd = new byte[2];
        newCmd[0] = (byte)OutgoingCommand.PostConnectMessage.ordinal();
        newCmd[1] = (byte)connHandle;
        sendOutgoingCommand(newCmd);*/
        mBleListViewAdapter.notifyDataChanged();
    }

    public void removeBleDevice(int connHandle){
        mBleListViewAdapter.removeByConnHandle(connHandle);
    }

    public void bleDeviceDataUpdate(int connHandle, byte []data){
        boolean newButtonState;
        BleDevice updatedDevice = mBleListViewAdapter.findByConnHandle(connHandle);
        if(updatedDevice != null && data.length >= 1) {
            newButtonState = (data[0] != 0);
            if(newButtonState != updatedDevice.ButtonState) {
                updatedDevice.ButtonState = newButtonState;
                // If the button is pressed, invert checked status
                if (newButtonState) {
                    if(!mInThingySelectMode){
                        for(int i = 0; i < mBleListViewAdapter.getCount(); i++){
                            ((BleDevice)mBleListViewAdapter.getItem(i)).Checked = false;
                        }
                        mInThingySelectMode = true;
                    }
                    updatedDevice.Checked = !updatedDevice.Checked;
                    if(updatedDevice.Checked){
                        sendOutgoingCommand(updatedDevice.getCommandSetPulse(1, 1.0f, 50));
                    }
                    else {
                        sendOutgoingCommand(updatedDevice.getCommandResetColor());
                    }
                }
            }
            updatedDevice.setPhy((int)data[1]);
            updatedDevice.setRssi((int)data[2]);
            mBleListViewAdapter.notifyDataChanged();
        }
    }

    public void bleDevicesLedUpdate(int ledState){
        for(int i = 0; i < mBleListViewAdapter.getCount(); i++){
            ((BleDevice)mBleListViewAdapter.getItem(i)).LedState = (ledState != 0);
        }
        mBleListViewAdapter.notifyDataChanged();
    }

    private void sendOutgoingCommand(byte []command){
        if(mService != null && mService.isConnected()){
            mService.writeRXCharacteristic(command);
        }
    }

    private void sendColorAll(boolean state, int deviceMask){
        byte []newCmd = new byte[8];
        newCmd[0] = (byte)OutgoingCommand.SetLedColorAll.ordinal();
        newCmd[1] = state ? (byte)1 : (byte)0;
        newCmd[2] = (byte)(mSelColorR * mSelColorInt * 255.0f);
        newCmd[3] = (byte)(mSelColorG * mSelColorInt * 255.0f);
        newCmd[4] = (byte)(mSelColorB * mSelColorInt * 255.0f);
        for(int i = 0; i < 3; i++){
            newCmd[5 + i] = (byte)(deviceMask & 0xFF);
            deviceMask >>= 8;
        }
        mInThingySelectMode = false;
        sendOutgoingCommand(newCmd);
    }

    private void sendColorStateAll(boolean state, int deviceMask){
        byte []newCmd = new byte[5];
        newCmd[0] = (byte)OutgoingCommand.SetLedStateAll.ordinal();
        newCmd[1] = state ? (byte)1 : (byte)0;
        for(int i = 0; i < 3; i++){
            newCmd[2 + i] = (byte)(deviceMask & 0xFF);
            deviceMask >>= 8;
        }
        mInThingySelectMode = false;
        sendOutgoingCommand(newCmd);
    }

    private int getConnIdSelectedMask(){
        int mask = 0;
        BleDevice bleDevice;
        for(int i = 0; i < mBleListViewAdapter.getCount(); i++) {
            bleDevice = (BleDevice)mBleListViewAdapter.getItem(i);
            if(bleDevice.Checked) mask |= (1 << bleDevice.getConnHandle());
        }

        // If no devices are selected, select all
        if(mask == 0) mask = 0xFFFFFFFF;

        // Return the mask
        return mask;
    }

    private boolean currentState = true;
    public boolean toggleLedStateIntensityAll(float intensity){
        currentState = !currentState;
        setLedStateIntensityAll(currentState, intensity);
        return currentState;
    }

    public void setLedStateIntensityAll(boolean state, float intensity){
        int deviceMask = getConnIdSelectedMask();
        BleDevice bleDevice;
        mSelColorInt = intensity;
        for(int i = 0; i < mBleListViewAdapter.getCount(); i++){
            bleDevice = (BleDevice)mBleListViewAdapter.getItem(i);
            if((deviceMask & (1 << bleDevice.getConnHandle())) != 0){
                bleDevice.setColorIntensity(state, mSelColorInt);
            }
        }
        mBleListViewAdapter.notifyDataChanged();
        sendColorStateAll(state, deviceMask);
    }

    public void setLedRgbAll(float r, float g, float b){
        BleDevice bleDevice;
        int selectedConnIDs = 0;
        mSelColorR = r;
        mSelColorG = g;
        mSelColorB = b;
        selectedConnIDs = getConnIdSelectedMask();

        for(int i = 0; i < mBleListViewAdapter.getCount(); i++){
            bleDevice = (BleDevice)mBleListViewAdapter.getItem(i);
            if((selectedConnIDs & (1 << bleDevice.getConnHandle())) != 0){
                bleDevice.setColor(mSelColorR, mSelColorG, mSelColorB);
                bleDevice.setColorIntensity(true, mSelColorInt);
            }
        }
        mBleListViewAdapter.notifyDataChanged();
        sendColorAll(true, selectedConnIDs);
    }

    public void setLedRgbAll(int color){
        setLedRgbAll((float)(color >> 16) / 255.0f, (float)((color >> 8) & 0xFF) / 255.0f, (float)((color >> 0) & 0xFF) / 255.0f);
    }

    public void listSelectAll(){
        for(int i = 0; i < mBleListViewAdapter.getCount(); i++) {
            ((BleDevice)mBleListViewAdapter.getItem(i)).Checked = true;
        }
        mBleListViewAdapter.notifyDataChanged();
    }

    public void listDeselectAll(){
        for(int i = 0; i < mBleListViewAdapter.getCount(); i++) {
            ((BleDevice)mBleListViewAdapter.getItem(i)).Checked = false;
        }
        mBleListViewAdapter.notifyDataChanged();
    }

    public void disconnectAllPeripherals(){
        byte []newCmd = new byte[1];
        newCmd[0] = (byte)OutgoingCommand.DisconnectAllPeripherals.ordinal();
        sendOutgoingCommand(newCmd);
    }

    public void disconnectCentral(){
        byte []newCmd = new byte[1];
        newCmd[0] = (byte)OutgoingCommand.DisconnectCentral.ordinal();
        sendOutgoingCommand(newCmd);
    }

    public void itemClicked(int index){
        if(index < mBleListViewAdapter.getCount()) {
            BleDevice bleDevice = (BleDevice)mBleListViewAdapter.getItem(index);
            bleDevice.Checked = !bleDevice.Checked;
            mBleListViewAdapter.notifyDataChanged();
        }
    }

    public void clearBleDevices(){
        mBleListViewAdapter.clear();
        mBleListViewAdapter.notifyDataChanged();
    }

    private class BleDevice{
        private String  mName;
        public boolean  ButtonState;
        private int     mColor;
        private float   []mBaseColors = {1.0f, 1.0f, 1.0f};
        private float   mColorIntensity = 1.0f;
        private boolean mRgbLedSupported = false;
        public boolean  LedState;
        public boolean  Checked;
        private int     mType;
        private int     mConnHandle;
        private int     mPhy;
        private int     mRssi;

        public BleDevice(){
            mName = "Noname";
            ButtonState = false;
            LedState = true;
            Checked = false;
            mColor = 0xFFFFFFFF;
        }

        public BleDevice(int connHandle, int type){
            mConnHandle = connHandle;
            mType = type;
            mRgbLedSupported = (type == 2);
            mName = "Noname";
            ButtonState = false;
            LedState = true;
            Checked = false;
            mColor = 0xFFFFFFFF;
            mPhy = 1;
            mRssi = 0;
        }

        public void setName(String name){
            mName = name;
        }

        public String getName(){
            return mName;
        }

        public boolean connHandleMatch(int connHandle) {
            return mConnHandle == connHandle;
        }

        public int getConnHandle(){
            return mConnHandle;
        }

        public int getType(){
            return mType;
        }

        public int getColor(){ return mColor; }

        public void setColor(float r, float g, float b){
            mBaseColors[0] = mRgbLedSupported ? r : 1.0f;
            mBaseColors[1] = mRgbLedSupported ? g : 1.0f;
            mBaseColors[2] = mRgbLedSupported ? b : 1.0f;
            mColor = (int)((mBaseColors[0] * mColorIntensity) * 255.0f);
            mColor = (int)((mBaseColors[1] * mColorIntensity) * 255.0f) | mColor << 8;
            mColor = (int)((mBaseColors[2] * mColorIntensity) * 255.0f) | mColor << 8;
            mColor |= 0xFF000000;
        }

        public void setColor(byte r, byte g, byte b){
            setColor((float)(Integer.valueOf(r & 0xFF)) / 255.0f, (float)(Integer.valueOf(g & 0xFF)) / 255.0f, (float)(Integer.valueOf(b & 0xFF)) / 255.0f);
        }

        public void setColorIntensity(boolean state, float intensity){
            LedState = state;
            mColorIntensity = intensity;
            mColor = (int)((mBaseColors[0] * mColorIntensity) * 255.0f);
            mColor = (int)((mBaseColors[1] * mColorIntensity) * 255.0f) | mColor << 8;
            mColor = (int)((mBaseColors[2] * mColorIntensity) * 255.0f) | mColor << 8;
            mColor |= 0xFF000000;
        }

        public void setPhy(int phy) { mPhy = phy; }

        public int getPhy(){ return mPhy; }

        public void setRssi(int rssi){ mRssi = rssi; }

        public int getRssi(){ return mRssi; }

        public byte []getCommandSetPulse(int colorIndex, float intensity, int delay){
            byte []newCommand = new byte[6];
            newCommand[0] = (byte)OutgoingCommand.SetLedPulse.ordinal();
            newCommand[1] = (byte)mConnHandle;
            newCommand[2] = (byte)colorIndex;
            newCommand[3] = (byte)(intensity * 100.0f);
            newCommand[4] = (byte)((delay >> 8) & 0xFF);
            newCommand[5] = (byte)(delay & 0xFF);
            return newCommand;
        }

        public byte []getCommandResetColor(){
            byte []newCommand = new byte[8];
            int deviceMask = 1 << mConnHandle;
            newCommand[0] = (byte)OutgoingCommand.SetLedColorAll.ordinal();
            newCommand[1] = LedState ? (byte)1 : (byte)0;
            newCommand[2] = (byte)(mSelColorR * mSelColorInt * 255.0f);
            newCommand[3] = (byte)(mSelColorG * mSelColorInt * 255.0f);
            newCommand[4] = (byte)(mSelColorB * mSelColorInt * 255.0f);
            for(int i = 0; i < 3; i++){
                newCommand[5 + i] = (byte)(deviceMask & 0xFF);
                deviceMask >>= 8;
            }
            return newCommand;
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

        public BleDevice findByConnHandle(int connHandle){
            for(int i = 0; i < getCount(); i++){
                if(((BleDevice)getItem(i)).connHandleMatch(connHandle)){
                    return (BleDevice)getItem(i);
                }
            }
            return null;
        }

        public void notifyDataChanged(){
            notifyDataSetChanged();
        }


        @Override
        public View getView(int position, View convertView, ViewGroup viewGroup) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.ble_link_view, viewGroup, false);
            }
            BleDevice currentDevice = (BleDevice)getItem(position);
            Resources res = getContext().getResources();
            ((TextView)convertView.findViewById(R.id.name)).setText(currentDevice.getName());
            String []nameList = {"Unknown!","Blinky","Thingy"};
            if(currentDevice.getType() < nameList.length) {
                //((TextView)convertView.findViewById(R.id.type)).setText(nameList[currentDevice.getType()]);
                switch(currentDevice.getType()){
                    case 1:
                        ((ImageView)convertView.findViewById(R.id.img_type)).setBackground(res.getDrawable(R.drawable.dev_dk));
                        break;
                    case 2:
                        ((ImageView)convertView.findViewById(R.id.img_type)).setBackground(res.getDrawable(R.drawable.dev_thingy));
                        break;
                }

            }
            ((TextView)convertView.findViewById(R.id.connHandle)).setText(String.valueOf(currentDevice.getConnHandle()));
            TextView buttonStateText = (TextView)convertView.findViewById(R.id.btnState);
            buttonStateText.setText(currentDevice.ButtonState ? "On" : "Off");
            buttonStateText.setTextAppearance(getContext(), currentDevice.ButtonState ?
                                                            R.style.FontBleDeviceFieldOn :
                                                            R.style.FontBleDeviceField);
            ((RelativeLayout)convertView.findViewById(R.id.bleDeviceListMainLayout)).setBackground(res.getDrawable(currentDevice.Checked ?
                    R.drawable.ble_device_list_background_checked :
                    R.drawable.ble_device_list_background));

            // Change the color of the LED state drawable
            Drawable ledStateBackground = ((ImageView)convertView.findViewById(R.id.ledState)).getBackground();
            if (ledStateBackground instanceof ShapeDrawable) {
                ((ShapeDrawable)ledStateBackground).getPaint().setColor(currentDevice.LedState ? currentDevice.getColor() : Color.BLACK);
            } else if (ledStateBackground instanceof GradientDrawable) {
                ((GradientDrawable)ledStateBackground).setColor(currentDevice.LedState ? currentDevice.getColor() : Color.BLACK);
            } else if (ledStateBackground instanceof ColorDrawable) {
                ((ColorDrawable)ledStateBackground).setColor(currentDevice.LedState ? currentDevice.getColor() : Color.BLACK);
            }

            String []phyList = {"Invalid!", "1Mbps", "2Mbps", "Invalid!", "Coded"};
            TextView phyStateText = (TextView)convertView.findViewById(R.id.phyState);
            phyStateText.setText(phyList[currentDevice.getPhy()]);

            TextView rssiStateText = (TextView)convertView.findViewById(R.id.rssiState);
            rssiStateText.setText(String.valueOf(currentDevice.getRssi()) + " dBm");
            return convertView;
        }
    }
}
