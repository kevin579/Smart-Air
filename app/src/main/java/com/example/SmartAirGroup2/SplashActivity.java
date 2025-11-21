package com.example.SmartAirGroup2;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.SmartAirGroup2.Helpers.SaveState;

public class SplashActivity extends AppCompatActivity {

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