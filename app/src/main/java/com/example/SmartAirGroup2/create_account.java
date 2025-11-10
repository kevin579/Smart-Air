package com.example.SmartAirGroup2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.FirebaseDatabase;

public class create_account extends AppCompatActivity {
    private EditText username;
    private EditText password;
    private Button createButton;
    private FirebaseDatabase db;

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
        createButton = findViewById(R.id.new_account_button);
        db = FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/");

        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String userInput = username.getText().toString().trim();
                String passInput = password.getText().toString().trim();

                if (notInDatabase(userInput) && isValid(passInput)) {
                    Toast.makeText(create_account.this, "account registered!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(create_account.this, MainActivity.class);
                    startActivity(intent);
                }
                else if (!notInDatabase(userInput)){
                    Toast.makeText(create_account.this, "account already exist", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(create_account.this, "Password must be at least 8 characters with uppercase, lowercase, number, and special character", Toast.LENGTH_LONG).show();
                }
            }
        });
    }


}