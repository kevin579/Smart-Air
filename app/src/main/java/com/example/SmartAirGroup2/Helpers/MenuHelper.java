package com.example.SmartAirGroup2.Helpers;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.example.SmartAirGroup2.Main.MainActivity;
import com.example.SmartAirGroup2.ParentDashboard.AlertCenterFragment;
import com.example.SmartAirGroup2.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * A utility class for handling common menu setup and actions within fragments.
 * This class provides static methods to inflate menus, handle item selections,
 * show pop-up menus, and manage a notification badge icon.
 */
public class MenuHelper {

    /**
     * Inflates the standard menu and tints the icons for better visibility on a dark toolbar.
     *
     * @param menu The menu to inflate.
     * @param inflater The MenuInflater to use.
     * @param context The context, used for resource access.
     */
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

    /**
     * Inflates a menu variation that excludes the alert/notification icon.
     *
     * @param menu The menu to inflate.
     * @param inflater The MenuInflater to use.
     * @param context The context for resource access.
     */
    public static void setupMenuWithoutAlerts(@NonNull Menu menu, @NonNull MenuInflater inflater, @NonNull Context context) {
        menu.clear();
        inflater.inflate(R.menu.menu_no_alert, menu);

        // Tint icons white for visibility
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.getIcon() != null) {
                item.getIcon().setTint(context.getResources().getColor(android.R.color.white));
            }
        }
    }

    /**
     * Handles the selection of common menu items, like notifications and settings.
     *
     * @param item The selected MenuItem.
     * @param fragment The fragment from which the selection was made.
     * @return true if the event was handled, false otherwise.
     */
    public static boolean handleMenuSelection(@NonNull MenuItem item, @NonNull Fragment fragment) {
        int id = item.getItemId();
        if (id == R.id.action_notifications) {
            Context ctx = fragment.requireContext();
            SharedPreferences prefs = ctx.getSharedPreferences("APP_DATA", Context.MODE_PRIVATE);
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

    /**
     * Finds the View associated with a MenuItem, typically for use as an anchor for a PopupMenu.
     *
     * @param fragment The fragment containing the menu.
     * @param item The MenuItem to find.
     * @return The View for the MenuItem, or the fragment's root view as a fallback.
     */
    private static View findMenuItemView(@NonNull Fragment fragment, @NonNull MenuItem item) {
        Toolbar toolbar = fragment.requireActivity().findViewById(R.id.toolbar);
        if (toolbar != null) {
            // If the item has a direct action view, use it
            if (item.getActionView() != null) {
                return item.getActionView();
            }
            // Otherwise, anchor to the toolbar itself
            return toolbar;
        }
        // Fallback to the fragment's root view if no toolbar is found
        return fragment.getView();
    }

    /**
     * Shows a popup menu anchored to a specific view, used for the settings dropdown.
     *
     * @param fragment The fragment where the menu is shown.
     * @param anchorView The view to anchor the popup menu to.
     */
    private static void showSettingsPopupMenu(@NonNull Fragment fragment, @NonNull View anchorView) {
        PopupMenu popup = new PopupMenu(fragment.requireContext(), anchorView, Gravity.END);
        popup.getMenuInflater().inflate(R.menu.dropdown, popup.getMenu());

        popup.setOnMenuItemClickListener(menuItem -> {
            int itemId = menuItem.getItemId();
            if (itemId == R.id.action_logout) {
                new AlertDialog.Builder(fragment.requireContext())
                        .setTitle("Logout")
                        .setMessage("Are you sure you want to logout?")
                        .setPositiveButton("Yes", (d, w) -> logoutUser(fragment.requireContext()))
                        .setNegativeButton("No", null)
                        .show();
                return true;
            } else if (itemId == R.id.action_set_privacy) {
                // Placeholder for navigating to a PrivacySettingsFragment
                return true;
            } else if (itemId == R.id.action_more) {
                // Placeholder for navigating to a MoreSettingsFragment
                return true;
            }
            return false;
        });

        popup.show();
    }

    /**
     * Logs out the current user by clearing session data and navigating to the main activity.
     *
     * @param context The context used for accessing SharedPreferences and starting the new activity.
     */
    public static void logoutUser(@NonNull Context context) {
        SessionTimeoutManager.getInstance(context).stopTimer();

        // Clear SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences("APP_DATA", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();

        // Navigate to Login Activity and clear the activity stack
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    /**
     * Sets up the notification icon in the menu, including its badge.
     *
     * @param fragment The fragment owning the menu.
     * @param menu The menu containing the notification item.
     * @param inflater The menu inflater.
     */
    public static void setupNotification(@NonNull Fragment fragment, @NonNull Menu menu, @NonNull MenuInflater inflater) {
        setupMenu(menu, inflater, fragment.requireContext());

        MenuItem bellItem = menu.findItem(R.id.action_notifications);
        if (bellItem == null || bellItem.getActionView() == null) {
            return;
        }

        View actionView = bellItem.getActionView();
        View badgeView = actionView.findViewById(R.id.viewBadge);

        actionView.setOnClickListener(v -> fragment.onOptionsItemSelected(bellItem));
        updateNotificationBadge(fragment.requireContext(), badgeView);
    }

    /**
     * Checks for alerts for the parent's children in Firebase and updates the visibility of the notification badge.
     *
     * @param ctx The context.
     * @param badgeView The badge view to show or hide.
     */
    private static void updateNotificationBadge(Context ctx, View badgeView) {
        if (badgeView == null) {
            return;
        }

        badgeView.setVisibility(View.GONE); // Default to hidden

        SharedPreferences prefs = ctx.getSharedPreferences("APP_DATA", Context.MODE_PRIVATE);
        String parentUname = prefs.getString("parentUname", null);

        if (parentUname == null || parentUname.trim().isEmpty()) {
            return;
        }

        FirebaseDatabase db = FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/");
        DatabaseReference childrenRef = db.getReference("categories/users/parents").child(parentUname).child("children");

        childrenRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists() || !snapshot.hasChildren()) {
                    return;
                }

                for (DataSnapshot childSnap : snapshot.getChildren()) {
                    String childUname = childSnap.getValue(String.class);
                    if (childUname != null && !childUname.trim().isEmpty()) {
                        checkChildStatusForAlerts(db, childUname, badgeView);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Failed to read children list, badge remains hidden
            }
        });
    }

    /**
     * Helper method to check a single child's status for alerts.
     *
     * @param db The FirebaseDatabase instance.
     * @param childUname The username of the child to check.
     * @param badgeView The badge view to update.
     */
    private static void checkChildStatusForAlerts(FirebaseDatabase db, String childUname, View badgeView) {
        DatabaseReference statusRef = db.getReference("categories/users/children").child(childUname).child("status");
        statusRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot statusSnap) {
                if (statusHasAlert(statusSnap)) {
                    badgeView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Failed to read status, badge visibility is unchanged for this child
            }
        });
    }

    /**
     * Determines if a child's status snapshot contains any condition that should trigger an alert.
     *
     * @param statusSnap The DataSnapshot of the child's status node.
     * @return true if an alert condition is found, false otherwise.
     */
    private static boolean statusHasAlert(DataSnapshot statusSnap) {
        if (!statusSnap.exists()) {
            return false;
        }

        // Check for PEF red zone
        Integer pefZone = statusSnap.child("pefZone").getValue(Integer.class);
        if (pefZone != null && pefZone == 2) {
            return true;
        }

        // Check for inventory alerts (low or expired medicine)
        DataSnapshot invSnap = statusSnap.child("inventory");
        if (invSnap.exists()) {
            for (DataSnapshot medSnap : invSnap.getChildren()) {
                for (DataSnapshot snapIndex : medSnap.getChildren()) {
                    Integer code = snapIndex.getValue(Integer.class);
                    if (code != null && (code == 1 || code == 2)) {
                        return true; // Found a low or expired medicine
                    }
                }
            }
        }

        return false;
    }
}
