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
import android.widget.Spinner;
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
 * Author: [Your Name]
 * Last Updated: [Date]
 */

public class LinkChildFragment extends Fragment {

    // ───────────────────────────────
    // UI Components
    // ───────────────────────────────
    private EditText editTextUname, editTextPassword;
    private Button buttonAdd;
    private Toolbar toolbar;

    // ───────────────────────────────
    // Firebase References
    // ───────────────────────────────
    private FirebaseDatabase db;
    private DatabaseReference childrenRef, parentRef;

    // ───────────────────────────────
    // Data
    // ───────────────────────────────
    private String parentUname;   // passed from previous fragment

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
        View view = inflater.inflate(R.layout.activity_link_child_fragment, container, false);

        // Initialize toolbar
        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        // Enable back navigation
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        // Initialize UI components
        editTextUname = view.findViewById(R.id.editTextUname);
        editTextPassword = view.findViewById(R.id.editTextPassword);
        buttonAdd = view.findViewById(R.id.buttonAdd);

        // Initialize Firebase
        db = FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/");

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
        String uname = editTextUname.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // Validate input fields
        if (uname.isEmpty() || password.isEmpty()) {
            Toast.makeText(getContext(), "Please enter both username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference childrenRef = FirebaseDatabase.getInstance()
                .getReference("categories/users/children");

        // Check if the entered child username exists
        childrenRef.child(uname).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Step 1: Validate child existence
                if (!snapshot.exists()) {
                    Toast.makeText(getContext(), "Child account not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Step 2: Convert snapshot to User object
                User child = snapshot.getValue(User.class);
                if (child == null) {
                    Toast.makeText(getContext(), "Invalid data for this user", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Step 3: Verify password
                if (!child.getPassword().equals(password)) {
                    Toast.makeText(getContext(), "Username and password do not match", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Step 4: Get parent's "children" node reference
                DatabaseReference parentChildrenRef = FirebaseDatabase.getInstance()
                        .getReference("categories/users/parents/" + parentUname + "/children");

                // Step 5: Check if this child is already linked
                parentChildrenRef.child(uname).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot existingChildSnapshot) {
                        if (existingChildSnapshot.exists()) {
                            Toast.makeText(getContext(), "This child is already linked", Toast.LENGTH_SHORT).show();
                        } else {
                            // Step 6: Link the child under parent's list
                            parentChildrenRef.child(uname).setValue(uname)
                                    .addOnCompleteListener(linkTask -> {
                                        if (linkTask.isSuccessful()) {
                                            Toast.makeText(getContext(), "Child linked successfully", Toast.LENGTH_SHORT).show();
                                            requireActivity().getSupportFragmentManager().popBackStack();
                                        } else {
                                            Toast.makeText(getContext(), "Failed to link child", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

