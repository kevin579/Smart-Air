package com.example.SmartAirGroup2.auth.login;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.SmartAirGroup2.R;
import com.example.SmartAirGroup2.auth.data.repo.AuthRepository;
import com.example.SmartAirGroup2.auth.data.repo.FirebaseRtdbAuthRepository;

public class LoginFragment extends Fragment implements LoginContract.View {
    private LoginPresenter presenter;
//    private EditText  emailLayout, passwordLayout;
    private EditText  emailInput, passwordInput;
    private View btnCheck;


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

        AuthRepository repo = new FirebaseRtdbAuthRepository();
        presenter = new LoginPresenter(repo);
        presenter.attach(this);

        btnLogin.setOnClickListener(view -> {
            String email = emailInput.getText() == null ? "" : emailInput.getText().toString();
            String pwd   = passwordInput.getText()== null ? "" : passwordInput.getText().toString();
            presenter.onLoginClicked(email, pwd);
        });

        btnCreate.setOnClickListener(view -> {
            // TODO: switch to register page
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
}

