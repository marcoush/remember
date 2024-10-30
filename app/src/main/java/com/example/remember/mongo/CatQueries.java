package com.example.remember.mongo;

import static com.example.remember.Main.pageSizeHome;
import static com.example.remember.Main.pageSizeSearch;
import static com.example.remember.mongo.AudioDocProcessor.alreadyQueriedmemosforyou;
import static com.example.remember.mongo.AudioDocProcessor.alreadyQueriedmemossearch;
import static com.example.remember.mongo.AudioDocProcessor.modelListforyou;
import static com.example.remember.mongo.AudioDocProcessor.modelListsearch;
import static com.example.remember.mongo.AudioDocProcessor.processAudioDocandaddtomodelList;
import static com.example.remember.ui.home.HomeFragment.currentmemosglobalforyou;
import static com.example.remember.ui.search.SearchFragment.currentmemosglobalsearch;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.remember.MainViewModel;
import com.example.remember.R;
import com.google.android.material.snackbar.Snackbar;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import io.realm.mongodb.RealmResultTask;
import io.realm.mongodb.mongo.MongoCollection;
import io.realm.mongodb.mongo.iterable.FindIterable;
import io.realm.mongodb.mongo.iterable.MongoCursor;

public class CatQueries {
    private static final String TAG = "CatQueries";
    List<String> catList;
    MongoCollection<Document> audiosColl;
    Context context;
    View view;
    MainViewModel mainViewModel;
    private boolean queryNextPage, isQueryingData;
    private int currentmemosinmodellist, fillupamount;
    private String searchterm, mode, language;
    int pageSize;

    //boolean to decide whether to nin-filter the alreadyqueriedmemosforyou during the subsequent foryouqueries
    public static boolean foryoumemoshavebeenqueried;
    public static boolean searchmemoshavebeenqueried;
    private Bson querylang;
    private boolean nosearchresults = false;
    //private int currentforyoumemosbomba = 0; //kann ned hier hin, weil diese class ja immer neu instantiated wirt! ab damit'in Main oder homeFrag weiß noch ned


    //TODO (A) (query) (CatQueries) wenn bei 1 query nen FEHLER auftritt, dann NICHT ABBRECHEN!! SONDERN WEITERMACHEN ?
    // :D momentan wird ja gequittet DANN!!!


