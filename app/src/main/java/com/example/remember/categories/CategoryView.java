package com.example.remember.categories;

import static com.example.remember.Main.prefCatHaveChanged;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.example.remember.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CategoryView {
    private static final String TAG = "CategoryView";
    //TODO (more categories) //TODO (more categories)
    //TODO (more categories)
    //TODO (more categories)
    //TODO (more categories)
    //TODO (more categories) xD 16.1. ist mein Datum! ich bin Janvier, nato dans janvier


    public static int maxcat = 3;

    private Context context;
    //buttons
    Button party, festival, club,
            travel, trip, adventure, nature, illegal,
            family, friends, relationship,
            erasmus, university, school, work,
            dream, luck,
            happy, relieved, inlove,
            sad, depressive, lonely, nostalgic,
            angry, desperate, anxious;
    private List<Button> buttonList; //TODO (more categories)
    //farben für buttons
    int grayedviolet,grayedred, grayedblueish,grayeddarkred,grayedgreen,grayedlightgreen,grayedcyan,grayedorange;
    int violet,red,blueish,darkred,green,lightgreen,cyan,orange;
    //container
    //View includedcategoriescontainer;

    //sp (profile)
    SharedPreferences catPref, catAmountPref; //(info) es muss catAmountPref separat geben, weil in catPref mit .getAll auf alle categories zugegriffen werden soll, aber nicht auf irgendnnen amount-int yesyes

    //searchquery cats (search)
    public static ArrayList<String> searchquerycatsList = new ArrayList<>();

    public CategoryView(Context context, String userid) {
        this.context = context;
        //initaliatzions
        catPref = context.getSharedPreferences("preferredCategoriesOf" + userid, Context.MODE_PRIVATE); //'...of userid', weil wenn man user wechselt muss es auch andere SP geben jajaja pipaop(sure14.10.23)
        catAmountPref = context.getSharedPreferences("amountOfPreferredCategoriesOf" + userid, Context.MODE_PRIVATE); //weil man max soundsoviel pref cat haben darf
    }

    public void setupCategoryView(View categoryView) {
        Log.d(TAG, "setupCategoryView");
        getButtons(categoryView);
        doColors();
        setButtonClickListenerForAllButtons("search");
    } //11.1. this can also be used when I decide to decdvive the expandable popupwindow for categorycontainer

    public void getButtons(View catcontainerview) {
        Log.d(TAG, "getButtons");
        //catcontainerview = LayoutInflater.from(context).inflate(R.layout.categories_container, null);
        //Log.d(TAG, "catcontainerview: "+ catcontainerview);

        //the new code 15.11.23 is basically to prevent this: party = catcontainerview.findViewById(R.id.party); festival = catcontainerview.findViewById(R.id.festival); ....
        //..ich habs versucht 15.11.23
        party = catcontainerview.findViewById(R.id.party);
        festival = catcontainerview.findViewById(R.id.festival);
        friends = catcontainerview.findViewById(R.id.friends);
        nature = catcontainerview.findViewById(R.id.nature);
        erasmus = catcontainerview.findViewById(R.id.abroad);
        university = catcontainerview.findViewById(R.id.university);
        illegal = catcontainerview.findViewById(R.id.illegal);
        adventure = catcontainerview.findViewById(R.id.adventure);
        relationship = catcontainerview.findViewById(R.id.relationship);
        trip = catcontainerview.findViewById(R.id.trip);
        dream = catcontainerview.findViewById(R.id.dream);
        luck = catcontainerview.findViewById(R.id.luck);
        travel = catcontainerview.findViewById(R.id.travel);
        school = catcontainerview.findViewById(R.id.school);
        family = catcontainerview.findViewById(R.id.family);
        work = catcontainerview.findViewById(R.id.work);
        happy = catcontainerview.findViewById(R.id.happy);
        inlove = catcontainerview.findViewById(R.id.inlove);
        lonely = catcontainerview.findViewById(R.id.lonely);
        desperate = catcontainerview.findViewById(R.id.desperate);
        relieved = catcontainerview.findViewById(R.id.relieved);
        sad = catcontainerview.findViewById(R.id.sad);
        depressive = catcontainerview.findViewById(R.id.depressive);
        angry = catcontainerview.findViewById(R.id.angry);
        anxious = catcontainerview.findViewById(R.id.anxious);
        nostalgic = catcontainerview.findViewById(R.id.nostalgic);
        club = catcontainerview.findViewById(R.id.club);

        Log.d(TAG, "party:" + party);
        //and put all buttons in list after assigning them
        buttonList = new ArrayList<>(Arrays.asList(party, festival, friends, nature, erasmus, university, illegal, adventure, relationship, trip, dream, luck,
                travel, school, family, work, happy, inlove, lonely, desperate, relieved, sad, depressive, angry, anxious, nostalgic, club));

        /*int[] buttonIds = {
                R.id.party, R.id.festival, R.id.friends, R.id.nature,
                R.id.erasmus, R.id.university, R.id.illegal, R.id.adventure,
                R.id.relationship, R.id.trip, R.id.dream, R.id.luck,
                R.id.travel, R.id.school, R.id.family, R.id.work,
                R.id.happy, R.id.inlove, R.id.lonely, R.id.desperate,
                R.id.relieved, R.id.sad, R.id.depressive, R.id.angry,
                R.id.anxious, R.id.nostalgic, R.id.club
        };
        Log.d(TAG, "bsp buttonId[0] is: " + buttonIds[0]);

        buttonList = new ArrayList<>(Arrays.asList(party, festival, friends, nature, erasmus, university, illegal, adventure, relationship, trip, dream, luck,
                travel, school, family, work, happy, inlove, lonely, desperate, relieved, sad, depressive, angry, anxious, nostalgic, club));
        Log.d(TAG, "bsp buttonList.get(0) is: " + buttonList.get(0));
        int i = 0;
        for (int buttonId : buttonIds ) { //int buttonId : buttonIds //int i = 0; i < buttonList.size(); i++
            buttonList.set(i, catcontainerview.findViewById(buttonId));
            i++; //(info) IMPOSSIBLE: i cannot assign the buttons by setting them inside the list to a new value. this only updates the list entry, but not the ACTUAL value of the Button ...
        }*/ //Abkürzungsversuch, hat ned functioniert
        //Log.d(TAG, "bsp buttonList.get(0) is: " + buttonList.get(0));
        //Log.d(TAG, "bsp party is: " + party);
    }

    public void doColors() {
        Log.d(TAG, "doColors called");
        // Perform color and tag setup for each button
        //3.1 grayed colors
        grayedviolet = ContextCompat.getColor(context, R.color.grayedviolet);grayedred = ContextCompat.getColor(context, R.color.grayedred);
        grayeddarkred = ContextCompat.getColor(context, R.color.grayeddarkred);grayedblueish = ContextCompat.getColor(context, R.color.grayedblueish);
        grayedorange = ContextCompat.getColor(context, R.color.grayedorange);grayedgreen = ContextCompat.getColor(context, R.color.grayedgreen);
        grayedlightgreen = ContextCompat.getColor(context, R.color.grayedlightgreen);grayedcyan = ContextCompat.getColor(context, R.color.grayedcyan);
        //3.1 highlighted colors
        violet = ContextCompat.getColor(context, R.color.violet);red = ContextCompat.getColor(context, R.color.red);
        darkred = ContextCompat.getColor(context, R.color.darkred); blueish = ContextCompat.getColor(context, R.color.blueish);
        orange = ContextCompat.getColor(context, R.color.orange);green = ContextCompat.getColor(context, R.color.green);
        lightgreen = ContextCompat.getColor(context, R.color.lightgreen);cyan = ContextCompat.getColor(context, R.color.cyan);
        //3.3 set grayed colors to buttons:
        party.setBackgroundColor(grayedviolet);        festival.setBackgroundColor(grayedviolet);        club.setBackgroundColor(grayedviolet);
        work.setBackgroundColor(grayedorange);        university.setBackgroundColor(grayedorange);        school.setBackgroundColor(grayedorange);        erasmus.setBackgroundColor(grayedorange);
        adventure.setBackgroundColor(grayedgreen);        nature.setBackgroundColor(grayedgreen);        illegal.setBackgroundColor(grayedgreen);        trip.setBackgroundColor(grayedgreen);        travel.setBackgroundColor(grayedgreen);
        friends.setBackgroundColor(grayedred);        family.setBackgroundColor(grayedred);        relationship.setBackgroundColor(grayedred);
        dream.setBackgroundColor(grayedcyan);             luck.setBackgroundColor(grayedlightgreen);
        happy.setBackgroundColor(grayedlightgreen);        inlove.setBackgroundColor(grayedlightgreen);        relieved.setBackgroundColor(grayedlightgreen);
        sad.setBackgroundColor(grayedblueish);        depressive.setBackgroundColor(grayedblueish);        nostalgic.setBackgroundColor(grayedblueish);        lonely.setBackgroundColor(grayedblueish);
        angry.setBackgroundColor(grayeddarkred);        anxious.setBackgroundColor(grayeddarkred);        desperate.setBackgroundColor(grayeddarkred);
        //3.4 set the tags for the grayed colors for the buttons
        party.setTag(grayedviolet);        festival.setTag(grayedviolet);        club.setTag(grayedviolet);
        work.setTag(grayedorange);        university.setTag(grayedorange);        school.setTag(grayedorange);        erasmus.setTag(grayedorange);
        adventure.setTag(grayedgreen);        nature.setTag(grayedgreen);        illegal.setTag(grayedgreen);        trip.setTag(grayedgreen);        travel.setTag(grayedgreen);
        friends.setTag(grayedred);        family.setTag(grayedred);        relationship.setTag(grayedred);
        dream.setTag(grayedcyan);          luck.setTag(grayedlightgreen);
        happy.setTag(grayedlightgreen);        inlove.setTag(grayedlightgreen);        relieved.setTag(grayedlightgreen);
        sad.setTag(grayedblueish);        depressive.setTag(grayedblueish);        nostalgic.setTag(grayedblueish);        lonely.setTag(grayedblueish);
        angry.setTag(grayeddarkred);        anxious.setTag(grayeddarkred);        desperate.setTag(grayeddarkred);
        //TODO (new categories) onViewCreated [ProfileFrag]
        //TODO (unzufr.) hmm... adW werden erst alle buttons grayed , dann werden die preferred cat buttons HIGHLIGHTED - ws kein problem, aber nicht die ideale 5-sterne-Lösung :(
    }

    public void colorizeButtonsOfPrefCat() {
        if (catAmountPref.getInt("amount", 0) > 0) { //erstmal checken, ob die SP überhaupt bereits existieren... - früher stand hier: if (catPref != null) ; am 14.10.23 geändert
            Log.d(TAG, "cat pref != null" + " (in onViewCreated)");
            Map<String, ?> allEntries = catPref.getAll();
            Log.d(TAG, "AKTUELLE CAT PREF :)" +
                    "\nanzahl cat pref in catPref (nicht in catAmountPref): " + allEntries.size() +
                    "\nanzahl cat pref in catAmountPref: " + catAmountPref.getInt("amount", 0) + " (hftl. dasselbe)" +
                    "\nund die pref cat sind: " + allEntries);
            //int amountofcatinPCPREF = 0; //TODO debug
            //(info) hier ist das Vorgehen, die pcPref durchzugehen und für jede cat
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                String categorystringfrompcPref = entry.getKey(); //jede category button id wird einzeln aus SP ausgelesen (loop)
                Log.d(TAG, "pref cat is: " + categorystringfrompcPref + " (in onViewCreated)");
                //von pref cat auf dazugeh. button schließen [wenn zB pref cat = "anxious", dann soll der anxious-button (heißt literally "anxious" hier) der categorybutton sein]
                Button categorybutton = switch (categorystringfrompcPref) {
                    case "party" -> party;
                    case "festival" -> festival;
                    case "club" -> club;
                    case "work" -> work;
                    case "school" -> school;
                    case "university" -> university;
                    case "erasmus" -> erasmus;
                    case "adventure" -> adventure;
                    case "illegal" -> illegal;
                    case "nature" -> nature;
                    case "travel" -> travel;
                    case "trip" -> trip;
                    case "relationship" -> relationship;
                    case "family" -> family;
                    case "friends" -> friends;
                    case "dream" -> dream;
                    case "luck" -> luck;
                    case "happy" -> happy;
                    case "relieved" -> relieved;
                    case "inlove" -> inlove;
                    case "sad" -> sad;
                    case "depressive" -> depressive;
                    case "lonely" -> lonely;
                    case "nostalgic" -> nostalgic;
                    case "angry" -> angry;
                    case "anxious" -> anxious;
                    case "desperate" -> desperate;
                    default -> null; //man muss es ja assignen.. aber in realität unnötig, weil der categorybutton wird ja über einen der folgenden if-clauses zugewiesen
                    //TODO (new categories) onViewCreated [ProfileFrag]
                };
                //Log.d(TAG, "categorybutton ist: "+ categorybutton);
                //int categorybuttonid = Integer.parseInt(categorystringfrompcPref);
                //Button categorybutton2 = requireView().findViewById(categorybuttonid); //über findviewbyid auf den button der id kommen //TODO debug
                //og.d(TAG, categorybutton2.getText().toString() + " ist in den pc pref" + " (in onViewCreated)"); //TODO debug
                //Log.d(TAG, "entry.getKey = " + entry.getKey());
                //Log.d(TAG, "entry.getValue = " + entry.getValue() + " (wenn true, dann ist " + categorybutton.getText() + " bzw. " + entry.getKey() + " in pref - wenn false, dann ned)");
                boolean isCategoryPreferred = (Boolean) entry.getValue();
                if (isCategoryPreferred) {
                    //Log.d(TAG, categorybutton.getText() + " ist in pcPref");
                    //CategoryManager is stored in SP as preferred (boolean value is true) -> highlight its respective button in oncreateview
                    //Button categorybutton = requireView().findViewById(categorybuttonid); //über findviewbyid auf den button der id kommen
                    //Log.d(TAG, categorybutton.getText().toString() + " ist in pcPref UND pref (redundant if-clause, weil was in pcPref ist, ist IMMER pref"); //NEIN STIMMT NICHT (weil wird removed): cönnte ja auch wieder de-preferred worden sein..  und dann wäre es trotzdem noch in den preferences drin i guess
                    //aus der grayed color die saturated color machen:
                    //1 retrieve current color (button is dummerweise ripple drawable -> daher kann man color nicht direkt auslesen, sondern nur indirekt übers tag, was man aktualisieren muss)
                    int currentColor = (int) categorybutton.getTag(); //(info) die tags müssen die ints der Farben sein, weil die tags mit den echten Farben in if-sentences verglichen werden!
                    //Log.d(TAG, " current color ist: " + currentColor + " (in onViewCreated)");
                    //2 set background color based on color before
                    changeBackgroundOfCatButton(categorybutton); //onCreate
                }
                //TODO (new categories) onViewCreated pcPref [ProfileFrag]
                //amountofcatinPCPREF++; //TODO debug
            }
            //Log.d(TAG, "amount of cat in pcPref = " + amountofcatinPCPREF);//TODO debug
        }
        else {//TODO debug
            Log.d(TAG, "amount of cat in catAmountPref = 0"); //TODO debug
        }
        //Log.d(TAG, "amount of cat in catAmountPref = " + catAmountPref.getInt("amount", 0)); //TODO debug
    }

    public void setButtonClickListenerForAllButtons(String mode) {
        Log.d(TAG, "setButtonClickListenerForAllButtons"); //TODO debug

        if (mode.equals("profile")) {
            //profile: add/remove cat from/to prefcat
            for (int i = 0; i < buttonList.size(); i++) {
                buttonList.get(i).setOnClickListener(buttonClickListenerProfile);
            }
        /*party.setOnClickListener(buttonClickListener);       festival.setOnClickListener(buttonClickListener);        club.setOnClickListener(buttonClickListener);
        work.setOnClickListener(buttonClickListener);          university.setOnClickListener(buttonClickListener);        school.setOnClickListener(buttonClickListener);        erasmus.setOnClickListener(buttonClickListener);
        relationship.setOnClickListener(buttonClickListener);        friends.setOnClickListener(buttonClickListener);        family.setOnClickListener(buttonClickListener);
        adventure.setOnClickListener(buttonClickListener);        trip.setOnClickListener(buttonClickListener);        travel.setOnClickListener(buttonClickListener);        illegal.setOnClickListener(buttonClickListener);        nature.setOnClickListener(buttonClickListener);
        dream.setOnClickListener(buttonClickListener);           luck.setOnClickListener(buttonClickListener);
        happy.setOnClickListener(buttonClickListener);        relieved.setOnClickListener(buttonClickListener);        inlove.setOnClickListener(buttonClickListener);
        sad.setOnClickListener(buttonClickListener);        depressive.setOnClickListener(buttonClickListener);        nostalgic.setOnClickListener(buttonClickListener);        lonely.setOnClickListener(buttonClickListener);
        anxious.setOnClickListener(buttonClickListener);        angry.setOnClickListener(buttonClickListener);        desperate.setOnClickListener(buttonClickListener);*/ //long version xD
            //TODO (new categories) onViewCreated [ProfileFrag]
        } else { //"search"
            //search: add/remove cat from/to searchquery
            for (int i = 0; i < buttonList.size(); i++) {
                buttonList.get(i).setOnClickListener(buttonClickListenerSearch);
            }

        }
    }

    View.OnClickListener buttonClickListenerProfile = v -> {
        //(info) dieser code gilt für 1 einzelnen button!
        Button clickedButton = (Button) v;
        //1 prüfen, welcher button (id, nicht text weil sonst präkitationen mit SP) geklickt wird + herausfinden ob schon in catPref:
        String clickedbuttoncategoryraw = clickedButton.getText().toString(); //button text für categoryname nehmen + umwandeln in sprachenunabhängigen categoryname (button id kan man ned nehme, weil probleme mit neu-bildung von id bei neustart etc.)
        //2 falls anderssprachige buttons: raw clickedbuttoncategory string muss umgewandelt werden in die catPref-version auf engl.
        String clickedbuttoncategory = convertCatFromDiffLangToEng(clickedbuttoncategoryraw);
        Log.d(TAG, clickedbuttoncategory + " button geklickt");

        //3 before changing prefs, retrieve current pref cat from catPref (to later on know whether the cat pref law have been amended)
        Map<String, ?> beforePrefCat = catPref.getAll();

        //boolean isCategoryPreferred = catPref.getBoolean(clickedbuttoncategory, false); //default für jede category ist nat. false wenn noch nicht hinzugefügt:)
        //das ist ALTT ALLT!!! weil wie soll ich die pref cat retrieven, wenn ich die als booleans abgespeichert hab hää vergangenheitsmarchat was sollte das denn für die heinzig14.10.23

        boolean isCatPreferred = catPref.getBoolean(clickedbuttoncategory, false);

        //4A CategoryManager was already selected, remove it from pcPref & decrement pcamountPref -1:
        //UNSINN DOCHNICHT man kann auch booleans auslesen lol xD (info) prefcatstring is a string composed out of all pref cat
        if (isCatPreferred == true) {
            Log.d(TAG,clickedbuttoncategory + " IS pref -> unpref it");
            //1 setup pref editors
            SharedPreferences.Editor editorCatPref = catPref.edit(); //(info) editor für pref, in der die booleans für categories sind (auf die dann später mit .getAll zugegriffen wird) 15.10.23 !!!! Ganz wichtig!!
            SharedPreferences.Editor editorCatAmountPref = catAmountPref.edit(); //(info) editor für amount pref, wo nur +-1 gerechnet wird, um track zu halten wie viele cat prefd sind
            //2-> .. remove clickedbuttoncategory from pcPref:
            editorCatPref.remove(clickedbuttoncategory);
            //3-> .. decrement pcamountPref by 1:
            int currentAmount = catAmountPref.getInt("amount", 0);
            editorCatAmountPref.putInt("amount", currentAmount - 1);
            //4 apply pref editors
            editorCatPref.apply();
            editorCatAmountPref.apply();
            /*Log.d(TAG, "Amount of pref cat decremented by 1 [now: " + catAmountPref.getInt("amount", 0) + "]\n" +
                    "Updated amount of pref cat = " + catAmountPref.getInt("amount", 0) + "\n" +
                    "Updated color of " + clickedbuttoncategory + " is (tag=): " + clickedButton.getTag() + "\n" +
                    clickedbuttoncategory + " category removed from pcPref\n" +
                    catPref.getAll() + " are now pref cat");*/ //TODO debug
            //5-> set background color based on color before
            changeBackgroundOfCatButton(clickedButton); //unpref cat
            //(rubbish) categ name wird in SP in jew. landessprache übernommen :( das ist dann schwierig mit auslesen und dementspr. anzeigen in oncreate zu beginn der fragment-start
            // häää, was meinst du vergangenheitsmarc, es ist doch gelöst das Problem: mmit dieser method hier, no? convertCatFromDiffLangToEng()
        }
        //4B CategoryManager is not preferred yet --> add it to pcPref & increment pcamountPref +1 (IF not already maxcat cat selected as pref!)
        else { //if (isCatPreferred == false)
            Log.d(TAG,clickedbuttoncategory + " is NOT pref -> ADD to pref (if not already maxcat pref ones)");
            //1B.1 aus user pref auslesen, ob schon maxcat lieblingskategorien ausgewählt
            int currentAmount = catAmountPref.getInt("amount", 0); //returns 0 if amount doesn't exist which makes sense because then the amount of pc IS null
            //Log.d(TAG,"current amount of pref cat:" + currentAmount);//TODO debug
            if (currentAmount < maxcat) {
                //Log.d(TAG,"current amount of pref cat < maxcat [" + currentAmount + "] -> ADD " + clickedbuttoncategory  + " to pref"); //TODO debug
                //1 setup pref editors
                SharedPreferences.Editor editorCatPref = catPref.edit(); //(info) editor für pref, in der die booleans für categories sind (auf die dann später mit .getAll zugegriffen wird) 15.10.23 !!!! Ganz wichtig!!
                SharedPreferences.Editor editorCatAmountPref = catAmountPref.edit(); //(info) editor für amount pref, wo nur +-1 gerechnet wird, um track zu halten wie viele cat prefd sind
                //2-> wenn weniger als maxcat, add +1 amount
                editorCatAmountPref.putInt("amount", currentAmount + 1);
                //3-> ... und füge cat der Ursprungsliste hinzu
                editorCatPref.putBoolean(clickedbuttoncategory, true);
                //4 apply pref editors
                editorCatPref.apply();
                editorCatAmountPref.apply();
                /*Log.d(TAG, "Amount of pref cat incremented by 1 [now: " + catAmountPref.getInt("amount", 0) + "]\n" +
                        "Updated amount of pref cat = " + catAmountPref.getInt("amount", 0) + "\n" +
                        "Updated color of " + clickedbuttoncategory + " is (tag=): " + clickedButton.getTag() + "\n" +
                        clickedbuttoncategory + " category added to pcPref\n" +
                        catPref.getAll() + " are now pref cat");*///TODO debug
                //5-> set background color based on color before
                changeBackgroundOfCatButton(clickedButton); //pref cat
            }
            else {
                //-> wenn maxcat o. mehr -> mehr GEHT NED
                Toast.makeText(context,R.string.onlymaxcatpreferredcategoriesALLOWED, Toast.LENGTH_SHORT).show();
            }
        }

        //6 has the user changed pref cat?
        Map<String, ?> afterPrefCat = catPref.getAll();
        //Log.d(TAG,"beforePrefCat:" + beforePrefCat + "\nafterPrefCat:" + afterPrefCat);//TODO debug
        if (!(afterPrefCat == beforePrefCat)) prefCatHaveChanged = true;
    };

    View.OnClickListener buttonClickListenerSearch = v -> {
        Button clickedButton = (Button) v;
        //1 prüfen, welcher button (id, nicht text weil sonst präkitationen mit SP) geklickt wird + herausfinden ob schon in catPref:
        String clickedbuttoncategoryraw = clickedButton.getText().toString(); //button text für categoryname nehmen + umwandeln in sprachenunabhängigen categoryname (button id kan man ned nehme, weil probleme mit neu-bildung von id bei neustart etc.)
        //2 falls anderssprachige buttons: raw clickedbuttoncategory string muss umgewandelt werden in die catPref-version auf engl.
        String clickedbuttoncategory = convertCatFromDiffLangToEng(clickedbuttoncategoryraw);
        Log.d(TAG, clickedbuttoncategory + " button geklickt");

        //3 before changing searchquery cats, retrieve current searchquery cats (to later on know whether the cat pref law have been amended)
        //int amountofsearchquerycats = searchquerycatsList.size();  //TODO 11.1. idk if neeDeD

        //4 add/remove button from/to searchquery?
        if (!searchquerycatsList.contains(clickedbuttoncategory)) {
            searchquerycatsList.add(clickedbuttoncategory);
            Log.d(TAG,clickedbuttoncategory + " added to searchquerycatsList");
        } else {
            searchquerycatsList.remove(clickedbuttoncategory);
            Log.d(TAG,clickedbuttoncategory + " removed from searchquerycatsList");
        }

        //5-> set background color based on color before
        changeBackgroundOfCatButton(clickedButton); //search

        //6 has the user changed searchquery cats?
        //... ws ned //TODO 11.1. idk if neeDeD
    };

    public void changeLangofButtons(String language) {
        if (language.equals("en")) {
            party.setText("party"); festival.setText("festival"); club.setText("club");
            nature.setText("nature"); illegal.setText("illegal"); adventure.setText("adventure"); trip.setText("trip"); travel.setText("travel");
            erasmus.setText("erasmus"); university.setText("university"); school.setText("school"); work.setText("work");
            friends.setText("friends"); relationship.setText("relationship"); family.setText("family");
            dream.setText("dream"); luck.setText("luck");
            happy.setText("happy"); inlove.setText("in love"); relieved.setText("relieved");
            lonely.setText("lonely"); desperate.setText("desperate"); sad.setText("sad"); depressive.setText("depressive");
            angry.setText("angry"); anxious.setText("anxious"); nostalgic.setText("nostalgic");
        } else {
            party.setText("Party");festival.setText("Festival");club.setText("Club");
            nature.setText("Natur");illegal.setText("Illegal");adventure.setText("Abenteuer");trip.setText("Ausflug");travel.setText("Reisen");
            erasmus.setText("Erasmus");university.setText("Uni");school.setText("Schule");work.setText("Arbeit");
            friends.setText("Freunde");relationship.setText("Beziehung");family.setText("Familie");
            dream.setText("Traum");luck.setText("Glück");
            happy.setText("fröhlich");inlove.setText("verliebt");relieved.setText("erleichtert");
            lonely.setText("einsam");desperate.setText("verzweifelt");sad.setText("traurig");depressive.setText("depressiv");
            angry.setText("wütend");anxious.setText("ängstlich");nostalgic.setText("nostalgisch");
        }
    }


    private String convertCatFromDiffLangToEng(String clickedbuttoncategory) {
        //TODO (languages) (future) sprachen hinzufügen
        switch (clickedbuttoncategory) {
            case "Party" -> clickedbuttoncategory = "party";
            case "Festival" -> clickedbuttoncategory = "festival";
            case "Club" -> clickedbuttoncategory = "club";
            case "Arbeit" -> clickedbuttoncategory = "work";
            case "Schule" -> clickedbuttoncategory = "school";
            case "Uni" -> clickedbuttoncategory = "university";
            case "Erasmus" -> clickedbuttoncategory = "erasmus";
            case "Abenteuer" -> clickedbuttoncategory = "adventure";
            case "Illegal" -> clickedbuttoncategory = "illegal";
            case "Natur" -> clickedbuttoncategory = "nature";
            case "Reisen" -> clickedbuttoncategory = "travel";
            case "Ausflug" -> clickedbuttoncategory = "trip";
            case "Beziehung" -> clickedbuttoncategory = "relationship";
            case "Familie" -> clickedbuttoncategory = "family";
            case "Freunde" -> clickedbuttoncategory = "friends";
            case "Traum" -> clickedbuttoncategory = "dream";
            case "Glück" -> clickedbuttoncategory = "luck";
            case "fröhlich" -> clickedbuttoncategory = "happy";
            case "erleichtert" -> clickedbuttoncategory = "relieved";
            case "verliebt" -> clickedbuttoncategory = "inlove";
            case "in love" -> clickedbuttoncategory = "inlove";
            //TODO (info) omg .. gut dass aufgefallen
            case "traurig" -> clickedbuttoncategory = "sad";
            case "depressiv" -> clickedbuttoncategory = "depressive";
            case "einsam" -> clickedbuttoncategory = "lonely";
            case "nostalgisch" -> clickedbuttoncategory = "nostalgic";
            case "wütend" -> clickedbuttoncategory = "angry";
            case "ängstlich" -> clickedbuttoncategory = "anxious";
            case "verzweifelt" -> clickedbuttoncategory = "desperate";

            //TODO (new categories) buttonClickListener [ProfileFrag]
        }
        return clickedbuttoncategory;
    }

    private void changeBackgroundOfCatButton(Button button) {
        //Log.d(TAG, "changeBackgroundOfCatButton"); //TODO debug
        //1 retrieve current color
        int currentColor = (int) button.getTag(); //(info) die tags müssen die ints der Farben sein, weil die tags mit den echten Farben in if-sentences verglichen werden!
        //Log.d(TAG,"currentColor of " + button.getText() + ": " + currentColor + " (in: ADD cat to pref)");
        //2 set background color based on color before
        //A) grayify
        if (currentColor == blueish) {
            button.setBackgroundColor(grayedblueish);
            button.setTag(grayedblueish);}
        else if (currentColor == violet) {
            button.setBackgroundColor(grayedviolet);
            button.setTag(grayedviolet);}
        else if (currentColor == cyan) {
            button.setBackgroundColor(grayedcyan);
            button.setTag(grayedcyan);}
        else if (currentColor == red) {
            button.setBackgroundColor(grayedred);
            button.setTag(grayedred);}
        else if (currentColor == darkred) {
            button.setBackgroundColor(grayeddarkred);
            button.setTag(grayeddarkred);}
        else if (currentColor == green) {
            button.setBackgroundColor(grayedgreen);
            button.setTag(grayedgreen);}
        else if (currentColor == lightgreen) {
            button.setBackgroundColor(grayedlightgreen);
            button.setTag(grayedlightgreen);}
        else if (currentColor == orange) {
            button.setBackgroundColor(grayedorange);
            button.setTag(grayedorange);}
        //TODO (new categories) buttonClickListener [ProfileFrag]
        //oder B) colorize
        else if (currentColor == grayedblueish) {
            button.setBackgroundColor(blueish);
            button.setTag(blueish);}
        else if (currentColor == grayedviolet) {
            button.setBackgroundColor(violet);
            button.setTag(violet);}
        else if (currentColor == grayedcyan) {
            button.setBackgroundColor(cyan);
            button.setTag(cyan);}
        else if (currentColor == grayedred) {
            button.setBackgroundColor(red);
            button.setTag(red);}
        else if (currentColor == grayeddarkred) {
            button.setBackgroundColor(darkred);
            button.setTag(darkred);}
        else if (currentColor == grayedgreen) {
            button.setBackgroundColor(green);
            button.setTag(green);}
        else if (currentColor == grayedlightgreen) {
            button.setBackgroundColor(lightgreen);
            button.setTag(lightgreen);}
        else if (currentColor == grayedorange) {
            button.setBackgroundColor(orange);
            button.setTag(orange);}
        //TODO (new categories) buttonClickListener [ProfileFrag]
    }


}
