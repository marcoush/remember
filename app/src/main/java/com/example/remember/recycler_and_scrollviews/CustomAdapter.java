package com.example.remember.recycler_and_scrollviews;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.remember.R;
import com.example.remember.ui.home.HomeFragment;
import com.example.remember.ui.profile.ProfileFragment;
import com.example.remember.ui.search.SearchFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class CustomAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> { //former: RecyclerView.Adapter<Viewholder_SEARCH>
    private static final String TAG = "CustomAdapter";

    //damit ich getString() benutzen kann lol hmm idw
    //private Context context;

    //diverse view types
    SearchFragment searchFragment;
    ProfileFragment profileFragment;
    HomeFragment homeFragment;
    private static final int VIEW_TYPE_SEARCH = 1;
    private static final int VIEW_TYPE_UPLOADED = 2;
    private static final int VIEW_TYPE_LISTENED = 3;
    private static final int VIEW_TYPE_HOME = 4;
    //private static final int VIEW_TYPE_LOADMORE = 5;
    List<Model> modelList;

    //wenn man in der globalen Suche suchen will
    //TODO (methodizing) (customadapter scope) ich glaube, diese 3 kann man in 1 zsmfassen
    public CustomAdapter(SearchFragment listFragment, List<Model> modelList) {
        this.searchFragment = listFragment;
        this.modelList = modelList;
       // this.context = context; //added for the line "holder.languageflag.setBackground(ContextCompat.getDrawable(context, R.drawable.germanflag)" ... hat funktinoioert though falls ich sowas nochmal neede
    }
    //wenn man in Profile suchen will -> TODO (A) (customadapter scope) nach selbst angehörten oder selbst erstellen memories
    public CustomAdapter(ProfileFragment listFragment, List<Model> modelList) {
        this.profileFragment = listFragment;
        this.modelList = modelList;
    }
 //wenn man in Home adapter angezeigt bekommt
    public CustomAdapter(HomeFragment listFragment, List<Model> modelList) {
        this.homeFragment = listFragment;
        this.modelList = modelList;
       // this.context = context; //added for the line "holder.languageflag.setBackground(ContextCompat.getDrawable(context, R.drawable.germanflag)" ... hat funktinoioert though falls ich sowas nochmal neede
    }

    //interaction to memoFrag when memo data is there
    private CustomAdapterListener listenerCommunicationCAtoMain;



    @NonNull
    @Override
    //public ViewHolder_SEARCH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) { //former..
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //Log.d(TAG,"onCreateViewHolder called");
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        //View itemView; //former..

        if (viewType == VIEW_TYPE_UPLOADED) {
            View viewUploaded = inflater.inflate(R.layout.recyclerview_model_uploaded, parent, false);
            //Log.d(TAG,"VIEW_TYPE_UPLOADED start");
            ViewHolder_UPLOADED viewHolderUploaded = new ViewHolder_UPLOADED(viewUploaded);
            viewHolderUploaded.setClickListener(new ViewHolder_UPLOADED.ClickListener() {
                @Override
                public void onItemClick(View view, int position) {
                    Log.d(TAG, "onItemClick UPLOADED called");
                    // You can access the clicked item using the position parameter

                    retrievemodelListandsendtoMain(viewUploaded, position);

                }

                @Override
                public void onItemLongClick(View view, int position) {
                    //TODO (c) Handle item long click here
                }
            });
            //Log.d(TAG,"VIEW_TYPE_UPLOADED end");
            return viewHolderUploaded;
        }
        else if (viewType == VIEW_TYPE_LISTENED) {
            View viewListened = inflater.inflate(R.layout.recyclerview_model_listened, parent, false);
            //Log.d(TAG,"VIEW_TYPE_LISTENED start");
            ViewHolder_LISTENED viewHolderListened = new ViewHolder_LISTENED(viewListened);
            viewHolderListened.setClickListener(new ViewHolder_LISTENED.ClickListener() {
                @Override
                public void onItemClick(View view, int position) {
                    Log.d(TAG, "onItemClick LISTENED called");
                    // You can access the clicked item using the position parameter
                    retrievemodelListandsendtoMain(viewListened, position);
                }

                @Override
                public void onItemLongClick(View view, int position) {
                    //TODO (c) Handle item long click here
                }
            });
            //Log.d(TAG,"VIEW_TYPE_LISTENED end");
            return viewHolderListened;
        }
        else if (viewType == VIEW_TYPE_SEARCH) {
            View viewSearch = inflater.inflate(R.layout.recyclerview_model_search, parent, false);
            //Log.d(TAG,"VIEW_TYPE_SEARCH start");
            ViewHolder_SEARCH viewHolderSearch = new ViewHolder_SEARCH(viewSearch);
            viewHolderSearch.setClickListener(new ViewHolder_SEARCH.ClickListener() {
                @Override
                public void onItemClick(View view, int position) {
                    Log.d(TAG, "onItemClick SEARCH called");
                    // You can access the clicked item using the position parameter
                    retrievemodelListandsendtoMain(viewSearch, position);
                }

                @Override
                public void onItemLongClick(View view, int position) {
                    //TODO (c) Handle item long click here
                }
            });
            //Log.d(TAG,"VIEW_TYPE_SEARCH end");
            return viewHolderSearch;
        }
        else if (viewType == VIEW_TYPE_HOME) {
            View viewHome = inflater.inflate(R.layout.recyclerview_model_home, parent, false); //TODO (methodizing) (CA) könnte man zu view machen und außerhalb der ifs initiaten , fühlt sich aber gefährlich an, weil für jeden einzelnen initiierten CA (pro rv item) gibt's ja iwie einen clicklistener usw. not sure wie sich das verhellt
            //Log.d(TAG,"VIEW_TYPE_HOME start");
            ViewHolder_HOME viewHolderHome = new ViewHolder_HOME(viewHome);
            viewHolderHome.setClickListener(new ViewHolder_HOME.ClickListener() {
                @Override
                public void onItemClick(View view, int position) {
                    Log.d(TAG, "onItemClick HOME called");
                    // You an access the clicked item using the position parameter
                    retrievemodelListandsendtoMain(viewHome, position);
                }

                @Override
                public void onItemLongClick(View view, int position) {
                    //TODO (c) Handle item long click babuschka martinius
                }
            });
            //Log.d(TAG,"VIEW_TYPE_HOME end");
            return viewHolderHome;
        }

        throw new IllegalArgumentException("Invalid view type");
    }




    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        //Log.d(TAG,"onBindViewHolder called");
        Model model;

        if (holder instanceof ViewHolder_UPLOADED viewholderuploaded) {
            Log.d(TAG,"ViewHolder_UPLOADED is set up");
            model = modelList.get(position);
            //UPLOADED hochkant-darstellung von: title, duration, kein creator, date, listeners, language, categories
            //0 title, duration, date
            viewholderuploaded.titletextview.setText(model.getTitle());
            viewholderuploaded.durationtextview.setText(model.getDuration());
            viewholderuploaded.clock.setBackgroundResource(R.drawable.clock);
            viewholderuploaded.headphone.setBackgroundResource(R.drawable.headphone2);
            //viewholderuploaded.listenerstextview.setText(String.valueOf(model.getListeners()) + R.string.listeners); //TODO kann weg
            //1 listeners
            String listeners = String.valueOf(model.getListeners()); //als string einlesen i dont care
            if (listeners.equals("0")) viewholderuploaded.listeners.setText(R.string.nolisteners); //TODO ( ":(" da war ich spaßig, aber lassen wir es erstmal drin ist rallig)
            else {
                viewholderuploaded.listeners.setText(listeners);
            }
            //2 date
            SimpleDateFormat outputFormatNormal = new SimpleDateFormat("dd. MMM yyyy", Locale.ENGLISH);//frontend
            String date = outputFormatNormal.format(model.getDate());
            viewholderuploaded.datetextview.setText(date);
            viewholderuploaded.calendar.setBackgroundResource(R.drawable.calendar);
            //3 language
            if(model.getLanguage().equals("de")) {
                viewholderuploaded.languageflag.setBackgroundResource(R.drawable.germanflag);
            } else {
                //holder.languageflag.setBackground(ContextCompat.getDrawable(context, R.drawable.englishflag));
                viewholderuploaded.languageflag.setBackgroundResource(R.drawable.englishflag);
            } //TODO (more languages) CustomAdapter onBindViewHolder UPLOADED
            //4 categories
            if (model.getCategories() != null) {
                //4.1 Decompose the category string if categoriesstring exists in Firestore
                List<String> categoriesList = model.getCategories();
                String categoryString = categoriesStringBuilder(categoriesList); //uploaded
                String[] categories = categoryString.split(",");
                //4.2 Clear existing buttons in the category container
                viewholderuploaded.categorycontainerrow1.removeAllViews();
                viewholderuploaded.categorycontainerrow2.removeAllViews();
                //4.3 Create and add buttons for each category
                Context context = holder.itemView.getContext();
                createCategoryButtons(categories, viewholderuploaded.categorycontainerrow1, viewholderuploaded.categorycontainerrow2, context, model);
            } else {
                //4.9 hide hsv if categoriesstring doesn't exist in Firestore
                viewholderuploaded.categorycontainerrow1.setVisibility(View.GONE);
                viewholderuploaded.categorycontainerrow2.setVisibility(View.GONE);
            }
            //Log.d(TAG,"ViewHolder_UPLOADED wurde gesetzt!");
        }
        else if (holder instanceof ViewHolder_LISTENED viewholderlistened) {
            Log.d(TAG,"ViewHolder_LISTENED is set up");
            model = modelList.get(position);
            //LISTENED hochkant-darstellung von: title, duration, creator, date, listeners, language, categories
            //0 title, duration, creator, listeners
            viewholderlistened.titletextview.setText(model.getTitle());
            viewholderlistened.durationtextview.setText(model.getDuration());
            viewholderlistened.clock.setBackgroundResource(R.drawable.clock);
            viewholderlistened.headphone.setBackgroundResource(R.drawable.headphone2);
            viewholderlistened.listeners.setText(String.valueOf(model.getListeners()));//bei listeners kein ":("
            //1 compose/assemble string out of creator + date
            SimpleDateFormat outputFormatNormal = new SimpleDateFormat("yyyy", Locale.ENGLISH);//frontend
            String date = outputFormatNormal.format(model.getDate());
            String creatoranddate = model.getCreator() + " (" + date + ")";
            viewholderlistened.creatoranddatetextview.setText(creatoranddate);
            //2 .nix.
            //4 categories
            if (model.getCategories() != null) {
                //4.1 Decompose the category string if categoriesstring exists in Firestore
                List<String> categoriesList = model.getCategories();
                String categoryString = categoriesStringBuilder(categoriesList); //listened
                String[] categories = categoryString.split(",");
                //4.2 Clear existing buttons in the category container
                viewholderlistened.categorycontainerrow1.removeAllViews();
                viewholderlistened.categorycontainerrow2.removeAllViews();
                //4.3 Create and add buttons for each category
                Context context = holder.itemView.getContext();
                createCategoryButtons(categories, viewholderlistened.categorycontainerrow1, viewholderlistened.categorycontainerrow2, context, model);
            } else {
                //4.9 hide hsv if categoriesstring doesn't exist in Firestore
                viewholderlistened.categorycontainerrow1.setVisibility(View.GONE);
                viewholderlistened.categorycontainerrow2.setVisibility(View.GONE);
            }
            //Log.d(TAG,"ViewHolder_LISTENED wurde gesetzt!");
        }
        else if (holder instanceof ViewHolder_SEARCH viewholdersearch) {
            Log.d(TAG,"ViewHolder_SEARCH is set up");
            //ViewHolder_SEARCH viewholdersearch = (ViewHolder_SEARCH) holder; //replaced with pattern varialbe
            model = modelList.get(position);
            //SEARCH normal-darstellung von: title, duration, creator, date, listeners, categories
            //0 title, duration, creator, listeners
            viewholdersearch.titletextview.setText(model.getTitle());
            viewholdersearch.durationtextview.setText(model.getDuration());
            viewholdersearch.clock.setBackgroundResource(R.drawable.clock);
            viewholdersearch.headphone.setBackgroundResource(R.drawable.headphone2);
            viewholdersearch.listeners.setText(String.valueOf(model.getListeners()));
            //1 .|. languages werden nicht eingelesen, weil die suche selbst über die languageflag auf engl/deutsch/... umgestellt werden kann
            //2 compose/assemble string out of creator + date
            SimpleDateFormat outputFormatNormal = new SimpleDateFormat("yyyy", Locale.ENGLISH);//frontend
            String date = outputFormatNormal.format(model.getDate());
            String creatoranddate = model.getCreator() + " (" + date + ")";
            viewholdersearch.creatoranddatetextview.setText(creatoranddate);
            //3 .la bomba.
            //4 categories
            if (model.getCategories() != null) {
                //4.1 Decompose the category string if categoriesstring exists in Firestore
                List<String> categoriesList = model.getCategories();
                String categoryString = categoriesStringBuilder(categoriesList); //search
                String[] categories = categoryString.split(",");
                //4.2 Clear existing buttons in the category container
                viewholdersearch.categorycontainerrow1.removeAllViews();
                //4.3 Create and add buttons for each category
                Context context = holder.itemView.getContext();
                createCategoryButtons(categories, viewholdersearch.categorycontainerrow1, null, context, model);
            } else {
                //4.9 hide hsv if categoriesstring doesn't exist in Firestore
                viewholdersearch.categorycontainerrow1.setVisibility(View.GONE);
            }
            //Log.d(TAG,"ViewHolder_SEARCH wurde gesetzt!");

        }
        else if (holder instanceof ViewHolder_HOME viewholderhome) {
            Log.d(TAG,"ViewHolder_HOME is set up");
            model = modelList.get(position);
            //LISTENED hochkant-darstellung von: title, duration, creator, date, listeners, language, categories
            //0 title, duration, creator, listeners
            viewholderhome.titletextview.setText(model.getTitle());
            viewholderhome.durationtextview.setText(model.getDuration());
            viewholderhome.clock.setBackgroundResource(R.drawable.clock);
            viewholderhome.headphone.setBackgroundResource(R.drawable.headphone2);
            viewholderhome.listeners.setText(String.valueOf(model.getListeners()));
            //1 .ambob:l
            //2 compose/assemble string out of creator + date
            SimpleDateFormat outputFormatNormal = new SimpleDateFormat("yyyy", Locale.ENGLISH);//frontend
            String date = outputFormatNormal.format(model.getDate());
            String creatoranddate = model.getCreator() + " (" + date + ")";
            viewholderhome.creatoranddatetextview.setText(creatoranddate);
            //4 categories
            if (model.getCategories() != null) {
                //4.1 Decompose the category string if categoriesstring exists in Firestore
                List<String> categoriesList = model.getCategories();
                String categoryString = categoriesStringBuilder(categoriesList); //home
                String[] categories = categoryString.split(",");
                //4.2 Clear existing buttons in the category container
                viewholderhome.categorycontainerrow1.removeAllViews();
                viewholderhome.categorycontainerrow2.removeAllViews();
                //4.3 Create and add buttons for each category
                Context context = holder.itemView.getContext();
                createCategoryButtons(categories, viewholderhome.categorycontainerrow1, viewholderhome.categorycontainerrow2, context, model);
            } else {
                // Hide category containers if categories field is empty
                viewholderhome.categorycontainerrow1.setVisibility(View.GONE);
                viewholderhome.categorycontainerrow2.setVisibility(View.GONE);
            }
            //Log.d(TAG,"ViewHolder_HOME wurde gesetzt!");
        }
    }





    private void retrievemodelListandsendtoMain(View viewtype, int position) {
        //1 retrieve the metadata from the modellist (everyt as String)
        String title = modelList.get(position).getTitle();
        String duration = modelList.get(position).getDuration();
        String creator = modelList.get(position).getCreator();
        String creatorid = modelList.get(position).getCreatorid();
        String date = modelList.get(position).getDate().toString();
        String listeners = String.valueOf(modelList.get(position).getListeners());
        String hearts = String.valueOf(modelList.get(position).getHearts());
        List<String> categoriesarray = modelList.get(position).getCategories();
        String language = modelList.get(position).getLanguage();
        String url = modelList.get(position).getURL();
        String id = modelList.get(position).getID();

        //1.1 aus dem categoriesarray die einzelnen cat rausholen (danke chat)
        String categories = categoriesStringBuilder(categoriesarray); //uploaded

        //2 put all metadata in MainViewModel so that MemoFragment can access it beautifli
        //mainViewModel = new ViewModelProvider((ViewModelStoreOwner) viewUploaded.getContext()).get(MainViewModel.class); //old VM
        String[] metadataArray = {title, duration, creator, creatorid, date, listeners, hearts, categories, language, url, id};
        ArrayList<String> memometadatalist = new ArrayList<>(Arrays.asList(metadataArray)); //also einträge 0 (title) bis 8 (id) .. this is basically: memometadatalist.add(title); memometadatalist.add(duration); ...
        //mainViewModel.setMemoMetadata(memometadatalist); //old VM

        //3 send signal of memo data acquiring over interface to Main and there will be decided how to pass data to memoFrag
        //Log.d(TAG, "listenerforcommunicationwithmemoFrag BEFORE: " + listenerCommunicationCAtoMain);
        if (listenerCommunicationCAtoMain == null && viewtype.getContext() instanceof CustomAdapterListener) {
            //Log.d(TAG, "listenerforcommunicationwithmemoFrag == null  ,  view.getContext() instanceof CustomAdapterListener");
            listenerCommunicationCAtoMain = (CustomAdapterListener) viewtype.getContext();
            //Log.d(TAG, "listenerforcommunicationwithmemoFrag: " + listenerCommunicationCAtoMain);
        }
        listenerCommunicationCAtoMain.onRetrieveDataFromCustomAdapter(memometadatalist);
    }

    private String categoriesStringBuilder(List<String> categoriesarray) {
        //(info) damit es einfacher ist, die memo daten vom viewholder ans MemoFrag weiterzuleiten, wird auch das categoriesarray in eienn (composed) string transferiert und converted und umgewandelt und adaptisitiert
        StringBuilder categoriesBuilder = new StringBuilder();
        for (String category : categoriesarray) {
            if (categoriesBuilder.length() > 0) {categoriesBuilder.append(",");}
            categoriesBuilder.append(category);
        }
        String result = categoriesBuilder.toString();
        Log.d(TAG,"categoriesStringBuilder: " + result);
        return result;
    }

    //TODO (A) diese method auch für die anderen viewholder (uploaded,listened,search) einpflegen!!!!!!!!!
    @SuppressLint("SetTextI18n")
    private void createCategoryButtons(String[] categories, ViewGroup containerRow1,
                                       ViewGroup containerRow2, Context context, Model model) {
        for (String category : categories) {
            new ContextThemeWrapper(context, R.style.BTN_CTGRY_SMALL_COLORLESS);
            Context contextThemeWrapper = switch (category) {
                case "party", "festival", "club" ->
                        new ContextThemeWrapper(context, R.style.BTN_CTGRY_SMALL_VIOLET);
                case "work", "school", "university", "erasmus" ->
                        new ContextThemeWrapper(context, R.style.BTN_CTGRY_SMALL_ORANGE);
                case "adventure", "illegal", "nature", "travel", "trip" ->
                        new ContextThemeWrapper(context, R.style.BTN_CTGRY_SMALL_GREEN);
                case "relationship", "family", "friends" ->
                        new ContextThemeWrapper(context, R.style.BTN_CTGRY_SMALL_RED);
                case "dream" ->
                        new ContextThemeWrapper(context, R.style.BTN_CTGRY_SMALL_CYAN);
                case "happy", "lucky", "relieved", "inlove" ->
                        new ContextThemeWrapper(context, R.style.BTN_CTGRY_SMALL_LIGHTGREEN);
                case "sad", "depressive", "lonely", "nostalgic" ->
                        new ContextThemeWrapper(context, R.style.BTN_CTGRY_SMALL_BLUEISH);
                case "angry", "anxious", "desperate" ->
                        new ContextThemeWrapper(context, R.style.BTN_CTGRY_SMALL_DARKRED);
                default ->
                        //important to set COLORLESS! when there's not categories for memo!!
                        new ContextThemeWrapper(context, R.style.BTN_CTGRY_SMALL_COLORLESS);
                //TODO (new categories) CustomAdapter onBindViewHolder LISTENED
            };
            Button button = new Button(contextThemeWrapper);// Create the button using the new context //TODO (A) bei zu schnellem Navigieren zw. Frags NOR error
            //4.3.2 format button further
            button.setMinHeight(0);//"setMinHeight is defined by TextView, while setMinimumHeight is defined by View. According to the docs, the greater of the two values is used, so both must be set"
            button.setMinimumHeight(0);
            button.setMinWidth(0);
            button.setMinimumWidth(0);
            button.setPadding(20, 20, 20, 20);
            button.setEnabled(false);
            //button text in anderen sprachen (weil in firestore sind categories immer auf engl.)
            if (model.getLanguage().equals("en")) { //wenn lang=en -> use category name for button
                if (category.equals("inlove")) button.setText("in love"); //spezialfall "inlove/in love"
                else button.setText(category);
            } //TODO (more languages) CustomAdapter onBindViewHolder HOME
            else { //wenn lang=de -> translate categories for button texts
                switch (category) {
                    case "party" -> button.setText("Party");
                    case "festival" -> button.setText("Festival");
                    case "club" -> button.setText("Club");
                    case "work" -> button.setText("Arbeit");
                    case "school" -> button.setText("Schule");
                    case "university" -> button.setText("Uni");
                    case "erasmus" -> button.setText("Erasmus");
                    case "adventure" -> button.setText("Abenteuer");
                    case "illegal" -> button.setText("Illegal");
                    case "nature" -> button.setText("Natur");
                    case "travel" -> button.setText("Reisen");
                    case "trip" -> button.setText("Ausflug");
                    case "relationship" -> button.setText("Beziehung");
                    case "family" -> button.setText("Familie");
                    case "friends" -> button.setText("Freunde");
                    case "dream" -> button.setText("Traum");
                    case "happy" -> button.setText("fröhlich");
                    case "lucky" -> button.setText("glücklich");
                    case "relieved" -> button.setText("erleichtert");
                    case "inlove" -> button.setText("verliebt");
                    case "sad" -> button.setText("traurig");
                    case "depressive" -> button.setText("depressiv");
                    case "lonely" -> button.setText("einsam");
                    case "nostalgic" -> button.setText("nostalgisch");
                    case "angry" -> button.setText("wütend");
                    case "anxious" -> button.setText("ängstlich");
                    case "desperate" -> button.setText("verzweifelt");

                    //TODO (new categories) CustomAdapter onBindViewHolder HOME
                }
            }

            //wenn reihe 2 nicht existiert -> alles in reihe 1
            if (containerRow2 == null) {
                containerRow1.addView(button);
            }
            //ansonsten aufteilen in reihe 1 und 2
            else {
                //4.3.3 Add the category button to the containerrow1 (if it won't be >160dp width afterwards - if that happens, add to containerrow2)
                //1 buttonwidth
                int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                button.measure(widthMeasureSpec, heightMeasureSpec);
                int buttonWidth = button.getMeasuredWidth();

                //2 row1width
                int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                containerRow1.measure(widthSpec, heightSpec);
                int row1width = containerRow1.getMeasuredWidth();

                //3. Sum button width + current containerrow1 width together: if >160dp, add button to row1, otherwise to row2
                if (row1width + buttonWidth <= dpToPx(context, 130)) {
                    containerRow1.addView(button);
                } else {
                    containerRow2.addView(button);
                }
            }

        }
    }

    @Override //ALTE/NORMALE VERSION OHNE DEN LOADMOREBUTTON
    public int getItemCount() {
        return modelList.size();
    }

    @Override
    public int getItemViewType(int position) {
        // Determine the view type for the item at the given position - also welcher recyclerview soll ausgelöst werden?

        Model model = modelList.get(position);
        String type = model.getType();

        return switch (type) {
            case "uploaded" -> VIEW_TYPE_UPLOADED;
            case "listened" -> VIEW_TYPE_LISTENED;
            case "search" -> VIEW_TYPE_SEARCH;
            case "home" -> VIEW_TYPE_HOME;
            default -> super.getItemViewType(position);
        };

    }



    public static int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }


    //interface for communicating to memoFrag
    public interface CustomAdapterListener {
        void onRetrieveDataFromCustomAdapter(ArrayList<String> memolist);
    }

}