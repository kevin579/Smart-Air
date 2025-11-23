package com.example.SmartAirGroup2;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class ParentDashboardActivity extends BaseActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_dashboard);

        // Get username sent from login
        String username = getIntent().getStringExtra("username");

        // Load fragment and pass username
        ParentDashboardFragment fragment = ParentDashboardFragment.newInstance(username);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}