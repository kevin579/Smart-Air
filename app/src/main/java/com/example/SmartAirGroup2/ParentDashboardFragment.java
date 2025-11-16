package com.example.SmartAirGroup2;

import android.annotation.SuppressLint;
import android.content.Context;
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
 * This fragment serves as the main dashboard for parent users.
 * It allows parents to:
 *   • View their linked child accounts.
 *   • Add or link new child accounts.
 *   • Remove linked children.
 *
 * The fragment dynamically builds its UI using CardViews that represent each child.
 * It connects to Firebase Realtime Database to fetch and manage child relationships.
 *
 * Firebase Structure (relevant paths):
 * └── categories/
 *     └── users/
 *         ├── parents/{parentUname}/children/{childUname: String}
 *         └── children/{childUname}/... (child details)
 *
 * Core Features:
 *   - Loads all children linked to the current parent.
 *   - Dynamically generates a CardView for each child.
 *   - Provides delete functionality to unlink a child.
 *   - Supports navigation to AddChildFragment and LinkChildFragment.
 *   - Uses a toolbar with notification and settings menu options.
 *
 * Author: [Your Name]
 * Date: [Date]
 */

public class ParentDashboardFragment extends Fragment {

    // ───────────────────────────────
    // UI COMPONENTS
    // ───────────────────────────────
    private Toolbar toolbar;
    private CardView cardAddChild;
    private LinearLayout contentContainer;

    // ───────────────────────────────
    // FIREBASE REFERENCES
    // ───────────────────────────────
    private FirebaseDatabase db;
    private DatabaseReference childrenRef;

    // Hardcoded parent username for demonstration
    // (should later be replaced by logged-in parent’s username)
    private String uname = "kevin579";

