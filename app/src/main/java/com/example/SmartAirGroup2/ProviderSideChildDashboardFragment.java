package com.example.SmartAirGroup2;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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

import java.util.ArrayList;


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
public class ProviderSideChildDashboardFragment extends Fragment {

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
    private CardView cardInventory, cardPEF, cardSymptom, cardLog, cardSummary, cardAdherence, cardTriage;


    // ═══════════════════════════════════════════════════════════════════════
    // CHILD IDENTITY
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Display name of the child.
     * Retrieved from fragment arguments, passed from ParentDashboardFragment.
     * Used in toolbar title to personalize the view.
     */
    private String name, uname;
    private ArrayList<String> permissions;

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
            permissions = getArguments().getStringArrayList("permissions");
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
        View view = inflater.inflate(R.layout.fragment_provider_side_child_dashboard, container, false);

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
        cardLog = view.findViewById(R.id.cardLog);
        cardAdherence = view.findViewById(R.id.cardAdherence);
        cardTriage = view.findViewById(R.id.cardTriage);
        cardSummary = view.findViewById(R.id.cardSummary);

        //get permissions
//        for (String permission:permissions){
//            Toast.makeText(getContext(), permission, Toast.LENGTH_SHORT).show();
//        }


        // 1. Check Inventory Card Permission
        if (!permissions.contains("inventory")) {
            cardInventory.setVisibility(View.GONE);
            // Remove the click listener to prevent accidental clicks
            cardInventory.setOnClickListener(null);
        } else {
            // Inventory Card Click Handler (ONLY set if permission exists)
            cardInventory.setOnClickListener(v -> {
                InventoryFragment invFrag = new InventoryFragment();
                Bundle args = new Bundle();
                args.putString("childUname", uname);
                args.putString("childName", name);
                args.putString("user", "provider");
                invFrag.setArguments(args);
                loadFragment(invFrag);
            });
        }

        // ─────────────────────────────────────────────────────────────────
        // PEF Card Click Handler
        // ─────────────────────────────────────────────────────────────────
        // Navigate to Peak Expiratory Flow measurement view
        if (!permissions.contains("pef")) {
            cardPEF.setVisibility(View.GONE);
            cardPEF.setOnClickListener(null);
        } else {
            // PEF Card Click Handler
            cardPEF.setOnClickListener(v -> {
                PEFZone pefFrag = new PEFZone();
                Bundle args = new Bundle();
                args.putString("childUname", uname);
                args.putString("childName", name);
                args.putString("user", "provider");
                pefFrag.setArguments(args);
                loadFragment(pefFrag);
            });
        }

        // ─────────────────────────────────────────────────────────────────
        // Symptom Card Click Handler
        // ─────────────────────────────────────────────────────────────────
        // Navigate to symptom tracking and history view
        if (!permissions.contains("symptoms")) {
            cardSymptom.setVisibility(View.GONE);
            cardSymptom.setOnClickListener(null);
        } else {
            // Symptom Card Click Handler
            cardSymptom.setOnClickListener(v -> {
                SymptomHistoryFragment sympFrag = new SymptomHistoryFragment();
                Bundle args = new Bundle();
                args.putString("childUname", uname);
                args.putString("childName", name);
                args.putString("user", "provider");
                if (!permissions.contains("triggers")) {
                    args.putString("triggers", "yes");
                }else{
                    args.putString("triggers", "no");
                }
                sympFrag.setArguments(args);
                loadFragment(sympFrag);
            });
        }

        // ─────────────────────────────────────────────────────────────────
        // Logs Card Click Handler
        // ─────────────────────────────────────────────────────────────────
        // Navigate to Peak Expiratory Flow measurement view
        if (!permissions.contains("rescueLog")) {
            cardLog.setVisibility(View.GONE);
            cardLog.setOnClickListener(null);
        } else {
            // PEF Card Click Handler
            cardLog.setOnClickListener(v -> {
                PEFZone pefFrag = new PEFZone();
                Bundle args = new Bundle();
                args.putString("childUname", uname);
                args.putString("childName", name);
                args.putString("user", "provider");
                pefFrag.setArguments(args);
                loadFragment(pefFrag);
            });
        }

        // ─────────────────────────────────────────────────────────────────
        // Logs Card Click Handler
        // ─────────────────────────────────────────────────────────────────
        // Navigate to Peak Expiratory Flow measurement view
        if (!permissions.contains("controllerAdherence")) {
            cardAdherence.setVisibility(View.GONE);
            cardAdherence.setOnClickListener(null);
        } else {
            // PEF Card Click Handler
            cardAdherence.setOnClickListener(v -> {
                PEFZone pefFrag = new PEFZone();
                Bundle args = new Bundle();
                args.putString("childUname", uname);
                args.putString("childName", name);
                args.putString("user", "provider");
                pefFrag.setArguments(args);
                loadFragment(pefFrag);
            });
        }

        // ─────────────────────────────────────────────────────────────────
        // Logs Card Click Handler
        // ─────────────────────────────────────────────────────────────────
        // Navigate to Peak Expiratory Flow measurement view
        if (!permissions.contains("triage")) {
            cardTriage.setVisibility(View.GONE);
            cardTriage.setOnClickListener(null);
        } else {
            // PEF Card Click Handler
            cardTriage.setOnClickListener(v -> {
                PEFZone pefFrag = new PEFZone();
                Bundle args = new Bundle();
                args.putString("childUname", uname);
                args.putString("childName", name);
                args.putString("user", "provider");
                pefFrag.setArguments(args);
                loadFragment(pefFrag);
            });
        }


        // ─────────────────────────────────────────────────────────────────
        // Logs Card Click Handler
        // ─────────────────────────────────────────────────────────────────
        // Navigate to Peak Expiratory Flow measurement view
        if (!permissions.contains("charts")) {
            cardSummary.setVisibility(View.GONE);
            cardSummary.setOnClickListener(null);
        } else {
            // PEF Card Click Handler
            cardSummary.setOnClickListener(v -> {
                PEFZone pefFrag = new PEFZone();
                Bundle args = new Bundle();
                args.putString("childUname", uname);
                args.putString("childName", name);
                args.putString("user", "provider");
                pefFrag.setArguments(args);
                loadFragment(pefFrag);
            });
        }


        return view;
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