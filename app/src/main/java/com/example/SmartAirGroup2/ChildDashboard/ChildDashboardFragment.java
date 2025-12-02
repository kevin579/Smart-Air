package com.example.SmartAirGroup2.ChildDashboard;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.widget.Toolbar;

import com.example.SmartAirGroup2.Main.CurrentUser;
import com.example.SmartAirGroup2.Helpers.MenuHelper;
import com.example.SmartAirGroup2.Main.OnboardingActivity;
import com.example.SmartAirGroup2.ParentDashboard.SymptomDashboardFragment;
import com.example.SmartAirGroup2.R;
import com.example.SmartAirGroup2.Main.TermsDialogFragment;


/**
 * ChildDashboard (Child-side view)
 * ------------------------------------------
 * Simple activity presenting a child's dashboard with quick navigation
 * to child-specific features such as logs and symptoms.
 *
 * Purpose:
 *   - Provide a lightweight, child-focused entry screen that greets the child and offers
 *     buttons to view logs and navigate to symptom tracking.
 *
 * Core Features:
 *   - Displays a personalized welcome message using the provided childId
 *   - Button to open child logs
 *   - Button to open one tap triage
 *   - Button to open technique helper
 *   - Button to open streaks and badges
 *   - Button to open symptoms page
 *
 * UI Behavior:
 *   - 4 buttons to redirect to other pages
 *   - Adiitional flair for streaks and badges page for engagement
 *   - Welcome message shows the current child id (blank if none provided)
 *
 * Navigation Flow:
 *   Parent/Child list -> ChildDashboard (this)
 *       -> ChildLogsActivity
 *       -> SymptomDashboardFragment
 *
 * Activity Arguments (Intent extras):
 *   • "childId" : Firebase key / identifier for the child. If omitted,
 *     currentChildId remains null and UI will show "Welcome, null!" (caller should set it).
 *
 * Dependencies:
 *   • BaseActivity
 *   • SymptomDashboardFragment — fragment to show symptom dashboard
 *   • ChildLogsActivity — activity that shows child's logs
 *
 * Author: Andy Liu
 * Last Updated: December 2025
 */

public class ChildDashboardFragment extends Fragment {

    // ─────────────────────────────────────────────────────────────────
    // FIELDS
    // ─────────────────────────────────────────────────────────────────

    /**
     * The child identifier passed in via arguments bundle.
     * Used to personalize UI and to pass along to child-specific screens.
     */
    private String currentChildId;

    private Toolbar toolbar;
    // ─────────────────────────────────────────────────────────────────
    // FACTORY METHOD (Replaces Intent usage for Fragments)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Factory method to create a new instance of this fragment and pass arguments.
     *
     * @param childId The unique ID/username of the child whose dashboard is being viewed.
     * @return A new instance of ChildDashboardFragment.
     */
    public static ChildDashboardFragment newInstance(String childId) {
        ChildDashboardFragment fragment = new ChildDashboardFragment();
        Bundle args = new Bundle();
        args.putString("username", childId);
        fragment.setArguments(args);
        return fragment;
    }


    // ─────────────────────────────────────────────────────────────────
    // LIFECYCLE
    // ─────────────────────────────────────────────────────────────────

