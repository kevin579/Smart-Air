package com.example.SmartAirGroup2;

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
 * ParentDashboardFragment
 * -----------------------
 * This fragment serves as the primary dashboard for parent users within the Smart Air application.
 * It provides an overview of all linked child accounts and enables parents to perform account
 * management actions.
 *
 * Core User Actions:
 *   • View all children currently linked to their account.
 *   • Add a new child or link an existing child account.
 *   • Remove/unlink a child account when needed.
 *   • Navigate to individual child dashboards for detailed monitoring.
 *
 * UI Behavior:
 *   - The fragment dynamically generates CardViews representing each linked child.
 *   - Each card displays:
 *       • Child name (or username if name unavailable)
 *       • Status indicator (color-coded background)
 *       • A delete icon to allow unlinking
 *   - If no children are linked, only the "Add Child" card is displayed.
 *   - Cards use ripple effects for modern touch feedback.
 *
 * Firebase Structure (Relevant Paths):
 * └── categories/
 *     └── users/
 *         ├── parents/{parentUname}/children/{childUname: String}
 *         └── children/{childUname}/
 *             ├── name: String
 *             ├── uname: String
 *             └── status/{individual status: Integer}
 *
 * Status Logic:
 *   - The background color of each child card reflects their status history:
 *       • Red (alert color) → Contains alert values (1 or 2 detected)
 *       • Green (good color) → No concerning status entries
 *   - Status values are retrieved from the child's status history in Firebase.
 *
 * Fragment Lifecycle Responsibilities:
 *   ✔ Initialize toolbar and UI components
 *   ✔ Fetch Firebase data for linked children
 *   ✔ Listen for child database changes
 *   ✔ Dynamically create UI elements based on data
 *   ✔ Persist login metadata using SharedPreferences
 *   ✔ Handle user interactions (navigation, deletion, linking)
 *
 * Navigation:
 *   - Tapping a child card navigates to ChildDashboardFragment with child details.
 *   - Tapping "Add Child" opens a dialog prompting:
 *        → "Yes": Navigate to LinkChildFragment (for existing accounts)
 *        → "No" : Navigate to AddChildFragment (for new accounts)
 *
 * Dependencies:
 *   • Firebase Realtime Database for user and relationship data.
 *   • SharedPreferences for user type and logged-in identity persistence.
 *   • MenuHelper for toolbar menu operations.
 *   • User model class for child data representation.
 *
 * Author: Kevin Li
 * Last Updated: November 18 2025
 */
public class ProviderDashboardFragment extends Fragment {

    // ═══════════════════════════════════════════════════════════════════════
    // UI COMPONENTS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Toolbar component displayed at the top of the fragment.
     * Provides navigation and menu actions for the parent user.
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

    /**
     * Database reference to the parent's children node.
     * Path: categories/users/parents/{uname}/children
     * Contains all child usernames linked to this parent.
     */

    // ═══════════════════════════════════════════════════════════════════════
    // USER IDENTITY
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Username of the currently logged-in parent.
     */

    /**
     * User type identifier for the current user.
     * Always "parent" for this fragment.
     */
    private String uname, email, type;
    private View notificationActionView;
    private View notificationBadge;


    // ═══════════════════════════════════════════════════════════════════════
    // LIFECYCLE METHODS
    // ═══════════════════════════════════════════════════════════════════════

    public static ProviderDashboardFragment newInstance(String username) {
        ProviderDashboardFragment fragment = new ProviderDashboardFragment();
        Bundle args = new Bundle();
        args.putString("username", username);
        fragment.setArguments(args);
        return fragment;

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve parent username passed as argument
        if (getArguments() != null) {
            uname = getArguments().getString("username");
        }
        type="provider";
    }

