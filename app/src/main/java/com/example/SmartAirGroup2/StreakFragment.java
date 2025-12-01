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

    // 配置阈值（默认值），启动时从 settings/streakConfig 覆盖
    private long controllerWeekLength = 7;        // 默认 7 天
    private long highQualityTarget = 10;          // 默认 10 次高质量练习

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

        // badges
        badgePerfectWeek = view.findViewById(R.id.badge_perfect_week);
        badgeTenTech = view.findViewById(R.id.badge_ten_tech);
        badgeLowRescue = view.findViewById(R.id.badge_low_rescue);

        hideAllBadgeContainers();

        btnBack.setOnClickListener(v ->
                requireActivity()
                        .getSupportFragmentManager()
                        .popBackStack()
        );

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

        // 1. 先读配置阈值
        loadStreakConfig();
        // 2. 再读 streak 和 badges 显示
        getStreakData();
        updateBusinessBadges();

        return view;
    }

    // ──────────────────── 公共引用 ────────────────────

    private DatabaseReference getChildRef() {
        return FirebaseDatabase.getInstance()
                .getReference()
                .child("categories")
                .child("users")
                .child("children")
                .child(uname);
    }

    private DatabaseReference getConfigRef() {
        return FirebaseDatabase.getInstance()
                .getReference()
                .child("categories")
                .child("settings")
                .child("streakConfig");
    }

    // ──────────────────── 读取配置阈值 ────────────────────

    private void loadStreakConfig() {
        getConfigRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long weekLenVal = snapshot.child("controllerWeekLength").getValue(Long.class);
                Long targetVal = snapshot.child("highQualityTechniqueTarget").getValue(Long.class);

                if (weekLenVal != null) {
                    controllerWeekLength = weekLenVal;
                }
                if (targetVal != null) {
                    highQualityTarget = targetVal;
                }

                // 调试用：确认路径对不对
                // Toast.makeText(getContext(),
                //         "Config week=" + controllerWeekLength +
                //                 " techTarget=" + highQualityTarget,
                //         Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    // ──────────────────── 读取并显示两个 streak ────────────────────

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

    // ──────────────────── Controller Check in + 按阈值解锁 Perfect Week ────────────────────

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

                            tvCurrentStreak.setText(String.valueOf(newCurrent));
                            tvLongestStreak.setText(String.valueOf(newLongest));

                            // 按阈值解锁
                            checkPerfectControllerWeek(newCurrent);
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

    private void checkPerfectControllerWeek(long newCurrent) {
        if (newCurrent < controllerWeekLength) {
            return;
        }

        DatabaseReference badgeRef = getChildRef()
                .child("badges")
                .child("perfectControllerWeek");

        // 已经解锁过就不用再写
        badgeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean already = snapshot.getValue(Boolean.class);
                if (Boolean.TRUE.equals(already)) return;

                badgeRef.setValue(true)
                        .addOnSuccessListener(v -> {
                            if (getContext() != null) {
                                Toast.makeText(getContext(),
                                        "Perfect controller week badge unlocked!",
                                        Toast.LENGTH_SHORT).show();
                            }
                            updateBusinessBadges();
                        })
                        .addOnFailureListener(e -> {
                            if (getContext() != null) {
                                Toast.makeText(getContext(),
                                        "Failed to write perfectControllerWeek: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    // ──────────────────── Technique Check in + 按阈值解锁 Ten Tech ────────────────────

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

                            tvTechCurrent.setText(String.valueOf(newCurrent));
                            tvTechLongest.setText(String.valueOf(newLongest));

                            if (isHighQuality) {
                                incrementHighQualityTechCountAndCheck();
                            }
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(getContext(),
                                        "Update failed: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void incrementHighQualityTechCountAndCheck() {
        DatabaseReference counterRef = getChildRef()
                .child("counters")
                .child("highQualityTechCount");

        counterRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long val = snapshot.getValue(Long.class);
                long newCount = (val != null ? val : 0L) + 1;

                counterRef.setValue(newCount)
                        .addOnSuccessListener(aVoid -> checkTenHighQualitySessions(newCount));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void checkTenHighQualitySessions(long count) {
        if (count < highQualityTarget) {
            return;
        }

        DatabaseReference badgeRef = getChildRef()
                .child("badges")
                .child("tenHighQualitySessions");

        badgeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean already = snapshot.getValue(Boolean.class);
                if (Boolean.TRUE.equals(already)) return;

                badgeRef.setValue(true)
                        .addOnSuccessListener(v -> {
                            if (getContext() != null) {
                                Toast.makeText(getContext(),
                                        "10 high-quality technique sessions badge unlocked!",
                                        Toast.LENGTH_SHORT).show();
                            }
                            updateBusinessBadges();
                        })
                        .addOnFailureListener(e -> {
                            if (getContext() != null) {
                                Toast.makeText(getContext(),
                                        "Failed to write tenHighQualitySessions: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    // ──────────────────── 徽章显示 ────────────────────

    private void updateBusinessBadges() {
        DatabaseReference badgeRef = getChildRef().child("badges");

        badgeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean perfectWeek = snapshot.child("perfectControllerWeek").getValue(Boolean.class);
                Boolean tenTech = snapshot.child("tenHighQualitySessions").getValue(Boolean.class);
                Boolean lowRescue = snapshot.child("lowRescueMonth").getValue(Boolean.class);

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
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void hideAllBadgeContainers() {
        if (badgePerfectWeek != null) {
            ((View) badgePerfectWeek.getParent()).setVisibility(View.GONE);
        }
        if (badgeTenTech != null) {
            ((View) badgeTenTech.getParent()).setVisibility(View.GONE);
        }
        if (badgeLowRescue != null) {
            ((View) badgeLowRescue.getParent()).setVisibility(View.GONE);
        }
    }

    // ──────────────────── 日期 & streak 计算 ────────────────────

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
