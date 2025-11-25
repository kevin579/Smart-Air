package com.example.SmartAirGroup2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

public class ChildDashboardDemo extends Fragment {
    // 空构造函数（可选，Java会自动生成）
    public ChildDashboardDemo() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.child_dashboard_demo, container, false);
        CardView cardStreak = view.findViewById(R.id.cardStreak);

        if (cardStreak != null) {
            cardStreak.setOnClickListener(v -> {
                requireActivity()
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new StreakFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }

        return view;
    }
}