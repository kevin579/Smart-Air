package com.example.SmartAirGroup2;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;


/**
 * ChildDashboardFragment (Parent-Side View)
 * ------------------------------------------
 * This fragment provides a comprehensive overview of a single child's health status from the
 * parent's perspective. It serves as a navigation hub for accessing detailed child information
 * across three main categories: Inventory, PEF (Peak Expiratory Flow), and Symptoms.
 *
 * Purpose:
 *   Enables parents to monitor their child's asthma management by providing:
 *   • Visual status indicators for medication inventory levels
 *   • PEF zone monitoring (Green/Yellow/Red zones)
 *   • Symptom tracking overview
 *   • Quick navigation to detailed views for each category
 *
 * Core Features:
 *   • Color-coded status cards that reflect real-time child health metrics
 *   • Three primary navigation cards:
 *       → Inventory Card: Medicine stock levels and usage
 *       → PEF Card: Breathing function measurements and zones
 *       → Symptom Card: Logged symptoms and patterns
 *   • Dynamic status updates from Firebase
 *   • Back navigation to parent dashboard
 *
 * UI Behavior:
 *   - Displays child's name in the toolbar title
 *   - Three large CardViews with color-coded backgrounds:
 *       • Inventory: Red (alert) if any medicine is low/critical, Green (good) otherwise
 *       • PEF: Red (zone 2), Yellow (zone 1), or Green (zone 0) based on breathing status
 *       • Symptom: Currently static, may be extended for symptom status
 *   - Each card navigates to its respective detailed fragment on click
 *
 * Firebase Structure (Relevant Paths):
 * └── categories/
 *     └── users/
 *         └── children/{childUname}/
 *             └── status/
 *                 ├── pefZone: Integer (0=green, 1=yellow, 2=red)
 *                 └── inventory/
 *                     └── {medicineName}/
 *                         └── {timestamp: Integer (0=good, 1=warning, 2=alert)}
 *
 * Status Logic:
 *   - Inventory Status:
 *       • 2 (Alert/Red): Any medicine has critical low stock - HIGHEST PRIORITY
 *       • 1 (Warning/Yellow): Any medicine approaching low stock
 *       • 0 (Good/Green): All medicines adequately stocked
 *   - PEF Status:
 *       • 2 (Red Zone): Severe breathing difficulty, immediate action needed
 *       • 1 (Yellow Zone): Caution, medication may be needed
 *       • 0 (Green Zone): Breathing is normal
 *
 * Navigation Flow:
 *   ParentDashboardFragment → ChildDashboardFragment → [Inventory/PEF/Symptom]Fragment
 *
 * Fragment Arguments (Required):
 *   • childName (String): Display name of the child
 *   • childUname (String): Unique username/identifier for the child in Firebase
 *
 * Dependencies:
 *   • Firebase Realtime Database for status data
 *   • MenuHelper for toolbar menu operations
 *   • Three destination fragments: InventoryFragment, ParentPEF, SymptomDashboardFragment
 *
 * Color Resources Used:
 *   • R.color.alert (Red): Critical status requiring immediate attention
 *   • R.color.warning (Yellow): Caution status requiring monitoring
 *   • R.color.good (Green): Normal/healthy status
 *
 * Author: Kevin Li
 * Last Updated: November 2025
 */
public class ProviderSideChildRescueSummary extends Fragment {

    // ═══════════════════════════════════════════════════════════════════════
    // UI COMPONENTS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Toolbar component displayed at the top of the fragment.
     * Shows the child's name and provides back navigation to parent dashboard.
     */
    private Toolbar toolbar;

    /**
     * CardView for accessing the child's status .
     * Color-coded based on medicine stock levels:
     *   - Red (alert)
     *   - Green (good)
     */
    private CardView  cardTrend;



    // ═══════════════════════════════════════════════════════════════════════
    // CHILD IDENTITY
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Display name of the child.
     * Retrieved from fragment arguments, passed from ParentDashboardFragment.
     * Used in toolbar title to personalize the view.
     */
    private String name;

    /**
     * Unique username/identifier of the child in Firebase.
     * Retrieved from fragment arguments, used to query child-specific data.
     */
    private String uname;

    private double pb, averagePEF;

    private RescueTrendFetcher trendFetcher;
    private Context chartContext;

    private BarChart chart;
    private TextView statusTextView; // tv_chart_status
    private MaterialButtonToggleGroup toggleGroup;
    private Button btn7Days;

