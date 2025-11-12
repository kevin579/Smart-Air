package com.example.SmartAirGroup2;

import android.app.DatePickerDialog;
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

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DataSnapshot;
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

public class AddInventoryFragment extends Fragment {

    // ───────────────────────────────
    // UI Components
    // ───────────────────────────────
    private EditText editTextName, editTextPurchaseDate, editTextExpireDate, editTextPrescriptionAmount, editTextCurrentAmount;
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
    private String uname, medicine;   // passed from previous fragment

    // ───────────────────────────────
    // Lifecycle: Fragment Creation
    // ───────────────────────────────
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve parent username passed as argument
        if (getArguments() != null) {
            uname = getArguments().getString("childUname");
            medicine = getArguments().getString("medicineName");
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
        View view;
        if (medicine!=null){
            view = inflater.inflate(R.layout.activity_update_medicine_fragment, container, false);
        } else{
            view = inflater.inflate(R.layout.activity_add_medicine_fragment, container, false);
        }


        // Initialize toolbar
        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        toolbar.setTitle("Add Medicine");

        // Handle back navigation (up button)
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        // Initialize UI elements
        if (medicine==null){
            editTextName = view.findViewById(R.id.editTextName);
        }

        editTextPurchaseDate = view.findViewById(R.id.editTextPurchaseDate);
        editTextExpireDate = view.findViewById(R.id.editTextExpireDate);
        editTextPrescriptionAmount = view.findViewById(R.id.editTextPrescriptionAmount);
        editTextCurrentAmount = view.findViewById(R.id.editTextCurrentAmount);
        buttonAdd = view.findViewById(R.id.buttonAdd);

        editTextPurchaseDate.setOnClickListener(v -> showDatePickerDialog(editTextPurchaseDate));
        editTextExpireDate.setOnClickListener(v -> showDatePickerDialog(editTextExpireDate));

        // Initialize Firebase instance
        db = FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/");

        // Add button listener
        buttonAdd.setOnClickListener(v -> addMedicine());

        return view;
    }

    private void showDatePickerDialog(EditText targetEditText) {
        final Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (view, year, month, dayOfMonth) -> {
                    // Format: YYYY/MM/DD
                    String selectedDate = String.format(Locale.getDefault(), "%04d/%02d/%02d", year, month + 1, dayOfMonth);
                    targetEditText.setText(selectedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
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
    private void addMedicine() {
        // Collect input values
        if (medicine==null){
            medicine = editTextName.getText().toString().trim();
        }

        String purchaseDateStr = editTextPurchaseDate.getText().toString().trim();
        String expireDateStr = editTextExpireDate.getText().toString().trim();
        String prescriptionAmountStr = editTextPrescriptionAmount.getText().toString().trim();
        String currentAmountStr = editTextCurrentAmount.getText().toString().trim();

        // --- Validation ---
        if (medicine.isEmpty() || purchaseDateStr.isEmpty() || expireDateStr.isEmpty() || prescriptionAmountStr.isEmpty()) {
            Toast.makeText(getContext(), "Please fill out all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check number types
        int prescriptionAmount, currentAmount;
        try {
            prescriptionAmount = Integer.parseInt(prescriptionAmountStr);
            if (prescriptionAmount <= 0) {
                Toast.makeText(getContext(), "Prescription amount must be positive", Toast.LENGTH_SHORT).show();
                return;
            }

            // If current amount is empty, default = prescription amount
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

        // Check date order: purchaseDate < expireDate
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

        DatabaseReference medicineRef = FirebaseDatabase.getInstance()
                .getReference("categories/users/children")
                .child(uname) // ensure childUname is passed to this fragment
                .child("inventory")
                .child(medicine);

        Map<String, Object> medicineData = new HashMap<>();
        medicineData.put("purchaseDate", purchaseDateStr);
        medicineData.put("expireDate", expireDateStr);
        medicineData.put("prescriptionAmount", prescriptionAmount);
        medicineData.put("currentAmount", currentAmount);

        medicineRef.setValue(medicineData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Medicine added successfully", Toast.LENGTH_SHORT).show();
                        requireActivity().getSupportFragmentManager().popBackStack(); // go back
                    } else {
                        Toast.makeText(getContext(), "Failed to add medicine", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
