package com.example.SmartAirGroup2.ParentDashboard;

import androidx.annotation.NonNull;

import com.example.SmartAirGroup2.ChildDashboard.ControllerLog;
import com.example.SmartAirGroup2.ChildDashboard.ControllerSchedule;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AdherenceRepository {

    public interface AdherenceCallback {
        void onResult(AdherenceCalculator.AdherenceResult result);
        void onError(Exception e);
    }

    public static void getLast7DaysAdherence(String childUname, AdherenceCallback callback) {

        DatabaseReference childRef = FirebaseDatabase.getInstance()
                .getReference("categories")
                .child("users")
                .child("children")
                .child(childUname);

        DatabaseReference scheduleRef = childRef.child("controller_schedule");
        DatabaseReference logRef = childRef.child("logs").child("controller_log");

        scheduleRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ControllerSchedule schedule = snapshot.getValue(ControllerSchedule.class);

                logRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<ControllerLog> logs = new ArrayList<>();

                        for (DataSnapshot child : snapshot.getChildren()) {
                            Long ts = child.child("timestamp").getValue(Long.class);
                            String med = child.child("medication").getValue(String.class);
                            if (ts == null) continue;
                            logs.add(new ControllerLog(ts, med));
                        }

                        long endMillis = System.currentTimeMillis();
                        long startMillis = endMillis - 6L * 24L * 60L * 60L * 1000L;

                        AdherenceCalculator.AdherenceResult result =
                                AdherenceCalculator.calculate(schedule, logs, startMillis, endMillis);

                        callback.onResult(result);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError(error.toException());
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.toException());
            }
        });
    }
}
