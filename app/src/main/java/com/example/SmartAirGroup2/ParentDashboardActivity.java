package com.example.SmartAirGroup2;

import android.content.SharedPreferences;
import android.os.Bundle;

public class ParentDashboardActivity extends BaseActivity{
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
        } else {
            ParentDashboardFragment fragment = ParentDashboardFragment.newInstance(username);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
        }

    }
}