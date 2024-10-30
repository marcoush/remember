package com.example.remember.ui.memo;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MemoViewModel extends ViewModel {
    private static final String TAG = "MemoViewModel";

//1 mtext
    private final MutableLiveData<String> mText;
    public MemoViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("this is memo frag");
    }
    public LiveData<String> getText() {return mText;}



//2



}