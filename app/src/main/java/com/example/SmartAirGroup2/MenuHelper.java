package com.example.SmartAirGroup2;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.SmartAirGroup2.AlertCenterFragment;

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
            // TODO: Implement settings later
            return true;
        }
        return false;
    }
}
