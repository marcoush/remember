package com.example.remember.ui.search;

import static com.example.remember.mongo.AudioDocProcessor.modelListsearch;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.remember.Main;
import com.example.remember.MainViewModel;
import com.example.remember.R;
import com.example.remember.categories.CategoryView;
import com.example.remember.databinding.ActivityMainBinding;
import com.example.remember.databinding.FragSearchBinding;
import com.example.remember.events.ManipulateBSBonKeyboardChange;
import com.example.remember.events.OpenSearchfield;
import com.example.remember.mongo.CatQueries;
import com.example.remember.recycler_and_scrollviews.CustomAdapter;
import com.example.remember.recycler_and_scrollviews.Model;
import com.mongodb.MongoClientSettings;

import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.realm.mongodb.App;
import io.realm.mongodb.AppConfiguration;
import io.realm.mongodb.User;
import io.realm.mongodb.mongo.MongoClient;
import io.realm.mongodb.mongo.MongoCollection;
import io.realm.mongodb.mongo.MongoDatabase;

public class SearchFragment extends Fragment /*implements Main.MainListener*/ {
    private static final String TAG = "search_frag";

//tims stuff anfankchpo
    //TODO (A)  TUT ES NICHT , UMSETZEN: herausfinden, ob es während des eingebens von text schon sucht oder erst on button click searchBtn

    //binding
    private FragSearchBinding binding;

    //versuche für den searchbar fragment bnv sbb stuff omg ich mache mir aufwand aber ich will ES!
    private ActivityMainBinding mainBinding;
    private SearchViewModel searchViewModel;

    //UI
    EditText searchfield; //BNV ist im bottom navigation view "search_bnb.xml"
    //private ViewGroup rootView; //diese ViewGroup ersetzt den Container sozusagen in onCreateView und wird
    ImageButton languagebutton, updatesearchbutton, catfoldinbutton, catfoldoutbutton; //die beiden flaggen sind im inflateten popup-window drin, searchbutton ausm Sortiment genommen
    RelativeLayout rlsearchfield;
    View includedcategoriescontainer; //TODO am 11.1. in Rente gegange
    View includedcategoriesbar; //TODO am 11.1. gebore ne doch nicht
    Button party,festival,friends,nature, abroad,university,illegal,adventure,relationship,trip,dream,luck,travel,school,family,work,happy,inlove,lonely,desperate,relieved,sad,depressive,angry,anxious,nostalgic,club;
    ProgressBar progressBar;
    
    //die ModelList, in der die Suchergebnisse aufgelistet werden + recycler view zum Anzeigen + adaoter als Mezzanine für die Daarten
    private RecyclerView rvsearch;
    CustomAdapter adaptersearch;
    String enteredText, language;

    //mongoDB realm initializa
    MongoDatabase mongoDatabase;
    MongoClient mongoClient;
    MongoCollection<Document> mongoUsersCollection, mongoAudiosCollection;
    User user;
    App app;
    String userid, usermail;


    //alle categories in 2 listen, können in reihenfolge angepasst werden, wenn ich merke, dass eine category mehr relevanz benötigt und weiter vorne angezeigt werden soll o.so

    //when click butôn, add button-text (=category) to this array
    ArrayList<String> selectedCategoriesArray = new ArrayList<>(); //Array für category-filter- in diesem array stehen ctgrs, die gefiltert werden und die farbe ändern und in savedinstancestate gespeichert werden
    ProgressBar languageprocessbar;
    //der allseits beliebte boolean isenglishflag kommt auch hier zum vorschein:
    private boolean isEnglishFlag; //TODO (info) also wenn isEnglishFlag=true, dann wird bei der query nur gequeried, wenn das field "language" = "en" ist... & vice versa
    //for the focus listener of searchfield
    //private boolean issearchfieldFocused;
    //neuer versuch mit window insets listener (inside the onfocuschangelistener)
    //private View.OnApplyWindowInsetsListener windowInsetsListener;
    //versuch bnv visibility manipulation mit gll , läuft 3.7.
    private ViewTreeObserver.OnGlobalLayoutListener keyboardVisibilityListener; //TODO (9.1.) public machen?.. sodass aus memoFrag accesset werden kann, damit der VTO gekillt werden kann, sobald man memoFrag hochziehen will
    private View mainActivityRootView;
    private View root;
    InputMethodManager inputMethodManager; //tastiera an sich

    //für den icon click, der im search-frag das searchfield apertet 3.7.
    private boolean isInSearchFragment = true; //relevant für onResume


    //für das edittext für die suche, ob das keyboard/ gerade auf ist
    //private boolean keyboardActive;


    //farben für buttons
    int grayedviolet,grayedred, grayedblueish,grayeddarkred,grayedgreen,grayedlightgreen,grayedcyan,grayedorange;
    int violet,red,blueish,darkred,green,lightgreen,cyan,orange;

    //queries..
    private Document lastvisibledoc; //TODO (A+) ALARM DIESE WERDEN NICHT BENUTZT
    Boolean isScrolling = false; //yt tutorial
    int currentItems, totalItems, scrollOutItems; //yt tutorial
    private String searchtermio;
    private boolean searchtermioExists;
    private boolean isQueryingData;

    //counter for how many items are currently in rv (in order to get lower bound for range, imp for adding new items to adapter)
    //private int currentsearchmemosinrv = 0; //jetzt mit notifyDataSetChanged();

    //drawables
    private int playint, pauseint, heartint, heartunfilledint, germanint, englishint, color_ui, color_one;
    //private boolean keyboardopened;
    private boolean isgloballayoutlisteneradded;
    private RelativeLayout searchfieldcontainer, rlcatbar;
    //manipulate memoFrag bottomsheet when searchfield
    //private SearchFragmentListener listenerCommunicationSearchtoMain;//stattdessen Eventbus

    PopupWindow popupWindow; //11.1. das öffnet sich, wenn die cats expanded werden solln

    //vm
    MainViewModel mainViewModel; //retrieves the data from outsourced searchquerycat mongo queries

    public static int currentmemosglobalsearch = 0; //TODO (sis) (serachFrag) MUSS BEI SAVED INSTANCE STATE AUCH BEIBEHALTEN WERDEN!!!!
    private static String selectedlang;
    private boolean isKeyboardOpen = false;
    private boolean isKeyboardStateChanged = false;

    //TODO 19.1. gll ➝ onpredrawlistener
    private ViewTreeObserver.OnPreDrawListener onPreDrawListener;
    private boolean isOnPreDrawListenerEnabled = true;


    // This method will be called when a SomeOtherEvent is posted
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onOpenSearchfield(OpenSearchfield signal) {
        Log.d(TAG, "onOpenSearchfield");
        openSearchfield();
    }


    //@Override //TODO (searchFrag) (override) DOES THAT NEED TO BE HERE???
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG,"onCreateView search");
        binding = FragSearchBinding.inflate(inflater, container, false); //ALLES HIER GEBINDET MIT DEM NAVIGATION FRAGMETN !!!!
        root = binding.getRoot();

        //TODO (B) muss man das hier alles noch mal callen, nachdem man den bnv geändert that?
//0 ui bindings
        rlsearchfield = binding.rlsearchfieldid;
        searchfield = binding.searchfieldid;
        languagebutton = binding.languagefilterid;
        rvsearch = binding.searchfragrvid;
        progressBar = binding.progressbarsearchid;
        updatesearchbutton = binding.updatesearchbuttonid;
        searchfieldcontainer = binding.searchfieldcontainerid;
        catfoldinbutton = binding.catfoldinbuttonid;
        catfoldoutbutton = binding.catfoldoutbuttonid;
        rlcatbar = binding.rlcatbarid;
        //cat container ._. bzw. bar
        //includedcategoriescontainer = root.findViewById(R.id.categoriescontainerid); //TODO 10.1. schmutz alt
        includedcategoriesbar = root.findViewById(R.id.includedcategoriesbarid); //TODO 11.1. gebore

        //llparty = binding.llparty;lltravel = binding.lltravel;llpeople = binding.llpeople;llsad = binding.llsad;llhappy = binding.llhappy;llangry = binding.llangry; //buttonLL unnvolst. werden eh nicht genutzt..
        /*party = includedcategoriescontainer.findViewById(R.id.party);
        festival = includedcategoriescontainer.findViewById(R.id.festival);
        friends = includedcategoriescontainer.findViewById(R.id.friends);
        nature = includedcategoriescontainer.findViewById(R.id.nature);
        erasmus = includedcategoriescontainer.findViewById(R.id.abroad);
        university = includedcategoriescontainer.findViewById(R.id.university);
        illegal = includedcategoriescontainer.findViewById(R.id.illegal);
        adventure = includedcategoriescontainer.findViewById(R.id.adventure);
        relationship = includedcategoriescontainer.findViewById(R.id.relationship);
        trip = includedcategoriescontainer.findViewById(R.id.trip);
        dream = includedcategoriescontainer.findViewById(R.id.dream);
        travel = includedcategoriescontainer.findViewById(R.id.travel);
        school = includedcategoriescontainer.findViewById(R.id.school);
        family = includedcategoriescontainer.findViewById(R.id.family);
        work = includedcategoriescontainer.findViewById(R.id.work);
        happy = includedcategoriescontainer.findViewById(R.id.happy);
        lucky = includedcategoriescontainer.findViewById(R.id.luck);
        inlove = includedcategoriescontainer.findViewById(R.id.inlove);
        lonely = includedcategoriescontainer.findViewById(R.id.lonely);
        desperate = includedcategoriescontainer.findViewById(R.id.desperate);
        relieved = includedcategoriescontainer.findViewById(R.id.relieved);
        sad = includedcategoriescontainer.findViewById(R.id.sad);
        depressive = includedcategoriescontainer.findViewById(R.id.depressive);
        angry = includedcategoriescontainer.findViewById(R.id.angry);
        anxious = includedcategoriescontainer.findViewById(R.id.anxious);
        nostalgic = includedcategoriescontainer.findViewById(R.id.nostalgic);
        club = includedcategoriescontainer.findViewById(R.id.club);
        //TODO (new categories) onCreateView [SearchFrag]
        *///11.1. jetzt nicht mehr im frag direkt, sondern in includetem layout

