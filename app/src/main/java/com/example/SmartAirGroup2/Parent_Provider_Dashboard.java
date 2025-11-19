package com.example.SmartAirGroup2;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class Parent_Provider_Dashboard extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_provider_dashboard);

        // Only add fragment if not already added
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ParentDashboardFragment())
                    .commit();
        }
    }
}