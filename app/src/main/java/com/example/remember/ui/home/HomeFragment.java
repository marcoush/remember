package com.example.remember.ui.home;

import static com.example.remember.Main.prefCatHaveChanged;
import static com.example.remember.Main.prefCatList;
import static com.example.remember.mongo.AudioDocProcessor.modelListforyou;
import static com.example.remember.mongo.AudioDocProcessor.modelListrecent;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.remember.Login;
import com.example.remember.MainViewModel;
import com.example.remember.Register;
import com.example.remember.categories.CategoryManager;
import com.example.remember.databinding.FragHomeBinding;
import com.example.remember.mongo.CatQueries;
import com.example.remember.mongo.RecentQuery;
import com.example.remember.recycler_and_scrollviews.CustomAdapter;
import com.example.remember.recycler_and_scrollviews.Model;
import com.mongodb.MongoClientSettings;

import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.List;

import io.realm.mongodb.App;
import io.realm.mongodb.AppConfiguration;
import io.realm.mongodb.User;
import io.realm.mongodb.mongo.MongoClient;
import io.realm.mongodb.mongo.MongoCollection;
import io.realm.mongodb.mongo.MongoDatabase;

public class HomeFragment extends Fragment {

    //(info) ich lass das mit dem callback erstmal und mache stattdessen klassisch viewmodel + observer, weil sonst problem mit fragment-noch-nicht-erstellt-wenn-in-onCreate(Main)

    //TODO (A) youtube tutorial für scrollview horizontal der geil ist:
    // https://www.youtube.com/watch?v=4yyLeI4H1rQ&ab_channel=SainalSultan
    // (discrete scrollv [der geil wehre] geht ja ned leider wegen jcenter discontinuity oder so ne SCHEISE 10.7.)


    private static final String TAG = "home_frag";

    //binding
    private FragHomeBinding binding;

    //vm
    MainViewModel mainViewModel;

    //ui
    Button managecategories, backtologinbutton, backtoregisterbutton;
    //FloatingActionButton addpublishfab;
    PopupWindow popupWindow;
    RecyclerView rvrecent, rvforyou;
    RecyclerView.LayoutManager layoutManagerRecent, layoutManagerForyou;
    CustomAdapter adapterrecent, adapterforyou;

    //mongoDB realm initializa
    MongoDatabase mongoDatabase;
    MongoClient mongoClient;
    MongoCollection<Document> mongoUsersCollection, mongoAudiosCollection;
    //MongoNamespace mongoNamespace;
    User user;
    //UserIdentity userIdentity; //You can link multiple user identities to a single user account , erstmal not needed
    App app;
    String userid, usermail;

    //für intent zu Publish
    //private static final int PICKFILE_REQUEST_CODE = 61; //now in Main 19.11.

    //private boolean isrecentQueryingData = false; // Flag to prevent concurrent fetches //chat

    //yt tutorial (works!! https://www.youtube.com/watch?v=2-vZ1g_G1Zo&ab_channel=CheezyCode)
    Boolean isScrollingrecent = false; //yt tutorial
    Boolean isScrollingforyou = false; //yt tutorial
    int currentItemsrecent, totalItemsrecent, scrollOutItemsrecent, currentItemsforyou, totalItemsforyou, scrollOutItemsforyou; //yt tutorial
    ProgressBar progressbarrecent, progressbarforyou; //yt tutorial

    //interaction to Main for foryou rv updating when pref cat in profileFrag were changed
    private HomeFragmentListener listenerCommunicationHomefragToMain;

    //foryouqueries ARE OUTSOURced in that class bc. accessed from Main & HomeFragment
    RecentQuery recentQuery;
    //private int currentrecentmemosinrv = 1; //TDO (sis) (homeFrag) MUSS BEI SAVED INSTANCE STATE AUCH BEIBEHALTEN WERDEN!!!!
    //private int currentforyoumemosinrv = 1; //TDO (sis) (homeFrag) MUSS BEI SAVED INSTANCE STATE AUCH BEIBEHALTEN WERDEN!!!!

