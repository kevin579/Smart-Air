package com.example.SmartAirGroup2.auth.login;

/**
 * Defines the contract between the View and the Presenter for the login functionality.
 * This contract ensures a clean separation of concerns, making the code more modular,
 * testable, and maintainable. The `LoginContract` class is a container for the
 * nested `View` and `Presenter` interfaces.
 */
public class LoginContract {
    /**
     * The View interface, implemented by the Activity or Fragment, that displays the UI
     * and forwards user actions to the Presenter.
     */
    public interface View{
        /**
         * Called when the login is successful.
         * @param role The role of the logged-in user.
         */
        void showLoginSuccess(String role);

        /**
         * Called when the login fails due to incorrect credentials or other server-side issues.
         */
        void showLoginFailed();

        /**
         * Shows an error message for invalid input.
         * @param message The error message to display (e.g., "Please select a role").
         */
        void showInputError(String message);
    }

    /**
     * The Presenter interface that contains the business logic for the login screen.
     */
    interface Presenter{
        /**
         * Attaches the view to the presenter. Typically called in onViewCreated.
         * @param v The view to attach.
         */
        void attach(View v);

        /**
         * Detaches the view from the presenter. Typically called in onDestroyView to avoid memory leaks.
         */
        void detach();

        /**
         * Called when the user clicks the login button.
         * @param role The selected user role.
         * @param username The entered username.
         * @param email The entered email.
         * @param password The entered password.
         */
        void onLoginClicked(String role, String username, String email, String password);


    }
}