    /**
     * Creates and initializes the view hierarchy for this fragment.
     *
     * Responsibilities:
     *   - Inflates the parent dashboard layout
     *   - Sets up the toolbar with menu support
     *   - Initializes Firebase database references
     *   - Loads children data from Firebase
     *   - Persists user identity to SharedPreferences
     *   - Configures "Add Child" button click handler
     *
     * @param inflater           LayoutInflater to inflate the view
     * @param container          Parent view that this fragment's UI will be attached to
     * @param savedInstanceState Previously saved state, if any
     * @return                   The root view for this fragment
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
     *   - Displays child's name (or username as fallback)
     *   - Color-coded background based on status:
     *       • Red if status list contains 1 or 2 (alert)
     *       • Green otherwise (good)
     *   - Delete icon for unlinking the child
     *   - Ripple effect for touch feedback
     *   - Navigates to ChildDashboardFragment on click
     *
     * @param childName   Display name of the child
     * @param childKey    Username/key of the child in Firebase
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
     * @param dp Value in density-independent pixels
     * @return   Equivalent value in pixels for the current device
     */
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
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
        super.onCreateOptionsMenu(menu, inflater);

        MenuHelper.setupMenu(menu, inflater, requireContext());

        MenuItem notifItem = menu.findItem(R.id.action_notifications);
        if (notifItem != null) {
            notificationActionView = notifItem.getActionView();
            if (notificationActionView != null) {
                notificationBadge = notificationActionView.findViewById(R.id.viewBadge);

                notificationActionView.setOnClickListener(v -> onOptionsItemSelected(notifItem));
            }
        }
//        checkAlertsAndUpdateBadge();
    }
//    private void updateNotificationBadge(boolean hasAlerts) {
//        if (notificationBadge == null) return;
//        notificationBadge.setVisibility(hasAlerts ? View.VISIBLE : View.GONE);
//    }
//
//    private void checkAlertsAndUpdateBadge() {
//        if (db == null) {
//            db = FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/");
//        }
//
//        DatabaseReference parentChildrenRef = db.getReference("categories/users/parents")
//                .child(uname)
//                .child("children");
//
//        parentChildrenRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
//                    updateNotificationBadge(false);
//                    return;
//                }
//
//                int totalChildren = (int) snapshot.getChildrenCount();
//                final int[] finished = {0};
//                final boolean[] hasAlerts = {false};
//
//                for (DataSnapshot childSnap : snapshot.getChildren()) {
//                    String childUname = childSnap.getValue(String.class);
//
//                    if (childUname == null || childUname.trim().isEmpty()) {
//                        if (++finished[0] == totalChildren && !hasAlerts[0]) {
//                            updateNotificationBadge(false);
//                        }
//                        continue;
//                    }
//
//                    DatabaseReference statusRef = db.getReference("categories/users/children")
//                            .child(childUname)
//                            .child("status");
//
//                    statusRef.addListenerForSingleValueEvent(new ValueEventListener() {
//                        @Override
//                        public void onDataChange(@NonNull DataSnapshot statusSnap) {
//                            if (!hasAlerts[0]) {
//                                if (statusHasAlert(statusSnap)) {
//                                    hasAlerts[0] = true;
//                                    updateNotificationBadge(true);
//                                }
//                            }
//
//                            if (++finished[0] == totalChildren && !hasAlerts[0]) {
//                                updateNotificationBadge(false);
//                            }
//                        }
//
//                        @Override
//                        public void onCancelled(@NonNull DatabaseError error) {
//                            if (++finished[0] == totalChildren && !hasAlerts[0]) {
//                                updateNotificationBadge(false);
//                            }
//                        }
//                    });
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                updateNotificationBadge(false);
//            }
//        });
//    }
//
//
//    private boolean statusHasAlert(DataSnapshot statusSnap) {
//        if (statusSnap == null || !statusSnap.exists()) {
//            return false;
//        }
//
//        // PEF red zone
//        Integer pefZone = statusSnap.child("pefZone").getValue(Integer.class);
//        if (pefZone != null && pefZone == 2) {
//            return true;
//        }
//
//        DataSnapshot invSnap = statusSnap.child("inventory");
//        if (invSnap != null && invSnap.exists()) {
//            for (DataSnapshot medSnap : invSnap.getChildren()) {
//                for (DataSnapshot snapIndex : medSnap.getChildren()) {
//                    Integer code = snapIndex.getValue(Integer.class);
//                    if (code != null && (code == 1 || code == 2)) {
//                        return true;
//                    }
//                }
//            }
//        }
//
//        return false;
//    }
//


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
