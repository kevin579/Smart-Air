package com.example.SmartAirGroup2.auth.login;

public class LoginContract {
    interface View{
        void showLoginSuccess(String role);
        void showLoginFailed();
        void showInputError(String message); //"Please select a role"
    }

    interface Presenter{
        void attach(View v); //using by onViewCreated
        void detach();  //using by onDestroyView

        void onLoginClicked(String role, String username, String email, String password);


    }
}