    /**
     * onCreate - called before onCreateView.
     * Retrieves the child ID from the arguments bundle (which replaced the Activity Intent).
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve the child ID passed via arguments bundle
        if (getArguments() != null) {
            currentChildId = getArguments().getString("username");
        }

        // Safety check: ensure currentChildId is not null, though it should be set by the host Activity/Fragment
        if (currentChildId == null || currentChildId.isEmpty()) {
            currentChildId = "User"; // Fallback value
        }
    }


    /**
     * onCreateView - inflates the layout and initializes UI, listeners, and performs checks.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_child_dashboard, container, false); // Assuming the layout is appropriate for a Fragment

        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        setHasOptionsMenu(true);

        toolbar.setTitle("Welcome, " + currentChildId + "!");


        // -----------------------------------------------------------------
        // START: Terms Acceptance Check
        // -----------------------------------------------------------------
        SharedPreferences prefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);

        String userId = CurrentUser.get().getUname();
        String userEmail = CurrentUser.get().getEmail();
        String userType = CurrentUser.get().getType();

        // Check if this user has accepted terms
        boolean hasAcceptedTerms = prefs.getBoolean("accepted_terms_" + userType + userId + userEmail, false);

        if (!hasAcceptedTerms) {
            // Show the TermsDialogFragment using the Fragment's Manager
            TermsDialogFragment dialog = new TermsDialogFragment();
            // Use getParentFragmentManager() to show dialog over the hosting activity/fragment
            dialog.show(getParentFragmentManager(), "terms_dialog");
        }
        // -----------------------------------------------------------------
        // END: Terms Acceptance Check
        // -----------------------------------------------------------------

        // 3. View Logs button -> starts ChildLogsActivity and forwards childId
        Button btnViewLogs = view.findViewById(R.id.btnViewLogs);
        btnViewLogs.setOnClickListener(v -> {
            // NOTE: Removed setContentView(R.layout.activity_base) as it belongs to the hosting Activity, not the Fragment
            ChildLogsFragment logFrag = new ChildLogsFragment();
            Bundle args = new Bundle();
            args.putString("childUname", currentChildId);
            args.putString("childName", currentChildId);
            args.putString("user", "parent");
            logFrag.setArguments(args);
            loadFragment(logFrag);
        });

        Button btnHelper = view.findViewById(R.id.btnTechnique);
        btnHelper.setOnClickListener(v -> {
            launchTechniqueOnboarding();
        });

        Button btnTriage = view.findViewById(R.id.btnTriage);
        btnTriage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchTriageOnboarding();
            }
        });



        Button btnStreaks = view.findViewById(R.id.btnBadges);
        btnStreaks.setOnClickListener(v -> {
            // NOTE: Removed setContentView(R.layout.activity_base) as it belongs to the hosting Activity, not the Fragment
            StreakFragment streakFrag = new StreakFragment();
            Bundle args = new Bundle();
            args.putString("childUname", currentChildId);
            args.putString("childName", currentChildId);
            args.putString("user", "parent");
            streakFrag.setArguments(args);
            loadFragment(streakFrag);
        });



        // 4. Symptoms button -> loads SymptomDashboardFragment into fragment container
        Button btnSymptoms = view.findViewById(R.id.btnSymptoms);
        btnSymptoms.setOnClickListener(v -> {
            // NOTE: Removed setContentView(R.layout.activity_base) as it belongs to the hosting Activity, not the Fragment
            SymptomDashboardFragment sympFrag = new SymptomDashboardFragment();
            Bundle args = new Bundle();
            args.putString("childUname", currentChildId);
            args.putString("childName", currentChildId);
            args.putString("user", "parent");
            sympFrag.setArguments(args);
            loadFragment(sympFrag);
        });

        return view;
    }

    private void launchTriageOnboarding() {
        // 1. Create an Intent to start your OnboardingActivity.
        Intent intent = new Intent(requireContext(), OnboardingActivity.class);

        // 2. Set the flag to "help" to trigger the triage data collection flow.
        intent.putExtra("onboardingType", "help");

        // 3. Pass the current child's username/ID to the OnboardingActivity.
        //    The 'currentChildId' variable already holds the correct ID.
        intent.putExtra("username", currentChildId);
        intent.putExtra("type", "children");

        // 4. Start the activity.
        startActivity(intent);
    }

    private void launchTechniqueOnboarding() {
        // 1. Create an Intent to start your OnboardingActivity.
        Intent intent = new Intent(requireContext(), OnboardingActivity.class);

        // 2. Set the flag to "technique" to trigger the correct set of slides.
        intent.putExtra("onboardingType", "technique");
        intent.putExtra("username", currentChildId);
        intent.putExtra("type", "children");
        // Note: We don't need to pass the username here as this flow doesn't save data.

        // 3. Start the activity.
        startActivity(intent);
    }

    // ───────────────────────────────
    // MENU HANDLING
    // ───────────────────────────────
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        MenuHelper.setupMenuWithoutAlerts(menu, inflater, requireContext());
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return MenuHelper.handleMenuSelection(item, this) || super.onOptionsItemSelected(item);
    }


    // ─────────────────────────────────────────────────────────────────
    // NAVIGATION HELPERS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Replace the fragment_container with the provided fragment and add the transaction
     * to the back stack so the user can navigate back.
     *
     * Notes:
     * - Uses getParentFragmentManager() to interact with the hosting Activity's fragment stack.
     *
     * @param fragment Fragment instance to display
     */
    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);

        transaction.addToBackStack(null);
        transaction.commit();
    }
}
