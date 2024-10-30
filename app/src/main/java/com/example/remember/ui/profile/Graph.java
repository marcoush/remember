package com.example.remember.ui.profile;

import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.remember.R;
import com.example.remember.databinding.FragGraphBinding;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.mongodb.MongoClientSettings;

import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.realm.mongodb.App;
import io.realm.mongodb.AppConfiguration;
import io.realm.mongodb.User;
import io.realm.mongodb.mongo.MongoClient;
import io.realm.mongodb.mongo.MongoCollection;
import io.realm.mongodb.mongo.MongoDatabase;

public class Graph extends Fragment implements OnChartGestureListener, OnChartValueSelectedListener {
    //TODO (A) diese methode kopieren und ein neues frag aufmachen für Listenings week
    // also insg. wird es wenn alles nach plan verläuft 3x uplaod und 3x listenings also 6 frags in total für graph geben :)
    //TODO (info) konvention: 1,2,3 am ende der variablen für 1:week,2:month,3:sixmonths (falls ich mal sixmonths zu jahr ändern sollte, wird es so einfacher)
    private static final String TAG = "graph_frag";//logt .. unten für den meddl als "TAG" vonnöten

    //binding
    private FragGraphBinding binding;


    //chart & entrylists 
    private BarChart graphChart;


    //cifviewmodel
    private ProfileViewModel profileViewModel; //onActivityCreated nicht vergessen bei fragmentes

    //uploads & listenings in letzter Woche hinzugefügt
    List<DatesOfMemos> duringLastWeekAddedUploadsList1 = new ArrayList<>();
    List<DatesOfMemos> duringLastWeekAddedListeningsList1 = new ArrayList<>();


    //TODO (graph) (methodizing) diese auch ändern
    List<DatesOfMemos> duringLastMonthAddedUploadsList2 = new ArrayList<>();
    List<DatesOfMemos> duringLastSixmonthsAddedUploadsList3 = new ArrayList<>();
    List<DatesOfMemos> duringLastMonthAddedListeningsList2 = new ArrayList<>();
    List<DatesOfMemos> duringLastSixmonthsAddedListeningsList3 = new ArrayList<>();
    Date currentDate, weekBeforeDate1, monthBeforeDate2, sixmonthsBeforeDate3;
    String weekBeforeDay1, monthBeforeDay2, sixmonthsBeforeDay3; //in the format EEE (Mon, Tue, Wed, ...)

    //for the method that returns the weekday:
    private int nextDayIndex1 = 0;
    //for the method that returns the week:
    //private int nextWeekIndex = 0;//braucht man nicht
    //for the method that returns the month:
    //private int nextMonthIndex = 0;//braucht man nicht

    //for the method that returns the amount of uploads each weekday:
    private int substractionAmountWeek1 = -6; //initialize with -6 { 7-1 }, because otherwise i don't get the CURRENT day also in the week :( //TODO (AAA) (graph) muss das hier static sein?
    //new version statt month/4w for 6w , vorher: //initialize with -27 { month=4weeks, also (4*7)-1 } , because otherwise i don't get the CURRENT day
    private int subtractionAmountMonth2 = -41; //initialize with -41 { 6weeks, also (6*7)-1 } , because otherwise i don't get the CURRENT day //TODO (AAA) (graph) muss das hier static sein?
    private int substractionAmountSixmonths3 = -179; //initialize with -179 { (6*30)-1 } ,because otherwise i don't get the CURRENT day //TODO (AAA) (graph) muss das hier static sein?

    private int timescalledint = 1; //TODO debug

//mongoDB realm initializa
    MongoDatabase mongoDatabase;
    MongoClient mongoClient;
    MongoCollection<Document> mongoUsersCollection, mongoAudiosCollection;
    User user;
    App app;
    String userid, usermail;


    //ausgelesen aus dem user doc:
    private final ArrayList<Integer> weekdayindexArray1 = new ArrayList<>();
    int weekdayint1 = 0;

    private final ArrayList<Integer> amountofmemosatthatweekdayList1 = new ArrayList<>(); //memos: uploads o. listenings at that weekday ... unito nell'una metoda

    private List<String> datesofuploadsList;
    private List<String> datesoflisteningsList;


    //3.11.23 just put those here so i can use methods :)
    BarDataSet dataset1; //(info) deve essere perché java.. sarà che dataset1 sarà determinato in uno dei if-clausi ma java non lo so
    String[] weekdays1; //(info) deve essere perché java.. sarà che weekdays1 sarà determinato in uno dei if-clausi ma java non lo so

    //muss ins scope gepackt werden, sodass es in versch. methods aufgegriffen werden kann, wird mit null initiiert
    Date uplorlisdate = null;
    String dateString, audioID;
    private String graphtypeglobal;


    //(info) (graph) es macht keinen sinn bei MONTH/SIXMONTHS, für jeden tag einzeln die uploads rauszusuchen, dann geht man ja 30 (o. 180 bei six months) tage durch .. das wäre
    // ja irrwitzig.. daher: versuchen zu implementieren, irgendwie die uploadswithdates ALLE rauszusuchen und dann davon ausgehend die dates auszulesen und dann darauf basierend
    // --> also: alle memos, die innerhalb von diesem und diesem date sind (1 woche), werden zusammengezählt und dann wird diese anzahl als 1 balken im graph ausgegeben   für die
    // jew. Woche -> ist implementiert xD



    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        //View viewweek = inflater.inflate(R.layout.frag_graphweek, container, false);
        binding = FragGraphBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class); //mit dem Profileviewmodel connected!
        //Log.d(TAG, "profileViewModel is: " + profileViewModel);
        //(info) hier musste ich requireActivity() nehmen (statt "this"), damit das viewmodel in profilefrag und graphweek läuft!

//-1 chart initalizations
        graphChart = binding.graphid;
        graphChart.setOnChartGestureListener(this);
        graphChart.setOnChartValueSelectedListener(this);
        //en-disable chart stuff
        graphChart.setDragEnabled(true);//moving the chart with the finger
        graphChart.setScaleEnabled(false);//kein zooming
        graphChart.getAxisRight().setEnabled(false);//rechts keine axis-werte

//0 mongodb stuff
        app = new App(new AppConfiguration.Builder("remember-dxcgp").build());
        user = app.currentUser();
        userid = user != null ? user.getId() : null; //sieht funny aus, deswegen hab ich's etabliert , wurde vorgshlagen in der mwmw heit franen :)
        usermail = user != null ? user.getProfile().getEmail() : null; //sieht funny aus, deswegen hab ich's etabliert , wurde vorgshlagen in der mwmw heit franen :)
        Log.d(TAG,"user: " + user +
                "\nuserid: " + userid +
                "\nusermail: " + usermail);
        mongoClient = user.getMongoClient("mongodb-atlas");
        mongoDatabase = mongoClient.getDatabase("remember");
        // registry to handle POJOs (Plain Old Java Objects)
        //CodecRegistry pojoCodecRegistry = fromRegistries(AppConfiguration.DEFAULT_BSON_CODEC_REGISTRY,
        //        fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        //TODO (info) (mongo) wenn Updates,Filters usen will, implementen: -> CodecRegistry defaultJavaCodecRegistry = MongoClientSettings.getDefaultCodecRegistry();
        CodecRegistry defaultJavaCodecRegistry = MongoClientSettings.getDefaultCodecRegistry();
        //(info) sicherheitshalber die codecregistry eingebaut für Sort
        mongoUsersCollection = mongoDatabase.getCollection("users").withCodecRegistry(defaultJavaCodecRegistry);
        mongoAudiosCollection = mongoDatabase.getCollection("audios").withCodecRegistry(defaultJavaCodecRegistry);
        //mongoCategoriesCollection = mongoDatabase.getCollection("categories", Category.class).withCodecRegistry(pojoCodecRegistry);

