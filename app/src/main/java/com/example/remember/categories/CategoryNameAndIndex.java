package com.example.remember.categories;

import androidx.annotation.NonNull;

public class CategoryNameAndIndex {
    //diese klasse existiert aus folg. Grund:
    // ich benötigte Kombinationen aus category name + category id , die ich in ein Array einlesen kann. hiermit geht das, indem ich diese class ins Array einlese und dann in Publish das nutze , um
    // die associatet id zu einem selected category button zu finden :) viele grüße aus napoli maroncelli
    //(die associated id aus firestore lese ich aus, damit die kategorien immer im gleichen format composed werden, zB "party,festival,depressive" (und niemals "depressive,festival,party" oder sowas])
    private String name;
    private int index;

    public CategoryNameAndIndex(String name, int index) {
        this.name = name;
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }

    @NonNull
    @Override
    public String toString() {
        return "CategoryNameAndIndex{" +
                "name='" + name + '\'' +
                ", id=" + index +
                '}';
    }
}