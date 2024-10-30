package com.example.remember;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.remember.categories.CategoryNameAndIndex;
import com.example.remember.databinding.ActivityMainBinding;
import com.example.remember.events.ManipulateBSBonKeyboardChange;
import com.example.remember.events.OpenSearchfield;
import com.example.remember.mongo.CatQueries;
import com.example.remember.mongo.RecentQuery;
import com.example.remember.publish.Publish;
import com.example.remember.recycler_and_scrollviews.CustomAdapter;
import com.example.remember.ui.home.HomeFragment.HomeFragmentListener;
import com.example.remember.ui.memo.MemoFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.mongodb.MongoClientSettings;

import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import io.realm.mongodb.App;
import io.realm.mongodb.AppConfiguration;
import io.realm.mongodb.User;
import io.realm.mongodb.mongo.MongoClient;
import io.realm.mongodb.mongo.MongoCollection;
import io.realm.mongodb.mongo.MongoDatabase;


public class Main extends AppCompatActivity implements HomeFragmentListener, CustomAdapter.CustomAdapterListener/*, SearchFragment.SearchFragmentListener*/ {
    private static final String TAG = "MAIN";

    //TODO (info) insane wäre es gewesen, die audioIDs so aufzubauen, dass in ihnen alle infos des audios enthalten sind (und mit _ separated), und dann ganz am Ende von der ID eben eine ganz individuelle ID wäre zB [...]_408934
    // -> aus solchen IDs könnte man gleich alles rauslesen, ohne querien zu müssen - was zB sehr nützlich wäre bei der Anzeige der listened/uploaded audios in ProfileFrag (weil da die audioIDs feststehen und momentan dazu
    //    genutzt werden müssen, um in der audiosColl zu querien) -> Prökitation: dynamische Audiodaten.. zB listenerscount oder Sterne - die ändern sich dauernd und daher wäre höchstens für listened-RV eine valide Idee (weil da wäre mir nur Titel + Creator + Dauer wichtig und die sind final)

    //binding für den bottom navigation bar + die fragments (?) bissl undurchsichtig...
    public ActivityMainBinding binding;

//ui
    //fab
    //FloatingActionButton fab;
    PopupWindow popupWindow;
    //private static final int PICKFILE_REQUEST_CODE = 61; //intent zu Publish NOT NEEDED anymore due to new version 19.11.
    BottomNavigationView bnv;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    //private ExtendedFloatingActionButton bottomsheetstatetv; //TODO kannweg (war nur debug bsb)

    //current language
    private String currentAppLanguage, currentMemoLanguage; //TODO (main) was ist das?? marc hä was willst du asslam wallaH

    //shared pref
    SharedPreferences languagePreferences, mainstartPreferences, catPref;

    //mongoDB realm initializa
    MongoDatabase mongoDatabase;
    MongoClient mongoClient;
    MongoCollection<Document> mongoUsersCollection, mongoAudiosCollection, mongoCategoriesCollection;
    //MongoCollection<Category> mongoCategoriesCollection;
    User user;
    public App app;
    String userid, usermail;

    //mongo username wird in main eingelesen
    public static String username;

//stuff for retrieving the memos in the views
    //model list //(info) wird jetzt in Main eingelesen und nur hier angezeigt (über viewmodel)
    MainViewModel mainViewModel;
    //für foryou: die in den SP gespeicherten fav. categories müssen ausgelesen werden, daher SP:
    public static List<String> prefCatList = new ArrayList<>();
    //die already listened memos sollen natürlich nicht angezeigt werden in homeFrag:
    public static List<String> alreadylistenedmemoidsList = new ArrayList<>();
    //die memos, die im recent / foryou RV angezeigt werden, sollen nicht doppelt angezeigt werden loL!:
    //ArrayList, die alle von firestore eingelesenen Category enthält , und zwar die jew. Kombination aus name + id (public static, weil natürlich auch in Publish beim Hochladen von memo genutzt wird)
    public static List<CategoryNameAndIndex> categoryNameAndIDList = new ArrayList<>();

    //pagination for home
    public static int pageSizeHome = 6; // Number of items to fetch per page !!! IN HOME XD danke chat
    public static int pageSizeProfile = 16; // Number of items to fetch per page !!! IN PROFILE
    public static int pageSizeSearch = 12; // Number of items to fetch per page !!! SEARCH RESULTS


    //boolean prefCatHaveChanged für foryou: falls man pref cat in profileFrag ändert, muss natürlich eine neue query für foryou memories stattfinden, um anschließend in homeFrag die neuen suchergebnisse anzuzeigen
    public static boolean prefCatHaveChanged = false;
    //amountofprefcat wird genutzt für if-clauses, um die foryoulist zu emergieren
    public static int amountofprefcat;
    private boolean bsbandplayingservicesetup;


    //private int currentrecentmemos;
    //wenn ein utente eine pref cat removed, und dann doch wieder added, soll natürlic
    // h nicht neugeladen werden!!! wie schafft man das 00:08 am 18.10.2023 xD marcoushStylz

