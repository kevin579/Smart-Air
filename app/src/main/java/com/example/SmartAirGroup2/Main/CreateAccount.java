package com.example.SmartAirGroup2.Main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.SmartAirGroup2.R;
import com.example.SmartAirGroup2.auth.data.repo.ProfileCheck;
import com.example.SmartAirGroup2.auth.data.repo.newUserAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.example.SmartAirGroup2.auth.data.repo.AuthRepository;
import com.example.SmartAirGroup2.auth.data.repo.FirebaseRtdbAuthRepository;

/**
 * Activity for creating a new user account.
 */
public class CreateAccount extends AppCompatActivity {

    /**
     * The username input field.
     */
    private EditText username;
    /**
     * The email input field.
     */
    private EditText email;
    /**
     * The password input field.
     */
    private EditText password;
    /**
     * The confirm password input field.
     */
    private EditText confirmPassword;
    /**
     * The create account button.
     */
    private Button createButton;
    /**
     * The Firebase database instance.
     */
    private FirebaseDatabase db;
    /**
     * The role spinner for selecting user role.
     */
    private Spinner roleSpinner;
    /**
     * The selected role from the spinner.
     */
    private String selectedRole;
    /**
     * The button to go back to the previous screen.
     */
    private ImageButton goBack;

    /**
     * The authentication repository.
     */
    private AuthRepository authRepo;

    /**
     * Called when the activity is first created.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        username        = findViewById(R.id.username_input);
        password        = findViewById(R.id.password_input);
        confirmPassword = findViewById(R.id.confirmPassword_input);
        email           = findViewById(R.id.email_input);
        createButton    = findViewById(R.id.new_account_button);
        goBack          = findViewById(R.id.back_button);

        db = FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/");
        authRepo = new FirebaseRtdbAuthRepository();

        roleSpinner = findViewById(R.id.role_spinner);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.roles_array,
                R.layout.spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(adapter);

        roleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedRole = parent.getItemAtPosition(position).toString();

                if (position != 0) {
                    Toast.makeText(CreateAccount.this,
                            "Selected: " + selectedRole,
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        goBack.setOnClickListener(view -> {
            Intent intent = new Intent(CreateAccount.this, MainActivity.class);
            startActivity(intent);
        });

        createButton.setOnClickListener(view -> {
            String userInput        = username.getText().toString().trim();
            String emailInput       = email.getText().toString().trim();
            String passInput        = password.getText().toString().trim();
            String confirmPassInput = confirmPassword.getText().toString().trim();

            if (selectedRole == null || selectedRole.equals("Select a role")) {
                Toast.makeText(CreateAccount.this,
                        "Please select a role", Toast.LENGTH_SHORT).show();
                return;
            }

            if (userInput.isEmpty() || emailInput.isEmpty()
                    || passInput.isEmpty() || confirmPassInput.isEmpty()) {
                Toast.makeText(CreateAccount.this,
                        "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!passInput.equals(confirmPassInput)) {
                Toast.makeText(CreateAccount.this,
                        "passwords do not match!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newUserAuth.CheckPassword(passInput)) {
                Toast.makeText(CreateAccount.this,
                        "Password must be at least 8 characters with uppercase, lowercase, number, and special character",
                        Toast.LENGTH_LONG).show();
                return;
            }

            new Thread(() -> {
                try {
                    ProfileCheck profile = authRepo.Check_if_User_exist(
                            selectedRole,
                            userInput,
                            emailInput
                    );

                    boolean exists = profile.exists;

                    if (exists) {
                        // account exists
                        runOnUiThread(() -> Toast.makeText(
                                CreateAccount.this,
                                "account already exist",
                                Toast.LENGTH_SHORT
                        ).show());
                    } else {
                        // no exists - create new account
                        String role = selectedRole.trim();
                        newUserAuth.createUser(role, userInput, passInput, emailInput);

                        runOnUiThread(() -> {
                            Toast.makeText(CreateAccount.this,
                                    "account registered!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(CreateAccount.this, MainActivity.class);
                            startActivity(intent);
                        });
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(
                            CreateAccount.this,
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show());
                }
            }).start();
        });
    }
}