    public void doCatQueries(List<String> catlist,
                             MongoCollection<Document> mongoaudioscollection,
                             boolean querynextpage,
                             Context contextus,
                             MainViewModel mainviewmodel,
                             View viewol,
                             String searchtermio,
                             String selectedlang,
                             String modeo) { //modeo: foryou o. search
        Log.d(TAG, "doCatQueries, cats:" + catlist.size() +
                ", querynextpage:" + querynextpage +
                ", lang:" + selectedlang +
                ", modeo:" + modeo + "(searchterm:" + searchtermio + ")");
        pageSize = modeo.equals("foryou") ? pageSizeHome : pageSizeSearch;
        catList = catlist;
        audiosColl = mongoaudioscollection;
        context = contextus;
        mainViewModel = mainviewmodel;
        queryNextPage = querynextpage;
        view = viewol;
        searchterm = searchtermio;
        language = selectedlang; //explicity selected, i.e. in searchquery
        mode = modeo; //TODO 11.1. weiter - wenn mode=search, dann sollen Taskbuilder.buildTask nicht nötig? oder doch ???, weil es werden alreadylistenedmemos auch recycled
        Log.d(TAG, "alreadyQueriedmemosforyou: " + alreadyQueriedmemosforyou); //these lists are static
        Log.d(TAG, "alreadyQueriedmemossearch: " + alreadyQueriedmemossearch); //these lists are static
        if (catList.size() > 3) Log.e(TAG , "catlist.size NOT ALLOWED to be > 3");

        //TODO (CatQueries) (query) einbauen, dass wenn man nur 4 von 8 ergebnissen gefudnen hat, dass dann überhaupt
        // keine query mehr beim nächsten mal stattfindet -weil es ist ja dann schon bekannt, dass eine weitere
        // query zu NICHTS fyren wyrde xDE)Diosfklm,39_._.____dffq02q3467890ß32q02ekswlds

        if (queryNextPage) { //wenn aus homeFrag geöffnet wird, dann ist immer queryNextPage!
            Log.d(TAG, "isQueryingData is " + isQueryingData + " ➝ if TRUE: END METHOD DIRECTLY (queryNextPage)");
            if (isQueryingData) return; // Prevent concurrent fetches, end method
            isQueryingData = true;
        }

        //wenn keine lang selected wurde (search), dann language zu app-language setzen und nur memos in app-lang zeigen
        if (language == null) language = Locale.getDefault().getLanguage();
        Log.d(TAG, "language:"+language);
        querylang = Filters.eq("language", language);


        //if (mode.equals("foryou") && amountofprefcat > 0 || mode.equals("search") && catlist.size() > 0)
        if (catList.size() > 0) { //erstmal checken, ob überhaupt pref/searched cat da utente scelto eranoen...
            Log.d(TAG, "amountofcat > 0 ➝ page with memos fitting those cats");
            //TODO (A+) (CatQueries) only ALL cat or MINIMUM ALL cat?
            // ➝ query wäre dann $in statt $all ... $in würde auch bspw. house,party,festival,belliboy rausgeben bei pref/searched cat: house,party oder house,party,belliboy

            //TODO 11.1. weiter, queries mit $and-operator verbinden & modes & dran denge

            //1 query no1: finde memos, dessen cat = pref/searched cat
            //Document querycat = new Document("categories", new Document("$all", catList)); //TODO 15.1. changed to Bson
            Bson querycat = Filters.all("categories", catList); //1. query (all pref/searched cat) :D;
            Bson querycombined = Filters.and(querylang, querycat); //all cat
            RealmResultTask<MongoCursor<Document>> findTask = dofindTask(querycombined, "all", pageSize); //all pref/searched cat
            
            //4 perform more queries if query no1 hasn't spit out >pageSize results
            findTask.getAsync(task -> {
                //Log.d(TAG, "getAsync done: allcat"); //war nur, um zu sehen, bei welchem filter / bei welcher _id nen error auftritt
                if (task.isSuccess()) {
                    //Log.v(TAG, "successful query on docs");
                    //1 browse through the docset and get the doc data
                    MongoCursor<Document> docset = task.get();
                    //aus "foryou" kurzerhand "home" mache weil processAudioDocandaddtomodelList nimmt nur "home" an, nicht "foryou"
                    String modeltype = mode.equals("foryou") ? "home" : "search"; 
                    //nur, wenn mind. 1 doc im docset enthalten ist, retrieve docs
                    if (docset.hasNext())
                        processAudioDocandaddtomodelList(docset, modeltype, 2, queryNextPage); //query allcat (modeo: 2)    (info: if modeltype=home, homequerymode=2 will be relevant; otherwise it's just ignoreddd)
                    //else showProgressBarLonger(); //> 0 pref/searched cat , deakt. weil ja noch queries folgen können L0O0L

                    //2 wenn modelList zu wenig einträge hat, um eine seite im RV abzudecken (aktuell 8) ➝ then do second query, um den RV mit memos "aufzufüllen" (sofern mind. 2 pref/searched cat). ansonsten put modelListForYou in VM
                    if (mode.equals("foryou")) currentmemosinmodellist = modelListforyou.size(); //allcat foryou
                    else currentmemosinmodellist = modelListsearch.size(); //allcat search
                    
                    if (currentmemosinmodellist < pageSize) {
                        Log.d(TAG, "onComplete fitallcat: modelList contains only " + currentmemosinmodellist + " (<" + pageSize + ") memos: now fill up with memos which match one of the pref/searched cat");
                        if (catList.size() == 3) { //TODO (A) (CatQueries) umbenennen in if (amountofcat > 2), wenn pref/searched cat max. > 3 gesetzt wird & generell hier auffrischen
                            Log.d(TAG, "onComplete fitallcat: there are 3 pref/searched cat ➝ do max. 2 more queries");
                            queryforMemosthatcontain2of3cat(); //query 2/3
                        } else if (catList.size() == 2) {
                            Log.d(TAG, "onComplete fitallcat: there are 2 pref/searched cat ➝ do max. 1 more query");
                            queryforMemosthatfitoneofthecat(2); //query 2/2, von 1st query aus
                        } else {
                            Log.d(TAG, "onComplete fitallcat: there's only 1 pref/searched cat ➝ as many results as found will just be put in VM and if<pageSize, then ist's halt so"); //(info) wenn bei 1 pref/searched cat eben nicht alle 8 gefüllt werden können, ist es dann halt sooo...
                            //wenn diese class von homeFrag aus accessed wird, ist über VM ein Umweg, aber leupht auch
                            putModelListinVM(1); //1 pref/searched cat
                        }
                    }
                    //3 wenn modelListf genug Einträge für pageSize hat, dann let's go! ➝ Update the ViewModel with the retrieved data
                    else {
                        putModelListinVM(123); //all pref/searched cat (whether it's 1, 2 or 3)
                        if (mode.equals("foryou")) Log.d(TAG, "onComplete fitallcat: " + currentmemosinmodellist + " memos which correspond to all pref cat put into modelListforyou:" + modelListforyou); //TODO debug
                        else Log.d(TAG, "onComplete fitallcat: " + currentmemosinmodellist + " memos which correspond to all searched cat put into modelListsearch:" + modelListsearch); //TODO debug
                    }
                } else {
                    Log.e(TAG, "failed to query docs: ", task.getError());
                    disableProgressBar(500); //>1cat error
                    isQueryingData = false; //>1cat error
                }
            });
        }//END > 0 pref/searched cat
        else { //0 pref/searched cat
            if (mode.equals("foryou")) Log.d(TAG, "amountofprefcats = 0 ➝ query for random memos");
            else Log.d(TAG, "amountofsearchquerycats = 0 ➝ just regex / or if no searchterm, just _id-descending (not recent cuz alr. in home)");
            //random query.
            //1 build iterable which becomes findTask in next step
            //FindIterable<Document> findIterable; //TODO 15.1. putted in dofindTask
            //RealmResultTask<MongoCursor<Document>> findTask; //TODO 15.1. putted in dofindTask
            RealmResultTask<MongoCursor<Document>> findTask = dofindTask(querylang, "none", pageSize); //0 cat
            /*if (mode.equals("foryou")) {
                //3 build task depending on whether alreadyListenedMemos is empty or not

                /*findIterable = audiosColl.find(querylang).limit(pageSize);
                if (queryNextPage) findTask = TaskBuilder.buildTask(findIterable, alreadyQueriedmemosforyou, "foryou"); //0 cat
                else findTask = TaskBuilder.buildTask(findIterable, null, "foryou"); //0 cat
            }
            else { //"search"
                //wenn searchquerycatsList leer is, einfach regex query
                if (searchterm == null) { //keine regex query, wenn searchterm = null
                    findIterable = audiosColl.find(querylang).limit(pageSize).sort(Sorts.descending("_id"));

                } else {
                    if (!searchterm.equals("")) //áuch keine regex query, wenn searchterm = ""
                        findIterable = audiosColl.find(querylang).limit(pageSize).sort(Sorts.descending("_id"));
                    else {
                        Bson querysearch = Filters.regex("title", searchterm, "i"); //searchData
                        Bson querycombined = Filters.and(querylang, querysearch);
                        findIterable = audiosColl.find(querycombined).limit(pageSize);
                    }
                }
                findTask = TaskBuilder.buildTask(findIterable, null, "search"); //searchData
            }*/ //TODO 15.1. putted in dofindTask

            //3 perform query
            findTask.getAsync(task -> {
                //Log.d(TAG, "getAsync done: 0cat"); //war nur, um zu sehen, bei welchem filter / bei welcher _id nen error auftritt
                if (task.isSuccess()) {
                    //Log.v(TAG, "successful query on docs");
                    //(info) A MongoCursor<Document> represents the result set obtained from a query
                    //1 browse through the docset and get the doc data
                    MongoCursor<Document> docset = task.get();
                    //aus "foryou" kurzerhand "home" mache weil processAudioDocandaddtomodelList nimmt nur "home" an, nicht "foryou"
                    String modeltype = mode.equals("foryou") ? "home" : "search";
                    //nur, wenn mind. 1 doc im docset enthalten ist, retrieve docs
                    if (docset.hasNext())
                        processAudioDocandaddtomodelList(docset, modeltype, 0, queryNextPage);
                    else if (mode.equals("search")) {
                        if (!queryNextPage) {
                            Log.d(TAG, "no search results ➝ searchData: show empty search list with hint that no results, and suggest user to search on diff. lang");
                            Toast.makeText(context, R.string.nosearchresults, Toast.LENGTH_SHORT).show();
                        }
                        else {
                            Log.d(TAG, "no search results ➝ queryNextPage: just keep current list and hint that no extra results found");
                            Toast.makeText(context, R.string.noextrasearchresults, Toast.LENGTH_SHORT).show();
                        }
                        nosearchresults = true;
                    } else Log.d(TAG, "no foryou results");

                    //else showProgressBarLonger(); //0 pref/searched cat
                    //2 Update the ViewModel with the retrieved data
                    putModelListinVM(0); //0 pref/searched cat
                    if (mode.equals("foryou")) Log.d(TAG, "onComplete foryou 0 pref cat: modelListforyou finalized and put in VM"); //TODO debug
                    else Log.d(TAG, "onComplete search 0 searched cat: modelListsearch finalized and put in VM"); //TODO debug
                } else {
                    Log.e(TAG, "failed to query docs: ", task.getError());
                    disableProgressBar(500); //0cat error
                    isQueryingData = false; //0cat error
                }
            });
        }//END = 0 pref/searched cat
    }


