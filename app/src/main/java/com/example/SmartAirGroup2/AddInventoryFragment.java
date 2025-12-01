package com.example.SmartAirGroup2;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
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

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * AddInventoryFragment
 * -------------------------------
 * This fragment allows users to create or update medicine inventory records
 * for a selected child user in Firebase Realtime Database.
 *
 * Behavior:
 * - If no medicine name is passed via arguments → the fragment functions in **Add Mode**
 *   and displays a blank form for new input.
 * - If a medicine name is provided via arguments → the fragment functions in **Update Mode**
 *   and pre-fills the fields with existing information (loaded externally before navigation).
 *
 * Core Features:
 *   • Add a new medicine to a child's inventory.
 *   • Update an existing medicine entry (same database path used).
 *   • Input validation: positive doses, chronological dates, required fields.
 *   • Automatically formats and stores dates in `yyyy/MM/dd`.
 *   • Uses Firebase Realtime Database under:
 *
 * Firebase Path:
 * categories/users/children/{childUsername}/inventory/{medicineName}
 *
 * Stored Fields:
 *   - purchaseDate: String (formatted yyyy/MM/dd)
 *   - expireDate: String (formatted yyyy/MM/dd)
 *   - prescriptionAmount: int (required, >0)
 *   - currentAmount: int (≤ prescriptionAmount)
 *
 * UI Contains:
 *   - Text input fields for medicine name (add mode only)
 *   - Purchase date picker
 *   - Expiration date picker
 *   - Prescription and current quantity fields
 *   - Add/Save button
 *
 * Author: Kevin Li
 * Last Updated: November 18, 2025
 */
public class AddInventoryFragment extends Fragment {

    // -------------------------------------------------------------------------
    // UI COMPONENTS
    // -------------------------------------------------------------------------
    private EditText editTextName, editTextPurchaseDate, editTextExpireDate,
            editTextPrescriptionAmount, editTextCurrentAmount;
    private Button buttonAdd;
    private Toolbar toolbar;

    // -------------------------------------------------------------------------
    // FIREBASE
    // -------------------------------------------------------------------------
    private FirebaseDatabase db;
    private DatabaseReference childrenRef;

    // -------------------------------------------------------------------------
    // ARGUMENT DATA
    // -------------------------------------------------------------------------
    private String uname;      // child username (required)
    private String medicine;   // medicine name (null → add mode, otherwise update mode)