    // ───────────────────────────────
    // LIFECYCLE METHODS
    // ───────────────────────────────
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_parent_dashboard, container, false);

        // Toolbar setup
        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        setHasOptionsMenu(true);

        // UI references
        contentContainer = view.findViewById(R.id.contentContainer);
        cardAddChild = view.findViewById(R.id.cardAddChild);

        // Initialize Firebase references
        db = FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/");
        childrenRef = db.getReference("categories/users/parents/" + uname + "/children");

        // Load all children into the dashboard
        loadChildrenFromDatabase();

        // Handle "Add Child" button logic
        cardAddChild.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Confirm Action")
                    .setMessage("Does your child already have an account?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // Navigate to link-existing-child fragment
                        LinkChildFragment linkFrag = new LinkChildFragment();
                        Bundle args = new Bundle();
                        args.putString("parentUname", uname);
                        linkFrag.setArguments(args);
                        loadFragment(linkFrag);
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        // Navigate to create-new-child fragment
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

    // ───────────────────────────────
    // FIREBASE DATA LOADING
    // ───────────────────────────────
    /**
     * Loads the list of children linked to the current parent.
     * Each child entry creates a CardView dynamically inside `contentContainer`.
     * If no children exist, only the "Add Child" card is displayed.
     */
    private void loadChildrenFromDatabase() {
        childrenRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (contentContainer == null || getContext() == null) return;

                contentContainer.removeAllViews();
                int totalChildren = (int) snapshot.getChildrenCount();

                if (totalChildren == 0) {
                    contentContainer.addView(cardAddChild);
                    Toast.makeText(getContext(), "No children linked yet", Toast.LENGTH_SHORT).show();
                    return;
                }

                final int[] loadedChildren = {0};
                final int[] completedStatusLoads = {0}; // Track status loads completion

                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    String childUname = childSnapshot.getValue(String.class);
                    if (childUname == null || childUname.isEmpty()) {
                        loadedChildren[0]++;
                        completedStatusLoads[0]++;
                        if (completedStatusLoads[0] == totalChildren)
                            contentContainer.addView(cardAddChild);
                        continue;
                    }

                    DatabaseReference childRef = FirebaseDatabase.getInstance()
                            .getReference("categories/users/children")
                            .child(childUname);

                    // Retrieve child details
                    childRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot childData) {
                            if (childData.exists()) {
                                User child = childData.getValue(User.class);
                                if (child != null) {

                                    String displayName = (child.getName() != null && !child.getName().isEmpty())
                                            ? child.getName()
                                            : child.getUname();

                                    DatabaseReference statusRef = FirebaseDatabase.getInstance()
                                            .getReference("categories/users/children")
                                            .child(child.getUname())
                                            .child("status");

                                    statusRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot statusSnap) {
                                            List<Integer> statusList = new ArrayList<>();

                                            if (statusSnap.exists()) {
                                                for (DataSnapshot s : statusSnap.getChildren()) {
                                                    try {
                                                        Integer val = s.getValue(Integer.class);
                                                        if (val != null) statusList.add(val);
                                                    } catch (Exception ignored) {}
                                                }
                                            }

                                            addChildCard(displayName, child.getUname(), statusList);

                                            // Add button after all status loads complete
                                            completedStatusLoads[0]++;
                                            if (completedStatusLoads[0] == totalChildren)
                                                contentContainer.addView(cardAddChild);
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            // Still show card with default color
                                            addChildCard(displayName, child.getUname(), new ArrayList<>());

                                            // Add button after all status loads complete
                                            completedStatusLoads[0]++;
                                            if (completedStatusLoads[0] == totalChildren)
                                                contentContainer.addView(cardAddChild);
                                        }
                                    });
                                } else {
                                    // Child data null
                                    completedStatusLoads[0]++;
                                    if (completedStatusLoads[0] == totalChildren)
                                        contentContainer.addView(cardAddChild);
                                }
                            } else {
                                // Child doesn't exist
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



    // ───────────────────────────────
    // UI CONSTRUCTION HELPERS
    // ───────────────────────────────
    /**
     * Dynamically creates a card for each linked child.
     * Displays child name and includes a delete icon to unlink the child.
     */
    @SuppressLint("ResourceType")
    private void addChildCard(String childName, String childKey, List<Integer> statusList) {
        if (!isAdded() || getContext() == null) return;
        Context ctx = requireContext();

        // Create the outer CardView
        CardView cardView = new CardView(ctx);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dpToPx(16));
        cardView.setLayoutParams(cardParams);
//        cardView.setCardBackgroundColor(0xFFC8E6C9);
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

        // Add ripple effect for touch feedback
        TypedValue outValue = new TypedValue();
        if (ctx.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)) {
            Drawable selectable = ContextCompat.getDrawable(ctx, outValue.resourceId);
            if (selectable != null) cardView.setForeground(selectable);
        }

        // Inner layout for content
        LinearLayout innerLayout = new LinearLayout(ctx);
        innerLayout.setOrientation(LinearLayout.HORIZONTAL);
        innerLayout.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        ));
        innerLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        innerLayout.setGravity(Gravity.CENTER_VERTICAL);

        // Child name text
        TextView textView = new TextView(ctx);
        textView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        textView.setText(childName);
        textView.setTextSize(18);

        // Delete icon
        ImageView imageView = new ImageView(ctx);
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(dpToPx(24), dpToPx(24));
        imgParams.setMarginEnd(dpToPx(12));
        imageView.setLayoutParams(imgParams);
        imageView.setImageResource(android.R.drawable.ic_delete);

        innerLayout.addView(textView);
        innerLayout.addView(imageView);
        cardView.addView(innerLayout);

        // Card click (show child details, future use)
        cardView.setOnClickListener(v ->{
                    Bundle args = new Bundle();
                    args.putString("childUname", childKey);
                    args.putString("childName", childName);
//                    Toast.makeText(ctx, "Clicked: " + childName, Toast.LENGTH_SHORT).show();
                    ChildDashboardFragment childFrag = new ChildDashboardFragment();
                    childFrag.setArguments(args);
                    loadFragment(childFrag);
                }

        );

        // Delete logic
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

        if (cardView.getParent() == null)
            contentContainer.addView(cardView);
    }

    /** Converts dp to pixels for consistent UI spacing. */
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // ───────────────────────────────
    // MENU HANDLING
    // ───────────────────────────────
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        MenuHelper.setupMenu(menu, inflater, requireContext());
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (MenuHelper.handleMenuSelection(item, this)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ───────────────────────────────
    // FRAGMENT NAVIGATION
    // ───────────────────────────────
    /**
     * Utility method for fragment navigation inside the same activity.
     */
    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
