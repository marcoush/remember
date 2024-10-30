package com.example.remember.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HomeViewModel extends ViewModel {
    private static final String TAG = "HomeViewModel";

    private final MutableLiveData<String> mText;

    public HomeViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("alpha version 10/2023");
    }

    public LiveData<String> getText() {
        return mText;
    }
}