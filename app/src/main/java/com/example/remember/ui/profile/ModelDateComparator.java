package com.example.remember.ui.profile;

import com.example.remember.recycler_and_scrollviews.Model;

import java.util.Comparator;
import java.util.Date;

public class ModelDateComparator implements Comparator<Model> { //vorher stand hier erst Comparator<Date> und Comparator<String>, aber es muss ja die modelList nach Date geordnet werden!
    //existiert, um die Liste nach Datum zu ordnen in Graph
   // private SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);

    @Override
    public int compare(Model model1, Model model2) { //loopy lookk
        //(info) 'compare' returns the value 0 if the argument Date is equal to this Date; a value less than 0 if this Date is before the Date argument; and a value greater than 0 if this Date is after the Date argument.
        //Date parsedDate1 = dateFormat.parse(model1.getDate()); //clever, chat!!! bin ich gearde nicht drauf gekommen mit .getDate() :D dafür ist"s ja da
        // Date parsedDate2 = dateFormat.parse(model2.getDate());

        //takes in the dates from the models that shall be sorted :D TODO (A) might be performance intensive?
        Date date1 = model1.getDate();
        Date date2 = model2.getDate();

        if (date1 != null && date2 != null) {
            return date2.compareTo(date1); // Compare in descending order
        } else if (date1 == null && date2 == null) {
            return 0; // Both dates are null, consider them equal
        } else if (date1 == null) {
            return 1; // parsedDate1 is null, consider parsedDate2 greater
        } else {
            return -1; // parsedDate2 is null, consider parsedDate1 greater
        }
    }








//vorher mit Date gewesen:
    /*@Override
    public int compare(Date date1, Date date2) {
        return date2.compareTo(date1); // Compare in descending order
    }*/

//vorher mit string gewesen stattF ate:
  /*  private SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");

    @Override
    public int compare(String date1, String date2) {
        try {
            Date parsedDate1 = dateFormat.parse(date1);
            Date parsedDate2 = dateFormat.parse(date2);
            return parsedDate2.compareTo(parsedDate1); // Compare in descending order
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }*/
}