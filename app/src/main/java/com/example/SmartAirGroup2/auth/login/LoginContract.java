package com.example.SmartAirGroup2.auth.login;

public class LoginContract {
    interface View{
        void showLoginSuccess();
        void showLoginFailed();
    }

    interface Presenter{
        void attach(View v); //using by onViewCreated
        void detach();  //using by onDestroyView

        void onLoginClicked(String email, String password);


    }
}