//1 recyclerview & adapoter
        //List<Model> emptylist = new ArrayList<>();
        //1
        rvsearch.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        rvsearch.setLayoutManager(layoutManager);
        adaptersearch = new CustomAdapter(SearchFragment.this, modelListsearch);
        rvsearch.setAdapter(adaptersearch); //(info) hier schon setten, weil später geht iwie nicht, ws wg ui thread blocking

//1 vm & categoryView
        this.mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        CategoryView categoryView = new CategoryView(requireContext(), userid);


//2 mongodb stuff
        app = new App(new AppConfiguration.Builder("remember-dxcgp").build());
        user = app.currentUser();
        userid = user != null ? user.getId() : null; //sieht funny aus, deswegen hab ich's etabliert , wurde vorgshlagen in der mwmw heit franen :)
        usermail = user != null ? user.getProfile().getEmail() : null; //sieht funny aus, deswegen hab ich's etabliert , wurde vorgshlagen in der mwmw heit franen :)
        Log.d(TAG,"user: " + user +
                "\nuserid: " + userid +
                "\nusermail: " + usermail);
        mongoClient = user.getMongoClient("mongodb-atlas");
        mongoDatabase = mongoClient.getDatabase("remember");
        // registry to handle POJOs (Plain Old Java Objects)
        //CodecRegistry pojoCodecRegistry = fromRegistries(AppConfiguration.DEFAULT_BSON_CODEC_REGISTRY,
        //        fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        //TODO (info) (mongo) wenn Updates,Filters usen will, implementen: ➝ CodecRegistry defaultJavaCodecRegistry = MongoClientSettings.getDefaultCodecRegistry();
        CodecRegistry defaultJavaCodecRegistry = MongoClientSettings.getDefaultCodecRegistry();
        //(info) sicherheitshalber die codecregistry eingebaut für Sort
        mongoUsersCollection = mongoDatabase.getCollection("users").withCodecRegistry(defaultJavaCodecRegistry);
        mongoAudiosCollection = mongoDatabase.getCollection("audios").withCodecRegistry(defaultJavaCodecRegistry);
        //mongoCategoriesCollection = mongoDatabase.getCollection("categories", Category.class).withCodecRegistry(pojoCodecRegistry);

//3.1 keyboard initiate & assign
        inputMethodManager = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE); //TODO (C) context könnte sein dass nicht funktiniert
//3.2 main act rootview (but bnv)
        mainActivityRootView = requireActivity().findViewById(android.R.id.content);
//3.2 inititate viewmodel
        this.searchViewModel = new ViewModelProvider(requireActivity()).get(SearchViewModel.class);
//3.3 zugriff auf main using mainActivityRootView & mainActivityContainer, um das Main Layout zu inflaten: (geschieht dezentral !!! wegen NOR / NPE gefahr
        mainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        //TODO (A) das funktionierte nicht und gab null pointer exception beim ersten zugriff auf searchbottombaredittext.doSomething ,...
        //TODO (info) ws. kein Problem: diese Views und EditText müssen doch eigentlich außerhalb onCreateView, um von überall aus zugegriffen werden zu könenn ?
        // naja, vlt wenn methods gibt... abero k eigentlich sollte das kein problem sein
//3-M drawable ints
        //heartint = R.drawable.heart_white;
        //heartunfilledint = R.drawable.heart_white_unfilled;
        germanint = R.drawable.germanflag;
        englishint = R.drawable.englishflag;


//4 searchfield EDITOR ACTION
        searchfield.setOnEditorActionListener((v, actionId, event) -> {
            Log.d(TAG, "searchfield onEditorAction");
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_GO) {
                //1 Close the keyboard
                //inputMethodManager.hideSoftInputFromWindow(searchfield.getWindowToken(), 0); //(info) hide keyboard
                searchtermio = searchfield.getText().toString().trim();
                //Log.d(TAG, "searchtermio:"+searchtermio);
                if (!searchtermio.equals("")) {
                    searchtermioExists = true;
                    searchData(); //onEditorActionListener
                } else {
                    searchtermioExists = false;
                    showData(); //onEditorActionListener
                }
                return true;
            }
            //Log.d(TAG, "searchfield onEditorAction END");
            return false;
        });

        //TODO 1.4. remove focus of edittext when a) ui back button or b) keyboard closed manually ➝ doesn't go
        //TODO 1.4. getlocationonscreen(); method tryouder





        /*
//assign onPreDrawListener
        onPreDrawListener = () -> {
            Log.d(TAG, "onPreDrawListener:" + onPreDrawListener);

            //first, temporarily remove the onPreDrawListener so that the next part of code doesn't trigger the onPreDrawListener infinitely
            //Log.d(TAG, "temporarily remove the onPreDrawListener");
            //removeOnPreDrawCategoriesLayoutListener(rlsearchfield); //TODO 20.1. something in here seems to trigger the onpredrawListener over and over again... some code

            // Check the position of the LinearLayout relative to the root view
            int[] location = new int[2]; //array with 2 ints that stores x&y-coordinates
            rlsearchfield.getLocationOnScreen(location); //give out location[1] y-coordinate
            int layoutbottomheight = location[1] + rlsearchfield.getHeight();
            //TODO 20.1. something in here seems to trigger the onpredrawListener over and over again... some code

            //int[] rootViewLocation = new int[2];
            //root.getRootView().getLocationOnScreen(rootViewLocation);
            int rootviewheight = root.getRootView().getHeight();

            // Check if the LinearLayout is above a certain threshold, consider it as the keyboard being open
            boolean newKeyboardOpenState = layoutbottomheight > rootviewheight * 0.65; //0.65
            Log.d(TAG, "layoutbottomheight:" + layoutbottomheight +
                    "\nx-coordinate of layoutbottom (location[1]):" + location[1] +
                    //"\nrlsearchfield.getHeight():"+rlsearchfield.getHeight() +
                    "\nrootviewheight:" + rootviewheight +
                    "\nnewKeyboardOpenState:" + newKeyboardOpenState);

            //TODO 19.1. ggf. remove + add later on listener again

            // If the keyboard state has changed, handle it accordingly
            if (newKeyboardOpenState != isKeyboardOpen) { //isKeyboardOpen as current state of keyboard (true=open , false=closed)
                isKeyboardOpen = newKeyboardOpenState; //addOnPreDrawListener
                Log.d(TAG, "isKeyboardOpen updated to:" + isKeyboardOpen);
                if (isKeyboardOpen) {
                    Log.d(TAG, "oPDL: keyboard open");
                    handleKeyboard(false); //addOnPreDrawCategoriesLayoutListener
                } else {
                    Log.d(TAG, "oPDL: keyboard closed");
                    handleKeyboard(true); //addOnPreDrawCategoriesLayoutListener
                    inputMethodManager.hideSoftInputFromWindow(searchfield.getWindowToken(), 0); //hide keyboard if not yet done :D
                }
            }

            //add OnPreDrawListener again
            Log.d(TAG, "add OnPreDrawListener again");
            Log.d(TAG, "onPreDrawListener:"+onPreDrawListener); //TODO 19.1. weiter wieso ist das null
            //rlsearchfield.getViewTreeObserver().addOnPreDrawListener(onPreDrawListener); //TODO 20.1. something in here seems to trigger the onpredrawListener over and over again... some code

            return true;
        };*/ //TODO 20.1. gll ➝ onpredrawnlistener nicht hier, sondern direkt zugewiesen im onviewcreated

