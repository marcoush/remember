package com.example.remember;

import static com.example.remember.Main.username;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import java.util.Locale;
import java.util.Objects;

import io.realm.mongodb.App;
import io.realm.mongodb.AppConfiguration;
import io.realm.mongodb.User;

public class Settings extends AppCompatActivity {
    private static final String TAG = "SettINGS";

    Button screenmodebutton, applanguagebutton;
    /*   boolean nightMODE = false;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor sharedpreferenceseditor;*/

    private TextView currentusertextview;
    private ImageButton toolbarSettingsButton;
    private Button logoutbutton;
    //sp
    SharedPreferences languagePreferences;
    //mongo
    App app;
    User user;
    String usermail;


    //ich bin so ein vollhonk, ich kann doch die language einfach immer als System setting einstellen und gar keine Änderung erlauben, wer will denn überhaupt ändern lol!==???!!
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"onCreate Settings");

//0 Language
        //Get the languauage which is right now active and maybe change this activity's language to it (lang might have been changed in Settings that's why...)
        LanguageUtils languageUtils = new LanguageUtils();
        languageUtils.updateLanguage(this);
        //TODO (A) JA DENKE SCHON ich muss nur contentview hiernach CALLEN ERST XDDDDDDDDDDDDDDDDDDDDnoch etwas unsicher, obi ch das hier problemlso callen cann oder ob das irgendwie probleme bereitet, es gab 1 error im emulator z.B.
//M Content View
        setContentView(R.layout.activity_settings);
