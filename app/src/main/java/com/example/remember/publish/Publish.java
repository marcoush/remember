package com.example.remember.publish;

import static com.example.remember.Main.categoryNameAndIDList;
import static com.example.remember.Main.username;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;

import com.example.remember.LanguageUtils;
import com.example.remember.Main;
import com.example.remember.R;
import com.example.remember.categories.CategoryNameAndIndex;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.model.Updates;

import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.realm.mongodb.App;
import io.realm.mongodb.AppConfiguration;
import io.realm.mongodb.User;
import io.realm.mongodb.mongo.MongoClient;
import io.realm.mongodb.mongo.MongoCollection;
import io.realm.mongodb.mongo.MongoDatabase;



public class Publish extends AppCompatActivity implements RecordingService.RecordingStateChangeListener {
    private static final String TAG = "Publish";


    //idk wo das herkommt:
    //private static final int REQUEST_CODE_PERMISSIONS = 0x1

    ImageButton recordbutton, publishbutton, /*categorysearchbutton, */languagebutton;
    Button finishbutton, deletebutton;
    EditText fillintitle/*, categorysearchfield*/;
    LinearLayout createnamesandcategories, untereschaltflaeche/*, row1situations, row2situations, row1feelings, row2feelings*/;
    RelativeLayout categoriescontainer;
    //HorizontalScrollView situationsscrollview, feelingsscrollview;
    TextView durationtextview;
    ProgressBar progressbar, languageprocessbar;


    //Audio auf handy + online speichern ... YT tut 1 https://www.youtube.com/watch?v=_wQrlpPC1f8&list=PLs1w5cUPuHqPbA0TG79yCjwFr1eh2rvfZ&index=8&ab_channel=AndroidX
    private String audioDuration;

    //firebase initalizations
    FirebaseUser fUser;
    //String userid;
    FirebaseAuth fAuth;
    FirebaseFirestore fStore;
    FirebaseStorage fStorage;
    //CollectionReference audiosCollection, usersCollection, categoriesCollection;
    //DocumentReference audioDocument, userDocument;
    //StorageReference storageRef; //das hier ist nur für Dateien, die auf dem Handy liegen ... kann ich als Alternative anbieten zum In-App-Recording
    //StorageTask uploadTask;

    //mongo initliaziations
    MongoDatabase mongoDatabase;
    MongoClient mongoClient;
    MongoCollection<Document> mongoUsersCollection;
    MongoCollection<Document> mongoAudiosCollection;
    //MongoCollection<Audio> mongoAudiosCollection; //TODO debug
    //MongoCollection<Category> mongoCategoriesCollection;
    //MongoNamespace mongoNamespace; //was ist das
    User user;
    //UserIdentity userIdentity; //You can link multiple user identities to a single user account , erstmal not needed
    App app;
    String userid, usermail;



    //DatabaseReference referenceAudios;
    //MediaMetadataRetriever metadataRetriever;
    private boolean audiorecordedandfinished;//funktioniert das wirklich??!? testen! scheint ja
    private boolean audiochosenfromphonestorage; //wichtig für publishAudio() und onBackPressed()

    //boolean, um zu prüfen, ob audio erfolgreich (& kein error) uploaded wurde für abfrage zum Saven des audios in onDestroy
    //private boolean hasThereBeenUploadError;
    private boolean audioHasBeenUploaded;



    //NEU
    //farben für buttons
    int grayedviolet,grayedred, grayedblueish,grayeddarkred,grayedgreen,grayedlightgreen,grayedcyan,grayedorange;
    int violet,red,blueish,darkred,green,lightgreen,cyan,orange;
    //cat buttons
    Button party,festival,friends,nature,erasmus,university,illegal,adventure,relationship,trip,dream,travel,school,family,work,happy,lucky,inlove,lonely,desperate,relieved,sad,depressive,angry,anxious,nostalgic,club;

    //  ArrayList<String> categoryButtonsArray = new ArrayList<>(); //each buttoùns goes inside this array when read in from firebase
    ArrayList<String> selectedCategoriesArray = new ArrayList<>(); //when click buttoùn, add button-text to this arary
    //private boolean shallCategorysearchbuttonOpenSearchfield = true;
    //5.1 das categoriesarray wird für sowohl 1 cat als auch 2,3,4 cat verwendet und ist ein behelfsarray, durch das die queries erleichtert / überhaupt möglich gemacht werden!! (19.10.23 :D finalmente l'ho trovata, la soluzione)
    ArrayList<String> categoriesarray = new ArrayList<>(); //das array, was in mongo geworfen wird danno

    //boolean for finding the language flag
    private boolean isEnglishFlag;
    //process bar, der anzeigt, dass die aktualisierung der buttons an die neue sprache noch ingange ist ➝ oben zsm. middem anderen ProcessBar

    //ArrayList, die alle von firestore eingelesenen Category enthält , und zwar die jew. Kombination aus name + id
    //List<CategoryNameAndIndex> categoryNameAndIDList = new ArrayList<>(); //stattdessen in Main public static einlesen :D
    private int recordint, pauseint, germanint, englishint, color_ui, color_one;



    //choose audiofile from phone chatGPT tutorial
    //private static final int PICKFILE_REQUEST_CODE = 63; //dieses tag wichtig mittlerweile in Main, da wird im fab die file gepickt und an hier übergeben + vorteil, dass wenn man backbutton drückt, ist man immer noch in MAIN <3
    private String vomUserEingetippterAudioName;
    private Uri selectedFileUri;

    //boolean, ob die audiodatei erfolgreich deleted wurde
    private boolean isFileSuccessfullyDeleted;

    //TODO (C) die aktuelle aufnahmensekundenzahl kann am ende aus dem edittext oder aus dem long ausgelesen werden xD für die metadatem

    //service 1
    //TODO : Ensure that you acquire the necessary permissions (e.g., RECORD_AUDIO) in your manifest file and handle runtime permissions ...
    // ... if targeting Android 6.0 (Marshmallow) or above.
    //private RecordingServiceConnection serviceConnection; //das wurde simultan gentuzt mit der private class RecordingServiceConnection implements ServiceConnection
    private RecordingService recordingService = null;
    private boolean shallStartPauseRecordingTriggerOnRecord = false; // Variable zur Überprüfung des Service-Zustands, ersetzt durch isBound (isServiceRunning war ursprünglich gedacht für startPauseRecording() - dabei ist die abfrage, ob die actvitiy mit dem service BOUND ist, genauso gewinnbringend einzusetzen. wenn der service nicht bound ist, dann war er es noch nie oder ist zwischendurch durch handybildschirm aus unbounded worden. wenn der service bound ist, dann wird onRecord angegriffen.
    private boolean isBound = false;
    //private boolean isActivityActive = false; //wird bei onStart auf true gesetzt , und bei onStop / onDestroy auf false ➝ dann wird der service zum Foreground service!


    //permissions für microphone und notification
    //public static final int NOTIFICATION_REQUEST_CODE = 61; //permission für insb. FOREGROUND NOTIFICATION bei aufnahme AUSGELAGERT IN PUBLISH
    public static final int MICROPHONE_REQUEST_CODE = 62; //permission für RECORD AUDIO mit microfon AUSGELAGERT IN PUBLISH
    private boolean deniedonce;
    private boolean deniedtwice;
    //private boolean notificationPermissionGranted = false;
    //private boolean microphonePermissionGranted = false;
    //int MIC_NOTIF_REQUEST_CODE = 42;
    //String[] micAndNotifPermissions = {Manifest.permission.ACCESS_NOTIFICATION_POLICY, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
    //permissions dialog für notific in-app (könnte ich auch für alle anderen notifications wie micro verwenden though, müsste ich dann umbauen, läuft aber auch so)
    ActivityResultLauncher<String> requestNotificPermissionLauncher;


    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"onCreate Publish");
        super.onCreate(savedInstanceState);
//0 Language
        //Get the languauage which is right now active and maybe change this activity's language to it (lang might have been changed in Settings that's why...)
        LanguageUtils languageUtils = new LanguageUtils();
        languageUtils.updateLanguage(this);
//M Content View
        setContentView(R.layout.activity_publish);
//1 UI
        // Set up the toolbar (kann man leider ned outsource , oder ich & chatGTP wissen nichtr wie herzliche grüße aus dem juni beginn)
        //toolbar stuff
        Toolbar toolbar = findViewById(R.id.toolbarid);
        TextView toolbarTitle = findViewById(R.id.toolbartitleid);
        ImageButton toolbarSettingsButton = findViewById(R.id.toolbarsettingsid);
        ImageButton toolbarUploadButton = findViewById(R.id.toolbaruploadbuttonid);
        ImageButton toolbarRecordyourselfButton = findViewById(R.id.toolbarrecordyourselfbuttonid);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        // Set activity title
        String activityTitle = getString(R.string.shareyourmemory);
        toolbarTitle.setText(activityTitle);
        // Handle ImageButton click
        toolbarSettingsButton.setOnClickListener(view -> {
            Intent intent = new Intent(this, com.example.remember.Settings.class);
            startActivity(intent);
        });
        //toolbar items upload+recordyourself shalln't show in publish
        toolbarUploadButton.setVisibility(View.GONE);
        toolbarRecordyourselfButton.setVisibility(View.GONE);
        //das layout, was dann visible gesetzt wird, wenn name erschafefn wird + categories eingelesen werdne + pipapoooooooooo
        createnamesandcategories = findViewById(R.id.createnameandcategoriesid);
        categoriescontainer = findViewById(R.id.categoriescontainerid);
        //cat buttons
        party = findViewById(R.id.party);festival = findViewById(R.id.festival);friends = findViewById(R.id.friends);nature = findViewById(R.id.nature);erasmus = findViewById(R.id.abroad);university = findViewById(R.id.university);illegal = findViewById(R.id.illegal);
        adventure = findViewById(R.id.adventure);relationship = findViewById(R.id.relationship);trip = findViewById(R.id.trip);dream = findViewById(R.id.dream);travel = findViewById(R.id.travel);school = findViewById(R.id.school);family = findViewById(R.id.family);
        work = findViewById(R.id.work);happy = findViewById(R.id.happy);lucky = findViewById(R.id.luck);inlove = findViewById(R.id.inlove);lonely = findViewById(R.id.lonely);desperate = findViewById(R.id.desperate);relieved = findViewById(R.id.relieved);
        sad = findViewById(R.id.sad);depressive = findViewById(R.id.depressive);angry = findViewById(R.id.angry);anxious = findViewById(R.id.anxious);nostalgic = findViewById(R.id.nostalgic);club = findViewById(R.id.club); //26.
        //TODO (new categories) onCreate [Publish]
        //situationsscrollview = findViewById(R.id.situationsscrollviewid);
        //feelingsscrollview = findViewById(R.id.feelingsscrollviewid);
        fillintitle = findViewById(R.id.fillintitleid);
        //categorysearchfield = findViewById(R.id.categorysearchfieldid);
        //categorysearchbutton = findViewById(R.id.categorysearchbuttonid);
        //row1situations = findViewById(R.id.row1situationsid); //TODO (info) alter schinken von create cat dynamiclly from firebase...
        //row2situations = findViewById(R.id.row2situationsid);
        //row1feelings = findViewById(R.id.row1feelingsid);
        //row2feelings = findViewById(R.id.row2feelingsid);
        languagebutton = findViewById(R.id.applanguagebuttonid);
        languageprocessbar = findViewById(R.id.languageprocessbarid);
        progressbar = findViewById(R.id.progressbarid);
        publishbutton = findViewById(R.id.publishbuttonid);
        //untere schaltfläche mit recordbutton, deletebutton, finishbutton, durationtextview
        untereschaltflaeche = findViewById(R.id.untereschaltflaecheid);
        durationtextview = findViewById(R.id.durationid);
        recordbutton = findViewById(R.id.recordbuttonid);
        //setBackgroundofimagebutton(recordbutton, recordint);
        finishbutton = findViewById(R.id.audiofinishid);
        deletebutton = findViewById(R.id.audiodeleteid);

