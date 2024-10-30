package com.example.remember.ui.memo;

import static com.example.remember.ui.memo.MemoFragment.hearto;
import static com.example.remember.ui.memo.MemoFragment.timer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;

import com.example.remember.R;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

public class PlayingService extends MediaBrowserServiceCompat { //extends Service
    private static final String TAG = "PlayingService";
    //ich brauche diesen foreground service leider, damit man auch bei geschlossener activity weiter aufnehmen kann (weil der service läuft weiter im hintergrund, und die activity kann das nicht)


//basic MediaRecorder stuff
    private MediaPlayer player = null; //TODO
    public static boolean isPlaying = false;

//5 duration timer für MediaRecorder
    long remainingTime;
    private String remainingTimeFormatted = ""; // Initialize with an empty string
    //private boolean isTimerPaused = false;
    //TODO (C) die aktuelle aufnahmensekundenzahl kann am ende aus dem edittext oder aus dem long ausgelesen werden xD für die metadatem


//startForeground stuff
    private static final String CHANNEL_ID = "NOTIFCHANNEL_PLAYING";
//callback interface to notify the UI in the Publish-activity when PLAYING states have changed:
    public PlayingStateChangeListener playingStateChangeListener;

    //url wird über intent von MemoFrag zu hier transferiert und mittels der url kann der player prepared werden, sodass schon von anfang an geseekt werden kann  :D
    String url;

    //boolean, weil seekBar anders funktioniert, wenn player noch nicht angestellt ist und actual currentDuration = 0 ist, aber der Balken schon verschoben wurde... (bevor audio abgespielt)
    //public static boolean playbackHasBeenStarted; //nicht mehr nötig, weil seekBar in prepareAsync listener initialized würt

    //handler für onTick
    private Handler handler;
    private Runnable updateRunnable;

    //notification
    private MediaSessionCompat mediaSession; //yt tut
    private MediaControllerCompat mediaController;
    private int flag; //for the pendingIntents
    //public static boolean playbackcompleted; //TODO (player) (complete) ähhhhh playbackcompleted marcoushhhhhh

