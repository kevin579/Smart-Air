package com.example.SmartAirGroup2.ProviderDashboard;

import android.os.Bundle;

import com.example.SmartAirGroup2.Helpers.BaseActivity;
import com.example.SmartAirGroup2.R;

/**
 * An activity that hosts the {@link ProviderDashboardFragment}.
 * This activity serves as the main entry point for the provider's view of the dashboard.
 */
public class ProviderDashboardActivity extends BaseActivity {

    /**
     * Called when the activity is first created.
     * <p>
     * This method initializes the activity, sets its content view, retrieves the
     * provider's username from the intent, and loads the {@link ProviderDashboardFragment}
     * into the activity's layout.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        // Get username sent from login
        String username = getIntent().getStringExtra("username");

        // Load fragment and pass username
        ProviderDashboardFragment fragment = ProviderDashboardFragment.newInstance(username);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
