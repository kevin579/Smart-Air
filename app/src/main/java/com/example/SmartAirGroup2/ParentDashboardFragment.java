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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
import java.util.List;

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
public class ParentDashboardFragment extends Fragment {

    // ═══════════════════════════════════════════════════════════════════════
    // UI COMPONENTS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Toolbar component displayed at the top of the fragment.
     * Provides navigation and menu actions for the parent user.
     */
    private Toolbar toolbar;

    /**
     * CardView for the "Add Child" button.
     * Always displayed at the bottom of the children list.
     */
    private CardView cardAddChild;

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
    private DatabaseReference childrenRef;

    // ═══════════════════════════════════════════════════════════════════════
    // USER IDENTITY
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Username of the currently logged-in parent.
     * TODO: Replace hardcoded value with dynamic authentication.
     */
    private String uname = "kevin579";

    /**
     * User type identifier for the current user.
     * Always "parent" for this fragment.
     */
    private String type = "parent";

    // ═══════════════════════════════════════════════════════════════════════
    // LIFECYCLE METHODS
    // ═══════════════════════════════════════════════════════════════════════

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
        View view = inflater.inflate(R.layout.activity_parent_dashboard, container, false);

        // ─────────────────────────────────────────────────────────────────
        // Toolbar Configuration
        // ─────────────────────────────────────────────────────────────────
        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        setHasOptionsMenu(true);

        // ─────────────────────────────────────────────────────────────────
        // UI Component Initialization
        // ─────────────────────────────────────────────────────────────────
        contentContainer = view.findViewById(R.id.contentContainer);
        cardAddChild = view.findViewById(R.id.cardAddChild);

        // ─────────────────────────────────────────────────────────────────
        // Firebase Initialization
        // ─────────────────────────────────────────────────────────────────
        db = FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/");
        childrenRef = db.getReference("categories/users/parents/" + uname + "/children");

        // ─────────────────────────────────────────────────────────────────
        // Load Children Data
        // ─────────────────────────────────────────────────────────────────
        loadChildrenFromDatabase();

        // ─────────────────────────────────────────────────────────────────
        // Persist User Identity
        // ─────────────────────────────────────────────────────────────────
        SharedPreferences prefs = requireContext().getSharedPreferences("APP_DATA", Context.MODE_PRIVATE);
        prefs.edit().putString("parentUname", uname).apply();
        prefs.edit().putString("type", type).apply();

