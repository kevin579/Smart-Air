package com.example.SmartAirGroup2;

import static androidx.core.content.ContentProviderCompat.requireContext;

import static java.security.AccessController.getContext;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.Gravity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import com.example.SmartAirGroup2.AlertCenterFragment;

import com.example.SmartAirGroup2.auth.data.repo.FirebaseRtdbAuthRepository;
import com.example.SmartAirGroup2.auth.login.LoginFragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MenuHelper {
    public static void setupMenu(@NonNull Menu menu, @NonNull MenuInflater inflater, @NonNull Context context) {
        menu.clear();
        inflater.inflate(R.menu.menu, menu);

        // Tint icons white for visibility
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.getIcon() != null) {
                item.getIcon().setTint(context.getResources().getColor(android.R.color.white));
            }
        }
    }

    // Handle menu item clicks
    public static boolean handleMenuSelection(@NonNull MenuItem item, @NonNull Fragment fragment) {

        int id = item.getItemId();
        if (id == R.id.action_notifications) {
            Context ctx = fragment.requireContext();
            SharedPreferences prefs =
                    ctx.getSharedPreferences("APP_DATA", Context.MODE_PRIVATE);
            String parentUname = prefs.getString("parentUname", null);

            AlertCenterFragment alertFrag = new AlertCenterFragment();
            if (parentUname != null && !parentUname.trim().isEmpty()) {
                Bundle args = new Bundle();
                args.putString("parentUname", parentUname);
                alertFrag.setArguments(args);
            }

            fragment.requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, alertFrag)
                    .addToBackStack(null)
                    .commit();
            return true;
        } else if (id == R.id.action_settings) {
            View settingsIconView = findMenuItemView(fragment, item);
            showSettingsPopupMenu(fragment, settingsIconView);
            return true;
        }
        return false;
    }

    private static View findMenuItemView(@NonNull Fragment fragment, @NonNull MenuItem item) {

        Toolbar toolbar = fragment.requireActivity().findViewById(R.id.toolbar);

        if (toolbar != null) {
            if (item.getActionView() != null) {
                return item.getActionView();
            }

            return toolbar;
        }

        // If we can't find the Toolbar, return the Fragment's root view as a fallback anchor
        return fragment.getView();
    }

    private static void showSettingsPopupMenu(@NonNull Fragment fragment, @NonNull View anchorView) {
        // 1. Create the PopupMenu, anchored to the settings icon View
        PopupMenu popup = new PopupMenu(fragment.requireContext(), anchorView, Gravity.END);

        // 2. Inflate the menu XML we created
        popup.getMenuInflater().inflate(R.menu.dropdown, popup.getMenu());

        // 3. Set the listener for when an item in the popup is clicked
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.action_logout) {
                    new AlertDialog.Builder(fragment.requireContext())
                            .setTitle("Logout")
                            .setMessage("Are you sure you want to logout?")
                            .setPositiveButton("Yes", (d, w) -> {
                                logoutUser(fragment.requireContext());
                            })
                            .setNegativeButton("No", null)
                            .show();

                    return true;
                } else if (itemId == R.id.action_set_privacy) {
                    // Navigate to a PrivacySettingsFragment
                    return true;
                } else if (itemId == R.id.action_more) {
                    // Navigate to a MoreSettingsFragment
                    return true;
                }
                return false;
            }
        });

        // 4. Show the menu
        popup.show();
    }

    public static void logoutUser(@NonNull Context context) {

        SessionTimeoutManager.getInstance(context).stopTimer();

        // 1. Clear SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences("APP_DATA", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Clear everything
        editor.clear();
        editor.apply();

        // 3. Navigate to Login Activity
        Intent intent = new Intent(context, MainActivity.class);

        // Set the flags to clear the activity stack!
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // Start the Login Activity
        context.startActivity(intent);
    }

    public static void setupNotification(@NonNull Fragment fragment, @NonNull Menu menu, @NonNull MenuInflater inflater){
        setupMenu(menu,inflater,fragment.requireContext());

        MenuItem bellItem = menu.findItem(R.id.action_notifications);
        if(bellItem == null){
            return;
        }
        View actionView = bellItem.getActionView();
        if (actionView == null){
            return;
        }

        View badgeView = actionView.findViewById(R.id.viewBadge);

        actionView.setOnClickListener(v -> fragment.onOptionsItemSelected(bellItem));
        updateNotificationBadge(fragment.requireContext(), badgeView);
    }

    private static void updateNotificationBadge(Context ctx, View badgeView){
        if(badgeView == null){
            return;
        }

        badgeView.setVisibility(View.GONE);

        SharedPreferences prefs = ctx.getSharedPreferences("APP_DATA", Context.MODE_PRIVATE);
        String parentUname = prefs.getString("parentUname", null);

        if(parentUname == null|| parentUname.trim().isEmpty()){
            return;
        }

        FirebaseDatabase db = FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/");

        DatabaseReference childRef = db.getReference("categories/users/parents")
                .child(parentUname)
                .child("children");

        childRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(!snapshot.exists() || snapshot.getChildrenCount()==0){
                    return;
                }
                int childrenCount = (int)snapshot.getChildrenCount();
                final int[] finished = {0};
                final boolean[] hasAlerts = {false};
                for (DataSnapshot childSnap : snapshot.getChildren()) {
                    String childUname = childSnap.getValue(String.class);

                    if (childUname == null || childUname.trim().isEmpty()) {
                        if (++finished[0] == childrenCount && !hasAlerts[0]) {
                            badgeView.setVisibility(View.GONE);
                        }
                        continue;
                    }

                    DatabaseReference statusRef = db.getReference("categories/users/children")
                            .child(childUname)
                            .child("status");

                    statusRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot statusSnap) {
                            if (!hasAlerts[0] && statusHasAlert(statusSnap)) {
                                hasAlerts[0] = true;
                                badgeView.setVisibility(View.VISIBLE);   // find alert: show red badge
                            }

                            if (++finished[0] == childrenCount && !hasAlerts[0]) {
                                badgeView.setVisibility(View.GONE);      // no alert: do not show red badge
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            if (++finished[0] == childrenCount && !hasAlerts[0]) {
                                badgeView.setVisibility(View.GONE);
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                badgeView.setVisibility(View.GONE);
            }
        });
    }

    private static boolean statusHasAlert(DataSnapshot statusSnap) {
        if (statusSnap == null || !statusSnap.exists()) {
            return false;
        }

        // 1. pefZone == 2 : red zone
        Integer pefZone = statusSnap.child("pefZone").getValue(Integer.class);
        if (pefZone != null && pefZone == 2) {
            return true;
        }

        DataSnapshot invSnap = statusSnap.child("inventory");
        if (invSnap != null && invSnap.exists()) {
            for (DataSnapshot medSnap : invSnap.getChildren()) {
                for (DataSnapshot snapIndex : medSnap.getChildren()) {
                    Integer code = snapIndex.getValue(Integer.class);
                    if (code != null && (code == 1 || code == 2)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }




}
