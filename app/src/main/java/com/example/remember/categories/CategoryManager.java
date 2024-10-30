package com.example.remember.categories;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.remember.LanguageUtils;
import com.example.remember.Main;
import com.example.remember.R;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class CategoryManager extends AppCompatActivity {
    //ui
    Button createcategorybutton, addtranslatedcategorybutton, backtomainbutton, addtagtocategorybutton, movebutton, createsubcollectionbutton, addamountofaudiosbutton;
    EditText createcategoryedittext, addtranslatedcategoryedittext, existingcategoryedittext, translatedfield, addtagtocategoryedittext, createsubcollectionedittext, amountofaudiosedittext;
    TextView categoryid;
    //firebase initializations
    private FirebaseFirestore fStore;
    //private CollectionReference categoriesCollection, situationsCollection, feelingsCollection;
    private DocumentReference categoryDocument;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("onCreate in CategoryManager started");
        super.onCreate(savedInstanceState);

//-1 Initalizie Firstoere
        fStore = FirebaseFirestore.getInstance();
        //TODO (justsayin) for now [or always]: keep all the categories in 1 COLLECTION!
        // categoriesCollection = fStore.collection("categories");
        // situationsCollection = categoriesCollection.document("situations").collection("situations");
        // feelingsCollection = categoriesCollection.document("feelings").collection("feelings");
        // NO: Get references to the "categories" collection and the inside collections "situations" & "feelings"

//0 Language
        //Get the languauage which is right now active and maybe change this activity's language to it (lang might have been changed in Settings that's why...)
        LanguageUtils languageUtils = new LanguageUtils();
        languageUtils.updateLanguage(this);
//M Content View
        setContentView(R.layout.activity_category);
//1 UI
        createcategoryedittext = findViewById(R.id.createcategoryedittextid);
        createcategorybutton = findViewById(R.id.createcategorybuttonid);
        existingcategoryedittext = findViewById(R.id.existingcategoryedittextid);
        addtranslatedcategoryedittext = findViewById(R.id.addtranslatedcategoryedittextid);
        addtranslatedcategorybutton = findViewById(R.id.addtranslatedcategorybuttonid);
        translatedfield = findViewById(R.id.translatedfieldedittextid);
        backtomainbutton = findViewById(R.id.backtologinbuttonid);
        addtagtocategorybutton = findViewById(R.id.addtagtocategorybuttonid);
        addtagtocategoryedittext = findViewById(R.id.addtagtocategoryedittextid);
        amountofaudiosedittext = findViewById(R.id.amountofaudiosedittextid);
        addamountofaudiosbutton = findViewById(R.id.addamountofaudiostocategorybuttonid);
        categoryid = findViewById(R.id.categoryidid);

        //buttons
        createcategorybutton.setOnClickListener(v -> {
            String newCategory;
            newCategory = createcategoryedittext.getText().toString();
            createAndSetCategoryDocument(newCategory);
            existingcategoryedittext.setText(newCategory); //damit man schnell direkt übersetzen kann, wenn man createt hat
        });
        addtranslatedcategorybutton.setOnClickListener(v -> {
            // Get the translations from the EditText fields
            String existingCategory = existingcategoryedittext.getText().toString().trim();//bestehende kategorie, wo translation hinzugefügt werden soll
            String translatedCategory = addtranslatedcategoryedittext.getText().toString().trim();//übersetzter Kategoriename
            String translatedField = translatedfield.getText().toString().trim();//abhängig von der sprache, z.B. name_de, name_fr, ..

            // Query to find the document with the matching field name of category -> give this document another field with translated category
            Query query = fStore.collection("categories").whereEqualTo("name", existingCategory);
            query.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    QuerySnapshot snapshot = task.getResult();
                    if (snapshot != null && !snapshot.isEmpty()) {
                        DocumentSnapshot document = snapshot.getDocuments().get(0);
                        categoryDocument = document.getReference();

                        // Update the language-specific field with any translation [translated field z.B. "name_de" (statisch), und translatedCategory z.B. Familie (jedes Mal neu eintippen)]
                        categoryDocument.update(translatedField, translatedCategory).addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                Log.d("Firestore", "Translation added successfully as new field in document");
                            } else {
                                Log.e("Firestore", "Error adding translation as new field in document");
                            }
                        });
                    } else {
                        Log.e("Firestore", "Error: no document with the given name is found");
                    }
                } else {
                    Log.e("Firestore", "Error: query in collection 'categories' doesn't work somehow HUHUHAUAAAAQ");
                }
            });
        });
        addtagtocategorybutton.setOnClickListener(v -> {
            // Get the translations from the EditText fields
            String existingCategory = existingcategoryedittext.getText().toString().trim();//bestehende kategorie, der ein tag hinzugefügt werden soll
            String tagName = addtagtocategoryedittext.getText().toString().trim();//Name vom Tag, also: situations oder feelings (oder places {?})

            // Query to find the document with the matching field name of category -> give this document another field with a tag (tag can be situations or feelings (or places {?})
            Query query = fStore.collection("categories").whereEqualTo("name", existingCategory);
            query.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    QuerySnapshot snapshot = task.getResult();
                    if (snapshot != null && !snapshot.isEmpty()) {
                        DocumentSnapshot document = snapshot.getDocuments().get(0);
                        categoryDocument = document.getReference();

                        // Update the language-specific field with any translation [tagField = "tag" (statisch), und tagName entweder situations oder feelings (oder places {?})]
                        categoryDocument.update("tag", tagName).addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                Log.d("Firestore", "Tag and tag name added successfully as new field in document");
                            } else {
                                Log.e("Firestore", "Error adding tag and tag name as new field in document");
                            }
                        });
                    } else {
                        Log.e("Firestore", "Error: no document with the given name is found");
                    }
                } else {
                    Log.e("Firestore", "Error: query in collection 'categories' doesn't work somehow HUHUHAUAAAAQ");
                }
            });
        });
        //add amount of audios to category (0)
        addamountofaudiosbutton.setOnClickListener(v -> {
            String amountofuploadedANDlistenedaudiosincategorystring = categoryid.getText().toString();
            int amountofuploadedANDlistenedaudiosincategoryint = Integer.parseInt(amountofuploadedANDlistenedaudiosincategorystring);
            String categoryidstring = categoryid.getText().toString();
            final int[] categoryidint = {Integer.parseInt(categoryidstring)}; //damit ich es danach +1 incrementen kann, nur dafür - hat nichts mit firestore zu tun
            // Query to find the document with the matching field name of category -> give this document another field with a tag (tag can be situations or feelings (or places {?})
            Query query = fStore.collection("categories").whereEqualTo("id", categoryidstring);
            query.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    QuerySnapshot snapshot = task.getResult();
                    if (snapshot != null && !snapshot.isEmpty()) {
                        DocumentSnapshot document = snapshot.getDocuments().get(0);
                        categoryDocument = document.getReference();

                        //audios uploaded / listened = 0
                        categoryDocument.update("audios_uploaded", 0);
                        categoryDocument.update("audios_listened", 0);

                        categoryidint[0]++; //id um 1 erhöhen und dann in textview anzeigen - dann muss man nicht händisch immer 2, 3, 4, /5 ....
                        categoryid.setText(categoryidint[0]);
                    } else {
                        Log.e("Firestore", "Error: no document with the given name is found");
                    }
                } else {
                    Log.e("Firestore", "Error: query in collection 'categories' doesn't work somehow HUHUHAUAAAAQ");
                }
            });
        });



        backtomainbutton.setOnClickListener(v -> {
            Intent intent = new Intent(this, Main.class);
            startActivity(intent);
        });


