package com.example.SmartAirGroup2.auth.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.SmartAirGroup2.R;
import com.example.SmartAirGroup2.auth.data.repo.AuthRepository;
import com.example.SmartAirGroup2.auth.data.repo.FirebaseRtdbAuthRepository;
import com.example.SmartAirGroup2.create_account;
import com.example.SmartAirGroup2.password_recover;

public class LoginFragment extends Fragment implements LoginContract.View {
    private LoginPresenter presenter;
//    private EditText  emailLayout, passwordLayout;
    private EditText  emailInput, passwordInput;
    private View btnCheck;
    private Spinner roleSpinner;
    private String selectedRole;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.activity_login_page, container, false);

        emailInput       = v.findViewById(R.id.username_input);
        passwordInput    = v.findViewById(R.id.password_input);
        View btnLogin    = v.findViewById(R.id.login_button);
        View btnCreate   = v.findViewById(R.id.new_account_button);
        View btnRecover  = v.findViewById(R.id.password_recover);
        roleSpinner = v.findViewById(R.id.role_spinner);

        AuthRepository repo = new FirebaseRtdbAuthRepository();
        presenter = new LoginPresenter(repo);
        presenter.attach(this);

        setupRoleSpinner();

        btnLogin.setOnClickListener(view -> {
            String email = emailInput.getText() == null ? "" : emailInput.getText().toString();
            String pwd   = passwordInput.getText()== null ? "" : passwordInput.getText().toString();
            presenter.onLoginClicked(email, pwd);
        });

        btnCreate.setOnClickListener(view -> {
            Intent intent = new Intent(getActivity(), create_account.class);
            startActivity(intent);
        });

        btnRecover.setOnClickListener(view -> {
            Intent intent = new Intent(getActivity(), password_recover.class);
            startActivity(intent);
        });

        return v;
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        presenter.detach();
    }


    @Override
    public void showLoginFailed() {
        if (emailInput != null)    emailInput.setError("User not found or invalid password");
        if (passwordInput != null) passwordInput.setError("User not found or invalid password");
    }

    @Override
    public void showLoginSuccess() {
        if (emailInput != null)    emailInput.setError(null);
        if (passwordInput != null) passwordInput.setError(null);
        Toast.makeText(getContext(), "Login success", Toast.LENGTH_SHORT).show();
    }

    //for the drop down menu

    // Separate method for spinner setup
    private void setupRoleSpinner() {
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getContext(),  // Use getContext() instead of v.this
                R.array.roles_array,
                R.layout.spinner_item        );

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        roleSpinner.setAdapter(adapter);

        // Set up listener to get selected value
        roleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedRole = parent.getItemAtPosition(position).toString();

                // Don't process if "Select a role" is chosen
                if (position != 0) {
                    Toast.makeText(getContext(),  // Use getContext() instead of create_account.this
                            "Selected: " + selectedRole,
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }
}

