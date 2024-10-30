package com.example.remember.mongo;

import static com.example.remember.Main.alreadylistenedmemoidsList;

import android.util.Log;

import com.mongodb.client.model.Filters;

import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

import io.realm.mongodb.RealmResultTask;
import io.realm.mongodb.mongo.iterable.FindIterable;
import io.realm.mongodb.mongo.iterable.MongoCursor;

public class TaskBuilder {
    private static final String TAG = "TaskBuilder";


    //TODO (info) (taskBuilder) filters override each other → whenever there is another nin-filter in a method taht accesses
    // this class, i have to pass the arraylist to this class !!!
    // ... don't know what the behaviour of in-filters would behere, they shouldn't override with nin

    public static RealmResultTask<MongoCursor<Document>> buildTask(FindIterable<Document> findIterable, List<String> alreadyqueriedmemosidsList, String modeltype) {
        Log.d(TAG, "buildTask, modeltype:"+modeltype);
        RealmResultTask<MongoCursor<Document>> findTask;

        //recent o. foryou
        if (!modeltype.equals("search")) {
            //when there're alreadylistenedmemos
            if (alreadylistenedmemoidsList != null) { //alreadyListenedMemos.isEmpty() NOR gefahr, weil iiiiiirgendwie ist die manchmal null
                if (alreadyqueriedmemosidsList == null) {
                    Log.d(TAG, "alreadyListenedMemos != null ➝ filter(Filters.nin(_id, alreadyListenedMemos))");
                    findTask = findIterable.filter(Filters.nin("_id", alreadylistenedmemoidsList)).iterator();
                }
                else {
                    //wenn bei fetchNextPage eine neue query geschieht, muss eine mergedlist erstellt werden
                    Log.d(TAG, "alreadyListenedMemos & alreadyqueriedmemosidsList != null ➝ filter(Filters.nin(_id, mergedList))");
                    //1 merge alreadyQueriedmemos + alreadyListenedMemos (otherwise 2. filter overrides 1.)
                    List<String> mergedList = new ArrayList<>(alreadyqueriedmemosidsList);
                    mergedList.addAll(alreadylistenedmemoidsList);
                    findTask = findIterable.filter(Filters.nin("_id", mergedList)).iterator();
                }
            }
            //when there are no alreadylistenedmemos
            else {
                if (alreadyqueriedmemosidsList == null) {
                    Log.d(TAG, "alreadyListenedMemos & alreadyqueriedmemosidsList == null ➝ no filter");
                    findTask = findIterable.iterator();
                }
                else {
                    //wenn bei fetchNextPage eine neue query geschieht, muss eine mergedlist erstellt werden
                    Log.d(TAG, "alreadyqueriedmemosidsList != null ➝ filter(Filters.nin(_id, alreadyqueriedmemosidsList))");
                    findTask = findIterable.filter(Filters.nin("_id", alreadyqueriedmemosidsList)).iterator();
                }
            }
        }
        //search
        else {
            if (alreadyqueriedmemosidsList == null) {
                Log.d(TAG, "search: alreadyqueriedmemosidsList == null ➝ no filter");
                findTask = findIterable.iterator();
            }
            else {
                //wenn bei fetchNextPage eine neue query geschieht
                Log.d(TAG, "search: alreadyqueriedmemosidsList != null ➝ filter(Filters.nin(_id, alreadyqueriedmemosidsList))");
                findTask = findIterable.filter(Filters.nin("_id", alreadyqueriedmemosidsList)).iterator();
            }
        }
        return findTask;
    }
}