package com.example.SmartAirGroup2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

public class SymptomFragment extends Fragment {

    // ───────────────────────────────
    // UI COMPONENTS
    // ───────────────────────────────
    private Toolbar toolbar;
    private CardView cardAddSymptom, cardExport, cardViewHistory;
    private LinearLayout contentContainer;

    // ───────────────────────────────
    // FIREBASE REFERENCES
    // ───────────────────────────────
    private FirebaseDatabase db;
    private DatabaseReference symptomRef;

    // Hardcoded parent username for demonstration
    // (should later be replaced by logged-in parent’s username)
    private String name, uname, author;

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
            author = getArguments().getString("author");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_child_symptom, container, false);

        // Toolbar setup
        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        setHasOptionsMenu(true);
        toolbar.setTitle(name + "'s Symptoms");
        // Handle back navigation (up button)
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        // UI references
        contentContainer = view.findViewById(R.id.contentContainer);
        cardAddSymptom = view.findViewById(R.id.cardAddSymptom);
        cardExport = view.findViewById(R.id.cardExport);
        cardViewHistory = view.findViewById(R.id.cardViewHistory);

        // Initialize Firebase references
        db = FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/");
        symptomRef = db.getReference("categories/users/children/" + uname + "/data/symptoms");

        // Handle button logic
        cardAddSymptom.setOnClickListener(v -> {
            AddSymptomFragment addFrag = new AddSymptomFragment();
            Bundle args = new Bundle();
            args.putString("childUname", uname);
            args.putString("childName", name);
            addFrag.setArguments(args);
            loadFragment(addFrag);

        });

        cardExport.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Export Data", Toast.LENGTH_SHORT).show();
        });

        cardViewHistory.setOnClickListener(v -> {
            Toast.makeText(getContext(), "View History", Toast.LENGTH_SHORT).show();
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
