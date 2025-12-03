package com.example.SmartAirGroup2.ProviderDashboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

import com.example.SmartAirGroup2.Helpers.MenuHelper;
import com.example.SmartAirGroup2.R;
import com.example.SmartAirGroup2.ParentDashboard.TriageFilterFragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ProviderTriageLog
 * -------------------
 * A fragment designed for healthcare providers to view a child's triage history.
 *
 * This fragment fetches and displays a list of triage entries from Firebase. It offers
 * robust filtering capabilities, allowing the provider to narrow down the history based on
 * text content (e.g., red flags), a date range, or specific trigger keywords. This view
 * is read-only.
 *
 * Core Features:
 *  - Displays a list of triage incidents for a specific child.
 *  - Fetches data from the Firebase Realtime Database (`/categories/users/children/{childId}/data/triages`).
 *  - Integrates with {@link TriageFilterFragment} to receive filter criteria.
 *  - Dynamically builds and displays {@link CardView}s for each triage entry.
 *  - Supports clearing applied filters to return to the full list.
 *
 * Usage:
 *  This fragment should be instantiated with the child's name and unique username passed as arguments.
 *
 * Originally adapted from a symptom history-style fragment, this class has been modified to
 * handle the structure and fields of triage data.
 */
public class ProviderTriageLog extends Fragment {

    private Toolbar toolbar;
    private LinearLayout triageContainer; // Renamed from symptomContainer

    private String name, uname;
    private String filterTriageField, filterStartDate, filterEndDate; // Renamed from filterSymptom
    private List<String> filterTriggers;

    private View view;

    /**
     * Called when the fragment is first created.
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state, this is the state.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            name = getArguments().getString("childName");
            uname = getArguments().getString("childUname");
        }
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_provider_triage_log, container, false);

        CardView cardFilter = view.findViewById(R.id.cardFilter);
        cardFilter.setOnClickListener(v -> {
            TriageFilterFragment filterFrag = new TriageFilterFragment();
            loadFragment(filterFrag);
        });

        // Toolbar setup
        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        setHasOptionsMenu(true);
        toolbar.setTitle(name + "'s Triage History");
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        triageContainer = view.findViewById(R.id.triageContainer); // Updated container

        getParentFragmentManager().setFragmentResultListener("triageFilter", this, (requestKey, bundle) -> {
            filterTriageField = bundle.getString("filter_triage_field", null);
            filterStartDate = bundle.getString("filter_start_date", null);
            filterEndDate = bundle.getString("filter_end_date", null);
            filterTriggers = bundle.getStringArrayList("filter_triggers");

            updateClearFilterButton();
            loadTriages();
        });

        loadTriages();

        return view;
    }

    /**
     * Manages the visibility and action of the "Clear Filters" button.
     * The button is only shown if there are active filters.
     */
    private void updateClearFilterButton() {
        LinearLayout filterActionContainer = view.findViewById(R.id.filterActionContainer);
        filterActionContainer.removeAllViews();

        boolean hasFilters =
                (filterTriageField != null && !filterTriageField.isEmpty()) ||
                        (filterStartDate != null && !filterStartDate.isEmpty()) ||
                        (filterEndDate != null && !filterEndDate.isEmpty()) ||
                        (filterTriggers != null && !filterTriggers.isEmpty());

        if (!hasFilters) return;

        Button clearBtn = new Button(requireContext());
        clearBtn.setText("Clear Filters");
        clearBtn.setAllCaps(false);
        clearBtn.setPadding(20, 20, 20, 20);
        clearBtn.setOnClickListener(v -> {
            filterTriageField = null;
            filterStartDate = null;
            filterEndDate = null;
            if (filterTriggers != null) filterTriggers.clear();

            updateClearFilterButton();
            loadTriages();
        });

        filterActionContainer.addView(clearBtn);
    }

