package com.example.remember.ui.profile;

import static com.example.remember.Main.pageSizeProfile;
import static com.example.remember.Main.username;
import static com.example.remember.mongo.AudioDocProcessor.modelListlistened;
import static com.example.remember.mongo.AudioDocProcessor.modelListuploaded;
import static com.example.remember.mongo.AudioDocProcessor.processAudioDocandaddtomodelList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.remember.R;
import com.example.remember.categories.CategoryView;
import com.example.remember.databinding.FragProfileBinding;
import com.example.remember.recycler_and_scrollviews.CustomAdapter;
import com.example.remember.recycler_and_scrollviews.Model;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.model.Sorts;

import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.realm.mongodb.App;
import io.realm.mongodb.AppConfiguration;
import io.realm.mongodb.RealmResultTask;
import io.realm.mongodb.User;
import io.realm.mongodb.mongo.MongoClient;
import io.realm.mongodb.mongo.MongoCollection;
import io.realm.mongodb.mongo.MongoDatabase;
import io.realm.mongodb.mongo.iterable.MongoCursor;

public class ProfileFragment extends Fragment {
    private static final String TAG = "profile_frag";
    private FragProfileBinding binding;

    //ui
    TextView usernametextview, registrationdatetextview, amountlistenedmemoriestextview, amountuploadedmemoriestextview;
    RecyclerView rvuploaded, rvlistened;
    RecyclerView.LayoutManager layoutManagerUploaded, layoutManagerListened;
    CustomAdapter adapteruploaded, adapterlistened;
    Button togglegraphtimebutton;
    ImageButton toggleuploadedlistenedbutton;
    View includedcategoriescontainer;

    //mongoDB realm initializa
    MongoDatabase mongoDatabase;
    MongoClient mongoClient;
    MongoCollection<Document> mongoUsersCollection, mongoAudiosCollection;
    //MongoCollection<Category> mongoCategoriesCollection;
    User user;
    App app;
    String userid, usermail;

    //ausgelesen aus dem user doc:
    Date registrationdate;
    int listenings, uploads;

    //SP
    SharedPreferences catPref, catAmountPref, graphPref; //(info) es muss catAmountPref separat geben, weil in catPref mit .getAll auf alle categories zugegriffen werden soll, aber nicht auf irgendnnen amount-int yesyes

    //VM
    ProfileViewModel profileViewModel;

    //farben für buttons
    int grayedviolet,grayedred, grayedblueish,grayeddarkred,grayedgreen,grayedlightgreen,grayedcyan,grayedorange;
    int violet,red,blueish,darkred,green,lightgreen,cyan,orange;

    //for query, retrieve only pageSizeProfile of the datesoflistenings / datesofuploads - if user scrolls to end of RV, fetch pageSizeProfile more by new query..
    List<String> datesofuploadsList, datesoflisteningsList;

    //TODO (A) (profile) saved instance state wäre nice für die Anzeigen in den RVs, damit die nicht resettet werden, wenn irgendwelche Frags neu geladen werden

    //yt tutorial (works!! https://www.youtube.com/watch?v=2-vZ1g_G1Zo&ab_channel=CheezyCode)
    Boolean isScrollingUpl = false; //yt tutorial
    Boolean isScrollingLis = false; //yt tutorial
    int currentItemsUpl, totalItemsUpl, scrollOutItemsUpl, currentItemsLis, totalItemsLis, scrollOutItemsLis; //yt tutorial
    ProgressBar progressBarlistened, progressBaruploaded; //yt tutorial

    //um track zu halten, wie viele upl/lis bereits angezeigt wurden, damit man bei fetchNextPage mittels if-clause abfragen kann, ob noch restliche überbleiben
    private int alreadyshownuploadsAmount, alreadyshownlisteningsAmount;
    private boolean alluploadsHaveBeenShown, alllisteningsHaveBeenShown;
    private int datesofuploadsAmount, datesoflisteningsAmount;

    private int currentuploadedmemosinrv = 0;
    private int currentlistenedmemosinrv = 0;



    //@Override //TODO (profileFrag) (override) DOES THAT NEED TO BE HERE???
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        //(info) hier musste ich requireActivity() nehmen (statt "this"), damit das viewmodel in profilefrag und graphweek läuft!
        Log.d(TAG,"onCreateView profil");


//-1 ui bindings
        usernametextview = binding.usernameid;
        registrationdatetextview = binding.registrationdateid;
        amountuploadedmemoriestextview = binding.amountuploadedmemoriesid;
        amountlistenedmemoriestextview = binding.amountlistenedmemoriesid;
        rvuploaded = binding.rvuploadedmemoriesid;
        rvlistened = binding.rvlistenedmemoriesid;
        toggleuploadedlistenedbutton = binding.toggleuploadedlistenedbuttonid;
        togglegraphtimebutton = binding.togglegraphbuttonid;
        progressBaruploaded = binding.progressbaruploadedid;
        progressBarlistened = binding.progressbarlistenedid;

