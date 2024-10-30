package com.example.remember.mongo;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Map;

public class CredentialsManager {
    private static final String PREFS_NAME = "credentialPref";
    private static final String USERNAME_KEY = "username";
    private static final String PASSWORD_KEY = "password";


    //TODO (info) BRAUCHE DEN GERADE GAR NCIHT, DACHTE NUR ICH BRÄUCHTE DEN ZWISCHENDRUCH, weil ich kurzzeitig nicht automatisch angemeldet wurde in Main, aber mittlerweile wird man automatisch angemeldet, also muss
    // man nicht händisch extra noch nen extra login in Main onCreate machen, weil der mongo user ohnehin schon autom loggined wird

    public static void saveCredentials(Context context, String username, String password) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(USERNAME_KEY, username);
        editor.putString(PASSWORD_KEY, password);

        editor.apply();
    }

    public static String getSavedUsername(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(USERNAME_KEY, null);
    }

    public static String getSavedPassword(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(PASSWORD_KEY, null);
    }

    public static Map<String, ?> getAllCredentials(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getAll();
    }

}
