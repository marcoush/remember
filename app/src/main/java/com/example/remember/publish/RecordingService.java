package com.example.remember.publish;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.remember.R;
import com.example.remember.ui.memo.MemoFragment;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.annotation.Nullable;

public class RecordingService extends Service {
    private static final String TAG = "RecordingService";
    //ich brauche diesen foreground service leider, damit man auch bei geschlossener activity weiter aufnehmen kann (weil der service läuft weiter im hintergrund, und die activity kann das nicht)

//basic MediaRecorder stuff
    private MediaRecorder recorder = null;
    boolean isRecording = false;
    boolean isPaused = false; //wenn ich oben in der class schreibe: boolean isPaused; -> dann hat (standardmäßig) den Wert "false"

//initiations for the pending status (that audio file is only seen in smarpthone stoarge when completed)
    ContentResolver resolver;
    ContentValues audioDetails;
    Uri audioContentUri;

    //für NotifBrotcastReceiver... damit der ne instance erhält weil über pending Intent geht iwie ned
    private static RecordingService instance;


    //TODO (B) es soll das recording im idealfall sogar oben in der benachrichtigungsleiste angezeigt werden

//5 duration timer für MediaRecorder
    CountDownTimer countDownTimer;
    long startTime;
    long recordedTime;
    private boolean isCountDownPaused = false;
    //TODO (C) die aktuelle aufnahmensekundenzahl kann am ende aus dem edittext oder aus dem long ausgelesen werden xD für die metadatem
//startForeground stuff
    private static final String CHANNEL_ID = "NOTIFCHANNEL_RECORDING";
    //private Notification foregroundNotification;
//callback interface to notify the UI in the Publish-activity when recording states have changed:
    private RecordingStateChangeListener recordingStateChangeListener;
    private String recordedTimeFormatted = "00:00"; // Initialize with an empty string

    //notification
    private int flag; //for the pendingIntents


    //1 onCreate
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.d(TAG,"onCreate im RecordingService HELLO!");






    }//ENDE onCreate__-------------------------------------------------------------------------------------------------------------------------------------------




//2 Binder
    private final IBinder binder = new LocalBinder(); //"binder" is grey: UNSINN, KAPPES, QUATSCHO, KOKOLORES the binder field serves its purpose in establishing the connection NOT ANYMORE THANKS chatGPT FOR SOLVING YOUR OWN MISTAKES
    // between the service and the client component, even if it is not directly used within the service class itself.
    public class LocalBinder extends Binder {//Using local binder is common approach to establish connection between service and client component like activity
        public RecordingService getService() {
            return RecordingService.this;
        }
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "binder ist: " + binder);
        flag = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ? PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT;
        return binder; //DER HAT HIER NULL RETURNT FÜR JAHRE, WIESO WIESO WARUM WIESO ? warum chatGPT trollst du mich und frisist meine kostbare unkostbare ZETI!°?!??!"§`?Q29dkls
    }

//2 interface
    //callback interface to notify the UI in the Publish-activity when recording states have changed:
    public interface RecordingStateChangeListener {
        void onRecordingStarted();
        void onRecordingPaused();
        void onRecordingResumed();
        void onRecordingStopped();
        void onCountDownTick(String recordedTimeFormatted);
    }
    public void setRecordingStateChangeListener(RecordingStateChangeListener listener) {
        recordingStateChangeListener = listener;
    }
    public void removeRecordingStateChangeListener() {
        recordingStateChangeListener = null;
    }




//3 Lifecycle of service (start & stop)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG,"onStartCommand in service gestartet");
        if (intent != null) {
            // initiate and start the MediaRecorder when the service is initiated
            Log.d(TAG,"intent != null,  (in onStartCommand, service)");
            //TODO (A) bind here

        }
        Log.d(TAG,"Ende onStartCommand (in: service)");
        return START_REDELIVER_INTENT; //or START_STICKY
        //those both: If the service is killed by the system, it will be restarted and the last intent passed to it will be re-delivered via onStartCommand()
        //If you want to ensure that the service is restarted with the same intent, use START_REDELIVER_INTENT.
        //If you don't require the intent or need a fresh start, you can use START_STICKY.
        //chatGPT advised me to put the if (intent != null) clause in the beginning of onStartCommand to ensure it isn't called multiple times
    }

    @Override
    public void onDestroy() { //TODO (B) bei onDestroy könnte ich die Logik von stopRecording() reinhauen...   hab ich mal gemacht mal jetzt mal schauen
        Log.d(TAG,"onDestroy in service ausgelöst");
        // Stop and release the MediaRecorder when the service is destroyed
        if (recorder != null) {
            stopRecording(); //onDestroy greift auf stopRecording zu
        }

        super.onDestroy();
    }














