package com.example.SmartAirGroup2;

import android.content.Context;
import android.content.SharedPreferences;
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


import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


/**
 * AddChildFragment
 * ---------------------
 * This fragment allows a parent user to create a new child account in Firebase Realtime Database,
 * and automatically links that child account to the parent’s account.
 *
 * Workflow:
 *  1. Parent enters the child’s username, name, email, and password (twice).
 *  2. The app validates inputs (non-empty, password match, min length).
 *  3. It checks if the username already exists across all user categories.
 *  4. If available, the child is added to the "children" node.
 *  5. The child’s username is then linked under the parent’s "children" list.
 *
 * Author: Kevin Li
 * Last Updated: November 18, 2025
 */

public class AddChildFragment extends Fragment {

    // ───────────────────────────────
    // UI Components
    // ───────────────────────────────
    private EditText editTextUname, editTextName, editTextEmail, editTextPassword, editTextPassword2;
    private Button buttonAdd;
    private Toolbar toolbar;

    // ───────────────────────────────
    // Firebase References
    // ───────────────────────────────
    private FirebaseDatabase db;
    private DatabaseReference childrenRef;

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

        // Retrieve parent username passed as argument
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

        // Inflate layout for this fragment
        View view = inflater.inflate(R.layout.activity_add_child_fragment, container, false);

        // Initialize toolbar
        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);

        // Handle back navigation (up button)
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        // Initialize UI elements
        editTextUname = view.findViewById(R.id.editTextUname);
        editTextName = view.findViewById(R.id.editTextName);
        editTextEmail = view.findViewById(R.id.editTextEmail);
        editTextPassword = view.findViewById(R.id.editTextPassword);
        editTextPassword2 = view.findViewById(R.id.editTextPassword2);
        buttonAdd = view.findViewById(R.id.buttonAdd);

        // Initialize Firebase instance
        db = FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/");

        // Add button listener
        buttonAdd.setOnClickListener(v -> addChild());

        return view;
    }

    // ───────────────────────────────
    // Toolbar Menu Setup
    // ───────────────────────────────
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu, menu);

        // Tint menu icons white
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.getIcon() != null) {
                item.getIcon().setTint(getResources().getColor(android.R.color.white));
            }
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    // ───────────────────────────────
    // Main Logic: Add Child
    // ───────────────────────────────
    private void addChild() {
        // Collect input values
        String uname = editTextUname.getText().toString().trim();
        String name = editTextName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String password2 = editTextPassword2.getText().toString().trim();

        // Validate fields
        if (uname.isEmpty() || password.isEmpty() || password2.isEmpty()) {
            Toast.makeText(getContext(), "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 8) {
            Toast.makeText(getContext(), "Password must be at least 8 characters long", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(password2)) {
            Toast.makeText(getContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if username already exists before creating
        checkIfUsernameExists(uname,
                () -> {
                    // Username is available
                    User user = new User(uname, name, email, password, "child");
                    addUserToDatabase(user);
                },
                () -> {
                    // Username already taken
                    Toast.makeText(getContext(), "Username already exists", Toast.LENGTH_SHORT).show();
                }
        );
    }

    // ───────────────────────────────
    // Firebase: Add New Child User
    // ───────────────────────────────
    private void addUserToDatabase(User user) {
        childrenRef = db.getReference("categories/users/children");

        // Store child under "children" node
        childrenRef.child(user.getUname()).setValue(user).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Child account created", Toast.LENGTH_SHORT).show();

                // Also link the child to the parent’s "children" list
                DatabaseReference parentChildrenRef = db.getReference(
                        "categories/users/parents/" + parentUname + "/children");

                parentChildrenRef.child(user.getUname()).setValue(user.getUname())
                        .addOnCompleteListener(linkTask -> {
                            if (linkTask.isSuccessful()) {
                                Toast.makeText(getContext(), "Linked child to parent", Toast.LENGTH_SHORT).show();
                                SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString("current_child_username", user.getUname());
                                editor.apply();
                                requireActivity().getSupportFragmentManager().popBackStack();
                            } else {
                                Toast.makeText(getContext(), "Failed to link child", Toast.LENGTH_SHORT).show();
                            }
                        });

            } else {
                Toast.makeText(getContext(), "Failed to create child account", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ───────────────────────────────
    // Firebase: Check Username Availability
    // ───────────────────────────────
    private void checkIfUsernameExists(String uname, Runnable onAvailable, Runnable onTaken) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance()
                .getReference("categories/users");

        // Check in all three user categories
        Task<DataSnapshot> parentsTask = usersRef.child("parents").child(uname).get();
        Task<DataSnapshot> childrenTask = usersRef.child("children").child(uname).get();
        Task<DataSnapshot> providersTask = usersRef.child("providers").child(uname).get();

        // Wait until all three checks complete
        Tasks.whenAllComplete(parentsTask, childrenTask, providersTask)
                .addOnCompleteListener(task -> {
                    boolean exists =
                            (parentsTask.getResult() != null && parentsTask.getResult().exists()) ||
                                    (childrenTask.getResult() != null && childrenTask.getResult().exists()) ||
                                    (providersTask.getResult() != null && providersTask.getResult().exists());

                    if (exists) onTaken.run();
                    else onAvailable.run();
                });
    }
}
