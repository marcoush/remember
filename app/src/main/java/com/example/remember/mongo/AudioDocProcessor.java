package com.example.remember.mongo;

import android.util.Log;

import com.example.remember.recycler_and_scrollviews.Model;

import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.realm.mongodb.mongo.iterable.MongoCursor;

public class AudioDocProcessor {
    /**
     * this class is for all queries that read in multiple docs (docset), e.g. search query in searchFrag, query for foryou/recent memos in homeFrag and query for fitting memos of datesofmemos in profileFrag
     */
    private static final String TAG = "AudioDocProcessor";
    public static List<Model> modelListrecent = new ArrayList<>();
    public static List<Model> modelListforyou = new ArrayList<>();
    public static List<Model> modelListsearch = new ArrayList<>();
    public static List<Model> modelListuploaded = new ArrayList<>();
    public static List<Model> modelListlistened = new ArrayList<>();

    public static List<String> alreadyQueriedmemosrecent = new ArrayList<>();
    public static List<String> alreadyQueriedmemosforyou = new ArrayList<>();
    public static List<String> alreadyQueriedmemossearch = new ArrayList<>();
    //private static List<String> alreadyQueriedmemosuploaded = new ArrayList<>(); //ne
    //private static List<String> alreadyQueriedmemoslistened = new ArrayList<>();

    private static Document lastvisibledocrecent;
    private static Document lastvisibledocforyou;
    private static Document lastvisibledocsearch;
    //private static Document lastvisibledocuploaded; //ne
    //private static Document lastvisibledoclistened;
    static String modelType;
    static Model modelinstance;

