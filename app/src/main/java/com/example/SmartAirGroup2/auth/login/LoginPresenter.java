package com.example.SmartAirGroup2.auth.login;

import static android.app.PendingIntent.getActivity;

//import com.example.SmartAirGroup2.ChildDashboard;
import com.example.SmartAirGroup2.auth.data.repo.AuthRepository;

import android.os.Handler;
import android.os.Looper;

/**
 * The presenter for the login screen, responsible for handling user interactions
 * and business logic. It communicates with the {@link AuthRepository} to authenticate
 * users and updates the {@link LoginContract.View} with the results.
 */
public class LoginPresenter implements LoginContract.Presenter{
    private final AuthRepository repo;
    private LoginContract.View view;
    private final Handler mainHandler;

    /**
     * Constructs a new LoginPresenter with a default main looper handler.
     * @param repo The authentication repository.
     */
    public LoginPresenter(AuthRepository repo) {
        this(repo, new Handler(Looper.getMainLooper()));
    }

    /**
     * Constructs a new LoginPresenter with a specified handler.
     * This constructor is useful for testing, allowing a mock handler to be injected.
     * @param repo The authentication repository.
     * @param handler The handler for posting results to the UI thread.
     */
    public LoginPresenter(AuthRepository repo, Handler handler) {
        this.repo = repo;
        this.mainHandler = handler;
    }

    /**
     * Attaches the view to the presenter.
     * @param v The view to attach.
     */
    @Override
    public void attach(LoginContract.View v) {
        this.view = v;
    }

    /**
     * Detaches the view from the presenter to prevent memory leaks.
     */
    @Override
    public void detach() {
        this.view = null;
    }

    /**
     * Handles the login button click event. It validates the user's input and then
     * attempts to authenticate the user in a background thread.
     * The result is then posted back to the UI thread.
     * @param role The user's selected role.
     * @param username The user's entered username.
     * @param email The user's entered email.
     * @param password The user's entered password.
     */
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