        //cat container ._.
        includedcategoriescontainer = root.findViewById(R.id.categoriescontainerid);

//0 mongodb stuff
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
        //TODO (info) (mongo) wenn Updates,Filters usen will, implementen: -> CodecRegistry defaultJavaCodecRegistry = MongoClientSettings.getDefaultCodecRegistry();
        CodecRegistry defaultJavaCodecRegistry = MongoClientSettings.getDefaultCodecRegistry();
        //(info) sicherheitshalber die codecregistry eingebaut für Sort
        mongoUsersCollection = mongoDatabase.getCollection("users").withCodecRegistry(defaultJavaCodecRegistry);
        mongoAudiosCollection = mongoDatabase.getCollection("audios").withCodecRegistry(defaultJavaCodecRegistry);
        //mongoCategoriesCollection = mongoDatabase.getCollection("categories", Category.class).withCodecRegistry(pojoCodecRegistry);


//1 recyclerview & adapterous
        //List<Model> emptylist = new ArrayList<>();
        rvuploaded.setHasFixedSize(false);
        layoutManagerUploaded = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        rvuploaded.setLayoutManager(layoutManagerUploaded);
        adapteruploaded = new CustomAdapter(ProfileFragment.this, modelListuploaded);
        rvuploaded.setAdapter(adapteruploaded); //(info) hier schon setten, weil später geht iwie nicht, ws wg ui thread blocking

        rvlistened.setHasFixedSize(false);
        layoutManagerListened = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        rvlistened.setLayoutManager(layoutManagerListened);
        adapterlistened = new CustomAdapter(ProfileFragment.this, modelListlistened);
        rvlistened.setAdapter(adapterlistened);
        //Log.d(TAG, "adapters initialized!");

//2 username + registration date + amount of uploaded&listened memos auslesen
        getUserDataAndPlugitin();

//3 graphPref & togglebutton uploaded/listened + onclicklistener
        //1 graphPref einlesen bei onCreateView
        // in den graphPref sind alle mgl. Graphkonfigurationen gespeichert, die werden hier ausgelesen - wenn nichts in graphPref ist, dann show uploads aus letzter woche (defaultValue)
        //(info) onCreate wird ohnehin immer gecallt, wenn neu geöffnet wird das fragment
        graphPref = requireContext().getSharedPreferences("graphPreferencesOf" + userid, Context.MODE_PRIVATE);
        String graphtypefrompreferences = graphPref.getString("graphtype", "uploads_week"); //den graphtype auslesen aus graphPref, defaultValue ist "uploads_week"
        //Log.d(TAG, "graphtypefrompreferences ist: " + graphtypefrompreferences);
        //??? was marc1.1 und AUCH: togglebutton background
        // basierend darauf, welche graphPref gerad vorherrscht, wird das jew. simbolo displaiato
        toggleGraphSymbol(graphtypefrompreferences);