    //MediaControllerCompat.Callback: receive updates about changes in playback state, metadata, ...
    public final MediaControllerCompat.Callback mediaControllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            // Handle changes in playback state (e.g., playing, paused, stopped)
            Log.d(TAG, "MCC state changed: " + state);
        }
        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            // Handle changes in metadata (e.g., title, artist, album)
            Log.d(TAG, "MCC metadata changed: " + metadata);
        }
    };

    //MediaSessionCompat.Callback: wenn man was in der notific drückt (actions), dann wird es hier erkannt:
    public final MediaSessionCompat.Callback mediaSessionCallback = new MediaSessionCompat.Callback() {
        @Override
        public void onPlay() {
            Log.d(TAG,"MSC onPlay");
            super.onPlay();
            /*if( !successfullyRetrievedAudioFocus() ) {
                return;
            }*/ //github 2016haha

            //start player
            resumePlaying(); //MSC onPlay

            //extra stuff
            //mediaSession.setActive(true); //github //TODO (weiter) 29.11. NÖTIG???
            buildNotification(remainingTimeFormatted, "play", 1f); //MSC onPlay
        }

        @Override
        public void onPause() {
            Log.d(TAG,"MSC onPause");
            super.onPause();
            if(player.isPlaying() ) {
                //pause player
                pausePlaying(); //MSC onPause

                //extra stuff
                buildNotification(remainingTimeFormatted, "pause", 0f); //MSC onPause
            }
        }

       /* @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            super.onPlayFromMediaId(mediaId, extras);

            try {
                AssetFileDescriptor afd = getResources().openRawResourceFd(Integer.valueOf(mediaId));
                if( afd == null ) {
                    return;
                }

                try {
                    mMediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());

                } catch( IllegalStateException e ) {
                    mMediaPlayer.release();
                    initMediaPlayer();
                    mMediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                }

                afd.close();
                initMediaSessionMetadata();

            } catch (IOException e) {
                return;
            }

            try {
                mMediaPlayer.prepare();
            } catch (IOException e) {}

            //Work with extras here if you want
        } */ //github @Override onPlayFromMediaId

        @Override
        //!!! MEDIA_BUTTON events are typically those that appear on headsets or other external media controllers .:::: oare AARE TEHY????? I AM NOT SURE
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            /*if (!isPlaying) {
                Log.d(TAG,"MSC onMediaButtonEvent: start player");
                //start player
                playingStateChangeListener.onPlayingStarted();
                isPlaying = true;
                player.start();
                buildNotification(remainingtimeformatted, R.drawable.pausebutton, 1f); //MSC: play
            } else {
                Log.d(TAG,"MSC onMediaButtonEvent: pause player");
                //pause player
                playingStateChangeListener.onPlayingPaused();
                isPlaying = false;
                player.pause();
                buildNotification(remainingtimeformatted, R.drawable.playbutton, 0f); //MSC: pause
            }*/ //old stuff with NotifBrotcastReceiver...
            return super.onMediaButtonEvent(mediaButtonEvent);
        }

        @Override
        public void onSeekTo(long pos) {
            //Log.d(TAG,"MSC onSeekTo (pos:" + pos + ")");
            super.onSeekTo(pos);
            player.seekTo((int) pos);

            int STATE = isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;
            float playbackSpeed = isPlaying ? 1f : 0f;
            PlaybackStateCompat playbackStateNew = new PlaybackStateCompat.Builder()
                    .setState(STATE, player.getCurrentPosition(), playbackSpeed)
                    .setActions(PlaybackStateCompat.ACTION_SEEK_TO //bitmap | so
                            | PlaybackStateCompat.ACTION_PLAY_PAUSE
                            | PlaybackStateCompat.ACTION_SET_RATING)
                    .build();
            Log.d(TAG,"MSC onSeekTo (pos:" + pos + ") , isPlaying=" + isPlaying);
            mediaSession.setPlaybackState(playbackStateNew);
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            super.onCustomAction(action, extras);
            Log.d(TAG,"MSC onCustomAction");

            if (action.equals("action_heart")) {
                /*boolean heart = false;
                if (extras != null) {
                    if (extras.containsKey("heart")) {
                        heart = extras.getBoolean("heart");
                        Log.d(TAG,"MSC heart:" + heart);
                    }
                }*/ //TODO (4.12. neu)
                Log.d(TAG,"action = action_heart");
                //onclick: toggle heartbutton in memoFrag + insert hearttoggle to mongo
                if (playingStateChangeListener != null) playingStateChangeListener.onToggleHeart(); //MSC onCustomAction
                //onclick: toggle heartbutton in notific
                if (isPlaying) buildNotification(remainingTimeFormatted, "play", 1f); //MSC onCustomAction
                else buildNotification(remainingTimeFormatted, "pause", 0f); //MSC onCustomAction
            }

        }

        @Override
        public void onSetRating(RatingCompat rating, Bundle extras) {
            super.onSetRating(rating, extras);
            Log.d(TAG,"MSC onCustomAction, rating:" + rating + " , extras:" + extras);
            /*RatingCompat heartRating = null;
            if (rating.isRated()) { //wenn Herz ausgewählt
                // Handle the case where the user rated the content as a favorite
                heartRating = RatingCompat.newHeartRating(true);
            } else {
                // Handle the case where the user rated the content as not a favorite
                heartRating = RatingCompat.newHeartRating(false);
            }*/ //idk; alt, | so !)´^
            //2 an memoFrag ferren: ui + mongo
            if (playingStateChangeListener != null) playingStateChangeListener.onToggleHeart(); //MSC onSetRating //rating.isRated() //TODO (4.12. neu)
        }
    };




    //1 onCreate
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"onCreate im PlayingService ALOHA!");



    }//ENDE onCreate__-------------------------------------------------------------------------------------------------------------------------------------------

