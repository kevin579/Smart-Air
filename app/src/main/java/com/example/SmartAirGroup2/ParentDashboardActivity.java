package com.example.SmartAirGroup2;

import android.os.Bundle;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ParentDashboardActivity extends BaseActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        // Get username sent from login
        String username = getIntent().getStringExtra("username");

        // Load fragment and pass username
        ParentDashboardFragment fragment = ParentDashboardFragment.newInstance(username);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();

        // ====== Testing controller_schedule  ======

        String childUsername = "andy6688";
        testAdherenceCalculation(childUsername);


        DatabaseReference scheduleRef = FirebaseDatabase.getInstance()
                .getReference("categories")
                .child("users")
                .child("children")
                .child(childUsername)
                .child("controller_schedule");

        scheduleRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                ControllerSchedule schedule = snapshot.getValue(ControllerSchedule.class);
                if (schedule == null) {
                    Log.d("AdherenceTest", "No schedule found for child " + childUsername);
                } else {
                    Log.d("AdherenceTest", "Schedule loaded: med=" + schedule.medication
                            + ", timesPerDay=" + schedule.timesPerDay
                            + ", daysOfWeek=" + schedule.daysOfWeek);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("AdherenceTest", "Failed to load schedule", error.toException());
            }
        });
        // ====== 测试读取 controller_schedule 结束 ======

        // ====== 测试读取 controller_log ======
        DatabaseReference logRef = FirebaseDatabase.getInstance()
                .getReference("categories")
                .child("users")
                .child("children")
                .child(childUsername)
                .child("logs")
                .child("controller_log");

        logRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                List<ControllerLog> logs = new ArrayList<>();

                for (DataSnapshot child : snapshot.getChildren()) {

                    Long ts = child.child("timestamp").getValue(Long.class);
                    String med = child.child("medication").getValue(String.class);

                    if (ts == null) {
                        continue;
                    }

                    ControllerLog log = new ControllerLog(ts, med);
                    logs.add(log);
                }

                Log.d("AdherenceTest", "Loaded " + logs.size() + " controller logs for child " + childUsername);

                for (ControllerLog log : logs) {
                    Log.d("AdherenceTest", "log ts=" + log.timestamp + ", med=" + log.medication);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("AdherenceTest", "Failed to load controller logs", error.toException());
            }
        });
    }

    private void testAdherenceCalculation(String childUsername) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference("categories")
                .child("users")
                .child("children")
                .child(childUsername);

        DatabaseReference scheduleRef = rootRef.child("controller_schedule");
        DatabaseReference logRef = rootRef.child("logs").child("controller_log");

        scheduleRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                ControllerSchedule schedule = snapshot.getValue(ControllerSchedule.class);

                if (schedule == null) {
                    Log.d("AdherenceTest", "No schedule for " + childUsername);
                } else {
                    Log.d("AdherenceTest", "Got schedule: med=" + schedule.medication
                            + ", timesPerDay=" + schedule.timesPerDay);
                }

                logRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<ControllerLog> logs = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Long ts = child.child("timestamp").getValue(Long.class);
                            String med = child.child("medication").getValue(String.class);
                            if (ts == null) continue;
                            logs.add(new ControllerLog(ts, med));
                        }

                        Log.d("AdherenceTest", "Loaded " + logs.size()
                                + " logs for " + childUsername);

                        long endMillis = System.currentTimeMillis();
                        long startMillis = endMillis - 6L * 24L * 60L * 60L * 1000L; // 7 天（包含今天）

                        AdherenceCalculator.AdherenceResult result =
                                AdherenceCalculator.calculate(schedule, logs, startMillis, endMillis);

                        Log.d("AdherenceTest", "Overall adherence (7 days) = "
                                + result.overallPercent + "%");

                        for (AdherenceCalculator.DailyAdherence d : result.dailyList) {
                            Log.d("AdherenceTest", d.date + " expected="
                                    + d.expected + " actual=" + d.actual
                                    + " percent=" + d.percent);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e("AdherenceTest", "Failed to load logs", error.toException());
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("AdherenceTest", "Failed to load schedule", error.toException());
            }
        });
    }

}