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
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * SymptomHistoryFragment
 * -----------------------
 This fragment displays a chronological history of recorded symptom entries for
 *  a selected child profile. Symptoms are retrieved from Firebase Realtime Database
 *  and dynamically rendered into scrollable UI cards. Users may apply filters such
 *  as symptom name, trigger keywords, or date range to refine displayed results.
 *
 *  FEATURES:
 *  • Retrieves symptom data from Firebase (read + delete operations)
 *  • Supports dynamic UI card creation for each entry
 *  • Allows filtering by:
 *      - Symptom keyword search
 *      - Start/end date range
 *      - Trigger matches
 *  • Provides ability to clear all filters and reload full history
 *  • Implements fragment navigation for accessing filter settings screen
 *  • Uses Android Toolbar and parent fragment manager for back navigation
 *
 * Author: Kevin Li
 * Date: Nov 19 2025
 */

public class SymptomHistoryFragment extends Fragment {

    // ───────────────────────────────
    // UI COMPONENTS
    // ───────────────────────────────
    private Toolbar toolbar;
    private CardView cardFilter;

    private String name, uname;
    private String filterSymptom, filterStartDate, filterEndDate;
    private List<String> filterTriggers;

    private View view;

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
        view = inflater.inflate(R.layout.activity_symptom_history, container, false);

        // Toolbar setup
        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        setHasOptionsMenu(true);

        toolbar.setTitle(name +"'s Symptom History");

        // Handle back navigation (up button)
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        // UI references
        cardFilter = view.findViewById(R.id.cardFilter);

        getParentFragmentManager().setFragmentResultListener("symptomFilter", this, (requestKey, bundle) -> {

            filterSymptom = bundle.getString("filter_symptom", null);
            filterStartDate = bundle.getString("filter_start_date", null);
            filterEndDate = bundle.getString("filter_end_date", null);
            filterTriggers = bundle.getStringArrayList("filter_triggers");

            //If there are filters found, add the clear button.
            updateClearFilterButton();

            loadSymptoms(); // reload with filters applied
        });

        loadSymptoms();

        cardFilter.setOnClickListener(v -> {
            SymptomFilterFragment filterFrag = new SymptomFilterFragment();
            loadFragment(filterFrag);
        });