    private void queryforMemosthatcontain2of3cat() {
        Log.d(TAG, "queryforMemosthatcontain2of3cat called (query 2/3)");

        //query 2/3: finde memos, bei denen 2 von 3 pref/searched cat vorkommen
        //1 baue eine List, in der 3 Lists sind, die die 2er-cat-combis enthalten
        List<List<String>> list2of3cat = new ArrayList<>();
        list2of3cat.add(Arrays.asList(catList.get(0), catList.get(1)));
        list2of3cat.add(Arrays.asList(catList.get(0), catList.get(2)));
        list2of3cat.add(Arrays.asList(catList.get(1), catList.get(2)));
        //Log.d(TAG, "list2of3cat is: " + list2of3cat);

        //2 query bauen, welche alle docs zulässt, dessen categories list1, list2 oder list3 enthält
        //2.1  Constructing the query using $in operator for each combination danke chat 31.10.23juhuio
        List<Document> orList = new ArrayList<>();
        for (List<String> combination : list2of3cat) {
            orList.add(new Document("categories", new Document("$all", combination)));
        }
        //2.2 Combining all $in queries using $or
        Document querycat = new Document("$or", orList); //query for 2of3 cat
        // TODO (query) (CatQueries) (2of3) prüfen, ob läuft

        //3 RV mit restlich zum kompletieren benötigten memos "auffüllen", demnach das .limit setzen
        if (mode.equals("foryou")) currentmemosinmodellist = modelListforyou.size(); //2of3cat foryou
        else currentmemosinmodellist = modelListsearch.size(); //2of3cat search
        fillupamount = pageSize - currentmemosinmodellist;
        
        //4 create findTask
        Bson querycombined = Filters.and(querylang, querycat); //2of3 cat
        RealmResultTask<MongoCursor<Document>> findTask = dofindTask(querycombined, "2of3", fillupamount); //2of3 pref/searched cat

        //TODO 16.1. die alreadyqueriedmemosforyou laufen wohl ned

        //5 perform query
        findTask.getAsync(task -> {
            //Log.d(TAG, "getAsync done: one2of3catcat"); //war nur, um zu sehen, bei welchem filter / bei welcher _id nen error auftritt
            if (task.isSuccess()) {
                //Log.v(TAG, "successfully did query on memos in the audiosColl");
                //1 browse through the docset and get the doc data
                MongoCursor<Document> docset = task.get();
                //aus "foryou" kurzerhand "home" mache weil processAudioDocandaddtomodelList nimmt nur "home" an, nicht "foryou"
                String modeltype = mode.equals("foryou") ? "home" : "search";
                //nur, wenn mind. 1 doc im docset enthalten ist, retrieve docs
                if (docset.hasNext())
                    processAudioDocandaddtomodelList(docset, modeltype, 3, queryNextPage); //query 2of3cat (mode: 3)
                else disableProgressBar(0); //2of3cat
                //2 wenn modelListforyou zu wenig einträge hat, um eine seite im RV abzudecken (aktuell 8) ➝ then do second query, um den RV mit memos "aufzufüllen". ansonsten modelListForyou in VM pagge
                if (currentmemosinmodellist < pageSize) {
                    if (mode.equals("foryou")) Log.d(TAG, "onComplete foonComplete fit2of3cat: modelListforyou contains only " + currentmemosinmodellist + " (<" + pageSize + ") memos ➝ now fill up with memos which match one of the pref/searched cat"); //TODO debug
                    else Log.d(TAG, "onComplete fit2of3cat: modellistsearch contains only " + currentmemosinmodellist + " (<" + pageSize + ") memos ➝ now fill up with memos which match one of the pref/searched cat"); //TODO debug
                    queryforMemosthatfitoneofthecat(3); //query 3/3, von 2nd query aus
                }
                else {
                    if (mode.equals("foryou")) Log.d(TAG, "onComplete fit2of3cat: modelListforyou contains " + currentmemosinmodellist + " (=" + pageSize + ") memos ➝ put modellistforyou in VMryou 0 pref cat: modelListforyou finalized and put in VM"); //TODO debug
                    else Log.d(TAG, "onComplete fit2of3cat: modellistsearch contains " + currentmemosinmodellist + " (=" + pageSize + ") memos ➝ put modellistsearch in VM"); //TODO debug
                    putModelListinVM(3); //3 pref/searched cat
                }
            } else {
                Log.e(TAG, "failed to find doc with: ", task.getError());
                disableProgressBar(0); //2of3cat error
                isQueryingData = false; //2of3cat error
            }
        });
    }

