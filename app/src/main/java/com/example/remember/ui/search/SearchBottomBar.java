package com.example.remember.ui.search;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.remember.R;
import com.example.remember.databinding.FragSearchBottombarBinding;

import java.util.Locale;

public class SearchBottomBar extends Fragment  { //implements GestureDetector.OnGestureListener jajajaj chat
    private static final String TAG = "sbb_frag";

    //binding besser als findviewbyid & generell besser
    private FragSearchBottombarBinding binding;

    //viewmodel für auslesen des edittextes und weitergabe der daten an das search-fragment ;)
    private SearchViewModel searchViewModel;

    //inputmanager fürs grand keyboard opening :D
    private InputMethodManager inputMethodManager;
    EditText sbbedittext; //sbb = search bottom bar
    ImageButton languagefilter;
    //der allseits beliebte boolean isenglishflag kommt auch hier zum vorschein:
    private boolean isEnglishFlag; //TODO (info) also wenn isEnglishFlag=true, dann wird bei der query nur gequeried, wenn das field "language" = "en" ist... & vice versa
    String language, enteredText;

    //damit man aus der tastatur rausswipen kann bei
    //SwipeListener swipeListener;
    //TODO (A) wenn funktioniert, auch bei Publish activity (edittext dort) einbauen
    //chat's way of doing it:
    //tragischer chat versuch 0/3, swipe gesture zu generaten, hat allerigns null funktinioert
    //private GestureDetector swipeDetector;
    //TODO (info) es gibt so viele wege, das keyboard zu schließen: back-button, swipe, keyboard links unten knopf zum schließen, enter drücken... idk maybe it's better to abhör the open/closed state of the keyboarde
    //stattdessen idee, keyboard state abzuhöreN 1/4:
    private View root;
    //private ViewTreeObserver.OnGlobalLayoutListener keyboardVisibilityListener; //shish es funktioniert!
    //determine whether the edittext has focus in order to shut uff global layout listener if needed
    private boolean isSbbEditTextFocused;
    private ViewTreeObserver.OnGlobalLayoutListener globalLayoutListener;
    //neuer versuch mit window insets listener (inside the onfocuschangelistener)
    private View.OnApplyWindowInsetsListener windowInsetsListener;



    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {// Inflate the layout for this fragment
        binding = FragSearchBottombarBinding.inflate(inflater, container, false);
        root = binding.getRoot(); //zuvor: View root , jetzt ist View root ausgelagert in scope (für onGlobalLayoutListener xd)
        Log.d(TAG, "onCreateView called in sbb");
        //big nothing this only exists because chat gpt i wan ttot try that outö.. NAJA GEHT SO

//-1 ui
        sbbedittext = binding.searchbottombaredittextid;
        languagefilter = binding.languagefilterid;
        //instantly request focus on the edittext when this fragment is created! TODO (A) wird das immer ausgelöst , oder nur 1x?
       // requestFocusOnEditText();

//0 inititate viewmodel & connection to main
        this.searchViewModel = new ViewModelProvider(requireActivity()).get(SearchViewModel.class);
        //zugriff auf main using mainActivityRootView & mainActivityContainer, um das Main Layout zu inflaten:
        //der BNV ist in Main, also outside the frag, therefore muss ich AUFM AIN ZUGREIFEN !
        // Get the root view of the main activity & Find the container in the main activity's layout
       // View mainActivityRootView = requireActivity().findViewById(android.R.id.content);
       // ViewGroup mainActivityContainer = mainActivityRootView.findViewById(R.id.relativelayoutmainid); //wurde genutzt vorher für

//1 keybord
        Log.d(TAG,"open keyboard & request focus in sbbedittext:");
        inputMethodManager = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE); //TODO (C) context könnte sein dass nicht funktiniert
        //sbbedittext.requestFocus();
        inputMethodManager.showSoftInput(sbbedittext, InputMethodManager.SHOW_IMPLICIT);

//2 sbb swipe listener
        //damit auf Pixel4a swipe die tastatur schließt
        //swipeListener = new SwipeListener(sbbedittext);
        // Initialize the GestureDetector
        //tragischer chat versuch 1/3, swipe gesture zu generaten, hat allerigns null funktinioert
        //swipeDetector = new GestureDetector(requireContext(), this);
        //stattdessen idee, keyboard state abzuhöreN 2/4:
        // Set up the keyboard visibility listener
        //setupKeyboardVisibilityListener(); //führt hier zu NPE, weil frag view noch nicht initiated (ist ja onCreateView) -> daher in onViewCreated..


//2 sbbedittext onclick
        binding.searchbottombaredittextid.setOnClickListener(v -> { //sicherheitshalber durchs binding ersetzt... gab ja sonst probleme iwie weil custom edittext und pipoaop
            Log.d(TAG, "sbbedittext onClick ausgelöst");

        });
//3 sbbedittext ontextchangedlistener
        binding.searchbottombaredittextid.addTextChangedListener(new TextWatcher() { //INFO wenn man binding.searchbottombaredittextid durch sbbedittext substituted, gibt's in der zeile searchViewModel.setEnteredText(sbbedittext.getText().toString().trim()); ne null object reference
            //ich glaube das liegt daran, dass es ein custom edittext ist und man den unbedingt übers binding aufruft !! genauso beim onClickListener !
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Log.d(TAG, "beforeTextChanged ausgelöst");
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Log.d(TAG, "onTextChanged hitted");
                //TODO (B) hier user-friendly d e  l    a   y einbauen, sodass nicht direkt die suche poppt?
                //übers viewmodel wird entered text weitergegeben .. und das search-frag liest es dann ein und gönnt sich update von row1feelings ... etc.
              /*  if (binding.searchbottombaredittextid.getText() != null) { //TODO (A) hier kommt dauernd null object reference
                    searchViewModel.setEnteredText(binding.searchbottombaredittextid.getText().toString().trim());
                }*/
                //TODO (A) manchmal wird der onTextChanged Listener random ausgelöst ???
                //TODO (A) wenn das hier aktiv ist, wird das suchergebnis 100000 mal angezeeigt , und es macht auch sonst problerme! D;
            }
            @Override
            public void afterTextChanged(Editable editable) {
                Log.d(TAG, "Searchbox has changed to: " + editable.toString());
                //übers viewmodel wird entered text weitergegeben .. und das search-frag liest es dann ein und gönnt sich update von row1feelings ... etc.
                searchViewModel.setEnteredText(sbbedittext.getText().toString().trim());
            }
        });



        //TODO (A) temporarily shut this down to see if onGlobalLayout Listener works on its own
