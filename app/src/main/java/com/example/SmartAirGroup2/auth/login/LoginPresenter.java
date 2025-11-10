package com.example.SmartAirGroup2.auth.login;

import com.example.SmartAirGroup2.auth.data.repo.AuthRepository;
import com.example.SmartAirGroup2.auth.data.repo.ProfileCheck;

public class LoginPresenter implements LoginContract.Presenter{
    private final AuthRepository repo;
    private LoginContract.View view;

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
    public void onLoginClicked(String email, String password) {
        if (email == null || email.trim().isEmpty()
                || password == null || password.trim().isEmpty()) {
            if (view != null) view.showLoginFailed(); // 统一提示：User not found or invalid password
            return;
        }

        new Thread(() -> {
            try {
                // check if email exists
                ProfileCheck pc = repo.Check_if_User_exist(email.trim());
                boolean check = false;
                if (pc.exists) {
                    // check password
                    check = repo.CheckPassword(email, password);
                }

                boolean finalOk = check;
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    if (view == null) return;
                    if (finalOk) view.showLoginSuccess();
                    else         view.showLoginFailed();
                });

            } catch (Exception e) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    if (view != null) view.showLoginFailed();
                });
            }
        }).start();
    }

}
