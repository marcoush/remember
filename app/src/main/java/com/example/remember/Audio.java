package com.example.remember;

import androidx.annotation.NonNull;

import java.util.Date;
import java.util.List;

public class Audio {
    private String _id;
    private String title;
    private String creator;
    private String duration;
    private Date date;
    private String language;
    private int listeners;
    private List<String> categories;
    private String url;

    // empty constructor required for MongoDB Data Access POJO codec compatibility
    public Audio() {}

    public Audio(String _id, String title, String creator, String duration, Date date, String language, int listeners, List<String> categories, String url) {
        this._id = _id;
        this.title = title;
        this.creator = creator;
        this.duration = duration;
        this.date = date;
        this.language = language;
        this.listeners = listeners;
        this.categories = categories;
        this.url = url;
    }


    //_id
    public String get_id() {
        return _id;
    }
    public void set_id(String _id) {
        this._id = _id;
    }

    //title
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    //creator
    public String getCreator() {
        return creator;
    }
    public void setCreator(String creator) {
        this.creator = creator;
    }

    //duration
    public String getDuration() {
        return duration;
    }
    public void setDuration(String duration) {
        this.duration = duration;
    }

    //date
    public Date getDate() {
        return date;
    }
    public void setDate(Date date) {
        this.date = date;
    }

    //language
    public String getLanguage() {
        return language;
    }
    public void setLanguage(String language) {
        this.language = language;
    }

    //listeners
    public int getListeners() {
        return listeners;
    }
    public void setListeners(int listeners) {
        this.listeners = listeners;
    }

    //categories
    public List<String> getCategories() {
        return categories;
    }
    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    //url
    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }

    @NonNull
    @Override
    public String toString() {
        return "Audio{" +
                //"_id=" + _id +
                ", title='" + title + '\'' +
                ", creator='" + creator + '\'' +
               /* ", duration='" + duration + '\'' +
                ", date=" + date +
                ", language='" + language + '\'' +
                ", listeners=" + listeners +
                ", categories=" + categories +
                ", url='" + url + '\'' +*/
                '}';

    }

}//END_______________________________________________________________________________________