    private void queryforMemosthatfitoneofthecat(int amountofcat) { //TODO debug (int amountofcat)
        //(info) can either be 1of3 or 1of2 cat
        if (amountofcat == 2) Log.d(TAG, "queryforMemosthatfitoneofthecat called (query 2/2)"); //TODO debug
        else Log.d(TAG, "queryforMemosthatfitoneofthecat called (query 3/3)"); //TODO debug

        //1 query bauen
        Document querycat = new Document("categories", new Document("$in", catList).append("$size", 1));
        //alternative with Bson Filters: Bson querycat = Filters.and(Filters.size("categories", 1),Filters.in("categories", catList);

        //3 RV mit restlich zum kompletieren benötigten memos "auffüllen", demnach das .limit setzen
        if (mode.equals("foryou")) currentmemosinmodellist = modelListforyou.size(); //oneofcat foryou
        else currentmemosinmodellist = modelListsearch.size(); //oneofcat search
        fillupamount = pageSize - currentmemosinmodellist;

        //4 create findTask
        Bson querycombined = Filters.and(querylang, querycat); //one of cat
        RealmResultTask<MongoCursor<Document>> findTask = dofindTask(querycombined, "1ofcat", fillupamount); //1ofcat pref/searched cat

        //5 perform query
        findTask.getAsync(task -> {
            //Log.d(TAG, "getAsync done: oneofcat"); //war nur, um zu sehen, bei welchem filter / bei welcher _id nen error auftritt
            if (task.isSuccess()) {
                //Log.v(TAG, "successfully did query on foryou memos in the audiosColl");
                //1 browse through the docset and get the doc data
                MongoCursor<Document> docset = task.get();
                //aus "foryou" kurzerhand "home" mache weil processAudioDocandaddtomodelList nimmt nur "home" an, nicht "foryou"
                String modeltype = mode.equals("foryou") ? "home" : "search";
                //nur, wenn mind. 1 doc im docset enthalten ist, retrieve docs
                if (docset.hasNext())
                    processAudioDocandaddtomodelList(docset, modeltype, 4, queryNextPage); //query oneofcat (mode: 4)
                else disableProgressBar(0); //oneofcat
                //2 wenn modelList immer noch zu wenig einträge hat, um eine seite im RV abzudecken (aktuell 8), dann ist es halt so, ich kann nicht mehr tun (random memos sollen dann auch ned angezeigt werden)
                //3 ➝ in putModelListinVM wird ja auch eine Notification angezeigt, wenn nicht genug memos gefunden wurden...
                putModelListinVM(23); //2 or 3 cat (from query oneofcat)
            } else {
                Log.e(TAG, "failed to find doc with: " + task.getError());
                disableProgressBar(0); //oneofcat error
                isQueryingData = false; //oneofcat error
            }
        });
    }

