package com.example.remember.ui.profile;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class ProfileViewModel extends ViewModel {
    private static final String TAG = "Profile_VIEWMODEL";

//0 unnötiges textivew
    private final MutableLiveData<String> mText;
    public ProfileViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is profile fragment");
    }
    public LiveData<String> getText() {
        return mText;
    }

/*
//1 übergabe der Lists an Graph, GraphMonth & co kg ERSTMAL UNSINN QUATSCHO
// private MutableLiveData<String> labomba = new MutableLiveData<String>();
    public void set(String input) { price.setValue(input);  }
    public LiveData<String> get() { return price; }
*/ //COUADXO


//graph type in graphFrag
    private MutableLiveData<String> graphType = new MutableLiveData<>();
    public void setGraphtype(String input) { graphType.setValue(input); Log.d(TAG, "setGraphtype called in viewmodel (graphtype: " + input + ")");}
    public LiveData<String> getGraphtype() { Log.d(TAG, "getGraphtype called in viewmodel , graphType: " + graphType); return graphType;} //graphType: "uploads_week", "...
    //im prinzip muss durch den onbuttonclick nur ein Signal gegeben werden an das graph fragment, sodass getogglet zw. uploads/listenings & week,month,sixmonths werden soll..

//userData (hier: datesofuploadsList & datesoflisteningsList) wird ans graphFrag weitergegeben, damit nur 1x gequeriet werden muß
    private MutableLiveData<ArrayList<List<String>>> userData = new MutableLiveData<>();
    public void setUserData(ArrayList<List<String>> input) { userData.setValue(input); Log.d(TAG, "setUserData called in viewmodel (userData: " + input + ")");}
    public LiveData<ArrayList<List<String>>> getUserData() { Log.d(TAG, "getUserData called in viewmodel , userData: " + userData); return userData;}







    /*//stockprice
    private MutableLiveData<Float> price = new MutableLiveData<Float>();
    public void setPrice(Float input) { price.setValue(input); }
    public LiveData<Float> getPrice() { return price; }

//stockchanges
    // pos
    private MutableLiveData<CharSequence> poschange = new MutableLiveData<>();
    public void setPosChange(CharSequence input) { poschange.setValue(input); }
    public LiveData<CharSequence> getPosChange() { return poschange; }
    // neg
    private MutableLiveData<CharSequence> negchange = new MutableLiveData<>();
    public void setNegChange(CharSequence input) { negchange.setValue(input); }
    public LiveData<CharSequence> getNegChange() { return negchange; }
    // 0
    private MutableLiveData<CharSequence> nochange = new MutableLiveData<>();
    public void setNoChange(CharSequence input) { nochange.setValue(input); }
    public LiveData<CharSequence> getNoChange() { return nochange; }

//absolute stockchanges
    // pos
    private MutableLiveData<CharSequence> posabschange = new MutableLiveData<>();
    public void setPosAbsChange(CharSequence input) { posabschange.setValue(input); }
    public LiveData<CharSequence> getPosAbsChange() { return posabschange; }
    // neg
    private MutableLiveData<CharSequence> negabschange = new MutableLiveData<>();
    public void setNegAbsChange(CharSequence input) { negabschange.setValue(input); }
    public LiveData<CharSequence> getNegAbsChange() { return negabschange; }
    // 0
    private MutableLiveData<CharSequence> noabschange = new MutableLiveData<>();
    public void setNoAbsChange(CharSequence input) { noabschange.setValue(input); }
    public LiveData<CharSequence> getNoAbsChange() { return noabschange; }

//buyamount
    private MutableLiveData<CharSequence> buyamount = new MutableLiveData<>();
    public void setBuyAmount(CharSequence input) { buyamount.setValue(input); }
    public LiveData<CharSequence> getBuyAmount() { return buyamount; }
//sellamount
    private MutableLiveData<CharSequence> sellamount = new MutableLiveData<>();
    public void setSellAmount(CharSequence input) { sellamount.setValue(input); }
    public LiveData<CharSequence> getSellAmount() { return sellamount; }*/ //stuff from telechargexDlmaoTrading




}