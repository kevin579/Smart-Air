package com.example.SmartAirGroup2.ParentDashboard;

import java.util.Map;

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