//TEMPORARY TRASH
   /*     // Get reference to the create button
        createsubcollectionedittext = findViewById(R.id.createsubcollectionedittextid);
        createsubcollectionbutton = findViewById(R.id.createsubcollectionbuttonid);
        createsubcollectionbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createSubcollection();
            }
        });
        // Get reference to the move button
        movebutton = findViewById(R.id.movebuttonid);
        movebutton.setOnClickListener(v -> moveDocumentsToSituations());*/

        System.out.println("onCreate in CategoryManager BEENDET!");
    }//____ENDEEE_onC_________R__________E____________A____________T____________E_____________________


    public void createAndSetCategoryDocument(String cat_name) {
        System.out.println("createAndSetCategoryDocument wurde gecallt (in: CategoryManager class)");
        DocumentReference docRef = fStore.collection("categories").document(); //returns doc with auto-generated id (=category id)

        // Set the fields of the document using the category object
        Map<String, Object> categoryMap = new HashMap<>();
        categoryMap.put("name", cat_name);
        docRef.set(categoryMap)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Document added successfully"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error adding document: " + e));
        System.out.println("Ende createAndSetCategoryDocument (in: CategoryManager class)");
    }


//falls ich in Zukunft mal documents in subcollections verschieben will (BTW: eine subcollection ist IN einem document in der collection btw!!!)
 /*   private void moveDocumentsToSituations() {
        // Retrieve documents from the "categories" collection
        categoriesCollection.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (DocumentSnapshot document : task.getResult()) {
                    // Get the data from the original document
                    Object data = document.getData();

                    // Create a new document in the "situations" subcollection with the same data
                    DocumentReference newDocument = situationsCollection.document(document.getId());
                    newDocument.set(data);
                }
            }
        });
    }*/

//falls ich mal ne subcollection erstellen möchte, zB feelings/ situations (BTW: eine subcollection ist IN einem document in der collection btw!!!)
 /*   private void createSubcollection() {
        // Get the name for the subcollection from editText3
        String subcollectionName = createsubcollectionedittext.getText().toString();
        // Create the subcollection within the "categories" collection
        categoriesCollection.document("situations").collection(subcollectionName)
                .add(new HashMap<>())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentReference documentReference = task.getResult();
                        // Subcollection created successfully
                        Log.d("Firestore", "Subcollection created successfully");
                    } else {
                        // Failed to create subcollection
                        Log.d("Firestore", "Error at creating subcollection");
                    }
                });
    }*/





}






