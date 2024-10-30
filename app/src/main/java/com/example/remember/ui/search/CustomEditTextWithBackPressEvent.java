package com.example.remember.ui.search;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Objects;

public class CustomEditTextWithBackPressEvent extends androidx.appcompat.widget.AppCompatEditText {

    private MyEditTextListener onBackPressListener;

    public CustomEditTextWithBackPressEvent(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setOnBackPressListener(MyEditTextListener onBackPressListener) {
        this.onBackPressListener = onBackPressListener;
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            //back button pressed
            if (Objects.requireNonNull(ViewCompat.getRootWindowInsets(getRootView())).isVisible(WindowInsetsCompat.Type.ime())) {
                //keyboard is open
                onBackPressListener.callback();
            }
            return false;
        }
        return super.dispatchKeyEvent(event);
    }

    public interface MyEditTextListener {
        void callback();
    }
}