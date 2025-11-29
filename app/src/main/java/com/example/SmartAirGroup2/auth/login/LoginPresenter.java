package com.example.SmartAirGroup2.auth.login;


import com.example.SmartAirGroup2.auth.data.repo.AuthRepository;

import android.os.Handler;
import android.os.Looper;



/**
 * LoginPresenter implements the LoginContract.Presenter interface.
 *
 * This class handles all business logic related to the login feature,
 * acting as an intermediary between the View (UI) and the data layer.
 * It manages View lifecycle attachment/detachment and ensures that data operations
 * are performed on a separate thread, with results posted
 * back to the main thread for UI updates.
 */
public class LoginPresenter implements LoginContract.Presenter{

    // The repository handles the actual authentication and data operations.
    private final AuthRepository repo;

    // The view reference (can be null after detach() is called).
    private LoginContract.View view;

    // Used to post results back to the Android main (UI) thread from background threads.
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Constructor for the LoginPresenter.
     *
     * @param repo The authentication repository dependency used for data operations.
     */
    public LoginPresenter(AuthRepository repo){
        this.repo = repo;
    }

    /**
     * Attaches the View to the Presenter.
     * The Presenter holds a reference to the View to call its methods.
     *
     * @param v The View implementation (Login Fragment/Activity).
     */
    @Override
    public void attach(LoginContract.View v) {
        this.view = v;
    }

    /**
     * Detaches the View from the Presenter.
     * This sets the view reference to null to prevent memory leaks and
     * ensure no UI updates are attempted on a non-existent View.
     */
    @Override
    public void detach() {
        this.view = null;
    }

    /**
     * Executes the login business logic when the login button is clicked.
     *
     * This method first performs basic input validation. If validation passes,
     * it initiates the authentication process on a new background thread to prevent
     * blocking the main UI thread. Results are then posted back to the main thread.
     *
     * @param role The user's selected role.
     * @param username The entered username.
     * @param email The entered email.
     * @param password The entered password.
     */
    @Override
    public void onLoginClicked(String role, String username, String email,String password) {
        // Simple client-side validation for mandatory fields.
        if (email == null || email.trim().isEmpty()
                || password == null || password.trim().isEmpty()
                || username == null || username.trim().isEmpty()) {

            // If any required field is empty, show a general failure message (though a specific
            // input error message might be better, the current implementation shows LoginFailed).
            if (view != null) view.showLoginFailed(); // User not found or invalid password
            return;
        }

        // Start authentication on a new thread (background task)
        new Thread(() -> {
            try {
                // Attempt to authenticate using the repository
                boolean check = repo.CheckPassword(role, username, email, password);

                // Post the result back to the main UI thread
                mainHandler.post(() -> {
                    if (view == null) return; // Check if the view is still attached

                    if (check) {
                        // Authentication successful
                        view.showLoginSuccess(role);

                    } else {
                        // Authentication failed (incorrect credentials)
                        view.showLoginFailed();
                    }
                });
            } catch (Exception e) {
                // Handle exceptions (e.g., network error, unexpected repository error)
                mainHandler.post(() -> {
                    if (view == null) return; // Check if the view is still attached

                    // Show the specific error message, and then the general failure message
                    view.showInputError(e.getMessage());
                    view.showLoginFailed();
                });
            }
        }).start();
    }
}
