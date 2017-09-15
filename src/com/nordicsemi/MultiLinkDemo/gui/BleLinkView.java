package com.nordicsemi.MultiLinkDemo.gui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import com.nordicsemi.MultiLinkDemo.R;

/**
 * Created by too1 on 14-Sep-17.
 */

public class BleLinkView extends View {
    public BleLinkView(Context context) {
        super(context);
    }

    public BleLinkView(Context context, AttributeSet attrs){
        super(context, attrs);
        initializeViews(context);
    }

    public BleLinkView(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
        initializeViews(context);
    }

    private void initializeViews(Context context){

    }

}
