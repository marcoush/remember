package com.example.remember.categories;

import java.util.ArrayList;
import java.util.List;

public class CategoriesList {
    //TODO kannweg eigentlich, dachte vlt wär nice, hier die cat list zu erstellen und global hierauf zuzugreifen, aber ist weiß ned ob nödisch
    private static final String TAG = "CategoriesList";

    //categories
    public static String
            party_en, festival_en, club_en,
            travel_en, trip_en, adventure_en, nature_en, illegal_en,
            family_en, friends_en, relationship_en,
            erasmus_en, uni_en, school_en, work_en,
            dream_en, luck_en,
            happy_en, relieved_en, inlove_en,
            sad_en, depressive_en, lonely_en, nostalgic_en,
            angry_en, desperate_en, anxious_en,

            party_de, festival_de, club_de,
            travel_de, trip_de, adventure_de, nature_de, illegal_de,
            family_de, friends_de, relationship_de,
            erasmus_de, uni_de, school_de, work_de,
            dream_de, luck_de,
            happy_de, relieved_de, inlove_de,
            sad_de, depressive_de, lonely_de, nostalgic_de,
            angry_de, desperate_de, anxious_de;

    //categoryList
    public static ArrayList<String> categoriesENList = new ArrayList<>();

    //getter method
    public static List<String> getCategoryList() {
        return categoriesENList;
    }

    /*private static List<String> partyCategories = Arrays.asList("party", "festival");
    private static List<String> peopleCategories = Arrays.asList("relationship", "friends", "family");
    private static List<String> travelCategories = Arrays.asList("adventure", "nature", "travel", "excursion", "erasmus", "illegal");
    private static List<String> workCategories = Arrays.asList("uni", "school", "work");
    private static List<String> miscCategories = Arrays.asList("dream");

    private static List<String> sadCategories = Arrays.asList("sad", "depressive", "lonely");
    private static List<String> happyCategories = Arrays.asList("happy", "lucky", "in love");
    private static List<String> relievedCategories = Arrays.asList("relieved", "nostalgic");
    private static List<String> angryCategories = Arrays.asList("angry", "anxious", "desperate");



    public static List<String> getPartyCategories() {
        return partyCategories;
    }

    public static List<String> getPeopleCategories() {
        return peopleCategories;
    }

 public static List<String> getTravelCategories() {
        return travelCategories;
    }

    public static List<String> getWorkCategories() {
        return workCategories;
    }
 public static List<String> getMiscCategories() {
        return miscCategories;
    }



    public static List<String> getSadCategories() {
        return sadCategories;
    }
 public static List<String> getHappyCategories() {
        return happyCategories;
    }

    public static List<String> getRelievedCategories() {
        return relievedCategories;
    }
 public static List<String> getAngryCategories() {
        return angryCategories;
    }
     */ //ne marcoush ciao bruttissimolo
}
