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

public class ChildDashboardFragment extends Fragment {

    // ───────────────────────────────
    // UI COMPONENTS
    // ───────────────────────────────
    private Toolbar toolbar;
    private CardView cardInventory, cardPEF;

    // ───────────────────────────────
    // FIREBASE REFERENCES
    // ───────────────────────────────
    private FirebaseDatabase db;
    private DatabaseReference childrenRef;

    // Hardcoded parent username for demonstration
    // (should later be replaced by logged-in parent’s username)
    private String name, uname;

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
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_child_dashboard, container, false);

        // Toolbar setup
        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        setHasOptionsMenu(true);

        toolbar.setTitle(name +"'s Dashboard");

        // Handle back navigation (up button)
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        // UI references
        cardPEF = view.findViewById(R.id.cardPEF);
        cardInventory = view.findViewById(R.id.cardInventory);



//
//        cardPEF.setOnClickListener(v -> {
//            new AlertDialog.Builder(requireContext())
//                    .setTitle("Confirm Action")
//                    .setMessage("Does your child already have an account?")
//                    .setPositiveButton("Yes", (dialog, which) -> {
//                        // Navigate to link-existing-child fragment
//                        LinkChildFragment linkFrag = new LinkChildFragment();
//                        Bundle args = new Bundle();
//                        args.putString("parentUname", uname);
//                        linkFrag.setArguments(args);
//                        loadFragment(linkFrag);
//                    })
//                    .setNegativeButton("No", (dialog, which) -> {
//                        // Navigate to create-new-child fragment
//                        AddChildFragment addFrag = new AddChildFragment();
//                        Bundle args = new Bundle();
//                        args.putString("parentUname", uname);
//                        addFrag.setArguments(args);
//                        loadFragment(addFrag);
//                        dialog.dismiss();
//                    })
//                    .show();
//        });

        cardInventory.setOnClickListener(v -> {
            InventoryFragment invFrag = new InventoryFragment();
            Bundle args = new Bundle();
            args.putString("childUname", uname);
            args.putString("childName", name);
            invFrag.setArguments(args);
            loadFragment(invFrag);
        });

        return view;
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
