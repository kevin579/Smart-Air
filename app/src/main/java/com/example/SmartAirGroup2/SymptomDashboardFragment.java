package com.example.SmartAirGroup2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * SymptomDashboardFragment
 * ---------------------------------------------------------
 * This fragment acts as the main landing page for the symptom tracking module.
 * It provides the parent user access to the following actions:
 *
 *  - Add Symptom: Opens form allowing the parent or child to record new symptoms.
 *  - View History: Opens the historical symptom log, with filtering capabilities.
 *  - Export Symptoms : Intended to allow exporting the symptom records to CSV or PDF.
 *
 * Navigation:
 *  This fragment is designed to sit within the main container activity and uses
 *  fragment transactions for navigation (not new activities).
 *
 * Expected Fragment Arguments:
 *  - "childName" : String — displayed in UI title
 *  - "childUname": String — unique identifier used to fetch and store Firebase symptom data
 *
 * Firebase Usage:
 *  No read/write occurs in this fragment. Firebase references are passed to the
 *  next fragments (AddSymptomFragment & SymptomHistoryFragment), which perform actual operations.
 *
 * Last Updated: Nov 18, 2025
 * Author: Kevin Li
 */
public class SymptomDashboardFragment extends Fragment {

    // ───────────────────────────────
    // UI COMPONENTS
    // ───────────────────────────────
    private Toolbar toolbar;
    private CardView cardAddSymptom, cardExport, cardViewHistory;

    // ───────────────────────────────
    // RUNTIME DATA (Passed from previous screen)
    // ───────────────────────────────
    private String name, uname;

    // ───────────────────────────────
    // LIFECYCLE METHODS
    // ───────────────────────────────

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve identifying info passed from previous screen
        if (getArguments() != null) {
            name = getArguments().getString("childName");
            uname = getArguments().getString("childUname");
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.activity_child_symptom, container, false);

        toolbar = view.findViewById(R.id.toolbar);

        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        setHasOptionsMenu(true);

        toolbar.setTitle(name + "'s Symptoms");

        // Enable back navigation
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        cardAddSymptom = view.findViewById(R.id.cardAddSymptom);
        cardExport = view.findViewById(R.id.cardExport);
        cardViewHistory = view.findViewById(R.id.cardViewHistory);

        // → Add Symptom
        cardAddSymptom.setOnClickListener(v -> {
            AddSymptomFragment addFrag = new AddSymptomFragment();
            Bundle args = new Bundle();
            args.putString("childUname", uname);
            addFrag.setArguments(args);
            loadFragment(addFrag);
        });

        // → Export PDF or CSV
        cardExport.setOnClickListener(v ->{
            ExportSymptoms ExportFrag = new ExportSymptoms();
            Bundle args = new Bundle();
            args.putString("childUname", uname);
            args.putString("childName", name);
            ExportFrag.setArguments(args);
            loadFragment(ExportFrag);
        });

        // → View History
        cardViewHistory.setOnClickListener(v -> {
            SymptomHistoryFragment histFrag = new SymptomHistoryFragment();
            Bundle args = new Bundle();
            args.putString("childUname", uname);
            args.putString("childName", name);
            histFrag.setArguments(args);
            loadFragment(histFrag);
        });

        return view;
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
        return MenuHelper.handleMenuSelection(item, this) || super.onOptionsItemSelected(item);
    }

    // ───────────────────────────────
    // FRAGMENT NAVIGATION
    // ───────────────────────────────

    /**
     * Helper method for navigation to another fragment while keeping history.
     * Replaces the visible fragment inside the shared container.
     */
    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}

