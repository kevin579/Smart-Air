package com.example.SmartAirGroup2;

import android.app.TimePickerDialog;
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
 * AddSymptomFragment
 * ------------------------------------------------
 * This fragment allows a user (parent or child, depending on account type)
 * to record a new symptom entry associated with a child profile in the
 * Firebase Realtime Database.
 *
 * Functionality:
 * --------------
 * • User enters a symptom description.
 * • User selects one or more triggers from predefined checkboxes.
 * • User selects a timestamp using a combined DatePicker and TimePicker.
 * • Input validation ensures all required information is provided.
 * • Upon successful submission, the symptom is stored under:
 *      categories/users/children/{username}/data/symptoms
 *
 * Timestamp Format:
 * -----------------
 * yyyy/MM/dd HH:mm
 * (Example: 2025/11/17 13:42)
 *
 * Validation Rules:
 * -----------------
 * - Symptom text cannot be empty.
 * - Timestamp must be selected.
 * - At least one trigger must be selected.
 *
 * UI Behavior:
 * ------------
 * - The toolbar supports back navigation.
 * - After successful submission, the fragment navigates back automatically.
 *
 * Firebase Storage Example:
 * -------------------------
 * {
 *   "symptom": "Coughing",
 *   "time": "2025/11/17 14:05",
 *   "triggers": "Exercise, Cold Air",
 *   "type": "parent"
 * }
 *
 * Arguments Required:
 * -------------------
 * - "childUname" (String): The username of the child profile the symptom belongs to.
 *
 * Author: Your Name
 * Last Modified: Nov 17, 2025
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
    // Data
    // ───────────────────────────────
    private String uname, author;

    // ───────────────────────────────
    // Lifecycle: Fragment Creation
    // ───────────────────────────────
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve parent username passed as argument
        if (getArguments() != null) {
            uname = getArguments().getString("childUname");
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
        View view = inflater.inflate(R.layout.fragment_add_symptom_fragment, container, false);

        // Initialize toolbar
        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);

        // Handle back navigation (up button)
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

//        SharedPreferences prefs = requireContext().getSharedPreferences("APP_DATA", Context.MODE_PRIVATE);
//        author = prefs.getString("type", "null");

        author = CurrentUser.get().getType();


        // Initialize UI elements
        editTextSymptom = view.findViewById(R.id.editTextSymptom);
        editTime = view.findViewById(R.id.editTime);
        setupCheckboxes(view);

        editTime.setOnClickListener(v -> {

            Calendar calendar = Calendar.getInstance(); // Default: today

            TimePickerDialog timePicker = new TimePickerDialog(
                    getContext(),
                    (timePickerView, hourOfDay, minute) -> {

                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        calendar.set(Calendar.MINUTE, minute);

                        // Format including today’s date + chosen time
                        SimpleDateFormat sdf =
                                new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
                        editTime.setText(sdf.format(calendar.getTime()));
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
            );

            timePicker.show();
        });



        buttonAdd = view.findViewById(R.id.buttonAdd);
        // Add button listener
        buttonAdd.setOnClickListener(v -> addSymptom(view));

        return view;
    }


    // ───────────────────────────────
    // Main Logic: Add Symptom
    // ───────────────────────────────
    /**
     * Extracts the values input by the user (symptom, time, and triggers),
     * validates the fields, and uploads the new symptom record to Firebase.
     *
     * Required fields:
     *  - Symptom name
     *  - Date and time
     *  - At least one selected trigger
     *
     * On successful write, the fragment closes and returns to the previous screen.
     *
     * @param view The root view of the fragment, used for reference if needed.
     */
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


        Map<String, Object> symptomData = new HashMap<>();
        symptomData.put("symptom", symptom);
        symptomData.put("time", time);
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

    /**
     * Sets up all predefined trigger checkbox UI references from the layout.
     * This method simplifies initialization and keeps onCreateView cleaner.
     *
     * @param view The root inflated layout containing the checkbox views.
     */
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

    /**
     * Collects all user-selected trigger checkboxes and returns them as a
     * comma-separated string.
     *
     * Example output:
     *     "Exercise, Smoke, Illness"
     *
     * If no triggers are selected, returns an empty string.
     *
     * @return A formatted trigger list to store in Firebase.
     */
    private String getSelectedTriggers() {
        List<String> selected = new ArrayList<>();

        if (checkExercise.isChecked()) selected.add("Exercise");
        if (checkColdAir.isChecked()) selected.add("Cold Air");
        if (checkDust.isChecked()) selected.add("Dust");
        if (checkAllergy.isChecked()) selected.add("Allergy");
        if (checkSmoke.isChecked()) selected.add("Smoke");
        if (checkIllness.isChecked()) selected.add("Illness");
        if (checkPerfume.isChecked()) selected.add("Perfume");
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

}
