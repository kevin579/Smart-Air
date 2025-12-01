package com.example.SmartAirGroup2;

import android.app.DatePickerDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link TriageFilterFragment# newInstance} factory method to
 * create an instance of this fragment.
 */
public class TriageFilterFragment extends Fragment {

    // ───────────────────────────────
    // UI Components
    // ───────────────────────────────
    private EditText editRedflag, editStartDate, editEndDate;

    private CheckBox checkSpeakingIssue, checkBreathingIssue, checkChestIssue, checkLipIssue,
            checkNailIssue;

    private CardView cardApply, cardReset;
    private Toolbar toolbar;



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Inflate layout for this fragment
        View view = inflater.inflate(R.layout.activity_triage_filter, container, false);

        // Initialize toolbar
        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);

        // Handle back navigation (up button)
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());


        // Initialize UI elements
        editRedflag = view.findViewById(R.id.editTextRedflag);
        editStartDate = view.findViewById(R.id.editStartTime);
        editEndDate = view.findViewById(R.id.editEndTime);

        checkSpeakingIssue = view.findViewById(R.id.speakingIssue);
        checkBreathingIssue = view.findViewById(R.id.breathingIssue);
        checkChestIssue = view.findViewById(R.id.chestIssue);
        checkLipIssue = view.findViewById(R.id.lipIssue);
        checkNailIssue = view.findViewById(R.id.nailIssue);

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

            String selectedSymptom = editRedflag.getText().toString().trim();
            String startDate = editStartDate.getText().toString().trim();
            String endDate = editEndDate.getText().toString().trim();

            List<String> selectedTriggers = new ArrayList<>();

            if (checkSpeakingIssue.isChecked()) selectedTriggers.add("Trouble speaking");
            if (checkBreathingIssue.isChecked()) selectedTriggers.add("Trouble breathing");
            if (checkChestIssue.isChecked()) selectedTriggers.add("Chest Pulling or Retraction");
            if (checkLipIssue.isChecked()) selectedTriggers.add("Grey or Blue lips");
            if (checkNailIssue.isChecked()) selectedTriggers.add("Grey or Blue nails");


            Bundle result = new Bundle();
            result.putString("filter_triage_field", selectedSymptom); // match listener
            result.putString("filter_start_date", startDate);
            result.putString("filter_end_date", endDate);
            result.putStringArrayList("filter_triggers", new ArrayList<>(selectedTriggers));
            getParentFragmentManager().setFragmentResult("triageFilter", result); // match listener


            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        });


        // RESET BUTTON
        cardReset.setOnClickListener(v -> {

            editRedflag.setText("");
            editStartDate.setText("");
            editEndDate.setText("");

            List<CheckBox> allChecks = Arrays.asList(
                    checkSpeakingIssue, checkBreathingIssue, checkChestIssue, checkLipIssue,
                    checkNailIssue
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