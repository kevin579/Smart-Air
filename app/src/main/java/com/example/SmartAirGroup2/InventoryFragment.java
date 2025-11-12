package com.example.SmartAirGroup2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
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

public class InventoryFragment extends Fragment {

    // ───────────────────────────────
    // UI COMPONENTS
    // ───────────────────────────────
    private Toolbar toolbar;
    private CardView cardAddMediine;
    private LinearLayout contentContainer;

    // ───────────────────────────────
    // FIREBASE REFERENCES
    // ───────────────────────────────
    private FirebaseDatabase db;
    private DatabaseReference medicineRef;

    // Hardcoded parent username for demonstration
    // (should later be replaced by logged-in parent’s username)
    private String name, uname, medicineName;

    // ───────────────────────────────
    // LIFECYCLE METHODS
    // ───────────────────────────────

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve parent username passed as argument
        if (getArguments() != null) {
            name = getArguments().getString("childName");
            uname = getArguments().getString("childUname");
            medicineName = getArguments().getString("medicineName");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_child_medicine, container, false);

        // Toolbar setup
        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        setHasOptionsMenu(true);
        toolbar.setTitle(name + "'s Medicine Inventory");
        // Handle back navigation (up button)
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        // UI references
        contentContainer = view.findViewById(R.id.contentContainer);
        cardAddMediine = view.findViewById(R.id.cardAddMediine);

        // Initialize Firebase references
        db = FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/");
        medicineRef = db.getReference("categories/users/children/" + uname + "/inventory");

        // Load all children into the dashboard
        loadMedicineFromDatabase();

        // Handle "Add Child" button logic
        cardAddMediine.setOnClickListener(v -> {

            AddInventoryFragment addFrag = new AddInventoryFragment();
            Bundle args = new Bundle();
            args.putString("childUname", uname);
            addFrag.setArguments(args);
            loadFragment(addFrag);

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
    private void loadMedicineFromDatabase() {
        medicineRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (contentContainer == null || getContext() == null) return;

                contentContainer.removeAllViews();

                if (!snapshot.exists()) {
                    Toast.makeText(getContext(), "No medicine found", Toast.LENGTH_SHORT).show();
                    contentContainer.addView(cardAddMediine);
                    return;
                }

                for (DataSnapshot medSnapshot : snapshot.getChildren()) {
                    String medicineName = medSnapshot.getKey();

                    int currentAmount = medSnapshot.child("currentAmount").getValue(int.class);
                    int prescriptionAmount = medSnapshot.child("prescriptionAmount").getValue(int.class);
                    String purchaseDate = medSnapshot.child("purchaseDate").getValue(String.class);
                    String expireDate = medSnapshot.child("expireDate").getValue(String.class);

                    // You can use this info to dynamically create and display a card
                    addMedicineCard(medicineName, currentAmount, prescriptionAmount, purchaseDate, expireDate);
                }
                contentContainer.addView(cardAddMediine);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load medicines: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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
    private void addMedicineCard(String name, int current, int total, String purchaseDate, String expireDate) {
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
        cardView.setCardBackgroundColor(0xFFC8E6C9);
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
        LinearLayout outerLayout = new LinearLayout(ctx);
        outerLayout.setOrientation(LinearLayout.VERTICAL);
        outerLayout.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        ));
        outerLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

// ─────────────── Top row: Name + arrow ───────────────
        LinearLayout topRow = new LinearLayout(ctx);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        topRow.setGravity(Gravity.CENTER_VERTICAL);

// Child name (large)
        TextView nameView = new TextView(ctx);
        nameView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        nameView.setText(name);
        nameView.setTextSize(20);
        nameView.setTypeface(null, Typeface.BOLD);
        nameView.setTextColor(Color.BLACK);

// Arrow icon
        ImageView arrowView = new ImageView(ctx);
        LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(dpToPx(24), dpToPx(24));
        arrowView.setLayoutParams(arrowParams);
        arrowView.setImageResource(R.drawable.ic_arrow_right);
        arrowView.setColorFilter(ContextCompat.getColor(ctx, R.color.gray), PorterDuff.Mode.SRC_IN);

        topRow.addView(nameView);
        topRow.addView(arrowView);

        String text;

// ─────────────── Second row: Dates ───────────────
        TextView buyDateView = new TextView(ctx);
        text = "Purchased: " + purchaseDate;
        buyDateView.setText(text);
        buyDateView.setTextSize(14);
        buyDateView.setTextColor(Color.DKGRAY);
        buyDateView.setPadding(0, dpToPx(4), 0, 0);


// ─────────────── Third row: Dates ───────────────
        TextView expDateView = new TextView(ctx);
        text = "Expire: " + expireDate;
        expDateView.setText(text);
        expDateView.setTextSize(14);
        expDateView.setTextColor(Color.DKGRAY);
        expDateView.setPadding(0, dpToPx(4), 0, 0);

// ─────────────── Fourth row: Amounts ───────────────
        TextView amountView = new TextView(ctx);
        text = "Amount: " + current + " / " + total;
        amountView.setText(text);
        amountView.setTextSize(14);
        amountView.setTextColor(Color.DKGRAY);
        amountView.setPadding(0, dpToPx(2), 0, 0);

// ─────────────── Combine all ───────────────
        outerLayout.addView(topRow);
        outerLayout.addView(buyDateView);
        outerLayout.addView(expDateView);
        outerLayout.addView(amountView);

        cardView.addView(outerLayout);


//         Card click (show child details, future use)
        cardView.setOnClickListener(v ->{
                    Bundle args = new Bundle();
                    args.putString("childUname", uname);
                    args.putString("medicineName", name);
                    AddInventoryFragment addInvFrag = new AddInventoryFragment();
                    addInvFrag.setArguments(args);
                    loadFragment(addInvFrag);
                }

        );



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