//5 on focus change listener
        searchfield.setOnFocusChangeListener((view, hasFocus) -> { //dieser focus listener wird mit hasFocus initiiert, also beim 1. Mal wird direkt festgelegt, dass searchfield focus hat!
            //Log.d(TAG, "searchfield focus has changed, focus:" + hasFocus);
            //dieser boolean, damit nicht beide if-clauses auslösen
            if (hasFocus) { //&& focushasbeenchanged[0] == false
                Log.d(TAG, "onFocusChangeListener: searchfield focus has changed to true ➝ hide bnv & categories + setup globallayoutlistener");
                handleKeyboard(false); //onFocusChangeListener
                //keyboardopened = true;
                /*int[] location = new int[2]; //array with 2 ints that stores x&y-coordinates
                rlsearchfield.getLocationOnScreen(location); //give out location[1] y-coordinate, top edge of the screen is the origin from where the y-coordinates is calculated
                int layoutbottomheight = location[1] + rlsearchfield.getHeight();
                int rootviewheight = root.getRootView().getHeight();
                Log.d(TAG, "BEFORE layoutbottomheight:"+layoutbottomheight +
                        "\nBEFORE x-coordinate of layoutbottom (location[1]):"+location[1] +
                        //"\nBEFORE rlsearchfield.getHeight():"+rlsearchfield.getHeight() +
                        "\nBEFORE rootviewheight:"+rootviewheight);*/ //TODO debug kannweg

                //addOnPreDrawCategoriesLayoutListener(root, rlsearchfield); //TODO 19.1. stattdessen vorher, weil soll ja predrawn sein...

                // Add a layout change listener to the LinearLayout whenever the keyboard has opened once (to prevent charging this during fragment creation)
                addKeyboardVisibilityListener();
                /*rlsearchfield.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                    // Check the position of the LinearLayout relative to its parent
                    int[] location = new int[2];
                    rlsearchfield.getLocationInWindow(location);

                    // Calculate the bottom position of the LinearLayout within its parent
                    int bottomOfLinearLayout = location[1] + rlsearchfield.getHeight();

                    // Calculate the middle position of the parent
                    ViewGroup parentLayout = (ViewGroup) rlsearchfield.getParent();
                    int middleOfParent = parentLayout.getHeight() / 2;

                    // Check if the bottom of the LinearLayout is above the middle of its parent
                    boolean isLinearLayoutInMiddle = bottomOfLinearLayout < middleOfParent;

                    //simplified if-else
                    handleKeyboard(!isLinearLayoutInMiddle); //TODO 1.4.
                });*/ //TODO 1.4. all in the method above addKeyboardvisibleitliyListener();


                /*//call the method after 200ms to give the globallayout some time to unfold open keyboard + then only start listener
                Handler handler = new Handler();
                Runnable delayedRunnable = this::addKeyboardVisibilityListener; //onFocusChangeListener
                handler.postDelayed(delayedRunnable, 200);*/ //TODO 19.1. gll ➝ onpredrawnlistener, wegen predrawn brauche ich auch delay ned mehr i think
            } else {
                Log.d(TAG, "onFocusChangeListener: searchfield focus has changed to false ➝ show bnv & categories + setup globallayoutlistener");
                //Log.d(TAG, "onFocusChangeListener: sbbedittext hast lost focus and the keyboard must have been exited (e.g. through back-button, swipe or ENTER) ➝ show bnv & categories + setup globallayoutlistener");
                //root.setOnApplyWindowInsetsListener(null);
                handleKeyboard(true); //onFocusChangeListener
                //keyboardopened = false;
                //removeKeyboardVisibilityListener(); //setOnFocusChangeListener //TODO 19.1. gll ➝ onpredrawnlistener, wegen predrawn brauche ich auch delay ned mehr i think
            }
            //Log.d(TAG, "searchfield focus changed END");
        });

//6 sbbedittext ontextchangedlistener
        //!!!! INFO !!!! wenn man binding.searchbottombaredittextid durch sbbedittext substituted, gibt's in der zeile searchViewModel.setEnteredText(sbbedittext.getText().toString().trim()); ne null object reference
        binding.searchfieldid.addTextChangedListener(new TextWatcher() {
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
                //searchViewModel.setEnteredText(searchfield.getText().toString().trim());
            }
        });

//7 languagefilter + selectedlang
        setBackgroundofimagebutton(languagebutton,englishint);//TODO (unzufr.) wg NPE gefahr =?="?=Q§4023e
        languagebutton.setScaleType(ImageView.ScaleType.FIT_XY);
        //flagge anmalen initialization according to language: (darf in xml nicht in android:src stehen, sonst wäre dies der standard view und ein setBackground würde unangehem überlappen uff)
        //TODO 15.1. weil der langfilter soll nur 1x ganz am Anfang die applang annehmen, sonst die vorherig ausgewählte!
        language = selectedlang == null ? Locale.getDefault().getLanguage() : selectedlang; //variable = (condition) ? expression_if_true : expression_if_false;
        switch (language) {
            case "de" -> {
                Log.d(TAG,"lang = de, daher flagge des Großdeutschen Reiches");
                setBackgroundofimagebutton(languagebutton,germanint); //onCr
                isEnglishFlag = false;
                selectedlang = "de";
            }
            case "en" -> {
                Log.d(TAG,"lang = en, daher UK flagge");
                setBackgroundofimagebutton(languagebutton,englishint); //onCr
                isEnglishFlag = true;
                selectedlang = "en";
            }
            //TODO (languages)
            default -> {  //irgendeine ANDERE sprache, die auf dem handy läuft (wie bei mir random italienisch)
                Log.d(TAG,"lang = iwas, daher UK flagge");
                setBackgroundofimagebutton(languagebutton,englishint); //onCr
                isEnglishFlag = true;
                selectedlang = "en";
            }
        }
        languagebutton.setOnClickListener(v -> {
            Log.d(TAG,"languagebutton clicked");
            if(isEnglishFlag) {
                setBackgroundofimagebutton(languagebutton,germanint); //onClick
                categoryView.changeLangofButtons("de");
                isEnglishFlag = false;
                selectedlang = "de";
            } else {
                setBackgroundofimagebutton(languagebutton,englishint); //onClick
                isEnglishFlag = true;
                selectedlang = "en";
                categoryView.changeLangofButtons("en");
            }
        });

//8 rv onscrolllistener
        rvsearch.addOnScrollListener(new RecyclerView.OnScrollListener() {
            //yt tutorial:
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                    isScrolling = true;
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                Log.d(TAG, "onScrolled");
                //yt tutorial: :) tausend dank bro, es workt
                super.onScrolled(recyclerView, dx, dy); //TODO (A) brauche ich hier eig kein SUPER??? .. war vorher nicht drin (vor yt tutorial)
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                currentItems = layoutManager.getChildCount();
                totalItems = layoutManager.getItemCount();
                scrollOutItems = layoutManager.findFirstVisibleItemPosition();
                Log.d(TAG, "isScrolling is:" + isScrolling +
                        "\ncurrentItems is:" + currentItems +
                        "\ntotalItems is:" + totalItems +
                        "\nscrollOutItems is:" + scrollOutItems);

                if (dy > 0 && isScrolling && (currentItems + scrollOutItems == totalItems)) {
                    // User has scrolled to the end ➝ data fetch
                    Log.d(TAG, "onScrolled: user has scrolled to the bottom ➝ queryNextPage");
                    queryNextPage();
                    isScrolling = false;
                }
            }
        });


//9 show data in recyclerView
        showData(); //zeigt alle stocks untereinander an in dem recyclerview... TODO ich glaube das hier nicht onCreate ... (taktik frage)
        //TODO (A) show data soll - wenn es einen savedinstancestate gibt - den categoryfilter miteinbeziehen!


//10 searchfield hoch wenn memoFrag collapsed an is
        if (Main.isMemoFragOpened) {
            Log.d(TAG, "onCr: memofrag opened ➝ searchfield hoch (bottommargin 66)");
            moveSearchfieldContainer(66); //onCreate
            //EventBus.getDefault().post(new RiseFab(true));//fab kann auf derselben Höhe wie searchfield sein ...
        } else {
            Log.d(TAG, "onCr: memofrag NOT open ➝ searchfield runna (bottommargin 10)");
            moveSearchfieldContainer(10); //onCreate
        }
        //TODO 1 fab hoch
        // 2 in searchFrag memoFrag_collapsed hoch
        // 3 (in tastiera IMMER normales searchFrag layout)

//11 make the cats in the hsv setup + clickable
        Log.d(TAG, "onCr: categoryBarView functionality is imported");
        //includedcategoriescontainer = root.findViewById(R.id.categoriescontainerid); //das ist, wie es in profileFrag gemacht wirde 12.1.

        //View popupView = LayoutInflater.from(getContext()).inflate(R.layout.layout_categories_bar, container, false); //hier stand statt "container, false" nur "null", erzeugte aber warning
        // Call createCategoryContainerView with the correct parent container (e.g., a layout in your fragment)
        //  categoryView.setupCategoryView(popupView);

        categoryView.setupCategoryView(includedcategoriesbar);

//12 catfoldin/outbutton zum aus/einklappen des catbars
        catfoldinbutton.setOnClickListener(v -> {
            //(info) wenn man einclappt und danach wieder ausclappt, ist der hsv sogar immer noch am selben Ort omg wie geilllll!!! 12.1. big erfolg! sjajajjajaa
            rlcatbar.setVisibility(View.GONE);
            //includedcategoriesbar.setVisibility(View.GONE);
            //catfoldoutbutton.setVisibility(View.GONE);
            catfoldoutbutton.setVisibility(View.VISIBLE);
            catfoldoutbutton.setClickable(true);
//TODO (A) (search) (catfoldinbutton) zuvor ausgew cats entfernen aus suche, oder drinlassen in suche, wenn eingeklappt wird ?????
        });
        catfoldoutbutton.setOnClickListener(vi -> {
            rlcatbar.setVisibility(View.VISIBLE);
            //includedcategoriesbar.setVisibility(View.VISIBLE);
            //catfoldoutbutton.setVisibility(View.VISIBLE);
            catfoldinbutton.setVisibility(View.VISIBLE);
            catfoldinbutton.setClickable(true);
            catfoldoutbutton.setVisibility(View.INVISIBLE);
            catfoldoutbutton.setClickable(false);
        });