//1 get current date & week,month,sixmonths before date
        //1 Get the current date 3 times for either week,month,sixmonths & set simpledateformat
        Calendar calendar1 = Calendar.getInstance(); //(info) (graph) Calendar NICHT in den scope!! Gefahr des Überschreibens mit .add und so!!
        Calendar calendar2 = Calendar.getInstance(); //(info) (graph) Calendar NICHT in den scope!! Gefahr des Überschreibens mit .add und so!!
        Calendar calendar3 = Calendar.getInstance(); //(info) (graph) Calendar NICHT in den scope!! Gefahr des Überschreibens mit .add und so!!
        currentDate = calendar1.getTime();
        //TODO !!-info-!! ganz wichtig dass das hier ENGL ist! weil ich nutze weekBeforeDay für meine Kalkulationen, und nicht zur Anzeige!!!
        SimpleDateFormat outputFormatDay = new SimpleDateFormat("EEE", Locale.ENGLISH);//backend
        //1.1 week
        // Subtract 7 days from the current date
        calendar1.add(Calendar.DAY_OF_YEAR, -6); //7-1, because otherwise i don't get the CURRENT day
        weekBeforeDate1 = calendar1.getTime();
        // Print the dates
        Log.d(TAG,"Current Date: " + currentDate);
        Log.d(TAG,"Week Before Date: " + weekBeforeDate1);
        // Get the weekday 7 days ago (used in the method that calculates the x-axis values for the chart)
        weekBeforeDay1 = outputFormatDay.format(weekBeforeDate1);
        Log.d(TAG,"weekBeforeDay: " + weekBeforeDay1);
        
        //1.2 month (aktuell: 6w)
        // Subtract 6 weeks from the current date
        calendar2.add(Calendar.DAY_OF_YEAR, -41); //42-1, because otherwise i don't get the CURRENT day
        monthBeforeDate2 = calendar2.getTime();
        // Print the dates
        Log.d(TAG,"Month Before Date: " + monthBeforeDate2);
        // Get the monthday 30 days ago (used in the method that calculates the x-axis values for the chart)
        monthBeforeDay2 = outputFormatDay.format(monthBeforeDate2);
        Log.d(TAG,"monthBeforeDay: " + monthBeforeDay2);
 
        //1.3 sixMonths
        // Subtract 6 months from the current date
        calendar3.add(Calendar.DAY_OF_YEAR, -179); //180-1, because otherwise i don't get the CURRENT day
        sixmonthsBeforeDate3 = calendar3.getTime();
        // Print the dates
        Log.d(TAG,"6 Months Before Date: " + sixmonthsBeforeDate3);
        // Get the sixmonthsday 180 days ago (used in the method that calculates the x-axis values for the chart)
        sixmonthsBeforeDay3 = outputFormatDay.format(sixmonthsBeforeDate3);
        Log.d(TAG,"sixmonthsBeforeDay: " + sixmonthsBeforeDay3);

