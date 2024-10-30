package com.example.remember.ui.profile;

public class MemoryUploadsAndDates {
    //diese klasse existiert aus folg. Grund:
    // ich benötigte Kombinationen aus ID + Upload date eines audios, die ich in ein Array einlesen kann. hiermit geht das, indem ich diese class ins Array einlese und dann
    // in Publish das nutze , um die associatet id zu einem selected category button zu finden :) viele grüße aus napoli maroncelli
    //(die associated id aus firestore lese ich aus, damit die kategorien immer im gleichen format composed werden, zB "party,festival,depressive" (und niemals "depressive,festival,party" oder sowas])
    private String uploadedaudioid;
    private String formatteddatestring;
   // private Date uploaddate;

    public MemoryUploadsAndDates(String uploadedaudioid, String formatteddatestring) { //former instead of Date uploaddate: String formatteddatestring :D
        this.uploadedaudioid = uploadedaudioid;
        this.formatteddatestring = formatteddatestring;
       // this.uploaddate = uploaddate;
    }

    public String getUploadedAudioID() {
        return uploadedaudioid;
    }

    public String getFormattedUploadDateString() {
        return formatteddatestring;
    }
    /*public Date getUploadDate() {
        return uploaddate;
    }*/
}