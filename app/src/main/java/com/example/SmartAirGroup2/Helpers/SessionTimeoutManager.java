package com.example.SmartAirGroup2.Helpers;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

/**
 * Manages session timeouts due to user inactivity.
 * This class uses a singleton pattern to ensure a single, coordinated timer across the application.
 * After a period of inactivity (10 minutes), it will automatically log the user out by invoking
 * the {@link MenuHelper#logoutUser(Context)} method.
 *
 * The manager is initialized with an Application Context to avoid memory leaks associated with
 * holding references to Activity contexts.
 */
public class SessionTimeoutManager {

    private static final String TAG = "SessionTimeoutManager";
    private static final long TIMEOUT_MS = 600000; // 10 minutes

    private static SessionTimeoutManager instance; // Singleton instance
    private final Handler handler = new Handler();
    private Context applicationContext; // Will hold the safe Application Context

    // Private constructor prevents direct instantiation
    private SessionTimeoutManager() { }

    /**
     * Retrieves the single instance of the SessionTimeoutManager.
     * The first time this is called, it must be with a non-null context to initialize
     * the singleton and store the Application Context. Subsequent calls can use any context.
     *
     * @param context The application context, preferably from getApplicationContext().
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
     * A Runnable that performs the logout action when the timeout is reached.
     */
    private final Runnable logoutRunnable = () -> {
        Log.d(TAG, "Inactivity timeout reached. Logging out...");
        // Use the initialized applicationContext
        if (applicationContext != null) {
            MenuHelper.logoutUser(applicationContext);
        }
    };

    /**
     * Starts or resets the inactivity timer.
     * This should be called on any user interaction (e.g., in a BaseActivity's
     * dispatchTouchEvent or onResume) to postpone the automatic logout.
     */
    public void startOrResetTimer() {
        handler.removeCallbacks(logoutRunnable);
        // Ensure applicationContext is set before posting
        if (applicationContext != null) {
            handler.postDelayed(logoutRunnable, TIMEOUT_MS);
            Log.d(TAG, "Timer reset. Next logout check in 10 minutes.");
        } else {
            Log.e(TAG, "Application Context is NULL. Cannot start timer.");
        }
    }

    /**
     * Stops the inactivity timer completely.
     * This is useful when the user manually logs out or when the app is being
     * intentionally shut down, to prevent the logout runnable from executing.
     */
    public void stopTimer() {
        handler.removeCallbacks(logoutRunnable);
        Log.d(TAG, "Timer explicitly stopped.");
    }
}
