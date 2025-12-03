package com.example.SmartAirGroup2.ProviderDashboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.SmartAirGroup2.Main.CurrentUser;
import com.example.SmartAirGroup2.Helpers.MenuHelper;
import com.example.SmartAirGroup2.R;
import com.example.SmartAirGroup2.Main.TermsDialogFragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ProviderDashboardFragment
 * -------------------------
 * This fragment serves as the primary dashboard for provider users (e.g., doctors, caretakers)
 * within the Smart Air application. It provides an overview of all children whose parents have
 * granted this provider access to their data.
 *
 * Core User Actions:
 *   • View all children from different parents who have shared data with the provider.
 *   - Navigate to individual child dashboards for detailed monitoring of shared data.
 *
 * UI Behavior:
 *   - The fragment dynamically generates CardViews representing each accessible child.
 *   - Each card displays:
 *       • Child's name.
 *   - Cards use ripple effects for modern touch feedback.
 *
 * Firebase Structure (Relevant Paths):
 * └── categories/
 *     └── users/
 *         ├── provider/{providerUname}/parents/{parentUname: true}
 *         ├── parents/{parentUname}/children/{childUname: "child-username"}
 *         └── children/{childUname}/
 *             ├── name: String
 *             ├── uname: String
 *             └── shareToProviderPermissions/{permission: boolean}
 *
 * Data Fetching Logic:
 *   1.  Find all parents linked to the current provider.
 *   2.  For each parent, find all their linked children.
 *   3.  For each child, fetch their profile and check if they have granted any data sharing
 *       permissions to the provider.
 *   4.  If permissions are granted, display a card for the child.
 *
 * Fragment Lifecycle Responsibilities:
 *   ✔ Initialize toolbar and UI components.
 *   ✔ Fetch Firebase data for accessible children in a multi-step query.
 *   ✔ Dynamically create UI elements based on fetched data.
 *   ✔ Handle user interactions (navigation).
 *
 * Navigation:
 *   - Tapping a child card navigates to ProviderSideChildDashboardFragment with child details and permissions.
 *
 * Dependencies:
 *   • Firebase Realtime Database for user and relationship data.
 *   • CurrentUser for retrieving the logged-in provider's identity.
 *   • MenuHelper for toolbar menu operations.
 */
public class ProviderDashboardFragment extends Fragment {

    // ═══════════════════════════════════════════════════════════════════════
    // UI COMPONENTS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Toolbar component displayed at the top of the fragment.
     * Provides navigation and menu actions for the provider user.
     */
    private Toolbar toolbar;

    /**
     * Container that holds all dynamically generated child cards.
     * Children are added/removed from this layout based on Firebase data.
     */
    private LinearLayout contentContainer;

    // ═══════════════════════════════════════════════════════════════════════
    // FIREBASE REFERENCES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Firebase Database instance pointing to the Smart Air database.
     * Used to initialize all database references.
     */
    private FirebaseDatabase db;


    // ═══════════════════════════════════════════════════════════════════════
    // USER IDENTITY
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Username, email, and type of the currently logged-in user.
     */
    private String uname, email, type;
    private View notificationActionView;
    private View notificationBadge;


    // ═══════════════════════════════════════════════════════════════════════
    // LIFECYCLE METHODS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Creates a new instance of ProviderDashboardFragment with the given username.
     *
     * @param username The username of the provider.
     * @return A new instance of ProviderDashboardFragment.
     */
    public static ProviderDashboardFragment newInstance(String username) {
        ProviderDashboardFragment fragment = new ProviderDashboardFragment();
        Bundle args = new Bundle();
        args.putString("username", username);
        fragment.setArguments(args);
        return fragment;

    }