//2 Binder
    private final IBinder binder = new LocalBinder(); //"binder" is grey: UNSINN, KAPPES, QUATSCHO, KOKOLORES the binder field serves its purpose in establishing the connection NOT ANYMORE THANKS chatGPT FOR SOLVING YOUR OWN MISTAKES


    // between the service and the client component, even if it is not directly used within the service class itself.
    public class LocalBinder extends Binder {//Using local binder is common approach to establish connection between service and client component like activity
        public PlayingService getService() {
            return PlayingService.this;
        }
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "binder ist: " + binder);
        //mediaSession = new MediaSessionCompat(getApplicationContext(), "memo"); //yt tut
        initMediaSession(); //github/ /TODO (playingService) (mediaSession) wird nur 1 mal gecoalt, oder?
        return binder; //DER HAT HIER NULL RETURNT FÜR JAHRE, WIESO WIESO WARUM WIESO ? warum chatGPT trollst du mich und frisist meine kostbare unkostbare ZETI!°?!??!"§`?Q29dkls
    }

    @androidx.annotation.Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @androidx.annotation.Nullable Bundle rootHints) {
        // Return the root of your media content hierarchy
        return new BrowserRoot("memoid", null);
        //return null would deny access to media content ,sais chat
    } //belongs to MediaBrowserServiceCompat

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        // Load the media items under the given parent id
        // Populate the result with the list of MediaBrowserCompat.MediaItem

    } //belongs to MediaBrowserServiceCompat

    private void initMediaSession() {
        ComponentName mediaButtonReceiver = new ComponentName(getApplicationContext(), MediaButtonReceiver.class); //NotifBrotcastReceiver.class
        //TODO (weiter) MediaButtonReceiver.class am 30.11 rausgenommen, weil glaube, das führt dazu, dass heartbtn nicht vorkommt
        mediaSession = new MediaSessionCompat(getApplicationContext(), "mediaSession", mediaButtonReceiver, null);
        //mediaSession.setRatingType(RatingCompat.RATING_HEART); //23.12. glaube unnötig :D
        mediaSession.setCallback(mediaSessionCallback);
        //mediaSession.setFlags( MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS );
        //flag depend. on version
        flag = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ? PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT;

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setClass(this, MediaButtonReceiver.class); //NotifBrotcastReceiver.class
        //TODO (weiter) MediaButtonReceiver.class am 30.11 rausgenommen, weil glaube, das führt dazu, dass heartbtn nicht vorkommt
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, flag);
        mediaSession.setMediaButtonReceiver(pendingIntent); //allow restarting playback after the session has been stopped

        setSessionToken(mediaSession.getSessionToken());

        // Assuming mediaSessionCompat is your MediaSessionCompat instance
        mediaController = new MediaControllerCompat(this, mediaSession.getSessionToken());
        mediaController.registerCallback(mediaControllerCallback);
    }



//3 interface
    //callback interface to notify the UI in the Publish-activity when PLAYING states have changed:
    public interface PlayingStateChangeListener {
        void onPlayingStarted();
        void onPlayingPaused();
        void onPlayingResumed();
        void onPlayingFinished();
        void onToggleHeart(); //boolean heart

    //seekbar
        void onPlayingStateChanged(boolean isPlaying);

        void onTimerTick(String remainingTimeFormatted);
    } //put in extra class, so NotifBrotcastReceiver can also use interface
    public void setPlayingStateChangeListener(PlayingStateChangeListener listener) {
        playingStateChangeListener = listener;
    }


