package com.example.remember.ui.memo;

import static android.content.Context.BIND_NOT_FOREGROUND;
import static com.example.remember.Main.alreadylistenedmemoidsList;
import static com.example.remember.Main.username;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.remember.Main;
import com.example.remember.R;
import com.example.remember.databinding.FragMemoBinding;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import io.realm.mongodb.App;
import io.realm.mongodb.AppConfiguration;
import io.realm.mongodb.User;
import io.realm.mongodb.mongo.MongoClient;
import io.realm.mongodb.mongo.MongoCollection;
import io.realm.mongodb.mongo.MongoDatabase;

public class MemoFragment extends Fragment implements PlayingService.PlayingStateChangeListener {
    //TODO (A) BELIEBTHEIT . how to update a field (e.g. "listenings") in a firestore document of a collection , when somebody clicks a button in the app ? the field "listenings"
    // 1 is a number that gets increased by 1 when the memory is accessed / or when the memory has been listened to for at least XX time.
    // 2 but this can only happen once per logged in firebase user !
    //   folg. code incremented den listenings count um 1: documentRef.update("listenings", FieldValue.increment(1));
    // 3 ... if a user listened to memory, his listenings-count increases as well by 1 -> set up new collection for that? -> no, this belongs to the users collection then probably. yeahh it does
    //   -> add listened audio to doc in "users" collection by querying it with .isEqualTo("username", getDisplayUsername / "userid", getuserid) and incrementing the field "listenings" by 1
    //   amendment to that: only THOSE users that uploaded/listened to ANYTHING , get the fields "listenings" and "uploads" (then subsequent queries of "users" will be more efficient)
    //   ("audio id" still very important to link uploaded audios to users)


    //TODO (A) (memo) memo erst als "listened" einstufen, wenn man mindestens 2 min gehört hat (?)
    // .. um aus-versehen-auf-memo-klicken nicht fälschlich als listened einzustufen
    // .. der utente würde sich dann auch denken: Hää, das hab ich mir NIE angehört, das = eine Lüge
    // .. ja..

    //TODO (A) der playingservice muss foreground sein, ABER ich kann nicht wie bei PlayingService den service destroyen, wenn ich die Activity verlasse...
    // -> der user soll ja zurück in die Memory-activity gehen können. demnach müsste ich den service ewig
    // -> service nur dann aktiv halten, wenn ENTWEDER - gerade ein audio gehört wird
    //    ODER die activity aktiv ist
    //    ... --> service erst binden & foregrounden & connecten & anmachen überhaupt, wenn USER auf PLAY drückt
    // .
    // ist whack
    private static final String TAG = "memo_frag";
    public static Timer timer;
    //public static final String memoFragTag = "memofrag"; //wofür war das no gl. xd irgendwas mit notific oder so nov23 :D;DDD LOl kollo pol Polen jaja

    //TODO (A) youtube tutorial für scrollview horizontal der geil ist:
    // https://www.youtube.com/watch?v=4yyLeI4H1rQ&ab_channel=SainalSultan
    // (discrete scrollv geht ja ned leider wegen jcenter discontinuity oder so ne SCHEISE 10.7.)

    //binding
    private FragMemoBinding binding;


    //vom Intent eingelesene Daten zu einer Memory TODO(C) warum static ? ich weiß immer noch nicht , was static bedeutet LO!WIOIOÖSFDJI!OKfds
    public static String title, duration, creator, creatorid, date, language, categories, listeners, hearts, url, id;
    //ui
    TextView titletv, titlecollapsedtv, remainingdurationtv, creatortv,  datetv;
    ImageButton fastforwardbutton, rewindbutton, playpausebutton, playpausecollapsedbutton, heartbutton, heartbuttoncollapsed;
    ImageView languageflag;
    LinearLayout llcollapsed;

    //service 1
    //TODO : Ensure that you acquire the necessary permissions (e.g., RECORD_AUDIO) in your manifest file and handle runtime permissions ...
    // ... if targeting Android 6.0 (Marshmallow) or above.
    //private PlayingServiceConnection serviceConnection; //das wurde simultan gentuzt mit der private class PlayingServiceConnection implements ServiceConnection
    private static PlayingService playingService = null;
    private boolean shallStartPausePlayingTriggerOnPlay = false; // Variable zur Überprüfung des Service-Zustands, ersetzt durch isBound (isServiceRunning war ursprünglich gedacht für startPauseRecording() - dabei ist die abfrage, ob die actvitiy mit dem service BOUND ist, genauso gewinnbringend einzusetzen. wenn der service nicht bound ist, dann war er es noch nie oder ist zwischendurch durch handybildschirm aus unbounded worden. wenn der service bound ist, dann wird onRecord angegriffen.
    private boolean isBound = false;

    //mongoDB realm initializa
    MongoDatabase mongoDatabase;
    MongoClient mongoClient;
    MongoCollection<Document> mongoUsersCollection, mongoAudiosCollection;
    //MongoNamespace mongoNamespace;
    User user;
    //UserIdentity userIdentity; //You can link multiple user identities to a single user account , erstmal not needed
    App app;
    String userid, usermail;

    //seekbar
    static SeekBar seekBar; //TODO AAA (static) MEMORY LEAK
    //private Handler seekBarUpdateHandler;
    //public static int desiredPlaybackPosition = -1;  // Initialize to an invalid value

    View root;

    //wird in PlayingService aufgerufen, für buildNotification
    public static boolean hearto;
    private boolean initialheart; //wird dann beim update v. heart (in onStop am 4.12.) verglichen mit dem Endvalue, um zu entscheiden, ob +1 hearts_given(listener) & hearts_received(uploader)
    //TODO (memo) (initialheart) initialheart muss zurückgesetzt werden, wenn memo ausgetauscht wird
    private int playint, pauseint, heartint, heartunfilledint, germanint, englishint, color_ui, color_one;
    private String remainingtimeformatted;