//4 sbbedittext EDITOR ACTION
        sbbedittext.setOnEditorActionListener((v, actionId, event) -> {
            Log.d(TAG, "sbbedittext onEditorAction ausgelöst");
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                //1 Close the keyboard
                inputMethodManager.hideSoftInputFromWindow(sbbedittext.getWindowToken(), 0);
             //(info) der folgende part wird nun in onGlobalLayoutlistener abgewickelt
             /*   //2 initialize the view of BNV & show the bnv again :) TODO (info) den folg. code kann man nicht beim onCreateView setzen, weil sonst null obj ref bei bnvmain.setvisibil...
                View bnvmain = mainActivityContainer.findViewById(R.id.bottomnavigationid);
                bnvmain.setVisibility(View.VISIBLE);
                //3 show the textview & categories in fragsearch again
                searchViewModel.setTextViewVisibility(View.VISIBLE);
                searchViewModel.setCategoriesVisibility(View.VISIBLE);*/
                Log.d(TAG, "ENTER: textview in search frag sollte jetzt wieder visible sein");
                Log.d(TAG, "ENTER: cateogories in search frag sollten jetzt wieder visible sein");
                return true;
            }
            return false;
        });

        //Initialize the windowInsetsListener in the onViewCreated method
        windowInsetsListener = (v, insets) -> {
            Log.d(TAG, "windowInsetsListener called (also sbbedittext hasFocus, aber window hat sich trotzdem geändert zB durch swipen oder keyboard folding button)");
            // Handle changes in window insets
            Rect windowVisibleDisplayFrame = new Rect();
            v.getWindowVisibleDisplayFrame(windowVisibleDisplayFrame);

            // Check if the top position of the fragment has changed
            int topPosition = windowVisibleDisplayFrame.top;
            if (topPosition != 0) {
                Log.d(TAG, "windowInsetsListener: frag position changed to top");

                // The fragment's position has changed to the top, indicating the keyboard is open
                //2 initialize the view of BNV & show the bnv again :) TODO (info) den folg. code kann man nicht beim onCreateView setzen, weil sonst null obj ref bei bnvmain.setvisibil...
                View mainActivityRootView = requireActivity().findViewById(android.R.id.content);
                ViewGroup mainActivityContainer = mainActivityRootView.findViewById(R.id.relativelayoutmainid); //wurde genutzt vorher für
                View bnvmain = mainActivityContainer.findViewById(R.id.bottomnavigationid);
                bnvmain.setVisibility(View.VISIBLE);
                //3 show the textview & categories in fragsearch again
                searchViewModel.setTextViewVisibility(View.VISIBLE);
                searchViewModel.setCategoriesVisibility(View.VISIBLE);

            } else {
                Log.d(TAG, "windowInsetsListener: frag position hasn't changed to top");
                // The fragment's position has not changed, indicating the keyboard is closed

            }
            // Return the original insets
            return insets;
        };


