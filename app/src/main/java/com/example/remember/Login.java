package com.example.remember;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.bson.Document;

import java.util.concurrent.atomic.AtomicReference;

import io.realm.mongodb.App;
import io.realm.mongodb.AppConfiguration;
import io.realm.mongodb.Credentials;
import io.realm.mongodb.User;
import io.realm.mongodb.mongo.MongoClient;
import io.realm.mongodb.mongo.MongoCollection;
import io.realm.mongodb.mongo.MongoDatabase;

public class Login extends AppCompatActivity {
    private static final String TAG = "LOGOOIIIN";
    //TODO (B) login + register seiten sollen nicht im task-stack enthalten bleiben. deren traces sollen komplett vernichtet werden, wenn man von ihnen aus
    //TODO ... in Main kommt, z.B. mit flags bei intents kann man dafür sorgen, dass login + register ausm task gelöscht werdne und Main dann die neue root
    //TODO ... von allem (des Bösen) ist. z.B. wenn man NUR diese flag nimmt: Intent.FLAG_ACTIVITY_CLEAR_TASK , dann: In other words, if there are other
    //TODO ... activities in the task stack, they will be removed, and the Main activity will become the only activity in the task.
    EditText usernameoremailedittext, passwordedittext; //TODO user sollen sich mit entweder username ODER email anmelden können! <3
    Button loginbutton, mainbutton;
    TextView orregisterbutton;
    ProgressBar progressBar;

//firebase
    //FirebaseAuth fAuth;
//mongodb
    App app;
    Credentials credentials;
    User user, anonymoususer, apikeyuser;
    MongoDatabase mongoDatabase;
    MongoClient mongoClient;
    MongoCollection<Document> mongoUsersCollection, mongoAudiosCollection, mongoCategoriesCollection;
    String userid;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate Login");
        super.onCreate(savedInstanceState);
//0 Language
        //Get the languauage which is right now active and maybe change this activity's language to it (lang might have been changed in Settings that's why...)
        LanguageUtils languageUtils = new LanguageUtils();
        languageUtils.updateLanguage(this);
//M Content View
        setContentView(R.layout.activity_login);
//1 UI
        usernameoremailedittext = findViewById(R.id.usernameoremailid);
        passwordedittext = findViewById(R.id.passwordid);
        progressBar = findViewById(R.id.progressbar2id);
        loginbutton = findViewById(R.id.loginbuttonid);
        orregisterbutton = findViewById(R.id.orloginid);
        //mainbutton = findViewById(R.id.mainbuttonid);
        //fAuth = FirebaseAuth.getInstance();
        //mongo
        app = new App(new AppConfiguration.Builder("remember-dxcgp").build());





