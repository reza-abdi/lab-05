package com.example.lab5_starter;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements CityDialogFragment.CityDialogListener {

    private Button addCityButton;
    private Button deleteCityButton;
    private ListView cityListView;
    private boolean deleteMode = false;

    private FirebaseFirestore db;
    private CollectionReference citiesRef;


    private ArrayList<City> cityArrayList;
    private ArrayAdapter<City> cityArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set views
        addCityButton = findViewById(R.id.buttonAddCity);
        cityListView = findViewById(R.id.listviewCities);



        // create city array
        cityArrayList = new ArrayList<>();
        cityArrayAdapter = new CityArrayAdapter(this, cityArrayList);
        cityListView.setAdapter(cityArrayAdapter);

        //addDummyData();
        db = FirebaseFirestore.getInstance();
        citiesRef = db.collection("cities");

        citiesRef.addSnapshotListener((value, error) -> {
            if (error != null){
                Log.e("Firestore", error.toString());
            }
            cityArrayList.clear();
            if (value != null){
                cityArrayList.clear();
                for (QueryDocumentSnapshot snapshot : value){
                    String name = snapshot.getString("name");
                    String province = snapshot.getString("province");

                    cityArrayList.add(new City(name, province));
                }
                cityArrayAdapter.notifyDataSetChanged();
            }
        });

        // set listeners
        addCityButton.setOnClickListener(view -> {
            CityDialogFragment cityDialogFragment = new CityDialogFragment();
            cityDialogFragment.show(getSupportFragmentManager(),"Add City");
        });

        deleteCityButton = findViewById(R.id.buttonDelete);
        deleteCityButton.setOnClickListener(view -> {
            if (!deleteMode){
                deleteMode = true;
                deleteCityButton.setText("Cancel");
                Toast.makeText(this, "Delete mode: tap a city to remove it", Toast.LENGTH_SHORT).show();
            } else {
                deleteMode = false;
                deleteCityButton.setText("Delete");
                Toast.makeText(this, "Delete cancelled", Toast.LENGTH_SHORT).show();
            }

        });

        cityListView.setOnItemClickListener((adapterView, view, i, l) -> {
            City city = cityArrayAdapter.getItem(i);
            if (deleteMode) {
                new AlertDialog.Builder(this)
                        .setTitle("Delete city")
                        .setMessage("Delete " + city.getName() + " " + city.getProvince() + "?")
                        .setPositiveButton("Delete", (d, w) -> {
                            deleteByNameAndProvince(city.getName(), city.getProvince());
                            // exit delete mode after deleting
                            deleteMode = false;
                            deleteCityButton.setText("DELETE");
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            } else {
                CityDialogFragment cityDialogFragment = CityDialogFragment.newInstance(city);
                cityDialogFragment.show(getSupportFragmentManager(), "City Details");
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (deleteMode) {
                    deleteMode = false;
                    deleteCityButton.setText("DELETE");
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

    }

    @Override
    public void updateCity(City city, String title, String year) {
        city.setName(title);
        city.setProvince(year);
        cityArrayAdapter.notifyDataSetChanged();

        // Updating the database using delete + addition
    }

    @Override
    public void addCity(City city){
        cityArrayList.add(city);
        cityArrayAdapter.notifyDataSetChanged();

        DocumentReference docRef = citiesRef.document(city.getName());
        docRef.set(city);

    }

    private void deleteByNameAndProvince(String name, String province) {
        citiesRef.whereEqualTo("name", name)
                .whereEqualTo("province", province)
                .get()
                .addOnSuccessListener(q -> {
                    if (q.isEmpty()) {
                        Toast.makeText(this, "No matching city found", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot d : q.getDocuments()) {
                        batch.delete(d.getReference());
                    }
                    batch.commit()
                            .addOnSuccessListener(x ->
                                    Toast.makeText(this, "Deleted " + name + " " + province, Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Query failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


    public void addDummyData(){
        City m1 = new City("Edmonton", "AB");
        City m2 = new City("Vancouver", "BC");
        cityArrayList.add(m1);
        cityArrayList.add(m2);
        cityArrayAdapter.notifyDataSetChanged();
    }
}