/*//11 popup-window for cats
        //initializations
        //ViewGroup searchLayout = requireView().findViewById(R.id.relativesearchlayoutid); //TODO 11.1. ➝ ="container" lol
        CategoryView categoryContainer = new CategoryView(requireContext(), userid);

        //do all the functionality in the CategoryView.java
        View popupView = categoryContainer.createCategoryContainerView(container);
        //View popupView = getLayoutInflater().inflate(R.layout.layout_categories_container, container, false); //hier stand statt "container, false" nur "null", erzeugte aber warning

        //open the custom layout for the popup-window
        popupWindow = new PopupWindow(popupView, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
        popupWindow.setAnimationStyle(R.style.PopupAnimation);

        //catexpanderbutton: open the custom layout for the popup window
        catexpanderbutton.setOnClickListener(v -> {
            if (popupWindow.isShowing()) {
                popupWindow.dismiss(); // Close the popup window - does automatically as well the animation :9
            } else {
                // Show the popup window at the bottom-right corner of the screen
                popupWindow.showAsDropDown(catexpanderbutton, 0, catexpanderbutton.getHeight() * -3); //does automatically the animation :7
            }
        });*/ //11.1. popup-window for cats erstmal stillgelegt, weil der cat hsv bar ws erstmal reischtet ABER ES FUNKTIONIERT BASISCH!
        
        //Log.d(TAG, "onCreateView END");
        return root; //vorher war code innerhalb von onCreateView und der Code war vor dieser Zeile, naja hab mich noch nicht erkundgit wo besser sei
    }//_______________onCreateView__________________END___________________


    private void addOnPreDrawCategoriesLayoutListener(RelativeLayout rlsearchfield) {
        Log.d(TAG, "addOnPreDrawCategoriesLayoutListener");
        // Add OnPreDrawListener
        rlsearchfield.getViewTreeObserver().addOnPreDrawListener((ViewTreeObserver.OnPreDrawListener) () -> {
                Log.d(TAG, "onPreDrawListener:" + onPreDrawListener);

                //first, temporarily remove the onPreDrawListener so that the next part of code doesn't trigger the onPreDrawListener infinitely
                //Log.d(TAG, "temporarily remove the onPreDrawListener");
                //removeOnPreDrawCategoriesLayoutListener(rlsearchfield); //TODO 20.1. something in here seems to trigger the onpredrawListener over and over again... some code

                // Check the position of the LinearLayout relative to the root view
                int[] location = new int[2]; //array with 2 ints that stores x&y-coordinates
                rlsearchfield.getLocationOnScreen(location); //give out location[1] y-coordinate
                int layoutbottomheight = location[1] + rlsearchfield.getHeight();
                //TODO 20.1. something in here seems to trigger the onpredrawListener over and over again... some code

                //int[] rootViewLocation = new int[2];
                //root.getRootView().getLocationOnScreen(rootViewLocation);
                int rootviewheight = root.getRootView().getHeight();

                // Check if the LinearLayout is above a certain threshold, consider it as the keyboard being open
                boolean newKeyboardOpenState = layoutbottomheight > rootviewheight * 0.65; //0.65
                Log.d(TAG, "layoutbottomheight:" + layoutbottomheight +
                        "\nx-coordinate of layoutbottom (location[1]):" + location[1] +
                        //"\nrlsearchfield.getHeight():"+rlsearchfield.getHeight() +
                        "\nrootviewheight:" + rootviewheight +
                        "\nnewKeyboardOpenState:" + newKeyboardOpenState);

                //TODO 19.1. ggf. remove + add later on listener again

                // If the keyboard state has changed, handle it accordingly
                if (newKeyboardOpenState != isKeyboardOpen) { //isKeyboardOpen as current state of keyboard (true=open , false=closed)
                    isKeyboardOpen = newKeyboardOpenState; //addOnPreDrawListener
                    Log.d(TAG, "isKeyboardOpen updated to:" + isKeyboardOpen);
                    if (isKeyboardOpen) {
                        Log.d(TAG, "oPDL: keyboard open");
                        handleKeyboard(false); //addOnPreDrawCategoriesLayoutListener
                    } else {
                        Log.d(TAG, "oPDL: keyboard closed");
                        handleKeyboard(true); //addOnPreDrawCategoriesLayoutListener
                        inputMethodManager.hideSoftInputFromWindow(searchfield.getWindowToken(), 0); //hide keyboard if not yet done :D
                    }
                }
            return true;
        });
    }

    private void removeOnPreDrawCategoriesLayoutListener(RelativeLayout rlsearchfield) {
        // Remove OnPreDrawListener
        if ( onPreDrawListener != null) { //isOnPreDrawListenerEnabled &&
            Log.d(TAG, "removeOnPreDrawCategoriesLayoutListener ➝ remove really");
             rlsearchfield.getViewTreeObserver().removeOnPreDrawListener(onPreDrawListener);
            //onPreDrawListener = null; // Reset the listener reference
            //isOnPreDrawListenerEnabled = false; // Disable the listener temporarily
        } else {
            Log.d(TAG, "removeOnPreDrawCategoriesLayoutListener ➝ do nothing");
        }
    }


    //TODO (methodizing/classizing) basically selbe method moveFab in Main.java
    private void moveSearchfieldContainer(int bottommargin) {
        Log.d(TAG, "moveSearchfieldContainer ➝ bottommargin:" + bottommargin);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) searchfieldcontainer.getLayoutParams();
        int newMarginBottom = switch (bottommargin) {
            case 10 -> getResources().getDimensionPixelSize(R.dimen.dp_10);
            case 33 -> getResources().getDimensionPixelSize(R.dimen.dp_33);
            case 66 -> getResources().getDimensionPixelSize(R.dimen.dp_66);
            default -> throw new IllegalStateException("mSC: Unexpected value: " + bottommargin);
        };
        layoutParams.setMargins(layoutParams.leftMargin, layoutParams.topMargin, layoutParams.rightMargin, newMarginBottom);

        /*//THIS SHALL NOT TRIGGER THE onGlobalLayoutListener1113.12.
        //temporarily remove globallayoutlistener & add back after making the changes
        removeKeyboardVisibilityListener(); //moveSearchfieldContainer (temp)
        Log.d(TAG, "mSC: onGlobalLayoutListener temporarily removed, " +
                "\nnow change bottommargin of searchfield container");*/ //TODO 19.1. gll ➝ onpredrawnlistener, wegen predrawn brauche ich auch delay ned mehr i think

        searchfieldcontainer.setLayoutParams(layoutParams);

        /*//add gll again after 50ms (after the layoutchange has gone trhough) (this privents the infinite triggering of gll)
        Handler handler = new Handler();
        Runnable delayedCode = () -> {
            addKeyboardVisibilityListener(); //moveSearchfieldContainer
            Log.d(TAG, "mSC: onGlobalLayoutListener added again after 50ms");
        };
        handler.postDelayed(delayedCode, 50);*/ //TODO 19.1. gll ➝ onpredrawnlistener, wegen predrawn brauche ich auch delay ned mehr i think
    }






    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onViewCreated search");
        super.onViewCreated(view, savedInstanceState);

        //TODO 19.1. gll ➝ onpredrawnlistener TODO 20.1. put in onViewCreated, maybe that's better
        //addOnPreDrawCategoriesLayoutListener(rlsearchfield); //TODO 1.4. predrawn weg, nur mit focus

        //1 Observe the ViewModel for RECENT and FORYOU modellists that come from Main on app start!
        mainViewModel.getModelListSearch().observe(getViewLifecycleOwner(), this::displaysearchmemos); //TODO 11.1. hab's mal wie in homeFrag gmaxt
        mainViewModel.getProgressBarVisibilitysearch().observe(getViewLifecycleOwner(), this::setVisibilityProgressBar);//included into displayforyoumemos

        languageprocessbar = view.findViewById(R.id.languageprocessbarid); //warum auch immer wird der vbom binding. ... nicht erkannt

