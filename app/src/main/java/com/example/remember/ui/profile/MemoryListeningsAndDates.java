package com.example.remember.ui.profile;

public class MemoryListeningsAndDates {
    //diese klasse existiert aus folg. Grund:
    // ich benötigte Kombinationen aus category name + category id , die ich in ein Array einlesen kann. hiermit geht das, indem ich diese class ins Array einlese und dann in Publish das nutze , um
    // die associatet id zu einem selected category button zu finden :) viele grüße aus napoli maroncelli
    //(die associated id aus firestore lese ich aus, damit die kategorien immer im gleichen format composed werden, zB "party,festival,depressive" (und niemals "depressive,festival,party" oder sowas])
    private String listenedaudioid;
    private String formatteddatestring;
   // private Date listeningdate;

    public MemoryListeningsAndDates(String listenedaudioid, String formatteddatestring) { //former instead of Date listeningdate: String formatteddatestring :D
        this.listenedaudioid = listenedaudioid;
        this.formatteddatestring = formatteddatestring;
       // this.listeningdate = listeningdate;
    }

    public String getListenedAudioID() { return listenedaudioid; }

    public String getFormattedListeningDateString() {
        return formatteddatestring;
    }
    /*public Date getListeningDate() {
        return listeningdate;
    }*/
}