/**
 * onRecord, STARTRECORDING, PAUSERECORDING & STOPRECORDING
 * **/

    void onRecord() {
        if (isRecording == false) {
            Log.d(TAG,"onRecord ➝ resumeRecording");
            resumeRecording();
            buildNotification(recordedTimeFormatted, "record");
        } else {
            Log.d(TAG,"onRecord ➝ pauseRecording");
            pauseRecording();
            buildNotification(recordedTimeFormatted, "pause");
        }
    }

//3 pause, resume, stop and start&createaudiofile recording
    void pauseRecording() {
        Log.d(TAG,"Start pauseRecording, service");
        recorder.pause();
        //callback für UI change (recordbutton background change)
        if (recordingStateChangeListener != null) recordingStateChangeListener.onRecordingPaused();

        if (countDownTimer != null) {
            countDownTimer.cancel();
            isCountDownPaused = true;
            //Log.d(TAG, "isCountDownPaused auf true gesetzt" + " (in: pauseRecording, service)");
        }
        isRecording = false;
        isPaused = true;
    }

    void resumeRecording() { //könnte ich auch direkt ins else{} von startRecording reinbagge, aber wer weiß, was noch hinzukommen möge zu resumeRecording
        Log.d(TAG,"Anfang resumeRecording, service");
        recorder.resume();
        //callback für UI change (recordbutton background change)
        if (recordingStateChangeListener != null) recordingStateChangeListener.onRecordingResumed();

        //TODO (C) das hier sollte eigentlich genau zum gl. Zeitpkt passieren wie das recorder.resume(); naja xdrolflolmao
        if (countDownTimer != null) { //TODO (C) nicht nötig der if-clause
            //Log.d(TAG,"isCountDownPaused müsste true sein, und ist: " + isCountDownPaused + " (in: resumeRecording, service)");
            countDownTimer.start();
            isCountDownPaused = false;
            //Log.d(TAG,"isCountDownPaused auf false gesetzt" + " (in: resumeRecording, service)");
            startTime = System.currentTimeMillis();
            //Log.d(TAG,"startTime auf die bereits gelaufene Zeit gesetzt <3, nämlich: " + startTime + " (in: resumeRecording, service)");
        }
        isRecording = true;
        isPaused = false;
    }

    void stopRecording() { //stop bedeutet in dem fall: zerstöre & lösche ihn komplett (dw: die Datei hingegen, die er erzeugt hat, ist schon gespeichert)
        //annihilate the recorder!!!
        recorder.stop();
        recorder.release();
        isRecording = false;
        isPaused = false;
        Log.d(TAG,"recorder genullt, released und isRecording & isPaused zurückgesetzt (in stopRecording, service)");
        //annihilate the countdowntimer!!!
        countDownTimer.cancel();
        isCountDownPaused = false;
        recordedTime = 0;
        startTime = 0;
        Log.d(TAG,"countdowntimer gecancelt & resettet (in: stopRecording, service)");
        //reset the recorder!!!
        recorder = null;

        // Dateinamen der zuletzt aufgenommenen Audiodatei speichern
        //lastRecordedAudioName = getLastModifiedFile(); //TODO (A) ich glaube, das muss hier rein, um den lastRecordedFileName zu updaten... idk marc was hier dein plan war
    }





    public void startRecordingAndCreateAudioFile(@NonNull Context context) {
        Log.d(TAG,"startRecordingAndCreateAudioFile ausgelöst (in: service)");
        // Add a media item that other apps don't see until the item is fully written to the media store.
        resolver = context.getContentResolver();

        // Find all audio files on the primary external storage device.
        Uri audioCollection;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            audioCollection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        } else {
            audioCollection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }
        //get the specific date to name the temporary audiofile according to that
        Date currentDate = new Date();//aktuelles datum einlesen
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());//(temporary recording, frontend) // Define the desired date format
        String formattedDate = dateFormat.format(currentDate);// Format the current date using the specified format
        String temporaryrecording = "memo " + formattedDate + ".mp3";
        Log.d(TAG,"temporaryrecording ist: " + temporaryrecording);

        audioDetails = new ContentValues();
        audioDetails.put(MediaStore.Audio.Media.DISPLAY_NAME, temporaryrecording); //audioDetails ist nur für den Pendingstatus... (?) nahh, hier wird ja der name für memory 23.04.2028 17:46.mp3 erschaffen, der ist ja auch sonst releevenat
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            audioDetails.put(MediaStore.Audio.Media.IS_PENDING, 1);
        }
        audioContentUri = resolver.insert(audioCollection, audioDetails);
        try {
            assert audioContentUri != null;
            try (ParcelFileDescriptor pfd = resolver.openFileDescriptor(audioContentUri, "w", null)) { // "w" for write.
                // Write data into the pending audio file.
                //recorder konfigurieren
                recorder = new MediaRecorder();
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                assert pfd != null;
                recorder.setOutputFile(pfd.getFileDescriptor());
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                try { //recorder preparen
                    recorder.prepare();
                } catch (IOException e) {
                    Log.e(TAG, "prepare() failed");
                } //recorder starten
                recorder.start();
                Log.d(TAG,"recorder initially gestartet (in: startRecordingAndCreateAudioFile, service)");
                //countdowntimer starten
                countDownTimer = new CountDownTimer(Long.MAX_VALUE, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        if (!isCountDownPaused) {
                            long currentTime = System.currentTimeMillis();
                            if (startTime != 0) {
                                recordedTime += (currentTime - startTime) / 1000;
                            }
                            startTime = currentTime;
                            Log.d(TAG,"recordedTime: " + recordedTime + "                  startTime: " + startTime);
                        }
                        // Update the UI with the recorded time
                        updateRecordedtimetvAndNotification(recordedTime);
                    }
                    @Override
                    public void onFinish() {
                        //(do something when the timer finishes ...it never finishes...)
                    }
                };
                countDownTimer.start();
                Log.d(TAG,"countDownTimer initially gestartet (in: startRecordingAndCreateAudioFile, service)");
                //booleans anpassen
                isRecording = true;
                isPaused = false;
                isCountDownPaused = false;
                //callback für UI change (recordbutton background change)
                if (recordingStateChangeListener != null) {
                    Log.d(TAG,"recordingStateChangeListener != null, also wird callback eingerichtet (in: startRecordingAndCreateAudioFile, service)");
                    recordingStateChangeListener.onRecordingStarted();
                }
                //capturing the current system time when the timer starts for get a reference point from which you can calculate the elapsed time accurately
                if (startTime == 0) {
                    startTime = System.currentTimeMillis(); //TODO(C) vor dem countdownticker? idk , es geht auch hier, also eig nicht nötig
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Log.d(TAG,"Uri der Aufnahme (lastRecordedAudioUri) ist: " + audioContentUri + " (Bericht aus startRecordingAndCreateAudioFile, service)"); //TODO (C) warum wird hier ein String _data ausgegeben? check i net
        // Nachdem Aufnahme gestartet und `audioContentUri` erhalten wurde
        //lastRecordedAudioUri = audioContentUri;
        Log.d(TAG,"Ende startRecordingAndCreateAudioFile (in service)");
    }



//4 countdowntimer functionality
    private void updateRecordedtimetvAndNotification(long recordedtime) {
        Log.d(TAG,"updateRecordedtimetvAndNotification"); // called\n" +
        //"recordedtime ist: " + recordedtime);

        //chatGPT1/2: Format/Update the recorded time
        long recordedseconds = recordedtime; // / 1000; .. TODO 23.12. (weiter) recording timer umstellen auch auf ms ? nicht 100% nötig, aber wäre consistenter
        long hours = recordedseconds / 3600;
        if (hours < 1) {
            recordedTimeFormatted = String.format(Locale.getDefault(), "%02d:%02d", recordedseconds / 60, recordedseconds % 60);
        } else { //^= mind. 1h
            recordedTimeFormatted = String.format(Locale.getDefault(), "%02d:%02d:%02d", recordedseconds / 3600, recordedseconds / 60, recordedseconds % 60);
        }

        //2 Update the foreground notification with the recorded time every second
        if (isRecording) buildNotification(recordedTimeFormatted, "record"); //update
        else buildNotification(recordedTimeFormatted, "pause"); //update

        //3 Update the textview in MemoFrag with the recorded time
        if (recordingStateChangeListener != null) recordingStateChangeListener.onCountDownTick(recordedTimeFormatted);

        //Log.d(TAG,"updateRemainingtimetvAndNotification END (in service)");
    }


    //diese method muss in RecordingService sein, weil sie den content resolver und pipaooüp einliest jajaja , ansonsten hätte ich sie gernst in Publish hineinbefördert
//pending status ist die ganze Zeit des Recordens über 0, damit die währenddessen aufgenommene Datei nicht im Speicher gesehen wird. erst, wenn der user das audio finalisiert und ...
//... zugleich im alertdialog bestätigt, dass er die datei aufm handy speichern will, wird das hier ausgelöst
    public void setPendingStatusToZeroAndMakeAudiofileVisibleInSmartphoneStorage(Runnable callback) {//Runnable callback to investigate if this code has been run through in order to do the subsequent logic (3 application cases) in Publish activit
        // Now that you're finished, release the "pending" status and let other apps play the audio track.
        if (audioDetails != null) { //TODO (info) das gab exception, wenn activity verlassen wurde, ohne dass was aufgenommen wurde penso -> if-clause in Publish stattdessen
            Log.d(TAG,"audioDetails != null (" + audioDetails + "), daher set pending status to zero:");
            audioDetails.clear();
            audioDetails.put(MediaStore.Audio.Media.IS_PENDING, 0);
            resolver.update(audioContentUri, audioDetails, null, null);
            // Execute the callback function
            if (callback != null) {
                callback.run();
            }
            Log.d(TAG,"audiofile uri ist: " + audioContentUri + "(in: setPendingStatusToZeroAndMakeAudiofileVisibleInSmartphoneStorage, Service)");
            Log.d(TAG,"audioDetails sind: " + audioDetails + "(in: setPendingStatusToZeroAndMakeAudiofileVisibleInSmartphoneStorage, Service)");
        }

    }













    /*private Notification buildNotification(String recordedTimeFormatted) {
        // Customize the notification as per your requirements
        Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.recordingactive))
                .setContentText(getString(R.string.recordedtime, recordedTimeFormatted))
                //.setSmallIcon(R.drawable.ic_notifications_black_24dp)
                ;
        // Return the built notification
        return builder.build();
    }

    private void updateNotification(String recordedTimeFormatted) {
        Notification notification = buildNotification(recordedTimeFormatted);
        startForeground(1, notification);
    }

    public Notification getForegroundNotification() {
        Notification foregroundNotification;//vorher stand das hier zu Beginn als initialization und in dieser method stand nur return foregroundNotification;
        //foreground 1
        //chatGPT: By calling startForeground() with notification, service will run in the foreground and display the notification to the user
        createNotificationChannel();
        // Build the notification for your foreground service
        foregroundNotification = buildNotification(recordedTimeFormatted);
        // Start the service as a foreground service with the notification
        startForeground(1, foregroundNotification);
        return foregroundNotification;
    }*/ //old buildNotification 23.12.



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
        buildNotification(recordedTimeFormatted, "pause"); //initiateNotif //TODO 3.1. nötig?wird 2x aufgerufen hintereinander
        //Log.d(TAG,"initiateNotification END");
    }
    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Notification Channel Recording",
                NotificationManager.IMPORTANCE_LOW //importance_low oder _min to avoid vibrating
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }
    //build the fg notific
    public void buildNotification(String recordedTimeFormatted, String mode) { //TODO 3.1. modes unnötig, weil in brotcastReceiver onRecord gecallt wird
        Log.d(TAG,"buildNotification" +
                "\nmode:" + mode + " , recordedtime:" + recordedTimeFormatted);//mode:record/pause
        //1
        //flag depend. on version //already called in onBind
        //flag = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ? PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT;

        RemoteViews notificationLayout = new RemoteViews(getPackageName(), R.layout.layout_recorder);
        // Customize your notification layout here
        //notificationLayout.setTextViewText(R.id.notificationText, "Recording in progress");


        //1.1 intent open memoFrag (when clicked on anywhere but the playbtn o. seekbar)
        Intent memoFragIntent = new Intent(this, MemoFragment.class);
        PendingIntent memoFragPendingIntent = PendingIntent.getActivity(this, 0, memoFragIntent, flag);

        // Set up the pending intent for the action button (start/pause)
        Intent actionIntent = new Intent(this, NotifBrotcastReceiver.class);
        actionIntent.setAction("action_recordpause"); // Replace with your action
        PendingIntent actionPendingIntent = PendingIntent.getBroadcast(this, 0, actionIntent, flag);
        notificationLayout.setOnClickPendingIntent(R.id.remoterecorderbuttonid, actionPendingIntent);
        //actionIntent.setComponent(new ComponentName(this, NotifBrotcastReceiver.class)); // Include the service component

        //duration einstellen
        notificationLayout.setTextViewText(R.id.remotedurationid, recordedTimeFormatted);
        if (mode.equals("pause")) notificationLayout.setImageViewResource(R.id.remoterecorderbuttonid, android.R.drawable.ic_media_play);
        else notificationLayout.setImageViewResource(R.id.remoterecorderbuttonid, android.R.drawable.ic_media_pause);

        // Customize the notification as per your requirements
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentIntent(memoFragPendingIntent)
                //.setContentTitle(MemoFragment.title)
                //.setContentText(MemoFragment.creator)
                .setCustomContentView(notificationLayout)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.mic) //R.mipmap.ic_launcher TODO (notific) fg-icon zu R.mipmap.ic_launcher ändern
                .build();
        //TODO (C) (notific) auf notific gedrückt halten, um sie zu entfernen lol . geht bei spotify auch xD
        //Log.d(TAG,"notification: " + notification);

        startForeground(1, notification);
    }




    public static RecordingService getInstance() {
        return instance;
    }

}//END___________________D___________________E_______END________________________N__________END____________