    //die current memos werden ja in helper class verwaltet, daher kann ich nicht einfach die numberofitemsimrv abfragen, sondern rechne hier+
    //das existierd wege der query-clase (RecentQuery, CatQueries)
    public static int currentmemosglobalforyou = 0; //TODO (sis) (homeFrag) MUSS BEI SAVED INSTANCE STATE AUCH BEIBEHALTEN WERDEN!!!!
    public static int currentmemosglobalrecent = 0; //TODO (sis) (homeFrag) MUSS BEI SAVED INSTANCE STATE AUCH BEIBEHALTEN WERDEN!!!!


    //@Override //TODO (homeFrag) (override) DOES THAT NEED TO BE HERE???
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView home");
        HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        binding = FragHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.texthomeid; //TODO (CCC) irgendwann wech
        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

//0 ui
        //TUT ES MITTLEWRIELWE (: wenn man das popupmenu vom fab geöffnet hat und dann das fragment wechselt, soll das popumenu bitte schließen "popupWindow.dismiss();" TUT ES seit popup im homefrag nur ist und nicht in ganz main
        //addpublishfab = binding.addpublishid;
        managecategories = binding.managecategoriesbuttonid;//TODO (A) (BLUB) das heir ist nur für mich, um die categories zu managen, in CategoryManager.class darf der gemeine user natürlich nicht hinein :!
        backtologinbutton = binding.backtologinbuttonid;
        backtoregisterbutton = binding.backtoregisterbuttonid;
        rvrecent = binding.rvrecentmemoriesid;
        rvforyou = binding.rvforyoumemoriesid;
        progressbarrecent = binding.progressbarrecentid;
        progressbarforyou = binding.progressbarforyouid;

//1 recyclerview & adapoter
        //List<Model> emptylist = new ArrayList<>();
        rvrecent.setHasFixedSize(false); //true if adapter changes cannot affect the size of the RecyclerView.
        layoutManagerRecent = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        rvrecent.setLayoutManager(layoutManagerRecent);
        adapterrecent = new CustomAdapter(HomeFragment.this, modelListrecent);
        rvrecent.setAdapter(adapterrecent); //(info) hier schon setten, weil später geht iwie nicht, ws wg ui thread blocking

        rvforyou.setHasFixedSize(false);
        layoutManagerForyou = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        rvforyou.setLayoutManager(layoutManagerForyou);
        adapterforyou = new CustomAdapter(HomeFragment.this, modelListforyou);
        rvforyou.setAdapter(adapterforyou);
        Log.d(TAG, "adapters initialized!");

//2 mongo
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
        CodecRegistry defaultJavaCodecRegistry = MongoClientSettings.getDefaultCodecRegistry(); //TODO (mongo) für Sorts,Updates,Filters einbauen nöitg
        mongoUsersCollection = mongoDatabase.getCollection("users").withCodecRegistry(defaultJavaCodecRegistry);
        mongoAudiosCollection = mongoDatabase.getCollection("audios").withCodecRegistry(defaultJavaCodecRegistry);
        //mongoCategoriesCollection = mongoDatabase.getCollection("categories", Category.class).withCodecRegistry(pojoCodecRegistry);


//3 buttons
        managecategories.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), CategoryManager.class);
            startActivity(intent);
        });
        backtologinbutton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), Login.class);
            startActivity(intent);
        });
        backtoregisterbutton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), Register.class);
            startActivity(intent);
        });