    // -------------------------------------------------------------------------
    // LIFECYCLE: onCreate
    // -------------------------------------------------------------------------
    /**
     * Retrieves the arguments passed to this fragment:
     *   - childUname: identifies database path
     *   - medicineName: if present, fragment runs in update mode.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            uname = getArguments().getString("childUname");
            medicine = getArguments().getString("medicineName");
        }
    }


    // -------------------------------------------------------------------------
    // LIFECYCLE: onCreateView
    // -------------------------------------------------------------------------
    /**
     * Inflates the UI layout depending on mode (Add vs Update),
     * initializes form input listeners and toolbar configuration,
     * and prepares Firebase reference.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view;

        if (medicine != null) {
            view = inflater.inflate(R.layout.activity_update_medicine_fragment, container, false);
            toolbar = view.findViewById(R.id.toolbar);
            ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
            toolbar.setTitle("Update Medicine");
        } else {
            view = inflater.inflate(R.layout.fragment_add_medicine_fragment, container, false);
            toolbar = view.findViewById(R.id.toolbar);
            ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
            toolbar.setTitle("Add Medicine");
        }

        // Toolbar navigation
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        // UI initialization
        if (medicine == null) {
            editTextName = view.findViewById(R.id.editTextName);
        }

        editTextPurchaseDate = view.findViewById(R.id.editTextPurchaseDate);
        editTextExpireDate = view.findViewById(R.id.editTextExpireDate);
        editTextPrescriptionAmount = view.findViewById(R.id.editTextPrescriptionAmount);
        editTextCurrentAmount = view.findViewById(R.id.editTextCurrentAmount);
        buttonAdd = view.findViewById(R.id.buttonAdd);

        editTextPurchaseDate.setOnClickListener(v -> showDatePickerDialog(editTextPurchaseDate));
        editTextExpireDate.setOnClickListener(v -> showDatePickerDialog(editTextExpireDate));

        db = FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/");

        buttonAdd.setOnClickListener(v -> addMedicine());

        return view;
    }


    // -------------------------------------------------------------------------
    // SUPPORT: Date Picker
    // -------------------------------------------------------------------------
    /**
     * Opens a date picker dialog and assigns the selected date
     * to the targeted EditText in `yyyy/MM/dd` format.
     *
     * @param targetEditText EditText where formatted date will be applied.
     */
    private void showDatePickerDialog(EditText targetEditText) {
        final Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (view, year, month, dayOfMonth) -> {
                    String selectedDate =
                            String.format(Locale.getDefault(), "%04d/%02d/%02d", year, month + 1, dayOfMonth);
                    targetEditText.setText(selectedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    // -------------------------------------------------------------------------
    // MENU SETUP
    // -------------------------------------------------------------------------
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu, menu);

        for (int i = 0; i < menu.size(); i++) {
            if (menu.getItem(i).getIcon() != null) {
                menu.getItem(i).getIcon().setTint(
                        getResources().getColor(android.R.color.white));
            }
        }
        super.onCreateOptionsMenu(menu, inflater);
    }


    // -------------------------------------------------------------------------
    // CORE FUNCTION: Add or Update Medicine
    // -------------------------------------------------------------------------
    /**
     * Validates user input, ensures correct date ordering and numeric constraints,
     * then writes the medicine entry to Firebase. Same method handles creation
     * or update depending on whether `medicine` is already defined.
     *
     * On success:
     *   - Displays confirmation toast
     *   - Returns to previous fragment using back stack
     */
    private void addMedicine() {

        if (medicine == null) {
            medicine = editTextName.getText().toString().trim();
        }

        String purchaseDateStr = editTextPurchaseDate.getText().toString().trim();
        String expireDateStr = editTextExpireDate.getText().toString().trim();
        String prescriptionAmountStr = editTextPrescriptionAmount.getText().toString().trim();
        String currentAmountStr = editTextCurrentAmount.getText().toString().trim();

        // Required fields validation
        if (medicine.isEmpty() || purchaseDateStr.isEmpty() || expireDateStr.isEmpty() || prescriptionAmountStr.isEmpty()) {
            Toast.makeText(getContext(), "Please fill out all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int prescriptionAmount, currentAmount;

        try {
            prescriptionAmount = Integer.parseInt(prescriptionAmountStr);

            if (prescriptionAmount <= 0) {
                Toast.makeText(getContext(), "Prescription amount must be positive", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentAmountStr.isEmpty()) {
                currentAmount = prescriptionAmount;
            } else {
                currentAmount = Integer.parseInt(currentAmountStr);

                if (currentAmount > prescriptionAmount) {
                    Toast.makeText(getContext(), "Current amount cannot exceed prescription amount", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Please enter valid numbers for amounts", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate date order
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
        try {
            Date purchaseDate = sdf.parse(purchaseDateStr);
            Date expireDate = sdf.parse(expireDateStr);

            if (purchaseDate != null && expireDate != null && !purchaseDate.before(expireDate)) {
                Toast.makeText(getContext(), "Purchase date must be earlier than expire date", Toast.LENGTH_SHORT).show();
                return;
            }

        } catch (ParseException e) {
            Toast.makeText(getContext(), "Invalid date format. Use YYYY/MM/DD", Toast.LENGTH_SHORT).show();
            return;
        }

        long lastUpdated = System.currentTimeMillis();


        // Firebase write operation
        DatabaseReference medicineRef = FirebaseDatabase.getInstance()
                .getReference("categories/users/children")
                .child(uname)
                .child("inventory")
                .child(medicine);

        Map<String, Object> medicineData = new HashMap<>();
        medicineData.put("purchaseDate", purchaseDateStr);
        medicineData.put("expireDate", expireDateStr);
        medicineData.put("prescriptionAmount", prescriptionAmount);
        medicineData.put("currentAmount", currentAmount);
        medicineData.put("lastUpdated", lastUpdated);

        // Record the time of this update (millisecond timestamp)
        long now = System.currentTimeMillis();
        medicineData.put("lastUpdated", now);

        medicineRef.setValue(medicineData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Medicine saved successfully", Toast.LENGTH_SHORT).show();
                        requireActivity().getSupportFragmentManager().popBackStack();
                    } else {
                        Toast.makeText(getContext(), "Failed to save medicine", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
