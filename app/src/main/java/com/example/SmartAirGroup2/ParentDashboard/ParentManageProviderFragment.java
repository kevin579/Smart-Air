package com.example.SmartAirGroup2.ParentDashboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
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

import com.example.SmartAirGroup2.Main.CurrentUser;
import com.example.SmartAirGroup2.Helpers.MenuHelper;
import com.example.SmartAirGroup2.R;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


/**
 * A fragment for parent users to manage their linked provider accounts.
 * <p>
 * This fragment displays a list of all providers currently linked to the parent's account.
 * It allows the parent to perform the following actions:
 * <ul>
 *     <li>View a list of all linked providers.</li>
 *     <li>Navigate to a dedicated fragment to link a new provider account.</li>
 *     <li>Unlink/remove an existing provider from their account.</li>
 * </ul>
 * </p>
 * <p>
 * <b>UI and Behavior:</b>
 * <ul>
 *     <li>The fragment dynamically generates a {@link CardView} for each linked provider.</li>
 *     <li>Each card displays the provider's username and includes a delete icon for unlinking.</li>
 *     <li>A static "Add Provider" card is always present, which navigates to the {@link LinkProviderFragment}.</li>
 *     <li>User interactions (clicking a card, deleting a provider) are handled within this fragment.</li>
 * </ul>
 * </p>
 * <p>
 * <b>Firebase Integration:</b>
 * <p>The fragment interacts with the Firebase Realtime Database to fetch and manage provider relationships.</p>
 * Relevant Firebase Paths:
 * <ul>
 *     <li><code>/categories/users/parents/{parentUname}/providers/</code> - To read the list of linked provider usernames.</li>
 *     <li><code>/categories/users/provider/{providerUname}/parents/</code> - To remove the parent's reference when unlinking.</li>
 * </ul>
 * Unlinking is a two-way operation, removing the relationship from both the parent's and the provider's data nodes.
 * </p>
 * <p>
 * <b>Lifecycle:</b>
 * In {@code onCreateView}, the fragment initializes its UI components, sets up the toolbar,
 */
public class ParentManageProviderFragment extends Fragment {

    private Toolbar toolbar;

    private CardView cardAddProvider;


    private LinearLayout contentContainer;


    private FirebaseDatabase db;

    private DatabaseReference providerRef;


    private String uname;



