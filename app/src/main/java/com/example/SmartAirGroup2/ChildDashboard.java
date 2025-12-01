package com.example.SmartAirGroup2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class ChildDashboard extends BaseActivity {
    private String currentChildId = "andy6688";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_dashboard);

        String intentChildId = getIntent() == null ? null : getIntent().getStringExtra("childId");
        if (intentChildId != null && !intentChildId.isEmpty()) {
            currentChildId = intentChildId;
        }

        TextView welcome = findViewById(R.id.welcomeMessage);
        welcome.setText("Welcome, " + currentChildId + "!");

        Button btnViewLogs = findViewById(R.id.btnViewLogs);
        btnViewLogs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent toLogs = new Intent(ChildDashboard.this, ChildLogsActivity.class);
                toLogs.putExtra("childId", currentChildId);
                startActivity(toLogs);
            }
        });

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

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);

        transaction.addToBackStack(null);
        transaction.commit();
    }
}
