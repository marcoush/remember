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

import com.example.remember.mongo.CredentialsManager;

import org.bson.Document;

import java.util.ArrayList;
import java.util.Date;

import io.realm.mongodb.App;
import io.realm.mongodb.AppConfiguration;
import io.realm.mongodb.Credentials;
import io.realm.mongodb.User;
import io.realm.mongodb.mongo.MongoClient;
import io.realm.mongodb.mongo.MongoCollection;
import io.realm.mongodb.mongo.MongoDatabase;

public class Register extends AppCompatActivity {
    private static final String TAG = "REGISTER";

    //TODO (B) login + register seiten sollen nicht im task-stack enthalten bleiben. deren traces sollen komplett vernichtet werden, wenn man von ihnen aus
    //TODO ... in Main kommt, z.B. mit flags bei intents kann man dafür sorgen, dass login + register ausm task gelöscht werdne und Main dann die neue root
    //TODO ... von allem (des Bösen) ist. z.B. wenn man NUR diese flag nimmt: Intent.FLAG_ACTIVITY_CLEAR_TASK , dann: In other words, if there are other
    //TODO ... activities in the task stack, they will be removed, and the Main activity will become the only activity in the task.

    //TODO (C) nur zur info: user "wolf" und "marolo" haben bei der registrierung keinen DISPLAY_NAME verpasst bekommen, daher wird wenn man mit diesen eingeloggt ist, auch kein username angezeigt im toast: "username ist: "
    //private static final String TAG = "TAG";

    //TODO (A) müsste nicht hier weil das die allererste seite ist, direkt der nightmode & langauge auf systemeinstellungen gesetzt werden?
    //TODO (A) man darf sich nicht mit einem username registieren oder mit einer mail , die es schon gibt

    EditText usernameedittext, emailedittext, passwordedittext, mPhone;
    Button mRegisterBtn, mainbutton;
    TextView mLoginBtn;
    ProgressBar progressBar;

    //firebase
    /*FirebaseAuth fAuth;
    FirebaseFirestore fStore;
    //chatGPT 0/2 new users can pick a username next to email to login ... this is whats needed
    FirebaseDatabase fDatabase;
    DatabaseReference usersRef;
    FirebaseUser user;
    String userID;*/
    //mongodb
    App app;
    Credentials credentials;
    Document userCustomData;
    User user;
    MongoDatabase mongoDatabase;
    MongoClient mongoClient;
    MongoCollection<Document> mongoUsersCollection, mongoAudiosCollection, mongoCategoriesCollection;
    String userid, usermail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate Register");
        super.onCreate(savedInstanceState);
//0 Language
        //Get the languauage which is right now active and maybe change this activity's language to it (lang might have been changed in Settings that's why...)
        LanguageUtils languageUtils = new LanguageUtils();
        languageUtils.updateLanguage(this);
//M Content View
        setContentView(R.layout.activity_register);
//1 UI
        usernameedittext = findViewById(R.id.usernameoremailid); //für meine Zwecke brauche ich nciht wie bei TradeView nnoch Name + Telnr
        emailedittext = findViewById(R.id.email);
        passwordedittext = findViewById(R.id.passwordid);
       // mPhone = findViewById(R.id.phone);
        mRegisterBtn = findViewById(R.id.registerBtn);
        mLoginBtn = findViewById(R.id.orloginid);
        //mainbutton = findViewById(R.id.mainbuttonid);
        progressBar = findViewById(R.id.progressBar);

        //fb
        //fAuth = FirebaseAuth.getInstance();
        //fStore = FirebaseFirestore.getInstance();
        //mongo
        app = new App(new AppConfiguration.Builder("remember-dxcgp").build());


        /*if(fAuth.getCurrentUser() != null){
            startActivity(new Intent(getApplicationContext(), Main.class));
            finish();
        }*/ //TODO das hier musste vernichtet werden, damit ich von der main activity aus auf die register seite komme - in der realität wird es natürlich keinen button geben, der es einem ermöglicht, auf die register-seite zu kommen