    //searchfield openen in searchFrag von Main aus:
    //private MainListener listenerCommunicationMainToSearchfrag;//stattdessen Eventbus
    //global boolean isMemoFragOpened, um zB in searchFrag abzufragen wg. searchBar:
    public static boolean isMemoFragOpened; //TODO (11.12.23 könnte stattdessen auch in memoFrag public static String title != null abfragen.. dann könnte das hier wech)
    //eventbus for all the communication betw main + fragments

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onManipulateBSB(ManipulateBSBonKeyboardChange event) {
        Log.d(TAG, "onManipulateBSB, visible:" + event.visible);
        if (event.visible) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            bottomSheetBehavior.setHideable(false);
        } else {
            bottomSheetBehavior.setHideable(true);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
        //bottomSheetBehavior.setDraggable(event.visible); //13.12.23 geht ned, black screen overlaps searchfield... das+llcollapsedvis=false + buttonsclickable=false AUSKOMMENTIER
        //bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);//geht ned, weil hideable muss aus sein, sonst kann man weghiden soll man ned könne
        //bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    /*@Subscribe(threadMode = ThreadMode.MAIN)
    public void onChangeFabVisibility(ChangeFabVisibility event) {
        Log.d(TAG, "onChangeFabVisibility, visible:" + event.visible);
        if (event.visible) fab.setVisibility(View.VISIBLE);
        else fab.setVisibility(View.GONE);
    }*/ //aus searchfrag kam Befehl, dass fab bei openkeyboard weg soll, aber jetzt ist er GANZ weg im searchfrag 10.1.


    //_______________________ONCrea_TEE___________________________________________________________________________________________OK_:)_______________________
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"onCreate Main");
        super.onCreate(savedInstanceState);

//-1 WEITERLEITUNG AUF LOGIN WENN KEIN USER LOGGED IN OR USER BEGRÜSSUNG (aber nur 1x)
        app = new App(new AppConfiguration.Builder("remember-dxcgp").build());
        user = app.currentUser();
        //TODO (mongo) users can read and write all currently (23.10.23)

//0 user (info) UNBEDINGT RETURN STATEMENT! , danke an chat für diese aufklärung ajajjajaj wie unnötig... vorher hatte es auch ohne funktioniert, wieso jetzt plötzlich ned mehr ß? ???????ßwßw3w
        if (user == null) { //danke an chatGPT <3 :) (NAJA, der code SUCKT ASS)
            Log.d(TAG,"user = null -> Login");
            // No user is signed in, show the login activity:
            Intent intent = new Intent(this, Login.class); //TODO (B) warum auf login page weiterleiten und nicht auf register? wenn man logged in ist, dann wird man doch eh in Main weitergeleitet - und wenn man nicht logged in ist und die app das 1. mal nutzt, will man sich registrieren
            startActivity(intent);
            this.finish(); //Naim wird geschlossen und kann nicht durch zurückswipen erreicht werden! (wichtig!)
            Log.d(TAG,"Main sollte jetzt gefinisht werden und intent auf login passieren");
            return; // Add this return statement to exit the onCreate() method
        }
        else { // User is signed in -> rest of onCreate code
            // show welcome message (not more in this code snap) TODO debug (kind of)
            Log.d(TAG,"user != null -> onCreate Main");
            user = app.currentUser();
            userid = user != null ? user.getId() : null; //sieht funny aus, deswegen hab ich's etabliert , wurde vorgshlagen in der mwmw heit franen :)
            usermail = user != null ? user.getProfile().getEmail() : null; //sieht funny aus, deswegen hab ich's etabliert , wurde vorgshlagen in der mwmw heit franen :)
            Log.d(TAG,"user: " + user +
                    "\nuserid: " + userid +
                    "\nusermail: " + usermail);

//1 Language
            //Get the languauage which is right now active and maybe change this activity's language to it (lang might have been changed in Settings that's why...)
            LanguageUtils languageUtils = new LanguageUtils();
            languageUtils.updateLanguage(this);
            //hier wird die systemlanguage herausgefunden und app-wide als static string festgelegt, der nicht geändert werden kann

            languagePreferences = getSharedPreferences("languagePreferences", Context.MODE_PRIVATE);
            currentAppLanguage = languagePreferences.getString("selectedAppLanguage", "system"); //current language muss bei onCreate auf die aktuelle spLanguage gesetzt werden, damit in onResume currentLanguage=spLanguage ist und kein recreate vonstatten geht
            currentMemoLanguage = languagePreferences.getString("selectedMemoLanguage", "system"); //current language muss bei onCreate auf die aktuelle spLanguage gesetzt werden, damit in onResume currentLanguage=spLanguage ist und kein recreate vonstatten geht
//2 Content View + Binding
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            //TODO (A) wenn man von memory aus zurück nach searchfrag geht, passiert hier wegen inflaten des layouts in searchfrag ein fehler, die log msg wird nicht gecallt..!

            Log.d(TAG,"ActivityMainBinding.inflate(getLayoutInflater()); called");
            setContentView(binding.getRoot());
            Log.d(TAG,"ContentView in main set");

            //BottomNavigationView navView = findViewById(R.id.bottomnavigationid);
            // Passing each menu ID as a set of Ids because each menu should be considered as top level destinations.
            NavController navController = Navigation.findNavController(this, R.id.nav_viewfragid);
            //NavController navController = NavHostFragment.findNavController(this); //TODO try this maybe to combat the error here ... idk just a glimpse 11.10.23
            //TODO(info) wiek ann das sein, dass ich diese line entfernen kann & es trotzdem buttrig schmiert, häää wieso ist das dann da
            AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(R.id.searchnavfragid, R.id.homenavfragid, R.id.profilenavfragid).build();
            // NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);  //TODO (info) hier würde fehlermeldung kommen, wenn ich aktiviere
            NavigationUI.setupWithNavController(binding.bottomnavigationid, navController);
//3 ui
            // Set up the toolbar (kann man leider ned outsource , oder ich & chatGTP wissen nichtr wie herzliche grüße aus dem juni beginn)
            Toolbar toolbar = findViewById(R.id.toolbarid);
            TextView toolbarTitle = findViewById(R.id.toolbartitleid);
            ImageButton toolbarSettingsButton = findViewById(R.id.toolbarsettingsid);
            ImageButton toolbarUploadButton = findViewById(R.id.toolbaruploadbuttonid);
            ImageButton toolbarRecordyourselfButton = findViewById(R.id.toolbarrecordyourselfbuttonid);
            setSupportActionBar(toolbar);
            Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
            // Set activity title
            String activityTitle = getString(R.string.remember);
            toolbarTitle.setText(activityTitle);
            // Handle ImageButton click
            toolbarSettingsButton.setOnClickListener(view -> {
                Intent intent = new Intent(this, Settings.class);
                startActivity(intent);
            });
            //Declare a variable for the contract 1/2 (new version of startActivityForResult)
            ActivityResultLauncher<String> getContent = registerForActivityResult(new ActivityResultContracts.GetContent(), selectedFileUri -> {
                if (selectedFileUri != null) {
                    Intent publishIntent = new Intent(this, Publish.class);
                    publishIntent.setAction("OPEN_SELECTED_FILE");
                    publishIntent.setData(selectedFileUri);
                    startActivity(publishIntent);
                }
            });
            toolbarUploadButton.setOnClickListener(v1 -> {
                //dateiauswahlfenster wird hier geöffnet, beim anklick der datei wird Publish geöffnet und datei mit übergeben
                getContent.launch("audio/*");
            });
            toolbarRecordyourselfButton.setOnClickListener(v1 -> {
                Intent intent = new Intent(this, Publish.class); //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //nichtm ehr nötig!!
                startActivity(intent);
            });
            //fab = findViewById(R.id.fabid);
            bnv = findViewById(R.id.bottomnavigationid);
            //bottomsheetstatetv = findViewById(R.id.bottomsheetstatetvid);

//4 mongodb stuff
            mongoClient = user.getMongoClient("mongodb-atlas");
            mongoDatabase = mongoClient.getDatabase("remember");
            // registry to handle POJOs (Plain Old Java Objects)
            //CodecRegistry pojoCodecRegistry = fromRegistries(AppConfiguration.DEFAULT_BSON_CODEC_REGISTRY,
            //        fromProviders(PojoCodecProvider.builder().automatic(true).build()));
            //TODO (info) (mongo) wenn Updates,Filters usen will, implementen: -> CodecRegistry defaultJavaCodecRegistry = MongoClientSettings.getDefaultCodecRegistry();
            CodecRegistry defaultJavaCodecRegistry = MongoClientSettings.getDefaultCodecRegistry();
            //(info) sicherheitshalber die codecregistry eingebaut für Sort
            mongoUsersCollection = mongoDatabase.getCollection("users").withCodecRegistry(defaultJavaCodecRegistry);
            mongoAudiosCollection = mongoDatabase.getCollection("audios").withCodecRegistry(defaultJavaCodecRegistry);
            mongoCategoriesCollection = mongoDatabase.getCollection("categories").withCodecRegistry(defaultJavaCodecRegistry);
            //mongoCategoriesCollection = mongoDatabase.getCollection("categories", Category.class).withCodecRegistry(pojoCodecRegistry);

//5 VM & prefcat
            //auslesen und dann über viewmodel anzeigen in frags (i guess)
            mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
            buildprefCatList();

//6 query for user doc to get public static username + alreadyListenedMemos
            Document queryFilter  = new Document("_id", userid);
            mongoUsersCollection.findOne(queryFilter).getAsync(task -> {
                //Log.d(TAG, "queryfilter doc is: " + queryFilter); //war nur, um zu sehen, bei welchem filter / bei welcher _id nen error auftritt
                if (task.isSuccess()) {
                    Document doc = task.get();
                    if (doc != null) {
                        Log.v(TAG, "successfully found user doc with "+userid+" ("+usermail+") in usersColl: " + doc);
                        username = (String) doc.get("username");
                        //1 get the already listend memos
                        List<Document> datelisteningsheartArray = doc.getList("datesoflistenings", Document.class);
                        // Iterate over the array and extract the 'id' field
                        if (datelisteningsheartArray != null) {
                            if (!datelisteningsheartArray.isEmpty()) {
                                for (Document dateListening : datelisteningsheartArray) {
                                    String id = dateListening.getString("id");
                                    if (id != null) {
                                        alreadylistenedmemoidsList.add(id);
                                        Log.d(TAG, "alreadylistenedmemoidsList add entry: " + id); //title is ned hinterlegt in datesoflistenings :D
                                    }
                                }
                            }
                        }
                        //damit die message userid ist ... und fUser na.me ...ist ... NUR EINMAL ANGEZEIGT WIRD WENN MAN APP STARTÖTÄT lo.ol..  ALL DAS NUR FÜR DIE 1 EINZIGE (UND NUR 1) MESSAGE AM ANFANG
                        mainstartPreferences = getSharedPreferences("MainActivityStartPreferences", MODE_PRIVATE);
                        boolean firstTimeOpeningMain = mainstartPreferences.getBoolean("firstTimeOpeningMain", true);
                        if (firstTimeOpeningMain) {
                            //(firsttimeopening) dieses Toast begrüßt den User erstmalig jauchzalei1 23oktober32 ist sonnig gerade ich lieb's und feiere das neue Layout xD In der Wissensschafffabrik XDDDDD
                            Toast.makeText(this, "Herzlich willkommen " + username, Toast.LENGTH_LONG).show();
                            // Update the flag to indicate that it's not the first time anymore
                            SharedPreferences.Editor editor = mainstartPreferences.edit();
                            editor.putBoolean("firstTimeOpeningMain", false);
                            editor.apply();
                        }
                        /*List<String> datesoflisteningsList = doc.getList("datesoflistenings", String.class);
                        //2 extract only the audioIDs from the datesoflisteningsList and create new idsList
                        List<String> idsList = new ArrayList<>();
                        for (String dateofid : datesoflisteningsList) {
                            idsList.add(dateofid.split("_")[3]);
                        }
                        alreadylistenedmemoidsList = idsList;
                        Log.d(TAG, "username: " + username +
                                "\nalreadyListenedMemos: " + alreadylistenedmemoidsList);*/ //old als datesoflistenings noch a compoosd sdring war :D
                        //3 jetzt retrieve recent + foryou
                        retrieveHomeRecent(); //onCreate
                        retrieveHomeForyou(); //onCreate
                    } else Log.e(TAG, "no user doc with "+userid+" ("+usermail+") found in usersColl");
                } else {
                Log.e(TAG, "query to find user doc with "+userid+" ("+usermail+") in usersColl -> error: ", task.getError());
                }
            });

//7.1 bnv memoFrag geht in collapsed mode, wenn man irgendein bnv element drückt, das nicht das fragment is, in dem man schon zuvor war
            bnv.setOnItemSelectedListener(item -> {
                //Log.d(TAG, "bnv.setOnItemSelectedListener");
                // Get the current destination (fragment) on the NavController
                int currentnavfragid = Objects.requireNonNull(navController.getCurrentDestination()).getId();
                int clickednavfragid = item.getItemId();
                if (currentnavfragid != clickednavfragid) {
                    Log.d(TAG, "bnv setOnItemSelectedListener & diff nav item clicked");
                    // Navigate to the clicked destination (after deleting the gll in searchFrag if existing)
                    if (currentnavfragid == R.id.searchnavfragid) {
                        //hier müsste im Bestfall der gll in searchfrag gekillt werden... weil das hier vor onPause / onStop search auslöst
                        View searchfragview = findViewById(R.id.searchnavfragid);
                        ViewTreeObserver viewTreeObserver = searchfragview.getViewTreeObserver();
                        // Define the global layout listener that you want to remove
                        //ViewTreeObserver.OnGlobalLayoutListener yourGlobalLayoutListener = getKeyboardVisibilityListener();

                    }
                    navController.navigate(clickednavfragid);
                    //navController.popBackStack(currentnavfragid, false); //LOL dann würde NAVI gar NED MEHR GEHE

                    //TODO (weiter) 4.12. & searchbar muss auch hochrücken, wenn memoFrag coll an ist!

                    //MemoFragment memoFragment = (MemoFragment) Main.this.getSupportFragmentManager().findFragmentById(R.id.memofragid);
                    //assert memoFragment != null; //wouldn't work probably for whatever reason ...
                    //when bottomSheetBehavior != null, then memoFrag has been started to show memo
                    if (bottomSheetBehavior != null) {
                        Log.d(TAG, "bottomSheetBehavior != null ➝ collapse memoFrag");
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    }
                }
                return true;
            });

//7.2 bnv (navigation onclick, damit man von memo frag aus ... usw. + wegen der kolorierung)
            bnv.setOnItemReselectedListener(item -> {
                Log.d(TAG,"bnv.setOnItemReselectedListener");
                //1 bottom sheet sempre einklappen quando presse navbutón (se c'è uno)
                if (Main.isMemoFragOpened) {
                    if (bottomSheetBehavior == null) {
                        Log.d(TAG, "bottomSheetBehavior == null ➝ setup first/again");
                        FragmentContainerView memofragview = findViewById(R.id.memofragid);
                        bottomSheetBehavior = BottomSheetBehavior.from(memofragview); //TODO (memofragview) (bsb) wird immer aufs neue gecallt...
                    }
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
                //2 Set a listener that will be notified when the currently selected navigation item is reselected
                //Reset color for all items
                for (int i = 0; i < bnv.getMenu().size(); i++) {
                    MenuItem menuItem = bnv.getMenu().getItem(i);
                    if (menuItem.getItemId() != item.getItemId()) {
                        menuItem.setIconTintList(ContextCompat.getColorStateList(this, R.color.color_item_unselected));
                    }
                }
                //Set color to current item
                int itemId = item.getItemId();
                if (itemId == R.id.homenavfragid) {
                    Log.d(TAG,"itemId == R.id.homenavfragid");
                    //TODO (porter duff koloriern)
                    item.setIconTintList(ContextCompat.getColorStateList(this, R.color.color_one));
                    //TODO 2 (wenn andere items dekoloriern)
                }
                else if (itemId == R.id.searchnavfragid) {
                    Log.d(TAG,"itemId == R.id.searchnavfragid");
                    item.setIconTintList(ContextCompat.getColorStateList(this, R.color.color_one));
                    //in searchFrag öffnet sich suchfenster, wenn man unten auf lupe nochmal drückt haha
                    EventBus.getDefault().post(new OpenSearchfield());
                    /*// Get the NavHostFragment
                    Fragment navFragment = getSupportFragmentManager().findFragmentById(R.id.nav_viewfragid);
                    Log.d(TAG,"navFragment is: " + navFragment);
                    assert navFragment != null;
                    Fragment searchFragment = navFragment.getChildFragmentManager().findFragmentById(R.id.searchnavfragid);
                    if (searchFragment instanceof SearchFragment searchFragmentInstance) {
                        searchFragmentInstance.openSearchfield();
                    }
                    //SearchFragment searchFragment = (SearchFragment) getSupportFragmentManager().findFragmentById(R.id.searchnavfragid);
                    Log.d(TAG,"searchFragment is: " + searchFragment);*/ //try to get reference to SearchFragment ➝ unsuccessful, build interface instead
                    /*if (listenerCommunicationMainToSearchfrag != null) {
                        listenerCommunicationMainToSearchfrag.onRetrieveSignaltoOpenSearchfield();
                    }*///stattdesesn eventbus
                }
                else { //if (itemId == R.id.profilenavfragid)
                    Log.d(TAG,"itemId == R.id.profilenavfragid");
                    item.setIconTintList(ContextCompat.getColorStateList(this, R.color.color_one));
                }
            });

//8 fab - TODO 1.4. put instead in toolbar
            /*//Inflate the custom layout for the popup window
            ViewGroup rootLayout = findViewById(R.id.relativelayoutmainid); // Replace `rootLayout` with the ID of the root layout in your activity
            View popupView = getLayoutInflater().inflate(R.layout.layout_fab_publish, rootLayout, false); //hier stand statt "rootLayout, false" nur "null", erzeugte aber warning
            popupWindow = new PopupWindow(popupView, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
            popupWindow.setAnimationStyle(R.style.PopupAnimation);
            //Declare a variable for the contract 1/2 (new version of startActivityForResult)
            ActivityResultLauncher<String> getContent = registerForActivityResult(new ActivityResultContracts.GetContent(), selectedFileUri -> {
                if (selectedFileUri != null) {
                    Intent publishIntent = new Intent(this, Publish.class);
                    publishIntent.setAction("OPEN_SELECTED_FILE");
                    publishIntent.setData(selectedFileUri);
                    startActivity(publishIntent);
                }
            });
            //fab: open the custom layout for the popup window
            fab.setOnClickListener(v -> { //fab = floating action button
                if (popupWindow.isShowing()) {
                    popupWindow.dismiss(); // Close the popup window - does automatically as well the animation :9
                } else {
                    // Show the popup window at the bottom-right corner of the screen
                    popupWindow.showAsDropDown(fab, 0, fab.getHeight() * -3); //does automatically the animation :7

                    // Handle button clicks in the popup menu
                    Button option1ButtonUpload = popupView.findViewById(R.id.uploadfromstoragebuttonid); //normale aufnahme in Publish (-> ?ohne chooseAudioFromPhone button oder mit?)
                    Button option2ButtonRecord = popupView.findViewById(R.id.recordyourselfbuttonid); //fenster für chooseaudiofromphone bereits geöffnet & schaltfläche unten ausgeblendet

                    option1ButtonUpload.setOnClickListener(v1 -> {
                        //dateiauswahlfenster wird hier geöffnet, beim anklick der datei wird Publish geöffnet und datei mit übergeben
                        //Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        //intent.setType("audio/*");
                        //startActivityForResult(intent, PICKFILE_REQUEST_CODE);
                        // Open the file selection window (new version with AndroidX) 2/2
                        getContent.launch("audio/*");
                    });

                    option2ButtonRecord.setOnClickListener(v1 -> {
                        Intent intent = new Intent(this, Publish.class); //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //nichtm ehr nötig!!
                        startActivity(intent);
                    });
                }
            });

            //hide fab in search-frag
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                if (destination.getId() == R.id.searchnavfragid) {
                    Log.d(TAG, "navControlelr: hide fab in searchfrag");
                    fab.setVisibility(View.GONE);
                    //fab.hide(); //das ging, aber .show() ging iwie ned
                } else {
                    Log.d(TAG, "navControlelr: show fab in home/profilefrag");
                    fab.setVisibility(View.VISIBLE);
                    fab.setClickable(true);
                    //fab.show(); //ging iwie ned
                }
            });*/ //TODO 1.4. fab ersetzt mit toolbar

//9 onBckpressd
            // Create an OnBackPressedCallback
            OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    Log.d(TAG, "handleOnBackPressed");
                    NavController navController = Navigation.findNavController(Main.this, R.id.nav_viewfragid);
                    Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.nav_viewfragid);
                    //1 wenn man nicht in homefrag ist, soll back-button in homeFrag leiten
                    int currentnavfragid = Objects.requireNonNull(navController.getCurrentDestination()).getId();
                    if (currentnavfragid != R.id.homenavfragid) {
                        //Log.d(TAG, "current frag != home ➝ gotohome");
                        navController.navigate(R.id.homenavfragid);
                    }
                    //2 wenn memoFrag aktiv is, navigate back to the prev. fragment in the back stack
                    else if (currentFragment instanceof MemoFragment) {
                        //Log.d(TAG, "current frag = memoFrag ➝ navigate back to the prev. fragment in the back stack");
                        getSupportFragmentManager().popBackStack(); //das leupht (und removet auch das fragment)
                    }
                    else {
                        //3 sonst aus der app raus
                        //Log.d(TAG, "back-button ➝ raus aus app");
                        finish(); //TODO 23.12.
                    }
                }
            };

            // Add the callback to the OnBackPressedDispatcher
            //OnBackPressedDispatcher oBPdispatcher = getOnBackPressedDispatcher();
            getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
            //wenn man nicht in homefrag ist, soll back-button in homeFrag leiten, sonst aus der App raus

        } //___________end of -user is signed in-! (basically all onCreate)___________________
        Log.d(TAG,"Ende onCreate (in: Main)");
    }//------ENDE__________onC.RE.A...TE_________________________::::::-:::::::___________________--_________


    private void buildprefCatList() {
        //TODO (Main) (sort) ist der sort of prefcatlist necessary?Y y.y :DxDDDDDDDDDDDDDDKANI LMAORKEK NVEMBORE23
        //8.0.1 einholen amountofprefcat + prefCatList (u.a. für foryou):
        catPref = getSharedPreferences("preferredCategoriesOf" + userid, Context.MODE_PRIVATE);
        Map<String, ?> allPrefCat = catPref.getAll();
        //NOPE UNSINN ÜBER BORD GEWORFEN DIESE THESEN IDEE _ _ liste aus den cat pref einholen und dem category-string anfügen (letzterer ist null, se utente non ha scelto pref cat)
        //(info) hier ist das Vorgehen, die pcPref durchzugehen und für jede cat
        for (Map.Entry<String, ?> entry : allPrefCat.entrySet()) {
            //only retrieve entries with a boolean value of true
            if (entry.getValue() instanceof Boolean && (Boolean) entry.getValue()) {
                //die prefCatList ist basically dasselbe wie catPref.getAll(), nur als List (nicht Map) .. damit lässt sich einfacher arbeiten
                if (!prefCatList.contains(entry.getKey())) prefCatList.add(entry.getKey()); //add
            }
        }
        //herausfinden von amountofprefcat
        amountofprefcat = prefCatList.size();
        Log.d(TAG, "prefCatList("+ amountofprefcat +"):" + prefCatList);

    }


    @Override
    protected void onStart() {
        Log.d(TAG,"onStart Main");
        super.onStart();
        Intent intent = getIntent();
        if (intent.getAction() != null && intent.getAction().equals("SHOW_UPLOAD_SUCCESS")) {
            Log.d(TAG,"Aufzeichnung erfolgreich hochgeladen (in onStart, Main)");
            Toast.makeText(this, R.string.fileuploadedsuccessfully, Toast.LENGTH_SHORT).show(); //TODO(A) dieser Toast kommt auch, wenn man schon mal was hochgeladen hat und dann in Main über den fab option1 wählt und dann ne datei auswählt und anklickt
        } else if (intent.getAction() != null && intent.getAction().equals("SHOW_UPLOAD_ERROR")) {
            Log.d(TAG,"Fehler beim Hochladen (in onStart, Main)");
            Toast.makeText(this, R.string.errorfileupload, Toast.LENGTH_LONG).show();
        }
        //eventbus
        EventBus.getDefault().register(this);
        Log.d(TAG,"Ende onStart (in: Main)");
    }

    @Override
    protected void onResume() {//Called after onRestoreInstanceState, onRestart, or onPause. This is usually a hint for your activity to start interacting with the user
        Log.d(TAG,"onResume Main");
        super.onResume();
        //Log.d(TAG,"onResume END (in: Main)");
        // Reattach the listener if it's null
        /*if (listenerCommunicationMainToSearchfrag == null) {
            listenerCommunicationMainToSearchfrag = (MainListener) this;
        }*/ //stattdessen Eventbus
    }

    @Override
    protected void onPause() {
        //Called when the user no longer actively interacts with the activity, but it is still visible on screen. The counterpart to...
        //... onResume. This callback is mostly used for saving any persistent state the activity is editing, [...]
        Log.d(TAG,"onPause Main");
        //bevor die activity pausiert wird, wird die zuletzt aktive Sprache als currentLanguage festgehalten
        String languageCode = Locale.getDefault().getLanguage(); //current language wird aus dem Locale ausgelesen und im Format "germanSP" oder "englishSP" rausgegeben
        switch (languageCode) {
            case "de" -> currentAppLanguage = "germanSP";
            case "en" -> currentAppLanguage = "englishSP";
            //TODO (future) add more cases if more languages are in the app
        }
        super.onPause();
        //isActivityActive = false;
        //erstmal keine ÄNDERUNGEN in onPause !!!!!!!!!!!!!!!!!!!!!!!!!
        //Log.d(TAG,"Ende onPause (in: Main)");
    }

    @Override
    protected void onStop() {
        //Called when you are no longer visible to the user. You will next receive either onRestart, onDestroy, or ...
        //... nothing, depending on later user activity.
        Log.d(TAG,"onStop Main");
        if(popupWindow != null) { //wenn das popupWindow initiiert wurde (und offen ist.. wenns bereits dismissed ist dann happens niente)
            popupWindow.dismiss();
        }
        super.onStop();
        //eventbus
        EventBus.getDefault().unregister(this);
        //Log.d(TAG,"Ende onStop (in: Main)");
    }


    @Override //onDestroy wird gecallt, wenn jemand irgendwo this.finish() gezogen hat yeye
    protected void onDestroy() {
        //Perform any final cleanup before an activity is destroyed. This can happen either because the activity is finishing
        // (someone called finish on it), or because the system is temporarily destroying this instance of the activity to save space. You can
        // distinguish between these two scenarios with the isFinishing method.
        Log.d(TAG,"onDestroy Main");
        super.onDestroy();
        //Log.d(TAG,"Ende onDestroy (in: Main)");
    }