    ////////////////////////////////////
    /*private static final int STATE_PAUSED = 0;
    private static final int STATE_PLAYING = 1;

    private int currentstate;

    private MediaBrowserCompat mMediaBrowserCompat;
    private MediaControllerCompat mMediaControllerCompat;

    private MediaBrowserCompat.ConnectionCallback mMediaBrowserCompatConnectionCallback = new MediaBrowserCompat.ConnectionCallback() {

        @Override
        public void onConnected() {
            super.onConnected();
            try {
                mMediaControllerCompat = new MediaControllerCompat(getActivity(), mMediaBrowserCompat.getSessionToken());
                mMediaControllerCompat.registerCallback(mMediaControllerCompatCallback);
                setSupportMediaController(mMediaControllerCompat);
                getSupportMediaController().getTransportControls().playFromMediaId(String.valueOf(R.raw.warner_tautz_off_broadway), null);

            } catch( RemoteException e ) {

            }
        }
    };

    private MediaControllerCompat.Callback mMediaControllerCompatCallback = new MediaControllerCompat.Callback() {

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
            if( state == null ) {
                return;
            }

            switch( state.getState() ) {
                case PlaybackStateCompat.STATE_PLAYING: {
                    mCurrentState = STATE_PLAYING;
                    break;
                }
                case PlaybackStateCompat.STATE_PAUSED: {
                    mCurrentState = STATE_PAUSED;
                    break;
                }
            }
        }
    };*/ //github 2016
    ////////////////////////////////////







    //TODO (memoFrag) (mongo) wenn ein user ein audio ein 2. mal anhört, sollen die mongo data einfach überschr werden!


     /*@Subscribe(threadMode = ThreadMode.MAIN)
    public void onManipulateVisibilityofCollapsedMemoFragLL(ManipulateBSBonKeyboardChange event) {
        Log.d(TAG, "onManipulateVisibilityofCollapsedMemoFragLL, visible:" + event.visible);
        llcollapsed.setVisibility(event.visible ? View.VISIBLE : View.INVISIBLE);//GONE geht glaubi ned, weil dann würde aufrücken + will i net
        heartbuttoncollapsed.setClickable(false);
        playpausecollapsedbutton.setClickable(false);
    }*/ //13.12.23 geht ned, black screen overlaps searchfield... das+setDraggable=false AUSKOMMENTIERT


    //@Override //TODO (memoFrag) (override) DOES THAT NEED TO BE HERE???
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView memo");
        //MemoViewModel memoViewModel = new ViewModelProvider(this).get(com.example.remember.ui.memo.MemoViewModel.class);
        binding = FragMemoBinding.inflate(inflater, container, false);
        root = binding.getRoot();


//-1 ui
        titletv = binding.titleid;  titlecollapsedtv = binding.titlecollapsedid;
        creatortv = binding.creatorid;
        datetv = binding.dateid;
        remainingdurationtv = binding.remainingdurationid;
        languageflag = binding.languageflagid;
        playpausebutton = binding.playbuttonid;    playpausecollapsedbutton = binding.playbuttoncollapsedid;
        rewindbutton = binding.rewindbuttonid;
        fastforwardbutton = binding.fastforwardbuttonid;
        seekBar = binding.seekbarid;
        heartbutton = binding.heartbuttonid;
        heartbuttoncollapsed = binding.heartbuttoncollapsedid;
        llcollapsed = binding.llcollapsedid;
        //llcontent = binding.llcontentid;
        //llmemo = binding.llmemoid;

        //mongo
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
        CodecRegistry defaultJavaCodecRegistry = MongoClientSettings.getDefaultCodecRegistry(); //(!info!) (mongo) für Sorts,Updates,Filters einbauen nöitg
        mongoUsersCollection = mongoDatabase.getCollection("users").withCodecRegistry(defaultJavaCodecRegistry);
        mongoAudiosCollection = mongoDatabase.getCollection("audios").withCodecRegistry(defaultJavaCodecRegistry);
        //mongoCategoriesCollection = mongoDatabase.getCollection("categories", Category.class).withCodecRegistry(pojoCodecRegistry);

//resource ints
        //color_ui = ContextCompat.getColor(requireContext(), R.color.color_ui);
        //color_one = ContextCompat.getColor(requireContext(), R.color.color_one);
        playint = android.R.drawable.ic_media_play;
        pauseint = android.R.drawable.ic_media_pause;
        heartint = R.drawable.heart_white;
        heartunfilledint = R.drawable.heart_white_unfilled;
        germanint = R.drawable.germanflag;
        englishint = R.drawable.englishflag;

        Log.d(TAG, "onCreateView END");
        return root;
    }



    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onViewCreated memo");
        super.onViewCreated(view, savedInstanceState);

        //Main mainActivity = (Main) requireActivity();
        //mainActivity.createBottomSheetBehavior(this);