//1 UI
        // Set up the toolbar (kann man leider ned outsource , oder ich & chatGTP wissen nichtr wie herzliche grüße aus dem juni beginn)
        //ui & toolbar
        Toolbar toolbar = findViewById(R.id.toolbarid);
        TextView toolbarTitle = findViewById(R.id.toolbartitleid);
        toolbarSettingsButton = findViewById(R.id.toolbarsettingsid);
        ImageButton toolbarUploadButton = findViewById(R.id.toolbaruploadbuttonid);
        ImageButton toolbarRecordyourselfButton = findViewById(R.id.toolbarrecordyourselfbuttonid);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        // Set activity title
        String activityTitle = getString(R.string.settings);
        toolbarTitle.setText(activityTitle);
        // der settings knopf soll colored + disabled sein :) wird sind ja bereits im settingsland -> nach onStart umgezogen
        //toolbar items upload+recordyourself shalln't show in settings
        toolbarUploadButton.setVisibility(View.GONE);
        toolbarRecordyourselfButton.setVisibility(View.GONE);

        // Create shared preferences
        languagePreferences = getSharedPreferences("languagePreferences", Context.MODE_PRIVATE);

        //buttons
        screenmodebutton = findViewById(R.id.screenmodebuttonid);
        applanguagebutton = findViewById(R.id.applanguagebuttonid);
        logoutbutton = findViewById(R.id.logoutbuttonid);
        screenmodebutton.setOnClickListener(v -> showScreenmodeSettingsDialog());
        applanguagebutton.setOnClickListener(v -> showAppLanguageSettingsDialog());
        logoutbutton.setOnClickListener(v -> logOut());
        currentusertextview = findViewById(R.id.currentuserid);

        //memolanguagebutton = findViewById(R.id.memolanguagebuttonid);
        //memolanguagebutton.setOnClickListener(v -> showMemoLanguageSettingsDialog());

        //mongo
        app = new App(new AppConfiguration.Builder("remember-dxcgp").build());
        user = app.currentUser();
        usermail = user != null ? user.getProfile().getEmail() : null;
        //plug current user in the textview
        if (username != null) currentusertextview.setText(username);
        else if (user != null) currentusertextview.setText(usermail);


        // Create shared preferences to retrieve device language (important for displayed text on language button at first appstart and later for the change back to device settings)
        SharedPreferences languagePreferences = getSharedPreferences("languagePreferences", Context.MODE_PRIVATE);
        Log.d(TAG,"language aus languagePreferences ist: " + languagePreferences.getString("selectedAppLanguage", "system") + " (in: onCreate)");
        // Check if the system's language is already stored in SharedPreferences
        boolean firstTimeOpeningSettings = languagePreferences.getBoolean("firstTimeOpeningSettings", true);
        if (firstTimeOpeningSettings) { //dieser if clause löst nur 1x aus und nicht bei jedem onCreate
            Log.d(TAG,"first time opening settings");
            //language
            //systemLanguage = Locale.getDefault().getLanguage();// Retrieve the system's language TODO (info) stattdessen aus Main eingeholt, wo er ursprünglich erstellt wird der String
            SharedPreferences.Editor editor = languagePreferences.edit();// Store the system's language in SharedPreferences
            //editor.putInt("systemScreenmode", systemScreenmode);
            editor.putString("systemLanguage", LanguageUtils.getSystemLanguageFromSP(this)); //onCreate Main, wo app start passiert ... in Register/Login wird die system language bereits überprüft und der contentView entsprechend angepasst - aber man kann dort die Languages noch nicht ändern, daher sollte es passen, wenn man die System Language immer aus Main einliest
            editor.putBoolean("firstTimeOpeningSettings", false);
            editor.apply();
            applanguagebutton.setText(getString(R.string.system));//if first time opening settings, device language is active & the button shows as text the device language
            //screenmode
            //int systemScreenmode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK; // Retrieve the system's screenmode
            screenmodebutton.setText(getString(R.string.system));//if first time opening settings, device screenmode is active & the button shows as text the device screenmode
        } else {
            Log.d(TAG,"not first time opening settings");
            //screenmode
            SharedPreferences screenmodePreferences = getSharedPreferences("screenmodePreferences", Context.MODE_PRIVATE);
            String selectedScreenmode = screenmodePreferences.getString("selectedScreenmode", "system"); //"system" ist von android was, daher uninitiiert
            Log.d(TAG,"screenmode aus screenmodePreferences ist: " + selectedScreenmode);
          /*  switch (selectedScreenmode) {
                case "system":
                    languagebutton.setText(R.string.system);
                    break;
                case "Day":
                case "Tag":
                    languagebutton.setText(R.string.day);
                    break;
                case "Night":
                case "Nacht":
                    languagebutton.setText(R.string.night);
                    break;
            }*/
            switch (selectedScreenmode) {
                case "daymodeSP":
                    screenmodebutton.setText(R.string.day);
                    break;
                case "nightmodeSP":
                    screenmodebutton.setText(R.string.night);
                    break;
                case "system":
                    screenmodebutton.setText(R.string.system);
                    break;
            }
            //language
            String languageCode = languagePreferences.getString("selectedAppLanguage", "system");
            Log.d(TAG,"language code aus languagePreferences ist: " + languageCode + " (onCreate in: Settings)");
        /*    switch (languageCode) {
                case "System":
                    languagebutton.setText(R.string.system);
                    break;
                case "Deutsch":
                case "German":
                    languagebutton.setText(R.string.german);
                    break;
                case "Englisch":
                case "English":
                    languagebutton.setText(R.string.english);
                    break;
            }*/
            switch (languageCode) {
                case "germanSP":
                    applanguagebutton.setText(R.string.german);
                    break;
                case "englishSP":
                    applanguagebutton.setText(R.string.english);
                    break;
                case "system":
                    System.out.println(languageCode + " = system (onCreate in: Settings)");
                    applanguagebutton.setText(R.string.system);
                    break;
            }
            //languageButtonSetText(languageCode); //methode anstatt direkt langaugebutton zu setzen duwirst sehen marc XDFDDD NEIN LSOIE§IESDKL;DF


        }


                /*// Retrieve the current screen mode
        int currentScreenMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;*/
