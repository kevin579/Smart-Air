package com.example.SmartAirGroup2;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

public class SessionTimeoutManager {

    private static final String TAG = "SessionTimeoutManager";
    private static SessionTimeoutManager instance; // Singleton instance
    private final Handler handler = new Handler();
    private Context applicationContext; // Will hold the safe Application Context

    // Private constructor prevents direct instantiation
    private SessionTimeoutManager() { }

    /**
     * Retrieves the single instance of the SessionTimeoutManager.
     * Must be called with a non-null context on the first invocation.
     */
    public static synchronized SessionTimeoutManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionTimeoutManager();
            // Store the safe Application Context here, guaranteeing initialization
            instance.applicationContext = context.getApplicationContext();
        }
        return instance;
    }

    // The Runnable now correctly uses the guaranteed initialized applicationContext
    private final Runnable logoutRunnable = () -> {
        Log.d(TAG, "Inactivity timeout reached. Logging out...");
        // Use the initialized applicationContext
        MenuHelper.logoutUser(applicationContext);
    };

    // ... (rest of the methods: startOrResetTimer, stopTimer)

    public void startOrResetTimer() {
        handler.removeCallbacks(logoutRunnable);
        // Ensure applicationContext is set before posting
        if (applicationContext != null) {
            handler.postDelayed(logoutRunnable, 600000);
            Log.d(TAG, "Timer reset. Next logout check in 10 minutes.");
        } else {
            Log.e(TAG, "Application Context is NULL. Cannot start timer.");
        }
    }

    // Stop the timer (e.g., when the user logs out manually)
    public void stopTimer() {
        handler.removeCallbacks(logoutRunnable);
        Log.d(TAG, "Timer explicitly stopped.");
    }
}