//4 sbbedittext focus listener & ongloballayoutlistener für keyboard close
        sbbedittext.setOnFocusChangeListener((view, hasFocus) -> { //dieser focus listener wird mit hasFocus initiiert, also beim 1. Mal wird direkt festgelegt, dass sbbedittext focus hat!
            Log.d(TAG, "sbbedittext focus has changed");
            Log.d(TAG, "hasFocus ist: " + hasFocus);
            //dieser boolean, damit nicht beide if-clauses auslösen
            if (hasFocus) { //&& focushasbeenchanged[0] == false
                // Update the focus state
                isSbbEditTextFocused = true;
                Log.d(TAG, "sbbedittext has gained focus (keyboard = open) --> register the window insets listener:");
                //if the keyboard closes now and the edittext keeps focus ->


             /*   final int[] previousRootViewHeight = {root.getHeight()};
                Log.d(TAG, "rootviewheight outside globallayoutlistener is: " + previousRootViewHeight[0]);
                globalLayoutListener = () -> {
                    Log.d(TAG, "global layout has changed");
                    //when has focus and global layout changed without sbbedittext losing focus, the keyboard folding button has been clicked-> if current height < previous height
                    int currentRootViewHeight = root.getHeight();
                    int heightDifference = previousRootViewHeight[0] - currentRootViewHeight;
                    previousRootViewHeight[0] = currentRootViewHeight;
                    //threshold dafür, dass tastatur geschl. wurde: Verringerung von über 10% of the currentRootViewHeight
                    int threshold = (int) (currentRootViewHeight * 0.1);
                    Log.d(TAG, "previous height: " + previousRootViewHeight[0]);
                    Log.d(TAG, "current height: " + currentRootViewHeight);
                    Log.d(TAG, "height diff: " + heightDifference);
                    Log.d(TAG, "threshold (1/10 von current height): " + threshold);

                    // Check if the height difference is larger than the 10% and perform the necessary actions (code X)
                    if (heightDifference > threshold) {
                        Log.d(TAG, "current height >10% smaller than previous height --> keyboard has been closed over folding button");
                        //2 initialize the view of BNV & show the bnv again :) TODO (info) den folg. code kann man nicht beim onCreateView setzen, weil sonst null obj ref bei bnvmain.setvisibil...
                        View mainActivityRootView = requireActivity().findViewById(android.R.id.content);
                        ViewGroup mainActivityContainer = mainActivityRootView.findViewById(R.id.relativelayoutmainid); //wurde genutzt vorher für
                        View bnvmain = mainActivityContainer.findViewById(R.id.bottomnavigationid);
                        bnvmain.setVisibility(View.VISIBLE);
                        //3 show the textview & categories in fragsearch again
                        searchViewModel.setTextViewVisibility(View.VISIBLE);
                        searchViewModel.setCategoriesVisibility(View.VISIBLE);
                        //focushasbeenchanged[0] = true;
                 /*       //4 falls mittlerweile aus irgendwelchen Gründen der focus für sbbedittext verloren wurde...
                        if (!sbbedittext.hasFocus()) {
                            Log.d(TAG, "in if(hasFocus): sbbedittext hat am Ende irgendwie keinen focus mehr... daher neu requesten:");
                            sbbedittext.requestFocus();
                        }*/


                // Set the window insets listener to the root view
                root.setOnApplyWindowInsetsListener(windowInsetsListener);




          /*      Log.d(TAG, "current height >10% smaller than previous height --> keyboard has been closed over folding button");
                //2 initialize the view of BNV & show the bnv again :) TODO (info) den folg. code kann man nicht beim onCreateView setzen, weil sonst null obj ref bei bnvmain.setvisibil...
                View mainActivityRootView = requireActivity().findViewById(android.R.id.content);
                ViewGroup mainActivityContainer = mainActivityRootView.findViewById(R.id.relativelayoutmainid); //wurde genutzt vorher für
                View bnvmain = mainActivityContainer.findViewById(R.id.bottomnavigationid);
                bnvmain.setVisibility(View.VISIBLE);
                //3 show the textview & categories in fragsearch again
                searchViewModel.setTextViewVisibility(View.VISIBLE);
                searchViewModel.setCategoriesVisibility(View.VISIBLE);*/

                //TODO (A) how do i remove the same globallyoutlistener again when the fragment is destroyed? -> zurück nach HOME destryoed/stoppt sbb nicht, weil bound to Main...

            } else {
                // Update the focus state
                isSbbEditTextFocused = false;
                Log.d(TAG, "sbbedittext hast lost focus and the keyboard must have been exited (e.g. through back-button, swipe or ENTER) --> ");
                root.setOnApplyWindowInsetsListener(null);
                //2 initialize the view of BNV & show the bnv again :) TODO (info) den folg. code kann man nicht beim onCreateView setzen, weil sonst null obj ref bei bnvmain.setvisibil...
                View mainActivityRootView = requireActivity().findViewById(android.R.id.content);
                ViewGroup mainActivityContainer = mainActivityRootView.findViewById(R.id.relativelayoutmainid); //wurde genutzt vorher für
                View bnvmain = mainActivityContainer.findViewById(R.id.bottomnavigationid);
                bnvmain.setVisibility(View.VISIBLE);
                //3 show the textview & categories in fragsearch again
                searchViewModel.setTextViewVisibility(View.VISIBLE);
                searchViewModel.setCategoriesVisibility(View.VISIBLE);
                //remove window insets listeneeeer
                root.setOnApplyWindowInsetsListener(null);
            }


            Log.d(TAG, "sbbedittext focus has changed END");
        });