//String systemLanguage = languagePreferences.getString("systemLanguage", "");

     /*   //navi bitte wenden //BUtton, den ich entefernt habé
        publish = findViewById(R.id.publishid);
        publish.setOnClickListener(v -> {
            startActivity(new Intent(Settings.this, Publish.class));
        });*/


        Log.d(TAG,"Ende onCreate (in Settings)");
    }//________________onCreate_____________END________

    private void logOut() {
        // Log out the current user (if one is logged in)
        if (user != null) {
            user.logOutAsync(logout -> {
                if (logout.isSuccess()) {
                    //Log.d(TAG,"successfly loggedout");
                } else {
                    Log.e(TAG,"failed logout of user " + user + logout.getError() + " (prbly some issue in mongodb)");
                }
                Log.d(TAG,"logout of user " + user + " successful!");
                Intent intent = new Intent(this, Login.class);
                startActivity(intent);
                //damit man nicht wieder in Settings kommt, nachdem man aus Login in Main geht
                finish();
            });
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG,"onStart was called (in: Settings)");
        // der settings knopf soll colored + disabled sein :) wird sind ja bereits im settingsland (da sinnwa wiedera)
        Drawable threeLines = ContextCompat.getDrawable(toolbarSettingsButton.getContext(), R.drawable.threelines_grey);
        threeLines.setColorFilter(Color.parseColor("#060c12"), PorterDuff.Mode.SRC_ATOP);//TODO(future) if main_color & color_one_nightmode change -> change here manualli!
        toolbarSettingsButton.setImageDrawable(threeLines); //#42E0D1 alte cyan color , main_color #060c12
        //toolbarSettingsButton.setBackground(ContextCompat.getDrawable(Settings.this, R.drawable.threelines_main_color));
        //toolbarSettingsButton.setScaleType(ImageView.ScaleType.FIT_XY);
        //toolbarSettingsButton.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));

        toolbarSettingsButton.setEnabled(false);
        Log.d(TAG,"Ende onStart (in: Settings)");
    }


    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG,"onStop was called (in: Settings)");
        // Reset the color filter by setting the original drawable resource
        Drawable threeLines = ContextCompat.getDrawable(toolbarSettingsButton.getContext(), R.drawable.threelines_grey);
        threeLines.setColorFilter(Color.parseColor("#3D81AC"), PorterDuff.Mode.SRC_ATOP); //va bene  funzionafinalmente //TODO(future) if main_color & color_one_nightmode change -> change here manualli!
        toolbarSettingsButton.setImageDrawable(threeLines); //#42E0D1 alte cyan color , #3D81AC color_one_nightmode

        //toolbarSettingsButton.setBackground(ContextCompat.getDrawable(Settings.this, R.drawable.threelines_color_one_nightmode));
        //toolbarSettingsButton.setScaleType(ImageView.ScaleType.FIT_XY);
        //toolbarSettingsButton.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
