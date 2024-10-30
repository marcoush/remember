package com.example.remember.mongo;

import android.app.Application;

import io.realm.Realm;
import io.realm.RealmConfiguration;

//für mongoDB
public class MongoDBRealmConfig extends Application {
    //public static App app; //I have to do it inside each activity ... idkhow to centralize it

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Realm
        Realm.init(this);

        /*SyncConfiguration syncConfig = new SyncConfiguration.Builder()
                .withAppId("remember-dxcgp")
                .build();*/

        // Define the Realm configuration
        RealmConfiguration realmConfig = new RealmConfiguration.Builder()
                .name("remember")
                .allowQueriesOnUiThread(true)
                .allowWritesOnUiThread(true)
                .schemaVersion(0)
                .build();
        Realm.setDefaultConfiguration(realmConfig);

        //set app to my remember id from realm mongodb

        /*app = new App(new AppConfiguration.Builder("remember-dxcgp")
                .requestTimeout(30, TimeUnit.SECONDS)
                .build());*/

        // Optionally, you can set the MongoDB Realm App as the default app
        //App.setDefault(app); //nö , geht ned
    }

    /*public App getRealmApp() {
        return app;
    }*/
}