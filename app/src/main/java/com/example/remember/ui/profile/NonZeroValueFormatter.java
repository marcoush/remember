package com.example.remember.ui.profile;

import android.util.Log;

import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;



public class NonZeroValueFormatter extends ValueFormatter {
    private static final String TAG = "NonZeroValueFormatter (y values)";

    @Override
    public String getFormattedValue(float value) {
        if (value != 0.02f) { //0.1f ^= 0 (weil ich hab ja 0.1 aus 0 gemacht, damit man einen kleinen balken sieht bei 0)
            Log.d(TAG,"y value (" + value + ") ist nicht 0 -> Anzeige im graphen");
            return String.valueOf((int) value); // Convert float to int and return as string
        } else {
            Log.d(TAG,"y value (" + value + ") ist 0 -> keine Anzeige im graphen");
            return ""; // Return an empty string for zero values
        }
    }

    @Override
    public String getBarLabel(BarEntry barEntry) {
        // Customize the label for each bar (optional)
        return getFormattedValue(barEntry.getY());
    }
}