        //2 togglebutton uploaded/listened onclick
        // der togglebutton togglet den graph (TODO (A) + auch toggle den RV und nur 1 RV statt 2???) zw. uplods / listenngs
        toggleUploadedListened();
        //3 togglebutton time onclick
        toggleGraphTime();

//4 rv onscrolllisteners
        rvuploaded.addOnScrollListener(new RecyclerView.OnScrollListener() {
            //yt tutorial:
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                    isScrollingUpl = true;
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                Log.d(TAG, "onScrolled Uploaded called");
                //yt tutorial: :) tausend dank bro, es workt
                super.onScrolled(recyclerView, dx, dy); //TODO (A) brauche ich hier eig kein SUPER??? .. war vorher nicht drin (vor yt tutorial)
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                currentItemsUpl = layoutManager.getChildCount();
                totalItemsUpl = layoutManager.getItemCount();
                scrollOutItemsUpl = layoutManager.findFirstVisibleItemPosition();
                Log.d(TAG, "isScrollingUploaded ist:" + isScrollingUpl +
                        "\ncurrentItemsUploaded ist:" + currentItemsUpl +
                        "\ntotalItemsUploaded ist:" + totalItemsUpl +
                        "\nscrollOutItemsUploaded ist:" + scrollOutItemsUpl);

                if (dx > 0 && isScrollingUpl && (currentItemsUpl + scrollOutItemsUpl == totalItemsUpl)) {
                    // User has scrolled to the end -> data fetch
                    Log.d(TAG, "onScrolled Uploaded: user has scrolled to the end -> fetchNextPage");
                    fetchNextPage("uploaded");
                    isScrollingUpl = false;
                }
            }
        });

        rvlistened.addOnScrollListener(new RecyclerView.OnScrollListener() {
            //yt tutorial:
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                    isScrollingLis = true;
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                Log.d(TAG, "onScrolled Listened called");
                //yt tutorial: :) tausend dank bro, es workt
                super.onScrolled(recyclerView, dx, dy); //TODO (A) brauche ich hier eig kein SUPER??? .. war vorher nicht drin (vor yt tutorial)
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                currentItemsLis = layoutManager.getChildCount();
                totalItemsLis = layoutManager.getItemCount();
                scrollOutItemsLis = layoutManager.findFirstVisibleItemPosition();
                if (dx > 0 && isScrollingLis && (currentItemsLis + scrollOutItemsLis == totalItemsLis)) {
                    // User has scrolled to the end -> data fetch
                    Log.d(TAG, "onScrolled Listened: user has scrolled to the end -> fetchNextPage");
                    fetchNextPage("listened");
                    isScrollingLis = false;
                }
            }
        });



        //Log.d(TAG,"onCreateView END");
        return root;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onViewCreated profile");
        super.onViewCreated(view, savedInstanceState);

        //1 find buttons over CategoryView using fragment's context
        CategoryView categoryContainer = new CategoryView(requireContext(), userid);
        categoryContainer.getButtons(includedcategoriescontainer);
        //the new code 15.11.23 is basically to prevent this: party = includedcategoriescontainer.findViewById(R.id.party); festival = includedcategoriescontainer.findViewById(R.id.festival); ....

        //3 Call the doColors method with the buttons you want to process
        categoryContainer.doColors();
