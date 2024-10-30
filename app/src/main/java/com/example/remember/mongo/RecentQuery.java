package com.example.remember.mongo;

import static com.example.remember.Main.pageSizeHome;
import static com.example.remember.mongo.AudioDocProcessor.alreadyQueriedmemosrecent;
import static com.example.remember.mongo.AudioDocProcessor.modelListrecent;
import static com.example.remember.mongo.AudioDocProcessor.processAudioDocandaddtomodelList;
import static com.example.remember.ui.home.HomeFragment.currentmemosglobalrecent;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.remember.MainViewModel;
import com.example.remember.R;
import com.mongodb.client.model.Sorts;

import org.bson.Document;

import java.util.Locale;

import io.realm.mongodb.RealmResultTask;
import io.realm.mongodb.mongo.MongoCollection;
import io.realm.mongodb.mongo.iterable.FindIterable;
import io.realm.mongodb.mongo.iterable.MongoCursor;

public class RecentQuery {
    private static final String TAG = "recentQueries";
    MongoCollection<Document> audiosColl;
    Context context;
    MainViewModel mainViewModel;
    private boolean queryNextPage;
    private boolean isQueryingData;
    private String applanguage;
    //private int currentrecentmemos = 0;


    //TODO (A) (query) (recentquery) wenn bei 1 query nen FEHLER auftritt, dann NICHT ABBRECHEN!! SONDERN WEITERMACHEN ?
    // :D momentan wird ja gequittet DANN!!!


    public void doRecentQuery(MongoCollection<Document> mongoaudioscollection, boolean querynextpage, Context contextus, MainViewModel mainviewmodel) {
        Log.d(TAG, "doRecentQuery called, querynextpage:" + querynextpage);
        audiosColl = mongoaudioscollection;
        context = contextus;
        mainViewModel = mainviewmodel;
        queryNextPage = querynextpage;
        Log.d(TAG, "alreadyQueriedmemosrecent:" + alreadyQueriedmemosrecent);

        //TODO (recentqueries) (query) einbauen, dass wenn man nur 4 von 8 ergebnissen gefudnen hat, dass dann überhaupt
        // keine query mehr beim nächsten mal stattfindet -weil es ist ja dann schon bekannt, dass eine weitere
        // query zu NICHTS fyren wyrde xDE)Diosfklm,39_._.____dffq02q3467890ß32q02ekswlds

        if (queryNextPage) { //wenn aus homeFrag geöffnet wird, dann ist immer queryNextPage!
            Log.d(TAG, "isQueryingData is " + isQueryingData + " ➝ if TRUE: END METHOD DIRECTLY (queryNextPage)");
            if (isQueryingData) return; // Prevent concurrent fetches, end method
            isQueryingData = true;
        }

        //nur memos passend zu app-language anzeigen
        applanguage = Locale.getDefault().getLanguage();
        Document languagequery = new Document("language", applanguage);

        //1 build iterable which becomes findTask in next step
        FindIterable<Document> findIterable = audiosColl.find(languagequery).limit(pageSizeHome).sort(Sorts.descending("date"));
        
        //3 build task depending on whether alreadyQueriedmemosrecent & alreadyListenedMemos is empty or not
        RealmResultTask<MongoCursor<Document>> findTask;
        if (queryNextPage) findTask = TaskBuilder.buildTask(findIterable, alreadyQueriedmemosrecent, "recent"); //recent
        else findTask = TaskBuilder.buildTask(findIterable, null, "recent"); //recent
        
        //4 perform query
        findTask.getAsync(task -> {
            if (task.isSuccess()) {
                //Log.v(TAG, "successfully did recent query");
                //1 browse through the docset and get the doc data
                MongoCursor<Document> docset = task.get();
                //nur, wenn mind. 1 doc im docset enthalten ist, retrieve docs
                if (docset.hasNext()) {
                    processAudioDocandaddtomodelList(docset, "home", 1, queryNextPage);
                    //2 Update the ViewModel with the retrieved data
                    putModelListrecentinVM(); //TODO interface besser als VM I'd say
                    Log.v(TAG, "found recent docs");
                } else {
                    Log.v(TAG, "didn't find any recent docs");
                }
            } else {
                Log.e(TAG, "failed to do query to find recent docs: ", task.getError());
            }
            //show the progressbarrecent for ,5 sec to give utente queit sum feetbaq
            isQueryingData = false;
            disableProgressBar();
        });
        
        
    }

