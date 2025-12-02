package com.example.SmartAirGroup2.Main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.SmartAirGroup2.R;
import com.google.firebase.database.FirebaseDatabase;

public class PasswordRecovery extends AppCompatActivity {
    private EditText username;
    private EditText email;
    private Button recoverButton;
    private FirebaseDatabase db;
    private Spinner roleSpinner;
    private String selectedRole;
    private ImageButton goBack;

    private void showPasswordDialog(String password) {
        AlertDialog.Builder builder = new AlertDialog.Builder(PasswordRecovery.this);
        builder.setTitle("Password Recovery");
        builder.setMessage("Your password is:\n\n" + password);

        // OK button
        builder.setPositiveButton("OK", null);

        // Copy button
        builder.setNeutralButton("Copy", (dialog, which) -> {
            // Get clipboard service
            android.content.ClipboardManager clipboard =
                    (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Password", password);
            clipboard.setPrimaryClip(clip);

            // Optional: show a toast
            Toast.makeText(PasswordRecovery.this, "Password copied to clipboard", Toast.LENGTH_SHORT).show();
        });

        builder.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_recover);

        username = findViewById(R.id.username_input);
        email = findViewById(R.id.email_input);
        recoverButton = findViewById(R.id.Recover_button);
        goBack = findViewById(R.id.back_button);
        db = FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/");
        roleSpinner = findViewById(R.id.role_spinner);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.roles_array,
                R.layout.spinner_item        );

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        roleSpinner.setAdapter(adapter);

        // Set up listener to get selected value
        roleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedRole = parent.getItemAtPosition(position).toString();

                // Don't process if "Select a role" is chosen
                if (position != 0) {
                    Toast.makeText(PasswordRecovery.this,
                            "Selected: " + selectedRole,
                            Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        goBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PasswordRecovery.this, MainActivity.class);
                startActivity(intent);
            }
        });

        recoverButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                String userInput = username.getText().toString().trim();
                String emailInput = email.getText().toString().trim();
                String role = selectedRole;

                com.example.SmartAirGroup2.auth.data.repo.PasswordRecovery.accountExists(userInput, emailInput, role, new com.example.SmartAirGroup2.auth.data.repo.PasswordRecovery.AccountCheckCallback() {
                    @Override
                    public void onResult(String password) {
                        if (!password.equals("null")) {
                            Toast.makeText(PasswordRecovery.this, "Email sent!", Toast.LENGTH_SHORT).show();
                            showPasswordDialog(password);
                        } else {
                            Toast.makeText(PasswordRecovery.this, "Account does not exist", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(PasswordRecovery.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}