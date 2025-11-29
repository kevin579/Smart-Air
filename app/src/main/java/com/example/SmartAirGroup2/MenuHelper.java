package com.example.SmartAirGroup2;

import android.content.Context;
import android.content.Intent;
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

import com.example.SmartAirGroup2.auth.login.LoginFragment;

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
}