        loginbutton.setOnClickListener(view -> {
            Log.d(TAG, "loginbutton clicked");
            String usernameoremail1 = usernameoremailedittext.getText().toString().trim();
            String password1 = passwordedittext.getText().toString().trim();
            if(TextUtils.isEmpty(usernameoremail1)){
                usernameoremailedittext.setError(getString(R.string.pluginnameoremailplease));
                return;
            }
            if(TextUtils.isEmpty(password1)){
                passwordedittext.setError(getString(R.string.pluginpasswordplease));
                return;
            }
            progressBar.setVisibility(View.VISIBLE);
            orregisterbutton.setVisibility(View.GONE);

            //Login the user which has previously created an email/password account
            if (usernameoremailedittext.getText().toString().contains("@")) {
                //email was plugged in
                loginWithEmail(usernameoremail1, password1); //from loginbutton
            }
            else {
                //username was plugged in
                //TODO (login) momentan temporary API key usage for login... 2 problems
                // is it safe?
                // can multiple apikey users be logged in simulatenously on different devices?
                // ..bei der Alternative: anonymous users war halt die Präkitation, dass die meinen App Users Screen complett zucluttern, daher hab ich lieber api key entschieden
                // -> falls doch wieder anonym users ausprobieren will: https://www.mongodb.com/community/forums/t/delete-anonymous-users-upon-log-out-trigger/8264/4 - fct for deleting/removing anonUsers all 5 minutes...
                loginWithUsername(usernameoremail1, password1);
            }




            //TODO (login) das hier ist , wenn man einen bestehenden Acc mit einem neuen Acc linken will (d.h. wenn man Google Authentican nutzt und dann aber denselben Acc wie früher nutzen will)
            /*user.linkCredentialsAsync(Credentials.emailPassword(usernameoremail1, password1), result -> {
                if (result.isSuccess()) {
                    Log.v("EXAMPLE", "Successfully linked existing user " +
                            "identity with email/password user: " + result.get());


                } else {
                    Log.e("EXAMPLE", "Failed to link user identities with: " +
                            result.getError());
                }
            });*///link credentials..

            /*FirebaseFirestore fStore = FirebaseFirestore.getInstance(); //query for usernames
            CollectionReference usersRef = fStore.collection("users");
            com.google.firebase.firestore.Query query = usersRef.whereEqualTo("username", usernameoremail1).limit(1);
            System.out.println("Login username search query wird nun starten (in: Login)");
            query.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    QuerySnapshot querySnapshot = task.getResult();
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        System.out.println("Query has found username (in: Login)");
                        // Username found, retrieve the email address
                        DocumentSnapshot documentSnapshot = querySnapshot.getDocuments().get(0);
                        String email = documentSnapshot.getString("email");

                        // Proceed with signing in using the retrieved email and password
                        signInWithEmail(email, password1);
                    } else {
                        System.out.println("Query hasn't found username -> therefore try password for login (in: Login)");
                        // If username not found, check for email
                        signInWithEmail(usernameoremail1, password1);
                    }
                } else {
                    Toast.makeText(Login.this, "Query has problemed", Toast.LENGTH_SHORT).show();
                    Exception exception = task.getException();
                    if (exception != null) {
                        System.out.println("Query Exception (in: Login): " + exception);
                    }
                }
            });*///old fb


/*
            Query query = usersRef.orderByChild("username").equalTo(usernameoremail1).limitToFirst(1);
            System.out.println("la bomba");
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) { //TODO(info) annotated as non-null
                    System.out.println("query starts to search for usernames"); //TODO(A) es funktioniert nicht, der progressbar ist inifitely timelong -> vlt liegt's an der @NonNul ??
                    if (dataSnapshot.exists()) {
                        System.out.println("query found username");
                        //this query searches for usernames. if username found, retrieve the associated email address
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String email = snapshot.child("email").getValue(String.class);
                            // Proceed with signing in using the retrieved email and password
                            signInWithEmail(email, password1);
                            return; // Exit the method after signing in
                        }
                    }
                    System.out.println("query hasn't found username -> edittext must be email, therefore log in with that");
                    //If query couldn't find a username that fits the text in edittext, query the text in edittext for email-adresses
                    signInWithEmail(usernameoremail1, password1);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Handle database error //TODO (C)
                }
            });*///old fb



        });



        orregisterbutton.setOnClickListener(view -> {
            //anonymoususer erst ausloggen bevor, man zu register geht
            if (anonymoususer != null) {
                anonymoususer.logOutAsync(logout -> {
                    if (logout.isSuccess()) {
                        anonymoususer.remove();
                        Log.v(TAG, "anonymoususer logged out and user data removed -> log in the regular email/password user");
                        startActivity(new Intent(Login.this,Register.class));
                    } else {
                        Log.e(TAG, "failed to logout anonymoususer");
                        progressBar.setVisibility(View.GONE);
                    }
                });
            } else {
                startActivity(new Intent(Login.this,Register.class));
            }
        });
        //mainbutton.setOnClickListener(view -> startActivity(new Intent(Login.this, Main.class)));