        mLoginBtn.setOnClickListener(view -> startActivity(new Intent(Register.this,Login.class)));
        //mainbutton.setOnClickListener(view -> startActivity(new Intent(Register.this, Main.class)));
        mRegisterBtn.setOnClickListener(view -> {
            progressBar.setVisibility(View.VISIBLE);
            String username1 = usernameedittext.getText().toString().trim();
            String email1 = emailedittext.getText().toString().trim();
            String password1 = passwordedittext.getText().toString().trim();
            //final String fullName = mFullName.getText().toString();
            //final String phone = mPhone.getText().toString().trim();
            //final int saldo = 1000;

            if(TextUtils.isEmpty(username1)){
                usernameedittext.setError(getString(R.string.pluginnameplease));
                return;
            }
            if(username1.length() > 15 ){
                usernameedittext.setError(getString(R.string.namemaximumfifteencharacters));
                return;
            }
            /*if(usernameedittext.toString().contains("@") || //sonderzeichen können in edittext gar nicht erst eingegeben werden i amended
                    usernameedittext.toString().contains("/") ||
                    usernameedittext.toString().contains("&") ||
                    usernameedittext.toString().contains("%") ||
                    usernameedittext.toString().contains("$") ||
                    usernameedittext.toString().contains("!") ||
                    usernameedittext.toString().contains("?") ||
                    usernameedittext.toString().contains("=") ||
                    usernameedittext.toString().contains(")") ||
                    usernameedittext.toString().contains("(") ||
                    usernameedittext.toString().contains("€")) {
                usernameedittext.setError("Keine Sonderzeichen.");
                return;
            }*/
            if(TextUtils.isEmpty(email1)){
                emailedittext.setError(getString(R.string.pluginemailplease));
                return;
            }
            if(TextUtils.isEmpty(password1)){
                passwordedittext.setError(getString(R.string.pluginpasswordplease));
                return;
            }
            if(password1.length() < 8 ){
                passwordedittext.setError(getString(R.string.passwordminimumeightcharacters));
                return;
            }

            //register user in mongo realm
            app.getEmailPassword().registerUserAsync(email1, password1, register -> {
                if (register.isSuccess()) {
                    Log.d(TAG, "Successfully registered user " + email1);
                    Toast.makeText(this, "User Created", Toast.LENGTH_SHORT).show();
                    //0 get user & id BEFORE login
                    user = app.currentUser();
                    userid = user != null ? user.getId() : null; //sieht funny aus, deswegen hab ich's etabliert , wurde vorgshlagen in der mwmw heit franen :)
                    usermail = user != null ? user.getProfile().getEmail() : null; //sieht funny aus, deswegen hab ich's etabliert , wurde vorgshlagen in der mwmw heit franen :)
                    Log.d(TAG,"userdata BEFORE login of newly registered user:" +
                            "\nuser: " + user +
                            "\nuserid: " + userid +
                            "\nusermail: " + usermail);

                    //1 save credentials in SP
                    CredentialsManager.saveCredentials(getBaseContext(), email1, password1);

                    //2 get regist date
                    Date registrationDate = new Date();
                    Log.d(TAG, "registrationDate ist: " + registrationDate);
                    //Timestamp registrationTimestamp = new Timestamp(registrationDate.getTime());
                    //Log.d(TAG, "registrationTimestamp ist: " + registrationTimestamp); //timestamp: error fsr

                    //3 log in schöön nachdem registered wurde
                    app.loginAsync(Credentials.emailPassword(email1, password1), login -> {
                        if (login.isSuccess()) {
                            Log.d(TAG, "Login of " + username1 + "/"+ email1 + " successful");
                            //4 sobald user logged in is, put all dada in the user doc
                            //4.1 get user data AFTER login
                            user = app.currentUser();
                            userid = user != null ? user.getId() : null; //sieht funny aus, deswegen hab ich's etabliert , wurde vorgshlagen in der mwmw heit franen :)
                            usermail = user != null ? user.getProfile().getEmail() : null; //sieht funny aus, deswegen hab ich's etabliert , wurde vorgshlagen in der mwmw heit franen :)
                            Log.d(TAG,"userdata AFTER login of newly registered user:" +
                                    "\nuser: " + user +
                                    "\nuserid: " + userid +
                                    "\nusermail: " + usermail+
                                    "\nusername: " + username1);
                            //empty array to put in: listeningswithdates and uploadswithdates
                            ArrayList<String> emptyarraylist = new ArrayList<>();

                            //TODO (register) i could retrieve registration / creation date also from the users profile#... but show kares
                            
                            //(info) each doc in mongo realm has to have an _id field. in the case of usersDoc, I don't use ObjectId(...) for that field, but the unique user id which is given to authenticated users

                            //4.2 plug the server id of the user (userid) as _id-field in the usersDoc and add user data
                            mongoClient = user.getMongoClient("mongodb-atlas");
                            mongoDatabase = mongoClient.getDatabase("remember");
                            mongoUsersCollection = mongoDatabase.getCollection("users");
                            mongoUsersCollection.insertOne(new Document("_id", userid).
                                    append("username", username1)                 .
                                    append("email", email1)                       .
                                    append("registrationdate", registrationDate)  .
                                    append("listenings", 0)                       .
                                    append("uploads", 0)                          .
                                    append("listeningswithdates", emptyarraylist) .
                                    append("uploadswithdates", emptyarraylist))   .getAsync(result -> {
                                if (result.isSuccess()) {
                                    Log.d(TAG, "Inserted custom user data document. _id of inserted document: "
                                            + result.get().getInsertedId());
                                    startActivity(new Intent(getApplicationContext(), Main.class));
                                    finish(); //registeractivity muss gefinisht werden, man will ja nicht in den register-screen zurückswipen aus versehen! einmal raus, immer raus!
                                } else {
                                    Log.e(TAG, "Unable to insert custom user data. Error: " + result.getError());
                                }
                            });
                        }
                        else {
                            Log.e(TAG, "Failed to log \" + username1 + \" in: " + login.getError().getErrorMessage());
                        }
                    });

                } else {
                    Log.e(TAG, "Failed to register user \" + username1 + \" : " + register.getError().getErrorMessage());
                }
            });


            //register user in firebase
            /*fAuth.createUserWithEmailAndPassword(email1,password1).addOnCompleteListener(task -> {
                if(task.isSuccessful()){
                    Toast.makeText(Register.this, "User Created", Toast.LENGTH_SHORT).show();
                    userID = fAuth.getCurrentUser().getUid(); //TODO (C) wieso warnung= ist doch nur, wenn task scucessful.... und das heißt doch dann, dass ein user created wurde...

                    //0 Set the display name
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder().setDisplayName(username1).build();
                        user.updateProfile(profileUpdates).addOnCompleteListener(updateTask -> {
                            if (updateTask.isSuccessful()) {
                                Toast.makeText(Register.this, "Display name updated" + user.getDisplayName(), Toast.LENGTH_SHORT).show();
                                // Continue with the registration process or other actions
                                //get the registration date
                                Date registrationDate = new Date();
                                Log.d(TAG, "registration date ist: " + registrationDate);
                                // 1 "user" is name of the collection in Firestore where you are storing the user's data in a doc. with the user ID as the document name, i initiate this collection here!
                                DocumentReference documentReference = fStore.collection("users").document(userID);
                                Map<String,Object> userMap = new HashMap<>();
                                userMap.put("username",username1); //aus dem nameedittext ausgelesener username wird als username1 in die
                                userMap.put("email",email1);
                                userMap.put("password",password1);
                                userMap.put("listenings",0); //set initial value to 0 - so kann man auch sehen, wenn user 0 listenings hat
                                userMap.put("uploads",0); //set initial value to 0 - so kann man auch sehen, wenn user 0 uploads hat
                                userMap.put("registrationdate",registrationDate);

                                //2 sets the entire user Map as a document in the realtime database under the current user's userID
                                documentReference.set(userMap).addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "user profile created. ID: " + userID, Toast.LENGTH_SHORT).show();
                                    //3 "users" is name of node in Realtime Database where you are storing the user's data (das zuvor erstellte doc.) with the user ID as the key (userid ist der document name as indicated above)
                                    DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
                                    //usersRef.child(userID).setValue(username1); //chatGPT sagt: this only sets the value of the username key for the user's ID in the users node
                                    //4 create a doc. in the Realtime Database by using the setValue() method on a DatabaseReference object that points to the path where you want to store the data
                                    usersRef.child(userID).setValue(userMap).addOnSuccessListener(aVoid1 -> {
                                        Toast.makeText(this, "user profile created. ID: " + userID, Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(getApplicationContext(), Main.class));
                                    }).addOnFailureListener(e -> {
                                        Toast.makeText(Register.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show(); //Permission denied
                                        progressBar.setVisibility(View.GONE);
                                    });
                                }).addOnFailureListener(e -> {
                                    Toast.makeText(Register.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    progressBar.setVisibility(View.GONE);
                                });
                                //TODO (A) hier den user noch anmelden!
                                signInWithEmail(email1, password1);
                                //user was already authenticated above, but redirection to main depends also on if the setting of the display name was successful: here it was, so redirect to main
                                startActivity(new Intent(getApplicationContext(), Main.class));
                            } else {
                                // Failed to set the display name
                                Toast.makeText(Register.this, "Failed to set display name", Toast.LENGTH_SHORT).show();
                                progressBar.setVisibility(View.GONE);
                            }
                        });
                    } else {
                        // User is not authenticated
                        Toast.makeText(Register.this, "User not authenticated / known", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    }

                } else {
                    Toast.makeText(Register.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                }


            });*/
        });




        //damit der ENTER button als REGISTER button fungiert
        passwordedittext.setOnKeyListener((v, keyCode, event) -> {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                InputMethodManager imm = (InputMethodManager) getBaseContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                mRegisterBtn.performClick();
                return true;
            }
            return false;
        });




    }//________________________ Ende onCreate __________________________________


    /*private void signInWithEmail(String emailOrUsername, String password) {
        fAuth.signInWithEmailAndPassword(emailOrUsername, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                startActivity(new Intent(getApplicationContext(), Main.class));
                finish(); //registeractivity muss gefinisht werden, man will ja nicht in den register-screen zurückswipen aus versehen! einmal raus, immer raus!
            } else {
                Toast.makeText(Register.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show(); //TODO(C) might produce NPE
            }
        });
    }*/





}


