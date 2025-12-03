package com.example.SmartAirGroup2.ParentDashboard;

import java.util.Map;

/**
 * Callback interface for retrieving chart data asynchronously.
 * This interface is used to handle the results of a data query operation,
 * providing a mechanism to process the data once it's ready or to handle
 * any errors that occur during the process.
 */
public interface ChartDataCallback {
    /**
     * Called when daily rescue counts are successfully calculated.
     * @param dailyCounts A map where Key=Date String (e.g., "MM-DD") and Value=Rescue Count (Integer).
     */
    void onDataReady(Map<String, Integer> dailyCounts);

    /**
     * Called when the query fails.
     * @param errorMessage The error description.
     */
    void onFailure(String errorMessage);
}