        // ─────────────────────────────────────────────────────────────────
        // Add Child Button Handler
        // ─────────────────────────────────────────────────────────────────
        // Shows a dialog asking if the child already has an account:
        //   - "Yes" → Navigate to LinkChildFragment
        //   - "No"  → Navigate to AddChildFragment
        cardAddChild.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Confirm Action")
                    .setMessage("Does your child already have an account?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // Navigate to link existing child
                        LinkChildFragment linkFrag = new LinkChildFragment();
                        Bundle args = new Bundle();
                        args.putString("parentUname", uname);
                        linkFrag.setArguments(args);
                        loadFragment(linkFrag);
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        // Navigate to create new child
                        AddChildFragment addFrag = new AddChildFragment();
                        Bundle args = new Bundle();
                        args.putString("parentUname", uname);
                        addFrag.setArguments(args);
                        loadFragment(addFrag);
                        dialog.dismiss();
                    })
                    .show();
        });

        return view;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FIREBASE DATA LOADING
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Loads all children linked to the current parent from Firebase.
     *
     * Process:
     *   1. Queries the parent's children node for all linked usernames
     *   2. For each child username:
     *      a. Fetches child details (name, username)
     *      b. Fetches child status history
     *      c. Creates a CardView with appropriate color coding
     *   3. Ensures "Add Child" card is added after all children load
     *   4. Displays toast if no children are found
     *
     * Color Coding:
     *   - Red (alert) if status contains 1 or 2
     *   - Green (good) otherwise
     *
     * Threading:
     *   - Uses Firebase's asynchronous listeners
     *   - Tracks completion count to ensure "Add Child" appears last
     */
    private void loadChildrenFromDatabase() {
        childrenRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Validate fragment and context availability
                if (contentContainer == null || getContext() == null) return;

                // Clear existing views
                contentContainer.removeAllViews();
                int totalChildren = (int) snapshot.getChildrenCount();

                // Handle case with no children
                if (totalChildren == 0) {
                    contentContainer.addView(cardAddChild);
                    Toast.makeText(getContext(), "No children linked yet", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Track loading progress to ensure "Add Child" button appears last
                final int[] loadedChildren = {0};
                final int[] completedStatusLoads = {0};

                // Iterate through each child username
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    String childUname = childSnapshot.getValue(String.class);

                    // Skip invalid entries
                    if (childUname == null || childUname.isEmpty()) {
                        loadedChildren[0]++;
                        completedStatusLoads[0]++;
                        if (completedStatusLoads[0] == totalChildren)
                            contentContainer.addView(cardAddChild);
                        continue;
                    }

                    // Reference to child's data node
                    DatabaseReference childRef = FirebaseDatabase.getInstance()
                            .getReference("categories/users/children")
                            .child(childUname);

                    // Fetch child details
                    childRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot childData) {
                            if (childData.exists()) {
                                User child = childData.getValue(User.class);
                                if (child != null) {
                                    // Determine display name (prefer full name over username)
                                    String displayName = (child.getName() != null && !child.getName().isEmpty())
                                            ? child.getName()
                                            : child.getUname();

                                    // Fetch status history for color coding
                                    DatabaseReference statusRef = FirebaseDatabase.getInstance()
                                            .getReference("categories/users/children")
                                            .child(child.getUname())
                                            .child("status");

                                    statusRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot statusSnap) {
                                            // Parse status values from Firebase
                                            List<Integer> statusList = new ArrayList<>();

                                            if (statusSnap.exists()) {
                                                for (DataSnapshot s : statusSnap.getChildren()) {
                                                    try {
                                                        Integer val = s.getValue(Integer.class);
                                                        if (val != null) statusList.add(val);
                                                    } catch (Exception ignored) {}
                                                }
                                            }

                                            // Create child card with status-based color
                                            addChildCard(displayName, child.getUname(), statusList);

                                            // Check if all children loaded
                                            completedStatusLoads[0]++;
                                            if (completedStatusLoads[0] == totalChildren)
                                                contentContainer.addView(cardAddChild);
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            // Show card with default color on error
                                            addChildCard(displayName, child.getUname(), new ArrayList<>());

                                            completedStatusLoads[0]++;
                                            if (completedStatusLoads[0] == totalChildren)
                                                contentContainer.addView(cardAddChild);
                                        }
                                    });
                                } else {
                                    // Child data is null
                                    completedStatusLoads[0]++;
                                    if (completedStatusLoads[0] == totalChildren)
                                        contentContainer.addView(cardAddChild);
                                }
                            } else {
                                // Child doesn't exist in database
                                completedStatusLoads[0]++;
                                if (completedStatusLoads[0] == totalChildren)
                                    contentContainer.addView(cardAddChild);
                            }

                            loadedChildren[0]++;
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            loadedChildren[0]++;
                            completedStatusLoads[0]++;
                            if (completedStatusLoads[0] == totalChildren)
                                contentContainer.addView(cardAddChild);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load children: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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
     * @param statusList  List of status values for determining card color
     */
    @SuppressLint("ResourceType")
    private void addChildCard(String childName, String childKey, List<Integer> statusList) {
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
        if (statusList.contains(2)) {
            cardView.setCardBackgroundColor(ContextCompat.getColor(getContext(), R.color.alert));
        } else if (statusList.contains(1)) {
            cardView.setCardBackgroundColor(ContextCompat.getColor(getContext(), R.color.alert));
        } else {
            cardView.setCardBackgroundColor(ContextCompat.getColor(getContext(), R.color.good));
        }

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
        // Delete Icon
        // ─────────────────────────────────────────────────────────────────
        ImageView imageView = new ImageView(ctx);
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(dpToPx(24), dpToPx(24));
        imgParams.setMarginEnd(dpToPx(12));
        imageView.setLayoutParams(imgParams);
        imageView.setImageResource(android.R.drawable.ic_delete);

        // ─────────────────────────────────────────────────────────────────
        // Assemble Card
        // ─────────────────────────────────────────────────────────────────
        innerLayout.addView(textView);
        innerLayout.addView(imageView);
        cardView.addView(innerLayout);

        // ─────────────────────────────────────────────────────────────────
        // Card Click Handler - Navigate to Child Dashboard
        // ─────────────────────────────────────────────────────────────────
        cardView.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("childUname", childKey);
            args.putString("childName", childName);

            ChildDashboardFragment childFrag = new ChildDashboardFragment();
            childFrag.setArguments(args);
            loadFragment(childFrag);
        });

        // ─────────────────────────────────────────────────────────────────
        // Delete Icon Click Handler - Unlink Child
        // ─────────────────────────────────────────────────────────────────
        imageView.setOnClickListener(v -> {
            if (!isAdded()) return;

            new AlertDialog.Builder(requireContext())
                    .setTitle("Remove Child")
                    .setMessage("Are you sure you want to unlink " + childName + "?")
                    .setPositiveButton("Yes", (d, w) -> {
                        if (childrenRef != null) {
                            childrenRef.child(childKey).removeValue();
                            Toast.makeText(ctx, "Child removed", Toast.LENGTH_SHORT).show();
                            loadChildrenFromDatabase(); // Refresh view
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

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
