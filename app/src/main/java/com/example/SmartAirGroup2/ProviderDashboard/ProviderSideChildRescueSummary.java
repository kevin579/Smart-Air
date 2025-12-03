package com.example.SmartAirGroup2.ProviderDashboard;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.SmartAirGroup2.Helpers.MenuHelper;
import com.example.SmartAirGroup2.ParentDashboard.ChartDataCallback;
import com.example.SmartAirGroup2.ParentDashboard.RescueTrendFetcher;
import com.example.SmartAirGroup2.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * ProviderSideChildRescueSummary (Provider-Side View)
 * --------------------------------------------------
 * This fragment provides a visual summary of a child's rescue medication usage history,
 * designed for healthcare providers. It displays a bar chart illustrating the frequency
 * of rescue inhaler use over a selected period (7 or 30 days).
 *
 * Purpose:
 *   To help providers quickly assess trends in a child's rescue medication needs, which can
 *   indicate changes in asthma control.
 *
 * Core Features:
 *   • Displays a bar chart of daily rescue inhaler usage counts.
 *   • Allows toggling the view between the last 7 and 30 days.
 *   • Fetches data asynchronously using `RescueTrendFetcher`.
 *   • Shows loading and empty states to the user.
 *   • Provides back navigation to the child's main provider dashboard.
 *
 * UI Behavior:
 *   - Displays the child's name in the toolbar title (e.g., "John's Rescue History").
 *   - A `BarChart` visualizes the number of rescues per day.
 *   - A `MaterialButtonToggleGroup` allows the user to select the time frame.
 *   - A `TextView` shows status messages like "Loading data..." or "No rescue logs found."
 *
 * Firebase Structure (Relevant Paths):
 * └── categories/
 *     └── users/
 *         └── children/{childUname}/
 *             └── logs/
 *                 └── rescue_log/
 *                     └── {logId}/
 *                         └── timestamp: (long)
 *
 * Navigation Flow:
 *   ProviderSideChildDashboardFragment → ProviderSideChildRescueSummary
 *
 * Fragment Arguments (Required):
 *   • childName (String): Display name of the child, used in the toolbar.
 *   • childUname (String): Unique username/identifier for the child in Firebase, used for data fetching.
 *
 * Dependencies:
 *   • MPAndroidChart library for charting.
 *   • RescueTrendFetcher for fetching and processing data from Firebase.
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
     * Shows a title like "[Child Name]'s Rescue History" and provides back navigation.
     */
    private Toolbar toolbar;

    /**
     * A CardView for containing chart elements.
     */
    private CardView  cardTrend;



    // ═══════════════════════════════════════════════════════════════════════
    // CHILD IDENTITY
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Display name of the child.
     * Retrieved from fragment arguments, used in the toolbar title.
     */
    private String name;

    /**
     * Unique username/identifier of the child in Firebase.
     * Retrieved from fragment arguments, used to fetch the child's rescue log data.
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
     * Retrieves the child's name and unique username from fragment arguments.
     *
     * @param savedInstanceState Previously saved state, if any.
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
     *   - Inflates the layout for the rescue summary chart.
     *   - Sets up the toolbar with a dynamic title and back navigation.
     *   - Initializes the BarChart, status TextView, and duration toggle buttons.
     *   - Sets up listeners for the toggle buttons to refresh the chart data.
     *   - Initiates the initial data load for the 7-day view.
     *
     * @param inflater           LayoutInflater to inflate the view.
     * @param container          Parent view that this fragment's UI will be attached to.
     * @param savedInstanceState Previously saved state, if any.
     * @return                   The root view for this fragment.
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


    /**
     * Configures the visual appearance of the BarChart.
     * This includes disabling the description and legend, setting axis properties,
     * and defining formatters for the values.
     */
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

    /**
     * Triggered when the user selects a duration (e.g., 7 or 30 days).
     * It initiates fetching the rescue data for the specified period and updates the chart.
     *
     * @param days The number of days of data to display.
     */
    public void onDurationToggleClicked(int days) {
        if (chart == null) return;

        // Show loading state
        if (statusTextView != null) {
            statusTextView.setText("Loading data...");
            statusTextView.setVisibility(View.VISIBLE);
        }
        chart.setVisibility(View.INVISIBLE);

        // This callback has an `onFailure` method.
        // Assuming ChartDataCallback is flexible enough or has been modified to support this.
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
        MenuHelper.setupMenuWithoutAlerts(menu, inflater, requireContext());
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return MenuHelper.handleMenuSelection(item, this) || super.onOptionsItemSelected(item);
    }

}