//4 rv onscrolllisteners
        rvrecent.addOnScrollListener(new RecyclerView.OnScrollListener() {
            //yt tutorial:
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                    isScrollingrecent = true;
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                //yt tutorial: :) tausend dank bro, es workt
                super.onScrolled(recyclerView, dx, dy); //TODO (A) brauche ich hier eig kein SUPER??? .. war vorher nicht drin (vor yt tutorial)
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                currentItemsrecent = layoutManager.getChildCount();
                totalItemsrecent = layoutManager.getItemCount();
                scrollOutItemsrecent = layoutManager.findFirstVisibleItemPosition();
                Log.d(TAG, "onScrolled Recent called" +
                        "\ndx ist:" + dx +
                        "\nisScrollingrecent ist:" + isScrollingrecent +
                        "\ncurrentItemsrecent ist:" + currentItemsrecent +
                        "\ntotalItemsrecent ist:" + totalItemsrecent +
                        "\nscrollOutItemsrecent ist:" + scrollOutItemsrecent);

                if (dx > 0 && isScrollingrecent && (currentItemsrecent + scrollOutItemsrecent == totalItemsrecent)) {
                    // User has scrolled to the end -> data fetch
                    Log.d(TAG, "onScrolled Recent: user has scrolled to the end -> queryNextPage");
                    queryNextPage("recent");
                    isScrollingrecent = false;
                }
            }
        });

        rvforyou.addOnScrollListener(new RecyclerView.OnScrollListener() {
            //yt tutorial:
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                    isScrollingforyou = true;
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                //yt tutorial: :) tausend dank bro, es workt
                super.onScrolled(recyclerView, dx, dy); //TODO (A) brauche ich hier eig kein SUPER??? .. war vorher nicht drin (vor yt tutorial)
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                currentItemsforyou = layoutManager.getChildCount();
                totalItemsforyou = layoutManager.getItemCount();
                scrollOutItemsforyou = layoutManager.findFirstVisibleItemPosition();
                Log.d(TAG, "onScrolled Foryou called"+
                        "\ndx ist:" + dx +
                        "\nisScrollingforyou ist:" + isScrollingforyou +
                        "\ncurrentItemsforyou ist:" + currentItemsforyou +
                        "\ntotalItemsforyou ist:" + totalItemsforyou +
                        "\nscrollOutItemsforyou ist:" + scrollOutItemsforyou);
                
                if (dx > 0 && isScrollingforyou && (currentItemsforyou + scrollOutItemsforyou == totalItemsforyou)) {
                    // User has scrolled to the end -> data fetch
                    Log.d(TAG, "onScrolled Foryou: user has scrolled to the end -> queryNextPage");
                    queryNextPage("foryou");
                    isScrollingforyou = false;
                }
            }
        });