    // ═══════════════════════════════════════════════════════════════════════
    // LIFECYCLE METHODS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Called when the fragment is first created.
     * Retrieves child identity from fragment arguments passed by parent fragment.
     *
     * Expected Arguments:
     *   - childName: Display name of the child
     *   - childUname: Firebase username/key for the child
     *
     * @param savedInstanceState Previously saved state, if any
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve child identity passed as arguments
        if (getArguments() != null) {
            name = getArguments().getString("childName");
            uname = getArguments().getString("childUname");
        }
    }

    /**
     * Creates and initializes the view hierarchy for this fragment.
     *
     * Responsibilities:
     *   - Inflates the child dashboard layout
     *   - Sets up toolbar with child's name and back navigation
     *   - Initializes status card references
     *   - Loads current status from Firebase to color-code cards
     *   - Configures click handlers for navigation to detail fragments
     *
     * @param inflater           LayoutInflater to inflate the view
     * @param container          Parent view that this fragment's UI will be attached to
     * @param savedInstanceState Previously saved state, if any
     * @return                   The root view for this fragment
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_provider_child_summary, container, false);

        // ─────────────────────────────────────────────────────────────────
        // Toolbar Configuration
        // ─────────────────────────────────────────────────────────────────
        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        setHasOptionsMenu(true);

        // Set personalized title with child's name
        toolbar.setTitle(name + "'s Rescue History");

        // Enable back navigation to parent dashboard
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        // ─────────────────────────────────────────────────────────────────
        // UI Component Initialization
        // ─────────────────────────────────────────────────────────────────




        chart = view.findViewById(R.id.bar_chart);
        statusTextView = view.findViewById(R.id.tv_chart_status);
        toggleGroup = view.findViewById(R.id.toggle_duration_group);
        btn7Days = view.findViewById(R.id.btn_7_days);

        trendFetcher = new RescueTrendFetcher();
        chartContext = requireContext();

        setupChartAppearance();

        if (toggleGroup != null) {
            toggleGroup.check(R.id.btn_7_days);
            toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (isChecked) {
                    int days = (checkedId == R.id.btn_7_days) ? 7 : 30;
                    onDurationToggleClicked(days);
                }
            });
        }


        onDurationToggleClicked(7);

        return view;
    }



    private void setupChartAppearance() {
        if (chart == null) return;

        chart.getDescription().setEnabled(false);
        chart.setFitBars(true);
        chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(false);
        chart.setDrawValueAboveBar(true);

        // Y-axis (left)
        chart.getAxisLeft().setAxisMinimum(0f);
        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisLeft().setGranularity(1f); // Integer steps
        // Ensure Y-axis labels are integers
        chart.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });


        // Y-axis (right)
        chart.getAxisRight().setEnabled(false);

        // X-axis
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

    }

    public void onDurationToggleClicked(int days) {
        if (chart == null) return;

        // Show loading state
        if (statusTextView != null) {
            statusTextView.setText("Loading data...");
            statusTextView.setVisibility(View.VISIBLE);
        }
        chart.setVisibility(View.INVISIBLE);

        trendFetcher.loadRescueTrendData(uname, days, chartContext, new ChartDataCallback() {
            @Override
            public void onDataReady(Map<String, Integer> dailyCounts) {
                if (!isAdded()) return;

                if (statusTextView != null) statusTextView.setVisibility(View.GONE);
                chart.setVisibility(View.VISIBLE);

                if (dailyCounts.isEmpty()) {
                    if (statusTextView != null) {
                        statusTextView.setText("No rescue logs found in this period.");
                        statusTextView.setVisibility(View.VISIBLE);
                    }
                    chart.setData(null);
                    chart.invalidate();
                    return;
                }

                // Prepare Data
                List<BarEntry> entries = new ArrayList<>();
                // TreeMap sorts by date string keys automatically
                Map<String, Integer> sortedDailyCounts = new TreeMap<>(dailyCounts);
                List<String> labels = new ArrayList<>(sortedDailyCounts.keySet());

                for (int i = 0; i < labels.size(); i++) {
                    String date = labels.get(i);
                    Integer count = sortedDailyCounts.get(date);
                    entries.add(new BarEntry(i, count));
                }

                // Configure DataSet
                BarDataSet dataSet = new BarDataSet(entries, "Rescues");
                dataSet.setColor(Color.parseColor("#1565C0"));
                dataSet.setValueTextColor(Color.BLACK);
                dataSet.setValueTextSize(10f);
//                dataSet.setHighLightEnabled(false);

                // Fix: Format values as integers and hide 0 values
                dataSet.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        int intValue = (int) value;
                        if (intValue == 0) {
                            return ""; // Hide 0 values
                        }
                        return String.valueOf(intValue); // Show integer
                    }
                });


                // Set Data
                BarData barData = new BarData(dataSet);
                barData.setBarWidth(0.6f); // Thinner bars look better
                chart.setData(barData);

                // Format X-Axis
                // Fix: Show every 3rd label if we have more than 10 data points (e.g., 30 days)
                if (labels.size() > 10) {
                    chart.getXAxis().setValueFormatter(new ValueFormatter() {
                        @Override
                        public String getFormattedValue(float value) {
                            int index = (int) value;
                            if (index >= 0 && index < labels.size() && index % 3 == 0) {
                                return labels.get(index);
                            }
                            return ""; // Hide other labels
                        }
                    });
                    chart.getXAxis().setLabelCount(labels.size() / 3 + 1, false);
                } else {
                    // For 7 days, show all labels
                    chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
                    chart.getXAxis().setLabelCount(labels.size(), false);
                }


                // Refresh
                chart.animateY(600);
                chart.invalidate();
            }

            @Override
            public void onFailure(String errorMessage) {
                if (!isAdded()) return;
                chart.setVisibility(View.INVISIBLE);
                if (statusTextView != null) {
                    statusTextView.setText("Error: " + errorMessage);
                    statusTextView.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MENU HANDLING
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Inflates the toolbar menu using MenuHelper.
     * Called by the Android framework when the menu is being created.
     *
     * @param menu     Menu object to be populated
     * @param inflater MenuInflater to use for inflating menu resources
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        MenuHelper.setupMenu(menu, inflater, requireContext());
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * Handles menu item selection events.
     * Delegates to MenuHelper for consistent menu behavior across the app.
     *
     * @param item The menu item that was selected
     * @return     true if the event was handled, false otherwise
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (MenuHelper.handleMenuSelection(item, this)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}