//(info) OBP durch swipe wird DOCH erkannt vom smartphone! nur die custom method irgendwie ned
/*//5 edittext keyboard close when back-button
        binding.searchbottombaredittextid.setOnBackPressListener(() -> {//omg der scheiß funktioniert XD
            //handle clickyour code here -> Close the keyboard when back-button is hit while in the keyboard (doesn't trigger usual onbackpressed of activity main ... mainly bc. of SH IT)
            //inputMethodManager.hideSoftInputFromWindow(sbbedittext.getWindowToken(), 0); wird eh geschlossen, wenn bakc-button gedrückt wird, wäre ads dplt.
            //1 initialize the view of BNV & show the bnv again :)
            View bnvmain = mainActivityContainer.findViewById(R.id.bottomnavigationid);
            bnvmain.setVisibility(View.VISIBLE);
            //2 show the textview & categories in fragsearch again
            searchViewModel.setTextViewVisibility(View.VISIBLE);
            searchViewModel.setCategoriesVisibility(View.VISIBLE);
            Log.d(TAG, "BACK PRESS: textview in search frag sollte jetzt wieder visible sein");
            Log.d(TAG, "BACK PRESS: categories in search frag sollten jetzt wieder visible sein");
        });*/

//6 languagefilter
        languagefilter.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.flag_color_one_nightmode)); //TODO (unzufr.) wg NPE gefahr =?="?=Q§4023e
        languagefilter.setScaleType(ImageView.ScaleType.FIT_XY);
        //flagge anmalen initialization according to language: (darf in xml nicht in android:src stehen, sonst wäre dies der standard view und ein setBackground würde unangehem überlappen uff)
        language = Locale.getDefault().getLanguage();// Retrieve the app language when activity is started
        switch (language) {
            case "de":
                System.out.println("lang = de, daher flagge des Großdeutschen Reiches");
                languagefilter.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.germanflag));
                isEnglishFlag = false;

                break;
            case "en":
                System.out.println("lang = en, daher Großbrittanien flagge");
                languagefilter.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.englishflag));
                isEnglishFlag = true;
                break;
            default:  //irgendeine ANDERE sprache, die auf dem handy läuft (wie bei mir random italienisch) -> TODO (info / unzufr.) einfach english als standard weil das ausländer können
                languagefilter.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.englishflag));
                isEnglishFlag = true;
                break;
        }
        languagefilter.setOnClickListener(v -> {
            showLanguageFilterDialog();
        });