    /**
     * Creates and initializes the view hierarchy for this fragment.
     *
     * Responsibilities:
     *   - Inflates the parent dashboard layout
     *   - Sets up the toolbar with menu support
     *   - Initializes Firebase database references
     *   - Loads Provider data from Firebase
     *   - Persists user identity to SharedPreferences
     *   - Configures "Add provider" button click handler
     *
     * @param inflater           LayoutInflater to inflate the view
     * @param container          Parent view that this fragment's UI will be attached to
     * @param savedInstanceState Previously saved state, if any
     * @return                   The root view for this fragment
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_parent_manage_provider, container, false);

        // Get SharedPreferences

        // Get the current logged-in user's unique identifier
        uname = CurrentUser.get().getUname();  // or email, or a unique ID

        // Toolbar setup
        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        setHasOptionsMenu(true);

        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        // ─────────────────────────────────────────────────────────────────
        // UI Component Initialization
        // ─────────────────────────────────────────────────────────────────
        contentContainer = view.findViewById(R.id.contentContainer);
        cardAddProvider = view.findViewById(R.id.cardAddProvider);

        // ─────────────────────────────────────────────────────────────────
        // Firebase Initialization
        // ─────────────────────────────────────────────────────────────────
        db = FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/");
        providerRef = db.getReference("categories/users/parents/" + uname + "/providers");


        // ─────────────────────────────────────────────────────────────────
        // Load Provider Data
        // ─────────────────────────────────────────────────────────────────
        loadProviderFromDatabase();

        // ─────────────────────────────────────────────────────────────────
        // Add provider Button Handler
        // ─────────────────────────────────────────────────────────────────

        cardAddProvider.setOnClickListener(v -> {
            LinkProviderFragment linkFrag = new LinkProviderFragment();
            Bundle args = new Bundle();
            args.putString("parentUname", uname);
            linkFrag.setArguments(args);
            loadFragment(linkFrag);

        });

        return view;
    }


    // ═══════════════════════════════════════════════════════════════════════
    // FIREBASE DATA LOADING
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Loads all Provider linked to the current parent from Firebase.
     *
     * Process:
     *   1. Queries the parent's Provider node for all linked usernames
     *   2. For each provider username:
     *      a. Fetches provider details (name, username)
     *      b. Fetches provider status history
     *      c. Creates a CardView with appropriate color coding
     *   3. Ensures "Add provider" card is added after all Provider load
     *   4. Displays toast if no Provider are found
     *
     * Color Coding:
     *   - Red (alert) if status contains 1 or 2
     *   - Green (good) otherwise
     *
     * Threading:
     *   - Uses Firebase's asynchronous listeners
     *   - Tracks completion count to ensure "Add provider" appears last
     */
    private void loadProviderFromDatabase() {

        providerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Validate fragment and context availability
                if (contentContainer == null || getContext() == null) return;

                // Clear existing views
                contentContainer.removeAllViews();
                int totalProvider = (int) snapshot.getChildrenCount();

                // Handle case with no Provider
                if (totalProvider == 0) {
                    contentContainer.addView(cardAddProvider);
                    Toast.makeText(getContext(), "No providers linked yet", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Track loading progress to ensure "Add provider" button appears last
                final int[] loadedProvider = {0};
                final int[] completedStatusLoads = {0};

                // Iterate through each provider username
                for (DataSnapshot providerSnapshot : snapshot.getChildren()) {
                    String providerUname = providerSnapshot.getValue(String.class);

                    // Skip invalid entries
                    if (providerUname == null || providerUname.isEmpty()) {
                        loadedProvider[0]++;
                        completedStatusLoads[0]++;
                        if (completedStatusLoads[0] == totalProvider)
                            contentContainer.addView(cardAddProvider);
                        continue;
                    }

                    addproviderCard(providerUname);
                }
                contentContainer.addView(cardAddProvider);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FIREBASE", "Error: " + error.getMessage());
            }
        });
    }



    // ═══════════════════════════════════════════════════════════════════════
    // UI CONSTRUCTION HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Dynamically creates and adds a CardView for a linked provider.
     *
     * Card Features:
     *   - Displays provider's name (or username as fallback)
     *   - Color-coded background based on status:
     *       • Red if status list contains 1 or 2 (alert)
     *       • Green otherwise (good)
     *   - Delete icon for unlinking the provider
     *   - Ripple effect for touch feedback
     *   - Navigates to providerDashboardFragment on click
     *
     * @param providerKey    Username/key of the provider in Firebase
     */
    @SuppressLint("ResourceType")
    private void addproviderCard(String providerKey) {
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
        cardView.setCardBackgroundColor(ContextCompat.getColor(getContext(), R.color.blue_light));


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
        // provider Name TextView
        // ─────────────────────────────────────────────────────────────────
        TextView textView = new TextView(ctx);
        textView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        textView.setText(providerKey);
        textView.setTextSize(18);

        // ─────────────────────────────────────────────────────────────────
        // Delete Icon
        // ─────────────────────────────────────────────────────────────────
        ImageView deleteView = new ImageView(ctx);
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(dpToPx(24), dpToPx(24));
        imgParams.setMarginEnd(dpToPx(12));
        deleteView.setLayoutParams(imgParams);
        deleteView.setImageResource(android.R.drawable.ic_delete);
        deleteView.setColorFilter(ContextCompat.getColor(ctx, R.color.delete), PorterDuff.Mode.SRC_IN);



        // ─────────────────────────────────────────────────────────────────
        // Assemble Card
        // ─────────────────────────────────────────────────────────────────
        innerLayout.addView(textView);
        innerLayout.addView(deleteView);
        cardView.addView(innerLayout);

        // ─────────────────────────────────────────────────────────────────
        // Card Click Handler - Navigate to provider Dashboard
        // ─────────────────────────────────────────────────────────────────
//        cardView.setOnClickListener(v -> {
//            Bundle args = new Bundle();
//            args.putString("providerUname", providerKey);
//            args.putString("providerName", providerName);
//
//            ParentSideproviderDashboardFragment providerFrag = new ParentSideproviderDashboardFragment();
//            providerFrag.setArguments(args);
//            loadFragment(providerFrag);
//        });

        // ─────────────────────────────────────────────────────────────────
        // Delete Icon Click Handler - Unlink provider
        // ─────────────────────────────────────────────────────────────────
        deleteView.setOnClickListener(v -> {
            if (!isAdded()) return;

            new AlertDialog.Builder(requireContext())
                    .setTitle("Remove provider")
                    .setMessage("Are you sure you want to unlink " + providerKey + "?")
                    .setPositiveButton("Yes", (d, w) -> {
                        removeLink(providerKey, uname);
                        loadProviderFromDatabase();
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
     * Main function to remove the bidirectional link between a Parent and a Provider.
     * * @param providerUname The username of the provider to unlink.
     * @param parentUname The username of the currently logged-in parent.
     */
    private void removeLink(String providerUname, String parentUname) {

        // Validate input fields (assuming providerUname is the target to remove)
        if (providerUname == null || providerUname.trim().isEmpty()) {
            Toast.makeText(getContext(), "Please select a provider to unlink.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (parentUname == null || parentUname.isEmpty()) {
            Toast.makeText(getContext(), "Error: Current parent username not found.", Toast.LENGTH_LONG).show();
            return;
        }

        Task<Void> task1 = removeParentDB(providerUname, parentUname);
        Task<Void> task2 = removeProviderDB(providerUname, parentUname);

        Tasks.whenAll(task1, task2)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Successfully removed link with: " + providerUname, Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    // If either deletion failed, notify the user.
                    Toast.makeText(getContext(), "Error removing link. Please check connectivity: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * 1. Removes the Provider's reference from the Parent's database entry.
     * Path: categories/users/parents/{parentUname}/providers/{providerUname}
     */
    private Task<Void> removeParentDB(String providerUname, String parentUname) {
        // Reference to the specific provider key under the parent's providers list
        DatabaseReference parentRef = FirebaseDatabase.getInstance()
                .getReference("categories/users/parents/" + parentUname + "/providers");

        // Use removeValue() to delete the provider's username node
        return parentRef.child(providerUname).removeValue();
    }

    /**
     * 2. Removes the Parent's reference from the Provider's database entry.
     * Path: categories/users/provider/{providerUname}/parents/{parentUname}
     */
    private Task<Void> removeProviderDB(String providerUname, String parentUname) {
        // Reference to the specific parent key under the provider's parents list
        DatabaseReference providerRef = FirebaseDatabase.getInstance()
                .getReference("categories/users/provider/" + providerUname + "/parents");

        // Use removeValue() to delete the parent's username node
        return providerRef.child(parentUname).removeValue();
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
        MenuHelper.setupNotification(this,menu,inflater);

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
