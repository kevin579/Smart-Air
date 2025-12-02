package com.example.SmartAirGroup2.ChildDashboard;

import android.os.Bundle;

import com.example.SmartAirGroup2.Helpers.BaseActivity;
import com.example.SmartAirGroup2.R;

public class ChildDashboardActivity extends BaseActivity {
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