//7 zuvor eingebenen text wiederbekommen -> restore from saved instance state
        if (savedInstanceState != null) {
            Log.d(TAG, "savedinstancestate!=null, also speist sbbedittext mit dem zuvor entered text:");
            enteredText = savedInstanceState.getString("enteredText");
            sbbedittext.setText(enteredText);
        }



//8 observer        observeeeer         observeeeeeeeeeer              observeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeer                             observeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeer
        //4.1 language
      /*  this.searchViewModel.getSelectedLanguage().observe(getViewLifecycleOwner(), selectedLanguage -> {
            // Handle the selected language change here
            // You can access the updated selected language value using the "selectedLanguage" parameter
            // Perform your desired actions with the new language value
            //TODO (A) alle bisherigen suchergebnisse müssen neu gefiltert werden / oder es wird einfach generell neu gequeriet!
        });*/

        Log.d(TAG, "onCreateView END in sbb");
        return root;
    }



    @Override
    public void onDetach() {
        super.onDetach();
        // Remove the global layout listener when the fragment is detached from the activity
        if (globalLayoutListener != null) {
            root.getViewTreeObserver().removeOnGlobalLayoutListener(globalLayoutListener);
        }
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop called");

        Log.d(TAG, "onStop END");
        super.onStop();
    }
    //TODO (A) onstop oder ondestroyview lösen ned mal aus, wenn man zurück nach HOME geht -> irgendwie müsste ich sbbfrag mit dem fragment binden, aber ich weiß nicht wie, momentan binde
    // ich es ja mit Main und Main wird an bleiben, wenn ich zurück nach HOME gehe... :(

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView called");
        //stattdessen idee, keyboard state abzuhöreN 3/4:
        // Remove the keyboard visibility listener when the fragment view is destroyed
        //removeKeyboardVisibilityListener();
        Log.d(TAG, "onDestroyView END");
        super.onDestroyView();
    }