/*//categories buttons anzeigen
    //1.1 button colors
        //3.1 grayed colors
        grayedviolet = ContextCompat.getColor(requireContext(), R.color.grayedviolet);grayedred = ContextCompat.getColor(requireContext(), R.color.grayedred);
        grayeddarkred = ContextCompat.getColor(requireContext(), R.color.grayeddarkred);grayedblueish = ContextCompat.getColor(requireContext(), R.color.grayedblueish);
        grayedorange = ContextCompat.getColor(requireContext(), R.color.grayedorange);grayedgreen = ContextCompat.getColor(requireContext(), R.color.grayedgreen);
        grayedlightgreen = ContextCompat.getColor(requireContext(), R.color.grayedlightgreen);grayedcyan = ContextCompat.getColor(requireContext(), R.color.grayedcyan);
        //3.1 highlighted colors
        violet = ContextCompat.getColor(requireContext(), R.color.violet);red = ContextCompat.getColor(requireContext(), R.color.red);
        darkred = ContextCompat.getColor(requireContext(), R.color.darkred); blueish = ContextCompat.getColor(requireContext(), R.color.blueish);
        orange = ContextCompat.getColor(requireContext(), R.color.orange);green = ContextCompat.getColor(requireContext(), R.color.green);
        lightgreen = ContextCompat.getColor(requireContext(), R.color.lightgreen);cyan = ContextCompat.getColor(requireContext(), R.color.cyan);
        //3.3 set grayed colors to buttons:
        party.setBackgroundColor(grayedviolet);festival.setBackgroundColor(grayedviolet);club.setBackgroundColor(grayedviolet);
        work.setBackgroundColor(grayedorange);university.setBackgroundColor(grayedorange);school.setBackgroundColor(grayedorange);erasmus.setBackgroundColor(grayedorange);
        adventure.setBackgroundColor(grayedgreen);nature.setBackgroundColor(grayedgreen);illegal.setBackgroundColor(grayedgreen);trip.setBackgroundColor(grayedgreen);travel.setBackgroundColor(grayedgreen);
        friends.setBackgroundColor(grayedred);family.setBackgroundColor(grayedred);relationship.setBackgroundColor(grayedred);
        dream.setBackgroundColor(grayedcyan);
        happy.setBackgroundColor(grayedlightgreen);inlove.setBackgroundColor(grayedlightgreen);relieved.setBackgroundColor(grayedlightgreen);lucky.setBackgroundColor(grayedlightgreen);
        sad.setBackgroundColor(grayedblueish);depressive.setBackgroundColor(grayedblueish);nostalgic.setBackgroundColor(grayedblueish);lonely.setBackgroundColor(grayedblueish);
        angry.setBackgroundColor(grayeddarkred);anxious.setBackgroundColor(grayeddarkred);desperate.setBackgroundColor(grayeddarkred);
        //3.4 set the tags for the grayed colors for the buttons
        party.setTag(grayedviolet);festival.setTag(grayedviolet);club.setTag(grayedviolet);
        work.setTag(grayedorange);university.setTag(grayedorange);school.setTag(grayedorange);erasmus.setTag(grayedorange);
        adventure.setTag(grayedgreen);nature.setTag(grayedgreen);illegal.setTag(grayedgreen);trip.setTag(grayedgreen);travel.setTag(grayedgreen);
        friends.setTag(grayedred);family.setTag(grayedred);relationship.setTag(grayedred);
        dream.setTag(grayedcyan);
        happy.setTag(grayedlightgreen);inlove.setTag(grayedlightgreen);relieved.setTag(grayedlightgreen);lucky.setTag(grayedlightgreen);
        sad.setTag(grayedblueish);depressive.setTag(grayedblueish);nostalgic.setTag(grayedblueish);lonely.setTag(grayedblueish);
        angry.setTag(grayeddarkred);anxious.setTag(grayeddarkred);desperate.setTag(grayeddarkred);
    //1.2 button clicks
        party.setOnClickListener(buttonClickListener);festival.setOnClickListener(buttonClickListener);club.setOnClickListener(buttonClickListener);
        work.setOnClickListener(buttonClickListener);university.setOnClickListener(buttonClickListener);school.setOnClickListener(buttonClickListener);erasmus.setOnClickListener(buttonClickListener);
        relationship.setOnClickListener(buttonClickListener);friends.setOnClickListener(buttonClickListener);family.setOnClickListener(buttonClickListener);
        adventure.setOnClickListener(buttonClickListener);trip.setOnClickListener(buttonClickListener);travel.setOnClickListener(buttonClickListener);illegal.setOnClickListener(buttonClickListener);nature.setOnClickListener(buttonClickListener);
        dream.setOnClickListener(buttonClickListener);
        happy.setOnClickListener(buttonClickListener);lucky.setOnClickListener(buttonClickListener);relieved.setOnClickListener(buttonClickListener);inlove.setOnClickListener(buttonClickListener);
        sad.setOnClickListener(buttonClickListener);depressive.setOnClickListener(buttonClickListener);nostalgic.setOnClickListener(buttonClickListener);lonely.setOnClickListener(buttonClickListener);
        anxious.setOnClickListener(buttonClickListener);angry.setOnClickListener(buttonClickListener);desperate.setOnClickListener(buttonClickListener);
        //TODO (new categories) onViewCreated [SearchFrag]*/ //11.1. categories buttons werden jetzt in popupwindow gehandlet, nicht mehr in searchfrag

        //1.3 zuvor eingegebenen text wiederbekommen ➝ restore from saved instance state
        if (savedInstanceState != null) {
            Log.d(TAG, "savedinstancestate!=null, also speist sbbedittext mit dem zuvor entered text:");
            enteredText = savedInstanceState.getString("enteredText");
            searchfield.setText(enteredText);

            selectedCategoriesArray = savedInstanceState.getStringArrayList("selectedCategories");
            // Restore the button colors based on the selectedCategories
            //restoreButtonColors(); //TODO 11.1. in Rente gegangen, DAS MUSS ABER irgendwie ersetzt werden diese Funktionalität
            // Perform the filtered search with the restored category selection
            //TODO (A) statt showData filtered search veranlassen: showdata2() oder so-...,_, performFilteredSearch();

        }

        Log.d(TAG,"onViewCreated END");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume search" +
                "\nisInSearchFragment = true gesetzt");
        //when inside search-frag, search icon shall open searchfield :D 2/3
        isInSearchFragment = true;
        //Log.d(TAG, "isInSearchFragment wird auf true gesetzt, damit searchicon-onclicklistener wieder searchfield apertet (onResume)");

        //String eingebenersuchtext = String.valueOf(searchViewModel.getEnteredText());
        //searchfield.setText(eingebenersuchtext);
        //nee stattdessen im sbb, wenn man ENTER oder BACK drückt

        /*// Reattach the listener if it's null
        if (listenerCommunicationSearchtoMain == null && getActivity() instanceof SearchFragment.SearchFragmentListener) {
            listenerCommunicationSearchtoMain = (SearchFragment.SearchFragmentListener) getActivity();
        }*///stattdessen Eventbus

    }

    @Override
    public void onStart() {
        super.onStart();
        //eventubus
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop search" +
                "\nisInSearchFragment = false gesetzt");
        //when inside search-frag, search icon shall open searchfield :D 3/3
        isInSearchFragment = false;
        //eventubus
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause search");
        //kill gll if active
        if (rlsearchfield.getViewTreeObserver().isAlive()) {
            Log.d(TAG, "viewtreeobserver is alive ➝ kill gll");
            removeKeyboardVisibilityListener(); //onPause
        }//TODO 20.1. substituted with onpredrawListener
        /*//kill oPDL if active
        if (root.getViewTreeObserver().isAlive()) {
            Log.d(TAG, "viewtreeobserver is alive ➝ kill onpredrawListener");
            removeOnPreDrawCategoriesLayoutListener(rlsearchfield);
        }*/ //TODO 1.4. predrawn weg, nur mit focus
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        //Log.d(TAG, "onDestroyView search");

        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState search");
        outState.putStringArrayList("selectedCategories", selectedCategoriesArray);
    }












    //für die funktionality with the keyboard that it hides bnv, etc.
    private void addKeyboardVisibilityListener() {
        Log.d(TAG, "addKeyboardVisibilityListener");
        if (!isgloballayoutlisteneradded) searchfieldcontainer.getViewTreeObserver().addOnGlobalLayoutListener(
                keyboardVisibilityListener = new ViewTreeObserver.OnGlobalLayoutListener() {
                    //private final Rect rect = new Rect(); //TODO 1.4.
                    //13.12. der listener is eig. nur für das SCHLIESSEN des keyboards da... sonst nicht nötig
                    @Override
                    public void onGlobalLayout() {
                        Log.d(TAG, "onGlobalLayout: global layout changed, now check if keyboard is open");

                        //
                        // Check the position of the LinearLayout relative to its parent
                        int[] location = new int[2];
                        searchfieldcontainer.getLocationInWindow(location);

                        // Calculate the bottom position of the rlsearchfield within its parent
                        int bottomofsearchfieldcontainer = location[1] + searchfieldcontainer.getHeight();

                        // Get the screen height
                        DisplayMetrics displayMetrics = new DisplayMetrics();
                        ((Activity) requireContext()).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                        int screenHeight = displayMetrics.heightPixels;
                        // Calculate the middle of the screen
                        int keyboardThresholdHeight = screenHeight * 2 / 3;

                        // Check if the bottom of the LinearLayout is above the keyboardThresholdHeight
                        boolean issearchfieldcontainerlifted = bottomofsearchfieldcontainer < keyboardThresholdHeight;

                        //if searchfieldcontainer is below keyboardThresholdHeight ➝ keyboard closed and show bnv - otherwise hide bc. keyboard open
                        Log.d(TAG, "oGL: bottomofsearchfieldcontainer=" + bottomofsearchfieldcontainer +
                                "\noGL: screenHeight=" + screenHeight +
                                "\noGL: keyboardThresholdHeight=" + keyboardThresholdHeight +
                                "\noGL: issearchfieldcontainerlifted=" + issearchfieldcontainerlifted + "(weil:"+bottomofsearchfieldcontainer+" > "+keyboardThresholdHeight+")");

                        //simplified if-else
                        handleKeyboard(!issearchfieldcontainerlifted); //TODO 1.4.
                        //

                        /*//heights after layout change has occured:
                        root.getWindowVisibleDisplayFrame(rect);
                        int screenHeight = root.getRootView().getHeight(); //screenheight = visible screen of root (when keyboard plops up ➝ smaller)
                        int keyboardHeight = screenHeight - rect.bottom; //rect.bottom = bottom edge of the visible display frame (immer ganz unten lol)
                        Log.d(TAG, "oGL: keyboardHeight=" + keyboardHeight);
                        //(info) das hier funktioniert nur, zu erkennen, dass/wenn das keyboard not open is

                        //der listener is act. nur für das SCHLIESSEN des keyboards da... sonst nicht nötig
                        if (!(keyboardHeight > screenHeight * 0.15)) {
                            Log.d(TAG, "oGL: keyboard is gone ➝ show bnv");
                            handleKeyboard(true); //keyboardVisibilityListener
                            inputMethodManager.hideSoftInputFromWindow(searchfield.getWindowToken(), 0); //hide keyboard if not yet done :D
                        }*///TODO 1.4. vorherigeer stuff
                    }
                });
        //Log.d(TAG, "setupKeyboardVisibilityListener END");
        isgloballayoutlisteneradded = true;
    }

    //TODO 16.1. setup a viewtreeobserver for the LINEARLAYOUT of the searchbar.. when the searchbar is in the middle of the screen, then the keyboard must be open -
    // -. this also overgoes the problem of of fo not being able to open the memoFrag due to gLL ... :D

    private void removeKeyboardVisibilityListener() {
        if (root != null && keyboardVisibilityListener != null) {
            Log.d(TAG, "removeKeyboardVisibilityListener ➝ remove it really");
            searchfieldcontainer.getViewTreeObserver().removeOnGlobalLayoutListener(keyboardVisibilityListener);
        } else Log.d(TAG, "removeKeyboardVisibilityListener ➝ doesn't exist, so no remove psbl");
        isgloballayoutlisteneradded = false;
    }


    private void handleKeyboard(boolean bnvvisible) { //otherstuff: bnv + cat, also wenn otherstuffvis=true➝keyboard:gone
        Log.d(TAG, "handleKeyboard  -  bnvvisible:"+ bnvvisible+",keyboardvisible:"+!bnvvisible);
        int visibility = bnvvisible == true ? View.VISIBLE : View.GONE;
        /*//temoprarily shut down gll bc. gll might be turned on due to call of addKeyboardVisibilityListener in moveSearchfield()
        Log.d(TAG, "hK: temporarily shut down onGlobalLayoutListener");
        removeKeyboardVisibilityListener(); //handleKeyboard*/ //TODO 19.1. gll ➝ onpredrawnlistener, wegen predrawn brauche ich auch delay ned mehr i think
        //Log.d(TAG, "hK: temporarily shut down removeOnPreDrawCategoriesLayoutListener");
        //removeOnPreDrawCategoriesLayoutListener(rlsearchfield); //TODO 20.1.

        //1 bnv
        View botnavi = mainActivityRootView.findViewById(R.id.bottomnavigationid);
        botnavi.setVisibility(visibility);

        /*//das folgende, damit das searchfield direkt über der Tastatur is:
        if (bnvvisible) {
            Log.d(TAG, "hK: keyboard gone ➝ searchfield normal/wieder hoch (bottommargin 66)");
            //TODO (C) (searchfraG) (visibility) (cat) cats sind zwar 50ms noch invis, aber die ganze Zeit über clickable ... najua macht kaum was (C)
            Handler handler = new Handler();
            Runnable delayedCode = () -> {
                //3.1 fab
                //EventBus.getDefault().post(new ChangeFabVisibility(true)); //aus searchfrag kam Befehl, dass fab bei openkeyboard weg soll, aber jetzt ist er GANZ weg im searchfrag 10.1.
                //3.2 categories
                includedcategoriescontainer.setVisibility(View.VISIBLE);
            };
            handler.postDelayed(delayedCode, 50);
        }
        else {
            Log.d(TAG, "hK: keyboard open ➝ searchfield runna (bottommargin 10)");
            //wenn keyboard geöffnet wird, kann instantamente proceedet werde
            //3.1 fab
            //EventBus.getDefault().post(new ChangeFabVisibility(false)); //aus searchfrag kam Befehl, dass fab bei openkeyboard weg soll, aber jetzt ist er GANZ weg im searchfrag 10.1.
            //3.2 categories
            includedcategoriescontainer.setVisibility(visibility); //View.GONE
        }*/ //11.1. brauch i ned mer, weil includedcategoriescontainer durch popupwindow ersetzt wurde


        //2 show/hide collapsed memoFrag if it's there
        if (Main.isMemoFragOpened) {
            Log.d(TAG, "hK: memo frag currently opened " +
                    "\n➝ eventbus order ManipulateBSBonKeyboardChange:"+ bnvvisible);
            //OLD ManipulateBSBonKeyboardChange in Main
            //OLD ManipulateBSBonKeyboardChange: Main bsb dragging aussschalde && memoFrag: collapsedll invisibile + unclickable mache
            EventBus.getDefault().post(new ManipulateBSBonKeyboardChange(bnvvisible));

            //TODO (weiter) 13.12. AHHH dieser part is wichtig, um das searchfield runnerzusetzen wenn keyboard an is... ABER löst globallayoutlistener aus...

            //das folgende, damit das searchfield direkt über der Tastatur is:
            if (bnvvisible) {
                Log.d(TAG, "hK: keyboard gone ➝ searchfield normal/wieder hoch (bottommargin 66)");
                moveSearchfieldContainer(66); //handleKeyboard gone
            }
            else {
                Log.d(TAG, "hK: keyboard open ➝ searchfield runna (bottommargin 10)");
                moveSearchfieldContainer(10); //handleKeyboard open
            }
        }

        /*//4 add gll again after 50ms (after the layoutchange has gone trhough) (this privents the infinite triggering of gll)
        Handler handler = new Handler();
        Runnable delayedCode = () -> {
            addKeyboardVisibilityListener(); //handleKeyboard
            Log.d(TAG, "hK: (prev. shut down tempor.) onGlobalLayoutListener added again after 100ms (cuz above is alr. 50ms)");
        };//TODO (9.1.) delay auf 100ms erhäht, weil oben schon 50ms
        handler.postDelayed(delayedCode, 100); */ ////TODO 19.1. gll ➝ onpredrawnlistener, wegen predrawn brauche ich auch delay ned mehr i think

        /*Handler handler = new Handler();
        Runnable delayedCode = () -> {
            addOnPreDrawCategoriesLayoutListener(rlsearchfield); //handleKeyboard
            Log.d(TAG, "hK: (prev. shut down tempor.) OnPreDrawCategoriesLayoutListener added again after 50ms");
            isOnPreDrawListenerEnabled = true;
        };
        handler.postDelayed(delayedCode, 50);*/ //TODO 20.1.
    }

    //0 showData
    public void showData() {
        Log.v(TAG, "showData called");
        //TODO (B) (future) (search) statt hier random stoggs anzuzeigen, nach cat fields ordnen (meinetwegen Rotation Tag für Tag andere cat) und random stocks daraus anzeigen

        CatQueries searchquerycatQueries = new CatQueries();
        searchquerycatQueries.doCatQueries(CategoryView.searchquerycatsList, mongoAudiosCollection, false, requireContext(), mainViewModel, getView(), null, null, "search");

        //TODO (weiter) 13.12. doppelung von einträgen, weil rv nicht resettet
        //TODO (weiter) 13.12. alreadylistenedmemos markieren
        //shows at first start random recent memos, when searched before and utente returns to this frag, previous search results are kept
        //TODO (B) (search) (display) ➝ search results remain in the RV when user switches between fragmetns...

        /*//1 build task depending on whether alreadyListenedMemos is empty or not
        FindIterable<Document> findIterable = mongoAudiosCollection.find().limit(pageSizeSearch).sort(Sorts.descending("_id"));
        RealmResultTask<MongoCursor<Document>> findTask = TaskBuilder.buildTask(findIterable, null, "search"); //showData

        //2 perform query
        findTask.getAsync(task -> {
            //Log.d(TAG, "getAsync done: recent"); //war nur, um zu sehen, bei welchem filter / bei welcher _id nen error auftritt
            if (task.isSuccess()) {
                Log.v(TAG, "successfully did search query");
                //(info) A MongoCursor<Document> represents the result set obtained from a query
                MongoCursor<Document> docset = task.get();
                if (docset.hasNext()) {
                    processAudioDocandaddtomodelList(docset, "search", 0, false);
                    //set adapter
                    adaptersearch.notifyDataSetChanged();
                    //setAdaptersearch(modelListsearch); //showData
                    //adaptersearch.addEntries(modelListsearch);
                    //adapter.notifyDataSetChanged();

                    //modellist resetten, nachdem in adapter gepackt (damit nächstes Mal wieder nur neue Einträge hinzuk)
                    //modelListsearch.clear();
                }
            } else {
                Log.e(TAG, "failed to do search query: ", task.getError());
            }
        });*/ //TODO 15.1. jetzt hier nur noch CatQueries, und in CatQueries wird if-Abfrage gemacht!
    }




