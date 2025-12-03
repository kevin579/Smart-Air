package com.example.SmartAirGroup2.ParentDashboard;

import android.content.SharedPreferences;
import android.os.Bundle;

import com.example.SmartAirGroup2.Helpers.BaseActivity;
import com.example.SmartAirGroup2.R;

/**
 * Represents the main activity for the Parent Dashboard.
 * This activity serves as a container for various fragments related to the parent's view,
 * such as the main dashboard or the alert center. It handles the initial setup and
 * fragment transactions based on the intent it receives.
 */
public class ParentDashboardActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        // Get username sent from login
        String username = getIntent().getStringExtra("username");

        if (username != null && !username.trim().isEmpty()) {
            SharedPreferences appPrefs = getSharedPreferences("APP_DATA", MODE_PRIVATE);
            appPrefs.edit()
                    .putString("parentUname", username)
                    .putString("type", "parent")
                    .apply();
        }
        boolean openAlertCenter = getIntent().getBooleanExtra("open_alert_center", false);

        if (savedInstanceState == null) {
            if (openAlertCenter) {
                AlertCenterFragment alertFrag = new AlertCenterFragment();
                Bundle args = new Bundle();
                args.putString("parentUname", username);
                alertFrag.setArguments(args);

                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, alertFrag)
                        .commit();
            } else {
                ParentDashboardFragment fragment = ParentDashboardFragment.newInstance(username);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .commit();
            }
    }
    }
}