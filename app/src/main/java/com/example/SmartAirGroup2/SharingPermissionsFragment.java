package com.example.SmartAirGroup2; // Adjust package name as needed

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.SmartAirGroup2.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

// Inherit from Fragment
public class SharingPermissionsFragment extends Fragment {

    // Static final key for passing arguments
//    public static final String ARG_CHILD_ID = "childId";

    private String childId,childName;
    private FirebaseDatabase db;
    private DatabaseReference permRef;
    private Switch rescueLogSwitch, controllerAdherenceSwitch, shareSymptomsSwitch,
            shareTriggersSwitch, sharePefSwitch, shareTriageSwitch,
            shareChartsSwitch, shareInventorySwitch;

    private Toolbar toolbar;

    private final Map<String, Switch> permissionSwitches = new HashMap<>();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve identifying info passed from previous screen
        if (getArguments() != null) {
            childId = getArguments().getString("childUname");
            childName = getArguments().getString("childName");
        }
    }
    // The layout inflation logic goes here
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Use the Activity's layout resource
        View view = inflater.inflate(R.layout.fragment_sharing_permissions, container, false);
        toolbar = view.findViewById(R.id.toolbar);

        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        setHasOptionsMenu(true);

        toolbar.setTitle(childName + "'s Symptoms");

        // Enable back navigation
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        // Retrieve views using the inflated root view
        rescueLogSwitch = view.findViewById(R.id.rescuelogswitch);
        controllerAdherenceSwitch = view.findViewById(R.id.controlleradherenceswitch);
        shareSymptomsSwitch = view.findViewById(R.id.sharesymptomsswitch);
        shareTriggersSwitch = view.findViewById(R.id.sharetriggersswitch);
        sharePefSwitch = view.findViewById(R.id.sharepefswitch);
        shareTriageSwitch = view.findViewById(R.id.sharetriageswitch);
        shareChartsSwitch = view.findViewById(R.id.sharechartsswitch);
        shareInventorySwitch = view.findViewById(R.id.shareinventoryswitch);

        return view;
    }

    // Logic requiring the View and Context goes here, after the view hierarchy is stable
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);




        // 2. Initialize Firebase (using requireContext() if necessary, or just the URL)
        db = FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/");

        // 3. Populate Switch Map
        permissionSwitches.put("rescueLog", rescueLogSwitch);
        permissionSwitches.put("controllerAdherence", controllerAdherenceSwitch);
        permissionSwitches.put("symptoms", shareSymptomsSwitch);
        permissionSwitches.put("triggers", shareTriggersSwitch);
        permissionSwitches.put("pef", sharePefSwitch);
        permissionSwitches.put("triage", shareTriageSwitch);
        permissionSwitches.put("charts", shareChartsSwitch);
        permissionSwitches.put("inventory", shareInventorySwitch);

        // 4. Set up Database Reference
        permRef = db.getReference("categories/users/children")
                .child(childId)
                .child("shareToProviderPermissions");

        // 5. Initial Data Load and Setup Listeners
        loadPermissionsAndSetupListeners();
    }

    // Extracted method to keep onViewCreated cleaner
    private void loadPermissionsAndSetupListeners() {
        // Initial Read to set Switch states
        permRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DataSnapshot snapshot = task.getResult();

                // Suppress listener during load to prevent writing initial state back to DB
                // This assumes you defined 'suppressToggleListener' as a class field (I removed it here
                // to simplify, but it's good practice if you had complex listeners.)

                for (String key : permissionSwitches.keySet()) {
                    boolean value = false;

                    if (snapshot.child(key).exists()) {
                        // Safely read boolean value
                        value = Boolean.TRUE.equals(snapshot.child(key).getValue(Boolean.class));
                    }

                    // Set the initial UI state
                    permissionSwitches.get(key).setChecked(value);
                }

                // Now that the UI is initialized, attach the listeners
                attachChangeListeners();

            } else {
                Toast.makeText(requireContext(), "Failed to load permissions: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        MenuHelper.setupMenu(menu, inflater, requireContext());
        MenuHelper.setupNotification(this,menu,inflater);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return MenuHelper.handleMenuSelection(item, this) || super.onOptionsItemSelected(item);
    }

    // Extracted method to set up persistence listeners
    private void attachChangeListeners() {
        for (Map.Entry<String, Switch> entry : permissionSwitches.entrySet()) {
            String key = entry.getKey();
            Switch sw = entry.getValue();

            // When the user changes the switch state, update Firebase
            sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
                permRef.child(key).setValue(isChecked)
                        .addOnFailureListener(e -> {
                            Toast.makeText(requireContext(), "Failed to save " + key, Toast.LENGTH_SHORT).show();
                            // Optional: revert the switch state if save fails
                            sw.setChecked(!isChecked);
                        });
            });
        }
    }

}