//1 searchData

    //TODO (info) ich gehe jetzt erstmal davon aus, dass die "name_de" und "name" in categories collection jew. Kategoriennamen in der Landessprache enthalten (z.B. "name_de" enthält 'Natur', nicht 'Nature')

    //TODO (B) Der User sollte hier noch irgendwie einstellen können, wie dieser loop sucht :
    //   oder .orderBy("date").startAt(custom_start_time) (z.B. nur vor 2010 querien)
    // - nach Sprache [facile] ➝ .whereEqualTo("language", desiredlanguage) [insofern sind category names in firebase auf deutsch unnötig ??!!]
    // - nach Beliebtheit [hierzu muss ich noch ein field erstellen in jedem doc. in "audios", das +1 rechnet wenn jemand es anhört, also auf den Abspielenbutton klickt / Memoryseite öffnet]
    // - nach persönlichem Interesse [hierzu muss User Interessenskategorien angeben]
    // - nach kleinen Creators [hierzu auf selbes field zurückgreifen wie bei Beliebtheit, nur eben die "unbeliebten" anzeigen]
    // GRUNDEINSTELLUNG/Ordnung der Suchergebnisse should be Beliebtheit!


    //TODO (frage) brauche ich nicht so oder so 2 searchdatas (1 ohne categories chosen by searcher, 1 mit categories), weil wenn ich 1 mache, müsste ich als argument categories einlesen, aber
    // wenn es diesbzgl. keine gibt, stellt sich das schwierig heraus.
    //TODO (info) ich gehe jetzt erstmal davon aus, dass die "name_de" und "name" in categories collection jew. Kategoriennamen in der Landessprache enthalten (z.B. "name_de" enthält 'Natur', nicht 'Nature')
    private void searchData() {
        Log.d(TAG,"searchData called (searchtermio:"+searchtermio+")");

        //TODO 15.1. selectedlang parameter change as soon as user clicks langbutton

        CatQueries searchquerycatQueries = new CatQueries();
        searchquerycatQueries.doCatQueries(CategoryView.searchquerycatsList, mongoAudiosCollection, false, requireContext(), mainViewModel, getView(), searchtermio, selectedlang, "search");

     /*   //1 create query
        //check whether the selected searchquerycats fit to any memos
        RealmResultTask<MongoCursor<Document>> findTask;
        if (CategoryView.searchquerycatsList.isEmpty()) {
            //wenn searchquerycatsList leer is, einfach regex query
            Bson searchQuery = Filters.regex("title", searchtermio, "i"); //searchData
            FindIterable<Document> findIterable = mongoAudiosCollection.find(searchQuery).limit(pageSizeSearch);
            findTask = TaskBuilder.buildTask(findIterable, null, "search"); //searchData
            //perform query
            findTask.getAsync(task -> {
                //Log.d(TAG, "getAsync done: recent"); //war nur, um zu sehen, bei welchem filter / bei welcher _id nen error auftritt
                if (task.isSuccess()) {
                    Log.v(TAG, "successfully did search query");
                    //(info) A MongoCursor<Document> represents the result set obtained from a query
                    //1 browse through the docset and get the doc data
                    MongoCursor<Document> docset = task.get();
                    //nur, wenn mind. 1 doc im docset enthalten ist, retrieve docs
                    if (docset.hasNext()) {
                        processAudioDocandaddtomodelList(docset, "search", 0);
                        //set adapterr
                        adaptersearch.notifyDataSetChanged();
                        //setAdaptersearch(modelListsearch); //searchData
                        //adaptersearch.addEntries(modelListsearch);
                        //adapter.notifyDataSetChanged();

                        //modellist resetten, nachdem in adapter gepackt (damit nächstes Mal wieder nur neue Einträge hinzuk)
                        //modelListsearch.clear();
                    }

                } else {
                    Log.e(TAG, "failed to do search query: ", task.getError());
                }
            });
        } else {
            //wenn searchquerycatsList ned leer is, regex + cat query
            CatQueries searchquerycatQueries = new CatQueries();
            searchquerycatQueries.doCatQueries(CategoryView.searchquerycatsList, mongoAudiosCollection, false, requireContext(), mainViewModel, getView(), searchtermio, "search"); //TODO 11.1. (false???))
        }*/ //TODO 11.1. jetzt hier nur noch CatQueries, und in CatQueries wird if-Abfrage gemacht!
    }

    
    private void queryNextPage() {
        Log.d(TAG, "queryNextPage called");

        //enable progressbar while data is loading
        progressBar.setVisibility(View.VISIBLE);

        CatQueries searchquerycatQueries = new CatQueries();
        searchquerycatQueries.doCatQueries(CategoryView.searchquerycatsList, mongoAudiosCollection, true, requireContext(), mainViewModel, getView(), searchtermio, selectedlang,"search");

        //TODO (queryNextPage) if the user wants to load new memos in RV, and there are none ➝ notify user!!
        //TODO (classizing) (search) basically dieselbe method ist auch in Main... sol.lte ich hieraus ne class machen?

        /*//return when already fetching
        Log.d(TAG, "isUploadedFetchingData is " + isQueryingData + " ➝ wenn TRUE: END METHOD DIRECTLY (queryNextPage)");
        if (isQueryingData) return; // Prevent concurrent fetches, end method
        isQueryingData = true;

        //1 creeait the query
        Bson searchQuery = Filters.regex("title", searchtermio, "i"); //fNP
        FindIterable<Document> findIterable;
        if (searchtermioExists) findIterable = mongoAudiosCollection.find(searchQuery).limit(pageSizeSearch);
        else findIterable = mongoAudiosCollection.find().limit(pageSizeSearch);
        RealmResultTask<MongoCursor<Document>> findTask = TaskBuilder.buildTask(findIterable, alreadyQueriedmemossearch, "search");
       
        //3 perform query
        findTask.getAsync(task -> {
            //Log.d(TAG, "getAsync done: recent"); //war nur, um zu sehen, bei welchem filter / bei welcher _id nen error auftritt
            if (task.isSuccess()) {
                Log.v(TAG, "successfully did search query");
                //(info) A MongoCursor<Document> represents the result set obtained from a query
                //1 browse through the docset and get the doc data
                MongoCursor<Document> docset = task.get();
                //nur, wenn mind. 1 doc im docset enthalten ist, retrieve docs
                if (docset.hasNext()) {
                    processAudioDocandaddtomodelList(docset, "search", 0, true);
                    //set adapterrr
                    adaptersearch.notifyDataSetChanged();
                    //setAdaptersearch(modelListsearch); //qNP
                    //adaptersearch.addEntries(modelListsearch);
                    //adapter.notifyDataSetChanged();

                    //modellist resetten, nachdem in adapter gepackt (damit nächstes Mal wieder nur neue Einträge hinzuk)
                    //modelListsearch.clear();
                }
                isQueryingData = false;
                progressBar.setVisibility(View.GONE);
            } else {
                Log.e(TAG, "failed to do search query: ", task.getError());
                progressBar.setVisibility(View.GONE);
            }
        }); */ //TODO 15.1. jetzt hier nur noch CatQueries, und in CatQueries wird if-Abfrage gemacht!
    }


