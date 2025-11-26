package com.example.SmartAirGroup2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ChildDashboardDemo extends Fragment {
    private String childName = "parry6677";
    private TextView tvCurrent;
    private TextView tvLongest;

    public ChildDashboardDemo() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.child_dashboard_demo, container, false);
        CardView cardStreak = view.findViewById(R.id.cardStreak);

        tvCurrent = view.findViewById(R.id.tvDashboardCurrentStreak);
        tvLongest = view.findViewById(R.id.tvDashboardLongestStreak);

        Button btnOpenStreak = view.findViewById(R.id.btnOpenStreak);

        btnOpenStreak.setOnClickListener(v->{
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new StreakFragment())
                    .addToBackStack(null)
                    .commit();
        });


        loadDashboardStreak();

        return view;
    }

    private void loadDashboardStreak(){
        DatabaseReference rf = FirebaseDatabase.getInstance()
                .getReference()
                .child("categories")
                .child("users")
                .child("children")
                .child(childName)
                .child("streaks");


        rf.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                Long currentVal = snapshot.child("currentStreak").getValue(Long.class);
                Long longestVal = snapshot.child("longestStreak").getValue(Long.class);
                long currentStreak;
                long longestStreak;

                if (currentVal != null) {
                    currentStreak = currentVal;
                } else {
                    currentStreak = 0L;
                }

                if (longestVal != null) {
                    longestStreak = longestVal;
                } else {
                    longestStreak = 0L;
                }
                tvCurrent.setText("Current Streaks: " + currentStreak);
                tvLongest.setText("Longest Streaks: " + longestStreak);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    @Override
    public void onResume() {
        super.onResume();
        loadDashboardStreak();
    }
}