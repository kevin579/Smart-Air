package com.example.SmartAirGroup2.Helpers;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.SmartAirGroup2.Main.MainActivity;
import com.example.SmartAirGroup2.Main.OnboardingActivity;
import com.example.SmartAirGroup2.R;

/**
 * An initial splash screen activity that determines the user's starting point in the app.
 * This activity checks if the user has completed the onboarding process. If they have,
 * it navigates them directly to the {@link MainActivity}. Otherwise, it directs them
 * to the {@link OnboardingActivity}. The splash screen is then finished to prevent
 * the user from navigating back to it.
 */
public class SplashActivity extends AppCompatActivity {

    /**
     * Called when the activity is first created.
     * This method sets up the edge-to-edge display, checks the onboarding state using
     * {@link SaveState}, and launches the appropriate next activity.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down, this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}. Otherwise, it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        SaveState saveState = new SaveState(this, "0B");

        if (saveState.getState() == 1) {
            // Onboarding is done, go to MainActivity
            Intent i = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(i);
        } else {
            // Onboarding not done, go to OnboardingActivity
            Intent i = new Intent(SplashActivity.this, OnboardingActivity.class);
            startActivity(i);
        }
        // Finish SplashActivity so the user can't navigate back to it
        finish();
    }
}
