package com.example.SmartAirGroup2;

import android.util.Log;
import android.view.MotionEvent;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Base Activity class that provides core application functionality and lifecycle
 * management features, specifically implementing the application-wide session
 * timeout and inactivity tracking.
 *
 * All activities requiring automatic logout functionality due to inactivity should
 * extend this class.
 */
public class BaseActivity extends AppCompatActivity {

    // Get a reference to your timer manager (should be initialized in Application class)
    /**
     * Protected reference to the application's singleton SessionTimeoutManager.
     * This is used to control the inactivity timer.
     */
    protected SessionTimeoutManager timeoutManager;

    /**
     * Called after {@code onStart()}, or when the activity is relaunched
     * after being paused.
     *
     * This method ensures the inactivity timer is started or reset every time
     * the user returns to an activity, confirming they are active.
     */
    @Override
    protected void onResume() {
        super.onResume();
        Log.d("Confirm","Here");
        // Access the static singleton
        SessionTimeoutManager.getInstance(this).startOrResetTimer();
    }

    /**
     * Called to process touch screen events.
     *
     * This method is overridden to capture any user touch interaction and
     * explicitly reset the session timeout timer, ensuring the user is not
     * logged out while actively interacting with the UI.
     *
     * @param ev The motion event being dispatched.
     * @return True if the event was handled, false otherwise.
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            // Access the static singleton on user interaction
            SessionTimeoutManager.getInstance(this).startOrResetTimer();
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * Called when the system is about to start resuming a subsequent activity.
     *
     * The inactivity timer is intentionally left running here. If the user
     * switches to another application or the device screen turns off for the
     * duration of the timeout period (10 minutes), the user will be logged out.
     */
    @Override
    protected void onPause() {
        super.onPause();
        // The timer continues running even when paused, allowing the app to time out
        // if the user switches to another app for 10 minutes.
    }

}