//3 Lifecycle of service (start & stop)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG,"onStartCommand called (in service)");
        if (intent != null) {
            Log.d(TAG,"intent != null");
            url = intent.getStringExtra("url");

            //player konfigurieren
            player = new MediaPlayer();
            try {
                player.setDataSource(url); //data source ist url des audios..
                player.prepareAsync(); //prepare the MediaPlayer asynchronously (bc. url)
            } catch (IOException e) {
                Log.e(TAG, "exception when trying to setdatasource + prepare: " + e);
                e.printStackTrace();
            }
            Log.d(TAG,"player configured and is: " + player);

            //TODO (debug)
            //idee: player hier starten, damit er schon mal gestartet wurde und dann aber direkt pausieren, damit beim nächsten call .start nicht IMMER von 0 gestartet wird
            player.setOnPreparedListener(mp -> {
                //Log.d(TAG,"player started and instantly paused for SNEAKY solution heheheee");
                //(info) warum sneaky solution necessario? ➝ wenn geseekt wurde bevor das audio gestartet wurde, wurde der player zwar geseekt, aber wenn ich jetzt das audio starten würde, dann würde der player wieder auf pos 0 springen
                player.start();
                player.pause();
                //seekbar initialiez
                MemoFragment.setupSeekBar(); //TODO (player prepare) ich hoffe das prepareAsync geht schnell genug, sodass Utente seekBar nicht zu früh moven cann
            });
        }
        //Log.d(TAG,"onStartCommand END (in service)");
        return START_REDELIVER_INTENT; //or START_STICKY
        //those both: If the service is killed by the system, it will be restarted and the last intent passed to it will be re-delivered via onStartCommand()
        //If you want to ensure that the service is restarted with the same intent, use START_REDELIVER_INTENT.
        //If you don't require the intent or need a fresh start, you can use START_STICKY.
        //chatGPT advised me to put the if (intent != null) clause in the beginning of onStartCommand to ensure it isn't called multiple times
    }

    @Override
    public void onDestroy() { //TODO (B) bei onDestroy könnte ich die Logik von stopPLAYING() reinhauen...   hab ich mal gemacht mal jetzt mal schauen
        Log.d(TAG,"onDestroy in service ausgelöst");
        // Stop and release the MediaRecorder when the service is destroyed
        if (player != null) {
            finishPlaying(); //onDestroy
        }
        if (timer != null) { //this should prevent timer-npe when playignservice destroys
            timer.cancel();
            timer.purge();
        }
     /*   stopForeground(true); //onDestroy itself wouldn't suffice to close the service (bc. its foregroudn), those both have to be called
        stopSelf(); //stop the survice itself */
        super.onDestroy();
    }














/**
 * onPLAY, STARTPLAYING, PAUSEPLAYING & STOPPLAYING
 * **/

    public void onPlay() { //(info) public gemacht, weil es sonst mit dem fragment nicht läuft
        if (isPlaying == false) {
            Log.d(TAG,"onPlay ➝ resumePlaying");
            resumePlaying(); //onPlay
            buildNotification(remainingTimeFormatted, "play", 1f); //onPlay
        } else {
            Log.d(TAG,"onPlay ➝ pausePlaying");
            pausePlaying(); //onPlay
            buildNotification(remainingTimeFormatted, "pause", 0f); //onPlay
        }
    }