/*
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated called");

        // Set up the keyboard visibility listener
        setupKeyboardVisibilityListener();
        Log.d(TAG, "onViewCreated END");

    }




    //stattdessen idee, keyboard state abzuhöreN 4/4:
    private void setupKeyboardVisibilityListener() {
        Log.d(TAG, "setupKeyboardVisibilityListener called");
        keyboardVisibilityListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            private final Rect rect = new Rect();
            private boolean keyboardVisible;

            @Override
            public void onGlobalLayout() { //invoked when the global layout state or the visibility of views within the view tree changes
                Log.d(TAG, "onGlobalLayout called");
                root.getWindowVisibleDisplayFrame(rect);
                int screenHeight = root.getRootView().getHeight();
                int keyboardHeight = screenHeight - rect.bottom;

                if (keyboardHeight > screenHeight * 0.15) { //consider keyboard visible when it's larger than 15% of screen height (very generous even)
                    Log.d(TAG, "keyboard is visible (according to: keyboardHeight > screenHeight * 0.15) --> nichts tun");
                    // Keyboard is visible
                    keyboardVisible = true;
                    Log.d(TAG, "keyboardVisible auf TRUE gesetzt ");
                } else {  //otherwise, if the keyboard was previously visible and the height is less than 15% of the screen height, we assume that the keyboard has been closed
                    //sth. else triggered onGlobalLayout except for the keyboard (which is still hidden)
                    if (keyboardVisible) {
                        //CLOSE KEYBOARD here:
                        Log.d(TAG, "keyboard WAS visible but not anymore --> CLOSE IT:");
                        //1 Close the keyboard
                        inputMethodManager.hideSoftInputFromWindow(sbbedittext.getWindowToken(), 0);
                        //2 initialize the view of BNV & show the bnv again :) TODO (info) den folg. code kann man nicht beim onCreateView setzen, weil sonst null obj ref bei bnvmain.setvisibil...
                        View mainActivityRootView = requireActivity().findViewById(android.R.id.content);
                        ViewGroup mainActivityContainer = mainActivityRootView.findViewById(R.id.relativelayoutmainid);
                        View bnvmain = mainActivityContainer.findViewById(R.id.bottomnavigationid);
                        bnvmain.setVisibility(View.VISIBLE);
                        //3 show the textview & categories in fragsearch again
                        searchViewModel.setTextViewVisibility(View.VISIBLE);
                        searchViewModel.setCategoriesVisibility(View.VISIBLE);
                        Log.d(TAG, "ENTER: textview in search frag sollte jetzt wieder visible sein");
                        Log.d(TAG, "ENTER: cateogories in search frag sollten jetzt wieder visible sein");
                    }
                    keyboardVisible = false;
                    Log.d(TAG, "keyboardVisible auf FALSE gesetzt ");
                }
                Log.d(TAG, "onGlobalLayout END");
            }
        };
        root.getViewTreeObserver().addOnGlobalLayoutListener(keyboardVisibilityListener);
        Log.d(TAG, "setupKeyboardVisibilityListener END");
    }
    private void removeKeyboardVisibilityListener() {
        Log.d(TAG, "removeKeyboardVisibilityListener called");
        if (root != null && keyboardVisibilityListener != null) {
            Log.d(TAG, "rootView != null && keyboardVisibilityListener != null --> daher remove on global layout listener");
            root.getViewTreeObserver().removeOnGlobalLayoutListener(keyboardVisibilityListener);
        }
        Log.d(TAG, "removeKeyboardVisibilityListener END");
    }
*/

    //tragischer chat versuch 2/3, swipe gesture zu generaten, hat allerigns null funktinioert
  /*  @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        swipeDetector = new GestureDetector(requireContext(), this);
        View fragmentView = getView();
        if (fragmentView != null) {
            fragmentView.setClickable(true);
            fragmentView.setFocusable(true);
            fragmentView.setFocusableInTouchMode(true);
            fragmentView.setOnTouchListener((v, event) -> swipeDetector.onTouchEvent(event));
        }
    }*/


  /*  //tragischer chat versuch 3/3, swipe gesture zu generaten, hat allerigns null funktinioert
    // Implement the required methods of the OnGestureListener interface
    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        // Check if the swipe gesture is in the horizontal direction
        if (Math.abs(velocityX) > Math.abs(velocityY)) {
            if (e1.getX() - e2.getX() > 0 || e2.getX() - e1.getX() > 0) {
                // Swipe from right to left -> keybord cloas
                inputMethodManager.hideSoftInputFromWindow(sbbedittext.getWindowToken(), 0);
                return true;
            }
        }
        return false;
    }

    // Implement other methods of the OnGestureListener interface

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }*/












    //youtube:  https://www.youtube.com/watch?v=woR2nCOuRu4&ab_channel=BrandanJones (vlt gehts mit chat besser)
  /*  private class SwipeListener implements View.OnTouchListener {
        //initialize variable
        GestureDetector gestureDetector;
        //create constructor
        SwipeListener(View view) {
            //initialize threshold value
            int thresholdvalue = 100;
            int velocity_threshold = 100;
            //inititalize simple gesture listener
            GestureDetector.SimpleOnGestureListener listener = new GestureDetector.SimpleOnGestureListener(){
                @Override
                public boolean onFling(@NonNull MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
                    //get x and y difference
                    float xDiff = e2.getX() - e1.getX();
                    float yDiff = e2.getY() - e1.getY();
                    try {
                        //check condition
                        if (Math.abs(xDiff) > Math.abs(yDiff)){
                            //when x is greater than y
                            //check condition
                            if (Math.abs(xDiff) > thresholdvalue && Math.abs(velocityX) > velocity_threshold) {
                                //when x diff is greater than threshold
                                //when x velocity s greater than velocity threshold
                                if (xDiff > 0){
                                    //when swiped right
                                    //TODO right
                                } else {
                                    //when swiped left
                                    //TODO left
                                }
                                return true;
                            }
                        }
                        //TODO (info) hier könnte noch swipe up / down hinkommen, aber erstmal fine ohne
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                    return false; //standard wäre: super.onFling(e1, e2, velocityX, velocityY);
                }
            };
            //initialize gesture detector
            gestureDetector = new GestureDetector(listener);
            //set listener on view
            view.setOnTouchListener(this);
        }
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            //return gesture event
            return gestureDetector.onTouchEvent(event);
        }
    }*/






    //im sbbedittext eingegebener text soll gespeichert werden
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState called");
        //1 zuvor eingebenen text wiederbekommen
        outState.putString("enteredText", enteredText);
    }


    private void showLanguageFilterDialog() {
        Log.d(TAG, "showLanguageFilterDialog ausgelöst");
        Log.d(TAG, "aktuelle language ist " + language + " (in: showLanguageFilterDialog)");

        //String language wird entweder in onCreate erstellt oder am Ende dieses Dialogs geändeert und dann einfach wieder im Folg. eingelesen

        // Set up the alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity().getBaseContext()); //TODO (unzufr.) warummmm / oder TODO (C) vlt. getContext() einfach
        builder.setTitle(R.string.audiolanguage);
        // Options for languages
        String[] options = {getString(R.string.german), getString(R.string.english)}; //TODO (future) foodtour ins sprachen hinzufügen land
        builder.setSingleChoiceItems(options, getSelectedLanguageIndex(language), (dialog, which) -> {
            // Update the selected language
            String newLanguage = getLanguageFromIndex(which);
            switch (newLanguage) {
                case "de":
                    System.out.println("lang = de, daher flagge des Großdeutschen Reiches");
                    languagefilter.setBackground(ContextCompat.getDrawable(getActivity().getBaseContext(), R.drawable.germanflag));
                    isEnglishFlag = false;
                    //TODO (A) newLanguage an das SearchViewModel weitergeben, damit dieses die Suchergebnisse ab jetzt nur noch auf bspw. Englisch anzeigt
                    // Update the selected language
                    searchViewModel.setSelectedLanguage(newLanguage);
                    break;
                case "en":
                    System.out.println("lang = en, daher Großbrittanien flagge");
                    languagefilter.setBackground(ContextCompat.getDrawable(getActivity().getBaseContext(), R.drawable.englishflag));
                    isEnglishFlag = true;
                    //TODO (A) newLanguage an das SearchViewModel weitergeben, damit dieses die Suchergebnisse ab jetzt nur noch auf bspw. Englisch anzeigt
                    searchViewModel.setSelectedLanguage(newLanguage);
                    break;
                    //es gibt keinen default fall, weil im alertdialog nur die optionen DE und EN aufploppen ;)
                /*default:  //irgendeine ANDERE sprache, die auf dem handy läuft (wie bei mir random italienisch) -> TODO (info / unzufr.) einfach english als standard weil das ausländer können
                    languagefilter.setBackground(ContextCompat.getDrawable(getActivity().getBaseContext(), R.drawable.englishflag));
                    isEnglishFlag = true;
                    //TODO (A) newLanguage an das SearchViewModel weitergeben, damit dieses die Suchergebnisse ab jetzt nur noch auf bspw. Englisch anzeigt
                    searchViewModel.setSelectedLanguage(newLanguage);
                    break;*/
            }
            // Dismiss the dialog
            dialog.dismiss();
            Log.d(TAG, "Alert Dialog END (in: showLanguageSettingsDialog)");
        });
        // Show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
        Log.d(TAG, "showLanguageSettingsDialog END (in: showLanguageFilterDialog)");
    }
    private int getSelectedLanguageIndex(String selectedLanguage) {
        //TODO(C) problem to keep in mind: with multiple languages in the shared preferences, they don't work properly bc. user might put in language as shared preference in his language (e.g. "Deutsch") and then he switches languages and then "German" is not found in the shared preferences
        // -> that's why i put the enquiry for multiple languages (not ideal solution but it WOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO..)
        if (selectedLanguage.equals("de")) {
            return 0;
        } else if (selectedLanguage.equals("e")) {
            return 1;
        } else {
            return 1; // "en" ist der standard, wenn die lnaguage vom smartphone NICHT entweder engl. oder dt. ist
        }
    }
    //finde den index von der ausgewählten language
    private String getLanguageFromIndex(int index) {
        if (index == 0) {
            return "en";
        } else if (index == 1) {
            return "en";
        } else {
            return "en";
        }
    }








    public void requestFocusOnEditText() {
        if (sbbedittext != null) {
            sbbedittext.requestFocus();
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(sbbedittext, InputMethodManager.SHOW_IMPLICIT);
        }
    }
}