//2 observeeeeer für für userData (sprich: datesofuploadsList & datesoflisteningsList) wird gestartet, sobald die userData da is, kann der übliche getGraphtype observer auch dann starten (in dem wird dann
        //sobald datesofuploadsList & datesoflisteningsList aus der async operation beschafft wurden, sind sie auch ici im graphFrag erhältlich 3.11.23
        Log.d(TAG, "userData observer is turned on now");
        profileViewModel.getUserData().observe(getViewLifecycleOwner(), userData -> {

            //TODO (A) (graph) observer schlägt an, although setUserData hasn't been triggered from profileFrag WHEN it's the second
            // time that this activity starts ... not crucial, but unnötig

            Log.d(TAG, "userData observer HIT! (graphFrag)");
            Log.d(TAG, "userData ist " + userData);
            datesofuploadsList = userData.get(0);
            datesoflisteningsList = userData.get(1);

            //sobald die userData da ist, kann dann der graphType auch observed werden - und geändert werden :)
            Log.d(TAG, "graphtype observer is turned on now");
            profileViewModel.getGraphtype().observe(getViewLifecycleOwner(), graphType -> {
                Log.d(TAG, "graphtype observer HIT! (graphFrag)");
                Log.d(TAG, "graphtype ist " + graphType); //graphType: "uploads_week", "...
                graphtypeglobal = graphType;

                //(info) (graph) Konvention Zahlen 123 am Ende der Listen (erstmalig: loelallalsdfsl5.10.23 nachts, upgraded: 3.11.23)
                // 1:kleinste Zeiteinheit (aktuell: week),
                // 2:mittelprächtigste Zeiteinheit (aktuell: sixweeks),
                // 3:größte Zeiteinheit (aktuell: sixmonths)

                //(info) (graph) wegen probleme mit dem graphen, dass der valueformatter iwie nicht 4 values formatieren kann, sondern es erst ab 6 funktioniert, habe ich mich statt 4w für 1 month stattdessen für 6w entschieden
                // (weil 6w=6values und dann flutscht et) → "month" ist somit jetz eig "sixweeks"!!!
                // → das problem würde sich lösen lassen mit: xAxis.setLabelCount(barEntries.size()); ...set label count to 4... quelle:
                // ( https://stackoverflow.com/questions/51096413/valueformatter-at-mpandroidchart-repeated-twice-values ) ... aber ich hatte jetzt kein bedürfnis, das wieder zu 4weeks zurückzuändern

                //0 reset some variables first
                timescalledint = 1; //TODO debug
                substractionAmountWeek1 =  -6; //TODO (future) (graph) wenn sich Zeiteinheiten öndern, zB month/sixmonths
                //substractionAmountMONTH2 =  -27; //aus 4w -> 6w gemacht, wegen graph problemen anezige
                subtractionAmountMonth2 =  -41; //TODO (future) (graph) wenn sich Zeiteinheiten öndern, zB month/sixmonths
                substractionAmountSixmonths3 = -179; //TODO (future) (graph) wenn sich Zeiteinheiten öndern, zB month/sixmonths
                //ValueFormatterXAxisString.usedIndexesInValueFormatterXAxis.clear();

                //1 date check + wenn es in die richtige graph period passt, dann add to respective list
                addmemoIDandDateCombinationstoList();
                //(info) (sinnlos) (update 5.11.23 schmierd noa bessa, completamente übervorve diese tausende metodi da vom 5.10.23) (oh gott was hab ich getan...) (so, jetzt läuft edt)

                //2 prepare dataset using the constructed lists of date + id, depending on memoType
                switch (graphType) {
                    case "uploads_week", "listenings_week" -> preparedatasetWeek1();
                    case "uploads_month", "listenings_month" -> preparedatasetMonth2();
                    case "uploads_sixmonths", "listenings_sixmonths" -> preparedatasetSixmonths3();
                }


                //3 dataset graph formatting
                int color_one = ContextCompat.getColor(requireContext(), R.color.color_one); //TODO (unzufr.) (general) idk, is there a different way to retrieve color resorces in frags espaically ?
                dataset1.setValueFormatter(new NonZeroValueFormatter()); //value labels werden nicht angezeigt, wenn 0 -> easy //TODO (graph) may produce NPE
                dataset1.setDrawValues(true); //value labels der punkte AUF dem graphen (2, 3, ...)
                dataset1.setValueTextSize(12f); //text size of value labels
                dataset1.setValueTextColor(color_one); //text color of value labels
                dataset1.setColor(color_one); //color des graphen
                dataset1.setHighLightColor(color_one); //idk TODO (C) (graph) idk
                dataset1.notifyDataSetChanged(); //let the data know that the dataSet changed

                //4 BarData aus dataset erstelleln
                BarData data1 = new BarData(dataset1);
                Log.d(TAG, "dataset1: " + dataset1
                        + "\nBarData data1: " + data1);

                //5 x- und y-Achse wärden schön gämacht
                //first define arrays for month and six months (einfach straightforward)
                String[] monthArray = {getString(R.string.sixthlastweek), getString(R.string.fifthlastweek), getString(R.string.fourthlastweek), getString(R.string.thirdlastweek), getString(R.string.secondlastweek), getString(R.string.lastweek)};
                String[] sixMonthsArray = {getString(R.string.sixthlastmonth), getString(R.string.fifthlastmonth), getString(R.string.fourthlastmonth), getString(R.string.thirdlastmonth), getString(R.string.secondlastmonth), getString(R.string.lastmonth)};
                //5.1 format x-axis, depending on graphType
                XAxis xAxis = graphChart.getXAxis();
                if (graphType.contains("week")) {
                    xAxis.setValueFormatter(new ValueFormatterXAxisString(weekdays1)); //TODO (unzufr.) (graph) this ValueFormatterXAxisString gets called 4 times
                    xAxis.setDrawLabels(true); //werte an achsen //TODO (graph) x-values nur für week weil sonst hässlich
                } else if (graphType.equals("uploads_month") || graphType.equals("listenings_month")) {
                    xAxis.setValueFormatter(new ValueFormatterXAxisString(monthArray));
                    xAxis.setDrawLabels(false); //keine werte an achsen //TODO (graph) x-values nur für week weil sonst hässlich
                    //TODO (B) (future) (graph) wenn ich doch von 6weeks zu 4weeks gehen möchte, dann kann ich das schaffen, indem ich die number of labels to xAxis festlege:
                    //xAxis.setLabelCount(barEntries.size());
                }
                else { //if (graphType.equals("uploads_sixmonths") || graphType.equals("listenings_sixmonths"))
                    xAxis.setValueFormatter(new ValueFormatterXAxisString(sixMonthsArray));
                    xAxis.setDrawLabels(false); //keine werte an achsen //TODO (graph) x-values nur für week weil sonst hässlich
                }
             /*   // Create a custom ValueFormatter
                ValueFormatter zeropointoneformatter = new ValueFormatter() {
                    @Override
                    public String getBarLabel(BarEntry barEntry) {
                        // Format the value to your desired format, for example:
                        System.out.print("bar label is: " + String.format(Locale.getDefault(), "%.1f%n", barEntry.getY()));
                        return String.format(Locale.getDefault(), "%.1f", barEntry.getY());
                    }
                };
                data1.setValueFormatter(zeropointoneformatter);*/ //old zeropointoneformatter
                xAxis.setDrawAxisLine(false); //keine achsen
                xAxis.setDrawGridLines(false); //kein grid
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); //achse am boden!
                //5.2 y-axis
                YAxis yAxis = graphChart.getAxisLeft();
                yAxis.setAxisMinimum(0f); // Set a custom minimum value for the y-axis (so that the values of 0.1 aren't flowing in the middle of the chart instead of properly on bottom)
                yAxis.setDrawAxisLine(false); //keine achsen
                yAxis.setDrawGridLines(false); //kein grid
                yAxis.setDrawLabels(false); //keine werte an achsen
                //yAxis.setValueFormatter(new ValueFormatterYAxisWholeNumber()); //(info) (graph) ganze Zahlen methode funktioniert, aber ich will erstmal keine values auf der y-Achse

                //6 day night mode einstellungen
                int color_two = ContextCompat.getColor(requireContext(), R.color.color_two); //TODO (unzufr.) (graph) idk, is there a different way to retrieve color resorces in frags espaically ?
                int whitecolor = ContextCompat.getColor(requireContext(), R.color.white); //TODO (unzufr.) (graph) idk, is there a different way to retrieve color resorces in frags espaically ?
                int lightgreycolor = ContextCompat.getColor(requireContext(), R.color.lightgrey); //TODO (unzufr.) (graph) idk, is there a different way to retrieve color resorces in frags espaically ?
                int currentTheme = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                if (currentTheme == Configuration.UI_MODE_NIGHT_YES) { //night
                    //Log.d(TAG,"mode: night");
                    graphChart.setBackgroundColor(color_two);
                    xAxis.setTextColor(lightgreycolor);
                } else { //day
                    //Log.d(TAG,"mode: day");
                    graphChart.setBackgroundColor(whitecolor);
                    xAxis.setTextColor(color_two);
                }


                //7 generelle chart einstellungen + abschluss:
                //graphChart.setScaleMinima(2f, 1f);
                //graphChart.fitScreen();
                graphChart.getDescription().setEnabled(false); //oder: getDescription().setText("");
                graphChart.getLegend().setEnabled(false); //links unten das Ding weg.
                //TODO (B) wenn man dann auf den Graphen klickt (onValueSelected), dann soll das system die audioIDs der uploadeten memos an dem tag anzeigen! naja, soll es das wirklich... hm
                //(info) (graph) marker view brauche ich nicht mehr unbedingt, habe ihn disabled (weil value labels werden nun auf dem bar angezeigt standardmäßig)
                //damit man den graphen anclicken kann & den y-Value erhält D: - funktioniert so middl:
                //graphChart.setTouchEnabled(true);
                //IMarker marker = new YourMarkerView(getContext(), R.layout.custom_marker_view);
                //graphChart.setMarker(marker);
                graphChart.setData(data1);
                graphChart.notifyDataSetChanged(); //let the chart know it's data changed
                graphChart.invalidate(); //refresh (nur UI)


            });
        });

        Log.d(TAG, "onCreateView END");
        return root;
    }



    private void preparedatasetWeek1() { //memoType "uploads" o. "listenings"
        Log.d(TAG, "preparedatasetWeek1 called, memoType: " + graphtypeglobal);
        //erstellte Listen (duringLastWeekAddedUploadsList, duringLastWeekAddedListeningsList) werden ausgelesen im chart kind of
        ArrayList<BarEntry> entrylist = new ArrayList<>();
        ArrayList<BarEntry> entrylistwithsortedxvalues = new ArrayList<>();

        //this entrylist is supposed to be 7 entries big - one for each day in the past week.
        // 1 auf der x-axis are the dates, day16 wäre der tag vor genau 1w (Loop erstellen, der mir die daten der letzten 7 tage rausgibt und mit 7 limitiert ist eben)
        //   im loop wird der jew. tag zur entrylist hinzugefügt (von verg->gegenwart ZEITLICHE REIHENFOLGE!), also man beginnt mit dem tag vor 7 tagen

        //hierfür muss gemessen werden, wie viele memos es an dem jew. tag waren -> mit der method, die das datum abgleicht in der List DatesoFmemos

        //1 entrylist for the memos (day7, ...) and corresponding dates(x-axis) is creaded
        //TODO (info) (WICHTIG!!!!!!!) uploadsAndDatesEntryList wird zwar not used, der code ist aber nötig, damit uploadsatthatday() durchläuft und uploadsAtThatdayArray erstellt werden kann! (:
        entrylist.add(new BarEntry(weekday1(), memosatthatweekday1())); //(info) day before 1w!
        entrylist.add(new BarEntry(weekday1(), memosatthatweekday1()));
        entrylist.add(new BarEntry(weekday1(), memosatthatweekday1()));
        entrylist.add(new BarEntry(weekday1(), memosatthatweekday1()));
        entrylist.add(new BarEntry(weekday1(), memosatthatweekday1()));
        entrylist.add(new BarEntry(weekday1(), memosatthatweekday1()));
        entrylist.add(new BarEntry(weekday1(), memosatthatweekday1())); //(info) today!
        //(info) es müssen floats eingelesen werden als x- und y-werte, daher weekday() muss einen float zurückgeben, und ich würde sagen float=0:monday, 1:tue, 2:wed, etc. :)

        //2 weekdays1 strings werden erstellt
        weekdays1 = new String[]{weekdaystring1(), weekdaystring1(), weekdaystring1(), weekdaystring1(), weekdaystring1(), weekdaystring1(), weekdaystring1()}; //weekdaystrings für x-Axis
        Log.d(TAG, "weekdays1 String[] ist: " + Arrays.toString(weekdays1));

        //3 erstellen einer geordneten Liste (0,1,..,6 anstelle von 3,4,5,6,0,1,2 oder so) und herausnehmen der uploadsatthatday aus dem array einfach
        entrylistwithsortedxvalues.add(new BarEntry(0, amountofmemosatthatweekdayList1.get(0)));
        entrylistwithsortedxvalues.add(new BarEntry(1, amountofmemosatthatweekdayList1.get(1)));
        entrylistwithsortedxvalues.add(new BarEntry(2, amountofmemosatthatweekdayList1.get(2)));
        entrylistwithsortedxvalues.add(new BarEntry(3, amountofmemosatthatweekdayList1.get(3)));
        entrylistwithsortedxvalues.add(new BarEntry(4, amountofmemosatthatweekdayList1.get(4)));
        entrylistwithsortedxvalues.add(new BarEntry(5, amountofmemosatthatweekdayList1.get(5)));
        entrylistwithsortedxvalues.add(new BarEntry(6, amountofmemosatthatweekdayList1.get(6)));

        Log.d(TAG, "entrylist (week): " + entrylist);
        Log.d(TAG, "entrylist (week) with sorted xValues: " + entrylistwithsortedxvalues);

        //.... das ist der dümmste und umständlichste code, den die welt je gesehen hat AHAHAHAHHAAH 6.7.23

        //4 zero value formatter:
        //before setting the data to barchart, iterate through your dataset and replace zero values with a small positive value
        for (int i = 0; i < entrylistwithsortedxvalues.size(); i++) {
            BarEntry entry = entrylistwithsortedxvalues.get(i);
            if (entry.getY() == 0f) {
                entry.setY(0.02f); // Replace zero value with 0.1
                Log.d(TAG, "zero value of entry " + entry + " has been changed to 0.1f");
            }
        }

        //5 create & setup bardataset aus entrylistwithsortedxvalues (jew. uploads o. listenings)
        dataset1 = new BarDataSet(entrylistwithsortedxvalues, "");
        Log.d(TAG, "preparedatasetWeek1() END, memoType: " + graphtypeglobal);
    }
    private void preparedatasetMonth2() {
        Log.d(TAG, "preparedatasetMonth2 called, memoType: " + graphtypeglobal);
        ArrayList<BarEntry> entrylist = new ArrayList<>();

        //1 entrylist for the memos and corresponding dates(x-axis) is kreated
        entrylist.add(new BarEntry(0, memosinthatweek2())); //(info) the whole week that started 6 weeks ago!
        entrylist.add(new BarEntry(1, memosinthatweek2()));
        entrylist.add(new BarEntry(2, memosinthatweek2()));
        entrylist.add(new BarEntry(3, memosinthatweek2()));
        entrylist.add(new BarEntry(4, memosinthatweek2()));
        entrylist.add(new BarEntry(5, memosinthatweek2())); //(info) whole last week!

        Log.d(TAG, "entrylist (month) ist: " + entrylist);

        //2 before setting the data to barchart, iterate through your dataset and replace zero values with a small positive value
        for (int i = 0; i < entrylist.size(); i++) {
            BarEntry entry = entrylist.get(i);
            if (entry.getY() == 0f) {
                Log.d(TAG, "zero value of entry " + entry + " has been changed to 0.1f");
                entry.setY(0.02f); // Replace zero value with 0.1
            }
        }

        //3 create & setup bardataset aus entrylist
        dataset1 = new BarDataSet(entrylist, ""); //TODO (C) (graph) label? + (unzufr.) wieso gelb?
        Log.d(TAG, "preparedatasetMonth2 END, memoType: " + graphtypeglobal);
    }
    private void preparedatasetSixmonths3() {
        Log.d(TAG, "preparedatasetSixmonths3 called, memoType: " + graphtypeglobal);
        ArrayList<BarEntry> entrylist = new ArrayList<>();

        //1 entrylist for the memos and corresponding dates(x-axis) is createt
        entrylist.add(new BarEntry(0, memosinthatmonth3())); //(info) the whole month that started 6 months ago!
        entrylist.add(new BarEntry(1, memosinthatmonth3()));
        entrylist.add(new BarEntry(2, memosinthatmonth3()));
        entrylist.add(new BarEntry(3, memosinthatmonth3()));
        entrylist.add(new BarEntry(4, memosinthatmonth3()));
        entrylist.add(new BarEntry(5, memosinthatmonth3())); //(info) the last whole month!

        Log.d(TAG, "entrylist (month) ist: " + entrylist);

        //2 before setting the data to barchart, iterate through your dataset and replace zero values with a small positive value
        for (int i = 0; i < entrylist.size(); i++) {
            BarEntry entry = entrylist.get(i);
            if (entry.getY() == 0f) {
                Log.d(TAG, "zero value of entry " + entry + " has been changed to 0.1f");
                entry.setY(0.02f); // Replace zero value with 0.1
            }
        }

        //3 create & setup bardataset aus entrylist
        dataset1 = new BarDataSet(entrylist, ""); //TODO (C) (graph) label? + (unzufr.) wieso gelb?
        Log.d(TAG, "preparedatasetSixmonths3 END, memoType: " + graphtypeglobal);
    }



        @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }




    public void addmemoIDandDateCombinationstoList() {
        Log.d(TAG, "addmemoIDandDateCombinationstoList called, graphtype: " + graphtypeglobal);
        //diese method erstellt aus der einfachen liste datesofmemoidsList (in der einträge sind wie: Thu Oct 05 00:42:10 GMT+02:00 2023_2f8738ff-b99b-41d8-981c-925ca33dd0db)
        // eine neue liste (basierend auf der DatesOfMemos class), in der audioID und (formatierter) DateString separat drin enthalten sind

        //1 initializations for all 3 for-loops (week1,month2,sixmonths3)
        //List<DatesOfMemos> listToAddTo = new ArrayList<>();
        DatesOfMemos memoIDandDate;



        //2 durch alle einträge der datesofmemosList for-loopen und jew. entscheiden, in welche list geaddet werden soll (obwohl es immer dieselbe ist TODO (graph) (time) (methodizing))
        //entries zu lists nur adden, wenn die jew. list noch empty ist, weil sonst vervielfachen sich ja logicherweise die entries
        //(info) 3 for-loops, in denen basically dasselbe passiert, aber wenigstens wird nicht in jedem for-loop abgefragt, welcher graphType vorherrscht (weil graphType gibt's nur 1, muss ned imma wieda aufs Neue abgefragt werde)
        // -> minimal besser für die Performance + ich bin ein Perfektionist, Peniblist und Pessi- ne Optimist lol 6.11.23 - 1 :)
        //(mein Code ist darauf ausgelegt, dass möglichst WENIG in den for-loops geschieht, weil die die intensivsten sind - auch, wenn es mehr cluttering of same code gibt)
        //TODO (future) (graph) (timeperiods) wenn ich week,month,sixmonths ändere, muss das im Folgenden auch geändert werden
        //uploads_week
        //(info für Zukunftsmarc) entries zu list nur adden, wenn die duringLastWeekAddedUploadsList1 noch empty ist, weil sonst vervielfachen sich ja logicherweise die entries
        if (graphtypeglobal.equals("uploads_week") && datesofuploadsList != null && duringLastWeekAddedUploadsList1.isEmpty()) {
            for (String dateofmemo : datesofuploadsList) {
                String [] parts = dateofmemo.split("_");
                dateString = parts[0];
                audioID = parts[1];
                String uplorlisDAYdateString = retrieveDateofmemoasDAYString(dateString); //upl_week

                //If retrieved date is after the date one week1/month2/sixmonths3 before, then add to respective list:
                if (uplorlisdate.after(weekBeforeDate1)) {
                    //In der ArrayList duringLast - week1/month2/sixmonths3 - upl/lis - list wird ein Eintrag hinzugefügt für die aktuelle Kombination aus id + DAY date von die aktuelle memo
                    memoIDandDate = new DatesOfMemos(audioID, uplorlisDAYdateString);
                    duringLastWeekAddedUploadsList1.add(memoIDandDate);
                }
            }//END for-loop
            Log.d(TAG, duringLastWeekAddedUploadsList1.size() + " upl dates are after the date one week before ("+weekBeforeDate1+" and thenceforth added to the duringLastWeekAddedUploadsList1");
        }
        //listenings_week
        if (graphtypeglobal.equals("listenings_week") && datesoflisteningsList != null && duringLastWeekAddedListeningsList1.isEmpty()) {
            for (String dateofmemo : datesoflisteningsList) {
                String [] parts = dateofmemo.split("_");
                dateString = parts[0];
                audioID = parts[1];
                String uplorlisDAYdateString = retrieveDateofmemoasDAYString(dateString); //lis_week

                //If retrieved date is after the date one week1/month2/sixmonths3 before, then add to respective list:
                if (uplorlisdate.after(weekBeforeDate1)) {
                    //In der ArrayList duringLast - week1/month2/sixmonths3 - upl/lis - list wird ein Eintrag hinzugefügt für die aktuelle Kombination aus id + DAY date von die aktuelle memo
                    memoIDandDate = new DatesOfMemos(audioID, uplorlisDAYdateString);
                    duringLastWeekAddedListeningsList1.add(memoIDandDate);
                }
            }//END for-loop
            Log.d(TAG, duringLastWeekAddedListeningsList1.size() + " lis dates are after the date one week before ("+weekBeforeDate1+" and thenceforth added to the duringLastWeekAddedListeningsList1");
        }

        //uploads_month
        if (graphtypeglobal.equals("uploads_month") && datesofuploadsList != null && duringLastMonthAddedUploadsList2.isEmpty()) {
            for (String dateofmemo : datesofuploadsList) {
                //get Date, dateString & audioID:
                blabliblub(dateofmemo);//uploads_month

                //If retrieved date is after the date one week1/month2/sixmonths3 before, then add to respective list:
                if (uplorlisdate.after(monthBeforeDate2)) {
                    //In der ArrayList duringLast - week1/month2/sixmonths3 - upl/lis - list wird ein Eintrag hinzugefügt für die aktuelle Kombination aus id + DAY date von die aktuelle memo
                    memoIDandDate = new DatesOfMemos(audioID, dateString);
                    duringLastMonthAddedUploadsList2.add(memoIDandDate);
                }
            }//END for-loop
            Log.d(TAG, duringLastMonthAddedUploadsList2.size() + " upl dates are after the date one month before ("+monthBeforeDate2+" and thenceforth added to the duringLastMonthAddedUploadsList2");
        }
        //listenings_month
        if (graphtypeglobal.equals("listenings_month") && datesoflisteningsList != null && duringLastMonthAddedListeningsList2.isEmpty()) {
            for (String dateofmemo : datesoflisteningsList) {
                //get Date, dateString & audioID:
                blabliblub(dateofmemo);//listenings_month

                //If retrieved date is after the date one week1/month2/sixmonths3 before, then add to respective list:
                if (uplorlisdate.after(monthBeforeDate2)) {
                    //In der ArrayList duringLast - week1/month2/sixmonths3 - upl/lis - list wird ein Eintrag hinzugefügt für die aktuelle Kombination aus id + DAY date von die aktuelle memo
                    memoIDandDate = new DatesOfMemos(audioID, dateString);
                    duringLastMonthAddedListeningsList2.add(memoIDandDate);
                }
            }//END for-loop
            Log.d(TAG, duringLastMonthAddedListeningsList2.size() + " lis dates are after the date one month before ("+monthBeforeDate2+" and thenceforth added to the duringLastMonthAddedListeningsList2");
        }

        //uploads_sixmonths
        if (graphtypeglobal.equals("uploads_sixmonths") && datesofuploadsList != null && duringLastSixmonthsAddedUploadsList3.isEmpty()) {
            for (String dateofmemo : datesofuploadsList) {
                //get Date, dateString & audioID:
                blabliblub(dateofmemo);//uploads_sixmonths

                //If retrieved date is after the date one week1/month2/sixmonths3 before, then add to respective list:
                if (uplorlisdate.after(sixmonthsBeforeDate3)) {
                    //In der ArrayList duringLast - week1/month2/sixmonths3 - upl/lis - list wird ein Eintrag hinzugefügt für die aktuelle Kombination aus id + DAY date von die aktuelle memo
                    memoIDandDate = new DatesOfMemos(audioID, dateString);
                    duringLastSixmonthsAddedUploadsList3.add(memoIDandDate);
                }
            }//END for-loop
            Log.d(TAG, duringLastSixmonthsAddedUploadsList3.size() + " lis dates are after the date six months before ("+sixmonthsBeforeDate3+" and thenceforth added to the duringLastSixmonthsAddedUploadsList3");
        }
        //listenings_sixmonths
        if (graphtypeglobal.equals("listenings_sixmonths") && datesoflisteningsList != null && duringLastSixmonthsAddedListeningsList3.isEmpty()) {
            for (String dateofmemo : datesoflisteningsList) {
                //get Date, dateString & audioID:
                blabliblub(dateofmemo);//listenings_sixmonths

                //If retrieved date is after the date one week1/month2/sixmonths3 before, then add to respective list:
                if (uplorlisdate.after(sixmonthsBeforeDate3)) {
                    //In der ArrayList duringLast - week1/month2/sixmonths3 - upl/lis - list wird ein Eintrag hinzugefügt für die aktuelle Kombination aus id + DAY date von die aktuelle memo
                    memoIDandDate = new DatesOfMemos(audioID, dateString);
                    duringLastSixmonthsAddedListeningsList3.add(memoIDandDate);
                }
            }//END for-loop
            Log.d(TAG, duringLastSixmonthsAddedListeningsList3.size() + " lis dates are after the date six months before ("+sixmonthsBeforeDate3+" and thenceforth added to the duringLastSixmonthsAddedListeningsList3");
        }
        Log.d(TAG, "addmemoIDandDateCombinationstoList END, graphtype: " + graphtypeglobal);
    }

    private void blabliblub(String dateofmemo) {
        String[] parts = dateofmemo.split("_");
        dateString = parts[0];
        SimpleDateFormat inputFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);//backend //Thu Oct 26 17:33:50 GMT+02:00 2023
        try {uplorlisdate = inputFormat.parse(dateString);} catch (ParseException e) {e.printStackTrace();}
        assert uplorlisdate != null; //If assertion fails (i.e., uplorlisdate is null), this will throw an AssertionError
    }

    private String retrieveDateofmemoasDAYString(String dateString) {
        //Das Datum wird als DAY! abgespeichert, nur für week1 (wenn's sekundengenau wäre, würde es ja nie erkannt werden / man es in der Suche ja nie finden (?)
        //TODO (C) (graph) (date) sehr irrelevant, aber wenn um 23:59:59 jemand diese method auslöst, könnten Präkitationen entstehen lol
        SimpleDateFormat inputFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);//backend //Thu Oct 26 17:33:50 GMT+02:00 2023
        SimpleDateFormat outputFormatDayMonthYear = new SimpleDateFormat("dd. MMM yyyy", Locale.ENGLISH);//backend

        //(info) uplorlisdate is im scope initiiert, wird hier updated und dann 1 Ebene über dieser method in addmemoIDandDateCombinationstoList genutzt
        //Parse the datestring into DAY! Date-String
        try {uplorlisdate = inputFormat.parse(dateString);} catch (ParseException e) {e.printStackTrace();}
        assert uplorlisdate != null; //If assertion fails (i.e., uplorlisdate is null), this will throw an AssertionError
        return outputFormatDayMonthYear.format(uplorlisdate);
    }













    //ok meddl, das kam alles bei "implement methods" ganz oben in Z.23
    @Override
    public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
        Log.i(TAG, "onChartGestureStart: X: " + me.getX() + "Y: " + me.getY());
        //graphChart.getData().setHighlightEnabled(true); //die linie wird enabled, wenn man die chartinteraktion beginnt (aus Zeiten des linegraphsxD) die linie spackt rum, daher sollte setHighlightEnabled eig. an onValueSelected gekoppelt sein...
    }

    @Override
    public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
        Log.i(TAG, "onChartGestureEnd: " + lastPerformedGesture);
        //1  viewModel.setPrice(String.valueOf(Stock1.letzterwert0)); //der aktuelle stockprice wird ins viewmodel gePACKT
        graphChart.getData().setHighlightEnabled(false); //die linie wird disabled, wenn man die chartinteraktion beendet
    }

    @Override
    public void onChartLongPressed(MotionEvent me) {
        //Log.i(TAG, "onChartLongPressed: ");
    }

    @Override
    public void onChartDoubleTapped(MotionEvent me) {
        //Log.i(TAG, "onChartDoubleTapped: ");
    }

    @Override
    public void onChartSingleTapped(MotionEvent me) {
        //Log.i(TAG, "onChartSingleTapped: ");
    }

    @Override
    public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {
        //Log.i(TAG, "onChartFling: velocityX:" + velocityX + "velocityY: " + velocityY);
    }

    @Override
    public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
        //Log.i(TAG, "onChartScale: scaleX: " + scaleX + "scale Y: " + scaleY);
    }

    @Override
    public void onChartTranslate(MotionEvent me, float dX, float dY) {
        //Log.i(TAG, "onChartTranslate: dX: " + dX + "dY: " + dY);
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) { //das ist der latest shit
        Log.i(TAG, "onValueSelected: " + e.toString() + "        " + h.toString());
    }

    @Override
    public void onNothingSelected() {
        Log.i(TAG, "onNothingSelected: ");
    }



    //cifviewmodel (wenn ein ViewModel AN DIESES FRAGMENT etwas senden würde)
    //existiert, da onActivityCreated im fragment lifecycle nach onCreateView kommt und daher irgendwie nötig ist blabla ????? waaaaaas vergangenheitsmarc hääääääääääääää
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //viewModel = ViewModelProviders.of(getActivity()).get(GeneralViewModel.class); //deprecated cif mann mann
        //profileViewModel = new ViewModelProvider(getActivity()).get(ProfileViewModel.class); //????????? deaktiviert, ich weiß nicht, was das hier soll
        //falls ich mal Daten AN dieses fragment versenden will:
        //viewModel.getPrice().observe(getViewLifecycleOwner(), charSequence -> {}
    }


    private float weekday1() {
        int timescalled = timescalledint; //TODO debug
        timescalledint++; // Increment the value of weekdaycalledint //TODO debug
        Log.d(TAG, "weekday() called for " + timescalled + ". time");

        //0 This creates an array of strings (EEE formatted) representing the weekdays in the desired order
        String[] weekdays = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        //float[] weekdays = {1,2,3,4,5,6,7}; //This creates an array of floats representing the weekdays in the desired order.

        //1 Find the index of weekBeforeDay in the weekdays array (weekBeforeDay is in format EEE)
        int weekBeforeIndex = Arrays.asList(weekdays).indexOf(weekBeforeDay1);

        //2 Increment the next day index for subsequent calls
        nextDayIndex1 = (nextDayIndex1 + 1) % 7;
        int labomba = (weekBeforeIndex+ nextDayIndex1) % 7;   //%7, damit es ab index6 bei index0 weitergeht

        Log.d(TAG, "weekBeforeDay ist: " + weekBeforeDay1 + " (sollte im Format 'Mon' sein!!) [in: weekday()]\n" +
                "weekBeforeIndex ist: " + weekBeforeIndex + " [in: weekday()]\n" +
                "index of weekday is: " + labomba + "\n0:mon,1:tue,2:wed,3:thu,4:fri,5:sat,6:sun [in: weekday()]");

        //3 save weekdayindex to weekdayindex-array
        weekdayindexArray1.add(labomba);

        //4 return the weekday
        return labomba;
    }



    //für die Graphanzeige: HIERMIT ÜBERPRÜFEN, wie viele Uploads an jedem Tag der letzten Woche passiert sind, wenn zB keiner war, return null
    private int memosatthatweekday1() { //memoTypes: "uploads" o. "listenings"
        int timescalled = timescalledint; //TODO debug
        timescalledint++; // Increment the value of weekdaycalledint //TODO debug
        Log.d(TAG, "  --> memosatthatweekday1() called for " + timescalled + ". time, memoType: " + graphtypeglobal);

        //1 Retrieve the substractionAmountWEEK
        Log.d(TAG, "  substractionAmountWeek1 is: " + substractionAmountWeek1 + " (in beginning it should be -6 [not -7 bc. then I wouldn't get current day also])");

        //2 es wird jedes mal wenn diese method gecallt wird, ein neuer Calendar initalized (nötig, weil sonst Kalender überschrieben wird)
        //(sinnlose infoOrmATION!!! in ForMATION!!!!) method wird 7x nacheinander aufgerufen
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, substractionAmountWeek1); //(info) initialized with -6, because otherwise i don't get the CURRENT day also in the week :(
        Date dateofthatday = calendar.getTime();
        Log.d(TAG, "  date of that day is: " + dateofthatday); //TODO (debug) (graph) kann iwann weg, ist unten eh schon in nem log
        //"EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH ist das standardpattern, wenn ich das Date hier auslese. das kann dann mit dem date in der duringLastWeekAddedUploadsList abgeglichen werden

        //3 get the same date format/pattern as the one specified in //2 UPLOADS & LISTENINGS RAUSFINDEN, DIE IN DER LETZTEN WOCHE HINZUGEFÜGT WURDE!!!
        // (und nicht zu genaues datum!!! weil sekunden könnten probleme machen weil zeiten müssen ja übereinstimmeN!!!)
        SimpleDateFormat outputFormatNormal = new SimpleDateFormat("dd. MMM yyyy", Locale.ENGLISH);//backend
        String datestringofthatdayformatted = outputFormatNormal.format(dateofthatday);
        int amountofmemosatthatday = getmemoIDsfromDate(datestringofthatdayformatted); //methodized 3.11.23 juhuuu :)
        //jetzt hat man die jew. uploads / listenings vom date hergeholt und kann die weiterverarbeiten

        //4
        substractionAmountWeek1 = (substractionAmountWeek1 + 1) % 7; //increment substractionAmount, bis man beim jetzigen Tag angekommen ist mit -0 :) The modulo operator % 7 ensures that
        // substrAmount wraps around to range of 0-6 (since 7 weekdays) to avoid going beyond valid range
        // % returns remainder of division, ex. -6 % 7 = -6, -5 % 7 = -5, ... 8 % 7 = 1, 7 % 7 = 0  [-> no remainder] , % operator eig unnötig, weil wird eh nur 7x gecallt die method...
        //  Log.d(TAG, "  amount of uploads at day " + datestringofthatdayformatted + " is: " + amountofmemosatthatday);
        Log.d(TAG, "  amount of "+graphtypeglobal+" at day " + dateofthatday + " is: " + amountofmemosatthatday);

        //5 wichtig für das uploadsAndDatesEntryListWithSortedXValues später
        amountofmemosatthatweekdayList1.add(amountofmemosatthatday);
        //if (memoType.equals("uploads")) uploadsAtThatweekdayArray1.add(amountofmemosatthatday);
        //else listeningsAtThatweekdayArray1.add(amountofmemosatthatday); //nicht nötig, in getmemoIDsfromDate() wurden bereits die jew. uploads / listenings hergheolot

        Log.d(TAG, "  <-- memosatthatweekday1() END, memoType: " + graphtypeglobal);
        return amountofmemosatthatday;
    }


    private int memosinthatweek2() {
        int timescalled = timescalledint; //TODO debug
        timescalledint++; // Increment the value of weekcalledint //TODO debug
        Log.d(TAG, "  --> memosinthatweek2() called for " + timescalled + ". time, memoType: " + graphtypeglobal);

        //1 Retrieve the substractionAmountMONTH
        Log.d(TAG, "  subtractionAmountMonth2 is: " + subtractionAmountMonth2); // + " (in beginning it should be -41 [not -42 bc. then I wouldn't get current day also])");

        //es wird jedes mal wenn diese method gecallt wird, ein neuer Calendar initalized (nötig, weil sonst Kalender überschrieben wird)
        Calendar calendar = Calendar.getInstance();
        //jetzt wird der tag vor 6w (-1d, damit der jetzige tag noch miteinbezogen wird) berechnet (oder vor 6w, 5w, 4w, 3w, 2w, 1w - je nachdem, wie häufig memosinthatweek2() schon durschgelauve ist)
        calendar.add(Calendar.DAY_OF_YEAR, subtractionAmountMonth2);
        Date dateoffirstdayinthatweek = calendar.getTime();
        calendar.add(Calendar.DAY_OF_YEAR, 7); //der letzte tag in der jew. woche = +6 (zB 3.,4.,5.,6.,7.,8.,9.= 7 tage , also 3+6=9, also +6)
        Date dateoflastdayinthatweek = calendar.getTime();
        Log.d(TAG, "  date of first day in that week is: " + dateoffirstdayinthatweek + " and last one is: " + dateoflastdayinthatweek);

        //alle memos finden, die innerhalb von dateoffirstdayinthatweek und 7d später uploaded wurden (zB ganz zu Beginn: zwischen dem Tag vor 1mon/4w (-27 tage) und dem Tag vor 3w)
        //```???

        //2 getAmountofmemosduringPeriod - wie viele memos wurden upl/lis in der gesuchten period? /_\danke an chat/_\
        int amountofmemosinthatweek = getAmountofmemosduringPeriod(dateoffirstdayinthatweek, dateoflastdayinthatweek); //month2
        //int amountofmemosinthatweek = getAmountofmemosduringPeriod(datestringoffirstdayofthatweekformatted, datestringoflastdayofthatweekformatted, memoType); //month2

        //increment substractionAmount
        subtractionAmountMonth2 += 7; //= (subtractionAmountMonth2 + 7) % 42; , bis man bei der jetzigen Woche angekommen ist mit -0 :)
        //Log.d(TAG, "  amount of memos in week from " + datestringoffirstdayofthatweekformatted + " - " + datestringoflastdayofthatweekformatted + " is: " + amountofmemosinthatweek);
        Log.d(TAG, "  <-- memosinthatweek2() END");
        /*//wichtig für das uploadsAndDatesEntryListWithSortedXValues später NOPE NOT HERE. NOT FOR MONTH/SIXMONTHS.
        uploadsInThatWeekArray.add(amountofmemosinthatweek);*/
        return amountofmemosinthatweek;
    }


    private int memosinthatmonth3() {
        int timescalled = timescalledint; //TODO debug
        timescalledint++; // Increment the value of weekcalledint //TODO debug
        Log.d(TAG, "  --> memosinthatmonth3() called for " + timescalled + ". time, memoType: " + graphtypeglobal);

        Log.d(TAG, "  substractionAmountSixmonths3 is: " + substractionAmountSixmonths3 + " (in beginning it should be -179 [not -180 bc. then I wouldn't get current day also])");

        //es wird jedes mal wenn diese method gecallt wird, ein neuer Calendar initalized (nötig, weil sonst Kalender überschrieben wird)
        Calendar calendar = Calendar.getInstance();
        //jetzt wird der tag vor 6mon (-1d, damit der jetzige tag noch miteinbezogen wird) berechnet (oder vor 5mon, 4mon, .. - je nachdem, wie häufig uploadsinthatmonth() schon durschgelauve ist)
        calendar.add(Calendar.DAY_OF_YEAR, substractionAmountSixmonths3);
        Date dateoffirstdayinthatmonth = calendar.getTime();
        calendar.add(Calendar.DAY_OF_YEAR, 29); //der letzte tag des jew. months = +29
        Date dateoflastdayinthatmonth = calendar.getTime();
        Log.d(TAG, "  date of first day in that month is: " + dateoffirstdayinthatmonth + " and last one is: " + dateoflastdayinthatmonth);

        //alle memos finden, die innerhalb von dateoffirstdayinthatmonth und 1 month später uploaded wurden (zB ganz zu Beginn: zwischen dem Tag vor 6mon (-179 tage) und dem Tag vor 5mon)
        //1 dateoffirstdayinthatmonth & dateoflastdayinthatmonth formatieren:
        SimpleDateFormat outputFormatNormal = new SimpleDateFormat("dd. MMM yyyy", Locale.ENGLISH); //backend
        String datestringoffirstdayofthatmonthformatted = outputFormatNormal.format(dateoffirstdayinthatmonth);
        String datestringoflastdayofthatmonthformatted = outputFormatNormal.format(dateoflastdayinthatmonth);
        //2 getAmountOfUploadedAudiosDuringPeriod /_\danke an chat/_\
        int amountofmemosinthatmonth = getAmountofmemosduringPeriod(dateoffirstdayinthatmonth, dateoflastdayinthatmonth); //sixmonths3
        //int amountofmemosinthatmonth = getAmountofmemosduringPeriod(datestringoffirstdayofthatmonthformatted, datestringoflastdayofthatmonthformatted, memoType); //sixmonths3

        //increment substractionAmount, bis man beim jetzigen Monat angekommen ist mit -0 :)
        substractionAmountSixmonths3 = (substractionAmountSixmonths3 + 30) % 180;
        Log.d(TAG, "  amount of memos in month from " + datestringoffirstdayofthatmonthformatted + " - " + datestringoflastdayofthatmonthformatted + " is: " + amountofmemosinthatmonth);
        Log.d(TAG, "  <-- memosinthatmonth3() END");
        return amountofmemosinthatmonth;
    }


    //hilfemethode, um die zum date zugehörige ID des upl/lis zu finden (für week1 mit den Wochentagen)
    private int getmemoIDsfromDate(String date) { //memoTypes: "uploads" o. "listenings" ... former: String date instead of Date date
        Log.d(TAG, "    ----> getmemoIDsFromDate() called, memoType: " + graphtypeglobal);
        List<String> memoIDsList = new ArrayList<>(); //provisorische liste, dessen size dann returned wird einfach
        if (graphtypeglobal.equals("uploads_week")) {
            for (DatesOfMemos uploadanddate : duringLastWeekAddedUploadsList1) { //Iterates over the duringLastWeekAddedUploadsList
                Log.d(TAG, "    duringLastWeekAddedUploadsList size ist: " + duringLastWeekAddedUploadsList1.size() + " [im for loop]");
                if (uploadanddate.getFormattedUploadDateString().equals(date)) { //and checks if any of uploads match the specific/respective weekday date (info) hier war vorher: uploadanddate.getFormattedDateString().equals(date)
                    memoIDsList.add(uploadanddate.getMemoId()); //If found, it returns the associated audio id.
                }
            }
        } else { //"listenings_week"
            for (DatesOfMemos listeninganddate : duringLastWeekAddedListeningsList1) { //Iterates over the duringLastWeekAddedListeningsList1
                Log.d(TAG, "    duringLastWeekAddedListeningsList1 size ist: " + duringLastWeekAddedListeningsList1.size() + " [im for loop]");
                if (listeninganddate.getFormattedUploadDateString().equals(date)) { //and checks if any of uploads match the specific/respective weekday date (info) hier war vorher: listeninganddate.getFormattedDateString().equals(date)
                    memoIDsList.add(listeninganddate.getMemoId()); //If found, it returns the associated audio id.
                }
            }
        }
        Log.d(TAG, "    <---- getmemoIDsfromDate() END, memoType: " + graphtypeglobal);
        return memoIDsList.size(); //wenn keine memo IDs an dem date gefunden, dann gibt's 0 raus, wenn 2 gefunden wurden, dann 2 (und das kann dann als y-wert in den Graphen)
    }

    private String weekdaystring1() {
        Log.d(TAG, "weekdaystring() called for the " + (weekdayint1 +1) + ". time");
        int labomba = weekdayindexArray1.get(weekdayint1);
        weekdayint1++;
        return switch (labomba) {
            case 0 -> getString(R.string.monday);
            case 1 -> getString(R.string.tuesday);
            case 2 -> getString(R.string.wednesday);
            case 3 -> getString(R.string.thursday);
            case 4 -> getString(R.string.friday);
            case 5 -> getString(R.string.saturday);
            case 6 -> getString(R.string.sunday);
            default -> null;
        };
    }






    //month graph stuff chat 5.10.:   OR   //sixmonths graph stuff chat 5.10. 6:09:
    private int getAmountofmemosduringPeriod(Date startDate, Date endDate) {
        Log.d(TAG, "getAmountOfUploadedAudiosDuringPeriod called, memoType: " + graphtypeglobal +
                "\nstartdate("+startDate+")-enddate("+endDate+")");
        List<String> memoIDsList = new ArrayList<>();
        List<DatesOfMemos> addedmemosduringperiodList = switch (graphtypeglobal) {
            case "uploads_month" -> duringLastMonthAddedUploadsList2;
            case "listenings_month" -> duringLastMonthAddedListeningsList2;
            case "uploads_sixmonths" -> duringLastSixmonthsAddedUploadsList3;
            case "listenings_sixmonths" -> duringLastSixmonthsAddedListeningsList3;
            default -> new ArrayList<>(); //wird nie passieren
        };

        Log.d(TAG, "addedmemosduringperiodList: " + addedmemosduringperiodList); //TODO debug

        //dann for-loop:
        for (DatesOfMemos dateofmemo : addedmemosduringperiodList) {
            String memoDate = dateofmemo.getFormattedUploadDateString();
            if (isDateWithinRange(memoDate, startDate, endDate)) {
                //Log.d(TAG, "memodate ("+dateofmemo.getMemoId()+") is in range of start-enddate -> add 2 memoIDsList!");
                memoIDsList.add(dateofmemo.getMemoId());
            }
        }

        Log.d(TAG, "memoIDsList.size() ist:"+memoIDsList.size());
        Log.d(TAG, "getAmountofmemosduringPeriod END, memoType: " + graphtypeglobal);
        return memoIDsList.size();
    }


    //hilfsmethode für jew. month graph stuff chat 5.10.: und sixmonths graph stuff chat 5.10. 6:09:
    private boolean isDateWithinRange(String dateToCheck, Date startDate, Date endDate) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH); //backend //Thu Oct 26 17:33:50 GMT+02:00 2023
            Date date = dateFormat.parse(dateToCheck);
            //returns yes if the date is not before the start date and not after the end date
            assert date != null;
            boolean isInRange = !date.before(startDate) && !date.after(endDate);
            Log.d(TAG, "isDateWithinRange: " + isInRange +
                    "\n( dateToCheck: " + dateToCheck + "       startDate:" + startDate + "        endDate: " + endDate + " )");
            return isInRange;
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }
}