//methods zum einlesen der firebase-sachen in den frags
    //1 search

    //2 home (recent zuerst, weil das immer geht + foryou hat user mglw. noch nicht eingestellt)
    private void retrieveHomeRecent(/*DataFetchCallback callback*/) {
        Log.d(TAG, "retrieveHomeRecent called");
        //zu Beginn immer die modelListrecent clearen (damit bei erneutem onCreate unaktuelle Einträge nicht mehr angezeigt werden)
        //modelListrecent.clear(); //TODO (A) (modellist) clearen?

        //TODO (A) daher wird nun einfach nur gequeriet nach date und dann die schon gehörten memos schööön aussortiert
        // - auftretende Zusatzschwierigkeit/-komplikation: wenn ich die query auf 48 hits limitiere, dann kann es sein, dass NACH DER Aussortierung danach nur noch ... übrigbleiben.
        // -> daher muss der RV & logic generell drauf gefasst sei , dass eine random anzahl an hits kommt & wenn es <pageSize sind (pageSize werden each displayed in rv), dann muss neu gequeriet werden ODER es werden einfach
        //    nur die ... (<pageSize) angezeigt und bei mehr-laden-button wird dann neu gequeriet!
        //  ...einziger sonnenschein: bei recent memos ist die W'keit eh gering, dass utente sie schon gehört hat lol
        //helper class for foryouGueries
        RecentQuery recentQuery = new RecentQuery();
        recentQuery.doRecentQuery(mongoAudiosCollection, false, this, mainViewModel);
    }

    public void retrieveHomeForyou() {
        Log.d(TAG, "retrieveHomeForyou called");
        //zu Beginn immer die modellistforyou clearen (damit bei erneutem onCreate unaktuelle Einträge nicht mehr angezeigt werden)
        //modelListforyou.clear(); //TODO (A) (modellist) clearen?
        //TODO (A) (modellist) nur clearen, wenn dataset nur UPDATED wird (und nicht REASSIGNED), weil wenn neu- dann würde previous stuff gelöscht werden. ..
        //die foryou page soll memos anzeigen, die zu den in profileFrag ausgewählten cat (bereits in onCreate in prefCatList übersetzt) passen
        CatQueries foryouQueries = new CatQueries();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            foryouQueries.doCatQueries(prefCatList, mongoAudiosCollection, false, this, mainViewModel, requireViewById(R.id.relativelayoutmainid), null, null,"foryou");
        else foryouQueries.doCatQueries(prefCatList, mongoAudiosCollection, false, this, mainViewModel, findViewById(R.id.relativelayoutmainid), null, null,"foryou");
    }






    //method für bsb :)
    public void createBsbAndPlayingservice(MemoFragment memofragment) {
        Log.d(TAG, "createBsbAndPlayingservice called");
        //View memoFragView = findViewById(R.id.memofragid);
        //View memoFragView = memoFragment.requireView();

        //1 bsb
        //fab 56dp nach oben schieben -- 128 = 72 (base bottom margin) + 56 (bsb)
        //moveFab(128); //createBsbAndPlayingservice
        //TODO (fab) (memoFrag) (main) fab wieder runner wenn memoFrag geschl wird (und nicht gehidet)

        //initialize bsb
        FragmentContainerView memofragview = findViewById(R.id.memofragid);
        bottomSheetBehavior = BottomSheetBehavior.from(memofragview); //TODO (memofragview) (bsb) wird immer aufs neue gecallt...

        //memoFrag ui for callback
        LinearLayout llcollapsed = memofragview.findViewById(R.id.llcollapsedid);
        ImageButton playbuttoncollapsed = memofragview.findViewById(R.id.playbuttoncollapsedid);
        // bsb callback
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_COLLAPSED -> {
                        Log.d(TAG, "STATE_COLLAPSED");
                        //bottomsheetstatetv.setText("Collapsed");
                        playbuttoncollapsed.setEnabled(true);
                        //fadeinFab();
                        //llcollapsed onClick
                        llcollapsed.setOnClickListener(view ->
                                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED));
                    }
                    case BottomSheetBehavior.STATE_EXPANDED -> {
                        Log.d(TAG, "STATE_EXPANDED");
                        //bottomsheetstatetv.setText("Expanded");
                        playbuttoncollapsed.setEnabled(false);
                    }
                    case BottomSheetBehavior.STATE_DRAGGING -> {
                        Log.d(TAG, "STATE_DRAGGING");
                        //bottomsheetstatetv.setText("Dragging...");
                        playbuttoncollapsed.setEnabled(false);
                        //fadeawayFab(); //dragging
                    }
                    case BottomSheetBehavior.STATE_SETTLING -> {
                        Log.d(TAG, "STATE_SETTLING");
                        //bottomsheetstatetv.setText("Settling...");
                        playbuttoncollapsed.setEnabled(false);
                        //fadeawayFab(); //settling
                    }
                    //case BottomSheetBehavior.STATE_HALF_EXPANDED -> bottomsheetstatetv.setText("Half Expanded"); //never happens?
                    case BottomSheetBehavior.STATE_HIDDEN -> {
                        //bottomsheetstatetv.setText("Hidden");
                        //fab 56dp wieder nach unten schieben
                        //moveFab(72); //createBsbAndPlayingservice
                    }
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) { //0:collapsed, 1:expanded, -1:hidden
                //Log.d(TAG, "sliding ...  offset:"+slideOffset);
                updateGrayscaleFilter(slideOffset, llcollapsed);
                //if (slideOffset > 0) fab.setVisibility(View.GONE);
            }
        });

        //2 playingService Intent & binding in gang sätsen
        memofragment.initializePlayingService();

        //3 update bolean
        bsbandplayingservicesetup = true;
    }

    /*private void moveFab(int bottommargin) {
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) fab.getLayoutParams();
        int newMarginBottom = switch (bottommargin) {
            case 72 -> getResources().getDimensionPixelSize(R.dimen.dp_72);
            case 128 -> getResources().getDimensionPixelSize(R.dimen.dp_128);
            default -> throw new IllegalStateException("Unexpected value: " + bottommargin);
        };
        layoutParams.setMargins(layoutParams.leftMargin, layoutParams.topMargin, layoutParams.rightMargin, newMarginBottom);
        fab.setLayoutParams(layoutParams);
    }
    private void fadeinFab() {
        if (fab.getVisibility() != View.VISIBLE) {
            fab.animate().alpha(1.0f).setDuration(200).withStartAction(() -> fab.setVisibility(View.VISIBLE));
        }
    }*/ //TODO 1.4. fab ersetzt mit toolbar
    private void updateGrayscaleFilter(Float offset, LinearLayout collapsedview) {
        //float brightness = -1*(offset-1); //wenn offset:0 ➝ brightness:1 , offset:0.2 ➝ brightness:0.8 , ...
        float brightness = -(offset-1) * (offset-1) * (offset-1); //wenn offset:0 ➝ brightness:1 , usw. kubische Funktion XXDDDDDD
        //Log.d(TAG, "updateGrayscaleFilter| offset: " + offset + ", brightness: " + brightness);
        collapsedview.setAlpha(brightness);
    }




