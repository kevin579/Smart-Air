package com.example.SmartAirGroup2.ChildDashboard;

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

import com.example.SmartAirGroup2.R;
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

/**
 * A {@link Fragment} that displays the user's check-in streak, longest streak, and earned badges.
 * This fragment provides a gamified experience for the child user to encourage daily engagement.
 * It connects to Firebase to retrieve and update the user's streak information.
 */
public class StreakFragment extends Fragment {

    private TextView tvCurrentStreak;
    private TextView tvLongestStreak;

    private ImageView day5_badge, day10_badge, day15_badge, day20_badge, day25_badge, day30_badge;

    private Button btnCheckIn;
    private Button btnBadges;

    private String childName;

    /**
     * Called when the fragment is first created.
     * This method retrieves the child's username from the arguments passed to the fragment.
     * The username is essential for fetching the correct data from Firebase.
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state, this is the state.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve identifying info passed from previous screen
        if (getArguments() != null) {
            childName = getArguments().getString("childUname");
        }
    }

    /**
     * Called to create the view for this fragment.
     * This method inflates the layout, finds all the UI widgets, and sets up the click listeners
     * for the buttons. It also initiates the process of fetching streak data.
     *
     * @param inflater The LayoutInflater object that can be used to inflate views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
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

        btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        btnCheckIn.setOnClickListener(v -> onCheckInClicked());

        btnBadges.setOnClickListener(v -> requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new BadgeLibrary())
                .addToBackStack(null)
                .commit()
        );

        getStreakData();

        return view;
    }

    /**
     * Fetches the streak data (current and longest) for the specific child from the
     * Firebase Realtime Database. This is an asynchronous operation. Upon successful
     * data retrieval, it updates the UI.
     */
    private void getStreakData() {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("categories/users/children")
                .child(childName)
                .child("streaks");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long currentVal = snapshot.child("currentStreak").getValue(Long.class);
                Long longestVal = snapshot.child("longestStreak").getValue(Long.class);

                long currentStreak = (currentVal != null) ? currentVal : 0L;
                long longestStreak = (longestVal != null) ? longestVal : 0L;

                updateStreakViews(currentStreak, longestStreak);
                updateBadges(longestStreak);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load streak data.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Updates the UI to display the current and longest streak values.
     *
     * @param current The current daily check-in streak.
     * @param longest The longest daily check-in streak ever achieved.
     */
    private void updateStreakViews(long current, long longest) {
        tvCurrentStreak.setText(String.valueOf(current));
        tvLongestStreak.setText(String.valueOf(longest));
    }

    /**
     * Handles the user's click on the "Check-In" button.
     * It checks if the user has already checked in for the day. If not, it updates the
     * streak count and last check-in date in Firebase.
     */
    private void onCheckInClicked() {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("categories/users/children")
                .child(childName)
                .child("streaks");

        String today = getTodayString();

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String lastCompletedDate = snapshot.child("lastCompletedDate").getValue(String.class);

                if (today.equals(lastCompletedDate)) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "You have already checked in today!", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }

                Long currentVal = snapshot.child("currentStreak").getValue(Long.class);
                long currentStreak = (currentVal != null) ? currentVal : 0L;
                currentStreak++;

                Long longestVal = snapshot.child("longestStreak").getValue(Long.class);
                long longestStreak = (longestVal != null) ? longestVal : 0L;
                if (currentStreak > longestStreak) {
                    longestStreak = currentStreak;
                }

                Map<String, Object> updates = new HashMap<>();
                updates.put("currentStreak", currentStreak);
                updates.put("longestStreak", longestStreak);
                updates.put("lastCompletedDate", today);

                final long finalCurrent = currentStreak;
                final long finalLongest = longestStreak;
                ref.updateChildren(updates)
                        .addOnSuccessListener(aVoid -> {
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "Check-in successful!", Toast.LENGTH_SHORT).show();
                            }
                            updateStreakViews(finalCurrent, finalLongest);
                            updateBadges(finalLongest);
                        })
                        .addOnFailureListener(e -> {
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Firebase error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /**
     * Generates a string for the current date in "yyyy-MM-dd" format.
     * This format is used for storing and comparing check-in dates.
     *
     * @return The formatted date string.
     */
    private String getTodayString() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());
    }

    /**
     * Updates the visibility of the streak badges based on the longest streak achieved by the user.
     * Badges are shown for milestones like 5, 10, 15, etc., days.
     *
     * @param longestStreak The user's longest streak.
     */
    private void updateBadges(long longestStreak) {
        day5_badge.setVisibility(longestStreak >= 5 ? View.VISIBLE : View.GONE);
        day10_badge.setVisibility(longestStreak >= 10 ? View.VISIBLE : View.GONE);
        day15_badge.setVisibility(longestStreak >= 15 ? View.VISIBLE : View.GONE);
        day20_badge.setVisibility(longestStreak >= 20 ? View.VISIBLE : View.GONE);
        day25_badge.setVisibility(longestStreak >= 25 ? View.VISIBLE : View.GONE);
        day30_badge.setVisibility(longestStreak >= 30 ? View.VISIBLE : View.GONE);
    }
}
