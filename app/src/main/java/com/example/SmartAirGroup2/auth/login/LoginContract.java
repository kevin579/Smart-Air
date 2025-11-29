package com.example.SmartAirGroup2.auth.login;

/**
 * Contract class defining communication between the Login View (UI layer)
 * and the Presenter (logic layer) following the MVP architecture pattern.
 *
 * This interface ensures the View and Presenter interact only through
 * clearly defined methods, improving testability and separation of concerns.
 */
public class LoginContract {

    /**
     * View interface represents the UI component (Fragment/Activity)
     * responsible for rendering data and displaying messages but not
     * handling business logic.
     */
    public interface View{

        /**
         * Called when the login request is successful.
         *
         * @param role the user's assigned role (e.g., "Child", "Parent", "Provider").
         */
        void showLoginSuccess(String role);

        /**
         * Called when the login request fails due to incorrect credentials
         * or unexpected errors.
         */
        void showLoginFailed();

        /**
         * Displays an input validation message when the user submits
         * incomplete or invalid login information.
         *
         * @param message message describing what input is missing or incorrect (e.g., "Please select a role").
         */
        void showInputError(String message); //"Please select a role"
    }

    /**
     * Presenter interface contains login business logic. It receives input
     * from the View, validates it, communicates with the data layer, and
     * returns the results back to the View.
     */
    interface Presenter{

        /**
         * Injects the View instance into the Presenter.
         * This is typically called inside the View's lifecycle method (e.g., onViewCreated).
         *
         * @param v the View implementation to attach.
         */
        void attach(View v); //using by onViewCreated

        /**
         * Clears the reference to the View to prevent memory leaks.
         * This should be called inside the View's cleanup method (e.g., onDestroyView).
         */
        void detach();  //using by onDestroyView

        /**
         * Handles the login request triggered by the user action.
         * This method validates input, interacts with authentication
         * services or data sources, and returns the result to the View.
         *
         * @param role the selected role ("Child", "Parent", or "Provider").
         * @param username the user's unique username.
         * @param email the user's login email.
         * @param password the password entered by the user.
         */
        void onLoginClicked(String role, String username, String email, String password);
    }
}