    /**
     * Called to do initial creation of a fragment.
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state, this is the state.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve provider username passed as argument
        if (getArguments() != null) {
            uname = getArguments().getString("username");
        }
        type="provider";
    }

    /**
     * Creates and initializes the view hierarchy for this fragment.
     *
     * Responsibilities:
     *   - Inflates the provider dashboard layout.
     *   - Sets up the toolbar with menu support.
     *   - Initializes Firebase database references.
     *   - Loads children data from Firebase.
     *   - Persists user identity to SharedPreferences.
     *
     * @param inflater           LayoutInflater to inflate the view.
     * @param container          Parent view that this fragment's UI will be attached to.
     * @param savedInstanceState Previously saved state, if any.
     * @return                   The root view for this fragment.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_provider_dashboard, container, false);

        // Get SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);

        // Get the current logged-in user's unique identifier
        uname = CurrentUser.get().getUname();  // or email, or a unique ID
        email = CurrentUser.get().getEmail();
        type = CurrentUser.get().getType();

        // Check if this user has accepted terms
        boolean hasAcceptedTerms = prefs.getBoolean("accepted_terms_" + type + uname + email, false);

        if (!hasAcceptedTerms) {
            // Show the TermsDialogFragment
            TermsDialogFragment dialog = new TermsDialogFragment();
            dialog.show(getParentFragmentManager(), "terms_dialog");
        }


        // Toolbar setup
        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        setHasOptionsMenu(true);

        // ─────────────────────────────────────────────────────────────────
        // UI Component Initialization
        // ─────────────────────────────────────────────────────────────────
        contentContainer = view.findViewById(R.id.contentContainer);

        // ─────────────────────────────────────────────────────────────────
        // Firebase Initialization
        // ─────────────────────────────────────────────────────────────────
        db = FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/");


        // ─────────────────────────────────────────────────────────────────
        // Load Children Data
        // ─────────────────────────────────────────────────────────────────
       searchChildrenForProvider();




        return view;
    }

    /**
     * Queries the database to get all parent usernames linked to a specific provider.
     * Assumed path: /categories/users/provider/{providerUname}/parents
     * The node contains keys where the key is the parent's username and the value is boolean 'true'.
     *
     */
    public void searchChildrenForProvider() {
        if (uname == null || uname.trim().isEmpty()) {
            Toast.makeText(getContext(), "Provider username is invalid.", Toast.LENGTH_LONG).show();
            return;
        }

        // 1. Get the list of parents linked to this provider
        DatabaseReference selfRef = db.getReference("categories/users/provider/" + uname + "/parents");

        selfRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot parentsSnapshot) {
                if (!parentsSnapshot.exists() || !parentsSnapshot.hasChildren()) {
                    Toast.makeText(getContext(), "No parent accounts are linked to this provider.", Toast.LENGTH_LONG).show();
                    return;
                }

                // Proceed to Step 2: Get Children for all Parents
                fetchChildrenLinks(parentsSnapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Query Step 1 Failed: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Step 2: Iterates through all linked parents to find the children usernames.
     * @param parentsSnapshot The DataSnapshot containing the linked parents.
     */
    private void fetchChildrenLinks(@NonNull DataSnapshot parentsSnapshot) {
        Set<String> childUsernames = new HashSet<>();
        Map<String, String> childToParentMap = new HashMap<>();

        final int totalParents = (int) parentsSnapshot.getChildrenCount();
        final int[] parentsProcessed = {0};

        for (DataSnapshot parentLinkSnapshot : parentsSnapshot.getChildren()) {
            String parentUname = parentLinkSnapshot.getValue().toString();
            if (parentUname == null) continue;

            DatabaseReference childLinksRef = db.getReference("categories/users/parents/" + parentUname + "/children");

            childLinksRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot childLinksSnapshot) {
                    if (childLinksSnapshot.exists()) {
                        for (DataSnapshot childLink : childLinksSnapshot.getChildren()) {
                            String childUname = childLink.getKey();
                            if (childUname != null) {
                                childUsernames.add(childUname);
                                childToParentMap.put(childUname, parentUname);
                            }
                        }
                    }

                    parentsProcessed[0]++;
                    if (parentsProcessed[0] == totalParents) {
                        // Proceed to Step 3: Fetch final child profile data
                        fetchChildren(childUsernames, childToParentMap);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Log the error but continue counting processed parents
                    parentsProcessed[0]++;
                    if (parentsProcessed[0] == totalParents) {
                        fetchChildren(childUsernames, childToParentMap);
                    }
                }
            });
        }
    }