//interfaces______________________________________________________________________________________________________________________________________interfaces
    @Override
    public void onRetrieveSignalToReassignPrefCatListFromHomeforyou() {
        Log.d(TAG, "onRetrieveSignalToReassignPrefCatListFromHomeforyou");
        //prefCatList neu zuweisen
        Map<String, ?> newPrefCat = catPref.getAll();
        prefCatList.clear();
        for (Map.Entry<String, ?> entry : newPrefCat.entrySet()) {
            //String prefcat = (String) entry.getValue();
            //prefCatList.add(prefcat);
            //only retrieve entries with a boolean value of true
            if (entry.getValue() instanceof Boolean && (Boolean) entry.getValue()) {
                //die prefCatList ist basically dasselbe wie catPref.getAll(), nur als List (nicht Map) .. damit lässt sich einfacher arbeiten
                if (!prefCatList.contains(entry.getKey())) prefCatList.add(entry.getKey()); //add
            }
        }

        //trigger retrieveHomeForyou() from the fragment
        retrieveHomeForyou(); //communication from homeFrag (in profileFrag wurden pref cat geändert)
    }//stattdessen Eventbus

    //aus CA wird die modellist an Main transferred, and concurrent steps are taken depending on whether memoFrag exists already o n
    @Override
    public void onRetrieveDataFromCustomAdapter(ArrayList<String> memolist) {
        Log.d(TAG, "onRetrieveDataFromCustomAdapter");

        MemoFragment memoFragment = (MemoFragment) getSupportFragmentManager().findFragmentById(R.id.memofragid);
        Log.d(TAG, "memoFragment != null");

        //display memo data (before initiating service, bc. service intent needs urL)
        assert memoFragment != null;
        memoFragment.displaymemoDataandPrepareUi(memolist);

        //bsb + playingService initializen, falls noch nicht geschehen
        if (!bsbandplayingservicesetup) createBsbAndPlayingservice(memoFragment);

        //memoFrag ist schon als <fragment> im xml-foreground, aber invis ➝ vis maken
        FragmentContainerView memofrag = findViewById(R.id.memofragid);
        memofrag.setVisibility(View.VISIBLE);

        //expand bottom sheet
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        //Log.d(TAG, "bottom sheet state: " + bottomSheetBehavior.getState() + "(2:settl, 3:exp, 4:collaps, 5:hid)");

        //hide fab
        //fab.setVisibility(View.GONE);
    }//stattdessen Eventbus

    /*@Override
    public void onRetrieveSignalToManipulateBottomSheetBehaviour(Boolean hide) {
        Log.d(TAG, "onRetrieveSignalToManipulateBottomSheetBehaviour");
        //bottomSheetBehavior.setDraggable(false); //..und dann in memoFrag buttons disablen / oder gleich auf Visibility.GONE setzen ...oder:
        if (hide) bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        else bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

    }*///stattdessen Eventbus


    //interface for communicating to Main (for notifying Main to re-do the method retrieveHomeForYou() when in ProfileFrag the pref cat have been changed and in HomeFrag the foryouRV needs
    // to be updated due to that [which happens over livedata VM])
    public interface MainListener {
        void onRetrieveSignaltoOpenSearchfield();
    }

}//ENDEEEEEEE_____________________E_E_E_E_E_________________________________EE__E_E_____________________________E_E_____E_E________________