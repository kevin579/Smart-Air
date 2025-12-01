package com.example.SmartAirGroup2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

/**
 * ChildDashboard (Child-side view)
 * ------------------------------------------
 * Simple activity presenting a child's dashboard with quick navigation
 * to child-specific features such as logs and symptoms.
 *
 * Purpose:
 *   - Provide a lightweight, child-focused entry screen that greets the child and offers
 *     buttons to view logs and navigate to symptom tracking.
 *
 * Core Features:
 *   - Displays a personalized welcome message using the provided childId
 *   - Button to open child logs
 *   - Button to open one tap triage
 *   - Button to open technique helper
 *   - Button to open streaks and badges
 *   - Button to open symptoms page
 *
 * UI Behavior:
 *   - 4 buttons to redirect to other pages
 *   - Adiitional flair for streaks and badges page for engagement
 *   - Welcome message shows the current child id (blank if none provided)
 *
 * Navigation Flow:
 *   Parent/Child list -> ChildDashboard (this)
 *       -> ChildLogsActivity
 *       -> SymptomDashboardFragment
 *
 * Activity Arguments (Intent extras):
 *   • "childId" : Firebase key / identifier for the child. If omitted,
 *     currentChildId remains null and UI will show "Welcome, null!" (caller should set it).
 *
 * Dependencies:
 *   • BaseActivity
 *   • SymptomDashboardFragment — fragment to show symptom dashboard
 *   • ChildLogsActivity — activity that shows child's logs
 *
 * Author: Andy Liu
 * Last Updated: December 2025
 */

public class ChildDashboard extends BaseActivity {
    // ─────────────────────────────────────────────────────────────────
    // FIELDS
    // ─────────────────────────────────────────────────────────────────

    /**
     * The child identifier passed in via Intent extras.
     * Used to personalize UI and to pass along to child-specific screens.
     */
    private String currentChildId;

    // ─────────────────────────────────────────────────────────────────
    // LIFECYCLE
    // ─────────────────────────────────────────────────────────────────

    /**
     * onCreate - initialize the child dashboard UI.
     *
     * Responsibilities:
     *  - Inflate layout (R.layout.activity_child_dashboard)
     *  - Read "childId" extra from the Intent and store in currentChildId
     *  - Populate welcome message
     *  - Wire up:
     *      • btnViewLogs -> start ChildLogsActivity with childId extra
     *      • btnSymptoms -> load SymptomDashboardFragment into fragment_container
     *
     * @param savedInstanceState saved instance state bundle (nullable)
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_dashboard);

        // Try to read the childId provided by caller
        String intentChildId = getIntent() == null ? null : getIntent().getStringExtra("childId");
        if (intentChildId != null && !intentChildId.isEmpty()) {
            currentChildId = intentChildId;
        }

        // Welcome text personalization
        TextView welcome = findViewById(R.id.welcomeMessage);
        welcome.setText("Welcome, " + currentChildId + "!");

        // View Logs button -> starts ChildLogsActivity and forwards childId
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
        btnStartTriage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchTriageOnboarding();
            }
        });

        // Symptoms button -> loads SymptomDashboardFragment into fragment container
        Button btnSymptoms = findViewById(R.id.btnSymptoms);
        btnSymptoms.setOnClickListener(v -> {
            SymptomDashboardFragment sympFrag = new SymptomDashboardFragment();
            Bundle args = new Bundle();
            args.putString("childUname", currentChildId);
            args.putString("childName", currentChildId);
            args.putString("user", "parent");
            sympFrag.setArguments(args);
            loadFragment(sympFrag);
        });
    }

    // ─────────────────────────────────────────────────────────────────
    // NAVIGATION HELPERS
    // ─────────────────────────────────────────────────────────────────

    private void launchTriageOnboarding() {
        Intent intent = new Intent(ChildDashboard.this, OnboardingActivity.class);
        intent.putExtra("onboardingType", "help");
        intent.putExtra("username", currentChildId);
        startActivity(intent);
    }

    /**
     * Replace the fragment_container with the provided fragment and add the transaction
     * to the back stack so the user can navigate back.
     *
     * Notes:
     *  - Expects the activity layout to contain a view with id R.id.fragment_container.
     *  - Commits immediately.
     *
     * @param fragment Fragment instance to display
     */
    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);

        transaction.addToBackStack(null);
        transaction.commit();
    }
}