    /**
     * Step 3: Fetches the full profile data for all unique children found.
     * @param childUsernames A set of child usernames to fetch data for.
     * @param childToParentMap A map from child username to parent username.
     */
    private void fetchChildren(Set<String> childUsernames, Map<String, String> childToParentMap) {
        if (childUsernames.isEmpty()) {
            Toast.makeText(getContext(), "No children found linked via parents.", Toast.LENGTH_LONG).show();
            return;
        }

        final int totalChildren = childUsernames.size();
        final int[] childrenProcessed = {0};

        for (String childUname : childUsernames) {
            DatabaseReference childDataRef = db.getReference("categories/users/children/" + childUname);

            childDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot childDataSnapshot) {
                    if (childDataSnapshot.exists()) {

                        String childName = childDataSnapshot.child("name").getValue(String.class);
                        DataSnapshot permissionsSnapshot = childDataSnapshot.child("shareToProviderPermissions");

                        List<String> truePermissions = new ArrayList<>();

                        if (permissionsSnapshot.exists()) {
                            // Check all permissions
                            boolean hasTruePermission = false;
                            for (DataSnapshot permission : permissionsSnapshot.getChildren()) {
                                // Check if the value is boolean true
                                Boolean isShared = permission.getValue(Boolean.class);

                                if (isShared != null && isShared) {
                                    hasTruePermission = true;
                                    truePermissions.add(permission.getKey());
                                }
                            }

                            // If at least one permission is true, call the UI function
                            if (hasTruePermission) {

                                addChildCard(childName, childUname, truePermissions);
                            }
                        }
                    }

                    childrenProcessed[0]++;

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Handle failure for individual child fetch
                    childrenProcessed[0]++;
                    if (childrenProcessed[0] == totalChildren) {
                        // Complete the operation with whatever data was successfully retrieved
                        Toast.makeText(getContext(), "Some Children failed to load.", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }



    // ═══════════════════════════════════════════════════════════════════════
    // UI CONSTRUCTION HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Dynamically creates and adds a CardView for a linked child.
     *
     * Card Features:
     *   - Displays child's name (or username as fallback).
     *   - Color-coded background based on status.
     *   - Ripple effect for touch feedback.
     *   - Navigates to ProviderSideChildDashboardFragment on click.
     *
     * @param childName   Display name of the child.
     * @param childKey    Username/key of the child in Firebase.
     * @param permissions List of permissions granted by the parent.
     */
    @SuppressLint("ResourceType")
    private void addChildCard(String childName, String childKey, List<String> permissions) {
        // Validate fragment state
        if (!isAdded() || getContext() == null) return;
        Context ctx = requireContext();

        // ─────────────────────────────────────────────────────────────────
        // Create CardView Container
        // ─────────────────────────────────────────────────────────────────
        CardView cardView = new CardView(ctx);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dpToPx(16));
        cardView.setLayoutParams(cardParams);

        // ─────────────────────────────────────────────────────────────────
        // Set Background Color Based on Status
        // ─────────────────────────────────────────────────────────────────

        cardView.setCardBackgroundColor(ContextCompat.getColor(getContext(), R.color.good));

        cardView.setRadius(dpToPx(8));
        cardView.setCardElevation(0);
        cardView.setClickable(true);
        cardView.setFocusable(true);

        // ─────────────────────────────────────────────────────────────────
        // Add Ripple Effect
        // ─────────────────────────────────────────────────────────────────
        TypedValue outValue = new TypedValue();
        if (ctx.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)) {
            Drawable selectable = ContextCompat.getDrawable(ctx, outValue.resourceId);
            if (selectable != null) cardView.setForeground(selectable);
        }

        // ─────────────────────────────────────────────────────────────────
        // Create Inner Layout
        // ─────────────────────────────────────────────────────────────────
        LinearLayout innerLayout = new LinearLayout(ctx);
        innerLayout.setOrientation(LinearLayout.HORIZONTAL);
        innerLayout.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        ));
        innerLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        innerLayout.setGravity(Gravity.CENTER_VERTICAL);

        // ─────────────────────────────────────────────────────────────────
        // Child Name TextView
        // ─────────────────────────────────────────────────────────────────
        TextView textView = new TextView(ctx);
        textView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        textView.setText(childName);
        textView.setTextSize(18);


        // ─────────────────────────────────────────────────────────────────
        // Assemble Card
        // ─────────────────────────────────────────────────────────────────
        innerLayout.addView(textView);
        cardView.addView(innerLayout);

        // ─────────────────────────────────────────────────────────────────
        // Card Click Handler - Navigate to Child Dashboard
        // ─────────────────────────────────────────────────────────────────
        cardView.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("childUname", childKey);
            args.putString("childName", childName);
            args.putStringArrayList("permissions", (ArrayList<String>) permissions);

            ProviderSideChildDashboardFragment childFrag = new ProviderSideChildDashboardFragment();
            childFrag.setArguments(args);
            loadFragment(childFrag);
        });

        // ─────────────────────────────────────────────────────────────────
        // Delete Icon Click Handler - Unlink Child
        // ─────────────────────────────────────────────────────────────────

        // ─────────────────────────────────────────────────────────────────
        // Add Card to Container
        // ─────────────────────────────────────────────────────────────────
        if (cardView.getParent() == null)
            contentContainer.addView(cardView);
    }

    /**
     * Converts density-independent pixels (dp) to actual pixels (px).
     * Ensures consistent UI spacing across different screen densities.
     *
     * @param dp Value in density-independent pixels.
     * @return   Equivalent value in pixels for the current device.
     */
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MENU HANDLING
    // ═══════════════════════════════════════════════════════════════════════


    /**
     * Initialize the contents of the Fragment's standard options menu.
     *
     * @param menu The options menu in which you place your items.
     * @param inflater The MenuInflater to inflate the menu.
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        MenuHelper.setupMenuWithoutAlerts(menu, inflater, requireContext());
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * This hook is called whenever an item in your options menu is selected.
     *
     * @param item The menu item that was selected.
     * @return boolean Return false to allow normal menu processing to proceed, true to consume it here.
     */
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
     * @param fragment The fragment to navigate to.
     */
    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
