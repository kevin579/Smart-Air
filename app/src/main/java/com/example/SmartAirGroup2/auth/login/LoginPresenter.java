package com.example.SmartAirGroup2.auth.login;

import static android.app.PendingIntent.getActivity;

import static androidx.core.content.ContextCompat.startActivity;

import com.example.SmartAirGroup2.ChildDashboard;
import com.example.SmartAirGroup2.Parent_Provider_Dashboard;
import com.example.SmartAirGroup2.auth.data.repo.AuthRepository;
import com.example.SmartAirGroup2.auth.data.repo.ProfileCheck;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.content.Intent;



public class LoginPresenter implements LoginContract.Presenter{
    private final AuthRepository repo;
    private LoginContract.View view;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public LoginPresenter(AuthRepository repo){
        this.repo = repo;
    }

    @Override
    public void attach(LoginContract.View v) {
        this.view = v;
    }
    @Override
    public void detach() {
        this.view = null;
    }

    @Override
    public void onLoginClicked(String role, String username, String email,String password) {
        if (email == null || email.trim().isEmpty()
                || password == null || password.trim().isEmpty()
                || username == null || username.trim().isEmpty()) {
            if (view != null) view.showLoginFailed(); // User not found or invalid password
            return;
        }

        new Thread(() -> {
            try {
                boolean check = repo.CheckPassword(role, username, email, password);
                mainHandler.post(() -> {
                    if (view == null) return;

                    if (check) {
                        view.showLoginSuccess(role);

                    } else {
                        view.showLoginFailed();
                    }
                });
                } catch (Exception e) {
                    mainHandler.post(() -> {
                        if (view == null) return;
                        view.showInputError(e.getMessage());
                        view.showLoginFailed();
                    });
                }
            }).start();
        }
    }
