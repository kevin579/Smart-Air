package com.example.SmartAirGroup2;

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

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.SmartAirGroup2.auth.data.repo.ProfileCheck;
import com.example.SmartAirGroup2.auth.data.repo.newUserAuth;
import com.google.firebase.database.FirebaseDatabase;

public class create_account extends AppCompatActivity {
    private EditText username;
    private EditText email;
    private EditText password;
    private EditText confirmPassword;
    private Button createButton;
    private FirebaseDatabase db;
    private Spinner roleSpinner;
    private String selectedRole;
    private ImageButton goBack;

    private ProfileCheck profile;

    public boolean notInDatabase(String userInput){
        return true;
    }

    public boolean isValid(String passInput){
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        username = findViewById(R.id.username_input);
        password = findViewById(R.id.password_input);
        confirmPassword = findViewById(R.id.confirmPassword_input);
        email = findViewById(R.id.email_input);
        createButton = findViewById(R.id.new_account_button);
        goBack = findViewById(R.id.back_button);
        db = FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/");
        //for the drop down menu
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
                   Toast.makeText(create_account.this,
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
                Intent intent = new Intent(create_account.this, MainActivity.class);
                startActivity(intent);
            }
        });

        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String userInput = username.getText().toString().trim();
                String emailInput = email.getText().toString().trim();
                String passInput = password.getText().toString().trim();
                String confirmPassInput = confirmPassword.getText().toString().trim();
                String Role;
                if (selectedRole == null || selectedRole.equals("Select a role")) {
                    Toast.makeText(create_account.this, "Please select a role", Toast.LENGTH_SHORT).show();
                    //todo: show error message "Please select a role"

                }
                else if(!passInput.equals(confirmPassInput)){
                    Toast.makeText(create_account.this, "passwords do not match!", Toast.LENGTH_SHORT).show();
                    //todo: show error message "passwords do not match"
                }
                //todo: replace notInDatabase with accountExist(email, username, role)
                else if (notInDatabase(userInput) && newUserAuth.CheckPassword(passInput) ) {
                    Toast.makeText(create_account.this, "account registered!", Toast.LENGTH_SHORT).show();
                    Role = selectedRole.trim();
                    //todo: create new user based on the given role, username, password, and email
                    newUserAuth.createUser(Role, userInput, passInput, emailInput);
                    Intent intent = new Intent(create_account.this, MainActivity.class);
                    startActivity(intent);
                }
                else if (!notInDatabase(userInput)){
                    Toast.makeText(create_account.this, "account already exist", Toast.LENGTH_SHORT).show();
                    //todo: show error message "account already exist"
                }
                else{
                    Toast.makeText(create_account.this, "Password must be at least 8 characters with uppercase, lowercase, number, and special character", Toast.LENGTH_LONG).show();
                    //todo: Password must be at least 8 characters with uppercase, lowercase, number, and special character"
                }
            }
        });
    }




}