//5 when homeFrag onCreateView triggers, make sure whether the foryoumodellist has been changed due to user changing pref cat in profileFrag
        if (prefCatHaveChanged == true) {
            Log.d(TAG, "prefCatHaveChanged is true -> notify Main to perform retrieveHomeForYou()");
            if (listenerCommunicationHomefragToMain != null) {
                listenerCommunicationHomefragToMain.onRetrieveSignalToReassignPrefCatListFromHomeforyou();
            }
            prefCatHaveChanged = false;
        }

        Log.d(TAG, "onCreateView END");
        return root;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onViewCreated called");
        super.onViewCreated(view, savedInstanceState);

        //1 Observe the ViewModel for RECENT and FORYOU modellists that come from Main on app start!
        this.mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        mainViewModel.getModelListRecent().observe(getViewLifecycleOwner(), this::displayrecentmemos);
        mainViewModel.getModelListForyou().observe(getViewLifecycleOwner(), this::displayforyoumemos);
        mainViewModel.getProgressBarVisibilityforyou().observe(getViewLifecycleOwner(), this::setVisibilityProgressBarforyou);//included into displayforyoumemos
        mainViewModel.getProgressBarVisibilityrecent().observe(getViewLifecycleOwner(), this::setVisibilityProgressBarrecent);//included into displayrecentmemos
        //mainViewModel.getAdapterChange().observe(getViewLifecycleOwner(), this::setforyouAdapterChange);//included into displayforyoumemos

        Log.d(TAG, "onViewCreated END");
    }

    private void setVisibilityProgressBarforyou(Integer integer) {
        Log.d(TAG, "setVisibilityProgressBarforyou called, 8:gone:" + integer); //TODO debug
        progressbarforyou.setVisibility(integer);
    }
    private void setVisibilityProgressBarrecent(Integer integer) {
        Log.d(TAG, "setVisibilityProgressBarrecent called, 8:gone:" + integer); //TODO debug
        progressbarrecent.setVisibility(integer);
    }


    @Override
    public void onStop() {
        //Called when you are no longer visible to the user. You will next receive either onRestart, onDestroy, or ...
        //... nothing, depending on later user activity.
        Log.d(TAG, "onStop called");
        /*if (popupWindow != null) { //wenn das popupWindow initiiert wurde (und offen ist.. wenns bereits dismissed ist dann happens niente)
            popupWindow.dismiss();
        }*/ //now in Main 19.11.
        super.onStop();
        Log.d(TAG, "onStop END");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }



    private void displayrecentmemos(List<Model> toaddmodellistrecent) {
        //(info) the here passed "toaddmodellistrecent" contains only the new entries :9
        Log.d(TAG, "displayrecentmemos called (ordered through VM from Main)" +
             //           "\nget the most recent "+ pageSizeHome +
             //           "\ncurrentmodelListrecent ("+modelListrecent.size()+"): "+ modelListrecent +
                "\ntoaddmodellistrecent ("+toaddmodellistrecent.size()+"): "+ toaddmodellistrecent);

        //set adapter
       // setAdapterrecent(toaddmodellistrecent); //displayrecentmemos
        //adapterrecent.notifyItemRangeInserted(modelListrecent.size(), toaddmodellistrecent.size());
        //adapterrecent.addEntries(modelListrecent, toaddmodellistrecent);
        //toaddmodellistrecent resetten, nachdem in adapter gepackt (damit nächstes Mal wieder nur neue Einträge hinzuk)
        adapterrecent.notifyDataSetChanged();

        //TODO (.clear) wo kommt clear hin?? an den Anfang von load recent data natürlich!
        //modelListrecent.clear(); //das hier zu callen, hat daz geführt, dass das notifyItemRangeInserted nicht funktioniert hat, weil letzteres anscheinend asynchronous ist und die neuen Daten nicht mehr gefunden hat oder so 093uro n 4werisjo kaum zu vasse
        //progressbarrecent.setVisibility(View.GONE);
    }


    public void displayforyoumemos(List<Model> toaddmodellistforyou) {
        //(info) the here passed "toaddmodellistforyou" contains only the new entries :9
        Log.d(TAG, "displayforyoumemos called (ordered through VM from CatQueries)" +
               // "\nget the "+ pageSizeHome +" memos best-fitting the user's pref cat" +
               // "\ncurrentmodelListforyou ("+modelListforyou.size()+"): "+ modelListforyou +
                "\ntoaddmodellistforyou ("+toaddmodellistforyou.size()+"): "+ toaddmodellistforyou);

        //set adapter
        //setAdapterforyou(toaddmodellistforyou);
        //adapterforyou.notifyItemRangeInserted(currentitemslol, toaddmodellistforyou.size());
        //adapterforyou.addEntries(modelListforyou, toaddmodellistforyou);
        //toaddmodellistforyou resetten, nachdem in adapter gepackt (damit nächstes Mal wieder nur neue Einträge hinzuk)
        adapterforyou.notifyDataSetChanged();

        //TODO (.clear) wo kommt clear hin?? an den Anfang von load recent data natürlich!
        //modelListforyou.clear();
        //progressbarforyou.setVisibility(View.GONE); //für foryou wird's in CatQueries über VM gemacht :D
    }

    //für recent & foryou
    private void queryNextPage(String rvmode) {
        Log.d(TAG, "queryNextPage called (" + rvmode + ")"); //rvmode: recent,foryou
        //TODO (memos) (rv) (fetchnextpage) if the user wants to load new memos in RV, and there are none -> notify user!!

        //zu Beginn die modellist resetten, damit nicht die alten entries nochmal angezeigt werden!
        //modelListrecent.clear(); //ne, das erzeugt im RV onScrolled u.a. Probleme

        if (rvmode.equals("recent")) {
            progressbarrecent.setVisibility(View.VISIBLE);

            recentQuery = new RecentQuery();
            recentQuery.doRecentQuery(mongoAudiosCollection, true, requireContext(), mainViewModel);
        }
        else { //"foryou"
            progressbarforyou.setVisibility(View.VISIBLE);

            CatQueries foryoucatQueries = new CatQueries();
            foryoucatQueries.doCatQueries(prefCatList, mongoAudiosCollection, true, requireContext(), mainViewModel, getView(), null, null,"foryou");
        }

    }


    //interface for commun. to Main: Attach the Listener in the Fragment's onAttach()
    @Override
    public void onResume() {
        super.onResume();
        // Reattach the listener if it's null
        if (listenerCommunicationHomefragToMain == null && getActivity() instanceof HomeFragmentListener) {
            listenerCommunicationHomefragToMain = (HomeFragmentListener) getActivity();
        }
    }

    //interface for communicating to Main (for notifying Main to re-do the method retrieveHomeForYou() when in ProfileFrag the pref cat have been changed and in HomeFrag the foryouRV needs
    // to be updated due to that [which happens over livedata VM])
    public interface HomeFragmentListener {
        void onRetrieveSignalToReassignPrefCatListFromHomeforyou();
    }

}//ENDE_________________________________________________________________________________________________________________________________________