    private RealmResultTask<MongoCursor<Document>> dofindTask(Bson query, String catstoquery, int fillupamountorpagesize) {
        Log.d(TAG, "dofindTask, catstoquery:"+catstoquery+" , fillupamountorpagesize:"+fillupamountorpagesize+" , mode:"+mode);
        FindIterable<Document> findIterable;
        RealmResultTask<MongoCursor<Document>> findTask;
        List<String> alreadyQueriedmemosList;
        boolean memoshavebeenqueried;

        //1 build iterable which becomes findTask in next step
        if (mode.equals("foryou")) {
            alreadyQueriedmemosList = alreadyQueriedmemosforyou;
            memoshavebeenqueried = foryoumemoshavebeenqueried;
            //findIterable
            findIterable = audiosColl.find(query).limit(fillupamountorpagesize); //TODO 15.1. (B) (foryou) (query) kein sorting?ßß
            /*//findTask
            if (catstoquery.equals("all") || catstoquery.equals("none")) { //allcat / 0cat
                //bei allcat/0cat wird einfach nur queryNextPage abgefragt
                if (queryNextPage) findTask = TaskBuilder.buildTask(findIterable, alreadyQueriedmemosforyou, "foryou"); //all cat foryou
                else findTask = TaskBuilder.buildTask(findIterable, null, "foryou"); //all cat foryou
            } else { //2of3cat / 1ofcat
                //bei 2of3cat/1ofcat wird neben queryNextPage noch foryoumemoshavebeenqueried abgefragt
                if (queryNextPage || foryoumemoshavebeenqueried) findTask = TaskBuilder.buildTask(findIterable, alreadyQueriedmemosforyou, "foryou"); //2 of 3 cat
                else findTask = TaskBuilder.buildTask(findIterable, null, "foryou"); //2 of 3 cat / 1 of cat
            }
            Log.d(TAG, "foryoumemoshavebeenqueried: " + foryoumemoshavebeenqueried);*/ //TODO 15.1. findTask combined below
        }
        else { //"search" ➝ kann bedeuten: showData, searchData o. queryNextPage
            alreadyQueriedmemosList = alreadyQueriedmemossearch;
            memoshavebeenqueried = searchmemoshavebeenqueried;
            //findIterable ➝ wenn searchquerycatsList leer is, einfach regex query
            if (searchterm == null) { //keine regex query, wenn searchterm = null
                Log.d(TAG, "searchterm = null ➝ just sort descending");
                findIterable = audiosColl.find(query).limit(fillupamountorpagesize).sort(Sorts.descending("_id")); //TODO (search) sort by _id attualmente...
            } else { // (: dieses else muss sein :)
                if (searchterm.equals("")) {
                    //áuch keine regex query, wenn searchterm = ""
                    Log.d(TAG, "searchterm = '' ➝ just sort descending");
                    findIterable = audiosColl.find(query).limit(fillupamountorpagesize).sort(Sorts.descending("_id")); //TODO (search) sort by _id attualmente...
                }
                else {
                    Log.d(TAG, "searchterm = "+searchterm+" ➝ regex");
                    Bson querysearch = Filters.regex("title", searchterm, "i");
                    Bson querycombined = Filters.and(query, querysearch);
                    findIterable = audiosColl.find(querycombined).limit(fillupamountorpagesize);
                }
            }
            /*if (catstoquery.equals("all") || catstoquery.equals("none")) { //allcat / 0cat
                //bei allcat/0cat wird einfach nur queryNextPage abgefragt
                if (queryNextPage) findTask = TaskBuilder.buildTask(findIterable, alreadyQueriedmemossearch, "foryou"); //all cat foryou
                else findTask = TaskBuilder.buildTask(findIterable, null, "foryou"); //all cat foryou
            } else { //2of3cat / 1ofcat
                //bei 2of3cat/1ofcat wird neben queryNextPage noch foryoumemoshavebeenqueried abgefragt
                if (queryNextPage || searchmemoshavebeenqueried) findTask = TaskBuilder.buildTask(findIterable, alreadyQueriedmemossearch, "foryou"); //2 of 3 cat
                else findTask = TaskBuilder.buildTask(findIterable, null, "foryou"); //2 of 3 cat / 1 of cat
            }
            Log.d(TAG, "searchmemoshavebeenqueried: " + searchmemoshavebeenqueried);*/ //TODO 15.1. findTask combined below
        }

        //2 build findTask //TODO 15.1. new::::
        //TODO 15.1. same functionality for searchquery as for foryouqueries ? concerning queryNextPage, entry lane, ...
        if (catstoquery.equals("all") || catstoquery.equals("none")) { //allcat / 0cat
            //Log.d(TAG, "catstoquery: all | none");
            //bei allcat/0cat wird einfach nur queryNextPage abgefragt
            if (queryNextPage) findTask = TaskBuilder.buildTask(findIterable, alreadyQueriedmemosList, mode); //allcat / 0cat
            else findTask = TaskBuilder.buildTask(findIterable, null, mode); //allcat / 0cat
        }
        else { //2of3cat / 1ofcat
            //Log.d(TAG, "catstoquery: 2of3cat | 1ofcat");
            //bei 2of3cat/1ofcat wird neben queryNextPage noch foryoumemoshavebeenqueried abgefragt
            if (queryNextPage || memoshavebeenqueried) findTask = TaskBuilder.buildTask(findIterable, alreadyQueriedmemosList, mode); //2of3cat / 1ofcat
            else findTask = TaskBuilder.buildTask(findIterable, null, mode); //2of3cat / 1ofcat
        }
        Log.d(TAG, mode +"memoshavebeenqueried: " + memoshavebeenqueried); //TODO kannweg 15.1. logisch, dass hier "false" kommt wurde ja noch ned gequeriet

        return findTask;
    }

