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
    private EditText  emailLayout, passwordLayout;
    private EditText  emailInput, passwordInput;
    private View btnCheck;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.activity_login_page, container, false);

        EditText emailInput   = v.findViewById(R.id.username_input);
        EditText pwdInput     = v.findViewById(R.id.password_input);
        View btnLogin         = v.findViewById(R.id.login_button);
        View btnCreate = v.findViewById(R.id.new_account_button);

        // 绑定到你的 Presenter
        btnLogin.setOnClickListener(view -> {
            String email = emailInput.getText() == null ? "" : emailInput.getText().toString();
            String pwd   = pwdInput.getText()   == null ? "" : pwdInput.getText().toString();
            presenter.onLoginClicked(email, pwd);
        });

        return v;
    }


    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        emailLayout= v.findViewById(R.id.auth_email_layout);
        emailInput  =v.findViewById(R.id.auth_email_input);
        passwordLayout =v.findViewById(R.id.auth_pwd_layout);
        passwordInput= v.findViewById(R.id.auth_pwd_input);
        btnCheck = v.findViewById(R.id.auth_btn_check);

        // 2) prepare Presenter
        AuthRepository repo = new FirebaseRtdbAuthRepository();
        presenter = new LoginPresenter(repo);
        presenter.attach(this);

        // 3) click the button → using Presenter
        btnCheck.setOnClickListener(x -> {
            emailLayout.setError(null);
            String email = emailInput.getText() == null ? "" : emailInput.getText().toString();
            String password= passwordInput.getText()  == null ? "" : passwordInput.getText().toString();

            presenter.onLoginClicked(email,password);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        presenter.detach();
    }


    @Override
    public void showLoginFailed() {
        emailLayout.setError("User not found or invalid password");
        passwordLayout.setError(null);
    }

    @Override
    public void showLoginSuccess() {
        emailLayout.setError(null);
        passwordLayout.setError(null);
        Toast.makeText(getContext(), "Login success", Toast.LENGTH_SHORT).show();
    }
}

