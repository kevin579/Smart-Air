package com.example.SmartAirGroup2.ChildDashboard;

import android.os.Bundle;

import com.example.SmartAirGroup2.Helpers.BaseActivity;
import com.example.SmartAirGroup2.R;

/**
 * The main activity for the child's dashboard.
 * This activity serves as a container for the {@link ChildDashboardFragment},
 * which displays the child's main interface.
 */
public class ChildDashboardActivity extends BaseActivity {
    /**
     * Called when the activity is first created. This method sets up the view,
     * retrieves the username passed from the login screen, and loads the
     * {@link ChildDashboardFragment} into the activity's fragment container.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        // Get username sent from login
        String username = getIntent().getStringExtra("username");

        // Load fragment and pass username
        ChildDashboardFragment fragment = ChildDashboardFragment.newInstance(username);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
