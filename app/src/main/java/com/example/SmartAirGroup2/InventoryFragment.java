package com.example.SmartAirGroup2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * InventoryFragment
 * ------------------
 * This fragment displays and manages a child's medicine inventory.
 * It allows users (parents or guardians) to:
 *   • View all medicine entries associated with the selected child.
 *   • Add new medication records.
 *   • Open and modify existing medication details.
 *   • Automatically determine medication status based on expiry date and dosage level.
 *
 * Data is loaded from Firebase Realtime Database and displayed dynamically using CardViews.
 * Each medication card includes:
 *   - Name
 *   - Current remaining amount
 *   - Original prescribed amount
 *   - Purchase date
 *   - Expiry date
 *   - Automatic warning/alert coloring based on rules
 *
 * Firebase Database Structure (relevant path):
 * └── categories/
 *     └── users/
 *         └── children/{childUsername}/
 *               ├── inventory/{medicineName}/
 *               │     ├── currentAmount: Number
 *               │     ├── prescriptionAmount: Number
 *               │     ├── purchaseDate: String "yyyy/MM/dd"
 *               │     └── expireDate: String "yyyy/MM/dd"
 *               └── status/inventory/{medicineName}/[0|1|2]
 *
 * Status Codes:
 *   0 = Normal (Good)
 *   1 = Warning (Low Supply)
 *   2 = Alert (Expired)
 *
 * Core Features:
 *   - Retrieves all medicine records belonging to the selected child.
 *   - Automatically assigns status based on supply and expiration date.
 *   - Color-codes cards visually based on status severity.
 *   - Provides navigation to AddInventoryFragment for editing/adding medicine.
 *
 * Author: Kevin Li
 * Last Updated: November 18 2025
 */

public class InventoryFragment extends Fragment {

    // ───────────────────────────────
    // UI COMPONENTS
    // ───────────────────────────────
    private Toolbar toolbar;
    private CardView cardAddMedicine;
    private LinearLayout contentContainer;

    // ───────────────────────────────
    // FIREBASE REFERENCES
    // ───────────────────────────────
    private FirebaseDatabase db;
    private DatabaseReference medicineRef;

    // Hardcoded parent username for demonstration
    // (should later be replaced by logged-in parent’s username)
    private String name, uname;

    // ───────────────────────────────
    // LIFECYCLE METHODS
    // ───────────────────────────────

    /**
     * Called when the fragment is created.
     * Retrieves required arguments such as the child's username and name
     * from the passed navigation Bundle.
     *
     * @param savedInstanceState Previously saved instance state, if any.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve parent username passed as argument
        if (getArguments() != null) {
            name = getArguments().getString("childName");
            uname = getArguments().getString("childUname");
        }
    }

    /**
     * Inflates the fragment UI, initializes Firebase references,
     * configures the toolbar, and triggers the loading of medicine entries.
     *
     * @param inflater  LayoutInflater used to inflate XML UI.
     * @param container Optional parent view.
     * @param savedInstanceState Instance state if restored.
     * @return The fully constructed fragment view.
     */
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
        cardAddMedicine = view.findViewById(R.id.cardAddMedicine);

        // Initialize Firebase references
        db = FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/");
        medicineRef = db.getReference("categories/users/children/" + uname + "/inventory");

        // Load all medicine into the dashboard
        loadMedicineFromDatabase();

