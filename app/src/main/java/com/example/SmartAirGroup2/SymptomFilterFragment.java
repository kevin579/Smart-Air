package com.example.SmartAirGroup2;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;


/**
 * SymptomFilterFragment
 * ------------------------
 * This fragment allows users to filter their symptom history based on:
 *  - Symptom keyword
 *  - Date range (start + end date)
 *  - Environmental triggers (exercise, smoke, dust, etc.)
 *
 * When filters are applied, a result bundle is sent back to the previous fragment using
 * Fragment Result API with key: "symptomFilter".
 *
 * The user may also reset all filters using the Reset option.
 *
 * Data is not stored in Firebase — filters only affect local rendering.
 *
 * Author: Kevin Li
 * Last Updated: Nov 19 2025
 */

public class SymptomFilterFragment extends Fragment {

    // ───────────────────────────────
    // UI Components
    // ───────────────────────────────
    private EditText editSymptom, editStartDate, editEndDate;

    private CheckBox checkExercise, checkColdAir, checkDust, checkAllergy,
            checkSmoke, checkIllness, checkPerfume, checkStress;

    private CardView cardApply, cardReset;
    private Toolbar toolbar;



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Inflate layout for this fragment
        View view = inflater.inflate(R.layout.activity_symptom_filter, container, false);

        // Initialize toolbar
        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);

        // Handle back navigation (up button)
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());


        // Initialize UI elements
        editSymptom = view.findViewById(R.id.editTextSymptom);
        editStartDate = view.findViewById(R.id.editStartTime);
        editEndDate = view.findViewById(R.id.editEndTime);

        checkExercise = view.findViewById(R.id.checkExercise);
        checkColdAir = view.findViewById(R.id.checkColdAir);
        checkDust = view.findViewById(R.id.checkDust);
        checkAllergy = view.findViewById(R.id.checkAllergy);
        checkSmoke = view.findViewById(R.id.checkSmoke);
        checkIllness = view.findViewById(R.id.checkIllness);
        checkPerfume = view.findViewById(R.id.checkPerfume);
        checkStress = view.findViewById(R.id.checkStress);

        cardApply = view.findViewById(R.id.cardApply);
        cardReset = view.findViewById(R.id.cardReset);

        setupDatePickers();

        setListeners();

        return view;
    }

    /**
     * Initializes the start and end date picker dialogs.
     * When either EditText is clicked, a DatePicker dialog is displayed.
     *
     * Selected date format: yyyy/MM/dd
     *
     * Helps modularize the onCreateView logic and keeps UI setup cleaner.
     */
    private void setupDatePickers() {

        Calendar calendar = Calendar.getInstance();

        View.OnClickListener listener = v -> {
            EditText target = (EditText) v;

            DatePickerDialog dialog = new DatePickerDialog(
                    getContext(),
                    (view, year, month, dayOfMonth) ->
                            target.setText(year + "/" + (month + 1) + "/" + dayOfMonth),
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );

            dialog.show();
        };

        editStartDate.setOnClickListener(listener);
        editEndDate.setOnClickListener(listener);
    }

    /**
     * Attaches event listeners to user interaction components:
     *
     * 1. Apply Filter:
     *     - Collects user inputs (symptom text, date range, triggers).
     *     - Packages them into a Bundle.
     *     - Sends result via Fragment Result API.
     *     - Navigates back to previous screen.
     *
     * 2. Reset:
     *     - Clears all fields and unchecks all triggers.
     *     - Displays a Toast confirmation.
     *
     * This structure separates UI wiring from lifecycle logic and improves readability.
     */
    private void setListeners() {

        // APPLY BUTTON
        cardApply.setOnClickListener(v -> {

            String selectedSymptom = editSymptom.getText().toString().trim();
            String startDate = editStartDate.getText().toString().trim();
            String endDate = editEndDate.getText().toString().trim();

            List<String> selectedTriggers = new ArrayList<>();

            if (checkExercise.isChecked()) selectedTriggers.add("Exercise");
            if (checkColdAir.isChecked()) selectedTriggers.add("Cold Air");
            if (checkDust.isChecked()) selectedTriggers.add("Dust");
            if (checkAllergy.isChecked()) selectedTriggers.add("Allergy");
            if (checkSmoke.isChecked()) selectedTriggers.add("Smoke");
            if (checkIllness.isChecked()) selectedTriggers.add("IllNess");
            if (checkPerfume.isChecked()) selectedTriggers.add("Perfume");
            if (checkStress.isChecked()) selectedTriggers.add("Stress");


            Bundle result = new Bundle();
            result.putString("filter_symptom", selectedSymptom);
            result.putString("filter_start_date", startDate);
            result.putString("filter_end_date", endDate);
            result.putStringArrayList("filter_triggers", new ArrayList<>(selectedTriggers));


            getParentFragmentManager().setFragmentResult("symptomFilter", result);

            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        });


        // RESET BUTTON
        cardReset.setOnClickListener(v -> {

            editSymptom.setText("");
            editStartDate.setText("");
            editEndDate.setText("");

            List<CheckBox> allChecks = Arrays.asList(
                    checkExercise, checkColdAir, checkDust, checkAllergy, checkSmoke,
                    checkIllness, checkPerfume, checkStress
            );

            for (CheckBox box : allChecks) box.setChecked(false);

            Toast.makeText(getContext(), "Filters cleared", Toast.LENGTH_SHORT).show();
        });
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