//2 create buttons dynamically (or read them out before already in main so that its there when needed)

//TODO (A) 1. mal sollen die categories groß angezeigt werden, dann geht man in die suche und dann sollen sie irgendwie kleiner angezeigt werden (die die man ausgewählt hat
// oder alle kategorien irgendwie komprimiert) . suche nach einem Weg, wie man die Menge an categories easily accessible , aber nicht follando anzeigen kann... 27.6.

//TODO (B) diese method in main schon auslesen, aber UNABHÄNGIG VOM VIEW , also nur die connection zu firebase + die buttons einlesen , der View wird dann hier im fragment erst erstellt wenn letzteres geopened wird
    //rows für hsv mit buttons dynamically read in from firebase collection categories füllen
    //open the overall stuff with this method:
    private int spToPx(float sp) { //benötigt für calculateButtonLength bei HSV
        float density = getResources().getDisplayMetrics().scaledDensity;
        return (int) (sp * density);
    }
    //TODO (B) eig. sollte es so sein, dass man maximal 2 kategorien pro zeile auswählen kann oder so . habe ich aber noch nicht implementiert kein bock
    //TODO( A) 1/2 the user shall be able to create 1 category by himself for the audio, e.g. "la bomba festival" ... and then the category is created and others can chooooose it for their audio without having to create if for themselves
    //TODO (A) 2/2 PS when user wants to create a category that is similar (e.g. "la bomba") ➝ then maybe INFORM him that already exists a similar one which he can take ... or sth. :|  I have to consider this ...
   





    //auf diese method greife ich aus Main zu, um von dort aus on Reclick of search icon das searchfield zu klaparieren
    public void openSearchfield() {
        Log.d(TAG, "openSearchfield");
        searchfield.requestFocus();
        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(searchfield, InputMethodManager.SHOW_IMPLICIT);
        //keyboardopened = true;
    }

    //ctegory button anklicken und in array hinzufügen, welches den category-filter verwaltet
    View.OnClickListener buttonClickListener = v -> {
        Log.d(TAG, "cat button geklickt");
        Button clickedButton = (Button) v;
        //1 prüfen, welcher button (id, nicht text weil sonst präkitationen mit SP) geklickt wird + herausfinden ob schon in pc:
        String clickedbuttoncategory = clickedButton.getText().toString(); //button text für categoryname nehmen + umwandeln in sprachenunabhängigen categoryname (button id kan man ned nehme, weil probleme mit neu-bildung von id bei neustart etc.)
        //für deutsche buttons muss der clickedbuttoncategory string umgewandelt werden in die pc-version (standarmäßig engl.) (und für esp, fra, it, ...):
        //TODO (future) (languages) hinzufügen
        switch (clickedbuttoncategory) {
            case "Party": clickedbuttoncategory = "party";break;
            case "Festival": clickedbuttoncategory = "festival";break;
            case "Club": clickedbuttoncategory = "club";break;
            case "Arbeit": clickedbuttoncategory = "work";break;
            case "Schule": clickedbuttoncategory = "school";break;
            case "Uni": clickedbuttoncategory = "university";break;
            case "Erasmus": clickedbuttoncategory = "erasmus";break;
            case "Abenteuer": clickedbuttoncategory = "adventure";break;
            case "Illegal": clickedbuttoncategory = "illegal";break;
            case "Natur": clickedbuttoncategory = "nature";break;
            case "Reisen": clickedbuttoncategory = "travel";break;
            case "Ausflug": clickedbuttoncategory = "trip";break;
            case "Beziehung": clickedbuttoncategory = "relationship";break;
            case "Familie": clickedbuttoncategory = "family";break;
            case "Freunde": clickedbuttoncategory = "friends";break;
            case "Traum": clickedbuttoncategory = "dream";break;
            case "fröhlich": clickedbuttoncategory = "happy";break;
            case "glücklich": clickedbuttoncategory = "lucky";break;
            case "erleichtert": clickedbuttoncategory = "relieved";break;
            case "verliebt": clickedbuttoncategory = "inlove";break;
            case "in love": clickedbuttoncategory = "inlove";break; //omg .. gut dass aufgefallen
            case "traurig": clickedbuttoncategory = "sad";break;
            case "depressiv": clickedbuttoncategory = "depressive";break;
            case "einsam": clickedbuttoncategory = "lonely";break;
            case "nostalgisch": clickedbuttoncategory = "nostalgic";break;
            case "wütend": clickedbuttoncategory = "angry";break;
            case "ängstlich": clickedbuttoncategory = "anxious";break;
            case "verzweifelt": clickedbuttoncategory = "desperate";break;
            //TODO (new categories) buttonClickListener [SearchFrag]
        }
        //wenn array category onbuttonclick bereits containt ➝ removen , raus ausm array
        if (selectedCategoriesArray.contains(clickedbuttoncategory)) {
            selectedCategoriesArray.remove(clickedbuttoncategory);
            //1 retrieve current color
            int currentColor = (int) clickedButton.getTag();
            Log.d(TAG,"currentColor of " + clickedbuttoncategory + " is: " + currentColor + " (in: ADD cat to pref)");
            //2 set background color based on color before
            if (currentColor == blueish) {
                clickedButton.setBackgroundColor(grayedblueish);
                clickedButton.setTag(grayedblueish);}
            else if (currentColor == violet) {
                clickedButton.setBackgroundColor(grayedviolet);
                clickedButton.setTag(grayedviolet);}
            else if (currentColor == cyan) {
                clickedButton.setBackgroundColor(grayedcyan);
                clickedButton.setTag(grayedcyan);}
            else if (currentColor == red) {
                clickedButton.setBackgroundColor(grayedred);
                clickedButton.setTag(grayedred);}
            else if (currentColor == darkred) {
                clickedButton.setBackgroundColor(grayeddarkred);
                clickedButton.setTag(grayeddarkred);}
            else if (currentColor == green) {
                clickedButton.setBackgroundColor(grayedgreen);
                clickedButton.setTag(grayedgreen);}
            else if (currentColor == lightgreen) {
                clickedButton.setBackgroundColor(grayedlightgreen);
                clickedButton.setTag(grayedlightgreen);}
            else if (currentColor == orange) {
                clickedButton.setBackgroundColor(grayedorange);
                clickedButton.setTag(grayedorange);}
            //TODO (new categories) buttonClickListener [SearchFrag]
            Log.d(TAG,"updated color of " + clickedbuttoncategory + " is: " + clickedButton.getTag());
            Log.d(TAG,clickedbuttoncategory + " removed from array");
        }
        //wenn array category onbuttonclick not yet containt ➝ adden , ab ins array rein damit
        else {
            selectedCategoriesArray.add(clickedbuttoncategory);
            //1 retrieve current color
            int currentColor = (int) clickedButton.getTag();
            Log.d(TAG,"currentColor of " + clickedbuttoncategory + " is: " + currentColor + " (in: ADD cat to pref)");
            //2 set background color based on color before
            if (currentColor == grayedblueish) {
                clickedButton.setBackgroundColor(blueish);
                clickedButton.setTag(blueish);}
            else if (currentColor == grayedviolet) {
                clickedButton.setBackgroundColor(violet);
                clickedButton.setTag(violet);}
            else if (currentColor == grayedcyan) {
                clickedButton.setBackgroundColor(cyan);
                clickedButton.setTag(cyan);}
            else if (currentColor == grayedred) {
                clickedButton.setBackgroundColor(red);
                clickedButton.setTag(red);}
            else if (currentColor == grayeddarkred) {
                clickedButton.setBackgroundColor(darkred);
                clickedButton.setTag(darkred);}
            else if (currentColor == grayedgreen) {
                clickedButton.setBackgroundColor(green);
                clickedButton.setTag(green);}
            else if (currentColor == grayedlightgreen) {
                clickedButton.setBackgroundColor(lightgreen);
                clickedButton.setTag(lightgreen);}
            else if (currentColor == grayedorange) {
                clickedButton.setBackgroundColor(orange);
                clickedButton.setTag(orange);}
            //TODO (new categories) buttonClickListener [SearchFrag]
            Log.d(TAG,"updated color of " + clickedbuttoncategory + " is: " + clickedButton.getTag());
            Log.d(TAG,clickedbuttoncategory + " added to array an stelle" + selectedCategoriesArray.indexOf(clickedbuttoncategory));
        }


    }; //10.1. jetzt in CategoryView


    //TODO (A) wenn man einen button clickt, wird die category des buttons (e.g. "party") hinzugefügt zum Array ➝ wenn dann die Suche basierend auf diesen Arrayeinträgen geschieht,
    // dann sollen nur die audios gefiltert werden, die genau die gesuchten categories haben

    //im falle, dass in saved instance state (SIS) das selectedCategoriesArray nicht leer war: jew. buttons anmalen
    private void restoreButtonColors() {
        Log.d(TAG, "restoreButtonColors called");
        for (String category : selectedCategoriesArray) { //iterates over each element in the Array and calls those Strings "category"
            switch (category) {
                case "party" -> {
                    party.setBackgroundColor(violet);
                    party.setTag(violet);
                }
                case "festival" -> {
                    festival.setBackgroundColor(violet);
                    festival.setTag(violet);
                }
                case "club" -> {
                    club.setBackgroundColor(violet);
                    club.setTag(violet);
                }
                case "work" -> {
                    work.setBackgroundColor(orange);
                    work.setTag(orange);
                }
                case "school" -> {
                    school.setBackgroundColor(orange);
                    school.setTag(orange);
                }
                case "university" -> {
                    university.setBackgroundColor(orange);
                    university.setTag(orange);
                }
                case "erasmus" -> {
                    abroad.setBackgroundColor(orange);
                    abroad.setTag(orange);
                }
                case "adventure" -> {
                    adventure.setBackgroundColor(green);
                    adventure.setTag(green);
                }
                case "illegal" -> {
                    illegal.setBackgroundColor(green);
                    illegal.setTag(green);
                }
                case "nature" -> {
                    nature.setBackgroundColor(green);
                    nature.setTag(green);
                }
                case "travel" -> {
                    travel.setBackgroundColor(green);
                    travel.setTag(green);
                }
                case "trip" -> {
                    trip.setBackgroundColor(green);
                    trip.setTag(green);
                }
                case "relationship" -> {
                    relationship.setBackgroundColor(red);
                    relationship.setTag(red);
                }
                case "family" -> {
                    family.setBackgroundColor(red);
                    family.setTag(red);
                }
                case "friends" -> {
                    friends.setBackgroundColor(red);
                    friends.setTag(red);
                }
                case "dream" -> {
                    dream.setBackgroundColor(cyan);
                    dream.setTag(cyan);
                }
                case "happy" -> {
                    happy.setBackgroundColor(lightgreen);
                    happy.setTag(lightgreen);
                }
                case "lucky" -> {
                    luck.setBackgroundColor(lightgreen);
                    luck.setTag(lightgreen);
                }
                case "relieved" -> {
                    relieved.setBackgroundColor(lightgreen);
                    relieved.setTag(lightgreen);
                }
                case "inlove" -> {
                    inlove.setBackgroundColor(lightgreen);
                    inlove.setTag(lightgreen);
                }
                case "sad" -> {
                    sad.setBackgroundColor(blueish);
                    sad.setTag(blueish);
                }
                case "depressive" -> {
                    depressive.setBackgroundColor(blueish);
                    depressive.setTag(blueish);
                }
                case "lonely" -> {
                    lonely.setBackgroundColor(blueish);
                    lonely.setTag(blueish);
                }
                case "nostalgic" -> {
                    nostalgic.setBackgroundColor(blueish);
                    nostalgic.setTag(blueish);
                }
                case "angry" -> {
                    angry.setBackgroundColor(darkred);
                    angry.setTag(darkred);
                }
                case "anxious" -> {
                    anxious.setBackgroundColor(darkred);
                    anxious.setTag(darkred);
                }
                case "desperate" -> {
                    desperate.setBackgroundColor(darkred);
                    desperate.setTag(darkred);
                }
            }
            Log.d(TAG, "color of cat " + category + " updated");
        }

    }

    private void setBackgroundofimagebutton(ImageView imagebutton, int imageresource) {
        imagebutton.setImageResource(imageresource);
    }

    public void displaysearchmemos(List<Model> toaddmodellistsearch) {
        //(info) the here passed "toaddmodellistsearch" contains only the new entries :9
        Log.d(TAG, "displaysearchmemos called (ordered through VM from CatQueries)" +
                // "\nget the "+ pageSizeHome +" memos best-fitting the user's pref cat" +
                // "\ncurrentmodelListforyou ("+modelListforyou.size()+"): "+ modelListforyou +
                "\ntoaddmodellistsearch ("+toaddmodellistsearch.size()+"): "+ toaddmodellistsearch);

        //set adapter
        //setAdapterforyou(toaddmodellistsearch);
        //adapterforyou.notifyItemRangeInserted(currentitemslol, toaddmodellistsearch.size());
        //adapterforyou.addEntries(modelListforyou, toaddmodellistsearch);
        //toaddmodellistsearch resetten, nachdem in adapter gepackt (damit nächstes Mal wieder nur neue Einträge hinzuk)
        adaptersearch.notifyDataSetChanged();

        progressBar.setVisibility(View.GONE);

        //TODO (.clear) wo kommt clear hin?? an den Anfang von load recent data natürlich!
        //modelListforyou.clear();
        //progressbarforyou.setVisibility(View.GONE); //für foryou wird's in CatQueries über VM gemacht :D
    }

    private void setVisibilityProgressBar(Integer integer) {
        Log.d(TAG, "setVisibilityProgressBar called, 8:gone:" + integer); //TODO debug
        progressBar.setVisibility(integer);
    }

}