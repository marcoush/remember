package com.example.remember.recycler_and_scrollviews;

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Date;
import java.util.List;

public class Model {
    private static final String TAG = "MODEL";
    private String title, duration, creator, creatorid, language, id, url, type;
    private List<String> categories;
    private Date date;
    private int listeners, hearts;

    /*public Model(){//vlt wird es dadurch magisch funktinoirnenefurnew934wesdio
    }*/ //TODO (A) kann das nicht weg hier?

    //TODO (info) diese Model class verfolgt ja dasselbe Prinzip wie die CategoryNameAndIndex , MemoryListeningsAndDates , ... weil sie Kombinationen von Items einer Liste ausmacht (bspw. eintrag 1 in Liste hat titel,creator,etc.)

    //TODO (A) ! in der Anzeigeliste soll natürlich geshowt werden, wenn man eine Memory bereits angehört hat
    public Model(String title, String duration, String creator, String creatorid, Date date, int listeners, int hearts, List<String> categories, String language, String url, String id, String type) {
        this.title = title;
        this.duration = duration;
        this.creator = creator;
        this.creatorid = creatorid;
        this.date = date;
        this.listeners = listeners;
        this.hearts = hearts;
        this.categories = categories;
        this.language = language;
        this.url = url;
        this.id = id;
        this.type = type; //types: "home", "search", "uploaded", "listened" (weil die RVs davon alle versch sind)
        Log.d(TAG, "title:" + title + " | duration:" + duration + " | creator:" + creator + " | creatorid:" + creatorid + " | date:" + date +
                " | listeners:" + listeners + " | hearts:" + hearts + " | categories:" + categories + " | language:" + language +
                " | id:" + id + " | url:" + url);

    }
    //das Model ist nur dazu da, die aus dem document aus Firestore ausgelesenen fields (title, duration, eccetera) auszulesen und WEITERZUGEBEN (über die getter)

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public String getDuration() {return duration;}
    public void setDuration(String duration) {this.duration = duration;}

    public String getCreator() {return creator;}
    public void setCreator(String creator) {this.creator = creator;}

    public Date getDate() {
        return date;
    }
    public void setDate(Date date) {
        this.date = date;
    }

    public int getListeners() {        return listeners;    }
    public void setListeners(int listeners) { this.listeners = listeners;    }

  public List<String> getCategories() {        return categories;    }
    public void setCategories(List<String> categories) { this.categories = categories;    }

    public String getLanguage() {        return language;    }
    public void setLanguage(String language) { this.language = language;    }

    public String getURL() {        return url;    }
    public void setURL(String url) { this.url = url;    }

    public String getID() {        return id;    }
    public void setID(String id) { this.id = id;    }

    public String getType() {return type;}
    public void setType(String type) { this.type = type;    }

    public String getCreatorid() {return creatorid;}
    public void setCreatorid(String creatorid) { this.creatorid = creatorid;    }

    public int getHearts() {        return hearts;    }
    public void setHearts(int hearts) { this.hearts = hearts;    }


    @NonNull
    @Override
    public String toString() {
        return "Model{" +
                "title='" + title + '\'' +
               // ", creator=" + creator +
                '}';
    }
}
