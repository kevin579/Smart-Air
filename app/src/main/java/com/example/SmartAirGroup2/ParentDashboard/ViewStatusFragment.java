package com.example.SmartAirGroup2.ParentDashboard;

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

import com.example.SmartAirGroup2.Helpers.MenuHelper;
import com.example.SmartAirGroup2.R;
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



public class ViewStatusFragment extends Fragment {


    private Toolbar toolbar;


    private CardView cardPEF;
    private TextView PEFZone,RescueTime,RescueCount;



    private String name;

    private String uname;

    private double pb, averagePEF;

    private RescueTrendFetcher trendFetcher;
    private Context chartContext;

    private BarChart chart;
    private TextView statusTextView;
    private MaterialButtonToggleGroup toggleGroup;

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
        View view = inflater.inflate(R.layout.fragment_view_child_status, container, false);

        // ─────────────────────────────────────────────────────────────────
        // Toolbar Configuration
        // ─────────────────────────────────────────────────────────────────
        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        setHasOptionsMenu(true);

        // Set personalized title with child's name
        toolbar.setTitle(name + "'s Dashboard");

        // Enable back navigation to parent dashboard
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        // ─────────────────────────────────────────────────────────────────
        // UI Component Initialization
        // ─────────────────────────────────────────────────────────────────
        cardPEF = view.findViewById(R.id.cardPEF);


        PEFZone = view.findViewById(R.id.PEFZone);
        RescueTime = view.findViewById(R.id.RescueTime);
        RescueCount = view.findViewById(R.id.RescueCount);

        chart = view.findViewById(R.id.bar_chart);
        statusTextView = view.findViewById(R.id.tv_chart_status);
        toggleGroup = view.findViewById(R.id.toggle_duration_group);

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


        // 4. Load Data
        getPEF();
        getLastRescueTime();
        getWeeklyCount();
        onDurationToggleClicked(7);

