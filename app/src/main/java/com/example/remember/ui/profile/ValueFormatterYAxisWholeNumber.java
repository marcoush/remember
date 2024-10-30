package com.example.remember.ui.profile;

import com.github.mikephil.charting.formatter.ValueFormatter;

public class ValueFormatterYAxisWholeNumber extends ValueFormatter {
    @Override
    public String getFormattedValue(float value) {
        if (value == 0.0f || value % 1.0f != 0.0f) {
            return ""; // Return empty string for non-whole numbers or zero
        } else {
            return String.valueOf((int) value);
        }
    }
}
