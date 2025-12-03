package com.example.SmartAirGroup2.ParentDashboard;

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

import com.example.SmartAirGroup2.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;



/**
 * A fragment that allows a parent user to link an existing provider account to their profile.
 * <p>
 * This screen provides a user interface for a parent to enter the username of a provider.
 * Upon submission, the fragment updates the Firebase Realtime Database to establish a
 * two-way link:
 * <ul>
 *     <li>The provider's username is added to the parent's list of providers.</li>
 *     <li>The parent's username is added to the provider's list of parents.</li>
 * </ul>
 * The parent's username is passed to this fragment via arguments.
 *
 * @author Your Name/Team Name
 * @version 1.0
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
    // Main Logic: Link Provider
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

    /**
     * Updates the provider's record in the Firebase Realtime Database to add a reference
     * to the current parent.
     * <p>
     * This method constructs a {@link DatabaseReference} pointing to the provider's `parents`
     * list (based on the `providerUname` entered by the user). It then adds the current
     * parent's username (`parentUname`) as a new child in that list. This establishes the
     * second half of the two-way link between the parent and the provider.
     * <p>
     * A {@link Toast} message is displayed to the user if the database update fails.
     */
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


    /**
     * Updates the Firebase Realtime Database to link the specified provider to the current parent.
     * <p>
     * This method retrieves a reference to the current parent's "providers" list in the database
     * and adds the provider's username as a new child. The key and value of the new child are both
     * set to the provider's username. It handles both success and failure cases by displaying
     * appropriate toast messages to the user.
     */
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

