package com.example.remember.ui.search;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;



//war die idee, den bnv mit diesem ding hier links zu überlappen, damit searchicon-click zum öffnen des searchfields führt... hat aber fatal& kläglich agonievoll gescheitert
import com.example.remember.databinding.FragSearchiconBinding;

public class SearchIcon extends Fragment {
    private static final String TAG = "searchicon_frag";
    //idk , muss glaube ich nur existieren



    private FragSearchiconBinding binding;

    //viewmodel für auslesen des edittextes und weitergabe der daten an das search-fragment ;)
    private SearchViewModel searchViewModel;




    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {// Inflate the layout for this fragment
        binding = FragSearchiconBinding.inflate(inflater, container, false);
        View root = binding.getRoot(); //zuvor: View root , jetzt ist View root ausgelagert in scope (für onGlobalLayoutListener xd)
        Log.d(TAG, "onCreateView called in searchicon");




        Log.d(TAG, "onCreateView END in searchicon");
        return root;
    }


}
