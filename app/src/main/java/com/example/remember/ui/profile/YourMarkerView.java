package com.example.remember.ui.profile;

import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;


//TODO kannweg
public class YourMarkerView extends MarkerView {

    private TextView tvContent;

    public YourMarkerView(Context context, int layoutResource) {
        super(context, layoutResource);

        // find your layout components
        //tvContent = findViewById(R.id.txtViewData);
    }

    // callbacks everytime the MarkerView is redrawn, can be used to update the
// content (user-interface)
    @Override
    public void refreshContent(Entry e, Highlight highlight) {

        int yint = Float.valueOf(e.getY()).intValue(); //dasselbe nur uncooler
        if (yint != 0) tvContent.setText(String.valueOf(yint));
        else tvContent.setText("0");


        // this will perform necessary layouting
        super.refreshContent(e, highlight);
    }

    private MPPointF mOffset;

    @Override
    public MPPointF getOffset() {

        if(mOffset == null) {
            // center the marker horizontally and vertically
            mOffset = new MPPointF(-(getWidth() / 2), -getHeight());
        }

        return mOffset;
    }}