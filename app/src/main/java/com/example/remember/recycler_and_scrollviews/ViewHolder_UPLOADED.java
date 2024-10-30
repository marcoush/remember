package com.example.remember.recycler_and_scrollviews;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.remember.R;


public class ViewHolder_UPLOADED extends RecyclerView.ViewHolder implements View.OnClickListener {
    private static final String TAG = "ViewHolder_UPLOADED";

    //hier werden die textviews initiiert, in denen die aus dem CustomAdapter eingelesenen daten angezeigt werden im frontend
    TextView titletextview, durationtextview, creatortextview, datetextview, categoriestextview, listeners, listenerstextview, listenerstexttextview;
    ImageView languageflag, clock, calendar, headphone;
    //hsv und der darin liegende horizontally oriented ll container für
    LinearLayout categorycontainerrow1, categorycontainerrow2;    //alt: HorizontalScrollView categoryhsv; LinearLayout categorycontainer;
    View view;
    private ClickListener clickListener;

    public ViewHolder_UPLOADED(@NonNull View itemView) {
        super(itemView);
        view = itemView;

//0 initialize views with recyclerview_model_uploaded.xml (kein creator, weil man selbst der creator ist)
        titletextview = itemView.findViewById(R.id.titleid);
        durationtextview = itemView.findViewById(R.id.durationid);
        datetextview = itemView.findViewById(R.id.dateid);
        languageflag = itemView.findViewById(R.id.languageflagid);
        calendar = itemView.findViewById(R.id.calendarid);
        clock = itemView.findViewById(R.id.clockid);
        headphone = itemView.findViewById(R.id.headphoneid);
        listeners = itemView.findViewById(R.id.listenersid);
        categorycontainerrow1 = itemView.findViewById(R.id.llcategorycontainerrow1id);
        categorycontainerrow2 = itemView.findViewById(R.id.llcategorycontainerrow2id);
        //categoryhsv = itemView.findViewById(R.id.hsvmodelid);
        //languageflag = itemView.findViewById(R.id.languageflagid);
        //categoriestextview = itemView.findViewById(R.id.categoriesid);


        itemView.setOnClickListener(this);
    }



    @Override
    public void onClick(View view) {
        Log.d(TAG, "itemView onClick");
        // Handle the click event for the itemView
        if (clickListener != null) {
            int position = getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                clickListener.onItemClick(view, position);
            }
        }
    }

    public interface ClickListener {
        void onItemClick(View view, int position);
        void onItemLongClick(View view, int position);
    }

    public void setClickListener(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

}
