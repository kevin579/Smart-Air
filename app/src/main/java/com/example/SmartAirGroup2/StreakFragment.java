package com.example.SmartAirGroup2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class StreakFragment extends Fragment {

    private TextView tvCurrentStreak;
    private TextView tvLongestStreak;

    private ImageView day5_badge;
    private ImageView day10_badge;
    private ImageView day15_badge;
    private ImageView day20_badge;
    private ImageView day25_badge;
    private ImageView day30_badge;



    private Button btnCheckIn;
    private Button btnBadges;

    private String childName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve identifying info passed from previous screen
        if (getArguments() != null) {
            childName = getArguments().getString("childUname");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_streak, container, false);

        tvCurrentStreak = view.findViewById(R.id.tv_current_streak);
        tvLongestStreak = view.findViewById(R.id.tv_longest_streak);
        btnCheckIn = view.findViewById(R.id.btn_check_in);
        ImageButton btnBack = view.findViewById(R.id.btn_back);
        btnBadges = view.findViewById(R.id.btn_view_badges);
        day5_badge = view.findViewById(R.id.day5_badge);
        day10_badge = view.findViewById(R.id.day10_badge);
        day15_badge = view.findViewById(R.id.day15_badge);
        day20_badge = view.findViewById(R.id.day20_badge);
        day25_badge = view.findViewById(R.id.day25_badge);
        day30_badge = view.findViewById(R.id.day30_badge);

        btnBack.setOnClickListener(v -> requireActivity()
                        .getSupportFragmentManager()
                        .popBackStack()
        );

        btnCheckIn.setOnClickListener(v -> onCheckInClicked());

        btnBadges.setOnClickListener(v->requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new BadgeLibrary())
                .addToBackStack(null)
                .commit()
                );

        getStreakData();


        return view;
    }


    private void getStreakData() {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference()
                .child("categories")
                .child("users")
                .child("children")
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
                updateBadges(longestStreak);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load streak data", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void updateStreakViews(long current, long longest) {
        tvCurrentStreak.setText(String.valueOf(current));
        tvLongestStreak.setText(String.valueOf(longest));
    }


    private void onCheckInClicked() {

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference()
                .child("categories")
                .child("users")
                .child("children")
                .child(childName)
                .child("streaks");

        String today = getTodayString();

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

                if (today.equals(lastCompletedDate)) {
                    Toast.makeText(getContext(), "Today already checked in!", Toast.LENGTH_SHORT).show();
                    return;
                }

                currentStreak += 1;
                if (currentStreak > longestStreak) {
                    longestStreak = currentStreak;
                }

                final long updatedCurrent = currentStreak;
                final long updatedLongest = longestStreak;

                Map<String, Object> updates = new HashMap<>();
                updates.put("currentStreak", updatedCurrent);
                updates.put("longestStreak", updatedLongest);
                updates.put("lastCompletedDate", today);

                ref.updateChildren(updates)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Check In success!", Toast.LENGTH_SHORT).show();
                            updateStreakViews(updatedCurrent, updatedLongest);
                            updateBadges(updatedLongest);
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(getContext(),
                                        "Update failed: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(),
                        "Firebase error: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Return the string format，e.g. "2025-11-25"
     */
    private String getTodayString() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }

    private void updateBadges(long longestStreak){
        day5_badge.setVisibility(View.GONE);
        day10_badge.setVisibility(View.GONE);
        day15_badge.setVisibility(View.GONE);
        day20_badge.setVisibility(View.GONE);
        day25_badge.setVisibility(View.GONE);
        day30_badge.setVisibility(View.GONE);

        if(longestStreak >= 5){
            day5_badge.setVisibility(View.VISIBLE);
        }
        if(longestStreak >= 10){
            day10_badge.setVisibility(View.VISIBLE);
        }
        if(longestStreak >= 15){
            day15_badge.setVisibility(View.VISIBLE);
        }
        if(longestStreak >= 20){
            day20_badge.setVisibility(View.VISIBLE);
        }
        if(longestStreak >= 25){
            day25_badge.setVisibility(View.VISIBLE);
        }
        if(longestStreak >= 30){
            day30_badge.setVisibility(View.VISIBLE);
        }

    }
}
