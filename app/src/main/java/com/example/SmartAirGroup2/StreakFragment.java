package com.example.SmartAirGroup2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class StreakFragment extends Fragment {

    private TextView tvCurrentStreak, tvLongestStreak, tvTotalCompletions;
    private Button btnCompleteMedication;

    private String childName = "parry6677";

    public StreakFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_streak, container, false);

        tvCurrentStreak = view.findViewById(R.id.tvCurrentStreak);
        tvLongestStreak = view.findViewById(R.id.tvLongestStreak);
        btnCompleteMedication = view.findViewById(R.id.btnCompleteMedication);

        btnCompleteMedication.setOnClickListener(v -> onClickbutton());

        getStreakData();

        return view;
    }

    private void getStreakData(){
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("children")
                .child(childName)
                .child("streaks");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
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

                String lastCompletedDate = snapshot.child("lastCompletedDate").getValue(String.class);
                if (lastCompletedDate == null) {
                    lastCompletedDate = "";
                }

                updateStreakViews(currentStreak, longestStreak);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to get data", Toast.LENGTH_SHORT).show();
            }

        });
    }
    private void updateStreakViews(long current, long longest) {
        tvCurrentStreak.setText("Current streak: " + current + " days");
        tvLongestStreak.setText("Longest streak: " + longest + " days");
    }

    private void onClickbutton(){
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("children")
                .child(childName)
                .child("streaks");

        String time = getTimeString();

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
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

                String lastCompletedDate = snapshot.child("lastCompletedDate").getValue(String.class);
                if(lastCompletedDate == null){
                    lastCompletedDate = "";
                }
                // if recorded already today, return
                if(time.equals(lastCompletedDate)){
                    Toast.makeText(getContext(), "Already Recorded Today！", Toast.LENGTH_SHORT).show();
                    return;
                }
                currentStreak += 1;
                if (currentStreak > longestStreak) {
                    longestStreak = currentStreak;
                }

                final long updatedCurrent = currentStreak;
                final long updatedLongest = longestStreak;

                Map<String, Object> updates = new HashMap<>();
                updates.put("currentStreak", currentStreak);
                updates.put("longestStreak", longestStreak);
                updates.put("lastCompletedDate", time);

                ref.updateChildren(updates)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Recorded Success！", Toast.LENGTH_SHORT).show();
                            updateStreakViews(updatedCurrent, updatedLongest);
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(getContext(), "Recorded Failed：" + e.getMessage(), Toast.LENGTH_SHORT).show());

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }
    private String getTimeString() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        java.util.Date date = calendar.getTime();
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        return sdf.format(date);
    }
}

