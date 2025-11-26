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
 * PEFZone Fragment
 * --------------------------
 * This fragment displays and manages the Peak Expiratory Flow (PEF) status for a selected child user.
 * It retrieves the child's daily PEF history and PB (Personal Best) value from Firebase, computes their
 * current asthma control zone, updates UI color indicators, and stores the zone status back to Firebase.
 *
 * Functional Responsibilities:
 *  - Fetch PB and daily PEF data from Firebase.
 *  - Compute average daily PEF and determine the current asthma zone:
 *        * Green Zone (Good):    avgPEF >= 80% of PB
 *        * Yellow Zone (Warning): avgPEF >= 50% of PB
 *        * Red Zone (Alert):     avgPEF < 50% of PB
 *  - Update UI elements including:
 *        * PB text
 *        * Average PEF text
 *        * Status card color based on zone
 *  - Allow parent users to update the PB manually.
 *  - Save the resulting zone classification in Firebase under:
 *        categories/users/children/{username}/status/pefZone
 *
 * Expected Database Structure:
 *  categories/
 *      users/
 *          children/{uname}/
 *              data/
 *                  pb: double
 *                  dailyPEF/
 *                      timestamp1: double
 *                      timestamp2: double
 *                      ...
 *              status/
 *                  pefZone: int (0 = good, 1 = warning, 2 = alert)
 *
 * Navigation:
 *  Uses standard Fragment navigation and toolbar back action.
 *
 * Author: (Your Name)
 * Last Updated: November 18, 2025
 */
public class PEFZone extends Fragment {

    // ───────────────────────────────
    // UI COMPONENTS
    // ───────────────────────────────
    private Toolbar toolbar;
    private CardView cardAveragePEF;
    private TextView textPB, textAveragePEF;
    private EditText editTextPB;
    private Button buttonUpdate;

    // ───────────────────────────────
    // DATA VARIABLES
    // ───────────────────────────────
    private String name, uname,user;
    private String status;
    private double pb, averagePEF;
    private double[] pefs;

    // ───────────────────────────────
    // LIFECYCLE: Initialization
    // ───────────────────────────────
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve child name and username from previous fragment
        if (getArguments() != null) {
            name = getArguments().getString("childName");
            uname = getArguments().getString("childUname");
            user = getArguments().getString("user");
        }
    }

    // ───────────────────────────────
    // LIFECYCLE: View Binding & Setup
    // ───────────────────────────────
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_parent_set_pef, container, false);

        setupToolbar(view);

        // Bind UI elements
        cardAveragePEF = view.findViewById(R.id.cardAveragePEF);
        textPB = view.findViewById(R.id.textPB);
        textAveragePEF = view.findViewById(R.id.textAveragePEF);
        editTextPB = view.findViewById(R.id.editTextPB);
        buttonUpdate = view.findViewById(R.id.buttonUpdate);

        if (user.equals("parent")){
            buttonUpdate.setOnClickListener(v -> updatePB());
        }else{
            editTextPB.setVisibility(View.GONE);
            editTextPB.setOnClickListener(null);
            buttonUpdate.setVisibility(View.GONE);
            buttonUpdate.setOnClickListener(null);
        }


        // Load stored PEF and PB values
        getPEF();

        return view;
    }

    /** Toolbar configuration — sets back navigation and title */
    private void setupToolbar(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        setHasOptionsMenu(true);

        toolbar.setTitle(name + "'s PEF");

        // Back action
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());
    }

    /**
     * Retrieves PB and daily PEF values from Firebase,
     * converts them to numeric values, and triggers display update.
     */
    private void getPEF() {
        DatabaseReference pefRef = FirebaseDatabase.getInstance()
                .getReference("categories/users/children")
                .child(uname)
                .child("data");

        pefRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                // Load PB
                pb = snapshot.child("pb").getValue(Double.class) != null ? snapshot.child("pb").getValue(Double.class) : 0;

                // Load daily PEF measurements
                if (snapshot.child("dailyPEF").exists()) {
                    DataSnapshot pefListSnap = snapshot.child("dailyPEF");
                    pefs = new double[(int) pefListSnap.getChildrenCount()];
                    int index = 0;

                    for (DataSnapshot item : pefListSnap.getChildren()) {
                        pefs[index++] = item.getValue(Double.class) != null ? item.getValue(Double.class) : 0;
                    }

                    // Compute average
                    averagePEF = Arrays.stream(pefs).average().orElse(0);
                }

                setPEF();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Cannot access Data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Determines zone status, updates UI text & color, and saves zone to Firebase.
     */
    private void setPEF() {

        // Determine zone logic only if PB exists
        if (pb > 0 && averagePEF > 0) {
            if (averagePEF >= pb * 0.8) status = "good";
            else if (averagePEF >= pb * 0.5) status = "warning";
            else status = "alert";
        } else {
            status = "good"; // Default if no PB or values exist
        }

        // Update UI
        textPB.setText(pb > 0 ? "Personal Best: " + pb : "Haven't set Personal Best yet");
        textAveragePEF.setText(averagePEF > 0 ? "Average Daily PEF: " + averagePEF : "There is no PEF entry today");

        // Apply color theme
        int colorId = status.equals("good") ? R.color.good : status.equals("warning") ? R.color.warning : R.color.alert;
        cardAveragePEF.setCardBackgroundColor(getResources().getColor(colorId));

        // Convert status to numeric constant
        int pefZoneValue = status.equals("good") ? 0 : status.equals("warning") ? 1 : 2;

        // Save result in Firebase
        FirebaseDatabase.getInstance()
                .getReference("categories/users/children")
                .child(uname)
                .child("status")
                .child("pefZone")
                .setValue(pefZoneValue);
    }

    /**
     * Validates and updates the Personal Best value in Firebase.
     * Refreshes UI and recalculates zone afterward.
     */
    private void updatePB() {
        String pbStr = editTextPB.getText().toString().trim();

        if (pbStr.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a value.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate numeric
        double newPB = Double.parseDouble(pbStr);
        if (newPB <= 0) {
            Toast.makeText(getContext(), "PB must be greater than 0.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseDatabase.getInstance()
                .getReference("categories/users/children")
                .child(uname)
                .child("data")
                .child("pb")
                .setValue(newPB)
                .addOnSuccessListener(a -> {
                    Toast.makeText(getContext(), "Personal Best updated!", Toast.LENGTH_SHORT).show();
                    pb = newPB;
                    setPEF();
                });
    }
}