//in SP prüfen, welche categories preferred sind und dementsprechend buttons anmalen
        categoryContainer.colorizeButtonsOfPrefCat();
        //1.3 button clicks
        categoryContainer.setButtonClickListenerForAllButtons("profile");


        //Log.d(TAG, "onViewCreated END");
    }


    private void toggleGraphSymbol(String graphtypefrompreferences) {
        //baut den Graph in onCreate basierend auf graphPref
        switch (graphtypefrompreferences) {
            case "uploads_week" -> {
                profileViewModel.setGraphtype("uploads_week");
                toggleuploadedlistenedbutton.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.upload));
                togglegraphtimebutton.setText(R.string.week);
            }
            case "listenings_week" -> {
                profileViewModel.setGraphtype("listenings_week");
                toggleuploadedlistenedbutton.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.headphone2));
                togglegraphtimebutton.setText(R.string.week);
            }
            case "uploads_month" -> {
                profileViewModel.setGraphtype("uploads_month");
                toggleuploadedlistenedbutton.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.upload));
                togglegraphtimebutton.setText(R.string.month);
            }
            case "listenings_month" -> {
                profileViewModel.setGraphtype("listenings_month");
                toggleuploadedlistenedbutton.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.headphone2));
                togglegraphtimebutton.setText(R.string.month);
            }
            case "uploads_sixmonths" -> {
                profileViewModel.setGraphtype("uploads_sixmonths");
                toggleuploadedlistenedbutton.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.upload));
                togglegraphtimebutton.setText(R.string.sixmonths);
            }
            case "listenings_sixmonths" -> {
                profileViewModel.setGraphtype("listenings_sixmonths");
                toggleuploadedlistenedbutton.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.headphone2));
                togglegraphtimebutton.setText(R.string.sixmonths);
            }
        }
    }

    private void toggleGraphTime() {
        //für den onclicker
        togglegraphtimebutton.setOnClickListener(v -> {
            Log.d(TAG, "togglegraphtype clicked");
            Log.d(TAG, "graphtypefrompreferences ist vor click-event: " + graphPref.getString("graphtype", "uploads_week"));
            SharedPreferences.Editor editorGraphPref = graphPref.edit();
            //2.1 ans viewmodel wird die Änderung übergeben (und dann in Graph frag ausgelesen) und zudem werden die graphPref erneuert und zudem zudem der background geändert
            switch (graphPref.getString("graphtype", "uploads_week")) {
                case "uploads_week" -> {
                    profileViewModel.setGraphtype("uploads_month");
                    editorGraphPref.putString("graphtype", "uploads_month").apply();
                    togglegraphtimebutton.setText(R.string.month);
                }
                case "listenings_week" -> {
                    profileViewModel.setGraphtype("listenings_month");
                    editorGraphPref.putString("graphtype", "listenings_month").apply();
                    togglegraphtimebutton.setText(R.string.month);
                }
                case "uploads_month" -> {
                    profileViewModel.setGraphtype("uploads_sixmonths");
                    editorGraphPref.putString("graphtype", "uploads_sixmonths").apply();
                    togglegraphtimebutton.setText(R.string.sixmonths);
                }
                case "listenings_month" -> {
                    profileViewModel.setGraphtype("listenings_sixmonths");
                    editorGraphPref.putString("graphtype", "listenings_sixmonths").apply();
                    togglegraphtimebutton.setText(R.string.sixmonths);
                }
                case "uploads_sixmonths" -> {
                    profileViewModel.setGraphtype("uploads_week");
                    editorGraphPref.putString("graphtype", "uploads_week").apply();
                    togglegraphtimebutton.setText(R.string.week);
                }
                case "listenings_sixmonths" -> {
                    profileViewModel.setGraphtype("listenings_week");
                    editorGraphPref.putString("graphtype", "listenings_week").apply();
                    togglegraphtimebutton.setText(R.string.week);
                }
            }
            Log.d(TAG, "graphtypefrompreferences ist nach click-event: " + graphPref.getString("graphtype", "uploads_week"));
        });
    }

    private void toggleUploadedListened() {
        //für den onclicker
        toggleuploadedlistenedbutton.setOnClickListener(v -> {
            Log.d(TAG, "toggleuploadedlistenedbutton clicked");
            Log.d(TAG, "graphtypefrompreferences ist vor click-event: " + graphPref.getString("graphtype", "uploads_week"));
            SharedPreferences.Editor editorGraphPref = graphPref.edit();
            //2.1 ans viewmodel wird die Änderung übergeben (und dann in Graph frag ausgelesen) und zudem werden die graphPref erneuert und zudem zudem der background geändert
            switch (graphPref.getString("graphtype", "uploads_week")) {
                case "uploads_week" -> {
                    profileViewModel.setGraphtype("listenings_week");
                    editorGraphPref.putString("graphtype", "listenings_week").apply();
                    toggleuploadedlistenedbutton.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.headphone2));
                }
                case "listenings_week" -> {
                    profileViewModel.setGraphtype("uploads_week");
                    editorGraphPref.putString("graphtype", "uploads_week").apply();
                    toggleuploadedlistenedbutton.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.upload));
                }
                case "uploads_month" -> {
                    profileViewModel.setGraphtype("listenings_month");
                    editorGraphPref.putString("graphtype", "listenings_month").apply();
                    toggleuploadedlistenedbutton.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.headphone2));
                }
                case "listenings_month" -> {
                    profileViewModel.setGraphtype("uploads_month");
                    editorGraphPref.putString("graphtype", "uploads_month").apply();
                    toggleuploadedlistenedbutton.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.upload));
                }
                case "uploads_sixmonths" -> {
                    profileViewModel.setGraphtype("listenings_sixmonths");
                    editorGraphPref.putString("graphtype", "listenings_sixmonths").apply();
                    toggleuploadedlistenedbutton.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.headphone2));
                }
                case "listenings_sixmonths" -> {
                    profileViewModel.setGraphtype("uploads_sixmonths");
                    editorGraphPref.putString("graphtype", "uploads_sixmonths").apply();
                    toggleuploadedlistenedbutton.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.upload));
                }
            }
            Log.d(TAG, "graphtypefrompreferences ist nach click-event: " + graphPref.getString("graphtype", "uploads_week"));
        });
    }


    @SuppressLint("SetTextI18n")
    private void getUserDataAndPlugitin() {
        //(info) diese method holt tutta la user data ausm userDoc und puttet sie in textviews für die Anzeige der Profildaten

        //TODO (future) (user) Sternesystem einbauen, und hier die Druchschnittssternebewertung del Utente einlesen

        //TODO (info) ICH LASSE ES ERSTMAL, DIE SCHENKEL HIER REINZUKOPIEREN, WEIL DIE JA WEEK-MONTH-3MONTH-BEZOGEN SIND UND ICH DANN GENERELL NEU ÜBERLEGEN MÜSSTE, WIE ICH DIE METHODE AUFBAUE, ne mache ich erstmal niCHT!
        // ich gehe einfach durchs kalte Feuer und rechne hier die userbezogenen geuploadeten & gelistened audios ein (für die visuelle Anzeige im feld , wirst sehen marcoouhsh)

        //TODO (info) datewithuploadList is dasselbe wie datesofuploadsList (neue schreibweise, weil date nun zuerst kommt)

        //1 public static username aus Main
        usernametextview.setText(username);

        //2 carry out query to get user data from mongo
        //query filters: _id must be userid, datesoflistenings is sorted newest to oldest, only last max. pageSizeProfile entries of datesofuploads/datesoflistenings array are retrieved (sliced ;P)
        Document match = new Document("$match", new Document("_id", userid));
        Document sortUploads = new Document("$sort", new Document("datesofuploads", -1));
        //Document sliceUploads = new Document("$project", new Document("datesofuploads", new Document("$slice", Arrays.asList("$datesofuploads", pageSizeProfile))));
        // TODO (info) no more slicing, retrieving full list and then slice it afterwards - advantage: no more queries for fetchNextPage and no extra query in graphFrag!
        Document sortListenings = new Document("$sort", new Document("datesoflistenings", -1));
        //Document sliceListenings = new Document("$project", new Document("datesoflistenings", new Document("$slice", Arrays.asList("$datesoflistenings", pageSizeProfile))));
        //(info) for fetchNextPage queries, just start the slicing from the pageSizeProfileth last entry / or the xth last entry if $datesofuploads/$datesoflistenings contained x < pageSizeProfile entries
        List<Document> pipeline = Arrays.asList(match, sortUploads, /*sliceUploads, */ sortListenings /*, sliceListenings*/);
        RealmResultTask<Document> findTask = mongoUsersCollection.aggregate(pipeline).first(); //overkill mit den methodizings, findTask müsste ned sein, aber so sieht ma ma, wie die ganzen Sachen benannt sind
        //TODO (query) (profile) dieses .first() prüfen! i'm not shoor

        //perform query for userDoc
        //TODO (queries) (performance) there is the same userDoc query in Main. But in profileFrag this query should happen with each onCreateView not one single time at beginning ... hm
        findTask.getAsync(task -> {
            if (task.isSuccess()) {
                Log.v(TAG, "task successful, found userDoc");
                Document doc = task.get();
                //1 get user data:
                registrationdate = doc.getDate("registrationdate");
                uploads = doc.getInteger("uploads");
                listenings = doc.getInteger("listenings");
                datesofuploadsList = doc.getList("datesofuploads", String.class); //es wird jetzt die full list retrieved, von der zuallererst nur die ersten pageSizeProfile entries genomme werde, und dann bei fetchNextPage die restlichen
                //datesoflisteningsList = doc.getList("datesoflistenings", String.class); //old bitd when dolL contained compoasdstringz
                //1 get the heart-boolean (=hearto)
                List<Document> datelisteningsheartArray = doc.getList("datesoflistenings", Document.class);
                //Log.d(TAG, "datelisteningsheartArray:"+datelisteningsheartArray);
                if (datelisteningsheartArray != null) {
                    //Log.d(TAG, "datelisteningsheartArray != null");
                    if (!datelisteningsheartArray.isEmpty()) {
                        //Log.d(TAG, "datelisteningsheartArray is NOT empty");
                        for (Document datelisteningheart : datelisteningsheartArray) {
                            Log.d(TAG, "datelisteningheart:"+datelisteningheart);
                            Date date = datelisteningheart.getDate("date");
                            String id = datelisteningheart.getString("id");
                            Log.d(TAG, "date:" + date +
                                    "\nid:" + id);
                            if (datesoflisteningsList != null) {
                                datesoflisteningsList.add(date + "_" + id);
                            } else {
                                datesoflisteningsList = new ArrayList<>();
                                datesoflisteningsList.add(date + "_" + id);
                            }
                        }
                    }
                }

                //1.1 put userData in profileViewModel, so graphFrag can obtain it as soon as it has been obtained here from the async
                profileViewModel.setUserData(new ArrayList<>(List.of(datesofuploadsList, datesoflisteningsList)));

                //1.2 logs
                Log.d(TAG, "registrationdate:" + registrationdate + "\n" +
                        "listenings:" + listenings + "\n" +
                        "uploads:" + uploads + "\n");

                //2 populate TVs mit registrationdate, uplodas  & listniengs
                SimpleDateFormat outputFormatNormal = new SimpleDateFormat("dd. MMM yyyy", Locale.getDefault());//frontend
                String registrationDateFormattedString = outputFormatNormal.format(registrationdate);
                registrationdatetextview.setText(registrationDateFormattedString);
                if (uploads == 0) amountuploadedmemoriestextview.setText(R.string.nouploadedmemories);
                else amountuploadedmemoriestextview.setText(uploads + " " + getString(R.string.uploadedmemories));
                if (listenings == 0) amountlistenedmemoriestextview.setText(R.string.nolistenedmemories);
                else amountlistenedmemoriestextview.setText(listenings + " " + getString(R.string.listenedmemories));

                //3 cut datesofuploadsList & datesoflisteningsList to first pageSizeProfile entries (first, because list was retrieved upside down due to -1 sorting in query) and then call method addMetadataofMemotoModelList
                if (datesofuploadsList != null) {
                    //get list size
                    datesofuploadsAmount = datesofuploadsList.size();
                    Log.d(TAG, "datesofuploadsList contains " + datesofuploadsAmount + " entries (hpfly same as 'uploads' which is " + uploads+ ")");
                    //acc. to list size, build first uploads-RV
                    List<String> RV1datesofuploadsList;
                    if (datesofuploadsAmount > 0) {
                        //A wenn uploadsAmount > pageSizeProfile: sublist with pageSizeProfile entries
                        if (datesofuploadsAmount > pageSizeProfile) {
                            RV1datesofuploadsList = datesofuploadsList.subList(0,pageSizeProfile);
                            alreadyshownuploadsAmount = pageSizeProfile;
                        }
                        //B wenn uploadsAmount < pageSizeProfile: sublist with as many entries as are in the list
                        else {
                            RV1datesofuploadsList = datesofuploadsList.subList(0, datesofuploadsAmount);
                            alluploadsHaveBeenShown = true;
                        }
                        Log.d(TAG,"RV1datesofuploadsList: " + RV1datesofuploadsList); //TODO debug
                        addMetadataofMemotoModel(RV1datesofuploadsList, "uploaded"); //onCreate
                    } else progressBaruploaded.setVisibility(View.GONE); //wenn list 0 entries hat, disable progressBar again
                } else {
                    progressBaruploaded.setVisibility(View.GONE);
                    Log.d(TAG,"list datesofuploads ist LER -> disable progressBar again");
                }
                if (datesoflisteningsList != null) {
                    //get list size
                    datesoflisteningsAmount = datesoflisteningsList.size();
                    Log.d(TAG, "datesoflisteningsList contains " + datesoflisteningsAmount + " entries (hpfly same as 'listenings' which is " + listenings+ ")");
                    //acc. to list size, build first listenings-RV
                    List<String> RV1datesoflisteningsList;
                    if (datesoflisteningsAmount > 0) { //bzw. if (datesoflisteningsList.isEmpty())
                        //A wenn listeningsAmount > pageSizeProfile: sublist with pageSizeProfile entries
                        if (datesoflisteningsAmount > pageSizeProfile) {
                            RV1datesoflisteningsList = datesoflisteningsList.subList(0,pageSizeProfile);
                            alreadyshownlisteningsAmount = pageSizeProfile;
                        }
                        //B wenn listeningsAmount < pageSizeProfile: sublist with as many entries as are in the list
                        else {
                            RV1datesoflisteningsList = datesoflisteningsList.subList(0, datesoflisteningsAmount);
                            alllisteningsHaveBeenShown = true;
                        }
                        Log.d(TAG,"RV1datesoflisteningsList: " + RV1datesoflisteningsList); //TODO debug
                        addMetadataofMemotoModel(RV1datesoflisteningsList, "listened"); //onCreate
                    } else progressBarlistened.setVisibility(View.GONE); //wenn list 0 entries hat, disable progressBar again
                } else {
                    progressBarlistened.setVisibility(View.GONE);
                    Log.d(TAG,"list datesoflistenings ist LER -> disable progressBar again");
                }

            } else {
                Log.e(TAG, "Failed to find the doc of the user in 'users' coll: " + task.getError());
            }

        });
    }




    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //TODO (graph) ist das noch nötig?
      /*  SharedPreferences.Editor editorGraph = graphPreferences.edit();
        editorGraph.putString("graphtype", lastSelectedGraphType);
        editorGraph.apply();*/

        binding = null;
    }


    //für uploaded und listened memos Anzeige
    //TODO wo fetchNextPage wie in home einbauen?
    private void fetchNextPage(String recyclerviewmode) {
        Log.d(TAG, "fetchNextPage called (" + recyclerviewmode + ")");
        //(info) diese method ist für die RVs uploaded / listened memos vom user
        //(info) fetchNextPage is not anymore async because datesofuploadsList + datesoflisteningsList have already been queried in onCreate ...

        //TODO (memos) (rv) (fetchnextpage) if the user wants to load new memos in RV, and there are none -> notify user!!
        //TODO (methods) basically dieselbe method ist auch in Main... sol.lte ich hieraus ne class machen?

        if (recyclerviewmode.equals("uploaded")) {
            //enable progressbar while data is loading
            progressBaruploaded.setVisibility(View.VISIBLE);

            //wenn bereits alle upl gezeigt wurden und man trotzdem nach rechts scrollen will, wird kurz progressBar gezeigt, aber sonst passiert ni
            if (alluploadsHaveBeenShown) {
                Log.d(TAG, "allUploadsHaveBeenShown -> exit method");
                progressBaruploaded.setVisibility(View.GONE);
                return;
            }

            //(info) datesofuploadsList KANN NICHT 0 sein, wenn fetchNextPage über scrolllistener aufgeruve virt
            //(info) es bleiben DEFINITIV noch restliche uploads über, ansonsten wäre method schon return;ed worden
            List<String> toFetchdatesofuploadsList;
            //A wenn restlicher uploadsAmount > pageSizeProfile: sublist with pageSizeProfile entries
            if (datesofuploadsAmount - alreadyshownuploadsAmount > pageSizeProfile) {
                Log.d(TAG, "create sublist of datesofuploadsList-entries "+ alreadyshownuploadsAmount +"-"+ alreadyshownuploadsAmount + pageSizeProfile +
                        "\nalreadyshownUploadsAmount = " + alreadyshownuploadsAmount +" + "+ pageSizeProfile); //TODO debug
                toFetchdatesofuploadsList = datesofuploadsList.subList(alreadyshownuploadsAmount, alreadyshownuploadsAmount + pageSizeProfile);
                alreadyshownuploadsAmount += pageSizeProfile;
            }
            //B wenn uploadsAmount < pageSizeProfile: sublist with as many entries as remain
            else {
                Log.d(TAG, "create sublist of datesofuploadsList-entries "+ alreadyshownuploadsAmount +"-"+ datesofuploadsAmount +
                        "\nallUploadsHaveBeenShown = true"); //TODO debug
                toFetchdatesofuploadsList = datesofuploadsList.subList(alreadyshownuploadsAmount, datesofuploadsAmount);
                alluploadsHaveBeenShown = true;
            }
            addMetadataofMemotoModel(toFetchdatesofuploadsList, "uploaded"); //fetchNextPage
        }
        else { //"listened"
            progressBarlistened.setVisibility(View.VISIBLE); //enable progressbar while data is loading

            //wenn bereits alle upl gezeigt wurden und man trotzdem nach rechts scrollen will, wird kurz progressBar gezeigt, aber sonst passiert ni
            if (alllisteningsHaveBeenShown) {
                Log.d(TAG, "allListeningsHaveBeenShown -> exit method");
                progressBarlistened.setVisibility(View.GONE);
                return;
            }

            //(info) datesoflisteningsList KANN NICHT 0 sein, wenn fetchNextPage über scrolllistener aufgeruve virt
            //(info) es bleiben DEFINITIV noch restliche listenings über, ansonsten wäre method schon return;ed worden
            List<String> toFetchdatesoflisteningsList;
            //A wenn restlicher listeningsAmount > pageSizeProfile: sublist with pageSizeProfile entries
            if (datesoflisteningsAmount - alreadyshownlisteningsAmount > pageSizeProfile) {
                Log.d(TAG, "create sublist of datesoflisteningsList-entries "+ alreadyshownlisteningsAmount +"-"+ alreadyshownlisteningsAmount + pageSizeProfile +
                        "\nalreadyshownlisteningsAmount = " + alreadyshownlisteningsAmount +" + "+ pageSizeProfile); //TODO debug
                toFetchdatesoflisteningsList = datesoflisteningsList.subList(alreadyshownlisteningsAmount, alreadyshownlisteningsAmount + pageSizeProfile);
                alreadyshownlisteningsAmount += pageSizeProfile;
            }
            //B wenn listeningsAmount < pageSizeProfile: sublist with as many entries as remain
            else {
                Log.d(TAG, "create sublist of datesoflisteningsList-entries "+ alreadyshownlisteningsAmount +"-"+ datesoflisteningsAmount +
                        "\nalllisteningsHaveBeenShown = true"); //TODO debug
                toFetchdatesoflisteningsList = datesoflisteningsList.subList(alreadyshownlisteningsAmount, datesoflisteningsAmount);
                alllisteningsHaveBeenShown = true;
            }
            addMetadataofMemotoModel(toFetchdatesoflisteningsList, "listened"); //fetchNextPage

            
        }
    }


    //TODO (classization) & Faszination . es gibt eine sehr ähnliche method in Main.java (getDocDataandaddtoModelList)
    public void addMetadataofMemotoModel(List<String> datesofidsList, String type) {
        Log.d(TAG, "addMetadataofMemotoModel called, type: " + type); //type: "uploaded" o. "listened"
        //sinn dieser method: aus der ausm userDoc genommenen datesofidsList (uploaded o. listened) wird die audioid gesnackt, um die memos in der audiosColl zu finden und in die modellist zu pagge
        //AB HIER ALLES NEU:::::::::::::::::::::::: (changed from: for -> async findOne() to: async find() -> for

        //1 no. of entries in datesofidsList = no. of shown items in the RV = indexes to be fetched (for next page in RV, so the next page will start array from last index on)
        //clever: as many times this loop goes through (no. of entries in datesofidsList), as many indexes have to be fetched (so the next page will start array from last index on)

        //2 extract only the audioIDs from the datesofidsList and create new idsList
        List<String> idsList = new ArrayList<>();
        for (String dateofid : datesofidsList) {
            //ObjectId audioid = new ObjectId(parts[1]); //(info) in the mongoAudiosCollection, the _ids are NOT ANYMORE stored as ObjectIds //ObjectIds machen nur Probleme...
            idsList.add(dateofid.split("_")[1]);
        }

        //TODO (future) (mongo) (audioid) ich könnte dieser query (zumindest im Falle "listened") entgehen, wenn ich die audioid aus title,creator,duration,date zsmsetzen würde...
        //3 build query that finds all audio docs whose _id is contained in the datesofidsList
        Document query = new Document("_id", new Document("$in", idsList));
        RealmResultTask<MongoCursor<Document>> findTask = mongoAudiosCollection.find(query)
                .sort(Sorts.descending("date")).iterator();
        //TODO (future) (upl/lis RV) possibility to sort by other than #date

        //TODO 15.1. könnte ich das hier auch in catQueries hauen ?????

        findTask.getAsync(task -> {
            if (task.isSuccess()) {
                //Log.v(TAG, "success in query on all memos in the audiosColl that are in the idsList: " + idsList);
                MongoCursor<Document> docset = task.get();
                //browsing through the docset and get the doc data
                if (docset.hasNext()) {
                    processAudioDocandaddtomodelList(docset, type, 0, false);
                    if (type.equals("uploaded")) {
                        //set adapter
                        adapteruploaded.notifyDataSetChanged();
                        //setAdapteruploaded(modelListuploaded);
                        //adapteruploaded.addEntries(modelListuploaded);
                        //modelListuploaded.clear();
                    }
                    else { //"listened"
                        //set adapter
                        adapterlistened.notifyDataSetChanged();
                        //setAdapterlistened(modelListlistened);
                        //adapterlistened.addEntries(modelListlistened);
                        //modelListlistened.clear();
                    }
                }
                progressBaruploaded.setVisibility(View.GONE);
                progressBarlistened.setVisibility(View.GONE);
            } else {
                Log.e(TAG, "error in query to find all docs with those audioIDs:"+idsList+" in audiosColl -> ", task.getError());
                progressBaruploaded.setVisibility(View.GONE);
                progressBarlistened.setVisibility(View.GONE);
            }
        });
        Log.d(TAG, "addMetadataofMemotoModel END, type: " + type + " (1:uploaded, 2:listened)");
    }

    private void setAdapteruploaded(List<Model> modelListuploaded) {
        int newitems = modelListuploaded.size();
        adapteruploaded.notifyItemRangeInserted(currentuploadedmemosinrv, newitems);
        currentuploadedmemosinrv += newitems;
    }
    private void setAdapterlistened(List<Model> modelListlistened) {
        int newitems = modelListlistened.size();
        adapterlistened.notifyItemRangeInserted(currentlistenedmemosinrv, newitems);
        currentlistenedmemosinrv += newitems;
    }





}//END__________________ELF_______________________________E_EWW______________________ELCK________________