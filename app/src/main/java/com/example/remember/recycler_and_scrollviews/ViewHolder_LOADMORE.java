package com.example.remember.recycler_and_scrollviews;

import android.view.View;
import android.widget.Button;

import androidx.recyclerview.widget.RecyclerView;

import com.example.remember.R;

public class ViewHolder_LOADMORE extends RecyclerView.ViewHolder/* implements View.OnClickListener*/ {
    private static final String TAG = "ViewHolder_LOADMORE";

    //nur der button..:..::.
    Button loadmorebutton;

    public ViewHolder_LOADMORE(View itemView) {
        super(itemView);
        loadmorebutton = itemView.findViewById(R.id.loadmorebuttonid);
    }



    /*
    View view;
    private ViewHolder_LOADMORE.ClickListener clickListener;

    public ViewHolder_LOADMORE(@NonNull View itemView) {
        super(itemView);
        view = itemView;

//0 initialize views with recyclerview_model.xml
        loadmorebutton = itemView.findViewById(R.id.loadmorebuttonid);


        itemView.setOnClickListener(this);
    }


    @Override
    public void onClick(View view) {
        Log.d(TAG, "itemView onClick called");
        // Handle the click event for the itemView
        if (clickListener != null) {
            Log.d(TAG, "onClick: clickListener != null");
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

    public void setClickListener(ViewHolder_LOADMORE.ClickListener clickListener) {
        this.clickListener = clickListener;
    }
    */



}
