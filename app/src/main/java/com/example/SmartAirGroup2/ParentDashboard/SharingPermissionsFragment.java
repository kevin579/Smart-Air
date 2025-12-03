package com.example.SmartAirGroup2.ParentDashboard; // Adjust package name as needed

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

import com.example.SmartAirGroup2.Helpers.MenuHelper;
import com.example.SmartAirGroup2.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link Fragment} that allows a parent to manage data sharing permissions for a specific child.
 * This screen displays a list of toggles (switches) that correspond to different categories of
 * health data (e.g., rescue logs, symptoms, PEF data). The parent can grant or revoke a healthcare
 * provider's access to this data. The permissions are stored and synchronized with the
 * Firebase Realtime Database under the child's profile.
 */
public class SharingPermissionsFragment extends Fragment {

    /**
     * The username of the child whose permissions are being edited.
     */
    private String childId;
    /**
     * The display name of the child, used for the toolbar title.
     */
    private String childName;
    /**
     * The Firebase Realtime Database instance.
     */
    private FirebaseDatabase db;

    private DatabaseReference permRef;

    private Switch rescueLogSwitch, controllerAdherenceSwitch, shareSymptomsSwitch,
            shareTriggersSwitch, sharePefSwitch, shareTriageSwitch,
            shareChartsSwitch, shareInventorySwitch;

    /**
     * The toolbar for this fragment.
     */
    private Toolbar toolbar;

    /**
     * A map to associate the permission keys (as used in Firebase) with their corresponding Switch views.
     * This simplifies the process of reading from and writing to the database.
     */
    private final Map<String, Switch> permissionSwitches = new HashMap<>();


    /**
     * Called when the fragment is first created.
     * Retrieves the child's username and display name from the fragment's arguments.
     * @param savedInstanceState If the fragment is being re-created from a previous saved state,
     * this is the state.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve identifying info passed from previous screen
        if (getArguments() != null) {
            childId = getArguments().getString("childUname");
            childName = getArguments().getString("childName");
        }
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * Inflates the layout, sets up the toolbar, and finds all the Switch views.
     *
     * @param inflater The LayoutInflater object that can be used to inflate views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
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

    /**
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)} has returned,
     * but before any saved state has been restored in to the view.
     * This is where Firebase is initialized, the switch map is populated, and the initial data load is triggered.
     *
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 2. Initialize Firebase
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

    /**
     * Fetches the current permission settings from Firebase and updates the UI.
     * This method performs a one-time read to get the current state of all permissions.
     * It then iterates through the local switch map, setting each switch to match the state in Firebase.
     * After the UI is synchronized, it calls {@link #attachChangeListeners()} to enable saving on toggle.
     */
    private void loadPermissionsAndSetupListeners() {
        // Initial Read to set Switch states
        permRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DataSnapshot snapshot = task.getResult();

                for (String key : permissionSwitches.keySet()) {
                    boolean value = false;

                    if (snapshot.child(key).exists()) {
                        // Safely read boolean value
                        value = Boolean.TRUE.equals(snapshot.child(key).getValue(Boolean.class));
                    }

                    // Set the initial UI state
                    permissionSwitches.get(key).setChecked(value);
                }

                // Now that the UI is initialized, attach the listeners that will save changes.
                attachChangeListeners();

            } else {
                if(getContext() != null) {
                    Toast.makeText(requireContext(), "Failed to load permissions: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /**
     * Inflates the menu for this fragment.
     * @param menu The menu to inflate.
     * @param inflater The MenuInflater to use.
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        MenuHelper.setupMenu(menu, inflater, requireContext());
        MenuHelper.setupNotification(this,menu,inflater);
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * Handles menu item selections.
     * @param item The selected MenuItem.
     * @return true if the event was handled, false otherwise.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return MenuHelper.handleMenuSelection(item, this) || super.onOptionsItemSelected(item);
    }

    /**
     * Attaches an {@link android.widget.CompoundButton.OnCheckedChangeListener} to each switch.
     * When a switch's state is changed by the user, the listener triggers and writes the new
     * boolean value to the corresponding key in Firebase. This ensures that permissions are
     * saved in real-time. Includes failure handling to notify the user.
     */
    private void attachChangeListeners() {
        for (Map.Entry<String, Switch> entry : permissionSwitches.entrySet()) {
            String key = entry.getKey();
            Switch sw = entry.getValue();

            // When the user changes the switch state, update Firebase
            sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
                permRef.child(key).setValue(isChecked)
                        .addOnFailureListener(e -> {
                            if(getContext() != null) {
                                Toast.makeText(requireContext(), "Failed to save " + key, Toast.LENGTH_SHORT).show();
                                // Optional: revert the switch state if save fails
                                sw.setChecked(!isChecked);
                            }
                        });
            });
        }
    }

}
