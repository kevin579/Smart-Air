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


import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class AddChildFragment extends Fragment {
    private EditText editTextUname, editTextName, editTextEmail, editTextPassword, editTextPassword2, editTextDescription;
    private Spinner spinnerCategory;
    private Button buttonAdd;

    private Toolbar toolbar;

    private FirebaseDatabase db;
    private DatabaseReference childrenRef, parentRef;

    private String parentUname;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            parentUname = getArguments().getString("parentUname");
        }
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_add_child_fragment, container, false);

        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        toolbar.setNavigationOnClickListener(v -> {
            getParentFragmentManager().popBackStack();
        });

        editTextUname = view.findViewById(R.id.editTextUname);
        editTextName = view.findViewById(R.id.editTextName);
        editTextEmail = view.findViewById(R.id.editTextEmail);
        editTextPassword = view.findViewById(R.id.editTextPassword);
        editTextPassword2 = view.findViewById(R.id.editTextPassword2);
        buttonAdd = view.findViewById(R.id.buttonAdd);

        db = FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/");


        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addChild    ();

            }
        });

        return view;
    }
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu, menu);

        // Tint icons white
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.getIcon() != null) {
                item.getIcon().setTint(getResources().getColor(android.R.color.white));
            }
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    private void addChild() {
        String uname = editTextUname.getText().toString().trim();
        String name = editTextName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String password2 = editTextPassword2.getText().toString().trim();

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

        checkIfUsernameExists(uname,
                () -> {
                    // Username is available — continue registration
                    Toast.makeText(getContext(), "Username available", Toast.LENGTH_SHORT).show();
                    User user = new User(uname, name, email, password,"child");
                    addUserToDatabase(user);
                },
                () -> {
                    // Username already exists — show message
                    Toast.makeText(getContext(), "Username already exists", Toast.LENGTH_SHORT).show();
                }
        );

    }

    private void addUserToDatabase(User user){
        childrenRef = db.getReference("categories/users/children");

        childrenRef.child(user.getUname()).setValue(user).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Item added", Toast.LENGTH_SHORT).show();

                DatabaseReference parentChildrenRef = db.getReference("categories/users/parents/" + parentUname + "/children");

                // Add child ID under parent's children list
                parentChildrenRef.child(user.getUname()).setValue(user.getUname()).addOnCompleteListener(linkTask -> {
                    if (linkTask.isSuccessful()) {
                        Toast.makeText(getContext(), "Linked child to parent", Toast.LENGTH_SHORT).show();
                        requireActivity().getSupportFragmentManager().popBackStack();
                    } else {
                        Toast.makeText(getContext(), "Failed to link child", Toast.LENGTH_SHORT).show();
                    }
                });

            } else {
                Toast.makeText(getContext(), "Failed to add item", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkIfUsernameExists(String uname, Runnable onAvailable, Runnable onTaken) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("categories/users");

        // Check in parents, children, and providers
        Task<DataSnapshot> parentsTask = usersRef.child("parents").child(uname).get();
        Task<DataSnapshot> childrenTask = usersRef.child("children").child(uname).get();
        Task<DataSnapshot> providersTask = usersRef.child("providers").child(uname).get();

        Tasks.whenAllComplete(parentsTask, childrenTask, providersTask)
                .addOnCompleteListener(task -> {
                    boolean exists =
                            (parentsTask.getResult() != null && parentsTask.getResult().exists()) ||
                                    (childrenTask.getResult() != null && childrenTask.getResult().exists()) ||
                                    (providersTask.getResult() != null && providersTask.getResult().exists());

                    if (exists) {
                        // username found in at least one node
                        onTaken.run();
                    } else {
                        // username available
                        onAvailable.run();
                    }
                });
    }
}