//toolbarSettingsButton.setImageResource(R.drawable.threelines_grey);
        toolbarSettingsButton.setEnabled(true);
        Log.d(TAG,"Ende onStoep (in: Settings)");
    }






    /*public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.densityDpi / 160f));
        return px;
    }*/







    private void showScreenmodeSettingsDialog() {
        // Create shared preferences
        SharedPreferences screenmodePreferences = getSharedPreferences("screenmodePreferences", Context.MODE_PRIVATE);

        // Get the saved screenmode OR use the default value ("system", which is a predefined value by sharedPref android)
        String selectedScreenmode = screenmodePreferences.getString("selectedScreenmode", "system"); //"system" ist von android was, daher uninitiiert
        Log.d(TAG,"Screenmode in screenmodePreferences ist: " + screenmodePreferences.getString("selectedScreenmode", "system"));

        // Set up the alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.screenmode);
        // Options for screen mode
        String[] options = {getString(R.string.day), getString(R.string.night), getString(R.string.system)};
        builder.setSingleChoiceItems(options, getSelectedScreenmodeIndex(selectedScreenmode), (dialog, which) -> {
            // Update the selected screenmode
            String newScreenmode = getScreenmodeFromIndex(which);

            // Save the new screenmode in shared preferences
            SharedPreferences.Editor editor = screenmodePreferences.edit();
            editor.putString("selectedScreenmode", newScreenmode);
            editor.apply();
            Log.d(TAG,"Screenmode in screenmodePreferences gesetzt als: " + screenmodePreferences.getString("selectedScreenmode", "system"));

            // Apply night mode setting
            applyScreenmode(newScreenmode);

            // Dismiss the dialog
            dialog.dismiss();
        });
        // Show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    //vom utente im AlertDialog ausgewählter screenmdoe
    private int getSelectedScreenmodeIndex(String selectedScreenmode) {
        if (selectedScreenmode.equals("daymodeSP")) {
            return 0;
        } else if (selectedScreenmode.equals("nightmodeSP")) {
            return 1;
        } else {
            return 2; // "system"
        }
    }
    //finde den index vom ausgew. screenmode
    private String getScreenmodeFromIndex(int index) {
        if (index == 0) {
            return "daymodeSP";
        } else if (index == 1) {
            return "nightmodeSP";
        } else {
            return "system";
        }
    }
    private void applyScreenmode(String screenmode) {
        if (screenmode.equals("daymodeSP")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            screenmodebutton.setText(getString(R.string.day));
        } else if (screenmode.equals("nightmodeSP")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            screenmodebutton.setText(getString(R.string.night));
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            screenmodebutton.setText(getString(R.string.system));
        }
    }








    //TODO (unzufr.) Weniger Code & einfacher wäre: utente dazu zwingen, dass eine änderung der sprache immer gleich die app neustartet
    //LANGUAGES
    private void showAppLanguageSettingsDialog() {
        Log.d(TAG,"showLanguageSettingsDialog ausgelöst (in: Settings)");
        // Get the selected/saved language or use the default value for it ("system")
        String selectedAppLanguage = languagePreferences.getString("selectedAppLanguage", "system");
        Log.d(TAG,"language in languagePreferences ist: " + languagePreferences.getString("selectedAppLanguage", "system") + " (in: direkt vor AlertDialog,  showLanguageSettingsDialog)");

        // Set up the alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.applanguage);
        // Options for languages
        String[] options = {getString(R.string.german), getString(R.string.english), getString(R.string.system)};
        builder.setSingleChoiceItems(options, getselectedLanguageIndex(selectedAppLanguage), (dialog, which) -> {
            // Update the selected language
            String newLanguage = getLanguageFromIndex(which);

       /*     // Check if the language has changed
            boolean languageChanged = !selectedAppLanguage.equals(newLanguage);
            Log.d(TAG,"boolean languageChanged: " + languageChanged + " (in: showLanguageSettingsDialog, Settings)");*/

            //Save language setting in shared preferences
            SharedPreferences.Editor editor = languagePreferences.edit();
            editor.putString("selectedAppLanguage", newLanguage);
            //editor.putBoolean("languageChanged", true);
            editor.apply();
            Log.d(TAG,"Sprache gewechselt zu: " + languagePreferences.getString("selectedAppLanguage", "system") + " (in: AlertDialog, showLanguageSettingsDialog)");

         /*   //Apply language setting in the app IFF it has changed //instead, languageChange is stored in SP and in each onResume of other activities
            if (languageChanged) {
                // Finish all previously opened activities except the current one
                finishPreviouslyOpenedActivities();

                //Apply language setting in the app with setLocale
            }*/
            
            //Apply language setting in the app
            applyLanguage(newLanguage);

            // Dismiss the dialog
            dialog.dismiss();
            Log.d(TAG,"Alert Dialog END (in: showLanguageSettingsDialog, Settings)");
        });
        // Show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
        Log.d(TAG,"showLanguageSettingsDialog END (in: Settings)");
    }
    private void showMemoLanguageSettingsDialog() {
        Log.d(TAG,"showMemoLanguageSettingsDialog ausgelöst (in: Settings)");
        // Get the selected/saved language or use the default value for it ("system")
        String selectedMemoLanguage = languagePreferences.getString("selectedMemoLanguage", "system");
        Log.d(TAG,"language in languagePreferences ist: " + languagePreferences.getString("selectedMemoLanguage", "system") + " (in: direkt vor AlertDialog,  showLanguageSettingsDialog)");

        // Set up the alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.memolanguage);
        // Options for languages
        String[] options = {getString(R.string.german), getString(R.string.english), getString(R.string.system)};
        builder.setSingleChoiceItems(options, getselectedLanguageIndex(selectedMemoLanguage), (dialog, which) -> {
            // Update the selected language
            String newLanguage = getLanguageFromIndex(which);

       /*     // Check if the language has changed
            boolean languageChanged = !selectedMemoLanguage.equals(newLanguage);
            Log.d(TAG,"boolean languageChanged: " + languageChanged + " (in: showLanguageSettingsDialog, Settings)");*/

            //Save language setting in shared preferences
            SharedPreferences.Editor editor = languagePreferences.edit();
            editor.putString("selectedMemoLanguage", newLanguage);
            //editor.putBoolean("languageChanged", true);
            editor.apply();
            Log.d(TAG,"Sprache gewechselt zu: " + languagePreferences.getString("selectedMemoLanguage", "system") + " (in: AlertDialog, showLanguageSettingsDialog)");

         /*   //Memoly language setting in the Memo IFF it has changed //instead, languageChange is stored in SP and in each onResume of other activities
            if (languageChanged) {
                // Finish all previously opened activities except the current one
                finishPreviouslyOpenedActivities();

                //Memoly language setting in the Memo with setLocale
            }*/

            //Memoly language setting in the Memo
            applyLanguage(newLanguage);

            // Dismiss the dialog
            dialog.dismiss();
            Log.d(TAG,"Alert Dialog END (in: showLanguageSettingsDialog, Settings)");
        });
        // Show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
        Log.d(TAG,"showLanguageSettingsDialog END (in: Settings)");
    }
    //vom utente im AlertDialog ausgewählte language
    private int getselectedLanguageIndex(String selectedLanguage) {
        //TODO(C) problem to keep in mind: with multiple languages in the shared preferences, they don't work properly bc. user might put in language as shared preference in his language (e.g. "Deutsch") and then he switches languages and then "German" is not found in the shared preferences
        // -> that's why i put the enquiry for multiple languages (not ideal solution but it WOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO..)
        if (selectedLanguage.equals("germanSP")) {
            return 0;
        } else if (selectedLanguage.equals("englishSP")) {
            return 1;
        } else {
            return 2; // "systemdefaultSP"
        }
    }
    //finde den index von der ausgewählten language
    private String getLanguageFromIndex(int index) {
        if (index == 0) {
            return "germanSP";
        } else if (index == 1) {
            return "englishSP";
        } else {
            return "system";
        }
    }
    private void applyLanguage(String language) {
        SharedPreferences labomba = getSharedPreferences("languagePreferences", Context.MODE_PRIVATE); //TODO (C) just for debug
        //1^change app language
        if (language.equals("germanSP")) {
            Log.d(TAG,"language in languagePreferences ist: " + labomba.getString("selectedAppLanguage", "system") + " (applyLanguage in AlertDialog, showLanguageSettingsDialog)");
            setLocale("de");
            Log.d(TAG,"locale ist: " + Locale.getDefault());
            applanguagebutton.setText(getString(R.string.german));
        } else if (language.equals("englishSP")) {
            Log.d(TAG,"language in languagePreferences ist: " + labomba.getString("selectedAppLanguage", "system") + " (applyLanguage in AlertDialog, showLanguageSettingsDialog)");
            setLocale("en");
            applanguagebutton.setText(getString(R.string.english));
        } else {
            //SharedPreferences languagePreferences = getSharedPreferences("languagePreferences", Context.MODE_PRIVATE);
            //String systemLanguage = languagePreferences.getString("systemLanguage", "");
            Log.d(TAG,"language in languagePreferences ist: " + labomba.getString("selectedAppLanguage", "system") + " (applyLanguage in AlertDialog, showLanguageSettingsDialog)");
            String systemLanguageinspformat = LanguageUtils.getSystemLanguageFromSP(this); //applyLanguage (Settings)
            String languageCode = systemLanguageinspformat.equals("germanSP") ? "de" : "en";//TODO (future) add more cases if more languages are in the app}
            Log.d(TAG,"languageCode gemäß Main.getSystemLanguageInSPFormat() ist: " +  languageCode + " (applyLanguage in AlertDialog, showLanguageSettingsDialog)");
            setLocale(languageCode);
            applanguagebutton.setText(getString(R.string.system));
        }
    }
    private void setLocale(String languageCode) {
        if(languageCode.equals("en")) {
            Log.d(TAG,"languageCode gemäß Main.getSystemLanguageInSPFormat() ist 'en', also Umstellung auf Englisch (setLocale in AlertDialog, showLanguageSettingsDialog)");
        }
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);

        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        recreate(); // Restart the activity to apply the new language //TODO (A) ist hat necessario=?
    }
 /*   private void finishPreviouslyOpenedActivities() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        if (activityManager != null) {
            List<ActivityManager.AppTask> appTasks = activityManager.getAppTasks();

            if (appTasks.size() > 1) {
                ActivityManager.AppTask currentTask = appTasks.get(0);

                for (ActivityManager.AppTask task : appTasks) {
                    if (task != currentTask) {
                        task.finishAndRemoveTask();
                    }
                }
            }
        }
    }*/
    private void finishPreviouslyOpenedActivities() {
        Log.d(TAG,"finishPreviouslyOpenedActivities has been called (in: AlertDialog, showLanguageSettingsDialog)");
        //this restarts the activity TODO if keep this, delete the line "restart()" otherwise restarts 2 times


        /*Intent intent = new Intent(this, Settings.class); //this would only bring Settings on top of the activity stack (navigation wise backstack diesdas), but not cause other activities to call onCreate again
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);*/
/*    ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE); //this didn't work simplemeitn

        if (activityManager != null) {
            List<ActivityManager.AppTask> appTasks = activityManager.getAppTasks();

            // Get the current task id
            int currentTaskId = getTaskId();

            for (ActivityManager.AppTask task : appTasks) {
                int taskId = task.getTaskInfo().id;
                Log.d(TAG,"task ID ist " + taskId + " (finishPreviouslyOpenedActivities in showLanguageSettingsDialog)");

                // Skip the current task (Settings activity)
                if (taskId != currentTaskId) {
                    Log.d(TAG,"task ID von " + task + taskId + " wird gelöscht (finishPreviouslyOpenedActivities in showLanguageSettingsDialog)");
                    task.finishAndRemoveTask();
                }
            }
        }*/
        Log.d(TAG,"Ende finishPreviouslyOpenedActivities (in: showLanguageSettingsDialog)");
    }




}