package com.example.remember.categories;

import androidx.annotation.NonNull;

public class Category {
    //TODO kannweg
    private int index;
    private String name;
    private String tag;

    // empty constructor required for MongoDB Data Access POJO codec compatibility
    public Category() {}

    public Category(int index, String name, String tag) {
        this.index = index;
        this.name = name;
        this.tag = tag;
    }
    
    //index
    public Integer getIndex() { return index; }
    public void setIndex(Integer index) { this.index = index; }
    
    //name
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    //tag
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    @NonNull
    @Override
    public String toString() {
        return "CategoryManager [index=" + index + "], " +
                "name=" + name + ", " +
                "tag=" + tag;
    }

}//END_______________________________________________________________________________________