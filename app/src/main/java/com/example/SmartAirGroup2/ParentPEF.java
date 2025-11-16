package com.example.SmartAirGroup2;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;

/**
 * ParentDashboardFragment
 * -----------------------
 * This fragment serves as the main dashboard for parent users.
 * It allows parents to:
 *   • View their linked child accounts.
 *   • Add or link new child accounts.
 *   • Remove linked children.
 *
 * The fragment dynamically builds its UI using CardViews that represent each child.
 * It connects to Firebase Realtime Database to fetch and manage child relationships.
 *
 * Firebase Structure (relevant paths):
 * └── categories/
 *     └── users/
 *         ├── parents/{parentUname}/children/{childUname: String}
 *         └── children/{childUname}/... (child details)
 *
 * Core Features:
 *   - Loads all children linked to the current parent.
 *   - Dynamically generates a CardView for each child.
 *   - Provides delete functionality to unlink a child.
 *   - Supports navigation to AddChildFragment and LinkChildFragment.
 *   - Uses a toolbar with notification and settings menu options.
 *
 * Author: [Your Name]
 * Date: [Date]
 */

public class ParentPEF extends Fragment {

    // ───────────────────────────────
    // UI COMPONENTS
    // ───────────────────────────────
    private Toolbar toolbar;
    private CardView cardPB, cardAveragePEF;
    private TextView textPB, textAveragePEF;
    private EditText editTextPB;
    private Button buttonUpdate;

    private String name, uname;

    private String status;
    private String pbText, averagePEFText;
    private double pb, averagePEF;
    private double[] pefs;

    // ───────────────────────────────
    // LIFECYCLE METHODS
    // ───────────────────────────────

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve parent username passed as argument
        if (getArguments() != null) {
            name = getArguments().getString("childName");
            uname = getArguments().getString("childUname");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_parent_set_pef, container, false);

        // Toolbar setup
        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        setHasOptionsMenu(true);

        toolbar.setTitle(name +"'s Dashboard");

        // Handle back navigation (up button)
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        // UI references
        cardPB = view.findViewById(R.id.cardPB);
        cardAveragePEF = view.findViewById(R.id.cardAveragePEF);

        textPB = view.findViewById((R.id.textPB));
        textAveragePEF = view.findViewById((R.id.textAveragePEF));

        editTextPB = view.findViewById(R.id.editTextPB);
        buttonUpdate = view.findViewById(R.id.buttonUpdate);

        buttonUpdate.setOnClickListener(v -> updatePB());

        getPEF();
        return view;
    }

    private void getPEF() {
        DatabaseReference pefRef = FirebaseDatabase.getInstance()
                .getReference("categories")
                .child("users")
                .child("children")
                .child(uname)
                .child("data");

        pefRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                // ---- GET PB ----
                if (snapshot.child("pb").exists()) {
                    try {
                        pb = snapshot.child("pb").getValue(Double.class);
                    } catch (Exception e) {
                        pb = 0.0;
                    }
                }

                // ---- GET dailyPEF LIST ----
                if (snapshot.child("dailyPEF").exists()) {
                    DataSnapshot pefListSnap = snapshot.child("dailyPEF");

                    int size = (int) pefListSnap.getChildrenCount();
                    pefs = new double[size];

                    int index = 0;
                    for (DataSnapshot item : pefListSnap.getChildren()) {
                        Double value = item.getValue(Double.class);
                        pefs[index++] = (value != null ? value : 0.0);
                    }
                }

                averagePEF = 0;
                if (pefs != null && pefs.length > 0) {
                    double sum = 0;
                    for (double v : pefs) sum += v;
                    averagePEF = sum / pefs.length;
                }

//                String x =  "pb=" + pb + " | pef=" + averagePEF;
//                Toast.makeText(getContext(), x, Toast.LENGTH_SHORT).show();
                setPEF();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Cannot access Data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setPEF(){
        if (averagePEF > 0 && pb > 0){
            averagePEFText = "Average Daily PEF: " + averagePEF;
            pbText = "Personal Best: " + pb;

            if (averagePEF>=pb*0.8){
                status = "good";
            } else if (averagePEF>=pb*0.5){
                status = "warning";
            }else{
                status = "alert";
            }
        }
        if (averagePEF == 0){
            averagePEFText = "There is no PEF entry today";
            status = "good";
        }

        if (pb == 0){
            pbText = "Haven't set Personal Best yet";
            status = "good";
        }

        textAveragePEF.setText(averagePEFText);
        textPB.setText(pbText);

        if (status.equals("good")){
            cardAveragePEF.setCardBackgroundColor(getResources().getColor(R.color.good));
        }else if (status.equals("warning")){
            cardAveragePEF.setCardBackgroundColor(getResources().getColor(R.color.warning));
        }else{
            cardAveragePEF.setCardBackgroundColor(getResources().getColor(R.color.alert));
        }

        int pefZoneValue;
        switch (status) {
            case "good": pefZoneValue = 0; break;
            case "warning": pefZoneValue = 1; break;
            default: pefZoneValue = 2; break; // alert
        }

        DatabaseReference pefZoneRef = FirebaseDatabase.getInstance()
                .getReference("categories/users/children")
                .child(uname)
                .child("status")
                .child("pefZone");

        pefZoneRef.setValue(pefZoneValue)
                .addOnSuccessListener(aVoid ->
                        Log.d("PEF_STATUS", "pefZone updated: " + pefZoneValue))
                .addOnFailureListener(e ->
                        Log.e("PEF_STATUS", "Failed to update pefZone: " + e.getMessage()));

    }

    private void updatePB() {
        // Get entered PB text
        String pbStr = editTextPB.getText().toString().trim();

        // Validate not empty
        if (pbStr.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a value.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate numeric
        double newPB;
        try {
            newPB = Double.parseDouble(pbStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid number.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate > 0
        if (newPB <= 0) {
            Toast.makeText(getContext(), "PB must be greater than 0.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Firebase reference: categories/users/children/{childId}/data/pb
        DatabaseReference pbRef = FirebaseDatabase.getInstance()
                .getReference("categories/users/children")
                .child(uname)
                .child("data")
                .child("pb");

        // Update PB value
        pbRef.setValue(newPB).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Personal Best updated!", Toast.LENGTH_SHORT).show();

                // Update UI immediately
                pb = newPB;
                setPEF();
            } else {
                Toast.makeText(getContext(), "Failed to update PB.", Toast.LENGTH_SHORT).show();
            }
        });
    }



    // ───────────────────────────────
    // MENU HANDLING
    // ───────────────────────────────
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        MenuHelper.setupMenu(menu, inflater, requireContext());
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (MenuHelper.handleMenuSelection(item, this)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ───────────────────────────────
    // FRAGMENT NAVIGATION
    // ───────────────────────────────
    /**
     * Utility method for fragment navigation inside the same activity.
     */
    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
