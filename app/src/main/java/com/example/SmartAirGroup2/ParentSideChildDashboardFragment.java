package com.example.SmartAirGroup2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


/**
 * ChildDashboardFragment (Parent-Side View)
 * ------------------------------------------
 * This fragment provides a comprehensive overview of a single child's health status from the
 * parent's perspective. It serves as a navigation hub for accessing detailed child information
 * across three main categories: Inventory, PEF (Peak Expiratory Flow), and Symptoms.
 *
 * Purpose:
 *   Enables parents to monitor their child's asthma management by providing:
 *   • Visual status indicators for medication inventory levels
 *   • PEF zone monitoring (Green/Yellow/Red zones)
 *   • Symptom tracking overview
 *   • Quick navigation to detailed views for each category
 *
 * Core Features:
 *   • Color-coded status cards that reflect real-time child health metrics
 *   • Three primary navigation cards:
 *       → Inventory Card: Medicine stock levels and usage
 *       → PEF Card: Breathing function measurements and zones
 *       → Symptom Card: Logged symptoms and patterns
 *   • Dynamic status updates from Firebase
 *   • Back navigation to parent dashboard
 *
 * UI Behavior:
 *   - Displays child's name in the toolbar title
 *   - Three large CardViews with color-coded backgrounds:
 *       • Inventory: Red (alert) if any medicine is low/critical, Green (good) otherwise
 *       • PEF: Red (zone 2), Yellow (zone 1), or Green (zone 0) based on breathing status
 *       • Symptom: Currently static, may be extended for symptom status
 *   - Each card navigates to its respective detailed fragment on click
 *
 * Firebase Structure (Relevant Paths):
 * └── categories/
 *     └── users/
 *         └── children/{childUname}/
 *             └── status/
 *                 ├── pefZone: Integer (0=green, 1=yellow, 2=red)
 *                 └── inventory/
 *                     └── {medicineName}/
 *                         └── {timestamp: Integer (0=good, 1=warning, 2=alert)}
 *
 * Status Logic:
 *   - Inventory Status:
 *       • 2 (Alert/Red): Any medicine has critical low stock - HIGHEST PRIORITY
 *       • 1 (Warning/Yellow): Any medicine approaching low stock
 *       • 0 (Good/Green): All medicines adequately stocked
 *   - PEF Status:
 *       • 2 (Red Zone): Severe breathing difficulty, immediate action needed
 *       • 1 (Yellow Zone): Caution, medication may be needed
 *       • 0 (Green Zone): Breathing is normal
 *
 * Navigation Flow:
 *   ParentDashboardFragment → ChildDashboardFragment → [Inventory/PEF/Symptom]Fragment
 *
 * Fragment Arguments (Required):
 *   • childName (String): Display name of the child
 *   • childUname (String): Unique username/identifier for the child in Firebase
 *
 * Dependencies:
 *   • Firebase Realtime Database for status data
 *   • MenuHelper for toolbar menu operations
 *   • Three destination fragments: InventoryFragment, ParentPEF, SymptomDashboardFragment
 *
 * Color Resources Used:
 *   • R.color.alert (Red): Critical status requiring immediate attention
 *   • R.color.warning (Yellow): Caution status requiring monitoring
 *   • R.color.good (Green): Normal/healthy status
 *
 * Author: Kevin Li
 * Last Updated: November 2025
 */
public class ParentSideChildDashboardFragment extends Fragment {

    // ═══════════════════════════════════════════════════════════════════════
    // UI COMPONENTS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Toolbar component displayed at the top of the fragment.
     * Shows the child's name and provides back navigation to parent dashboard.
     */
    private Toolbar toolbar;

    /**
     * CardView for accessing the child's status .
     * Color-coded based on medicine stock levels:
     *   - Red (alert)
     *   - Green (good)
     */
    private CardView cardInventory, cardPEF, cardSymptom, cardPrivacy,cardAdherence ;


    // ═══════════════════════════════════════════════════════════════════════
    // CHILD IDENTITY
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Display name of the child.
     * Retrieved from fragment arguments, passed from ParentDashboardFragment.
     * Used in toolbar title to personalize the view.
     */
    private String name;

    /**
     * Unique username/identifier of the child in Firebase.
     * Retrieved from fragment arguments, used to query child-specific data.
     */
    private String uname;

