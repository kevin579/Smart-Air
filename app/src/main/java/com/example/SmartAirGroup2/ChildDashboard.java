package com.example.SmartAirGroup2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ChildDashboard extends AppCompatActivity {

    private String currentChildId = "andy6688";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_dashboard);

        String intentChildId = getIntent() == null ? null : getIntent().getStringExtra("childId");
        if (intentChildId != null && !intentChildId.isEmpty()) {
            currentChildId = intentChildId;
        }

        TextView welcome = findViewById(R.id.welcomeMessage);
        welcome.setText("Welcome, " + currentChildId + "!");

        Button btnViewLogs = findViewById(R.id.btnViewLogs);
        btnViewLogs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent toLogs = new Intent(ChildDashboard.this, ChildLogsActivity.class);
                toLogs.putExtra("childId", currentChildId);
                startActivity(toLogs);
            }
        });


        Button btnStartTriage = findViewById(R.id.btnTriage);

        // 2. Set its click listener.
        btnStartTriage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // This is the bridge that starts your data collection flow.
                launchTriageOnboarding();
            }
        });

    }

    private void launchTriageOnboarding() {
        // 1. Create an Intent to start your OnboardingActivity.
        Intent intent = new Intent(ChildDashboard.this, OnboardingActivity.class);

        // 2. Set the flag to "help" to trigger the triage data collection flow.
        intent.putExtra("onboardingType", "help");

        // 3. Pass the current child's username/ID to the OnboardingActivity.
        //    The 'currentChildId' variable already holds the correct ID.
        intent.putExtra("username", currentChildId);

        // 4. Start the activity.
        startActivity(intent);
    }
}