    public static void processAudioDocandaddtomodelList(MongoCursor<Document> docset, String modeltype, int homequerymode, boolean querynextpage) {
        Log.d(TAG, "processAudioDocandaddtomodelList called, type: " + modeltype +
                "\nhomequerymode:" + homequerymode + " (0:NONE, 1:RECENT, 2:FORYOU_allprefcat, 3:FORYOU_2of3prefcat, 4:FORYOU_oneofprefcat, 5:FORYOU_noprefcat)" +
                "\nquerynextpage:" + querynextpage); //TODO 15.1. querynextpage auch für home wichtig! hier anpassen
        modelType = modeltype; //home; search; uploaded; listened
        List<Model> newItems = new ArrayList<>(); //TODO debug 1/9

        switch (modelType) {
            //1 home
            case "home" -> {
                //(info) homequerymode 0 would never happen bc. I wouldn't call this method then
                if (homequerymode == 1) { //recent
                    //modelListrecent.clear(); //wird stattdessen gecleart, nachdem adapter updated wurde (modellist shall consist of only NEW entries)
                    while (docset.hasNext()) {
                        Document doc = docset.next();
                        String id = doc.getString("_id");
                        Log.v(TAG, "successfully found a recent doc: " + doc.getString("title") + " ("+id+")");
                        //4 add doc data to model and then to modellistrecent
                        modelinstance = getmodelinstance(doc, "home");
                        if (!alreadyQueriedmemosrecent.contains(id)) alreadyQueriedmemosrecent.add(id);
                        lastvisibledocrecent = doc;
                        addtomodellist(modelListrecent, modelinstance, newItems);
                    }
                }
                else { //foryou
                    //modelListforyou.clear(); //wird stattdessen gecleart, nachdem adapter updated wurde
                    while (docset.hasNext()) {
                        Document doc = docset.next();
                        String id = doc.getString("_id");
                        Log.v(TAG, "successfully found a foryou doc: " + doc.getString("title") + " ("+id+")");
                        modelinstance = getmodelinstance(doc, "home");
                        CatQueries.foryoumemoshavebeenqueried = true;
                        if (!alreadyQueriedmemosforyou.contains(id)) alreadyQueriedmemosforyou.add(id);
                        lastvisibledocforyou = doc;
                        //modelListforyou.add(modelinstance);
                        addtomodellist(modelListforyou, modelinstance, newItems);
                        //wenn hier zu wenige gefunden, dann in Main/homeFrag neu querien (aber not in this class! :P 16.11.23)
                        /*
                        *                     //
                    //Log.d(TAG, "modellistlistened: " + modelListlistened);
                    //nur wenn kein Eintrag in modellistlistened denselben Titel wie der Titel der aktuellen modelinstance hat, dann hinzufügen zum RV
                    //Log.d(TAG, "ist " + modelinstance.getTitle() + " schon in modellistlis?");
                    if (modelListsearch.isEmpty()) {
                        //Log.d(TAG, modelinstance.getTitle() + " added to empty modellistlis");
                        //wenn modellistlistened eh noch keine Einträge enthält, einfach hinzufügen..
                        addtomodellist(modelListsearch, modelinstance, newItems);
                    } else {
                        //wenn schon Einträge hat, dann checken, ob modelinstance schon drin is:
                        boolean alreadyinmodellist = false;
                        for (Model model : modelListsearch) {
                            //Log.d(TAG, model.getTitle() + " wird gecheckt");
                            if (Objects.equals(model.getID(), modelinstance.getID())) {
                                //Log.d(TAG, modelinstance.getTitle() + " schon in modellistlis! ➝ don't add again");
                                alreadyinmodellist = true;
                                break;
                            }
                        }
                        if (!alreadyinmodellist) {
                            //Log.d(TAG, modelinstance.getTitle() + " noch nicht in modellistlis ➝ add");
                            //wenn titel noch nicht in modellistlis ist:
                            addtomodellist(modelListsearch, modelinstance, newItems);
                        }
                    }
                    //
                        * */ //TODO 10.1. kommt hier ws auch hin, damit sich modellist nicht vervielfältigt & resettet oder soll sie das idk bei home
                    }
                }
            }
            //2 search
            case "search" -> {
                //modelListsearch.clear(); //wird stattdessen gecleart, nachdem adapter updated wurde
                if (!querynextpage) {//overwrite modellist!
                    Log.d(TAG, "querynextpage false ➝ clear modellist before adding new entries");
                    modelListsearch.clear();
                }
                while (docset.hasNext()) {
                    Document doc = docset.next();
                    String id = doc.getString("_id");
                    Log.v(TAG, "successfully found a search doc: " + doc.getString("title") + " ("+id+")");
                    modelinstance = getmodelinstance(doc, "search");
                    CatQueries.searchmemoshavebeenqueried = true;
                    if (!alreadyQueriedmemossearch.contains(id)) alreadyQueriedmemossearch.add(id);
                    lastvisibledocsearch = doc;

                    //
                    //Log.d(TAG, "modellistlistened: " + modelListlistened);
                    //nur wenn kein Eintrag in modellistlistened denselben Titel wie der Titel der aktuellen modelinstance hat, dann hinzufügen zum RV
                    //Log.d(TAG, "ist " + modelinstance.getTitle() + " schon in modellistlis?");
                    if (modelListsearch.isEmpty()) {
                        Log.d(TAG, "modelListsearch is emtpy");
                        //Log.d(TAG, modelinstance.getTitle() + " added to empty modellistlis");
                        //wenn modellistlistened eh noch keine Einträge enthält, einfach hinzufügen..
                        addtomodellist(modelListsearch, modelinstance, newItems); //s (empty)
                    } else {
                        Log.d(TAG, "modelListsearch is NOT emtpy");
                        //wenn schon Einträge hat, dann checken, ob modelinstance schon drin is:
                        boolean alreadyinmodellist = false;
                        for (Model model : modelListsearch) {
                            //Log.d(TAG, model.getTitle() + " wird gecheckt");
                            if (Objects.equals(model.getID(), modelinstance.getID())) {
                                //Log.d(TAG, modelinstance.getTitle() + " schon in modellist! ➝ don't add again");
                                alreadyinmodellist = true;
                                break;
                            }
                        }
                        if (!alreadyinmodellist) {
                            Log.d(TAG, modelinstance.getTitle() + " not yet in modellist ➝ add");
                            //sonst titel in modellist hinzufüge:
                            addtomodellist(modelListsearch, modelinstance, newItems); //s (add)
                        }
                    }
                    //
                }
            }
            //3 uploaded
            case "uploaded" -> {
                //modelListuploaded.clear(); //wird stattdessen gecleart, nachdem adapter updated wurde
                while (docset.hasNext()) {
                    Document doc = docset.next();  Log.v(TAG, "Successfully found an uploaded doc: " + doc.getString("title"));
                    modelinstance = getmodelinstance(doc, "uploaded");
                    //Log.d(TAG, "modellistuploaded: " + modelListuploaded);
                    //nur wenn kein Eintrag in modellistuploaded denselben Titel wie der Titel der aktuellen modelinstance hat, dann hinzufügen zum RV
                    //Log.d(TAG, "ist " + modelinstance.getTitle() + " schon in modellistupl?");
                    if (modelListuploaded.isEmpty()) {
                        //Log.d(TAG, modelinstance.getTitle() + " added to empty modellistupl");
                        //wenn modellistuploaded eh noch keine Einträge enthält, einfach hinzufügen..
                        addtomodellist(modelListuploaded, modelinstance, newItems); //upl (empty)
                    } else {
                        //wenn schon Einträge hat, dann checken, ob modelinstance schon drin is:
                        boolean alreadyinmodellist = false;
                        for (Model model : modelListuploaded) {
                            //Log.d(TAG, model.getTitle() + " wird gecheckt");
                            if (Objects.equals(model.getID(), modelinstance.getID())) {
                                //Log.d(TAG, modelinstance.getTitle() + " schon in modellist! ➝ don't add again");
                                alreadyinmodellist = true;
                                break;
                            }
                        }
                        if (!alreadyinmodellist) {
                            //sonst titel in modellist hinzufüge:
                            addtomodellist(modelListuploaded, modelinstance, newItems); //upl
                        }
                    }
                }
            }
            //4 listened
            case "listened" -> {
                //modelListlistened.clear(); //wird stattdessen gecleart, nachdem adapter updated wurde
                while (docset.hasNext()) {
                    Document doc = docset.next();
                    Log.v(TAG, "Successfully found a listened doc: " + doc.getString("title"));
                    modelinstance = getmodelinstance(doc, "listened");
                    //
                    //Log.d(TAG, "modellistlistened: " + modelListlistened);
                    //nur wenn kein Eintrag in modellistlistened denselben Titel wie der Titel der aktuellen modelinstance hat, dann hinzufügen zum RV
                    //Log.d(TAG, "ist " + modelinstance.getTitle() + " schon in modellistlis?");
                    if (modelListlistened.isEmpty()) {
                        //Log.d(TAG, modelinstance.getTitle() + " added to empty modellistlis");
                        //wenn modellistlistened eh noch keine Einträge enthält, einfach hinzufügen..
                        addtomodellist(modelListlistened, modelinstance, newItems); // lis (empty)
                    } else {
                        //wenn schon Einträge hat, dann checken, ob modelinstance schon drin is:
                        boolean alreadyinmodellist = false;
                        for (Model model : modelListlistened) {
                            //Log.d(TAG, model.getTitle() + " wird gecheckt");
                            if (Objects.equals(model.getID(), modelinstance.getID())) {
                                //Log.d(TAG, modelinstance.getTitle() + " schon in modellist! ➝ don't add again");
                                alreadyinmodellist = true;
                                break;
                            }
                        }
                        if (!alreadyinmodellist) {
                            //sonst titel in modellist hinzufüge:
                            addtomodellist(modelListlistened, modelinstance, newItems); //lis
                        }
                    }
                    //
                }
            }
        }//end_switchy

        printDebugLogs(homequerymode, newItems);

        //Log.d(TAG, "processDocDataAndAddToModelList END");
    }

