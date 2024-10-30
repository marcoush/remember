package com.example.remember.ui.profile;

import android.util.Log;

import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.Arrays;

public class ValueFormatterXAxisString extends ValueFormatter {
    public static String TAG = "ValueFormatterXAxisString";

    int lastindex = -1;
    private String[] values;

    public ValueFormatterXAxisString(String[] values) { //The constructor takes an array of String values as a parameter and assigns it to the values variable of the class
        this.values = values;
        Log.d(TAG, "values ist:" + Arrays.toString(values));
    }

    //TODO (A) getFormattedValue darf nicht 2x auslösen nacheinander wenn es derselbe index ist... how to prevent that??? weil man muss das auslösen preventen, sonst wird ja was returned...

//bei month war ein unerklärlicher bug, weil 2x index 0 und index 2 retrieved wurde WHHAHYYY -> daher die Abfrage mit indexUsed aber das war auch nicht perfekt, gerade auf suche nach lsg...
    @Override
    public String getFormattedValue(float value) { //receives a float value, which represents the index of the data point (1,2,3,4,5,6,7 in my case)
        int index = (int) value; //converts the float value to an int
        Log.d(TAG, "lastindexist:" + lastindex);
        Log.d(TAG, "index   ist:" + index);
        //wenn der index eben schon war, return nix
        if (index==lastindex) return "";
        //wenn der index ein neuer war -> lastindex updaten
        lastindex = index;

        //Check if the index is within a valid range
        if (index >= 0 && index < values.length) { //checks if int falls within the valid range of the values array & that this index hasn't been used before
            Log.d(TAG, "der ValueFormatterXAxisString baut den values[index] folgendermaßen:" + values[index]); //TODO (unzufr.) this log gets called 4 times ... babuschkaaa25.6.
            return values[index];//retrieves the corresponding label from the values array using values[index] and returns it as the formatted value for that data point
        }

        return ""; //index is invalid, meaning it is outside the range of the values array -> return handgrenade bomba
    }

}

/*        //Check if the index is within a valid range
        if (index >= 0 && index < values.length && !isIndexUsed(index)) { //checks if int falls within the valid range of the values array & that this index hasn't been used before
            usedIndexesInValueFormatterXAxis.add(index);  // Add the index to the used indexes set
            Log.d(TAG, "der ValueFormatterXAxisString baut den values[index] folgendermaßen:" + values[index]); //TODO (unzufr.) this log gets called 4 times ... babuschkaaa25.6.
            return values[index];//retrieves the corresponding label from the values array using values[index] and returns it as the formatted value for that data point
        }

        return ""; //index is invalid, meaning it is outside the range of the values array -> return handgrenade bomba
    }
    // Track used indexes to avoid duplicates
    public static Set<Integer> usedIndexesInValueFormatterXAxis = new HashSet<>();

    // Helper method to check if an index has already been used
    private boolean isIndexUsed(int index) {
        return usedIndexesInValueFormatterXAxis.contains(index);
    }
*/