        //damit der ENTER button als LOGIN button fungiert
        passwordedittext.setOnKeyListener((v, keyCode, event) -> {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                InputMethodManager imm = (InputMethodManager) getBaseContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                loginbutton.performClick();
                return true;
            }
            return false;
        });
    }//________________________ Ende onCreate __________________________________


    @Override
    protected void onStop() {
        Log.d(TAG, "onStop called");
        if (anonymoususer != null) {
            if (anonymoususer.isLoggedIn()) {
                anonymoususer.logOutAsync(logout -> {
                    if (logout.isSuccess()) {
                        anonymoususer.remove();
                        Log.v(TAG, "anonymoususer logged out and user data removed");
                    } else {
                        Log.e(TAG, "failed to logout anonymoususer");
                    }
                });
            }
        }
        if (apikeyuser != null) {
            if (apikeyuser.isLoggedIn()) {
                apikeyuser.logOutAsync(logout -> {
                    if (logout.isSuccess()) {
                        Log.v(TAG, "apikeyuser logged out");
                    } else {
                        Log.e(TAG, "failed to logout apikeyuser");
                    }
                });
            }
        }
        Log.d(TAG, "onStop END");
        super.onStop();
    }

    /*@Override
    protected void onStart() {
        Log.d(TAG, "onStart called");
        //if anonymous user is not logged in: log her in
        if (anonymoususer != null) {
            if (!anonymoususer.isLoggedIn()) {
                Credentials anonymousCredentials = Credentials.anonymous();
                AtomicReference<User> anonymoususeratomicref = new AtomicReference<User>();
                app.loginAsync(anonymousCredentials, login -> {
                    if (login.isSuccess()) {
                        Log.v(TAG, "Successfully authenticated anonymously.");
                        anonymoususeratomicref.set(app.currentUser());
                        // Get the User object from the AtomicReference
                        anonymoususer = anonymoususeratomicref.get();
                        mongoClient = anonymoususer.getMongoClient("mongodb-atlas");
                        mongoDatabase = mongoClient.getDatabase("remember");
                        mongoUsersCollection = mongoDatabase.getCollection("users");
                        Log.d(TAG, "mongoUsersCollection is: " + mongoUsersCollection);
                    } else {
                        Log.e(TAG, "failed logging in anonymously: "+ login.getError().toString());
                    }
                }); //so, now an anonymous user is logged in who can access the usersColl to brwose for his/her email xD
            }
        }
        Log.d(TAG, "onStart END");
        super.onStart();
    }*///old version with anonymous user



    private void loginWithEmail(String emailio, String passwordius) {
        Log.v(TAG, "loginWithEmail called");
        //first log out the temporarily logged in apikeyuser ...
        if (apikeyuser != null) {
            Log.v(TAG, "apikeyuser != null -> logout apikeyuser + login emailpassworduser");
            apikeyuser.logOutAsync(logout -> {
                if (logout.isSuccess()) {
                    Log.v(TAG, "apikeyuser logged out -> log in the regular email/password user");
                    //... and now log in email-pw user
                    app.loginAsync(Credentials.emailPassword(emailio, passwordius), login -> {
                        if (login.isSuccess()) {
                            user = app.currentUser();
                            if (user != null) {
                                userid = user.getId();
                            }
                            Log.v(TAG, "Login of " + emailio + " successful [userid:" + userid + "]");
                            startActivity(new Intent(getApplicationContext(), Main.class));
                            finish(); //registeractivity muss gefinisht werden, man will ja nicht in den register-screen zurückswipen aus versehen! einmal raus, immer raus!
                        }
                        else {
                            Log.e(TAG, "Failed to log in " + emailio + " [userid:" + userid + "]: " + login.getError().getErrorMessage());
                            //notify user
                            Toast.makeText(this, R.string.credentialswrong, Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                        }
                    });
                } else {
                    Log.e(TAG, "failed to logout anonymoususer");
                    //TODO (login) (info) theoretisch kann ich email-pw auch einloggen, wenn anonymoususer trotzdem noch angemeldet war.. weil:
                    //When you call app.currentUser() in a new activity, it returns the currently logged-in user for the current session.
                    // It does not return multiple users if you have multiple users logged in simultaneously.
                    // It returns the user that was most recently logged in or whose session is active in the current context.
                }
            });
        } else {
            Log.v(TAG, "apikeyuser = null -> login emailpassworduser");
            //log in email-pw user
            app.loginAsync(Credentials.emailPassword(emailio, passwordius), login -> {
                if (login.isSuccess()) {
                    user = app.currentUser();
                    if (user != null) {
                        userid = user.getId();
                    }
                    Log.v(TAG, "Login of " + emailio + " successful [userid:" + userid + "]");
                    startActivity(new Intent(getApplicationContext(), Main.class));
                    finish(); //registeractivity muss gefinisht werden, man will ja nicht in den register-screen zurückswipen aus versehen! einmal raus, immer raus!
                }
                else {
                    Log.e(TAG, "Failed to log in " + emailio + " [userid:" + userid + "]: " + login.getError().getErrorMessage());
                    //notify user
                    Toast.makeText(this, R.string.credentialswrong, Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                }
            });
        }
        /*//first log out the temporarily logged in anonymous user ...
        if (anonymoususer != null) {
            anonymoususer.logOutAsync(logout -> {
                if (logout.isSuccess()) {
                    anonymoususer.remove(); //this would've also logged out the user by itself ... read docum roli
                    Log.v(TAG, "anonymoususer logged out -> log in the regular email/password user");
                    //... and now log in email-pw user
                    app.loginAsync(Credentials.emailPassword(emailio, passwordius), login -> {
                        if (login.isSuccess()) {
                            user = app.currentUser();
                            if (user != null) {
                                userID = user.getId();
                            }
                            Log.e(TAG, "Login of " + emailio + " successful [userid:" + userID + "]");
                            startActivity(new Intent(getApplicationContext(), Main.class));
                            finish(); //registeractivity muss gefinisht werden, man will ja nicht in den register-screen zurückswipen aus versehen! einmal raus, immer raus!
                        }
                        else {
                            Log.e(TAG, "Failed to log in " + emailio + " [userid:" + userID + "]: " + login.getError().getErrorMessage());
                            //notify user
                            Toast.makeText(this, R.string.credentialswrong, Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                        }
                    });
                } else {
                    Log.e(TAG, "failed to logout anonymoususer");
                    //TODO (login) (info) theoretisch kann ich email-pw auch einloggen, wenn anonymoususer trotzdem noch angemeldet war.. weil:
                    //When you call app.currentUser() in a new activity, it returns the currently logged-in user for the current session.
                    // It does not return multiple users if you have multiple users logged in simultaneously.
                    // It returns the user that was most recently logged in or whose session is active in the current context.
                }
            });
        }*///old version with anonymous user
    }

    private void loginWithUsername(String usernamio, String passwordius) {
        Log.d(TAG, "loginWithUsername called");

        //TODO unsafe with the API key , but this API Key can only read the usersColl , therefore no real security drama - but normally: never put api keys in C§DE!!!
        //here, i need to browse the usersColl first for the usernamio ... for that a user has to be logged in, using API Key now (instead of anonymous users that clutter my "App Users" tab in the realm)
        Credentials apiKeyCredentials = Credentials.apiKey("7oUfeF0f6MU9zubXmqeQNwgOyPqdpicBa01xTu4XpbpLZnXXJsfgqmiqaiWu9t7S");
        AtomicReference<User> apikeyuseratomicref = new AtomicReference<User>();
        app.loginAsync(apiKeyCredentials, login -> {
            if (login.isSuccess()) {
                Log.v(TAG, "Successfully authenticated using an API Key.");
                apikeyuseratomicref.set(app.currentUser()); //set the apikeyuseratomicref to the currentuser
                // Get the User object from the AtomicReference
                apikeyuser = apikeyuseratomicref.get();
                mongoClient = apikeyuser.getMongoClient("mongodb-atlas");
                mongoDatabase = mongoClient.getDatabase("remember");
                mongoUsersCollection = mongoDatabase.getCollection("users");
                browseUsersCollForUsername(usernamio, passwordius);
            } else {
                Log.e(TAG, "failed authenticating user using API Key" + login.getError());
                progressBar.setVisibility(View.GONE);
            }
        });
        /*//TODO (info) one has to be logged as user to access collections (-> usersCollection in case that utente plugs in username bc. then the usersCollection is browsed for the associated email)
        // --> therefore log in anonymously temporarily (and afterwards with the real credentials of the user) , anonymous user has to be logged out afterwards and removed, otherwise it remains in the user db...
        Credentials anonymousCredentials = Credentials.anonymous();
        AtomicReference<User> anonymoususeratomicref = new AtomicReference<User>();
        app.loginAsync(anonymousCredentials, login -> {
            if (login.isSuccess()) {
                Log.v(TAG, "Successfully authenticated anonymously.");
                anonymoususeratomicref.set(app.currentUser());
                // Get the User object from the AtomicReference
                anonymoususer = anonymoususeratomicref.get();
                mongoClient = anonymoususer.getMongoClient("mongodb-atlas");
                mongoDatabase = mongoClient.getDatabase("remember");
                mongoUsersCollection = mongoDatabase.getCollection("users");
                Log.v(TAG, "mongoUsersCollection is: " + mongoUsersCollection);
                browseUsersCollForUsername(usernamio, passwordius);
            } else {
                Log.v(TAG, "failed logging in anonymously: "+ login.getError().toString());
                progressBar.setVisibility(View.GONE);
            }
        }); //so, now an anonymous user is logged in who can access the usersColl to brwose for his/her email xD*/ //old version with anonymous user
    }

    void browseUsersCollForUsername(String usernamio, String passwordius) {
        //performing this as an anonymous user:
        Document queryfilterdocwiththisusername = new Document("username", usernamio);
        mongoUsersCollection.findOne(queryfilterdocwiththisusername).getAsync(task -> {
            if (task.isSuccess()) {
                Document result = task.get();
                Log.v(TAG, "successfully found doc with username " + usernamio + ": " + result);
                //now retrieve email from the doc containing this username
                String email = (String) result.get("email");
                //now log in with the regular email-password user
                loginWithEmail(email, passwordius); //from loginWithUsername
            } else {
                Log.e(TAG, "failed to find document with: ", task.getError());
                //notify user
                Toast.makeText(this, R.string.nousernamefound, Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }




    /*private void signInWithEmail(String emailOrUsername, String password) {
        fAuth.signInWithEmailAndPassword(emailOrUsername, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                startActivity(new Intent(getApplicationContext(), Main.class));
                finish(); //loginactivity muss gefinisht werden, man will ja nicht in den login-screen zurückswipen aus versehen! einmal raus, immer raus!
            } else {
                Toast.makeText(Login.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show(); //TODO(C) might produce NPE
                progressBar.setVisibility(View.GONE);
                orregisterbutton.setVisibility(View.VISIBLE);
            }
        });
    }*///old version with anonymous user






}