    private void putModelListinVM(int amountofcat) {
        if (mode.equals("foryou")) Log.d(TAG, "putModelListinVM called" + "\ncurrentforyoumemosinhomefrag: " + currentmemosglobalforyou);
        else Log.d(TAG, "putModelListinVM called" + "\ncurrentsearchmemos: " + currentmemosglobalsearch); //TODO 15.1. muss doch weg oder weil.,.search-query geht anderz
        //if the user hasn't chose any cat, display notific to user that random memos are shown
        int allmemos, newmemos;
        String categorytypDE, categorytypEN;
        if (mode.equals("foryou")) {
            allmemos = modelListforyou.size();
            newmemos = allmemos - currentmemosglobalforyou; //(info) on Main initial start, currentmemos=0, therefore newmemos=allmemos
            categorytypDE = "Lieblingskategorien";
            categorytypEN = "preferred categories";
        } else{
            allmemos = modelListsearch.size();
            newmemos = allmemos - currentmemosglobalsearch; //(info) on Main initial start, currentmemos=0, therefore newmemos=allmemos
            categorytypDE = "Suchkategorien";
            categorytypEN = "searched categories";
        }

        //display notification for user ( this works either for openedfrommain and for openedfromhomefrag (=queryNextPage) )
        if (newmemos > 0) {
            Log.d(TAG, "newmemos > 0");
            //if the list is < pageSize, display notific to user
            if (newmemos < pageSize) {
                Log.d(TAG, "modelList contains only " + newmemos + " memos: notify user that pref/searched cat only gave out this much hits for now");
                //TODO (A) thoas Toasts are provisional
                //TODO (A) message with "MORE fo..." ➝ more, weil modellist wird doch immer genullt wieder nach jedem durchlauf oder?

                if (amountofcat == 1) {
                    disableProgressBar(500); //putModelListinVM ➝ <pageSize new memos, 1 query
                } else {
                    //2 or 3 queries take long ➝ instantly disable progressBar
                    disableProgressBar(0); //putModelListinVM ➝ new memos, 2 or 3 queries
                    if (Locale.getDefault().getLanguage().equals("de")) {
                       Toast.makeText(context, "Es wurden nur " + newmemos + " memos gefunden, die zu deinen "+ categorytypDE +" passen", Toast.LENGTH_SHORT).show();
                    }
                    else if (Locale.getDefault().getLanguage().equals("en")) {
                        Toast.makeText(context, "There have only been found " + newmemos + " memos fitting your "+ categorytypEN, Toast.LENGTH_SHORT).show();
                    }
                    //TODO (languages)
                    else {
                        Toast.makeText(context, "There have only been found " + newmemos + " memos fitting your "+ categorytypEN, Toast.LENGTH_SHORT).show(); //any other language ➝ just english
                    }
                }
            } else { //newmemos = pageSize
                Log.d(TAG, "modelList contains "+pageSize+" memos");
                if (amountofcat == 1) {
                    disableProgressBar(500); //putModelListinVM ➝ newmemos=pageSize, 1 query
                } else {
                    disableProgressBar(0); //putModelListinVM ➝ newmemos=pageSize, 2 or 3 queries
                }
            }
        }
        else {
            Log.d(TAG, "newmemos = 0");
            disableProgressBar(500); //putModelListinVM ➝ newmemos = 0, 1 query
            if (amountofcat > 1) {
                //if RV still contains 0 memos, then .. idc then the user knows that his chosen pref/searched cat are unpopular xD (I know I did everything I could to show all memos fitting them in any way...)
                if (mode.equals("foryou")) Toast.makeText(context, context.getString(R.string.nomemosfoundfittinganyofyourprefcat), Toast.LENGTH_SHORT).show();
                else Toast.makeText(context, context.getString(R.string.nomemosfoundfittinganyofyoursearchedcat), Toast.LENGTH_SHORT).show();
                //TODO (CatQueries) (display) hier auch snackbar anzeigen, mit button a) auf profile, die pref/searched cat zu wechseln ODER/UND b) die Suche stattdessen auf random memos auszuweiten
            }
        }
        if (amountofcat == 0 && mode.equals("foryou")) hintUserToDetermineprefcat(); //new memos

        //neu setzen von current memos & Update the vm with the retrieved data
        if (mode.equals("foryou")) {
            currentmemosglobalforyou = allmemos;
            Log.d(TAG, "currentforyoumemosinhomefrag updated: " + currentmemosglobalforyou);
            foryoumemoshavebeenqueried = false;
            Log.d(TAG, "modelListforyou: " + modelListforyou);
            mainViewModel.setModelListforyou(modelListforyou);
        } else {
            currentmemosglobalsearch = allmemos;
            Log.d(TAG, "currentmemosglobalsearch updated: " + currentmemosglobalsearch);
            searchmemoshavebeenqueried = false;
            Log.d(TAG, "modelListsearch: " + modelListsearch);
            //wenn nosearchresults, show empty rv (therefore pass empty modellistsearch)
            if (nosearchresults && !queryNextPage) {
                Log.d(TAG, "nosearchresults ➝ pass empty modellist in order to show empty rv");
                modelListsearch.clear(); //TODO 15.1.
            }
            mainViewModel.setModelListSearch(modelListsearch);
        }
        isQueryingData = false; //putModelListinVM
    }


    private void hintUserToDetermineprefcat() {
        Snackbar.make(view, context.getString(R.string.noprefcatchosenwarning), Snackbar.LENGTH_LONG).
                setAction(context.getString(R.string.yes), v -> {
                    //open profileFrag - funkt sogar mit (Activity) context lol!vonb23
                    NavController navController = Navigation.findNavController((Activity) context, R.id.nav_viewfragid);
                    navController.navigate(R.id.profilenavfragid);
                }).show();

        //if 0cat, give user advice to determin'em
          //  Toast.makeText(context, context.getString(R.string.nocatchosenwarning), Toast.LENGTH_SHORT).show();
    }

    private void disableProgressBar(int delay) {
        //when this method is called, there was only 1 query ➝ show the progressbarrecent for ,5 sec to give utente queit sum feetbaq
        Handler handler = new Handler();
        if (mode.equals("foryou")) handler.postDelayed(() -> mainViewModel.setProgressBarVisibilityforyou(View.GONE), delay);
        else handler.postDelayed(() -> mainViewModel.setProgressBarVisibilitysearch(View.GONE), delay);
    }


}