        return view;
    }

    /**
     * Fetches the child's Personal Best (PB) PEF and their recent daily PEF measurements
     * from the Firebase Realtime Database. It calculates the average of the daily PEF
     * readings and then calls {@link #setPEF()} to update the UI based on these values.
     * <p>
     * The method retrieves data from {@code /categories/users/children/<uname>/data/}.
     * It handles cases where PB or daily PEF values might be missing. If data retrieval
     * fails, it displays a toast message.
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
                    double[] pefs = new double[(int) pefListSnap.getChildrenCount()];
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
        String txt = "PEF: "+averagePEF+"/"+pb;
        PEFZone.setText(txt);
        if (pb > 0 && averagePEF > 0) {
            if (averagePEF >= pb * 0.8) cardPEF.setCardBackgroundColor(getResources().getColor(R.color.good));
            else if (averagePEF >= pb * 0.5) cardPEF.setCardBackgroundColor(getResources().getColor(R.color.warning));
            else cardPEF.setCardBackgroundColor(getResources().getColor(R.color.alert));
        } else {
            cardPEF.setCardBackgroundColor(getResources().getColor(R.color.good));
            PEFZone.setText("No PEF or PB entries yet");
        }


    }


    /**
     * Retrieves the timestamp of the most recent rescue log entry from Firebase.
     * <p>
     * This method queries the 'rescue_log' node for the current child, ordering the entries
     * by their 'dateTime' field and limiting the result to the last (most recent) one.
     * The timestamp is then displayed in the {@code RescueTime} TextView.
     * <p>
     * If no rescue logs are found, it updates the UI to indicate this. If an error occurs
     * during the database operation, it displays a toast message and sets an error state.
     */
    private void getLastRescueTime() {
        if (uname == null || uname.trim().isEmpty()) {
            RescueTime.setText("Error: Child username missing");
            return;
        }

        // 1. Define the full path to the rescue_log node
        DatabaseReference rescueRef = FirebaseDatabase.getInstance()
                .getReference("categories/users/children")
                .child(uname)
                .child("logs")
                .child("rescue_log");

        // 2. Create the query: order by 'dateTime' descending, and limit to 1 result
        // Note: The dateTime format "YYYY-MM-DD HH:MM:SS" allows for correct string comparison/ordering.
        Query lastRescueQuery = rescueRef.orderByChild("dateTime").limitToLast(1);

        lastRescueQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists() || !snapshot.hasChildren()) {
                    // No rescue logs found
                    RescueTime.setText("No rescues yet");
                    return;
                }

                String lastTime = "No rescues yet";

                // Because we used limitToLast(1), we iterate over the single expected result
                // The result is ordered in ascending order of the key, but we only have 1 item.
                for (DataSnapshot logSnapshot : snapshot.getChildren()) {
                    // Get the dateTime value from the rescue entry
                    String dateTime = logSnapshot.child("dateTime").getValue(String.class);

                    if (dateTime != null) {
                        lastTime = dateTime;
                        // Since it's the only one, we can break immediately
                        break;
                    }
                }

                // Update the UI element with the found time
                RescueTime.setText(lastTime);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Failed to read rescue log data.", Toast.LENGTH_SHORT).show();
                RescueTime.setText("Error fetching data");
            }
        });
    }

    /**
     * Fetches and displays the total number of rescue inhaler uses for the child
     * within the last 7 days.
     * <p>
     * This method performs the following steps:
     * <ol>
     *     <li>Checks if the child's username ({@code uname}) is valid.</li>
     *     <li>Calculates the start and end date strings for the past 7-day period. The date format
     *         ("yyyy-MM-dd HH:mm") must match the format stored in Firebase.</li>
     *     <li>Constructs a Firebase Realtime Database query on the child's 'rescue_log' node,
     *         ordering by 'dateTime' and filtering the results to include only entries within
     *         the calculated 7-day range.</li>
     *     <li>Executes the query asynchronously. On success, it counts the number of resulting
     *         log entries and updates the {@code RescueCount} TextView.</li>
     *     <li>Handles cases where no logs are found in the period or if a database error occurs.</li>
     * </ol>
     */
    private void getWeeklyCount() {
        if (uname == null || uname.trim().isEmpty()) {
            if (RescueCount != null) RescueCount.setText("Error");
            return;
        }

        // --- 1. Calculate the Date Range (7 days ago to now) ---

        // Define the required date format for querying the database
        // Must match the format used in Firebase (YYYY-MM-DD HH:MM)
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        // Get today's date and set the time to the very end of today (for the upper bound)
        Calendar today = Calendar.getInstance();
        String endDateString = dateFormat.format(today.getTime());

        // Calculate the start date (7 days ago)
        Calendar sevenDaysAgo = Calendar.getInstance();
        sevenDaysAgo.add(Calendar.DAY_OF_YEAR, -7);
        String startDateString = dateFormat.format(sevenDaysAgo.getTime());

        Log.d("RESCUE_LOG", "Querying from: " + startDateString + " to: " + endDateString);

        // --- 2. Build the Firebase Query ---

        DatabaseReference rescueRef = FirebaseDatabase.getInstance()
                .getReference("categories/users/children")
                .child(uname)
                .child("logs")
                .child("rescue_log");

        // Use Query constraints to filter by the 'dateTime' field
        Query weeklyRescueQuery = rescueRef
                .orderByChild("dateTime")
                .startAt(startDateString) // Entries equal to or after the start date
                .endAt(endDateString);     // Entries equal to or before the end date

        // --- 3. Execute the Query and Count Results ---

        weeklyRescueQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (RescueCount == null) return; // Safety check

                if (!snapshot.exists() || !snapshot.hasChildren()) {
                    // No rescue logs found in the last week
                    RescueCount.setText("Weekly Rescue Count: 0");
                    return;
                }

                // The count is simply the number of children (rescue events) in the snapshot
                long count = snapshot.getChildrenCount();

                // Update the UI element
                String txt = "Weekly Rescue Count: " + count;
                RescueCount.setText(txt);

                Log.d("RESCUE_LOG", "Weekly Rescue Count: " + count);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (RescueCount != null) {
                    Toast.makeText(getContext(), "Failed to read weekly rescue count.", Toast.LENGTH_SHORT).show();
                    RescueCount.setText("E");
                }
            }
        });
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
        MenuHelper.setupNotification(this,menu,inflater);
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