package com.example.remember.publish;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NotifBrotcastReceiver extends BroadcastReceiver {
    //this class is used by RecordingService (not by PlayingService)
    private static final String TAG = "NotifBrotcastReceiver";
    //public static PlayingService playingservice;
    //public static RecordingService recordingService;
    //public static PlayingService.PlayingStateChangeListener playingstatechangelistener;
    //MediaPlayer mediaPlayer;

    // Add a zero-argument constructor
    public NotifBrotcastReceiver() {
//surpr. bomba BOOM kongo is down
    }

    //wäre benötigt worden, wenn ich die pending intent buttons custom gemacht hätte. dann wäre der button stuff hier
    // ... aber ich hab das von Android vorgegebene framework genommen mit Media bla Receiver bla Broadcast blallblalaba
    /*public NotifBrotcastReceiver(RecordingService recordingservice) {
        recordingService = recordingservice;
    }*/ //3.1. ciao brutto

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG,"onReceive");
        if (intent != null && intent.getAction() != null) {
            // Only play next or prev song when the music list contains more than one song
            //case ApplicationClass.PREVIOUS: //yt tut
            //    if (PlayerActivity.musicListPA.size() > 1) prevNextSong(false, context);
            //    break;
            /*if (intent.getAction().equals("action_heart")) {
                Log.d(TAG,"onReceive action_heart");
                //this is triggered in buildNotification -createRatingAction; HIER wird
                //1 entweder notific initially gebaut & rating button action miteinbezogen
                //2 rating button action click managed
                Bundle extras = intent.getExtras();
                boolean heart = false;
                if (extras != null) {
                    if (extras.containsKey("heart")) {
                        heart = extras.getBoolean("heart");
                        Log.d(TAG,"heart:" + heart);
                    }
                }
                //umweg über playingService, der dann über memoFrag interface ui + mongo change veranlasst
                playingservice.toggleHeart(heart); //30.11.
            }*/ //TODO (notific) 3.12. kannweg? stattdessen macht das PlaybackStateOnCustomAction...

            /*
            if (intent.getAction().equals("action_playpause")) {
                Log.d(TAG,"onReceive action_playpause");
                if (PlayingService.isPlaying)
                    playingservice.mediaSessionCallback.onPause();
                    //pauseMusic();
                else
                    playingservice.mediaSessionCallback.onPlay();
                //playMusic();
                //case ApplicationClass.NEXT:
                // if (PlayerActivity.musicListPA.size() > 1) prevNextSong(true, context);
                //   break;
                //case "exit" -> exitApplication();
            } //old, can go probably (TODO 30.11. prüfen, ob der NtoifcBrotcastReceir nciht doch auslöst aufs play intent..)
            */ //TODO 3.1. weggehaun, ist ja nicht genutzt in playingservice weil keine custom notific

            if (intent.getAction().equals("action_recordpause")) {
                Log.d(TAG,"onReceive action_recordpause");
                // Retrieve the instance of the RecordingService
                RecordingService recordingService = RecordingService.getInstance();
                if (recordingService != null) {
                    recordingService.onRecord();
                }
            }//TODO (weiter) 23.12. from the broadcast receiver, how can i make the recordbutton toggle background ?



        }
    }

    /*private void playMusic() {
        Log.d(TAG,"playMusic");
        PlayingService.isPlaying = true;
        mediaPlayer = playingservice.getMediaPlayer();
        playingstatechangelistener = playingservice.playingStateChangeListener;
        if (mediaPlayer != null) {
            // Now you can use mediaPlayer
            mediaPlayer.start();
        }

        //this class is called from buildNotification lol
        //playingservice.buildNotification("idk", "play", 1f); //play (click in notif)

        playingstatechangelistener.onPlayingPaused();
    }

    private void pauseMusic() {
        Log.d(TAG,"pauseMusic");
        PlayingService.isPlaying = false;
        mediaPlayer = playingservice.getMediaPlayer();
        playingstatechangelistener = playingservice.playingStateChangeListener;
        if (mediaPlayer != null) {
            // Now you can use mediaPlayer
            mediaPlayer.pause();
        }

        //this class is called from buildNotification lol
        //playingservice.buildNotification("idk", "pause", 0f); //pause (click in notif)

        playingstatechangelistener.onPlayingStarted();
    }*/ //TODO 3.1. weggehaun, ist ja nicht genutzt in playingservice weil keine custom notific

    /* private void prevNextSong(boolean increment, Context context) {
        setSongPosition(increment);
        PlayerActivity.musicService.createMediaPlayer();
        Glide.with(context)
                .load(PlayerActivity.musicListPA.get(PlayerActivity.songPosition).getArtUri())
                .apply(RequestOptions.placeholderOf(R.drawable.music_player_icon_slash_screen).centerCrop())
                .into(PlayerActivity.binding.songImgPA);
        PlayerActivity.binding.songNamePA.setText(PlayerActivity.musicListPA.get(PlayerActivity.songPosition).getTitle());
        Glide.with(context)
                .load(PlayerActivity.musicListPA.get(PlayerActivity.songPosition).getArtUri())
                .apply(RequestOptions.placeholderOf(R.drawable.music_player_icon_slash_screen).centerCrop())
                .into(NowPlaying.binding.songImgNP);
        NowPlaying.binding.songNameNP.setText(PlayerActivity.musicListPA.get(PlayerActivity.songPosition).getTitle());
        playMusic();
        PlayerActivity.fIndex = favouriteChecker(PlayerActivity.musicListPA.get(PlayerActivity.songPosition).getId());
        if (PlayerActivity.isFavourite) {
            PlayerActivity.binding.favouriteBtnPA.setImageResource(R.drawable.favourite_icon);
        } else {
            PlayerActivity.binding.favouriteBtnPA.setImageResource(R.drawable.favourite_empty_icon);
        }
    }*/ //don't need and want that prevNextSong
}
