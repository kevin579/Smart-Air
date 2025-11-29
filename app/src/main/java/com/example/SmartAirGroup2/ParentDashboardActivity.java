package com.example.SmartAirGroup2;

import android.os.Bundle;

/**
 * ParentDashboardActivity serves as the main entry point (Host Activity)
 * for the Parent user's dashboard interface.
 *
 * It extends {@link BaseActivity} to automatically include session timeout management.
 * Its primary responsibility is to load the correct layout and host the
 * {@link ParentDashboardFragment}, passing necessary data (like the username)
 * to the fragment upon creation.
 */
public class ParentDashboardActivity extends BaseActivity{

    /**
     * Called when the activity is first created.
     *
     * This method initializes the activity layout, retrieves the authenticated
     * username passed from the login process, and embeds the
     * {@link ParentDashboardFragment} into the main content area.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     * previously being shut down then this Bundle contains
     * the data it most recently supplied in onSaveInstanceState(Bundle).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        // Get username sent from login
        String username = getIntent().getStringExtra("username");

        // Load fragment and pass username
        ParentDashboardFragment fragment = ParentDashboardFragment.newInstance(username);

        // Embed the fragment into the designated container view
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}