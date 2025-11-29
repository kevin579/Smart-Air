package com.example.SmartAirGroup2;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

/**
 * Manages user session inactivity timeout across the application.
 *
 * This Singleton class provides functionality to start, reset, and stop an
 * inactivity timer (default 10 minutes). When the timer expires, it triggers
 * a forced logout via the {@link MenuHelper} utility.
 *
 * This utility uses an {@link Handler} to schedule the logout action on the
 * main thread after a period of inactivity.
 */
public class SessionTimeoutManager {

    private static final String TAG = "SessionTimeoutManager";
    private static SessionTimeoutManager instance; // Singleton instance
    private final Handler handler = new Handler();
    private Context applicationContext; // Will hold the safe Application Context

    /**
     * Private constructor to enforce the Singleton pattern.
     */
    private SessionTimeoutManager() { }

    /**
     * Retrieves the single instance of the SessionTimeoutManager.
     * Must be called with a non-null context on the first invocation.
     *
     * @param context The application context, used for triggering logout system-wide.
     * @return The singleton instance of SessionTimeoutManager.
     */
    public static synchronized SessionTimeoutManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionTimeoutManager();
            // Store the safe Application Context here, guaranteeing initialization
            instance.applicationContext = context.getApplicationContext();
        }
        return instance;
    }

    /**
     * The Runnable executed when the inactivity timeout period is reached.
     * It logs out the user using the application context.
     */
    private final Runnable logoutRunnable = () -> {
        Log.d(TAG, "Inactivity timeout reached. Logging out...");
        // Use the initialized applicationContext
        MenuHelper.logoutUser(applicationContext);
    };


    /**
     * Starts the inactivity timer, or resets it if it is already running.
     * The timer is set for 10 minutes (600,000 milliseconds).
     *
     * It first removes any pending logout callbacks and then schedules a new one.
     */
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

    /**
     * Stops the inactivity timer, preventing the automatic logout.
     * This is typically called when the user manually logs out or successfully logs in.
     */
    public void stopTimer() {
        handler.removeCallbacks(logoutRunnable);
        Log.d(TAG, "Timer explicitly stopped.");
    }
}