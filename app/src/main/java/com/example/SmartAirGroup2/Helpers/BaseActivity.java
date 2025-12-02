package com.example.SmartAirGroup2.Helpers;

import android.util.Log;
import android.view.MotionEvent;
import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {

    // Get a reference to your timer manager (should be initialized in Application class)
    protected SessionTimeoutManager timeoutManager;

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("Confirm","Here");
        // Access the static singleton
        SessionTimeoutManager.getInstance(this).startOrResetTimer();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            // Access the static singleton on user interaction
            SessionTimeoutManager.getInstance(this).startOrResetTimer();
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // The timer continues running even when paused, allowing the app to time out
        // if the user switches to another app for 10 minutes.
    }

}