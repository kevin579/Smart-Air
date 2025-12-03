package com.example.SmartAirGroup2.ParentDashboard;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import android.content.Context;
import android.widget.Toast;
import android.util.Log;
import androidx.annotation.NonNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;


/**
 * Fetches and aggregates rescue log data for trend charting (7 or 30 days).
 * Assumes Context and uname fields are accessible in the calling class scope.
 */
public class RescueTrendFetcher {

    private static final String TAG = "RescueTrendFetcher";
    private final FirebaseDatabase db;

    public RescueTrendFetcher() {
        this.db = FirebaseDatabase.getInstance();
    }

    /**
     * Loads rescue log data for the specified duration (7 or 30 days), aggregates it by day,
     * and passes the result to the ChartDataCallback.
     *
     * @param uname The child's username.
     * @param days The number of days to query (7 or 30).
     * @param context The context for Toast messages.
     * @param callback The interface to deliver the processed data to the charting component.
     */
    public void loadRescueTrendData(String uname, int days, @NonNull Context context, @NonNull ChartDataCallback callback) {
        if (uname == null || uname.trim().isEmpty()) {
            callback.onFailure("Child username is missing.");
            return;
        }

        // Ensure valid range
        if (days != 7 && days != 30) {
            callback.onFailure("Invalid duration requested. Must be 7 or 30 days.");
            return;
        }

        // --- 1. Date Range Calculation ---

        // Format used for Firebase query (must match DB format: YYYY-MM-DD HH:MM)
        SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        // Format used for Chart X-axis labels (e.g., "11-20")
        SimpleDateFormat chartLabelFormat = new SimpleDateFormat("MM-dd", Locale.getDefault());

        // Calculate the start date (X days ago)
        Calendar calStart = Calendar.getInstance();
        // Set to start of the day D-X days ago
        calStart.add(Calendar.DAY_OF_YEAR, -days + 1);
        calStart.set(Calendar.HOUR_OF_DAY, 0);
        calStart.set(Calendar.MINUTE, 0);

        // Calculate the end date (now/end of today)
        Calendar calEnd = Calendar.getInstance();
        // Set to end of the day today (future-proof query)
        calEnd.set(Calendar.HOUR_OF_DAY, 23);
        calEnd.set(Calendar.MINUTE, 59);

        String startDateString = dbDateFormat.format(calStart.getTime());
        String endDateString = dbDateFormat.format(calEnd.getTime());

        Log.d(TAG, "Fetching Trend Data for " + days + " days. Range: " + startDateString + " to " + endDateString);

        // --- 2. Build the Firebase Query ---

        DatabaseReference rescueRef = db.getReference("categories/users/children")
                .child(uname)
                .child("logs")
                .child("rescue_log");

        // Query: Order by 'dateTime' (the field containing the YYYY-MM-DD HH:MM string)
        Query trendQuery = rescueRef
                .orderByChild("dateTime")
                .startAt(startDateString)
                .endAt(endDateString);

        // --- 3. Execute the Query and Aggregate Data ---

        trendQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    callback.onDataReady(new TreeMap<>()); // Return empty map if no data
                    return;
                }

                // TreeMap ensures the dates/keys are ordered correctly for the chart
                Map<String, Integer> dailyCounts = new TreeMap<>();

                // Initialize the map with 0 for all days in the range (for sparse data)
                initializeDailyCounts(dailyCounts, calStart, days, chartLabelFormat);


                // Process the fetched rescue logs
                for (DataSnapshot logSnapshot : snapshot.getChildren()) {
                    String fullDateTime = logSnapshot.child("dateTime").getValue(String.class);

                    if (fullDateTime != null) {
                        try {
                            // Extract only the date part to use as the grouping key
                            Date logDate = dbDateFormat.parse(fullDateTime);
                            String dateKey = chartLabelFormat.format(logDate); // e.g., "11-27"

                            // Increment the count for that specific day
                            dailyCounts.put(dateKey, dailyCounts.getOrDefault(dateKey, 0) + 1);

                        } catch (ParseException e) {
                            Log.e(TAG, "Error parsing dateTime: " + fullDateTime, e);
                            // Skip invalid log entry
                        }
                    }
                }

                // --- 4. Deliver Result to Chart Component ---
                callback.onDataReady(dailyCounts);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(context, "Failed to load trend data.", Toast.LENGTH_LONG).show();
                callback.onFailure("Database error: " + databaseError.getMessage());
            }
        });
    }

    /**
     * Helper method to initialize the dailyCounts map with all days in the range set to 0.
     * This is essential to prevent gaps in the chart when no rescues occurred on a specific day.
     */
    private void initializeDailyCounts(Map<String, Integer> dailyCounts, Calendar startDate, int days, SimpleDateFormat labelFormat) {
        Calendar tempCal = (Calendar) startDate.clone();
        for (int i = 0; i < days; i++) {
            String dateKey = labelFormat.format(tempCal.getTime());
            dailyCounts.put(dateKey, 0);
            tempCal.add(Calendar.DAY_OF_YEAR, 1);
        }
    }
}