    // ═══════════════════════════════════════════════════════════════════════
    // LIFECYCLE METHODS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Called when the fragment is first created.
     * Retrieves child identity from fragment arguments passed by parent fragment.
     *
     * Expected Arguments:
     *   - childName: Display name of the child
     *   - childUname: Firebase username/key for the child
     *
     * @param savedInstanceState Previously saved state, if any
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve child identity passed as arguments
        if (getArguments() != null) {
            name = getArguments().getString("childName");
            uname = getArguments().getString("childUname");
        }
    }

    /**
     * Creates and initializes the view hierarchy for this fragment.
     *
     * Responsibilities:
     *   - Inflates the child dashboard layout
     *   - Sets up toolbar with child's name and back navigation
     *   - Initializes status card references
     *   - Loads current status from Firebase to color-code cards
     *   - Configures click handlers for navigation to detail fragments
     *
     * @param inflater           LayoutInflater to inflate the view
     * @param container          Parent view that this fragment's UI will be attached to
     * @param savedInstanceState Previously saved state, if any
     * @return                   The root view for this fragment
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_parent_side_child_dashboard, container, false);

        // ─────────────────────────────────────────────────────────────────
        // Toolbar Configuration
        // ─────────────────────────────────────────────────────────────────
        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        setHasOptionsMenu(true);

        // Set personalized title with child's name
        toolbar.setTitle(name + "'s Dashboard");

        // Enable back navigation to parent dashboard
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        // ─────────────────────────────────────────────────────────────────
        // UI Component Initialization
        // ─────────────────────────────────────────────────────────────────
        cardInventory = view.findViewById(R.id.cardInventory);
        cardPEF = view.findViewById(R.id.cardPEF);
        cardSymptom = view.findViewById(R.id.cardSymptom);
        cardPrivacy = view.findViewById(R.id.cardPrivacy);
        cardAdherence = view.findViewById(R.id.card_controller_adherence);


        // ─────────────────────────────────────────────────────────────────
        // Load and Apply Status Colors
        // ─────────────────────────────────────────────────────────────────
        loadChildStatus();

        // ─────────────────────────────────────────────────────────────────
        // Inventory Card Click Handler
        // ─────────────────────────────────────────────────────────────────
        // Navigate to detailed medicine inventory view
        cardInventory.setOnClickListener(v -> {
            InventoryFragment invFrag = new InventoryFragment();
            Bundle args = new Bundle();
            args.putString("childUname", uname);
            args.putString("childName", name);
            args.putString("user", "parent");
            invFrag.setArguments(args);
            loadFragment(invFrag);
        });

        // ─────────────────────────────────────────────────────────────────
        // PEF Card Click Handler
        // ─────────────────────────────────────────────────────────────────
        // Navigate to Peak Expiratory Flow measurement view
        cardPEF.setOnClickListener(v -> {
            PEFZone pefFrag = new PEFZone();
            Bundle args = new Bundle();
            args.putString("childUname", uname);
            args.putString("childName", name);
            args.putString("user", "parent");
            pefFrag.setArguments(args);
            loadFragment(pefFrag);
        });

        // ─────────────────────────────────────────────────────────────────
        // Symptom Card Click Handler
        // ─────────────────────────────────────────────────────────────────
        // Navigate to symptom tracking and history view
        cardSymptom.setOnClickListener(v -> {
            SymptomDashboardFragment sympFrag = new SymptomDashboardFragment();
            Bundle args = new Bundle();
            args.putString("childUname", uname);
            args.putString("childName", name);
            args.putString("user", "parent");
            sympFrag.setArguments(args);
            loadFragment(sympFrag);
        });

        cardPrivacy.setOnClickListener(v -> {
            SharingPermissionsFragment privacyFrag = new SharingPermissionsFragment();
            Bundle args = new Bundle();
            args.putString("childName", name);
            args.putString("childUname", uname);
            privacyFrag.setArguments(args);
            loadFragment(privacyFrag);
        });

        cardAdherence.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(getActivity(), ParentAdherenceActivity.class);
                intent.putExtra("childUsername", uname);
                intent.putExtra("childName", name);
                startActivity(intent);
            }
        });


        return view;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FIREBASE STATUS LOADING
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Loads the child's current health status from Firebase and applies color-coding to cards.
     *
     * Status Categories:
     *   1. Inventory Status (Medicine Stock Levels):
     *      - Scans all medicines under status/inventory
     *      - Priority system: Alert (2) > Warning (1) > Good (0)
     *      - If ANY medicine shows alert, entire card becomes red
     *      - If ANY medicine shows warning (and no alerts), card becomes yellow
     *      - Otherwise, card remains green
     *
     *   2. PEF Status (Breathing Zones):
     *      - Reads pefZone value directly from status node
     *      - Zone 2 (Red): Severe breathing difficulty
     *      - Zone 1 (Yellow): Caution zone
     *      - Zone 0 (Green): Normal breathing
     *
     *   3. Symptom Status:
     *      - Currently not implemented (future enhancement)
     *      - Card remains at default color
     *
     * Firebase Query Path:
     *   categories/users/children/{childUname}/status
     *
     * Threading:
     *   - Uses Firebase's asynchronous listener
     *   - Updates UI on callback thread (safe as Firebase handles this)
     *
     * Error Handling:
     *   - Logs errors to console with "STATUS" tag
     *   - Cards remain at default colors if data fetch fails
     */
    private void loadChildStatus() {
        DatabaseReference statusRef = FirebaseDatabase.getInstance()
                .getReference("categories/users/children")
                .child(uname)
                .child("status");

        statusRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                // ─────────────────────────────────────────────────────────
                // Read PEF Zone Status
                // ─────────────────────────────────────────────────────────
                Long pefZoneVal = snapshot.child("pefZone").getValue(Long.class);
                int pefZone = pefZoneVal != null ? pefZoneVal.intValue() : 0;

                // ─────────────────────────────────────────────────────────
                // Read Inventory Status Array
                // ─────────────────────────────────────────────────────────
                // Default to good status
                int inventoryStatus = 0;

                if (snapshot.child("inventory").exists()) {
                    // Iterate through each medicine
                    for (DataSnapshot medSnapshot : snapshot.child("inventory").getChildren()) {
                        // Each medicine has timestamped status entries
                        for (DataSnapshot statusNode : medSnapshot.getChildren()) {
                            Integer val = statusNode.getValue(Integer.class);

                            if (val != null) {
                                if (val == 2) {
                                    // ALERT overrides all other statuses
                                    inventoryStatus = 2;
                                    break;
                                } else if (val == 1 && inventoryStatus != 2) {
                                    // WARNING only if no alert found yet
                                    inventoryStatus = 1;
                                }
                            }
                        }

                        // Stop scanning if alert already found
                        if (inventoryStatus == 2) break;
                    }
                }

                // ─────────────────────────────────────────────────────────
                // Apply Colors to Cards
                // ─────────────────────────────────────────────────────────

                // Inventory Card Color
                if (inventoryStatus > 0) {
                    // Any warning or alert -> Red
                    cardInventory.setCardBackgroundColor(getResources().getColor(R.color.alert));
                } else {
                    // All good -> Green
                    cardInventory.setCardBackgroundColor(getResources().getColor(R.color.good));
                }

                // PEF Card Color
                if (pefZone == 2) {
                    // Red Zone: Severe
                    cardPEF.setCardBackgroundColor(getResources().getColor(R.color.alert));
                } else if (pefZone == 1) {
                    // Yellow Zone: Caution
                    cardPEF.setCardBackgroundColor(getResources().getColor(R.color.warning));
                } else {
                    // Green Zone: Normal
                    cardPEF.setCardBackgroundColor(getResources().getColor(R.color.good));
                }

                // Symptom Card Color
                // Currently uses default color (future enhancement)
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("STATUS", "Failed to read status: " + error.getMessage());
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MENU HANDLING
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Inflates the toolbar menu using MenuHelper.
     * Called by the Android framework when the menu is being created.
     *
     * @param menu     Menu object to be populated
     * @param inflater MenuInflater to use for inflating menu resources
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        MenuHelper.setupMenu(menu, inflater, requireContext());
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * Handles menu item selection events.
     * Delegates to MenuHelper for consistent menu behavior across the app.
     *
     * @param item The menu item that was selected
     * @return     true if the event was handled, false otherwise
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (MenuHelper.handleMenuSelection(item, this)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FRAGMENT NAVIGATION
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Navigates to another fragment within the same activity.
     * Adds the transaction to the back stack for back button support.
     *
     * @param fragment The fragment to navigate to
     */
    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}