package com.example.SmartAirGroup2.Helpers;

import android.util.Log;
import android.view.MotionEvent;
import androidx.appcompat.app.AppCompatActivity;

/**
 * A base activity that implements session timeout functionality.
 * Activities that extend this class will automatically reset the session timeout timer
 * on user interaction.
 */
public class BaseActivity extends AppCompatActivity {

    /**
     * A reference to the session timeout manager.
     */
    protected SessionTimeoutManager timeoutManager;

    /**
     * Called when the activity will start interacting with the user.
     * At this point, the activity is at the top of the activity stack, with user
     * input going to it.
     * It starts or resets the session timeout timer.
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
     * It resets the session timeout timer on any touch event.
     * @param ev The MotionEvent that is being dispatched.
     * @return True if the event was handled by the view, false otherwise.
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
     * Called as part of the activity lifecycle when an activity is going into the background,
     * but has not (yet) been killed.
     * The timer continues to run, allowing the app to time out if the user
     * switches away for the specified duration.
     */
    @Override
    protected void onPause() {
        super.onPause();
        // The timer continues running even when paused, allowing the app to time out
        // if the user switches to another app for 10 minutes.
    }

}
