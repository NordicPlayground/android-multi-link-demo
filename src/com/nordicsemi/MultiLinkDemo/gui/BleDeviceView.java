package com.nordicsemi.MultiLinkDemo.gui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nordicsemi.MultiLinkDemo.R;

/**
 * Created by too1 on 16-Aug-17.
 */

public class BleDeviceView extends LinearLayout {
    private TextView    mTextViewConnHandle, mTextViewButtonState;
    private Button      mButtonLedState;
    private boolean     mLedOn = false;
    private Drawable    mDrawableLedOn, mDrawableLedOff;

    public BleDeviceView(Context context, ViewGroup topView){
        super(context);
        initializeViews(context);
    }

    public BleDeviceView(Context context, AttributeSet attrs){
        super(context, attrs);
        initializeViews(context);
    }

    public BleDeviceView(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
        initializeViews(context);

    }

    private void initializeViews(Context context){

        /*mDrawableLedOn  = (Drawable)context.getResources().getDrawable(R.drawable.intensitybtn_led_on);
        mDrawableLedOff = (Drawable)context.getResources().getDrawable(R.drawable.intensitybtn_led_off);
        mTextViewConnHandle = (TextView)findViewById(R.id.textConnHandle);
        mTextViewButtonState = (TextView)findViewById(R.id.textButtonState);
        mButtonLedState = (Button)findViewById(R.id.buttonLedState);
        mButtonLedState.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLedOn = !mLedOn;
                mButtonLedState.setText(mLedOn ? "" : "");
                mButtonLedState.setBackground(mLedOn ? mDrawableLedOn : mDrawableLedOff);
            }
        });*/
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.ble_device_view_blinky, null);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();


    }

    public void setConnHandle(int connHandle){
        //mTextViewConnHandle.setText(String.valueOf(connHandle));
    }

    public void setButtonState(String buttonState){
        mTextViewButtonState.setText(buttonState);
    }
}
