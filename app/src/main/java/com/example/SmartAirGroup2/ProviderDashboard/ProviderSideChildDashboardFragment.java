package com.example.SmartAirGroup2.ProviderDashboard;

import android.content.Intent;
import android.os.Bundle;
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

import com.example.SmartAirGroup2.Helpers.MenuHelper;
import com.example.SmartAirGroup2.ParentDashboard.InventoryFragment;
import com.example.SmartAirGroup2.ParentDashboard.PEFZone;
import com.example.SmartAirGroup2.ParentDashboard.SymptomHistoryFragment;
import com.example.SmartAirGroup2.R;

import java.util.ArrayList;


/**
 * ProviderSideChildDashboardFragment (Provider-Side View)
 * ------------------------------------------------------
 * This fragment provides a comprehensive, permission-based overview of a single child's health
 * status from the healthcare provider's perspective. It acts as a navigation hub, granting
 * access to detailed child information based on the permissions granted by the parent.
 *
 * Purpose:
 *   Enables healthcare providers to monitor a child's asthma management by providing access to:
 *   • Medication inventory levels (if permitted)
 *   • PEF (Peak Expiratory Flow) zone monitoring (if permitted)
 *   • Symptom and trigger history (if permitted)
 *   • Rescue and controller medication logs (if permitted)
 *   • Medication adherence data (if permitted)
 *   • Triage and summary reports
 *
 * Core Features:
 *   • Displays a dashboard of cards, each corresponding to a specific health data category.
 *   • Dynamically shows or hides cards based on an ArrayList of permissions.
 *   • Provides navigation to more detailed fragments or activities for each category.
 *   • Back navigation to the main provider dashboard.
 *
 * UI Behavior:
 *   - Displays the child's name in the toolbar.
 *   - Presents multiple CardViews for different data categories (Inventory, PEF, Symptoms, Logs, etc.).
 *   - Cards are made visible or hidden (View.GONE) based on the `permissions` list.
 *   - Each visible card navigates to its respective detailed view on click.
 *
 * Firebase Structure (Relevant Paths):
 * └── categories/
 *     └── users/
 *         └── children/{childUname}/
 *             ├── data/ (For logs, etc.)
 *             └── status/ (For real-time status like PEF zone)
 *
 * Navigation Flow:
 *   ProviderDashboardFragment → ProviderSideChildDashboardFragment → [Detailed Fragments/Activities]
 *
 * Fragment Arguments (Required):
 *   • childName (String): Display name of the child.
 *   • childUname (String): Unique username/identifier for the child in Firebase.
 *   • permissions (ArrayList<String>): A list of strings defining which data sections the provider can access.
 *
 * Dependencies:
 *   • Firebase Realtime Database for data retrieval.
 *   • MenuHelper for toolbar menu setup.
 *   • Various destination fragments (e.g., InventoryFragment, PEFZone) and activities.
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
     * Shows the child's name and provides back navigation to the provider dashboard.
     */
    private Toolbar toolbar;

    /**
     * CardViews for accessing different sections of the child's health data.
     * Visibility is controlled by permissions granted by the parent.
     */
    private CardView cardInventory, cardPEF, cardSymptom, cardLog, cardSummary, cardAdherence, cardTriage;


    // ═══════════════════════════════════════════════════════════════════════
    // CHILD IDENTITY & PERMISSIONS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Display name and unique username of the child.
     * Retrieved from fragment arguments, passed from ProviderDashboardFragment.
     * The name is used in the toolbar title to personalize the view.
     */
    private String name, uname;
    /**
     * List of permissions granted by the parent to the provider.
     * Determines which data cards are visible on the dashboard.
     */
    private ArrayList<String> permissions;

    // ═══════════════════════════════════════════════════════════════════════
    // LIFECYCLE METHODS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Called when the fragment is first created.
     * Retrieves child identity and permissions from fragment arguments passed by the parent fragment (ProviderDashboardFragment).
     *
     * Expected Arguments:
     *   - childName: Display name of the child
     *   - childUname: Firebase username/key for the child
     *   - permissions: ArrayList of strings defining access rights
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
     *   - Initializes card references and configures their visibility based on permissions
     *   - Configures click handlers for navigation to detail fragments/activities
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

        // Enable back navigation to provider dashboard
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
                    args.putString("triggers", "no");
                }else{
                    args.putString("triggers", "yes");
                }
                sympFrag.setArguments(args);
                loadFragment(sympFrag);
            });
        }

        // ─────────────────────────────────────────────────────────────────
        // PEF Card Click Handler
        // ─────────────────────────────────────────────────────────────────
        // Navigate to Peak Expiratory Flow measurement view
        if (!permissions.contains("pef")) {
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
        // Adherence Card Click Handler (Provider)
        // ─────────────────────────────────────────────────────────────────

        if (!permissions.contains("controllerAdherence")) {
            cardAdherence.setVisibility(View.GONE);
            cardAdherence.setOnClickListener(null);
        } else {
            cardAdherence.setOnClickListener(v -> {
                if (getActivity() == null) return;

                Intent intent = new Intent(getActivity(), ProviderAdherenceActivity.class);
                intent.putExtra("childUname", uname);
                intent.putExtra("childName", name);
                intent.putExtra("user", "provider");
                startActivity(intent);
            });
        }
        // ─────────────────────────────────────────────────────────────────
        // Logs Card Click Handler
        // ─────────────────────────────────────────────────────────────────
        if (!permissions.contains("rescueLog")) {
            cardLog.setVisibility(View.GONE);
            cardLog.setOnClickListener(null);
        } else {
            cardLog.setOnClickListener(v -> {
                ProviderRescueLogsFragment logsFrag = new ProviderRescueLogsFragment();
                Bundle args = new Bundle();
                args.putString("childUname", uname);
                args.putString("childName", name);
                args.putString("user", "provider");
                logsFrag.setArguments(args);

                loadFragment(logsFrag);
            });
        }

        // ─────────────────────────────────────────────────────────────────
        // Triage Card Click Handler
        // ─────────────────────────────────────────────────────────────────
        // Navigate to Peak Expiratory Flow measurement view
        if (!permissions.contains("triage")) {
            cardTriage.setVisibility(View.GONE);
            cardTriage.setOnClickListener(null);
        } else {
            // PEF Card Click Handler
            cardTriage.setOnClickListener(v -> {
                ProviderTriageLog triageLog = new ProviderTriageLog();
                Bundle args = new Bundle();
                args.putString("childUname", uname);
                args.putString("childName", name);
                args.putString("user", "provider");
                triageLog.setArguments(args);
                loadFragment(triageLog);
            });
        }


        // ─────────────────────────────────────────────────────────────────
        // Rescue History Card Click Handler
        // ─────────────────────────────────────────────────────────────────
        // Navigate to Peak Expiratory Flow measurement view
        if (!permissions.contains("charts")) {
            cardSummary.setVisibility(View.GONE);
            cardSummary.setOnClickListener(null);
        } else {
            // PEF Card Click Handler
            cardSummary.setOnClickListener(v -> {
                ProviderSideChildRescueSummary summaryFrag = new ProviderSideChildRescueSummary();
                Bundle args = new Bundle();
                args.putString("childUname", uname);
                args.putString("childName", name);
                summaryFrag.setArguments(args);
                loadFragment(summaryFrag);
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
        MenuHelper.setupMenuWithoutAlerts(menu, inflater, requireContext());
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return MenuHelper.handleMenuSelection(item, this) || super.onOptionsItemSelected(item);
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