    private void putModelListrecentinVM() {
        Log.d(TAG, "putModelListrecentinVM called");
        //amountofprefcat unused, only for TODO debug
        //if the user hasn't chose any prefcat, display notific to user that random memos are shown
        int allrecentmemos = modelListrecent.size();
        int newrecentmemos = allrecentmemos - currentmemosglobalrecent; //(info) on Main initial start, currentmemos=0, therefore newmemos=allmemos
        //this works either for openedfrommain and for openedfromhomefrag (=queryNextPage)
        if (newrecentmemos > 0) { //TODO (info) nicht nötig, weil diese method nur aufgerufen wird, wenn docset.hasNext()
            Log.d(TAG, "newrecentmemos > 0");
            //quanda i nuovi memi recenti < pageSizeHome, mostra Toast a utente
            if (newrecentmemos < pageSizeHome) {
                Log.d(TAG, "modelListrecent contains only " + newrecentmemos + " of " + pageSizeHome + " memos: notify user that prefcat only gave out this much hits for now");
                //TODO (A) thoas Toasts are provisional
                //TODO (A) message with "MORE fo..." -> more, weil modellistrecent wird doch immer genullt wieder nach jedem durchlauf oder?

                if (Locale.getDefault().getLanguage().equals("de"))
                    Toast.makeText(context, "Es wurden nur " + newrecentmemos + " kürzliche memos gefunden", Toast.LENGTH_SHORT).show();
                else if (Locale.getDefault().getLanguage().equals("en"))
                    Toast.makeText(context, "There have only been found " + newrecentmemos + " recent memos", Toast.LENGTH_SHORT).show();
                    //TODO (languages)
                else
                    Toast.makeText(context, "There have only been found " + newrecentmemos + " recent memos", Toast.LENGTH_SHORT).show(); //any other language -> just english
            } else {
                Log.d(TAG, "modelListrecent contains "+pageSizeHome+" memos");
            }
            //---> Update the ViewModel with the retrieved data
            mainViewModel.setModelListrecent(modelListrecent);
        } else { //currentrecentmemos == 0
            Log.d(TAG, "currentrecentmemos = 0");
            //(info) if the RV still isn't filled with 8 memos, then there are just no memos online otherwise it would always just show the 8 most recent (even if they're far in the past)
            Log.v(TAG, "this would never hpn, as long as there are enough memos online");
            Toast.makeText(context, context.getString(R.string.norecentmemosfound), Toast.LENGTH_SHORT).show();
        }
        //neu setzen von current memos
        currentmemosglobalrecent = allrecentmemos;

        //mainViewModel.setProgressBarVisibility(false);//included into displayrecentmemos
        //mainViewModel.setAdapterChange(true);//included into displayrecentmemos

        Log.d(TAG, "modelListrecent: " + modelListrecent);
        //➝ Update the ViewModel with the retrieved data
        mainViewModel.setModelListrecent(modelListrecent);
        //mainViewModel.setProgressBarVisibility(View.GONE); //das wird bereits in displayrecentmemos gemacht -.- ;) 189500423nov.23
    }

    private void disableProgressBar() {
        //when this method is called, there was only 1 query ➝ show the progressbarrecent for ,5 sec to give utente queit sum feetbaq
        Handler handler = new Handler();
        handler.postDelayed(() -> mainViewModel.setProgressBarVisibilityrecent(View.GONE), 350);
    }



}
