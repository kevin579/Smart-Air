package com.example.SmartAirGroup2;

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

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.SmartAirGroup2.auth.data.repo.newUserAuth;
import com.google.firebase.database.FirebaseDatabase;

public class password_recover extends AppCompatActivity {
    private EditText username;
    private EditText email;
    private Button recoverButton;
    private FirebaseDatabase db;
    private Spinner roleSpinner;
    private String selectedRole;
    private ImageButton goBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_recover);

        username = findViewById(R.id.username_input);
        email = findViewById(R.id.email_input);
        recoverButton = findViewById(R.id.Recover_button);
        goBack = findViewById(R.id.back_button);
        db = FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/");

        goBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(password_recover.this, MainActivity.class);
                startActivity(intent);
            }
        });

        recoverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String userInput = username.getText().toString().trim();
                String emailInput = email.getText().toString().trim();
                //todo: check if user exist, if not, show invalid user
                //todo: send user their password to their email address
                Toast.makeText(password_recover.this, "Email sent!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}