        // Handle "Add Medicine" button logic
        cardAddMedicine.setOnClickListener(v -> {
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
     * Loads all medicine records from Firebase and dynamically populates
     * the inventory list as UI CardViews. If no records exist, the user
     * is notified and only the "Add Medicine" button is shown.
     */
    private void loadMedicineFromDatabase() {
        medicineRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (contentContainer == null || getContext() == null) return;

                contentContainer.removeAllViews();

                if (!snapshot.exists()) {
                    Toast.makeText(getContext(), "No medicine found", Toast.LENGTH_SHORT).show();
                    contentContainer.addView(cardAddMedicine);
                    return;
                }

                for (DataSnapshot medSnapshot : snapshot.getChildren()) {
                    String medicineName = medSnapshot.getKey();

                    int currentAmount = medSnapshot.child("currentAmount").getValue(int.class);
                    int prescriptionAmount = medSnapshot.child("prescriptionAmount").getValue(int.class);
                    String purchaseDate = medSnapshot.child("purchaseDate").getValue(String.class);
                    String expireDate = medSnapshot.child("expireDate").getValue(String.class);

                    updateStatus(medicineName, currentAmount, prescriptionAmount, expireDate);

                    CardView card = addMedicineCard(medicineName, currentAmount, prescriptionAmount, purchaseDate, expireDate);
                    applyMedicineStatusColor(medicineName, card);
                }
                contentContainer.addView(cardAddMedicine);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load medicines: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    /**
     * Computes and updates the status for a specific medicine based on:
     *   1) Remaining supply amount (below 20% triggers warning)
     *   2) Expiration date (past date triggers alert)
     *
     * The computed status is written to Firebase under "status/inventory".
     *
     * @param medicineName Name of the medicine record.
     * @param currentAmount Current supply remaining.
     * @param prescriptionAmount Total prescribed supply amount.
     * @param expireDate Expiration date formatted as yyyy/MM/dd.
     */
    private void updateStatus(String medicineName, int currentAmount, int prescriptionAmount, String expireDate) {
        if (medicineName == null) return;
        DatabaseReference statusRef = FirebaseDatabase.getInstance()
                .getReference("categories/users/children")
                .child(uname)
                .child("status")
                .child("inventory")
                .child(medicineName);

        List<Integer> statusList = new ArrayList<>();

        // 1. Check low supply (below 20%)
        if (prescriptionAmount > 0) {
            double ratio = (double) currentAmount / prescriptionAmount;
            if (ratio < 0.2) {
                statusList.add(1); // Low Supply
            }
        }

        // 2. Check expiration
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());

            Date expDate = sdf.parse(expireDate);
            Date today = Calendar.getInstance().getTime();

            if (expDate != null && expDate.before(today)) {
                statusList.add(2); // Expired
            }
        } catch (Exception e) {
            Log.e("updateStatus", "⚠️ Date parse error for: " + expireDate);
        }

        // 3. Upload updated status list (empty means no issues)
        statusRef.setValue(statusList)
                .addOnSuccessListener(unused -> Log.d("updateStatus", "Status updated: " + medicineName))
                .addOnFailureListener(e -> Log.e("updateStatus", "Failed update: " + e.getMessage()));
    }


    /**
     * Applies UI color changes to a medicine card based on its computed status.
     * The card color changes based on:
     *   • Good (no issues)
     *   • Warning (low supply)
     *   • Alert (expired)
     *
     * @param medName Name of the medicine entry.
     * @param card The UI CardView representing the medicine.
     */
    private void applyMedicineStatusColor(String medName, CardView card) {
        DatabaseReference statusRef = FirebaseDatabase.getInstance()
                .getReference("categories/users/children")
                .child(uname)
                .child("status")
                .child("inventory")
                .child(medName);

        statusRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                int status = 0; // default: good

                for (DataSnapshot s : snapshot.getChildren()) {
                    Integer v = s.getValue(Integer.class);

                    if (v != null) {
                        if (v == 2) {
                            status = 2; // alert override
                            break;
                        } else if (v == 1 && status != 2) {
                            status = 1; // warning unless overridden
                        }
                    }
                }

                int good = getResources().getColor(R.color.good);
//                int warning = getResources().getColor(R.color.warning);
                int alert = getResources().getColor(R.color.alert);

                if (status == 2) {
                    card.setCardBackgroundColor(alert);
                } else if (status == 1) {
                    card.setCardBackgroundColor(alert);
                } else {
                    card.setCardBackgroundColor(good);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }



    // ───────────────────────────────
    // UI CONSTRUCTION HELPERS
    // ───────────────────────────────
    /**
     * Dynamically builds a CardView object containing medicine details.
     * The generated card includes clickable interaction to edit or update
     * the selected medicine entry.
     *
     * @param name Medicine name.
     * @param current Current remaining amount.
     * @param total Total prescribed amount.
     * @param purchaseDate Date purchased (yyyy/MM/dd).
     * @param expireDate Expiration date (yyyy/MM/dd).
     * @return The generated CardView representing a medication item.
     */
    @SuppressLint("ResourceType")
    private CardView  addMedicineCard(String name, int current, int total, String purchaseDate, String expireDate) {
        if (!isAdded() || getContext() == null) return null;
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

        // Medicine name (large)
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

        //Delete Icon
        ImageView deleteView = new ImageView(ctx);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(dpToPx(24), dpToPx(24));
        deleteView.setLayoutParams(deleteParams);
        deleteView.setImageResource(android.R.drawable.ic_delete);
        deleteView.setColorFilter(ContextCompat.getColor(ctx, R.color.gray), PorterDuff.Mode.SRC_IN);

        //handle delete logic
        deleteView.setOnClickListener(v -> {
            new AlertDialog.Builder(ctx)
                    .setTitle("Delete Medicine")
                    .setMessage("Are you sure you want to delete \"" + name + "\" from the inventory?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        medicineRef.child(name).removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(ctx, "Medicine removed", Toast.LENGTH_SHORT).show();
                                    loadMedicineFromDatabase(); // Refresh UI
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(ctx, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                                );
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        topRow.addView(nameView);
        topRow.addView(deleteView);
        topRow.addView(arrowView);

        String text;

        // ─────────────── Second row: Purchase Dates ───────────────
        TextView buyDateView = new TextView(ctx);
        text = "Purchased: " + purchaseDate;
        buyDateView.setText(text);
        buyDateView.setTextSize(14);
        buyDateView.setTextColor(Color.DKGRAY);
        buyDateView.setPadding(0, dpToPx(4), 0, 0);


        // ─────────────── Third row: Expire Dates ───────────────
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


        // Card click (show child details, future use)
        cardView.setOnClickListener(v ->{
                    Bundle args = new Bundle();
                    args.putString("childUname", uname);
                    args.putString("medicineName", name);
                    AddInventoryFragment addInvFrag = new AddInventoryFragment();
                    addInvFrag.setArguments(args);
                    loadFragment(addInvFrag);
                }

        );


        if (cardView.getParent() == null){
            contentContainer.addView(cardView);
            return cardView;
        }
        return null;
    }

    /**
     * Converts a value in device-independent pixels (dp) to raw pixel units.
     *
     * @param dp Value in density-independent pixels.
     * @return Integer representing the pixel conversion result.
     */
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