//2.1 firebase stuff references initiaten
        fStore = FirebaseFirestore.getInstance();
        fStorage = FirebaseStorage.getInstance();
        fAuth = FirebaseAuth.getInstance(); //still need to be logged in to upload file to fb storage
        fUser = fAuth.getCurrentUser();
        /*faudiosCollection = fStore.collection("audios");
        usersCollection = fStore.collection("users");
        categoriesCollection = fStore.collection("categories");
        userid = fUser.getUid();*/

//2.2 mongo
        app = new App(new AppConfiguration.Builder("remember-dxcgp").build());
        user = app.currentUser();
        userid = user != null ? user.getId() : null; //sieht funny aus, deswegen hab ich's etabliert , wurde vorgshlagen in der mwmw heit franen :)
        usermail = user != null ? user.getProfile().getEmail() : null; //sieht funny aus, deswegen hab ich's etabliert , wurde vorgshlagen in der mwmw heit franen :)
        Log.d(TAG,"user: " + user +
                "\nuserid: " + userid +
                "\nusermail: " + usermail);
        mongoClient = user.getMongoClient("mongodb-atlas");
        mongoDatabase = mongoClient.getDatabase("remember");
        //chat 26.10.23 IT WORKS!!!
        CodecRegistry defaultJavaCodecRegistry = MongoClientSettings.getDefaultCodecRegistry(); // is used in the MongoDB Java driver for general-purpose MongoDB database interactions in Java applications
        //CodecRegistry defaultRealmCodecRegistry = AppConfiguration.DEFAULT_BSON_CODEC_REGISTRY; //  is used in MongoDB Realm, typically for mobile and real-time applications.

        // Create a custom codec registry for your BSON types
        CodecRegistry customCodecRegistry = fromProviders(
                PojoCodecProvider.builder().automatic(true).build()
                // Add other codec providers if needed
        );
        CodecRegistry combinedCodecRegistry = CodecRegistries.fromRegistries(defaultJavaCodecRegistry, customCodecRegistry);
        //CodecRegistry pojoCodecRegistry = fromRegistries(defaultRealmCodecRegistry, fromProviders(PojoCodecProvider.builder().automatic(true).build())); //POJO: plain old java objects...

        mongoUsersCollection = mongoDatabase.getCollection("users").withCodecRegistry(combinedCodecRegistry);
        mongoAudiosCollection = mongoDatabase.getCollection("audios" /*, Audio.class*/ )/* .withCodecRegistry(pojoCodecRegistry)*/ ; //(info) mit POJO macht er IMMER ObjectId für _id...!!
        //mongoCategoriesCollection = mongoDatabase.getCollection("categories", Category.class).withCodecRegistry(pojoCodecRegistry);


//3 resource ints
        //color_ui = ContextCompat.getColor(this, R.color.color_ui);
        //color_one = ContextCompat.getColor(this, R.color.color_one);
        recordint = android.R.drawable.ic_media_play;
        pauseint = android.R.drawable.ic_media_pause;
        germanint = R.drawable.germanflag;
        englishint = R.drawable.englishflag;

//4 INTENT
        //falls publish geöffnet wurde, indem auf "upload file from smartphone" in der main geklickt wurde -WAR VORHER IN onStart, ist aber nicht mehr nötig dort zu sein, da diese Activity eh IMmEr gefinisht() wird neuerdings, wenn sie verlassen wird
        Intent intent = getIntent();
        if (intent.getAction() != null && intent.getAction().equals("OPEN_SELECTED_FILE")) {
            selectedFileUri = intent.getData(); //die gepickte file wird über das intent von main übergeben
            //ALT die mittlere schaltfläche visibilisiert + der hsv da drin created
            //createNamesAndCategories();
            //NEU die coole cat ansicht ist da! ➝
            cat_labombailritmofatal();
            
            Log.d(TAG,"selectedFileUri ist: " + selectedFileUri + " (wurde von Main ➝ Publish übergeben) (in: onStart, Publish)");
            // Use the selectedFileUri to access the chosen file and extract important data
            String selectedFileName = getFileNameFromURI(selectedFileUri);
            Log.d(TAG,"selectedFileName ist: " + selectedFileName + "(in: onStart, Publish)");
         /*   int dotIndex = selectedFileName.lastIndexOf("."); //vorerst erstmal deaktiviert that the name of the audio from storage is converted into the edittext automatically..
            if (dotIndex > 0) {
                String fileNameWithoutExtension = selectedFileName.substring(0, dotIndex);
                fillintitle.setText(fileNameWithoutExtension);
            } else {
                // Handle the case where there is no file extension
                fillintitle.setText(selectedFileName);
            }*/
            audiochosenfromphonestorage = true; //audio wurde aus filepicker geholt, ist für publishAudio() & onBackpressed() relevant
            untereschaltflaeche.setVisibility(View.GONE); //hide schaltfläche unten kosmetika , GONE geht weil keine anderen layouts damit verknüpft sind (~;
        }

//5 BUTTONS
        //audio recorder buttons
        recordbutton.setOnClickListener(v -> initiateRecordingOrOnRecord()); //vorher hatte ich hier die Methode recordAudio() bentuzt, aber das ergab probleme mit dem Speicherort , und nun nutze ich MediaStore und habe die offizielle code basierend auf android documentation und dem YT video: https://www.youtube.com/watch?v=-_pCG0w2UYg&ab_channel=GeKaTeam
        finishbutton.setOnClickListener(v -> finishAudio()); //vorher hatte ich hier die Methode doneAudio() bentuzt
        deletebutton.setOnClickListener(v -> deleteAudio());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { //TODO (C) idk about this, ist wegen try/catch v. MediaMetadataRetriever
            publishbutton.setOnClickListener(this::publishAudio);
        }

        // hide virtual keyboard when pressing ENTER in editte
        fillintitle.setOnKeyListener((v, keyCode, event) -> { //neuer code drunter
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                InputMethodManager imm = (InputMethodManager) getBaseContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                return true;
            }
            return false;
        });


        //button anmalen wenn activity startet + wenn lagnuage wechselt
        String language = Locale.getDefault().getLanguage();// Retrieve the app language when activity is started
        if (language.equals("de")) {
            setBackgroundofimagebutton(languagebutton, germanint);
            isEnglishFlag = false; //if (language.equals("de"))
        } else if (language.equals("en")) {
            setBackgroundofimagebutton(languagebutton, englishint);
            isEnglishFlag = true; //if (language.equals("en"))
        } else { //irgendeine ANDERE sprache, die auf dem handy läuft (wie bei mir random italienisch) ➝ TODO (languages) english=default lang
            setBackgroundofimagebutton(languagebutton, englishint);
            isEnglishFlag = false; //irgendeine ANDERE sprache
        }
        languagebutton.setOnClickListener(v -> {
            //TODO (B) coole animation für die flaggE?
            if (isEnglishFlag)  { //languagebutton onclick 1
                Log.d(TAG,"languagebutton.setOnClickListener: isEnglishFlag ist true");
                setBackgroundofimagebutton(languagebutton, germanint);
                makeButtonsGerman();
                isEnglishFlag = false; //languagebutton onclick 2
                Log.d(TAG,"languagebutton.setOnClickListener: isEnglishFlag auf false gesetzt");
                //updateLanguageOfAllButtons(this::proceedWithUpdateLanguageOfAllButtonsAndSetEnglishFlagToFalse);
            } else {
                Log.d(TAG,"languagebutton.setOnClickListener: isEnglishFlag is false");
                setBackgroundofimagebutton(languagebutton, englishint);
                makeButtonsEnglish();
                isEnglishFlag = true; //languagebutton onclick 3
                Log.d(TAG,"languagebutton.setOnClickListener: isEnglishFlag auf true gesetzt");
                //updateLanguageOfAllButtons(this::proceedWithUpdateLanguageOfAllButtonsAndSetEnglishFlagToTrue);
            }
            //TODO (future) (foo[d]tour 2) für mehr sprachen erweitern hir HAHah dann muss ich mich noch einen boolean überlegen XddDDDSS
        });


//6 onBckpressd
        //TODO (info) (oBP) man kann mit onBackPressedCallback.setEnabled(false); das callback an- und ausschalten! nur so... dachte könnde indressand werde am 11.1.23456780
        // Create an OnBackPressedCallback //TODO 10.1. (C) (oBP) neu gemacht mit finish) statt super.onBackpresed() jaja
        OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Log.d(TAG, "handleOnBackPressed");
                if(audiorecordedandfinished == true) { //if an audio has been recorded and finished by utente
                    //pending status of audio set to 0, so audio file can be accessed in phone storage - here: either save/keep file (positivebutton) OR delete file (negativebutton)
                    //proceed logic damit die method nach dem this:: DANACH passiert also erst, wenn die vorige wirklich durchgelaufen ist!!!
                    recordingService.setPendingStatusToZeroAndMakeAudiofileVisibleInSmartphoneStorage(Publish.this::proceedWithOnBackPressedWhenPendingStatusIsReleased);
                } else if (recordingService.isRecording != false || recordingService.isPaused != false) { //if audio recorder is active...
                    exitWarning();
                } else {
                    //if audio has been chosen with the filepicker from the smartphone storage [if (audiochosenfromphonestorage == true)] ➝ nothing happens va bene
                    //or if Publish has been opened by option2 (record yourself), but recordbutton has never been pressed ➝ user can navigate back to Main for free
                    finish(); //finish: current activity is terminated, and the system navigates back to the previous
                }
                Log.d(TAG,"Ende onBackPressed (in: Publish)");

            }
        };

        // Add the callback to the OnBackPressedDispatcher
        //OnBackPressedDispatcher oBPdispatcher = getOnBackPressedDispatcher();
        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);



//9 AT THE END OF ONCREATE, SO THE ONSERVICECONNECTED HAS ALREADY GONE THROUGH
        //in onCreate: CREATE and BIND the service
        Intent serviceIntent = new Intent(this, RecordingService.class);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE); //bind the service and create it with flag BIND_AUTO_CREATE
        Log.d(TAG,"onCreate: recordingService ist " + recordingService);
        Log.d(TAG,"onCreate: Service Intent abgefeuert und Binding in Gang gesetzt (in: Publish)");

        Log.d(TAG,"Ende onCreate (in: Publish)");
    }//onCreate FINEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE


//TODO (C) so ganz sicher mit den onResume, onPause, etc. bin ich mir noch ned, was lifecycles im Generellen + boolean: isActivityActive angeht

    @Override
    protected void onStart() {
        Log.d(TAG,"onStart called");
        super.onStart();
        //if not yet: setup callback mechanism
        if (recordingService != null) {
            Log.d(TAG,"onStart: recordingService != null ➝ set callback");
            recordingService.setRecordingStateChangeListener(Publish.this); //onStart (Publish)
        }
        //if not yet bound: Bind to the service when the activity is started and keep (NOT) FOREGROUND state untouched:
        if (!isBound) { //should always apply, kind of unnecessary
            Log.d(TAG,"onStart: service is not bound and is = " + recordingService);
            Intent serviceIntent = new Intent(this, RecordingService.class);
            bindService(serviceIntent, serviceConnection, BIND_NOT_FOREGROUND); //bind the service and create it with flag BIND_AUTO_CREATE
            isBound = true; //.. blalballbla already existing foreground service
            Log.d(TAG,"onStart: binded service (service is = " + recordingService + ")");
        }
        Log.d(TAG,"onStart END");
    }

    @Override
    protected void onResume() {//Called after onRestoreInstanceState, onRestart, or onPause. This is usually a hint for your activity to start interacting with the user
        Log.d(TAG,"onResume called");
        super.onResume();
        //if not yet: setup callback mechanism
        if (recordingService != null) {
            Log.d(TAG,"onResume: recordingService != null ➝ set callback");
            recordingService.setRecordingStateChangeListener(Publish.this); //onResume
        }
        //if not yet bound: Bind to the service when the activity is resumed and keep (NOT) FOREGROUND state untouched:
        if (!isBound) { //should never apply (because onPause doesn't unbind and onStop leads to first onStart and onStart already binds...) but for safety keep this code here. you never know
            Log.d(TAG,"onResume: service is not bound und ist " + recordingService);
            Intent serviceIntent = new Intent(this, RecordingService.class);
            bindService(serviceIntent, serviceConnection, BIND_NOT_FOREGROUND); //bind the already existing foreground service without changing FOREGROUND/NOT FOREGROUND state
            isBound = true;
            Log.d(TAG,"onResume: binded service (service is = " + recordingService + ")");
        }
        Log.d(TAG,"onResume END");
    }

    @Override
    protected void onPause() {
        //Called when the user no longer actively interacts with the activity, but it is still visible on screen. The counterpart to...
        //... onResume. This callback is mostly used for saving any persistent state the activity is editing, [...]
        Log.d(TAG,"onPause called");
        //due to the fact that paused activities can still be visible (just not in foreground/focus), keep the service bind and state listener active!
        //isActivityActive = false;
        Log.d(TAG,"onPause END");
        super.onPause();
    }

    //TODO (done I think) onFragmentVerlassen oder so , soll dann Mediaplayer releaset werden? ne, oder? damit man nicht aus versehen alles löscht
    @Override
    protected void onStop() {
        //Called when you are no longer visible to the user. You will next receive either onRestart, onDestroy, or ...
        //... nothing, depending on later user activity.
        Log.d(TAG,"onStop called");
        //Unbind from the service when the activity is stopped because it is then no longer visible al utente
   //deaktiviere das hier mal, weil onStop auch ausgelöst hat wenn man tabbt
        Log.d(TAG,"onStop END");
        super.onStop();
    }

    @Override //onDestroy wird gecallt, wenn jemand irgendwo this.finish) gezogen hat oder onBackPressed durchjegangen ist yeye
    protected void onDestroy() {
        //Perform any final cleanup before an activity is destroyed. This can happen either because the activity is finishing
        // (someone called finish on it), or because the system is temporarily destroying this instance of the activity to save space. You can
        // distinguish between these two scenarios with the isFinishing method.
        Log.d(TAG,"onDestroy called");
        //Unbind from the service when the activity is destroyed:
        if (recordingService != null) {
            Log.d(TAG,"onDestroy: recordingService != null ➝ stop callback & foreground AND save audiofile:");
            //close callback mechanism
            recordingService.removeRecordingStateChangeListener();//generally good practice, but mostly depends on what happens inside the method though ➝ still ... keep
            //close foreground
            recordingService.stopForeground(true); //if the service wasn't running as a foreground service before calling this, this will have no effect
            if (!audioHasBeenUploaded) {
                //for safety save audiofile on phone
                saveLastModifiedAudiofileOnPhoneStorage();//TODO(A) PRüfen ob läuft
            }
        }
        if (isBound) {
            Log.d(TAG,"onDestroy: Service isBound ➝ unbind service:");
            unbindService(serviceConnection);
            //boolean is renewed anyway when Publish starts again.. but keeping track for other activities might be useful ➝ keep
            isBound = false;
        }
        //isActivityActive = false;//boolean is renewed anyway when Publish starts again but keeping track for other activities might be useful ➝ keep
        audiochosenfromphonestorage = false;
        audiorecordedandfinished = false;
        //onDestroy: recordingService wird geschlossen
        Log.d(TAG,"onDestroy: annihilate service by kicking recordingService.onDestroy()");
        if (recordingService != null) recordingService.onDestroy();
        Log.d(TAG,"onDestroy END");
        super.onDestroy();
    }

    /*@Override
    public void onBackPressed() {
        Log.d(TAG,"onBackPressed ausgelöst (in: Publish)");
        // Handle the back button press or other navigation gestures here
        if(audiorecordedandfinished == true) { //if an audio has been recorded and finished by utente
            //pending status of audio set to 0, so audio file can be accessed in phone storage - here: either save/keep file (positivebutton) OR delete file (negativebutton)
            //proceed logic damit die method nach dem this:: DANACH passiert also erst, wenn die vorige wirklich durchgelaufen ist!!!
            recordingService.setPendingStatusToZeroAndMakeAudiofileVisibleInSmartphoneStorage(this::proceedWithOnBackPressedWhenPendingStatusIsReleased);
        } else if (recordingService.isRecording != false || recordingService.isPaused != false) { //if audio recorder is active...
            exitWarning();
        } else {
            //if audio has been chosen with the filepicker from the smartphone storage [if (audiochosenfromphonestorage == true)] ➝ nothing happens va bene
            //or if Publish has been opened by option2 (record yourself), but recordbutton has never been pressed ➝ user can navigate back to Main for free
            super.onBackPressed();
        }
        Log.d(TAG,"Ende onBackPressed (in: Publish)");
    }*/ //war deprecated //TODO (CCC) prüfen, ob auf onBackPressed noch onDestroy folgt - tut es
    private void proceedWithOnBackPressedWhenPendingStatusIsReleased() {
        Log.d(TAG,"last modified audio file path ist:" + getLastModifiedAudioFilePath(this) + " (in: proceedWithOnBackPressedWhenPendingStatusIsReleased, Publish)");
        //does user want to save the recorded audio file at least as a file on the phone?
        AlertDialog.Builder builder = new AlertDialog.Builder(Publish.this);
        builder.setTitle(R.string.saveaudioonphone);
        builder.setMessage(R.string.saveaudioonphonetext);
        builder.setPositiveButton(R.string.discard, (dialog, which) -> {
            deleteLastModifiedAudiofile(); //exitWarning user entscheidet sich für delete
            finish();
        });
        builder.setNegativeButton(R.string.save, (dialog, which) -> {
            //recorded audio file is deleted from phone storage , service closed, activity left
            saveLastModifiedAudiofileOnPhoneStorage(); //exitWarning user entscheidet sich für save
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // AHH, recordingService ist nuR DIE VERBINDUNG zum RecordingService, aber nicth der RecordingService SELBST!!!



    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG,"onServiceConnected: recordingService ist " + recordingService + ", bevor der innere Part von oSC abgelaufen ist");
            Log.d(TAG, "onServiceConnected: RecordingService connected ➝ now set up binder & callback");
            RecordingService.LocalBinder binder = (RecordingService.LocalBinder) service;
            recordingService = binder.getService();
            //callback interface to notify the UI in the Publish-activity when recording states have changed:
            recordingService.setRecordingStateChangeListener(Publish.this);
            //shallStartPauseRecordingTriggerOnRecord = true; //weggehauen weil unnötig
            isBound = true;
            Log.d(TAG,"onServiceConnected: recordingService ist (befürchteterweise immer noch / mittlerweile endlich) " + recordingService);
            Log.d(TAG,"onServiceConnected durchgelaufen (in Publish), also recordingService gebindet, setRecordingStateChangeListener an");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            recordingService.removeRecordingStateChangeListener(); //dies könnte unerwartetes verhalten mit dem interface vermeiden... ist aber ws unnötig
            recordingService = null; //indicates that the connection is no longer active (recordingService != RecordingService xd)
            //shallStartPauseRecordingTriggerOnRecord = false; //wenn service disconnected, soll - wenn der service dann iwann reconnected - immer noch onRecord ausgelöst werden
            isBound = false;
            Log.d(TAG,"onServiceDisconnected durchgelaufen (in Publish), also recordingService = null");
        }
    };









    //TODO (C) zugriff hierauf momentan noch illegalerweise über "onClick" in xmL
    public void initiateRecordingOrOnRecord() { //diese method ist der Initiator für ALLES
        Log.d(TAG,"initiateRecordingOrOnRecord called");
        //TODO (C) prüfen: wenn ein user aufnimmt und dann die mikro permission währenddessen in den systemeinstellungen DENIED, dann wird die aufnahme gelöscht, oder?
        //TODO (B) cocky von mir: wenn ein user aufnimmt und dann die notification permission währenddessen in den systemeinstellungen DENIED, dann ist mir das ERST EINMAL egal - dann wird dem user halt kein foreground service mehr angezeigt
        //überprüfung, ob benachrichtigungen aktiv TODO (B) nochmal app neu installieren, um zu schauen, obs läuft
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        boolean notificationsEnabled = notificationManager.areNotificationsEnabled();
        Log.d(TAG,"notificationsEnabled ist: " + notificationsEnabled);
        if (!shallStartPauseRecordingTriggerOnRecord) { //wenn recordingService noch nicht existiert, dann wird hier jetzt schöööön der service gebindet & gestartet
            Log.d(TAG,"if-clause leitend zu startRecordingAndCreateAudioFile löst aus (in startPauseRecording, Publish)");
            //dieser teil des if-clauses löst (aufgrund des booleans) nur das allererste mal aus, wenn man den button klickt!
            //NEU: recordingService.startRecordingAndCreateAudioFile einlesen und nicht service starten - service wird MIT ACTIVITY gestartet & gestoppt/destroyed!

            //permission für microphone prüfen + ggf. requesten
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG,"mic permission nicht gegeben");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, MICROPHONE_REQUEST_CODE);
                Log.d(TAG,"mic permission sollte nun requested worden sein");
                //It seems like you are experiencing an issue where the permission request dialog doesn't appear after denying the permission multiple times...
                //...This behavior is intentional and designed by the Android system to prevent annoying the user with repetitive permission requests...
                //...This behavior is to respect the user's decision and prevent app developers from continuously asking for permissions.
                //To resolve this, you can direct the user to the app settings page where they can manually enable the required permission:
                if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                    // User has previously denied the permission but hasn't selected "Don't ask again"
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, MICROPHONE_REQUEST_CODE);
                } else {
                    // User has denied the permission multiple times and selected "Don't ask again"
                    if (deniedtwice == true) {
                        Log.d(TAG,"user hat perm. requ. 2x denied, daher showt der neue Dialog up");
                        showPermissionInstructionsDialog();
                    }
                } //und diese undurchsichtige if-clause mallorie qui läuft wirklich sahnig fein
            }

            //damit die foreground notification passieren kann, müssen Benachrichtigungen erteilt sein
            if(notificationsEnabled == false) {
                //request notif permissions launcher
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Log.d(TAG,"SDK >= 33 - show permi dialog in-app");
                    //wenn vers >= 33 , dann in-app notif permiss launcher anzeige
                    requestNotificPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                } else {
                    Log.d(TAG,"SDK < 33 (altbacken) - redirect to system settings for app notific");
                    //da current min vers = 26: show notif settings in diesem Fall
                    openNotifPermiGrantDialog();
                }


                Log.d(TAG,"notificationsEnabled ist false, daher öffnet sich der dialog zum ändern der systemsettings");
            }

            //wenn mic permission gegeben ist ... if-Abfrage muss ich wegen der funktionalität von asychronous requestPermissions leider leider machen :((((
            //und wenn notifications erlaubt sind (für foreground notific)
            if (notificationsEnabled && ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG,"mic permission gegeben und notifications enabled");
                //start recording & create pending audio file
                recordingService.startRecordingAndCreateAudioFile(this);
                shallStartPauseRecordingTriggerOnRecord = true; //TODO(A) prüfen, ob der zurückgesetzt wird auf "false", wenn activity destroyed und wiederaufgerufen wird
                Log.d(TAG,"startRecordingOrOnRecord: recordingService will be set to foreground, right now is " + recordingService);
                //start foreground service (and keep it on as long as recorder is active)
                //recordingService.startForeground(1, recordingService.getForegroundNotification()); //OLD 23.12.
                recordingService.initiateNotification();
                durationtextview.setVisibility(View.VISIBLE);//set durationtextview visible
                Log.d(TAG,"startRecordingOrOnRecord: recording begins and recordingService ist " + recordingService);
                Log.d(TAG,"service foregrounded & started (in startPauseRecording, Publish)");
            }
        } else {
            Log.d(TAG,"if-clause leitend zu onRecord löst aus (in startPauseRecording, Publish)");
            //wenn recordingService bereits gestartet (aka, recording in service class gerade anläuft oder pausiert ist), dann NUR onRecord
            recordingService.onRecord(); //zu Beginn wird an onRecord der boolean "false" übergeben, dann switcht es durch den Asudruck ... = !... unten immer durch
        }
        Log.d(TAG,"Ende startPauseRecording (in: Publish)");
    }

    private void openNotifPermiGrantDialog() {
        // Create an AlertDialog to request notification permission
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Set the dialog title and message
        builder.setTitle(getString(R.string.appnotifications)).setMessage(getString(R.string.enablenotificationsindevicesettings));

        // Set a positive button and its click listener
        builder.setPositiveButton(R.string.opensettings, (dialog, id) -> {
            // Open the app notification settings when the "Open Settings" button is clicked
            openSmartphoneNotificationSettings();
        });

        // Set a negative button to cancel the dialog
        builder.setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss());

        // Create and show the alert dialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /*//method for the dialogue (VERS >= 33) that asks for notif permissions in-app
    private void onPermissionResult(boolean permissionGranted) {
        if (permissionGranted) {
            //user hat eingewilligt
            Log.d(TAG, "user hat notif permi eingewilligt");
            //Snackbar.make(findViewById(android.R.id.content).getRootView(), "Now you can start the recording", Snackbar.LENGTH_LONG).show();
            //showDummyNotification(); //aus medium tutorial auf https://yggr.medium.com/exploring-android-13-notification-runtime-permission-6e198bb5ae3b
        } else {
            //user hat abgelehnt, notif anzuzeigen...
            Snackbar.make(findViewById(android.R.id.content).getRootView(), "Please grant Notification permission from App Settings", Snackbar.LENGTH_LONG).show();
        }
    }*/ //method for the dialogue (VERS >= 33) that asks for notif permissions in-app











    //PUBLISH, FINISH & DELETE AUDIO
    //TODO (A) ich will eine schöööne animation in der Main haben als Bedankung fürs Uplaoden und duolingo style fancy stuff für das Auge
    //TODO (A) wenn eine audiodatei vom namen her SCHON existiert, muss der user darüber benachrichtigt werden! sonst wird sie überschrieben XD abe es ausprobiert oder wird sie das ? im not sure
    // aktueller Stand war eigentlich, dass es unendliche viele dateien mit demselben namen geben kann, nur deren ID ist dann eben anders  :D -ü-

    //TODO (AAA) nach der aktuellen Logik werden die dynamically eingelesenen category buttons in der jew. Sprache hochgeladen- das erfordert allerdings, dass die Sprache des jew. Buttons geupdatet wurde ...
    // ... und der User nicht kurz vorm Uploadbutton die Sprache (z.B. auf Engl.) geändert hat und dann wird durch die isEnglishFlag=true "name" in categories gewählt und da könnte dann noch der deutsche CategoryManager name drinstehen.. weil der nicht rechtzeitig zum englischen updatet wurde der Button :(
    //1 PUBLISH
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void publishAudio(View view) {
        Log.d(TAG, "publishAudio called");
        progressbar.setVisibility(View.VISIBLE);
        //zum beginn den publishbutton disablen, weil asynchron und damit user nicht panisch nochmal drückt und 2x oder 3x hochlädt
        publishbutton.setEnabled(false);
        if(fillintitle.getText().toString().isEmpty()) { //wenn fillintitle leer ist, muss der user erstmal was eingeben dort
            Toast.makeText(this, R.string.pleasefillintitle, Toast.LENGTH_SHORT).show();
        } else {
            //keyboard wird geschlossen, wenn es noch nicht vom user geschlossen wurde, als der publishButton gedrückt wurde
            InputMethodManager imm = (InputMethodManager) getBaseContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

            if (audiorecordedandfinished == true) { //d.h. es gibt eine gefinishte, vom mediarecorder aufgenommene audiodatei
                Log.d(TAG, "audiowurdeaufgenommen = true, d.h. es gibt eine gefinishte, vom mediarecorder aufgenommene audiodatei (in: publishAudio)");
                //pending status aufheben, damit das system die letzte aufgenommene audio file überhaupt trovaten kann
                recordingService.setPendingStatusToZeroAndMakeAudiofileVisibleInSmartphoneStorage(this::proceedWithPublishRecordedAudio); //(publishAudio)
            }
            else if (audiochosenfromphonestorage == true) { //d.h. es wurde keine audiodatei mit mediarecorder aufgenommen, sondern aus dem handyspeicher geholt
                Log.d(TAG, "audiowurdeaufgenommen = false, daher über filepicker eingeholte audio hochladen");
                //selectedFileUri ist die vom user im ACTION_GET_CONTENT-intent ausgew. audiodatei, die wird jetzt eingelesen und hochgeladen
                Log.d(TAG, "selectedFileUri ist: " + selectedFileUri + " (in: publishAudio)");
                /*if (selectedFileUri != null) { //es gibt WIRKLICH eine datei :) */ //TODO(C) this always applies because if audiochosenfromphonestorage == true, there has to be a selectedFireUri
                //vom heiligen user angepasster audiotitel wird eingelesen, damit man ihn an die method unten weitergeben kann
                vomUserEingetippterAudioName = fillintitle.getText().toString();
                //audio dauer wird aus metadaten der auf dem smartphone gespeicherten audiodatei eingelesen
                try (MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever()) {//das hier requiret API scheiß mit dem try/catch
                    mediaMetadataRetriever.setDataSource(this, selectedFileUri);
                    String unformattedAudioDuration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    Log.d(TAG,"how does unformatted audio duration look?:" + unformattedAudioDuration);
                    assert unformattedAudioDuration != null; //if extractMetadata failure
                    long durationInMillis = Long.parseLong(unformattedAudioDuration);
                    long hours = TimeUnit.MILLISECONDS.toHours(durationInMillis);
                    long minutes = TimeUnit.MILLISECONDS.toMinutes(durationInMillis) % 60;
                    long seconds = TimeUnit.MILLISECONDS.toSeconds(durationInMillis) % 60;
                    //!!!!!!!!DONT INLINE VARIABLE!!!!!!!!!!!!!!!! welche variable marc das hättest du mal dazuschreiben können XD:D
                    String formattedAudioDuration;
                    if (durationInMillis < 3600000) formattedAudioDuration = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds); //<1h
                    else formattedAudioDuration = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds); //>1h
                    Log.d(TAG,"how does formatted audio duration look?:" + formattedAudioDuration);
                    audioDuration = formattedAudioDuration; //Das hier muss sein, sonst wird audioDuration von den nächsten methods nicht eingelesne + ist "null"
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                uploadFileAndSetMetadata(vomUserEingetippterAudioName, selectedFileUri); //from publishAudio (audio chosen from phone storage)
            }
            else { //es gibt leider KEINE datei :(
                Toast.makeText(Publish.this, R.string.nofileforuploadselected, Toast.LENGTH_SHORT).show();
            }
        }

        Log.d(TAG, "publishAudio END");
    }
    private void proceedWithPublishRecordedAudio() {
        // Suche den filepath der zuletzt geänderten Audiodatei im MediaStore [über die methode getLastModifiedAudioFilePath()]
        File lastModifiedAudioFile = new File(getLastModifiedAudioFilePath(this));
        //... und nimm daraus den Namen + die Uri
        String lastModifiedAudioName = lastModifiedAudioFile.getName();
        Uri lastModifiedAudioUri = Uri.fromFile(lastModifiedAudioFile);
        Log.d(TAG,"lastModifiedAudioName ist: " + lastModifiedAudioName + " und " +
                "\nlastModifiedAudioUri ist: " + lastModifiedAudioUri + " (in: publishAudio, Publish)");
        //das letzte aufgenommene audio wurde rausgesucht & jetzt muss noch der name geändert werden
        vomUserEingetippterAudioName = fillintitle.getText().toString();
        //der Rest geschieht schööööön in der method:
        uploadFileAndSetMetadata(vomUserEingetippterAudioName, lastModifiedAudioUri); //from proceedWithPublishRecordedAudio (audio recorded) (info: this is called before the uri "lastModifiedAudioUri" is printed)
    }


     //2.3 Hilfsfunktion zum Extrahieren des Dateipfads aus der URI
    private String getFileNameFromURI(Uri uri) {
        String[] projection = {MediaStore.Audio.AudioColumns.DISPLAY_NAME};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        assert cursor != null;
        int columnIndex = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DISPLAY_NAME);
        cursor.moveToFirst();
        String fileName = cursor.getString(columnIndex);
        cursor.close();
        Log.d(TAG,"fileName aus getFilePathFromURI ist: " + fileName);
        return fileName;
    }



    //3 FINISH
    // TODO 1/2 wenn man auf chooseAudioFromphoneButton klickte und sich das Mittelfenster mit dem PublishButton öffnet, und man dann aber ein Audio aufnimmt und es auch "DONET" (also pausiert), dann muss das vorherige vom Smartphonespeicher ausgewählte Audio durch die MeidaRecorder-Datei ersetzt werden
    //TODO 2/2 wenn man auf chooseAudioFromphoneButton klickte und sich das Mittelfenster mit dem PublishButton öffnet, und man dann aber ein Audio aufnimmt (+ nicht DONET) und auf Publish klickt, dann WAS? wird die vorher ausgerwählte Audiodatei hochgeladen (einfachste Lsg) oder wird dem User angezeigt, dass er sein Audio vorher stoppen soll (bessere Lsg)


    //TODO (A) nachdem man audio aufnahm & nicht hochlädt, den user darüber informieren, dass es als "...irgneentitel" abgespeichert weirde ➝ neue idee: fragen bevor Publish verlassen wird, ob gespeichert werden osll
    //TODO(A) wenn man aufnimmt und dann irgendwie Publish destroyet, user informieren, dass app als "..wfjdiksdf" gespeichert wurde auf handyspeicher
    private void finishAudio() { //finishAudio, um den recorder zu beenden (recorder.stop(); does exactly that)
        Log.d(TAG,"finishAudio wurde gedrückt (in: Publish)");
        if (recordingService.isRecording == true || recordingService.isPaused == true) {
            recordingService.pauseRecording();
            //aufnahme gestoppt, um sicherzugehen, dass der User wirklich das audio beenden will
            AlertDialog.Builder builder = new AlertDialog.Builder(Publish.this);
            builder.setTitle(R.string.finishrecording);
            builder.setMessage(R.string.finishrecordingtext);
            builder.setPositiveButton(R.string.yes, (dialog, which) -> {
                recordingService.stopRecording(); //give command to service to stop recording
                Toast.makeText(this, R.string.recordingfinished, Toast.LENGTH_SHORT).show();
                //durationtextview zeit wird ausgelesen und DANACH logischerweise erst auf 0 und INVIS gesetst
                audioDuration = durationtextview.getText().toString();
                //TODO(A) ask user if he wants to keep the audio file on smartphone as well when he publishes the audio
                //TODO(A) read in fileuri here
                durationtextview.setVisibility(View.INVISIBLE);
                durationtextview.setText(R.string.durationnull); //reset durationtext to 00:00 again
                //recorder ist nicht mehr an, daher foreground vom service ausschatlen
                recordingService.stopForeground(true);
                //ALT:
                //createNamesAndCategories();
                //NEU die coole cat ansicht ist da! ➝
                cat_labombailritmofatal();
                //recordbutton und fertigbutton disablen und gräuen
                recordbutton.setEnabled(false);
                recordbutton.setVisibility(View.INVISIBLE);
                finishbutton.setEnabled(false);
                finishbutton.setVisibility(View.INVISIBLE);

                Log.d(TAG,"recording finished (file path is null because audio still pending)");
                //damit publishbutton weiß, dass das zuletzt geänderte audio gezogen werden muss & damit deletebutton weiß, dass er aktiviert bleibt
                audiorecordedandfinished = true;
            });
            builder.setNegativeButton(R.string.notyet, (dialog, which) -> {
                // keep the pausing state active / do nothing because pausing is already active
                Log.d(TAG,"Finish Vorgang abgebrochen (in: Publish)");
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            CharSequence italicfinishaudiohinweis = HtmlCompat.fromHtml("Konfuzius sagt: <i>Ein nicht begonnenes Audio kann nicht beendet werden</i>", HtmlCompat.FROM_HTML_MODE_LEGACY);
            Toast.makeText(this, italicfinishaudiohinweis, Toast.LENGTH_SHORT).show();
        }
        Log.d(TAG,"Ende finishAudio (in: Publish)");
    }

    //4 DELETE
    private void deleteAudio() {
        Log.d(TAG,"Anfang deleteAudio (in: Publish)");
        if (recordingService.isRecording == true || recordingService.isPaused == true) { //wenn recorder läuft oder pausiert ist
            Log.d(TAG,"recorder ist aktiv, also erstmal pausieren und lösch-dialog abfragen (in: deleteAudio)");
            recordingService.pauseRecording();
            //sichergehen, dass wirklich deleted werden soll
            AlertDialog.Builder builder = new AlertDialog.Builder(Publish.this);
            builder.setTitle(R.string.deleterecording);
            builder.setMessage(R.string.deleterecordingtext);
            builder.setPositiveButton(R.string.yes, (dialog, which) -> {
                recordingService.stopRecording();
                //recorder ist nicht mehr an, daher foreground vom service ausschatlen
                recordingService.stopForeground(true);
                //delete file

                deleteLastModifiedAudiofile(); //deleteAudio()        delete file when recorder was active and paused

                //adjust UI elements
                durationtextview.setText(R.string.durationnull); //reset durationtext to 00:00 again
                //recordbutton und fertigbutton reaktivieren
                recordbutton.setEnabled(true);
                recordbutton.setVisibility(View.VISIBLE);
                finishbutton.setEnabled(true);
                finishbutton.setVisibility(View.VISIBLE);
                //wenn ab jetzt wieder recordbutton geklickt wird, soll wieder neue aufnahme starten & nicht resumeRecording ausgelöst werden
                shallStartPauseRecordingTriggerOnRecord = false;
                //notify utente of whether file successfully deleted
                if (isFileSuccessfullyDeleted) {
                    Toast.makeText(this, R.string.audiodeleted, Toast.LENGTH_SHORT).show();
                    Log.d(TAG,"Aufnahme gelöscht (in deleteAudio, Publish)");
                    isFileSuccessfullyDeleted = false; //and reset the boolean for new application cases
                } else {
                    // Fehler beim Löschen der Datei
                    Toast.makeText(this, R.string.audiodeletederror, Toast.LENGTH_SHORT).show();
                }
            });
            builder.setNegativeButton(R.string.no, (dialog, which) -> {
                // keep the pausing state active / do nothing because pausing is already active
                Log.d(TAG,"Delete Vorgang abgebrochen");
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        } else if (audiorecordedandfinished == true) { //wenn recorder gefinisht wurde und man im title+category fenster ist, will man immer noch löschen können
            Log.d(TAG,"audio was recorded and finished , jetzt lösch-dialog abfragen (in deleteAudio, Publish)");
            //sichergehen, dass wirklich deleted werden soll
            AlertDialog.Builder builder = new AlertDialog.Builder(Publish.this);
            builder.setTitle(R.string.deleterecording);
            builder.setMessage(R.string.deleterecordingtext);
            builder.setPositiveButton(R.string.yes, (dialog, which) -> {
                //delete file
                deleteLastModifiedAudiofile(); //delete file in deleteAudio when audio was recorded and finished already
                //wenn ab jetzt wieder recordbutton geklickt wird, soll wieder neue aufnahme starten & nicht resumeRecording ausgelöst werden
                shallStartPauseRecordingTriggerOnRecord = false;
                //category array zurücksetzen
                selectedCategoriesArray.clear();
                //audiorecordedandfinished zurücksetzen zu werkseinstellungen
                audiorecordedandfinished = false;
                //adjust UI elements
                //shallCategorysearchbuttonOpenSearchfield = true; //reset hiervor für die functionality of the searchbutton basically
                //categorysearchfield.setText("");//möglicherweise eingegebener suchtekts reset to ""
                fillintitle.setText("");//möglicherweise eingegebener title reset to ""
                createnamesandcategories.setVisibility(View.GONE);//schaltfläche i.d. midde wieder disablen
                durationtextview.setText(R.string.durationnull);//reset durationtext to 00:00 again (while the textview is invisible, just for the text reset whenever its visibilized again)
                recordbutton.setEnabled(true);//recordbutton und fertigbutton reaktivieren
                recordbutton.setVisibility(View.VISIBLE);
                finishbutton.setEnabled(true);
                finishbutton.setVisibility(View.VISIBLE);
                //notify utente of whether file successfully deleted
                if (isFileSuccessfullyDeleted) {
                    Toast.makeText(this, R.string.audiodeleted, Toast.LENGTH_SHORT).show();
                    Log.d(TAG,"Aufnahme gelöscht (in deleteAudio, Publish)");
                    isFileSuccessfullyDeleted = false; //and reset the boolean for new application cases
                } else {
                    // Fehler beim Löschen der Datei
                    Toast.makeText(this, R.string.audiodeletederror, Toast.LENGTH_SHORT).show();
                }
            });
            builder.setNegativeButton(R.string.no, (dialog, which) -> {
                // keep the pausing state active / do nothing because pausing is already active
                Log.d(TAG,"Delete Vorgang abgebrochen");
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            CharSequence italicdeleteaudiohinweis = HtmlCompat.fromHtml("Konfuzius sagt: <i>Ein nicht begonnenes Audio kann nicht gelöscht werden</i>", HtmlCompat.FROM_HTML_MODE_LEGACY);
            Toast.makeText(this, italicdeleteaudiohinweis, Toast.LENGTH_SHORT).show();
        }
        Log.d(TAG,"Ende deleteAudio (in: Publish)");
    }


    private void deleteLastModifiedAudiofile() { //wird NUR gecallt, wenn audio aufgenommen wurde
        Log.d(TAG,"deleteLastModifiedAudiofile called. last modified audio file path ist: " + getLastModifiedAudioFilePath(this) + " (weil pending status)");
        //delete recording from phone storage (kann auch in der activity & nicht im service gemacht werden, ist ja nicht unmittelbar abhängig vom mediarecorder)
        //pending status aufheben, damit das system die letzte aufgenommene audio file überhaupt trovaten kann and wait for the callback
        recordingService.setPendingStatusToZeroAndMakeAudiofileVisibleInSmartphoneStorage(this::proceedWithDeleteLastModifiedAudiofile); //(deleteLastModifiedAudiofile)
    }
    private void proceedWithDeleteLastModifiedAudiofile() {
        Log.d(TAG,"pending status wurde releaset: last modified audio file path ist: " + getLastModifiedAudioFilePath(this));
        if(getLastModifiedAudioFilePath(this) != null) {
            //finde die zuletzt geänderte Audiodatei im MediaStore [über die methode getLastModifiedAudioFilePath()]
            File lastModifiedAudioFile = new File(getLastModifiedAudioFilePath(this));
            String lastModifiedAudioName = lastModifiedAudioFile.getName();
            Uri lastModifiedAudioUri = Uri.fromFile(lastModifiedAudioFile);
            Log.d(TAG,"lastModifiedAudioName ist: " + lastModifiedAudioName + "und \n lastModifiedAudioUri ist: " + lastModifiedAudioUri + " (in: deleteLastModifiedAudiofile, Publish)");
            //lösche diese aufnahme vom handyspeicher
            isFileSuccessfullyDeleted = lastModifiedAudioFile.delete(); //löscht das audio + gleichzeitig erstellt nen boolean davon, der im Erfolgsfall true ist
        }
    }

    private void saveLastModifiedAudiofileOnPhoneStorage() { //kann auch gecallt werden, wenn kein audio aufgenommen wurde
        //wait until pending status is released first
        recordingService.setPendingStatusToZeroAndMakeAudiofileVisibleInSmartphoneStorage(this::proceedWithSaveLastModifiedAudiofileOnPhoneStorage); //(saveLastModifiedAudiofileOnPhoneStorage)
    }
    private void proceedWithSaveLastModifiedAudiofileOnPhoneStorage() {
        Log.d(TAG,"pending status wurde releaset: last modified audio file path ist: " + getLastModifiedAudioFilePath(this));
        //wenn audio aufgenommen wurde
        if(getLastModifiedAudioFilePath(this) != null) {
            //finde die zuletzt geänderte Audiodatei im MediaStore [über die methode getLastModifiedAudioFilePath()]
            File lastModifiedAudioFile = new File(getLastModifiedAudioFilePath(this));
            String lastModifiedAudioName = lastModifiedAudioFile.getName();
            Uri lastModifiedAudioUri = Uri.fromFile(lastModifiedAudioFile);
            Log.d(TAG,"lastModifiedAudioName ist: " + lastModifiedAudioName + "und \n lastModifiedAudioUri ist: " + lastModifiedAudioUri + " (in: saveLastModifiedAudiofileOnPhoneStorage, Publish)");
        }
        //wenn kein audio aufgenommen wurde (filePath = null)
        //... passiert halt nix (aber diese method muss gecallt werden, weil die pending status abfrage vorher passieren muss und dort entschieden wird, obs ein aufgen. audio gibt o ned!)
    }


    //-------------------------------------------------------------------------------------------ALLES MGL AN VOIDS HIER UND DA NEL MONDO





//4 FIREBASE UPLOAD (+ metadata) https://www.youtube.com/watch?v=kjpJU4Aj2Wg&list=PLs1w5cUPuHqPbA0TG79yCjwFr1eh2rvfZ&index=6&ab_channel=AndroidX
    //TODO (A) if langauge = german (oder jede andere Sprache außer engl.) ➝ then the audio has to be uploaded to name_de

    public void uploadFileAndSetMetadata(String fileName, Uri fileUri) {

        //TODO (INFO) wichtig!! ich uploade erst einmal immer noch zu Firebase Storage!!! nur die metadata + collections + userdata + alles ist in mongo
        // (kein Bock gehabt, mongo qwirl zu nutzen jetzt gerade um da audios zu storen auch noch direkt - das kan Zukunftsmarcz u einem speteren Zeitpunkt mache)

        Log.d(TAG,"uploadFileFromStorageToFirebase ausgelöst (in: Publish)");
        /*if(uploadTask != null && uploadTask.isInProgress()) {
            Toast.makeText(this, "Upload im Gange!", Toast.LENGTH_SHORT).show(); //TODO(C) brauch ich das hier? ist noch nie vorgekommen
        } else {*/

        //fStorage = FirebaseStorage.getInstance(); //TODO(info) es gibt momentan 2 ordner in Firestore, der alte hieß "audio"
        StorageReference storageRef = fStorage.getReference().child("audios").child(fileName); //das hier ist halt der path-string! .child("audios").child(fileName) geht genauso, das wäre dann entlang des "seitenbaums"
        StorageMetadata metadata = new StorageMetadata.Builder().setContentType("audios/mpeg").build(); //(info) MUSS SEIN sonst cazzo .3GPP VIDEO file Set the MIME type explicitly as "audio/mpeg" (sonst war es irgendwie "video/3gpp" ... [?])
        //metadata für contentType belassen, weil dateiendung sonst ja bekanntlichermaßen zerschossen wourde des Manchermalen
        UploadTask uploadTask = storageRef.putFile(fileUri, metadata); //(info) ich hab beim filepicker zwar festgelegt, dass man nur audio-files ausgewählen kanone, aber banone trotzdem kommen ohne metadata .gpp files..
        Log.d(TAG, "der code hat die uploadTask ausgeführt, jetzt entscheidet sich, ob erfolgreicH:");
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            // File UPLOADED successfully
            //(info) on success listener, weil download URL erst retrove werden kann, wenn upload erfolgreich war!
            Log.v(TAG, "file uploaded successfully to Firestore, now retrieve downloadurl:"); //this log message is far beyond... it takes ages until taskSnapshot triggers really xD #relieved
            //hasThereBeenUploadError = false;
            audioHasBeenUploaded = true;

            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {                //audio file wurde in Firebase Storage uploadet, now Retrieve the download URL of the uploaded audio file (in order to send it to Firestore metadata)
                //(info) on success listener, weil metadata nur dann hinzugefügt werden kann, wenn URL feststeht!
                Log.v(TAG, "downloadurl successfully retrieved, now create metadata in Firestore:"); //this log message is far beyond... it takes ages until taskSnapshot triggers really xD #relieved
                //get the URL from the audio in firebase STORAGE:
                String audioURL = uri.toString(); //(info) die URL enthält die audioID in sich drin!!! natürlich, macht Sinn und ist Neuerung seit dem 28.6. XDDDDDDDDDDDDDDDDD
                //Generate a unique ID for the audio file which will be used for both methods below (especailly used for the explicit identification of the audio in audios_categories)
                //aktuelles datum einlesen (für weitergabe an storeAudioMetadata & storeUserMetadata, so ist es auch sekundengenau das exakt gleiche Datum für beide Methoden ;))
                Date currentDate = new Date(); //Datum unformatiert in die firestore (für queries) & dann beim Einlesen in Textviews, etc. formatieren mit folg. code (siehe T0DO):
                Log.d(TAG,        "audioURL ist: " + audioURL + " (in: publishAudio, Publish)\n" +
                        "ausgelesene audioDuration ist: " + audioDuration + " (in: uploadFileFromStorageToFirebase)\n" +
                        "current Date ist: " + currentDate); //TODO debug
                //(info) date wird (query ist schuld) als normales date eingelesen ➝ der obige CODE fürs formateiren des datums findet dann wohl erst beim displaying of the date statt! :)

                storeAudioMetadata(audioURL, currentDate); //URL wichtig für audio listenen
                //storeUserMetadata(audioID, currentDate); //(info) !!!! wird nun gecallt im onComplete von storeAudioMetadata

                //createAudiosCategories(audioID); //das ist für die "audios_categories" collection //GANZ ALT, prbly 04/23, jetzt ist 10/23
                //TODO (A) behalte ich createAudiosCategories erstmal ?
                //TODO (A) "categories" field in "audios" ➝ nicht umwandeln bei dt. in "categories_de", oder? hmmm doch das wäre schnellere Query dann! kind of
                // nvm! die query kann mittels field "language" [bspw.] .isEqualTo("language", "de") einfach querien, anstelle nach "categories_de" zu suchen

            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error in retrieving download URL: " + e.getMessage());
                Toast.makeText(Publish.this, "Error in retrieving download URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                progressbar.setVisibility(View.GONE);
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "UplFileErr: " + e.getMessage());
            Toast.makeText(Publish.this, "UplFileErr: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            //hasThereBeenUploadError = true;
            audioHasBeenUploaded = false;
            progressbar.setVisibility(View.GONE);
        });
        /*}*/
        Log.d(TAG,"Ende uploadFileFromStorageToFirebase (in: Publish)");
    }














    private void storeAudioMetadata(String audiourl, Date currentdate) {//store metadata in Firestore 24.10.23:NEIN MONGO, so i can use Firestore queries (easiest) - read in the URL from firebase STORAGE

        //TODO (INFO) wichtig!! ich uploade erst einmal immer noch zu Firebase Storage!!! nur die metadata + collections + userdata + alles ist in mongo
        // (kein Bock gehabt, mongo qwirl zu nutzen jetzt gerade um da audios zu storen auch noch direkt - das kan Zukunftsmarcz u einem speteren Zeitpunkt mache)

        Log.d(TAG,"storeAudioMetadata ausgelöst (in: Publish)");
        //1 username einlesen
        //mongo ➝ publicstatic String username in Main
        //2 sprache einlesen
        String language;
        if (isEnglishFlag) language = "en"; //storeAudioMetadata
        else language = "de";
        //3 create doc in "audios" with audioid as its name

        //5 categories einlesen und basierend auf deren Menge (gar keine, genau eine oder > 1) entweder hinzufügen oder nicht
        //5.2 also wenn genau 1 category enthalten ist im selectedCategoriesArray / ausgewählt wurde vom user !
        //createcategoriesarrayOutOfselectedCategoriesArray(); //alte version, in der das categoriesarray zB so aussieht [house,party,luck,houseparty,houseluck,partyluck,housepartyluck] - jetzt (30.11.23) mit mongo muss das nicht mehr sein... daher wird einfach selectedCategoriesArray hochgeladen :)

        //else gibt es nicht mal- wenn user keine categories auswählt, geht das leben einfach weitr...
        //TODO (A) PRÜFEN PROBELEM!: wenn ein user für seine memory keine kategorien auswählt:
        // ➝ dann ist kein "category" field im document in "audios"
        // ➝ dann kann vom Model keine category-list eingelesen werden (prüfen vonnöten, dass hier keine nullpointer entstehen!)

        //4 Create a map to store the audio metadata bzw. set the audio (using Audio class)
        String audioid = new ObjectId().toString(); //TODO (_id) (Publish) wird dieser String immer einzigartig sein, weil er zuerst ObjectId war?
        Log.d(TAG, "eingelesener für upload genutzter publicstatic username aus Main ist: " + username +
                "\naudioid: "+audioid);
        //TODO (publish) (audioid) hmm.. man könnte audioid stattdessen auch aus title,creator,duration,date zsmsetzen - hätte Vorteil, dass man in profileFrag listenedaudiosRV nicht nochmal die audiosColl
        // querien müsste, weil bereits in der audioid - welche im userDoc im array datesoflistenings notiert ist - diese Daten vorhanden wären ... das könnte viel querying sparen.

        mongoAudiosCollection.insertOne(new Document("_id", audioid).
                append("title", vomUserEingetippterAudioName)                 .
                append("creator", username)                       .
                append("creatorid", userid)                       .
                append("duration", audioDuration)  .
                append("date", currentdate)                       .
                append("language", language)                          .
                append("listeners", 0)  .
                append("hearts", 0)               .
                append("categories", selectedCategoriesArray)  .
                append("url", audiourl)).getAsync(task -> {
                    if (task.isSuccess()) {
                        Log.v(TAG, "successfully inserted document with id: " + task.get().getInsertedId());
                        storeUserMetadata(audioid, currentdate); //from storeAudioMetadata
                    } else {
                        Log.e(TAG, "failed to insert the document with: " + task.getError().getErrorMessage());
                        progressbar.setVisibility(View.GONE);
                    }
                });

        Log.d(TAG,"Ende storeAudioMetadata (in: Publish)");
    }

    private void createcategoriesarrayOutOfselectedCategoriesArray() {
        if(selectedCategoriesArray.size() == 1) {
            String onlyonecategory = selectedCategoriesArray.get(0); //TODO (A) problem: wenn man schnell genug die sprache wechselt, kann bspw. "desperate" als kategoriename sein, obowhl "de" language
            categoriesarray.add(onlyonecategory);
            //TODO (cont.) ➝ lösung: in-app übersetzen, anstatt neu auszulesen - also categories nur als engl. version online speichern und in app übersetzen immer!!!!
            //audioDataMap.put("categories", onlyonecategory); //ich habe nun dem string-field "categories" abgeschworen & betreibe nur noch das array ;)
        }
        //5.3 wenn > 1 cat von user ausgewählt wurde
        else if(selectedCategoriesArray.size() > 1) {
            //man hat jetzt das selectedCategoriesArray<String> und die categoryNameAndIDList<CategoryNameAndIndex"> , und man muss für die selected category names im selectedCategoriesArray die associated IDs im anderen Array finden:
            // also wenn mind. 2 categories enthalten sind im selectedCategoriesArray / ausgewählt worden vom user !
            sortSelectedCategoriesArrayAndCreateEntriesForcategoriesarray();
            //3 hier in die metadaten die categories bagge!
        }
    }

    private void sortSelectedCategoriesArrayAndCreateEntriesForcategoriesarray() {
        //1 sortiere die cats in die reihenfolge basierend auf den respective IDs
        selectedCategoriesArray.sort((categoryName1, categoryName2) -> { //compares 2 strings
            //TODO (A) vlt dem user sagen, dass er mindestens 2 kategorien wählen soll - ne ACTUALLY ich will den Leuten freiheit lassen :) keine Restriktionen ! & mehr If-clauses für miCH ROFLCOPT2R
            // To sort selectedCategoriesArray with multiple elements, the sorting algorithm repeatedly calls the compare() method [through .sort] for different pairs of elements
            // until the entire array is sorted. The algorithm uses the comparison results to rearrange the elements based on their relative order.
            int categoryId1 = getCategoryID(categoryName1); //gets the IDs of the vom selectedCategoriesArray eingelesenen categorynames from the getter method in class CategoryNameAndIndex
            int categoryId2 = getCategoryID(categoryName2);
            Log.d(TAG, "category1 = " + categoryName1 + " (ID " + categoryId1 + ") ---- category2 = " + categoryName2 + " (ID " + categoryId2 + ")");
            return Integer.compare(categoryId1, categoryId2); //compares the two category IDs
            //It returns a negative value if categoryId1 is less than categoryId2, a positive value if categoryId1 is greater than categoryId2
            //This comparison determines the order of the strings during the sorting process
        });
        //il selectedCategoriesArray è adesso ufficialmente ordinato per ID ✓

        //2 Append each category to the StringBuilder separated by commas AND add to the categoriesarray
        StringBuilder composedCategoriesString = new StringBuilder();
        for (String categoryName : selectedCategoriesArray) {
            //2.1 add to composed-cat-string
            composedCategoriesString.append(categoryName);
            System.out.println("im for loop categoryName ist = " + categoryName);
            composedCategoriesString.append(","); //composed mit comma
            System.out.println( "im for loop geupdateter composedCategoriesString = " + composedCategoriesString);
        }
        Log.d(TAG, "composedCategoriesString = " + composedCategoriesString);
        //Remove the trailing comma and space if any
        composedCategoriesString.deleteCharAt(composedCategoriesString.length() - 1); // Remove the last comma
        String finalComposedCategoriesString = composedCategoriesString.toString();

        //(info) für die QUERIES: habe ich zusätzlich zum "categories"-field nun noch ein "categoriesarray" erstellt, in dem alle cat combis aren, also max 7 Arrayeinträge bei 3 gewöhlten cat ✓
        //im categoriesarray muss immer an 1. stelle die vollständige Hauptkombination der cat stehen! (rest der Reihenfolge egal, aber ich mach mal richtig, also dann 2er-Kombis und dann die einzelnen cat)
        //1/3 Build the categoriesarray: add main combi
        //add the finalComposedCategoriesString to the categoriesarray :)
        categoriesarray.add(finalComposedCategoriesString);
        //2/3 Build the categoriesarray: add 2er cat combi(s)
        //wenn utente 3 cat wählte (zB adventure,travel,school), dann noch die 2er-Kombis ins Array einfügen (zB adventure,travel | adventure,school | travel,school)
        if (selectedCategoriesArray.size() == 3) {
            String[] catcombinations = { selectedCategoriesArray.get(0) + "," + selectedCategoriesArray.get(1),
                    selectedCategoriesArray.get(0) + "," + selectedCategoriesArray.get(2),
                    selectedCategoriesArray.get(1) + "," + selectedCategoriesArray.get(2)};
            categoriesarray.addAll(Arrays.asList(catcombinations));
        }
        //3/3 Build the categoriesarray: add single cat
        categoriesarray.addAll(selectedCategoriesArray); //jetzt noch die einzelnen cat appenden
        Log.d(TAG, "finalComposedCategoriesString (main cat combi) = " + finalComposedCategoriesString +
                "\ncategoriesarray (all cat combis) = " + categoriesarray);
    }


    //hilfemethode, um in storeAudioMetadata die zum category name zugehörige category id zu finden (mittels der class CategoryNameAndIndex)
    private int getCategoryID(String categoryName) {
        Log.d(TAG,"getCategoryID called | categoryNameAndIDList is: " + categoryNameAndIDList);
        for (CategoryNameAndIndex categoryNameAndID : categoryNameAndIDList) { //Iterates over the categoryList
            if (categoryNameAndID.getName().equals(categoryName)) { //and checks if the category name matches the one read in from the selectedCategoriesArray<String>
                return categoryNameAndID.getIndex(); //If found, it returns the associated ID.
            }
        }
        return -1; // Return a default value if the category is not found TODO (A) was hat es hiermit auf ßich?
    }


    //TODO (info)
    // addOnCompleteListener: This listener is used when you want to handle the completion of a task, whether it was successful or not
    // addOnSuccessListener: This listener is used when you want to handle the success case of an asynchronous operation
    private void storeUserMetadata(String audioid, Date currentDate) {//store metadata in Firestore, so i can use Firestore queries (easiest) - read in the URL from firebase STORAGE
        Log.d(TAG, "storeUserMetadata ausgelöst (in: Publish)");
        //das hier ist da, um die ID und das Date des audios kombiniert abzuspeichern
        //1 reference to user doc
        //3 add composedaudioidanddate to field "datesofuploads" list or create list (if doesn't exist yet, Firestore will automatically create)
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);//backend //Thu Oct 26 17:33:50 GMT+02:00 2023
        String formattedDate = dateFormat.format(currentDate);

        String composedDateandUploadID = formattedDate + "_" + audioid; //composed mit "_", zuerst date, damit danach gefiltert werden kann auch falls mal muss
        Log.d(TAG,"currentDate: " + formattedDate +
                "\ncomposedDateandUploadID: " + composedDateandUploadID + " ");
        //2 update doc: increment uploads by 1 & add composedDateandUploadID string to end of datesofuploads-array
        //Bson filter = Filters.eq("_id", userid); //gibt error:
        //BSON_CODEC_NOT_FOUND(realm::app::CustomError:1100): Could not resolve codec for SimpleEncodingFilter
        //org.bson.codecs.configuration.CodecConfigurationException: Can't find a codec for CodecCacheKey{clazz=class com.mongodb.client.model.Filters$SimpleEncodingFilter, types=null}.
        //at org.bson.internal.ProvidersCodecRegistry.lambda$get$0$org-bson-internal-ProvidersCodecRegistry(ProvidersCodecRegistry.java:87)
        //stattdessen mit Document:
        Document queryFilter = new Document("_id", userid);
        Bson update = Updates.combine(
                Updates.inc("uploads", 1),
                Updates.addToSet("datesofuploads", composedDateandUploadID));
        Log.d(TAG, "update is: " + update);
        mongoUsersCollection.updateOne(queryFilter, update).getAsync(task -> {
            if (task.isSuccess()) {
                long count = task.get().getModifiedCount();
                if (count == 1) { //TODO (update) (Publish) das fragt nur ab, ob das inc funktioniert hat lol, nicht das addToSet
                    Log.v(TAG, "successfully updated userDoc with uploads + datesofuploads");
                    //nachdem sie hochgeladen wurde, kann die Datei gelöscht werden
                    deleteLastModifiedAudiofile(); //nachdem user daten geupdated wurden

                    //zum schluss kann der publishbutton wieder enabled + progressbar gone werden (ist aber eh irrelevant, weil die activity jetzt schließt)
                    publishbutton.setEnabled(true);
                    progressbar.setVisibility(View.GONE);

                    //activity Publish verlassen & zurück zu main
                    Intent intent = new Intent(Publish.this, Main.class);
                    intent.setAction("SHOW_UPLOAD_SUCCESS");
                    startActivity(intent);
                    Log.d(TAG, "storeUserMetadata END (in: Publish)");
                    finish(); //wenn man Publish verlässt, soll immer gefinisht werden, damit man nicht zurückswipen kann (auch wegen all der booleans etc).. ist auch im sinne des users

                } else {
                    Log.e(TAG, "did not update doc (but task=success)");
                }
            } else {
                Log.e(TAG, "failed to update doc with: ", task.getError());
                progressbar.setVisibility(View.GONE);
            }
        });


    }

    private void exitWarning() {
        Log.d(TAG,"exitWarning AUSGELÖST!");
        if (recordingService.isRecording != false || recordingService.isPaused != false) {
            recordingService.pauseRecording();
            //erstmal pausiert, um sicherzugehen, dass user wirklich exiten will & das bisher aufgenommene audio discarden
            AlertDialog.Builder builder = new AlertDialog.Builder(Publish.this);
            builder.setTitle(R.string.exitwarning); //TODO severe weather warning message
            builder.setMessage(R.string.exitwarningtext); //TODO (A) möchte ich wirklich, dass ein verlassen des fensters die aufnahme unterbricht?
            builder.setPositiveButton(R.string.yesitsfine, (dialog, which) -> {
                recordingService.stopRecording();
                Toast.makeText(this, R.string.recordingcancelled, Toast.LENGTH_SHORT).show();
                deleteLastModifiedAudiofile(); //exitWarning in dem Fall will man die Audiodatei vom handyspeicher mit Sicherheit gelöscht haben!

                //service wird geschlossen
                if (recordingService != null) recordingService.onDestroy();
                recordingService = null;
                //back key behaviour wird ausgelöst + activity geschlossen
                finish();
            });
            builder.setNegativeButton(R.string.cancel, (dialog, which) -> {
                // do nothing bc. recording is already paused
                Log.d(TAG,"Exit Vorgang abgebrochen (in: exitWarning)");
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            Log.d(TAG,"exitWarning löst ned aus, weil recordingService.isRecording und .isPaused beide = false sind");
            finish();
        }
    }

    private void showPermissionInstructionsDialog() { //das ist der dialog, der für die mic. perm. systemeinstellungen verweist und upshowt, wenn 2x permissions denied wurden
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.permissionrequired)
                .setMessage(R.string.permissionrequiredtext)
                .setPositiveButton(R.string.gotosettings, (dialog, which) -> {
                    // Open app settings
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //callback method that is called when the user responds to the permission request dialog
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MICROPHONE_REQUEST_CODE) { //result of this is for the microphone permission request
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {//microphone perm. granted by user:
                //microphonePermissionGranted = true;
                //after permission for microphone has been redeemed, request now permisison for notification (ALT):
                Log.d(TAG,"user granted microphone permission and recording can begin");
                //requestNotificationPermission(); //dann weiterleitung auf request notification permission (2. schritt)
            } else { //microphone perm. DENIED by user:
                Log.d(TAG,"user denied microphone permission");
                //do nothing I suppose and let the user TRY AGAIN!:D
                if (deniedonce == true) {
                    deniedtwice = true;
                }
                deniedonce = true; //diese logik existi- und funktioniert, weil nach 2x deny android purposely keinen normalen perm. req. mehr zeigt ➝ dann soll der verweis auf die systemeinstellungen aufploppen! :) und das tut er mit diesem if-clause gewusel dank allah + bombasticus
            }
        }
    }

    // Methode zum Anzeigen der Benachrichtigungseinstellungen TODO (C) muss noch gecheckt werden, ob läuft
    private void openSmartphoneNotificationSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    private String getLastModifiedAudioFilePath(Context context) { //TODO(A) funktioniert wahrscheinlich nicht, weil neuerdingser pending status die datei unfindbar macht für diese methode...
        String[] projection = {MediaStore.Audio.Media.DATA};
        String sortOrder = MediaStore.Audio.Media.DATE_MODIFIED + " DESC";
        Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, null, null, sortOrder);
        String filePath = null;
        if (cursor != null && cursor.moveToFirst()) {
            int dataIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            filePath = cursor.getString(dataIndex);
        }
        if (cursor != null) {
            cursor.close();
        }
        Log.d(TAG,"filePath ist: " + filePath + " (in: getLastModifiedAudioFilePath, Publish)");
        return filePath;
    }

    private void setBackgroundofimagebutton(ImageButton imagebutton, int imageresource) {
        imagebutton.setImageResource(imageresource);
        //if (color != 0) imagebutton.setColorFilter(color);
    }










    


    //TODO (B) eig. sollte es so sein, dass man maximal 2 kategorien pro zeile auswählen kann oder so . habe ich aber noch nicht implementiert kein bock
    //TODO( A) 1/2 the user shall be able to create 1 category by himself for the audio, e.g. "la bomba festival" ... and then the category is created and others can chooooose it for their audio without having to create if for themselves
    //TODO (A) 2/2 PS when user wants to create a category that is similar (e.g. "la bomba") ➝ then maybe INFORM him that already exists a similar one which he can take ... or sth. :|  I have to consider this ...

    //ctegory button anklicken und in array hinzufügen, aus dem dann später beim upload der composed category string erstellt wird
    View.OnClickListener buttonClickListener = v -> {
        Log.d(TAG, "cat button geklickt");
        Button clickedButton = (Button) v;
        //1 prüfen, welcher button (id, nicht text weil sonst präkitationen mit SP) geklickt wird + herausfinden ob schon in pc:
        //button text für categoryname nehmen + umwandeln in sprachenunabhängigen categoryname (button id kan man ned nehme, weil probleme mit neu-bildung von id bei neustart etc.)
        //für deutsche buttons muss der clickedbuttoncategory string umgewandelt werden in die pc-version (standarmäßig engl.) (und für esp, fra, it, ...):
        String clickedbuttoncategory = switch (clickedButton.getText().toString()) {
            case "Party" -> "party";
            case "Festival" -> "festival";
            case "Club" -> "club";
            case "Arbeit" -> "work";
            case "Schule" -> "school";
            case "Uni" -> "university";
            case "Erasmus" -> "erasmus";
            case "Abenteuer" -> "adventure";
            case "Illegal" -> "illegal";
            case "Natur" -> "nature";
            case "Reisen" -> "travel";
            case "Ausflug" -> "trip";
            case "Beziehung" -> "relationship";
            case "Familie" -> "family";
            case "Freunde" -> "friends";
            case "Traum" -> "dream";
            case "fröhlich" -> "happy";
            case "glücklich" -> "lucky";
            case "erleichtert" -> "relieved";
            case "verliebt" -> "inlove";
            case "in love" -> "inlove"; //omg .. gut dass aufgefallen
            case "traurig" -> "sad";
            case "depressiv" -> "depressive";
            case "einsam" -> "lonely";
            case "nostalgisch" -> "nostalgic";
            case "wütend" -> "angry";
            case "ängstlich" -> "anxious";
            case "verzweifelt" -> "desperate";
            default ->
                    clickedButton.getText().toString();
            //TODO (more languages)
            //TODO (more categories) buttonClickListener [Publish]
        };
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
            //TODO (new categories) buttonClickListener [Publish]
            Log.d(TAG,"updated color of " + clickedbuttoncategory + " is: " + clickedButton.getTag());
            Log.d(TAG,clickedbuttoncategory + " aus array removed");
        }
        //wenn array category onbuttonclick not yet containt ➝ adden , ab ins array rein damit (wenn < 3categories bereits ausgewählt sind)
        else {
            if (selectedCategoriesArray.size() < 3) { //wenn <4 cat bereits selected sind ➝ add , sonst nicht weil man nur max 4 cat wählen drf
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
                //TODO (new categories) buttonClickListener [Publish]
                Log.d(TAG,"updated color of " + clickedbuttoncategory + " is: " + clickedButton.getTag());
                Log.d(TAG,clickedbuttoncategory + " in array added an stelle " + selectedCategoriesArray.indexOf(clickedbuttoncategory));
            } 
            else { //also wenn 4 oder mehr cat bereits selected sind ➝ toast
                Toast.makeText(this, R.string.onlyfivememorycategoriesALLOWED, Toast.LENGTH_SHORT).show();
            }
        }


    };


    private void cat_labombailritmofatal() {
        //linear layout visibilisieren
        createnamesandcategories.setVisibility(View.VISIBLE);
        //1.1 button colors
        //3.1 grayed colors
        grayedviolet = ContextCompat.getColor(this, R.color.grayedviolet);grayedred = ContextCompat.getColor(this, R.color.grayedred);
        grayeddarkred = ContextCompat.getColor(this, R.color.grayeddarkred);grayedblueish = ContextCompat.getColor(this, R.color.grayedblueish);
        grayedorange = ContextCompat.getColor(this, R.color.grayedorange);grayedgreen = ContextCompat.getColor(this, R.color.grayedgreen);
        grayedlightgreen = ContextCompat.getColor(this, R.color.grayedlightgreen);grayedcyan = ContextCompat.getColor(this, R.color.grayedcyan);
        //3.1 highlighted colors
        violet = ContextCompat.getColor(this, R.color.violet);red = ContextCompat.getColor(this, R.color.red);
        darkred = ContextCompat.getColor(this, R.color.darkred); blueish = ContextCompat.getColor(this, R.color.blueish);
        orange = ContextCompat.getColor(this, R.color.orange);green = ContextCompat.getColor(this, R.color.green);
        lightgreen = ContextCompat.getColor(this, R.color.lightgreen);cyan = ContextCompat.getColor(this, R.color.cyan);
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
    }

    @SuppressLint("SetTextI18n") //weil ich will purposely spezifischen text setten, unabh. von der vom in der app ausgew. lang.
    public void makeButtonsEnglish() {
        Log.d(TAG,"makeButtonsEnglish started");
        party.setText("party");
        festival.setText("festival");
        club.setText("club");
        work.setText("work");
        school.setText("school");
        university.setText("university");
        erasmus.setText("erasmus");
        adventure.setText("adventure");
        illegal.setText("illegal");
        nature.setText("nature");
        travel.setText("travel");
        trip.setText("trip");
        relationship.setText("relationship");
        family.setText("family");
        friends.setText("friends");
        dream.setText("dream");
        happy.setText("happy");
        lucky.setText("lucky");
        relieved.setText("relieved");
        inlove.setText("in love");
        sad.setText("sad");
        depressive.setText("depressive");
        lonely.setText("lonely");
        nostalgic.setText("nostalgic");
        angry.setText("angry");
        anxious.setText("anxious");
        desperate.setText("desperate");
        //TODO (more categories) makeButtonsEnglish [Publish]
        Log.d(TAG,"makeButtonsEnglish closed");
    }

    @SuppressLint("SetTextI18n") //weil ich will purposely spezifischen text setten, unabh. von der vom in der app ausgew. lang.
    public void makeButtonsGerman() {
        Log.d(TAG,"makeButtonsGerman started");
        party.setText("Party");
        festival.setText("Festival");
        club.setText("Club");
        work.setText("Arbeit");
        school.setText("Schule");
        university.setText("Uni");
        erasmus.setText("Erasmus");
        adventure.setText("Abenteuer");
        illegal.setText("Illegal");
        nature.setText("Natur");
        travel.setText("Reisen");
        trip.setText("Ausflug");
        relationship.setText("Beziehung");
        family.setText("Familie");
        friends.setText("Freunde");
        dream.setText("Traum");
        happy.setText("fröhlich");
        lucky.setText("glücklich");
        relieved.setText("erleichtert");
        inlove.setText("verliebt");
        sad.setText("traurig");
        depressive.setText("depressiv");
        lonely.setText("einsam");
        nostalgic.setText("nostalgisch");
        angry.setText("wütend");
        anxious.setText("ängstlich");
        desperate.setText("verzweifelt");
        //TODO (more categories) makeButtonsGerman [Publish]
        Log.d(TAG,"makeButtonsGerman closed");
    }
































    //---------------------------------------------------------------------------------------------------CALLBACK INTERFACES
    //callback interface to notify the UI in the Publish-activity when recording states have changed:
    //isRecording muss übernommen werden, damit onRecord funktionieren kann
    //dasselbe mit der stoppuhr
    @Override
    public void onRecordingStarted() {
        Log.d(TAG,"onRecordingStarted callback has been called! Success! :) (in: Publish)");
        // Update the UI when recording starts (whether recording starts, is read out of the service-class over the interface)
        runOnUiThread(() -> {
            setBackgroundofimagebutton(recordbutton, pauseint);
        });
    }
    @Override
    public void onRecordingPaused() {
        // Update the UI when recording is paused
        runOnUiThread(() -> {
            setBackgroundofimagebutton(recordbutton, recordint);
        });
    }
    @Override
    public void onRecordingResumed() {
        // Update the UI when recording is resumed
        runOnUiThread(() -> {
            setBackgroundofimagebutton(recordbutton, pauseint);
        });
    }
    @Override
    public void onRecordingStopped() {
        // Update the UI when recording is stopped
        runOnUiThread(() -> {
            //TODO (C) vlt sollte der recordbutton auf pausebutton gesetzt werden? weil das audio ist vorbei ... oder .. naja habs jz mal gemacht:
            setBackgroundofimagebutton(recordbutton, recordint);
            durationtextview.setText(R.string.durationnull); //reset the durationtext to 00:00 again
        });
    }

    @Override
    public void onCountDownTick(String remainingTimeFormatted) {
        // Update the UI when the timer does 1 tick
        runOnUiThread(() -> {
            Log.d(TAG,"onCountDownTick geöffnet (callback-mechanismus in Publish)");
            // Update the TextView with the recorded time
            durationtextview.setText(remainingTimeFormatted);
           // remainingtimeformatted = remainingTimeFormatted;
            /*// Update the TextView with the recorded time
            long hours = timeInSeconds / 3600;
            if (hours > 0) {
                durationtextview.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", timeInSeconds / 3600, timeInSeconds / 60, timeInSeconds % 60));
            } else {
                durationtextview.setText(String.format(Locale.getDefault(), "%02d:%02d", timeInSeconds / 60, timeInSeconds % 60));
            }
            //durationtextview.setText(aktuellerCounterDowner);*/ //TODO kannweg
        });
    }



}