        return view;
    }


    /**
     * Updates the UI by showing or hiding a "Clear Filters" button depending
     * on whether filters are currently active.
     */
    private void updateClearFilterButton() {
        LinearLayout filterActionContainer = view.findViewById(R.id.filterActionContainer);

        // Clear existing button if already added
        filterActionContainer.removeAllViews();

        // Check if ANY filter is active
        boolean hasFilters =
                (filterSymptom != null && !filterSymptom.isEmpty()) ||
                        (filterStartDate != null && !filterStartDate.isEmpty()) ||
                        (filterEndDate != null && !filterEndDate.isEmpty()) ||
                        (filterTriggers != null && !filterTriggers.isEmpty());

        if (!hasFilters) return; // no filters → no button shown

        // Create button dynamically
        Button clearBtn = new Button(requireContext());
        clearBtn.setText("Clear Filters");
        clearBtn.setAllCaps(false);
        clearBtn.setPadding(20, 20, 20, 20);

        clearBtn.setOnClickListener(v -> {
            filterSymptom = null;
            filterStartDate = null;
            filterEndDate = null;
            if (filterTriggers != null) filterTriggers.clear();

            updateClearFilterButton(); // remove button again
            loadSymptoms(); // reload full list
        });


        filterActionContainer.addView(clearBtn);
    }

    /**
     * Loads symptom entries from Firebase and applies active filters before displaying.
     * Clears existing UI items before reloading.
     */
    private void loadSymptoms() {
        if (!isAdded()) return;

        if (view == null) return;

        DatabaseReference statusRef = FirebaseDatabase.getInstance()
                .getReference("categories/users/children")
                .child(uname)
                .child("data/symptoms");

        // Get container BEFORE database listener
        LinearLayout symptomContainer = view.findViewById(R.id.symptomContainer);

        statusRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                // 🔥 Clear existing cards before adding new ones
                symptomContainer.removeAllViews();

                // If database is empty show placeholder
                if (!snapshot.exists()) {
                    addSymptomCard("","No symptoms recorded", "", "", "");
                    return;
                }

                // Loop through data
                for (DataSnapshot symptomSnapshot : snapshot.getChildren()) {

                    String id = symptomSnapshot.getKey(); // Firebase ID

                    String symptom = symptomSnapshot.child("symptom").getValue(String.class);
                    String time = symptomSnapshot.child("time").getValue(String.class);
                    String triggers = symptomSnapshot.child("triggers").getValue(String.class);
                    String author = symptomSnapshot.child("type").getValue(String.class);


                    if (!passesFilter(symptom, time, triggers)) continue;

                    addSymptomCard(id, symptom, time, triggers, author);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("SYMPTOM_ERROR", error.getMessage());
            }
        });
    }

    /**
     * Determines whether a symptom entry should be shown based on:
     * - Text match
     * - Date range
     * - Trigger selections
     *
     * @return true if entry passes filters, false otherwise
     */
    private boolean passesFilter(String symptom, String time, String triggers) {

        if (filterSymptom != null && !filterSymptom.trim().isEmpty()) {
            if (!symptom.toLowerCase().contains(filterSymptom.toLowerCase())) {
                return false;
            }
        }

        try {
            // Extract ONLY yyyy/MM/dd portion from stored timestamp
            String dateOnly = time.length() >= 10 ? time.substring(0, 10) : time;

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
            Date entryDate = sdf.parse(dateOnly);

            // --- HANDLE FILTER CONDITIONS ---
            Date startDate = null;
            Date endDate = null;

            // If start date is valid, parse it; otherwise leave null (means no restriction)
            if (filterStartDate != null  && !filterStartDate.isEmpty()) {
                startDate = sdf.parse(filterStartDate);
            }

            // If end date is valid parse it; otherwise assume today
            if (filterEndDate != null && !filterEndDate.isEmpty()) {
                endDate = sdf.parse(filterEndDate);
            }

            // If startDate exists and entryDate is BEFORE it → exclude
            if (startDate != null && entryDate.before(startDate)) {
                return false;
            }

            // If entryDate is AFTER end date → exclude
            if (entryDate.after(endDate)) {
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            // Fail-safe: don't filter out data if parsing failed
        }

        if (filterTriggers != null && !filterTriggers.isEmpty()) {

            String entryTriggersLower = triggers.toLowerCase();
            boolean foundMatch = false;

            for (String trigger : filterTriggers) {
                if (entryTriggersLower.contains(trigger.toLowerCase())) {
                    foundMatch = true;
                    break;
                }
            }

            if (!foundMatch) return false;
        }

        return true;
    }

    /**
     * Dynamically builds and displays a card containing symptom info.
     * Includes delete functionality if the card represents a real database entry.
     *
     * @param symptomId Firebase ID used to delete card
     */
    @SuppressLint("ResourceType")
    private void addSymptomCard(String symptomId, String symptom, String time, String triggers, String author) {
        if (!isAdded() || getContext() == null) return;
        Context ctx = requireContext();

        // ----- Card container -----
        CardView cardView = new CardView(ctx);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dpToPx(16));
        cardView.setLayoutParams(cardParams);
        cardView.setCardBackgroundColor(0xFFC8E6C9);
        cardView.setRadius(dpToPx(8));
        cardView.setCardElevation(4);

        // Touch ripple effect
        TypedValue outValue = new TypedValue();
        if (ctx.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)) {
            Drawable selectable = ContextCompat.getDrawable(ctx, outValue.resourceId);
            if (selectable != null) cardView.setForeground(selectable);
        }

        // ----- Inner layout -----
        LinearLayout outerLayout = new LinearLayout(ctx);
        outerLayout.setOrientation(LinearLayout.VERTICAL);
        outerLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        // ======= Title Row =======
        LinearLayout topRow = new LinearLayout(ctx);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleView = new TextView(ctx);
        titleView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        titleView.setText(symptom);
        titleView.setTextSize(20);
        titleView.setTypeface(null, Typeface.BOLD);

        if (symptomId==""){
            topRow.addView(titleView);
            outerLayout.addView(topRow);
            cardView.addView(outerLayout);
            LinearLayout container = requireView().findViewById(R.id.symptomContainer);
            container.addView(cardView);
            return;
        }

        ImageView deleteView = new ImageView(ctx);
        deleteView.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(24), dpToPx(24)));
        deleteView.setImageResource(android.R.drawable.ic_delete);
        deleteView.setColorFilter(ContextCompat.getColor(ctx, R.color.delete), PorterDuff.Mode.SRC_IN);

        deleteView.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Delete Symptom")
                    .setMessage("Are you sure you want to delete this symptom entry?")
                    .setPositiveButton("Delete", (dialog, which) -> {

                        DatabaseReference symptomRef = FirebaseDatabase.getInstance()
                                .getReference("categories/users/children")
                                .child(uname)
                                .child("data/symptoms")
                                .child(symptomId);

                        symptomRef.removeValue()
                                .addOnSuccessListener(a -> {
                                    Toast.makeText(ctx, "Symptom removed", Toast.LENGTH_SHORT).show();
                                    loadSymptoms();

                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(ctx, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                );

                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        topRow.addView(titleView);
        topRow.addView(deleteView);

        // ======= Info Labels =======
        TextView timeView = buildInfoText("Time: " + time);
        TextView triggerView = buildInfoText("Triggers: " + triggers);
        TextView authorView = buildInfoText("Entered by: " + author);

        outerLayout.addView(topRow);
        outerLayout.addView(timeView);
        outerLayout.addView(triggerView);
        outerLayout.addView(authorView);

        cardView.addView(outerLayout);

        // ----- Add card to layout -----
        LinearLayout container = requireView().findViewById(R.id.symptomContainer);
        container.addView(cardView);
    }

    private TextView buildInfoText(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(14);
        tv.setTextColor(Color.DKGRAY);
        tv.setPadding(0, dpToPx(4), 0, 0);
        return tv;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
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