    private static void addtomodellist(List<Model> modellist, Model modelinstance, List<Model> newItems) {
        Log.d(TAG, "addtomodellist, modellistentries:"+modellist.size()+ ", newitems:"+newItems);
        modellist.add(modelinstance);
        newItems.add(modelinstance);//TODO debug 2/6

    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    private static void printDebugLogs(int querymode, List<Model> newItems) {
        switch (modelType) {
            case "home" -> {
                if (querymode == 1) {
                    Log.d(TAG, "lastvisibledocrecent is: " + lastvisibledocrecent.getString("title"));
                    Log.d(TAG, newItems.size() + " neu hinzugekommene recent memos: " + newItems);
                } else {
                    Log.d(TAG, "lastvisibledocforyou is: " + lastvisibledocforyou.getString("title"));
                    if (querymode == 2) Log.d(TAG, newItems.size() + " allprefcat foryou memos hinzugekommen: " + newItems);
                    else if (querymode == 3) Log.d(TAG, newItems.size() + " 2of3prefcat foryou memos hinzugekommen: " + newItems);
                    else if (querymode == 4) Log.d(TAG, newItems.size() + " oneofprefcat foryou memos hinzugekommen: " + newItems);
                    else if (querymode == 5) Log.d(TAG, newItems.size() + " noprefcat foryou memos hinzugekommen: " + newItems);
                }
            }
            case "search" -> {
                Log.d(TAG, "lastvisibledocsearch is: " + lastvisibledocsearch.getString("title"));
                Log.d(TAG, newItems.size() + " neu hinzugekommene search memos: " + newItems);
            }
            case "uploaded" -> {
                Log.d(TAG, newItems.size() + " neu hinzugekommene uploaded memos: " + newItems);
            }
            case "listened" -> {
                Log.d(TAG, newItems.size() + " neu hinzugekommene listened memos: " + newItems);
            }
        }

        newItems.clear(); //TODO debug 4/4
    }

    public static Document getLastVisibleDocRecent() {
        return lastvisibledocrecent;
    }

    public static Document getLastVisibleDocForyou() {
        return lastvisibledocforyou;
    }

    public static List<Model> getModelListrecent() {
        return modelListrecent;
    }

    public static List<String> getAlreadyQueriedRecentMemos() {
        return alreadyQueriedmemosrecent;
    }

    public static List<Model> getModelListforyou() {
        return modelListforyou;
    }

    public static List<String> getAlreadyQueriedForyouMemos() {
        return alreadyQueriedmemosforyou;
    }

    public static Model getmodelinstance(Document doc, String typus) {
        //typus: home,search,uploaded,listened
        modelinstance = new Model(
                doc.getString("title"),
                doc.getString("duration"),
                doc.getString("creator"),
                doc.getString("creatorid"),
                doc.getDate("date"), //TODO kein timestamp mehr in mongo...
                doc.getInteger("listeners"),
                doc.getInteger("hearts"),
                doc.getList("categories", String.class),
                doc.getString("language"),
                doc.getString("url"),
                doc.getString("_id"),
                typus
        );

        return modelinstance;
    }



}
