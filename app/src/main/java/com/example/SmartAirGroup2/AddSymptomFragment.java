package com.example.SmartAirGroup2;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


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
 * Author: [Your Name]
 * Last Updated: [Date]
 */

public class AddSymptomFragment extends Fragment {

    // ───────────────────────────────
    // UI Components
    // ───────────────────────────────
    private EditText editTextSymptom, editTime;
    private CheckBox checkExercise, checkColdAir, checkDust, checkAllergy, checkSmoke, checkIllness, checkPerfume, checkStress;
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
    private String uname, name, author;

    // ───────────────────────────────
    // Lifecycle: Fragment Creation
    // ───────────────────────────────
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve parent username passed as argument
        if (getArguments() != null) {
            uname = getArguments().getString("childUname");
            name = getArguments().getString("childName");
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
        View view = inflater.inflate(R.layout.activity_add_symptom_fragment, container, false);

        // Initialize toolbar
        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);

        // Handle back navigation (up button)
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        SharedPreferences prefs = requireContext().getSharedPreferences("APP_DATA", Context.MODE_PRIVATE);
        author = prefs.getString("type", "null");

        // Initialize UI elements
        editTextSymptom = view.findViewById(R.id.editTextSymptom);
        editTime = view.findViewById(R.id.editTime);
        setupCheckboxes(view);

        editTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            TimePickerDialog dialog = new TimePickerDialog(
                    getContext(),
                    (timePicker, h, m) -> {
                        String formatted = String.format(Locale.getDefault(), "%02d:%02d", h, m);
                        editTime.setText(formatted);
                    },
                    hour,
                    minute,
                    true
            );
            dialog.show();
        });

        buttonAdd = view.findViewById(R.id.buttonAdd);

        // Initialize Firebase instance
        db = FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/");

        // Add button listener
        buttonAdd.setOnClickListener(v -> addSymptom(view));
        Toast.makeText(getContext(), author, Toast.LENGTH_SHORT).show();

        return view;
    }


    // ───────────────────────────────
    // Main Logic: Add Symptom
    // ───────────────────────────────
    private void addSymptom(View view) {
        // Collect input values
        String symptom = editTextSymptom.getText().toString().trim();

        String time = editTime.getText().toString().trim();

        String triggers = getSelectedTriggers();

        if (symptom.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a symptom", Toast.LENGTH_SHORT).show();
            return;
        }

        if (time.isEmpty()) {
            Toast.makeText(getContext(), "Please select a time", Toast.LENGTH_SHORT).show();
            return;
        }

        if (triggers.isEmpty()) {
            Toast.makeText(getContext(), "Please select at least one trigger", Toast.LENGTH_SHORT).show();
            return;
        }

        String date = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                .format(Calendar.getInstance().getTime());

        String finalTimestamp = date + "/" + time;


        Map<String, Object> symptomData = new HashMap<>();
        symptomData.put("symptom", symptom);
        symptomData.put("time", finalTimestamp);
        symptomData.put("triggers", triggers);
        symptomData.put("type", author);

        // Firebase path
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("categories/users/children")
                .child(uname)
                .child("data")
                .child("symptoms");

        // store
        ref.push().setValue(symptomData)
                .addOnSuccessListener(a -> {
                    requireActivity().onBackPressed();

                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error saving data", Toast.LENGTH_SHORT).show()
                );
    }
    private void setupCheckboxes(View view) {
        checkExercise = view.findViewById(R.id.checkExercise);
        checkColdAir = view.findViewById(R.id.checkColdAir);
        checkDust = view.findViewById(R.id.checkDust);
        checkAllergy = view.findViewById(R.id.checkAllergy);
        checkSmoke = view.findViewById(R.id.checkSmoke);
        checkIllness = view.findViewById(R.id.checkIllness);
        checkPerfume = view.findViewById(R.id.checkPerfume);
        checkStress = view.findViewById(R.id.checkStress);
    }

    // Get selected triggers
    private String getSelectedTriggers() {
        List<String> selected = new ArrayList<>();

        if (checkExercise.isChecked()) selected.add("Exercise");
        if (checkColdAir.isChecked()) selected.add("Cold Air");
        if (checkDust.isChecked()) selected.add("Dust");
        if (checkAllergy.isChecked()) selected.add("Allergy");
        if (checkSmoke.isChecked()) selected.add("Smoke");
        if (checkIllness.isChecked()) selected.add("Illness");
        if (checkPerfume.isChecked()) selected.add("Perfume / Strong Odor");
        if (checkStress.isChecked()) selected.add("Stress");

        // Convert list -> comma separated string (e.g., "Exercise, Smoke, Stress")
        return TextUtils.join(", ", selected);
    }

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


}
