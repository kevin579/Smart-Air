package com.example.SmartAirGroup2;

import android.os.Bundle;

public class ProviderDashboardActivity extends BaseActivity{
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