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

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class LinkChildFragment extends Fragment {
    private EditText editTextUname,  editTextPassword;
    private Spinner spinnerCategory;
    private Button buttonAdd;

    private Toolbar toolbar;

    private FirebaseDatabase db;
    private DatabaseReference childrenRef, parentRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_link_child_fragment, container, false);

        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        toolbar.setNavigationOnClickListener(v -> {
            getParentFragmentManager().popBackStack();
        });

        editTextUname = view.findViewById(R.id.editTextUname);

        editTextPassword = view.findViewById(R.id.editTextPassword);
        buttonAdd = view.findViewById(R.id.buttonAdd);

        db = FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/");


        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                link();

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

    private void link() {
        String uname = editTextUname.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

    }
}
