package com.example.SmartAirGroup2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * LinkChildFragment
 * ---------------------
 * This fragment allows a parent user to link an existing child account
 * (already registered in Firebase) to their own parent profile.
 *
 * Workflow:
 *  1. The parent enters the child’s username and password.
 *  2. The app validates the input fields.
 *  3. The system checks Firebase to confirm that the child account exists
 *     and that the password matches.
 *  4. If validation passes and the child isn’t already linked,
 *     the app adds the child’s username to the parent’s “children” list.
 *
 * Author: Kevin Li
 * Last Updated: Nov 14, 2025
 */

public class LinkProviderFragment extends Fragment {

    // ───────────────────────────────
    // UI Components
    // ───────────────────────────────
    private EditText editTextUname;
    private Button buttonAdd;
    private Toolbar toolbar;


    // ───────────────────────────────
    // Data
    // ───────────────────────────────
    private String parentUname,providerUname;   // passed from previous fragment

    // ───────────────────────────────
    // Lifecycle: Fragment Creation
    // ───────────────────────────────
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve parent username argument
        if (getArguments() != null) {
            parentUname = getArguments().getString("parentUname");
        }
    }

    // ───────────────────────────────
    // Lifecycle: View Creation
    // ───────────────────────────────
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_link_provider_fragment, container, false);

        // Initialize toolbar
        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        // Enable back navigation
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        // Initialize UI components
        editTextUname = view.findViewById(R.id.editTextUname);
        buttonAdd = view.findViewById(R.id.buttonAdd);

        // Add click listener for linking operation
        buttonAdd.setOnClickListener(v -> link());

        return view;
    }

    // ───────────────────────────────
    // Toolbar Menu Setup
    // ───────────────────────────────
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu, menu);

        // Tint all menu icons white for visibility
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.getIcon() != null) {
                item.getIcon().setTint(getResources().getColor(android.R.color.white));
            }
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    // ───────────────────────────────
    // Main Logic: Link Existing Child
    // ───────────────────────────────
    private void link() {
        providerUname = editTextUname.getText().toString().trim();

        // Validate input fields
        if (providerUname.isEmpty() ) {
            Toast.makeText(getContext(), "Please enter username", Toast.LENGTH_SHORT).show();
            return;
        }

        updateParentDB();
        updateProviderDB();
    }

    private void updateProviderDB() {
        DatabaseReference providerRef = FirebaseDatabase.getInstance()
                .getReference("categories/users/provider/" + providerUname + "/parents");

        providerRef.child(parentUname).setValue(parentUname)
                .addOnSuccessListener(aVoid -> {

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error updating Provider link: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }


    private void updateParentDB() {
        DatabaseReference parentRef = FirebaseDatabase.getInstance()
                .getReference("categories/users/parents/" + parentUname + "/providers");

        parentRef.child(providerUname).setValue(providerUname)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Linking to " + providerUname + " complete.", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error updating Parent link: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}

