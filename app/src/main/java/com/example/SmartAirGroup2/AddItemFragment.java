package com.example.SmartAirGroup2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.example.SmartAirGroup2.models.User;


public class AddItemFragment extends Fragment {
    private EditText editTextTitle, editTextAuthor, editTextGenre, editTextDescription;
    private Spinner spinnerCategory;
    private Button buttonAdd;

    private FirebaseDatabase db;
    private DatabaseReference itemsRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_item, container, false);

        editTextTitle = view.findViewById(R.id.editTextTitle);
        editTextAuthor = view.findViewById(R.id.editTextAuthor);
        editTextGenre = view.findViewById(R.id.editTextGenre);
        editTextDescription = view.findViewById(R.id.editTextDescription);
        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        buttonAdd = view.findViewById(R.id.buttonAdd);

        db = FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/");

        // Set up the spinner with categories
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.categories_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addItem();
            }
        });

        return view;
    }


    private void addItem() {
        String uname = editTextTitle.getText().toString().trim(); // change variable name for clarity
        String email = editTextAuthor.getText().toString().trim();
        String password = editTextGenre.getText().toString().trim();

        if (uname.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(getContext(), "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        itemsRef = db.getReference("categories/users");
        String id = itemsRef.push().getKey();

        User user = new User("a", "a", "a","a","a");
//
        itemsRef.child(id).setValue(user).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Item added", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to add item", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
