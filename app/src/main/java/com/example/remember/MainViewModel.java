package com.example.remember;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.remember.recycler_and_scrollviews.Model;

import java.util.ArrayList;
import java.util.List;

public class MainViewModel extends ViewModel {



//1 get all the metadata when a memo is opened
    private ArrayList<String> memoMetadataList;
    public void setMemoMetadata(ArrayList<String> memoMetadataList) {this.memoMetadataList = memoMetadataList;}
    public ArrayList<String> getMemoMetadata() {return memoMetadataList;}

//2 retrieve onlinedata in Main and read them out as livedata in frags
//HOME
    //foryou
    private MutableLiveData<List<Model>> modelListForyouLiveData = new MutableLiveData<>();
    public void setModelListforyou(List<Model> modelList) {modelListForyouLiveData.setValue(modelList);}
    public LiveData<List<Model>> getModelListForyou() {return modelListForyouLiveData;}
    //recent
    private MutableLiveData<List<Model>> modelListRecentLiveData = new MutableLiveData<>();
    public void setModelListrecent(List<Model> modelList) {
        modelListRecentLiveData.setValue(modelList);
    }
    public LiveData<List<Model>> getModelListRecent() {return modelListRecentLiveData;}
    //search    //TODO 11.1. new:!!!!
    private MutableLiveData<List<Model>> modelListSearchLiveData = new MutableLiveData<>();
    public void setModelListSearch(List<Model> modelList) {modelListSearchLiveData.setValue(modelList);}
    public LiveData<List<Model>> getModelListSearch() {return modelListSearchLiveData;}

    //set home foryou progressBar visibility from CatQueries.java over VM (if query doesn't go to end)
    private MutableLiveData<Integer> progressBarVisibilityforyou = new MutableLiveData<>();
    public LiveData<Integer> getProgressBarVisibilityforyou() {return progressBarVisibilityforyou;}
    public void setProgressBarVisibilityforyou(int visibility) {progressBarVisibilityforyou.setValue(visibility);}
    //set home recent progressBar visibility from RecentQuery.java over VM (if query doesn't go to end)
    private MutableLiveData<Integer> progressBarVisibilityrecent = new MutableLiveData<>();
    public LiveData<Integer> getProgressBarVisibilityrecent() {return progressBarVisibilityrecent;}
    public void setProgressBarVisibilityrecent(int visibility) {progressBarVisibilityrecent.setValue(visibility);}
    //set searchFrag search progressBar visibility from CatQueries.java over VM (if query doesn't go to end)
    private MutableLiveData<Integer> progressBarVisibilitysearch = new MutableLiveData<>();
    public LiveData<Integer> getProgressBarVisibilitysearch() {return progressBarVisibilitysearch;}
    public void setProgressBarVisibilitysearch(int visibility) {progressBarVisibilitysearch.setValue(visibility);}


    
    
    /*//update rv from CatQueries.java over VM (after query is over)
    private MutableLiveData<Boolean> adapterChange = new MutableLiveData<>();
    public LiveData<Boolean> getAdapterChange() {return adapterChange;}
    public void setAdapterChange(boolean changed) {adapterChange.setValue(changed);}*/ //included into displayforyoumemos



   /* //main bottomSheetBehavior.addBottomSheetCallback dragging ➝ manipulate gray scale in memoFrag
    private MutableLiveData<Float> grayscaleOfCollapsedLLinmemoFrag = new MutableLiveData<>();
    public LiveData<Float> getGrayscaleOfCollapsedLLinmemoFrag() {return grayscaleOfCollapsedLLinmemoFrag;}
    public void setGrayscaleOfCollapsedLLinmemoFrag(float offset) {grayscaleOfCollapsedLLinmemoFrag.setValue(offset);}*/ //kann direkt in Main gemacht werden... lol





//SEARCH

//PROFILE


/*//1.1 title //der ganze scheiß hier ist NATÜRLICH nicht nötig LOL!!! schöön einfach die arraylist nehmen und auslesen mit get(8), get(7), ...
    private String title;
    //view model hier ist da, um den eingegebenen search-text im sbb-fragment auszulesen und an das search-fragment weiterzugeben
    public void setMemotitle(String text) {this.title = text;}
    public String getMemotitle() {return title;}
//2.2 duration
    private String duration;
    //view model hier ist da, um den eingegebenen search-text im sbb-fragment auszulesen und an das search-fragment weiterzugeben
    public void setMemoduration(String text) {this.duration = text;}
    public String getMemoduration() {return duration;}
//2.3 creator
    private String creator;
    //view model hier ist da, um den eingegebenen search-text im sbb-fragment auszulesen und an das search-fragment weiterzugeben
    public void setMemocreator(String text) {this.creator = text;}
    public String getMemocreator() {return creator;}
//2.4 date
    private String date;
    //view model hier ist da, um den eingegebenen search-text im sbb-fragment auszulesen und an das search-fragment weiterzugeben
    public void setMemodate(String text) {this.date = text;}
    public String getMemodate() {return date;}
//2.5 listeners
    private String listeners;
    //view model hier ist da, um den eingegebenen search-text im sbb-fragment auszulesen und an das search-fragment weiterzugeben
    public void setMemolisteners(String text) {this.listeners = text;}
    public String getMemolisteners() {return listeners;}
//2.6 categories
    private String categories;
    //view model hier ist da, um den eingegebenen search-text im sbb-fragment auszulesen und an das search-fragment weiterzugeben
    public void setMemocategories(String text) {this.categories = text;}
    public String getMemocategories() {return categories;}
//2.7 language
    private String language;
    //view model hier ist da, um den eingegebenen search-text im sbb-fragment auszulesen und an das search-fragment weiterzugeben
    public void setMemolanguage(String text) {this.language = text;}
    public String getMemolanguage() {return language;}
//2.8 url
    private String url;
    //view model hier ist da, um den eingegebenen search-text im sbb-fragment auszulesen und an das search-fragment weiterzugeben
    public void setMemourl(String text) {this.url = text;}
    public String getMemourl() {return url;}
//2.9 id
    private String id;
    //view model hier ist da, um den eingegebenen search-text im sbb-fragment auszulesen und an das search-fragment weiterzugeben
    public void setMemoid(String text) {this.id = text;}
    public String getMemoid() {return id;}*/




}