//3 pause, resume, stop and start&createaudiofile PLAYING
    void pausePlaying() {
        Log.d(TAG, """
                pausePlaying
                isPlaying set to false
                playingStateChangeListener.onPlayingPaused()
                timer callbacks removed""");
        //pause player
        player.pause();
        isPlaying = false;
        //callback für UI change (recordbutton background change)
        if (playingStateChangeListener != null) playingStateChangeListener.onPlayingPaused();
        //timer
        handler.removeCallbacks(updateRunnable); //pausePlaying()
        //handler.removeCallbacksAndMessages(null);
        //Log.d(TAG,"pausePlaying END (service)");
    }

    //TODO (A) adjusting the resumeplaying logic ? maybe...
    // Starts or resumes playback. If playback had previously been paused, playback will continue from where it was paused. If
    // playback had been stopped, or never started before, playback will start at the beginning.

    void resumePlaying() { //könnte ich auch direkt ins else{} von startPLAYING reinbagge, aber wer weiß, was noch hinzukommen möge zu resumePLAYING
        Log.d(TAG, """
                resumePlaying
                isPlaying set to true
                playingStateChangeListener.onPlayingResumed()
                handler callbacks started""");
        player.start();
        isPlaying = true;
        //callback für UI change (recordbutton background change)
        if (playingStateChangeListener != null) playingStateChangeListener.onPlayingResumed();

        //timer
        //TODO (C) das hier sollte eigentlich genau zum gl. Zeitpkt passieren wie das recorder.resume(); naja xdrolflolmao
        handler.post(updateRunnable); //resumePlaying()
        //Log.d(TAG,"resumePlaying END (service)");
    }

    void finishPlaying() {
        Log.d(TAG, """
                stopPlaying
                player nulled
                isPlaying & playbackHasBeenStarted reset
                timer callbacks removed""");

        //annihilate the recorder!!!
        player.stop();
        player.release();
        //annihilate the timer!!!
        //handler.removeCallbacksAndMessages(null);
        //isTimerPaused = false;
        //remainingTime = audioduration; //stimt das??? nein, weil es wird erstmal bei 00:00 bleiben - BIS Utente playpausebtn erneut presst, dann wäre remainingTime = audioduration ein true argument of life in sideways
        //reset the player!!!
        player = null;
        isPlaying = false;
        remainingTimeFormatted = "00:00";
        if (playingStateChangeListener != null) playingStateChangeListener.onPlayingFinished();

        //timer
        if (handler != null) handler.removeCallbacks(updateRunnable); //pausePlaying()

        //neuer boolean (9.10.23) für seekBar, auch zurücksetzen bei onStop
        //playbackHasBeenStarted = false;


        // Dateinamen der zuletzt aufgenommenen Audiodatei speichern
        //lastRecordedAudioName = getLastModifiedFile(); //TODO (A) ich glaube, das muss hier rein, um den lastRecordedFileName zu updaten... idk marc was hier dein plan war
        //Log.d(TAG,"stopPlaying END (service)");
    }




    public void initiatePlaying() { //or rather "initiatePlaying" - Pendant zu startPLAYINGAndCreateAudioFile
        Log.d(TAG,"startPlaying called" +
                "\nplayer is at position: " + player.getCurrentPosition());

        //1 start player
        player.start();

        //2 set onCompletionListener
        player.setOnCompletionListener(mp -> {
            //"playbackHasBeenStarted = false" +
            Log.d(TAG, """
                    Playback completed
                    playingStateChangeListener.onPlayingFinished()
                    handler callbacks removed
                    remainingTime set to 0"""); //TODO (player) (timer) stimmt ds?
            //pause
            pausePlaying(); //onCompletionListener
            remainingTime = 0; //müsste eh schon = 0 sein (handler <3 runnable)
            //TODO (weiter) 29.11. remainingTime en général ma abschegge, bin ma ned sicha
            updateRemainingtimetvAndNotification(remainingTime); //onCompletionListener
            //damit der notific button weiß, dass er das audio von vorn starten soll
            //playbackcompleted = true; //onCompletionListener


//TODO (weiter) 29.11. wenn FINISh, dann soll der playpausebtn pause sein & ein click auf ihn soll shallStartPausePlayingTriggerOnPlay NICHT = true setzen, weil das würde den gesamten Player neu preparen + notific initiieren unnötig,
// ➝ also einfach seekTo pos 0 wenn playpausebtn DIREKT danach gedrückt wird!
// wenn nicht direkt danach (seekBar bewegt ODER rewind taste gedrückt), dann normales behaviour
// ODER :::: android notific playpausebtn springt automatisch zum anfang zurück JA TUT ES !!!! LOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO:)OOOOOOOOOOOOOOOOOOOOOOOOOOOOL

        });

        //3 setup seekBar
        //MemoFragment.setupSeekBar("afterPlayingServiceIsPrepared"); //(info) jetzt in prepareAsync listener drin...

        //4 booleans anpassen
        isPlaying = true;
        //isTimerPaused = false;

        //5 callback für UI change (playpausebutton background change)
        //Log.d(TAG,"playingStateChangeListener != null, also wird callback eingerichtet");
        if (playingStateChangeListener != null)  playingStateChangeListener.onPlayingStarted(); //startPlaying


        //6 ticker für anzeige v. remaining duration
        //Update UI using the duration
        //updateRemainingtimetvAndNotification(audioduration); //bereits passiert im tv .. beim initiieren v. memoFrag
        // Schedule a runnable to update UI every second
        handler = new Handler(Looper.getMainLooper()); //handler = new Handler(); deprecated //TODO (weiter) (prüfen) 29.11.
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                int currentPosition = player.getCurrentPosition();
                remainingTime = player.getDuration() - currentPosition;
                //Update UI with remaining time
                updateRemainingtimetvAndNotification(remainingTime); //runnable+handler=<3

                //Repeat this runnable every second
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(updateRunnable); //initiatePlaying()

        //Log.d(TAG,"startPlaying END (in service)");
    }

