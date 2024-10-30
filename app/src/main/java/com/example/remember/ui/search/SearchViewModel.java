package com.example.remember.ui.search;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SearchViewModel extends ViewModel {


//0 show the useless textveil
    private final MutableLiveData<String> mText;

    public SearchViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is search fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }




//1 entered text in sbb edittext
    private MutableLiveData<String> enteredText = new MutableLiveData<>();
    //view model hier ist da, um den eingegebenen search-text im sbb-fragment auszulesen und an das search-fragment weiterzugeben
    public void setEnteredText(String text) {
        enteredText.setValue(text);
    }
    public LiveData<String> getEnteredText() {return enteredText;}




//2 languagefilter weitergabe von new/selectedLanguage aus alertdialog aus sbb
    private MutableLiveData<String> selectedLanguage = new MutableLiveData<>();
    public void setSelectedLanguage(String language) {
        selectedLanguage.setValue(language);
    }
    public LiveData<String> getSelectedLanguage() {
        return selectedLanguage;
    }




//3 visibility manipulation of textview in fragsearch
    private MutableLiveData<Integer> textViewVisibility = new MutableLiveData<>();
    public LiveData<Integer> getTextViewVisibility() {
        return textViewVisibility;
    }
    public void setTextViewVisibility(int visibility) {
        textViewVisibility.setValue(visibility);
    }


//4 visibility manipulation of categoriescontainer in fragsearch
    private MutableLiveData<Integer> categoriesVisibility = new MutableLiveData<>();
    public LiveData<Integer> getCategoriesVisibility() {return categoriesVisibility;}
    public void setCategoriesVisibility(int visibility) {categoriesVisibility.setValue(visibility);}



}