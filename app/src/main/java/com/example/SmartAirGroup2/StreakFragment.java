package com.example.SmartAirGroup2;

import android.content.Context;
import android.content.SharedPreferences;
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

    // Controller streak
    private TextView tvCurrentStreak;
    private TextView tvLongestStreak;

    // Technique streak
    private TextView tvTechCurrent;
    private TextView tvTechLongest;

    // Buttons
    private Button btnControllerCheckIn;
    private Button btnTechniqueCheckIn;
    private Button btnBadges;

    // Badges (业务徽章)
    private ImageView badgePerfectWeek;
    private ImageView badgeTenTech;
    private ImageView badgeLowRescue;

    private String uname;   // child username

    public StreakFragment() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_streak, container, false);

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("APP_DATA", Context.MODE_PRIVATE);
        uname = prefs.getString("childUname", "andy6688");

        // controller streak
        tvCurrentStreak = view.findViewById(R.id.tv_current_streak);
        tvLongestStreak = view.findViewById(R.id.tv_longest_streak);

        // technique streak
        tvTechCurrent = view.findViewById(R.id.tv_tech_current_streak);
        tvTechLongest = view.findViewById(R.id.tv_tech_longest_streak);

        // buttons
        btnControllerCheckIn = view.findViewById(R.id.btn_check_in);
        btnTechniqueCheckIn = view.findViewById(R.id.btn_technique_check_in);
        btnBadges = view.findViewById(R.id.btn_view_badges);
        ImageButton btnBack = view.findViewById(R.id.btn_back);

        badgePerfectWeek = view.findViewById(R.id.badge_perfect_week);
        badgeTenTech = view.findViewById(R.id.badge_ten_tech);
        badgeLowRescue = view.findViewById(R.id.badge_low_rescue);

        hideAllBadgeContainers();

        btnBack.setOnClickListener(v ->
                requireActivity()
                        .getSupportFragmentManager()
                        .popBackStack()
        );

        // Controller check-in
        btnControllerCheckIn.setOnClickListener(v -> onControllerCheckInClicked());

        btnTechniqueCheckIn.setOnClickListener(v -> onTechniqueCheckInClicked(true));

        btnBadges.setOnClickListener(v ->
                requireActivity()
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new BadgeLibrary())
                        .addToBackStack(null)
                        .commit()
        );

        getStreakData();
        updateBusinessBadges();

        return view;
    }

    private DatabaseReference getChildRef() {
        return FirebaseDatabase.getInstance()
                .getReference()
                .child("categories")
                .child("users")
                .child("children")
                .child(uname);
    }

    private void getStreakData() {
        DatabaseReference streaksRef = getChildRef().child("streaks");

        streaksRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // controller
                DataSnapshot controllerSnap = snapshot.child("controller");
                Long cCurrentVal = controllerSnap.child("currentStreak").getValue(Long.class);
                Long cLongestVal = controllerSnap.child("longestStreak").getValue(Long.class);

                long cCurrent = (cCurrentVal != null) ? cCurrentVal : 0L;
                long cLongest = (cLongestVal != null) ? cLongestVal : 0L;

                // technique
                DataSnapshot techSnap = snapshot.child("technique");
                Long tCurrentVal = techSnap.child("currentStreak").getValue(Long.class);
                Long tLongestVal = techSnap.child("longestStreak").getValue(Long.class);

                long tCurrent = (tCurrentVal != null) ? tCurrentVal : 0L;
                long tLongest = (tLongestVal != null) ? tLongestVal : 0L;

                updateStreakViews(cCurrent, cLongest, tCurrent, tLongest);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(),
                        "Failed to load streak data",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateStreakViews(long cCurrent, long cLongest,
                                   long tCurrent, long tLongest) {
        tvCurrentStreak.setText(String.valueOf(cCurrent));
        tvLongestStreak.setText(String.valueOf(cLongest));

        tvTechCurrent.setText(String.valueOf(tCurrent));
        tvTechLongest.setText(String.valueOf(tLongest));
    }

    /**
     * Controller Medication Check in
     */
    private void onControllerCheckInClicked() {
        DatabaseReference ref = getChildRef()
                .child("streaks")
                .child("controller");

        String today = getTodayString();

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                Long currentVal = snapshot.child("currentStreak").getValue(Long.class);
                Long longestVal = snapshot.child("longestStreak").getValue(Long.class);
                String lastCompletedDate = snapshot.child("lastCompletedDate").getValue(String.class);

                long currentStreak = (currentVal != null) ? currentVal : 0L;
                long longestStreak = (longestVal != null) ? longestVal : 0L;
                if (lastCompletedDate == null) lastCompletedDate = "";

                if (today.equals(lastCompletedDate)) {
                    Toast.makeText(getContext(), "Today already checked in!", Toast.LENGTH_SHORT).show();
                    return;
                }

                long oldCurrent = currentStreak;
                long newCurrent = updateStreakForToday(lastCompletedDate, currentStreak);
                long newLongest = Math.max(newCurrent, longestStreak);

                Map<String, Object> updates = new HashMap<>();
                updates.put("currentStreak", newCurrent);
                updates.put("longestStreak", newLongest);
                updates.put("lastCompletedDate", today);

                ref.updateChildren(updates)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(),
                                    "Controller check-in success!",
                                    Toast.LENGTH_SHORT).show();

                            // 更新 UI（controller 部分）
                            tvCurrentStreak.setText(String.valueOf(newCurrent));
                            tvLongestStreak.setText(String.valueOf(newLongest));

                            // 这里如果你有 perfect week badge 的解锁逻辑，
                            // 可以在成功后调用相应检查函数，然后再 updateBusinessBadges()
                            updateBusinessBadges();
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


    private void onTechniqueCheckInClicked(boolean isHighQuality) {
        DatabaseReference ref = getChildRef()
                .child("streaks")
                .child("technique");

        String today = getTodayString();

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                Long currentVal = snapshot.child("currentStreak").getValue(Long.class);
                Long longestVal = snapshot.child("longestStreak").getValue(Long.class);
                String lastCompletedDate = snapshot.child("lastCompletedDate").getValue(String.class);

                long currentStreak = (currentVal != null) ? currentVal : 0L;
                long longestStreak = (longestVal != null) ? longestVal : 0L;
                if (lastCompletedDate == null) lastCompletedDate = "";

                if (today.equals(lastCompletedDate)) {
                    Toast.makeText(getContext(),
                            "Technique already done today!",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                long newCurrent = updateStreakForToday(lastCompletedDate, currentStreak);
                long newLongest = Math.max(newCurrent, longestStreak);

                Map<String, Object> updates = new HashMap<>();
                updates.put("currentStreak", newCurrent);
                updates.put("longestStreak", newLongest);
                updates.put("lastCompletedDate", today);

                ref.updateChildren(updates)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(),
                                    "Technique check-in success!",
                                    Toast.LENGTH_SHORT).show();

                            // 更新 UI（technique 部分）
                            tvTechCurrent.setText(String.valueOf(newCurrent));
                            tvTechLongest.setText(String.valueOf(newLongest));

                            // 如果是高质量练习，你可以在这里更新 highQualityTechCount
                            // 并在成功后调用 updateBusinessBadges() 刷新徽章
                            updateBusinessBadges();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(getContext(),
                                        "Update failed: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }


    private void updateBusinessBadges() {
        DatabaseReference badgeRef = getChildRef().child("badges");

        badgeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean perfectWeek = snapshot.child("perfectControllerWeek").getValue(Boolean.class);
                Boolean tenTech = snapshot.child("tenHighQualitySessions").getValue(Boolean.class);
                Boolean lowRescue = snapshot.child("lowRescueMonth").getValue(Boolean.class);

                // 每个 ImageView 的 parent 是包着图和文字的 LinearLayout
                View perfectContainer = (View) badgePerfectWeek.getParent();
                View tenTechContainer = (View) badgeTenTech.getParent();
                View lowRescueContainer = (View) badgeLowRescue.getParent();

                perfectContainer.setVisibility(Boolean.TRUE.equals(perfectWeek)
                        ? View.VISIBLE : View.GONE);
                tenTechContainer.setVisibility(Boolean.TRUE.equals(tenTech)
                        ? View.VISIBLE : View.GONE);
                lowRescueContainer.setVisibility(Boolean.TRUE.equals(lowRescue)
                        ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void hideAllBadgeContainers() {
        if (badgePerfectWeek != null) {
            View p = (View) badgePerfectWeek.getParent();
            p.setVisibility(View.GONE);
        }
        if (badgeTenTech != null) {
            View p = (View) badgeTenTech.getParent();
            p.setVisibility(View.GONE);
        }
        if (badgeLowRescue != null) {
            View p = (View) badgeLowRescue.getParent();
            p.setVisibility(View.GONE);
        }
    }

    /**
     * format: "yyyy-MM-dd"
     */
    private String getTodayString() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }

    private long updateStreakForToday(String lastDateStr, long currentStreak) {
        String todayStr = getTodayString();

        if (todayStr.equals(lastDateStr)) {
            return currentStreak;
        }

        if (lastDateStr == null || lastDateStr.isEmpty()) {
            return 1;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar lastCal = Calendar.getInstance();
            Calendar todayCal = Calendar.getInstance();

            lastCal.setTime(sdf.parse(lastDateStr));
            todayCal.setTime(sdf.parse(todayStr));

            long diffMillis = todayCal.getTimeInMillis() - lastCal.getTimeInMillis();
            long diffDays = diffMillis / (1000L * 60 * 60 * 24);

            if (diffDays == 1) {
                return currentStreak + 1;
            } else {
                return 1;
            }
        } catch (Exception e) {
            return 1;
        }
    }
}