//In your PlayingService, you'll need to create methods to handle seeking within the audio file that CAN BE ACCESSED BY MemoFrag!!
    public void seekTo(int position) {
        player.seekTo(position);
        Log.d(TAG, "player seeked to: " + player.getCurrentPosition());
    }

    public int getCurrentPosition() {
        return player.getCurrentPosition();
    }

    public int getDuration() {
        return player.getDuration();
    }

    public void jumpPlayer(String mode) {
        int currentPosition = player.getCurrentPosition();
        int duration = player.getDuration();
        int newPosition;

        if (mode.equals("fastforward")) {
            newPosition = Math.min(currentPosition + 10000, duration);  //ensure newPosition is not > duration
            //wenn ende erreicht
            if (newPosition == duration) isPlaying = false; //hierdurch wird in updateRemainingtimetvAndNotification "pause" an notific übergebe
        } else { //"rewind"
            newPosition = Math.max(currentPosition - 10000, 0);  //ensure newPosition is not < 0
        }
        player.seekTo(newPosition);

        //3 timer updaten mit remainingTime
        remainingTime = duration - newPosition;
        Log.d(TAG, "jumpPlayer: " + mode +
                "\ncurrent pos:" + currentPosition +
                "\nnew pos:" + newPosition +
                "\ntotal duration:" + duration +
                "\nremaining duration:" + remainingTime);
        updateRemainingtimetvAndNotification(remainingTime); //jumpPlayer

           /*//2 wenn weniger als 10 sek bis zum ende des audios fehlen:
        Log.d(TAG, "current position:" + currentPosition +
                "\ntotal duration:" + duration);
        if (currentPosition + 10000 > duration) {
            int remainingtimeuntilend = duration - currentPosition;
            playedTime += remainingtimeuntilend;
        } else {
            playedTime = playedTime + 10000;
        }*/ //idk playedTime oder so, aber wozu marc hääää 29.11. xDDD was soll die schaloschen bruda
    }


    //4 timer functionality
    private void updateRemainingtimetvAndNotification(long remainingTime) {
        Log.d(TAG,"updateRemainingtimetvAndNotification"); // called\n" +
        //"remainingTime ist: " + remainingTime);

        //1 Update the UI with the recorded time (in s)
        long remainingSeconds = remainingTime / 1000;
        //chatGPT1/2: Format/Update the recorded time
        long hours = remainingSeconds / 3600;
        if (hours < 1) {
            remainingTimeFormatted = String.format(Locale.getDefault(), "%02d:%02d", remainingSeconds / 60, remainingSeconds % 60);
        } else { //^= mind. 1h
            remainingTimeFormatted = String.format(Locale.getDefault(), "%02d:%02d:%02d", remainingSeconds / 3600, remainingSeconds / 60, remainingSeconds % 60);
        }

        //2 Update the foreground notification with the remaining time every second
        //updateNotification(remainingTimeFormatted);
        //TODO (notification) (player) prüfen ob das mit build notification so geht!
        if (isPlaying) buildNotification(remainingTimeFormatted, "play", 1f); //update
        else buildNotification(remainingTimeFormatted, "pause", 0f); //update

        //3 Update the textview in MemoFrag with the remaining time
        if (playingStateChangeListener != null)  playingStateChangeListener.onTimerTick(remainingTimeFormatted); //TODO (playingStateChangeListener) hoffe, der ist nie null, wenn er nicht null sein sollte..

        //Log.d(TAG,"updateRemainingtimetvAndNotification END (in service)");
    }