    /**
     * Loads Triage entries from Firebase, applies any active filters, and populates the UI.
     */
    private void loadTriages() {
        if (!isAdded() || view == null) return;

        DatabaseReference triageRef = FirebaseDatabase.getInstance()
                .getReference("categories/users/children")
                .child(uname)
                .child("data/triages"); // 🔹 Updated path

        triageRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                triageContainer.removeAllViews();

                if (!snapshot.exists()) {
                    addTriageCard("", "No triage recorded", "", "", "", "");
                    return;
                }

                for (DataSnapshot triageSnapshot : snapshot.getChildren()) {
                    String id = triageSnapshot.getKey();

                    Object redflagsObj = triageSnapshot.child("redflags").getValue();
                    String redflags = mapToString(redflagsObj);
                    String time = triageSnapshot.child("time").getValue(String.class);
                    String guidance = triageSnapshot.child("guidance").getValue(String.class);
                    String response = triageSnapshot.child("response").getValue(String.class);
                    String PEF = triageSnapshot.child("PEF").getValue(String.class);

                    if (!passesFilter(redflags, time, guidance, response, PEF)) continue;

                    addTriageCard(id, redflags, time, guidance, response, PEF);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // 🔹 Updated log tag
                Toast.makeText(getContext(), "Error loading triages: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Helper method to convert a map object (from Firebase) into a comma-separated string.
     * @param value The object to convert.
     * @return A string representation of the map's keys where the value is true.
     */
    private String mapToString(Object value) {
        if (value == null) return "";

        if (value instanceof String)
            return (String) value;

        if (value instanceof Map) {
            StringBuilder sb = new StringBuilder();
            Map<?, ?> map = (Map<?, ?>) value;

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if ((boolean) entry.getValue()){
                    sb.append(entry.getKey()).append(", ");
                }
            }

            return sb.toString().trim();
        }

        return value.toString();
    }


    /**
     * Checks if a given triage entry passes the currently set filters.
     * @param redflags The red flags associated with the triage.
     * @param time The timestamp of the triage.
     * @param guidance The guidance given for the triage.
     * @param response The response to the triage.
     * @param PEF The PEF value associated with the triage.
     * @return True if the entry passes all active filters, false otherwise.
     */
    private boolean passesFilter(String redflags, String time, String guidance, String response, String PEF) {

        // --- Make everything null-safe ---
        if (redflags == null) redflags = "";
        if (time == null) time = "";
        if (guidance == null) guidance = "";
        if (response == null) response = "";
        if (PEF == null) PEF = "";

        // --- TEXT FILTER ---
        if (filterTriageField != null && !filterTriageField.trim().isEmpty()) {
            if (!redflags.toLowerCase().contains(filterTriageField.toLowerCase())) {
                return false;
            }
        }

        // --- DATE FILTER ---
        try {
            String dateOnly = time.length() >= 10 ? time.substring(0, 10) : time;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
            Date entryDate = sdf.parse(dateOnly);

            Date startDate = (filterStartDate != null && !filterStartDate.isEmpty()) ?
                    sdf.parse(filterStartDate) : null;

            Date endDate = (filterEndDate != null && !filterEndDate.isEmpty()) ?
                    sdf.parse(filterEndDate) : null;

            if (startDate != null && entryDate.before(startDate)) return false;
            if (endDate != null && entryDate.after(endDate)) return false;

        } catch (Exception e) {
            e.printStackTrace();
        }

        // --- TRIGGER LIST FILTER ---
        if (filterTriggers != null && !filterTriggers.isEmpty()) {

            String entryTriggersLower = redflags.toLowerCase();
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
     * Creates a styled CardView for a single triage entry and adds it to the layout.
     * @param triageId The unique ID of the triage entry.
     * @param redflags The red flags associated with the triage.
     * @param time The timestamp of the triage.
     * @param guidance The guidance given for the triage.
     * @param response The response to the triage.
     * @param PEF The PEF value associated with the triage.
     */
    @SuppressLint("ResourceType")
    private void addTriageCard(String triageId, String redflags, String time, String guidance, String response, String PEF) {
        if (!isAdded() || getContext() == null) return;
        Context ctx = requireContext();

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

        TypedValue outValue = new TypedValue();
        if (ctx.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)) {
            Drawable selectable = ContextCompat.getDrawable(ctx, outValue.resourceId);
            if (selectable != null) cardView.setForeground(selectable);
        }

        LinearLayout outerLayout = new LinearLayout(ctx);
        outerLayout.setOrientation(LinearLayout.VERTICAL);
        outerLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        LinearLayout topRow = new LinearLayout(ctx);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleView = new TextView(ctx);
        titleView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        titleView.setText(redflags); // Title now redflags instead of symptom
        titleView.setTextSize(20);
        titleView.setTypeface(null, Typeface.BOLD);

        if (triageId.isEmpty()) { // Placeholder
            topRow.addView(titleView);
            outerLayout.addView(topRow);
            cardView.addView(outerLayout);
            triageContainer.addView(cardView); // Use updated container
            return;
        }


        topRow.addView(titleView);

        TextView timeView = buildInfoText("Time: " + time);
        TextView guidanceView = buildInfoText("Guidance: " + guidance);
        TextView responseView = buildInfoText("Response: " + response);
        TextView PEFView = buildInfoText("PEF: " + PEF);

        outerLayout.addView(topRow);
        outerLayout.addView(timeView);
        outerLayout.addView(guidanceView);
        outerLayout.addView(responseView);
        outerLayout.addView(PEFView);

        cardView.addView(outerLayout);

        triageContainer.addView(cardView);
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
    // MENU HANDLING (unchanged)
    // ───────────────────────────────
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        MenuHelper.setupMenuWithoutAlerts(menu, inflater, requireContext());
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return MenuHelper.handleMenuSelection(item, this) || super.onOptionsItemSelected(item);
    }

    // ───────────────────────────────
    // FRAGMENT NAVIGATION (unchanged)
    // ───────────────────────────────
    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}