//alles in Main lol was für ein Nichtsnutzkomplex hier komplett
        //doBottomSheetBehaviour("create");
        // Customize the height of the bottom sheet (e.g., set it to 80% of the screen height) //bsdf
        //int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.8);  //bsdf
        //getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, height);   //bsdf

       /*  View collapsedview = view.findViewById(R.id.llcollapsedid);
       mainViewModel.getGrayscaleOfCollapsedLLinmemoFrag().observe(getViewLifecycleOwner(), offset -> {
            //Update grayscale of coollapsedLLinmemoFrag element based on the offset
            updateGrayscaleFilter(offset, collapsedview);
        });*/ //wird in Main gemacht lol

        Log.d(TAG, "onViewCreated END");
    }


    @Override
    public void onDestroyView() {
        Log.d(TAG,"onDestroyView memo");
        super.onDestroyView();
        binding = null;
    }






    @Override
    public void onStart() {
        Log.d(TAG,"onStart memo");
        super.onStart();
        /*if (!memofragstarted) { //25.11. prüfen*/
        //if not yet: setup callback mechanism
            if (playingService != null) {
                Log.d(TAG,"onStart: playingService != null -> set callback");
                playingService.setPlayingStateChangeListener(this); //onStart
            }
            //Bind to the service when the activity is started and keep (NOT) FOREGROUND state untouched:
            if (!isBound) {
                Log.d(TAG,"onStart: service is not bound und ist = " + playingService);
                Intent serviceIntent = new Intent(getContext(), PlayingService.class);
                requireContext().bindService(serviceIntent, serviceConnection, BIND_NOT_FOREGROUND);
                isBound = true;
                Log.d(TAG,"onStart: binded service ..BIND_NOT_FOREGROUND (service is = " + playingService + ")");
            }
        //eventbus
        //EventBus.getDefault().register(this); //TODO (eventbus memo)
        //Log.d(TAG,"onStart END");
    }

    @Override
    public void onResume() {//Called after onRestoreInstanceState, onRestart, or onPause. This is usually a hint for your activity to start interacting with the user
        Log.d(TAG,"onResume memo");
        super.onResume();
        /*if (!memofragstarted) { //25.11. prüfen*/
            //if not yet: setup callback mechanism
            if (playingService != null) {
                Log.d(TAG,"onResume: playingService != null -> set callback");
                playingService.setPlayingStateChangeListener(this); //onResume
            }
            //if not yet bound: Bind to the service when the activity is resumed and keep (NOT) FOREGROUND state untouched:
            if (!isBound) { //should never apply (because onPause doesn't unbind and onStop leads to first onStart and onStart already binds...) but for safety keep this code here. you never know
                Log.d(TAG,"onResume: service is not bound und ist " + playingService);
                Intent serviceIntent = new Intent(requireContext(), PlayingService.class);
                requireContext().bindService(serviceIntent, serviceConnection, BIND_NOT_FOREGROUND); //bind the already existing foreground service without changing FOREGROUND/NOT FOREGROUND state
                Log.d(TAG,"onResume: binded service ..BIND_NOT_FOREGROUND (service is = " + playingService + ")");
                isBound = true;
            }

        //memofragstarted = true; //nach onResume, onStart, onCreate erst updaten //25.11. prüfen
        //isActivityActive = true;
        //playingService.setPlayingStateChangeListener(Memory.this); //nicht nötig glaube ich, weil dies beim binden (oben) bereits abgewickelt irwd
        //Log.d(TAG,"onResume END");
    }

    @Override
    public void onPause() {//Called when the user no longer actively interacts with the activity, but it is still visible on screen. The counterpart to...
        Log.d(TAG,"onPause memo");
        //due to the fact that paused activities can still be visible (just not in foreground/focus), keep the service bind and state listener active!
        //Log.d(TAG,"onPause END");
        super.onPause();
    }

    //TODO (done I think) onFragmentVerlassen oder so , soll dann Mediaplayer releaset werden? ne, oder? damit man nicht aus versehen alles löscht
    @Override
    public void onStop() {//Called when you are no longer visible to the user. You will next receive either onRestart, onDestroy, or ...
        Log.d(TAG,"onStop memo");
        //due to the fact that stopped activities can still be visible (just not in foreground/focus), keep the service bind and state listener active!
        //Log.d(TAG,"onStop: unbinded service");


        //TODO (4.12. neu) ..now in onStop, instead of each time when heartbtn pressed
        //update mongo when this frag is closed, not every time user clicks heart-btn
        //entry in listener user doc for this memo SHOULD BE existent bc. memo has been listened to
        //(if memo hasn't been listened to even though user was in memoFrag: dann wird diese query Fehler throwen, weil kein datesoflistenings-array eintrag besteht)
        //hands-on Abfrage, ob title != null ➝ wurde überh schon ne memo eingelesen? weil memoFrag ist schon vorher aktiv + callt onStop ...
        //if (title != null) {
        if (Main.isMemoFragOpened) {
            Log.d(TAG,"title != null ➝ ne memo ist in memoFrag");
            updateheartbooleaninListenerUserDoc();
            //2 when initialheart was wrong and is now true:
            if (initialheart == false && hearto == true) {
                Log.d(TAG,"heart given! (und vorher keins)");
                incrementheartsAudioDoc();// +1 hearts(audio doc) ,
                incrementheartsgivenListenerUserDoc(); //+1 hearts_given(listeneruser doc) ,
                incrementheartsreceivedUploaderUserDoc(); //+1 hearts_received(uploaderuser doc)
            }
        }

        //eventbus
        //EventBus.getDefault().unregister(this); //TODO (eventbus memo)
        //Log.d(TAG,"onStop END");
        super.onStop();
    }




    @Override
    public void onDestroy() {//Perform any final cleanup before an activity is destroyed (e.g. finish) - if Player is active, it shall also die
        Log.d(TAG,"onDestroy memo");
        /*//if playing service is active:
        if (playingService != null) {
            //close callback mechanism
            playingService.setPlayingStateChangeListener(null);//generally good practice, but mostly depends on what happens inside the method though -> still ... keep
            //close foreground
            playingService.stopForeground(true); //if the service wasn't running as a foreground service before calling this, this will have no effect
            Log.d(TAG,"onDestroy: playingService != null -> callback and foreground removed");
        }
        //if not yet unbound: Unbind from the service when the activity is resumed and keep (NOT) FOREGROUND state untouched:
        if (isBound) {
            requireContext().unbindService(serviceConnection);
            Log.d(TAG,"onDestroy: isBound --> unbind service");
            //boolean is renewed anyway when Memory starts again.. but keeping track for other activities might be useful -> keep
            isBound = false;
        }
        //playingService wird geschlossen
        if (playingService != null) {
            playingService.onDestroy();
            Log.d(TAG,"onDestroy: crushed service by doing playingService.onDestroy()");
        }*/ //TODO (weiter) uncomment! playingService shall be destroyed when memoFrag is destroyed!  !
        //Log.d(TAG,"onDestroy END");
        super.onDestroy();
    }



    //this part didn't trigger because the updating of the PlayingService in onCreate was too slow for some reason so the PlayingService was still "null" ...
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG,"onServiceConnected(oSC): playingService ist " + playingService + "(hftl null), bevor der innere Part von oSC abgelaufen ist");
            Log.d(TAG, "oSC: service gets connected and binded:");
            PlayingService.LocalBinder binder = (PlayingService.LocalBinder) service;
            playingService = binder.getService();
            //callback interface to notify the UI in the Publish-activity when recording states have changed:
            playingService.setPlayingStateChangeListener(MemoFragment.this);
            //shallStartPauseRecordingTriggerOnRecord = true; //weggehauen weil unnötig
            isBound = true;
      /*      if (!isActivityActive) {//When activity becomes inactive, service will convert itself into foreground service using startForeground() method
                Log.d(TAG,"Activity not active, demnach convert into foreground service");
                // Convert the service to foreground service
                playingService.startForeground(1, playingService.getForegroundNotification());
                isBound = false;
            }*/ //not needed (anymore lol it has never) because foreground service only turns up while recorder in service is active, regardless of serviceconnection to Publish
            Log.d(TAG,"oSC: playingService ist " + playingService + ", nachdem oSC durchlief");
            Log.d(TAG,"oSC durchgelaufen (in Publish), also playingService gebindet, setplayingStateChangeListener an");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (playingService != null) {
                playingService.setPlayingStateChangeListener(null); //dies könnte unerwartetes verhalten mit dem interface vermeiden... ist aber ws unnötig
                playingService = null; //indicates that the connection is no longer active (playingService != playingService xd)
                //shallStartPauseRecordingTriggerOnRecord = false; //wenn service disconnected, soll - wenn der service dann iwann reconnected - immer noch onRecord ausgelöst werden
            }
            isBound = false;
            Log.d(TAG,"onServiceDisconnected durchgelaufen (in Publish), also playingService = null");
        }
    };






    public void initiatePlayingorOnPlay() { //diese method ist der Initiator für ALLES
        Log.d(TAG,"initiatePlayingorOnPlay");

        NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        boolean notificationsEnabled = notificationManager.areNotificationsEnabled();
        Log.d(TAG,"notificationsEnabled ist: " + notificationsEnabled);

        if (!shallStartPausePlayingTriggerOnPlay) { //wenn playingService noch nicht existiert, dann wird hier jetzt schöööön der service gebindet & gestartet
            Log.d(TAG,"INITIATE playing");
            //damit die foreground notification passieren kann, müssen Benachrichtigungen erteilt sein
            if(notificationsEnabled == false) {
                Log.d(TAG,"notificationsEnabled ist false, daher öffnet sich der dialog zum ändern der systemsettings");
                showNotificationSettingsDialog();
            }
            //wenn notifications erlaubt sind (für foreground notific)
            if (notificationsEnabled) {
                Log.d(TAG, "notifications enabled, also start playing\n" +
                        "url ist: " + url);
                      //  "playingService ist: " + playingService);
                //initiate playing
                playingService.initiatePlaying(); //startPlayingOrOnPlay: !shallStartPausePlayingTriggerOnPlay
                shallStartPausePlayingTriggerOnPlay = true; //initiatePlaying
                //start foreground service (and keep it on as long as recorder is active)
                //startForeground(2, playingService.getForegroundNotification());
                playingService.initiateNotification();
                //if audio not heard by user (userPreferences): add audio to audios-doc and to users-doc
                addAudioToAudioAndUsersDoc();

                Log.d(TAG,"➝➝➝ SERVICE FOREGRONDATO E INIZIATO");
            }
            //hier hatte ich seinerzeit setupSeekBar(); stehen, aber da das Preparen des mediaPlayers asynchronous ist, war der noch nicht preparet, als das hier auslöste -> error
        }
        else {
            Log.d(TAG,"RESUME/PAUSE playing");
            //wenn playingService bereits gestartet (aka, recording in service class gerade anläuft oder pausiert ist), dann NUR onPlay
            playingService.onPlay();
        }
        //seekbar
        // Start updating the seek bar
        //updateSeekBar(); old chatting
        Log.d(TAG,"initiatePlayingorOnPlay END");
    }


    //+1 listeners AUDIO & listenings USER +1 & listenedaudiowithdate USER, wenn user in memory auf playbutton gedrückt hat (geht nur 1x pro user/audio natürlich)
    private void addAudioToAudioAndUsersDoc() {
        Log.d(TAG, "addAudioToAudioAndUsersDoc called");

        //TODO (weiter) (memofrag) sobald user listened to memo, 1 update online datesoflistenings :) and 2 update homefrag & search display!!!!

        //update user doc (listeningswithdates) & alreadylistenedmemosList
        if (alreadylistenedmemoidsList != null) {
            Log.d(TAG,"alreadylistenedmemoidsList != null ➝ update userDoc depending on listenedyet or not");
            updateUserDoc(alreadylistenedmemoidsList.contains(id)); //if contains id=true (listenedyet), if not=false (!listenedyet)
            alreadylistenedmemoidsList.add(id);
        }
        else {
            Log.d(TAG,"alreadylistenedmemoidsList = null ➝ only update date of this specific memo in datesoflistenings ");
            updateUserDoc(false);
            alreadylistenedmemoidsList = new ArrayList<>();
            alreadylistenedmemoidsList.add(id);
        }
    }

    private void updateUserDoc(boolean listenedyet) {
        Log.d(TAG,"updateUserDoc, listenedyet:" + listenedyet);
        Date currentDate = new Date();
        Document queryUser = new Document("_id", userid);

        //1 user hasn't yet listened memo:
        if (!listenedyet) {
            Log.d(TAG, username + " non ha mai ascoltato a: " + title);
            //1.1 increment AUDIO listeners-count +1
            Document queryFilterAudio = new Document("_id", id);
            Bson updateAudio = Updates.inc("listeners", 1);
            Log.d(TAG, "audio update is: " + updateAudio);
            mongoAudiosCollection.updateOne(queryFilterAudio, updateAudio).getAsync(task -> {
                if (task.isSuccess()) {
                    long count = task.get().getModifiedCount();
                    if (count == 1) Log.v(TAG, "success update audiodoc");
                    else Log.e(TAG, "error update audiodoc");
                } else {
                    Log.e(TAG, "error in task update audiodoc: ", task.getError());
                }
            });
            //1.2 increment USER listening-count +1 & append memo-id to datesoflistenings-arrayfield
            //String dateofid = formattedDate + "_" + id + "_false"; //composed mit "_" , false für not-hearted
            Bson updateUser = Updates.combine(
                    Updates.inc("listenings", 1),
                    Updates.addToSet("datesoflistenings",
                            new Document("date", currentDate).append("id", id).append("heart", false))); //TODO (3.12. neu) umbau auf array-struktur von datesoflistenings
            Log.d(TAG, "user update is: " + updateUser);
            mongoUsersCollection.updateOne(queryUser, updateUser).getAsync(task -> {
                if (task.isSuccess()) {
                    long count = task.get().getModifiedCount();
                    if (count == 1) Log.v(TAG, "success update userdoc");
                    else Log.e(TAG, "error update userdoc");
                } else {
                    Log.e(TAG, "error in task update userdoc (updateUserDoc): ", task.getError());
                }
            });
        }

        //2 user has already listened memo
        else {
            Log.d(TAG, username + " ha già ascoltato a: " + title);
            //no need to increment listeners/listenings counts. only:
            //2.1 update date of this specific memo in datesoflistenings
            /*//change the first 34 characters (=date) in the entry in datesoflistenings
            List<String> idfilter = Collections.singletonList(id);
            Document updateUser = new Document("$set",
                    new Document("datesoflistenings.$[elem]", //[elem] is selfmade placeholder
                            new Document("$concat", //concatenate formattedDate (index0-33) with the range from index34 until end[-1]
                                    Arrays.asList(formattedDate,
                                    new Document("$substr", Arrays.asList("datesoflistenings.$[elem]", 34, -1))))))
                    .append("arrayFilters", List.of(new Document("elem", idfilter)));*/ //old datesoflistenings als 1 string, nicht doc-array of 3 diff. parts date+id+heart

            //this filter should not need to .append("datesoflistenings.id", id) because the id should be in it because it's in alreadylistenedmemoidsList
            Document filter = new Document("_id", userid).append("datesoflistenings.id", id);
            Document update = new Document("$set", new Document("datesoflistenings.$[element].date", currentDate)
                    .append("arrayFilters", List.of(new Document("element.audioid", Filters.eq(id)))));
            //query explanation: the query updates a document in the collection where the _id matches the provided userid and the datesoflistenings array contains an element with the specified id. It then sets the date field of that array element to the value of currentDate. The array filter ensures that the update is applied only to the array element with the matching audioid

            Log.d(TAG, "user update is: " + update);
            mongoUsersCollection.updateOne(filter, update).getAsync(task -> {
                if (task.isSuccess()) {
                    long count = task.get().getModifiedCount();
                    if (count == 1) Log.v(TAG, "success update userdoc");
                     else Log.e(TAG, "error update userdoc");
                } else {
                    Log.e(TAG, "error in task update userdoc (updateUserDoc): ", task.getError());
                }
            });
        }

    }


    public void initializePlayingService() {
        Log.d(TAG,"initializePlayingService called");
        //2 PlayingService Intent & binding in gang sätsen
        Intent serviceIntent = new Intent(requireContext(), PlayingService.class);
        serviceIntent.putExtra("url", url);
        requireContext().startService(serviceIntent); //start servic
        requireContext().bindService(serviceIntent, serviceConnection, BIND_NOT_FOREGROUND); //bind servic
        //requireContext().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE); //can't call that bc. i need to call .startService to trigger onStartCommand in the service!
        Log.d(TAG,"onCreate: service Intent abgefeuert und Binding in Gang gesetzt .. BIND_AUTO_CREATE" +
                "\nplayingService ist (noch) " + playingService);
        //(info) (memoFrag) hier kommt null, obwohl gerade gebindet wurde -> ist asynchroner Vorgang
    }


    //seekbar
    public static void setupSeekBar() { //String mode
        Log.d(TAG, "setupSeekBar");
        int audioduration = playingService.getDuration(); //in ms
        seekBar.setMax(audioduration);
        //2 timer for automatic progress setting of seekBar
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() { //TODO 15.1. hier kommt NPE wenn app geschl. wird weil playingservice dann shutdowned ist, aber timer an bleibt
                seekBar.setProgress(playingService.getCurrentPosition()); //setupSeekBarAfterPlayingServiceIsPrepared
            }
        }, 0, 100); //je niedriger period, desto kontinuierlicher die fortschrittsanzeige aufm seekBar //TODO (seekbar) EINSETELLEN
        //TODO (weiter) 13.12. (ONDESTROY) das hier löst (wai) aus bei ondestroy + playingService NOR , wenn audio not yet started...

        /*//Log.d(TAG, "setupSeekBar: " + mode);
        int audioduration;
        if (mode.equals("afterPlayingServiceIsPrepared")) {
            //1 set max sec for seekBar
            audioduration = playingService.getDuration(); //in ms
            seekBar.setMax(audioduration);
            //2 timer for automatic progress setting of seekBar
            new Timer().scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    seekBar.setProgress(playingService.getCurrentPosition()); //setupSeekBarAfterPlayingServiceIsPrepared
                }
            }, 0, 100); //je niedriger period, desto kontinuierlicher die fortschrittsanzeige aufm seekBar //TODO (seekbar) EINSETELLEN
        } else { //"beforePlayingServiceIsPrepared"
            audioduration = MemoFragment.convertDurationStringToInt(MemoFragment.duration, "ms");
            seekBar.setMax(audioduration);

        }*/ //don't need former mode 29.11.
        /*//3 user interaction with seekBar HAS TO BE IN ONCREATE BECAUSE OTHERWISE THE USER CAN'T CHANGE THE BAR BEFORE THE PLAYING OF THE AUDIO HAS BEEN STARTED
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d(TAG, "sSB onProgressChanged called");
                if (fromUser) {
                    playingService.seekTo(progress);
                    //update the remainingdurationtextview with the formatted remaining duration (not in ms)
                    int remainingduration = ( audioduration - playingService.getCurrentPosition() ) / 1000; //in s
                    int hours = remainingduration / 3600;
                    Log.d(TAG, "remainingduration is: " + remainingduration);
                    Log.d(TAG, "hours is: " + hours);

                    if (hours < 1) {
                        remainingdurationtextview.setText(String.format(Locale.getDefault(), "%02d:%02d", remainingduration / 60, remainingduration % 60));
                    } else { //^= mind. 1h
                        remainingdurationtextview.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", remainingduration / 3600, remainingduration / 60, remainingduration % 60));
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Do nothing or any actions needed when user starts interacting with the seek bar
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Do nothing or any actions needed when user stops interacting with the seek bar
            }
        });*/ //user interaction with seekBar HAS TO BE IN ONCREATE
        //Log.d(TAG, "setupSeekBar END");
    }




    public static int convertDurationStringToInt(String duration, String unit) {
        String[] parts = duration.split(":");
        int length = parts.length;

        int hours = 0;
        int minutes = 0;
        int seconds = 0;

        if (length == 3) {
            hours = Integer.parseInt(parts[0]);
            minutes = Integer.parseInt(parts[1]);
            seconds = Integer.parseInt(parts[2]);
        } else if (length == 2) {
            minutes = Integer.parseInt(parts[0]);
            seconds = Integer.parseInt(parts[1]);
        }

        int totalseconds = (hours * 3600) + (minutes * 60) + seconds;
        int totalmilliseconds = ((hours * 3600) + (minutes * 60) + seconds) * 1000;

        if (unit.equals("s")) {
            Log.d(TAG, "duration in s: " + totalseconds);
            return totalseconds;
        }
        else {
            Log.d(TAG, "duration in ms: " + totalmilliseconds);
            return totalmilliseconds; //"ms"
        }
    }


    private void incrementheartsAudioDoc() {
        Document queryfilteraudio = new Document("_id", id);
        Bson updateaudio = Updates.inc("hearts", 1);
        mongoAudiosCollection.updateOne(queryfilteraudio, updateaudio).getAsync(task -> {
            if (task.isSuccess()) {
                long count = task.get().getModifiedCount();
                if (count == 1) Log.v(TAG, "success update audiodoc");
                else Log.e(TAG, "error update audiodoc");
            } else {
                Log.e(TAG, "error in task update audiodoc: ", task.getError());
            }
        });
    }
    private void incrementheartsgivenListenerUserDoc() {
        Document queryfilterlistener = new Document("_id", userid);
        Bson updatelistener = Updates.inc("hearts_given", 1);
        mongoUsersCollection.updateOne(queryfilterlistener, updatelistener).getAsync(task -> {
            if (task.isSuccess()) {
                long count = task.get().getModifiedCount();
                if (count == 1) Log.v(TAG, "success update listener userdoc");
                else Log.e(TAG, "error update listener userdoc");
            } else {
                Log.e(TAG, "error in task update listener userdoc: ", task.getError());
            }
        });
    }

    private void updateheartbooleaninListenerUserDoc() {
        //1 change heart-boolean in userdoc (listener)
        //update heart-boolean in datesoflistenings + hearts_given
        Document filter = new Document("_id", userid).append("datesoflistenings.id", id);
        Document update = new Document("$set", new Document("datesoflistenings.$[element].heart", hearto)
                .append("arrayFilters", List.of(new Document("element.id", Filters.eq(id))))); //TODO (4.12.) kein error mehr jz? element.id
        //query explanation: the query updates a document in the collection where the _id matches the provided userid and the datesoflistenings array contains an element with the specified id. It then sets the date field of that array element to the value of currentDate. The array filter ensures that the update is applied only to the array element with the matching audioid
            /*List<String> idfilter = Collections.singletonList(id);
            Document queryUser = new Document("_id", userid);
            Document updateUser = new Document("$set",
                    new Document("datesoflistenings.$[elem]", //[elem] is selfmade placeholder
                            new Document("$concat",
                                    Arrays.asList(new Document("$substr", Arrays.asList("datesoflistenings.$[elem]", 34, -1), heartstring))
                                    )
                            )
                    )
                    .append("arrayFilters", Arrays.asList(new Document("elem", idfilter)));
            //concatenate formattedDate (index0-33) with the range from index34 until end[-1]*/ //old bitd when dateoflistenings was a composed string .lol. :D Krooc i'm a crook acc. to Pratim's observations
        Log.d(TAG, "user update is: " + update);
        mongoUsersCollection.updateOne(filter, update).getAsync(task -> {
            if (task.isSuccess()) {
                long count = task.get().getModifiedCount();
                if (count == 1) Log.v(TAG, "success update userdoc");
                else Log.e(TAG, "error update userdoc");
            } else {
                Log.e(TAG, "error in task update userdoc (onStop): ", task.getError());
            }
        });
    }

    private void incrementheartsreceivedUploaderUserDoc() {
        // creatorid nehmen, und über diesen dessen userdoc finden
        Document queryfilteruploader = new Document("creatorid", creatorid);
        Bson updateuploader = Updates.inc("hearts_received", 1);
        mongoUsersCollection.updateOne(queryfilteruploader, updateuploader).getAsync(task -> {
            if (task.isSuccess()) {
                long count = task.get().getModifiedCount();
                if (count == 1) Log.v(TAG, "success update uploader userdoc");
                else Log.e(TAG, "error update uploader userdoc");
            } else {
                Log.e(TAG, "error in task update uploader userdoc: ", task.getError());
            }
        });
    }




    // Methode zum Anzeigen der Benachrichtigungseinstellungen TODO (C) muss noch gecheckt werden, ob läuft
    private void showNotificationSettingsDialog() {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }


    public void displaymemoDataandPrepareUi(ArrayList<String> memometadatalist) {
        Log.d(TAG, "displayMemoData called");

        //jes:
        Main.isMemoFragOpened = true;

    //1 variables benennen
        //ArrayList<String> memometadatalist = mainViewModel.getMemoMetadata(); //old VM
        title = memometadatalist.get(0);
        duration = memometadatalist.get(1);
        creator = memometadatalist.get(2);
        creatorid = memometadatalist.get(3);
        date = memometadatalist.get(4);
        listeners = memometadatalist.get(5);
        hearts = memometadatalist.get(6);
        categories = memometadatalist.get(7);
        language = memometadatalist.get(8);
        url = memometadatalist.get(9);
        id = memometadatalist.get(10); //von hinten nach vorne auslesen aus arraylist, weil sich sonst die reihenfolge verendert :D wenn ich removen würde lol

        Log.d(TAG, "title: " + title +
                ", duration: " + duration +
                ", creator: " + creator +
                ", creatorid: " + creatorid +
                ", date: " + date +
                ", listeners: " + listeners +
                ", hearts: " + hearts +
                ", categories: " + categories +
                ", language: " + language +
                "\nurl: " + url +
                ", id: " + id);

    //2 textviews füllen
        //show tytle
        titletv.setText(title);
        titlecollapsedtv.setText(title);
        //show creatur
        creatortv.setText(creator);
        //show datumi
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);//backend //Thu Oct 26 17:33:50 GMT+02:00 2023
            Date parsedDate = dateFormat.parse(date);
            SimpleDateFormat outputFormatNormal = new SimpleDateFormat("dd. MMM yyyy", Locale.getDefault());//frontend
            assert parsedDate != null;
            String formattedDateString = outputFormatNormal.format(parsedDate);
            datetv.setText(formattedDateString);
            //Log.d(TAG, "date parsed correctly wird korrekt angezeigt...");
        }
        catch (ParseException e) {
            e.printStackTrace();
            Log.e(TAG, "ERRORRI while parse parsing parseus perseus date!! normal 'Date' is transmitted into textview...");
            datetv.setText(date);
        }
        //show remainingduro-ation
        remainingdurationtv.setText(duration);
        //show longage
        if (language.equals("en")) setBackgroundofimagebutton(languageflag, null, englishint);
        else setBackgroundofimagebutton(languageflag, null, germanint);
        //TODO (future) (languages)
        //show playbuttón (unnötig, wird schon angezeigt, aber was solls)
        //setBackgroundplaypausebutton(playbuttonint, color_ui); //TODO kannweg
        //fastforwardbutton.setBackground(ContextCompat.getDrawable(Memory.this, R.drawable.fastforwardbutton));
        //rewindbutton.setBackground(ContextCompat.getDrawable(Memory.this, R.drawable.rewindbutton));

    //3 button onClick listeners
        playpausebutton.setOnClickListener(v -> initiatePlayingorOnPlay());
        playpausecollapsedbutton.setOnClickListener(v -> initiatePlayingorOnPlay());
        fastforwardbutton.setOnClickListener(v -> playingService.jumpPlayer("fastforward"));
        rewindbutton.setOnClickListener(v -> playingService.jumpPlayer("rewind"));
        //TODO (A) when user listened to memo1 and then he switches to memo2 , the former playingService should be overwritten with the new one

    //4 seekBar onChange listener
        //convert durationstring into seconds and set those seconds as seekBar max
        int durationinms = convertDurationStringToInt(duration, "ms");
        seekBar.setMax(durationinms); //TODO (seekbar) ok ??? mit ms statt s [28.11. XDLMAOROFLMDRKEHH]

        //user interaction with seekBar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    Log.d(TAG, "sSB onProgressChanged called (user interaction), progress:"+ progress);
                    //TODO (A) eig müsste man hier noch abfragen vorher (um error zu vermeiden), ob der player in PlayingService bereits fertig prepared ist o net

                    //1 player seeken
                    playingService.seekTo(progress); //onProgressChanged //TODO (player) nur so , den playingService hab ich auch static gesetzt wegen der method setupSeekBar()

                    //2 ui anpassen
                    int remaining_ms = playingService.getDuration() - playingService.getCurrentPosition();
                    int remaining_s = remaining_ms / 1000; //in seconds
                    int hours = remaining_s / 3600;
                    Log.d(TAG, "remaining ms:" + remaining_ms +
                            "\nremaining s:" + remaining_s +
                            "\nremaining hours:" + hours);
                    /*if (PlayingService.playbackHasBeenStarted) { //GENAUER, fragt die current position vom player ab in ms und rechnet sie dann in s um
                        Log.d(TAG, "player wurde schon gestartet");
                        //update the remainingdurationtextview with the formatted remaining duration (not in ms)
                        remaining_ms = playingService.getDuration() - playingService.getCurrentPosition();
                        remaining_s = remaining_ms / 1000; //in seconds
                        hours = remaining_s / 3600;
                        Log.d(TAG, "remaining ms:" + remaining_ms +
                                "\nremaining s:" + remaining_s +
                                "\nremaining hours:" + hours);
                    }
                    else { //UNGENAUER, fragt den current progress im seekBar ab
                        Log.d(TAG, "player wurde noch nicht gestartet");
                        //wenn das playback noch nicht gestartet wurde, ist playingService.getCurrentPosition() sempre = 0, which does create problems for the above logic

                        //1 Calculate remaining duration based on the seek bar progress
                        remaining_ms = durationinms - progress; // wird als ms eingelesen
                        remaining_s = remaining_ms / 1000; // & dann *1000
                        hours = remaining_s / 3600;
                        Log.d(TAG, "remaining ms:" + remaining_ms +
                                "\nremaining s:" + remaining_s +
                                " \nremaining hours:" + hours);
                               // " \ncurrent progress: " + progress +
                              //  " \ntotal duration in s: " + durationinms

                        //2 set the base point for the audio playback to the current point of progress (XDD mal schaun, schaun wa ma was läuft/komt/geht , isaj ajjaaj ja eglalal)
                        if (!PlayingService.playbackHasBeenStarted) {
                            PlayingService.playedTime = progress / 1000; //convert ms into s
                        }
                    } */ //if (PlayingService.playbackHasBeenStarted) stuff, nicht mehr nötig, weil seekBar nun im prepareAsync listener initialized wird... 29.11.
                    String remainingsecondsString;
                    if (hours < 1) {
                        remainingsecondsString = String.format(Locale.getDefault(), "%02d:%02d", remaining_s / 60, remaining_s % 60);
                    } else { //^= mind. 1h
                        remainingsecondsString = String.format(Locale.getDefault(), "%02d:%02d:%02d", remaining_s / 3600, remaining_s / 60, remaining_s % 60);
                    }
                    remainingdurationtv.setText(remainingsecondsString);

                    //notif
                    if (PlayingService.isPlaying) playingService.buildNotification(remainingsecondsString, "play", 1f); //memo seekbar change
                    else playingService.buildNotification(remainingsecondsString, "pause", 0f); //memo seekbar change


                }
                //else Log.d(TAG, "sSB onProgressChanged called (NO user interact.), progress:" + progress); //wird alle 100ms gecallt (Wert festgelegt setupSeekBarAfterPlayingServiceIsPrepared)

            }


            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Do nothing or any actions needed when user starts interacting with the seek bar
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Do nothing or any actions needed when user stops interacting with the seek bar
            }
        });

        //set heart-btn (filled/unfilled)
        //query for userdoc of listener (if user has listened to audio yet, get heart - otherwise,
        Document filter = new Document("_id", userid).append("datesoflistenings.id", id);
        mongoUsersCollection.findOne(filter).getAsync(task -> {
            if (task.isSuccess()) {
                Document doc = task.get();
                if (doc != null) { //TODO (4.12. neu)
                    Log.v(TAG, "userdoc found ➝ set ui based on 'heart' boolean");
                    //1 get the heart-boolean (=hearto)
                    List<Document> datelisteningsheartArray = doc.getList("datesoflistenings", Document.class);
                    if (datelisteningsheartArray != null) { //(info) in this specific case redundant if-clause! can't be null bc. query found it
                        if (!datelisteningsheartArray.isEmpty()) {
                            for (Document datelisteningheart : datelisteningsheartArray) {
                                hearto = datelisteningheart.getBoolean("heart");
                                Log.d(TAG, "(initialheart = hearto = " + hearto);
                                initialheart = hearto;
                            }
                        }
                    }
                    if (hearto == true) {
                        setBackgroundofimagebutton(heartbutton, heartbuttoncollapsed, heartint);
                    } else {
                        setBackgroundofimagebutton(heartbutton, heartbuttoncollapsed, heartunfilledint);
                    }
                }
                else {
                    Log.v(TAG, "NO userdoc found ➝ set heart-unfilled");
                    setBackgroundofimagebutton(heartbutton, heartbuttoncollapsed, heartunfilledint);
                    initialheart = false;
                    hearto = false;
                }
                //onclicklisteners
                Log.d(TAG, "onclicklisteners are set");
                heartbutton.setOnClickListener(v -> toggleHeart()); //onToggleHeart() //TODO (4.12. neu)
                heartbuttoncollapsed.setOnClickListener(v -> toggleHeart()); //onToggleHeart() //TODO (4.12. neu)
            } else {
                Log.e(TAG, "error in task get hearto from userdoc: ", task.getError());
            }
        });
    }//END___displaymemoDataandPrepareUi__________________________________________________________________________

    private void toggleHeart() { //ehem. Zwischenschritt für den heartbutton onClickListener
        Log.d(TAG,"toggleHeart: " + hearto + "➝"+ !hearto + " (callback in memoFrag)");
        //toggle heart in memoFrag
        // 2 change memo ui
        if (hearto == true) {
            setBackgroundofimagebutton(heartbutton, heartbuttoncollapsed, heartunfilledint);
        } else {
            setBackgroundofimagebutton(heartbutton, heartbuttoncollapsed, heartint);
        }
        //flip toggle turn 'heart' (anstelle immer heart aufs neue aus der Datenbank auszulesen - also 'heart' WIRD geupdated in mongo on buttonclick - aber das Auslesen muss ja nicht sein, geht ja auch mit bololean )
        hearto = !hearto;
        //toggle heart in notific
        if (PlayingService.isPlaying) playingService.buildNotification(remainingtimeformatted, "play", 1f); //toggleHeart
        else playingService.buildNotification(remainingtimeformatted, "pause", 0f); //toggleHeart
    } //NICHT unnötig, kann zwar auch direkt in onToggleHeart gemacht werden, aber auf onToggleHeart greift ja auch playingService zu und der würde dann dplt machen

    private void setBackgroundofimagebutton(ImageView imagebutton1, ImageButton imagebutton2, int imageresource) {
        imagebutton1.setImageResource(imageresource);
        //imagebutton1.setColorFilter(color);
        if (imagebutton2 != null) imagebutton2.setImageResource(imageresource);
        //imagebutton2.setColorFilter(color);
    }


    //---------------------------------------------------------------------------------------------------CALLBACK INTERFACES
    //callback interface to notify the UI in the Publish-activity when Playing states have changed:
    //isPlaying muss übernommen werden, damit onPlay funktionieren kann
    //dasselbe mit der stoppuhr
    @Override
    public void onPlayingStarted() {
        // Update the UI when Playing starts (whether Playing starts, is read out of the service-class over the interface)
        requireActivity().runOnUiThread(() -> {
            Log.d(TAG,"onPlayingStarted (callback in memoFrag)");
            setBackgroundofimagebutton(playpausebutton, playpausecollapsedbutton, pauseint);
        });
    }
    @Override
    public void onPlayingPaused() {
        // Update the UI when Playing is paused
        requireActivity().runOnUiThread(() -> {
            Log.d(TAG,"onPlayingPaused (callback in memoFrag)");
            setBackgroundofimagebutton(playpausebutton, playpausecollapsedbutton, playint);
        });
    }
    @Override
    public void onPlayingResumed() {
        // Update the UI when Playing is resumed
        requireActivity().runOnUiThread(() -> {
            Log.d(TAG,"onPlayingResumed (callback in memoFrag)");
            setBackgroundofimagebutton(playpausebutton, playpausecollapsedbutton, pauseint);
        });
    }
    @Override
    public void onPlayingFinished() {
        // Update the UI when Playing is stopped
        requireActivity().runOnUiThread(() -> {
            Log.d(TAG,"onPlayingStopped (callback in memoFrag)");
            //TODO (C) vlt sollte der playbutton auf pausebutton gesetzt werden? weil das audio ist vorbei ... oder .. naja habs jz mal gemacht:
            setBackgroundofimagebutton(playpausebutton, playpausecollapsedbutton, playint);
            remainingdurationtv.setText(R.string.durationnull); //set the durationtext to 00:00
            shallStartPausePlayingTriggerOnPlay = false; //onPlayingFinished
        });
    }

    @Override
    public void onToggleHeart() { //boolean heart //TODO (4.12. neu)
        requireActivity().runOnUiThread(() -> {
            Log.d(TAG,"onToggleHeart: " + hearto + "➝"+ !hearto + " (callback in memoFrag)");
            //heartbutton clicks in memo + notific both lead here

            //1 update mongo user + audio
            //  in onStop - wird mir sonst zu viel rumgequerie und -inserte
            //2 change memo ui
            if (hearto == true) {
                setBackgroundofimagebutton(heartbutton, heartbuttoncollapsed, heartunfilledint);
            } else {
                setBackgroundofimagebutton(heartbutton, heartbuttoncollapsed, heartint);
            }
            //flip toggle turn 'heart' (anstelle immer heart aufs neue aus der Datenbank auszulesen - also 'heart' WIRD geupdated in mongo on buttonclick - aber das Auslesen muss ja nicht sein, geht ja auch mit bololean )
            hearto = !hearto;
            //Log.d(TAG,"hearto toggled to:"+hearto);

            //TODO (weiter) 3.12. kann das mit dem boolean so funktionieren, dass ich den statt der ständigen Weitergabe von "heart" als parameter nehme ??? :D
        });
    }


    //seekbar
    @Override
    public void onPlayingStateChanged(boolean isPlaying) {
        // Update your UI based on the playing state if needed
    }

    @Override
    public void onTimerTick(String remainingTimeFormatted) {
        // Update the UI when the timer does 1 tick
        requireActivity().runOnUiThread(() -> {
            Log.d(TAG,"onTimerTick (callback in memoFrag)");
            remainingdurationtv.setText(remainingTimeFormatted);
            remainingtimeformatted = remainingTimeFormatted;
        });
    }
}//END OF IT ALLLLLLLLLLL___dfs903iikdsfldfdsf________________________489gfdjkdfgjk3________________