//foreground 2
    //initial creation of the notific channel:
    public void initiateNotification() {
        Log.d(TAG,"initiateNotification");
        //foreground id 2 (player)
        //chatGPT: By calling startForeground() with notification, service will run in the foreground and display the notification to the user
        createNotificationChannel();
        // Build the notification for your foreground service
        //vorher stand das hier zu Beginn als initialization und in dieser method stand nur return foregroundNotification;
        // Start the service as a foreground service with the notification
        //startForeground(2, foregroundNotification);
        buildNotification(remainingTimeFormatted, "play", 1f); //initiateNotif  //TODO 3.1. nötig?wird 2x aufgerufen hintereinander
        //Log.d(TAG,"initiateNotification END");
    }
    public void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Notification Channel Playing",
                NotificationManager.IMPORTANCE_LOW //importance_low oder _min to avoid vibrating
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }
    //build the fg notific
    public void buildNotification(String remainingtimeformatted, String mode, float playbackSpeed) {//speed x2 possibility!
        //TODO (future) (notific) float playbackSpeed BEHALTEN ERSTMAL, falls ich speed x2 einbauen will oder so!!!
        Log.d(TAG,"buildNotification" +
        "\nmode:" + mode + " , remaintime:" + remainingtimeformatted + " , playspeed:" + playbackSpeed);
        //1
        //flag depend. on version //already called in onBind
        //flag = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ? PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT;

        //1.1 intent open memoFrag (when clicked on anywhere but the playbtn o. seekbar)
        Intent memoFragIntent = new Intent(this, MemoFragment.class);
        PendingIntent memoFragPendingIntent = PendingIntent.getActivity(this, 0, memoFragIntent, flag);
        //1.2 intent rating
        //... in extra method for it


        //1.2 intent to play/pause //actionPlayPause vorherig
        //Intent playIntent = new Intent(getApplicationContext(), NotifBrotcastReceiver.class).setAction("play");
        //PendingIntent playPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 88888, playIntent, flag);

        //Bitmap artwork = BitmapFactory.decodeResource(getResources(), R.drawable.lotti); //codinginflow //TODO (future) (memo pic)
        //Bitmap artwork = (imgArt != null) //yt tut
        //        ? BitmapFactory.decodeByteArray(imgArt, 0, imgArt.length) //yt tut
        //        : BitmapFactory.decodeResource(getResources(), R.drawable.music_player_icon_slash_screen); //yt tut

        //TODO (notific) 28.11: remainingduration + like button RAN schavve!

        // Customize the notification as per your requirements
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentIntent(memoFragPendingIntent)
                .setContentTitle(MemoFragment.title)
                .setContentText(MemoFragment.creator)
                .setSubText(remainingtimeformatted) //TODO 3.1. (notific) not displayed... WHYHYYYY
                //.addAction(createPlaypauseAction(mode)) //play \ poas //actionPlayPause vorherig
                //.addAction(createHeartAction(false)) //TODO (notific) false WÄGG, MemoFragment.heart HINN //30.11.
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        //.setShowActionsInCompactView(0, 1)
                        .setMediaSession(mediaSession.getSessionToken()))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.headphone2) //R.mipmap.ic_launcher TODO (notific) fg-icon zu R.mipmap.ic_launcher ändern
                //TODO (mongo) in database & co. "heart" fields includen :D
                //.setLargeIcon(artwork) //TODO (future) (notific) (memo pic)
                .build();
                //TODO (C) (notific) auf notific gedrückt halten, um sie zu entfernen lol . geht bei spotify auch xD
        //Log.d(TAG,"notification: " + notification);

        // Call sendCustomAction when the heart button is clicked to update notification TODO (notific) 30.11. where to put | even necessario ??????
        mediaController.getTransportControls().sendCustomAction("action_heart", null); //TODO (weiter) 30.11. was geht dgga wo callt man das ???? ka


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { //TODO (29.11.) unnötiger if-clause, Version ist ws immer drüber
            //float playbackSpeed = (isPlaying) ? 1F : 0F;

            mediaSession.setMetadata(new MediaMetadataCompat.Builder().putLong(MediaMetadataCompat.METADATA_KEY_DURATION, player.getDuration()).build());

            //heart button versuch 10000000: playbackStateBuilder.addCustomAction
            //wenn buildNotification zum 1. mal: dann nimm 'heart' boolean aus model
            int iconRes = hearto ? R.drawable.heart_white : R.drawable.heart_white_unfilled; //TODO (notific) 30.11. false WÄGG, MemoFragment.heart HINN //30.11.
            PlaybackStateCompat.CustomAction heartAction = new PlaybackStateCompat.CustomAction.Builder(
                    "action_heart","heart", iconRes).build();
            //TODO (weiter 30.11.) how to pass extras -> boolean heart over this action? i can give extras it sais in onCustomAction ?

            int STATE = mode.equals("play") ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;
            PlaybackStateCompat.Builder playbackStateBuilder = new PlaybackStateCompat.Builder()
                        .setState(STATE, player.getCurrentPosition(), playbackSpeed)
                        .setActions(PlaybackStateCompat.ACTION_SEEK_TO  //bitmap | so
                                | PlaybackStateCompat.ACTION_PLAY_PAUSE)
   //TODO 3.1. entfernt       //| PlaybackStateCompat.ACTION_SET_RATING //ACTION_SET_RATING call here doesn't SHOW it, only "activates" TODO ?
                        .addCustomAction(heartAction);
            PlaybackStateCompat playbackState = playbackStateBuilder.build();
            Log.d(TAG,"bN: playbackState set to "+mode);


            //Log.d(TAG,"playbackState: " + playbackState);
            mediaSession.setPlaybackState(playbackState);

            mediaSession.setCallback(mediaSessionCallback);
        } //yt tut

        //Log.d(TAG,"buildNotification END");
        // Return the built notification
        //return notification.build(); /&/old
        //NotificationManagerCompat.from(PlayingService.this).notify(2, builder.build()); //github
        startForeground(2, notification);
    }

    /*private NotificationCompat.Action createPlaypauseAction(String mode) {
        //playpausebtn in notific
        //(info) ich glaube, das hier ist für ältere Versionen, neue Versionen machen's nur mit PlaybackStateCompat
        //(info2) wenn dies für ältere Versionen ausgelegt ist, dann muss ich PlaybackStateCompat ws weghauen & mit PendingIntent + NotifBrotcastReceiver arbeiten :D
        int iconRes = mode.equals("play") ? R.drawable.pausebutton : R.drawable.playbutton;
        Intent playpauseIntent = new Intent("action_playpause");
        playpauseIntent.putExtra("mode", mode);
        PendingIntent pendingPlaypauseIntent = PendingIntent.getBroadcast(
                this, 1, playpauseIntent, flag); //NotifBrotcastReceiver triggers when this is called
        return new NotificationCompat.Action.Builder(iconRes, "playpause", pendingPlaypauseIntent).build();
    }

    private NotificationCompat.Action createHeartAction(boolean heart) {
        //an diese method wird übergeben, ob rated (heart) oder unrated (heart_unfilled) & darauf aufbauend notific gebaut
        int iconRes = heart ? R.drawable.heart_white : R.drawable.heart_white_unfilled;
        Intent heartIntent = new Intent("action_heart");
        heartIntent.putExtra("heart", heart);
        PendingIntent pendingHeartIntent = PendingIntent.getBroadcast(
                this, 7, heartIntent, flag); //NotifBrotcastReceiver triggers when this is called
        return new NotificationCompat.Action.Builder(iconRes, "heart", pendingHeartIntent).build();
    }*/ //old 2 NotificationCompat.Actions
    /*//wird aus NotifBrotcastReceiver gecallt, und sendet von hier aus übers interface an memoFrag die ui Änderungswünsche xD
    public void toggleHeart(boolean heart) {
        Log.d(TAG,"Zwischenschritt über toggleHeart: " + heart);
        playingStateChangeListener.onToggleHeart(); //Zwischenschritt über toggleHeart //TODO (4.12. neu)
    }*/ //4.12. rausgenommen wird aus NotifBrotcastReceiver gecallt, und sendet von hier aus übers interface an memoFrag die ui Änderungswünsche xD



    public MediaPlayer getMediaPlayer() {
        return player;
    }



}//ENDUS_____PENDUS________________OHA_WER_KOMMT_DENN_DAIST_DAS_ETWA_DER_STÖR_MÖR_______kdsksdkfldjalfökdspppppppp.l:)
