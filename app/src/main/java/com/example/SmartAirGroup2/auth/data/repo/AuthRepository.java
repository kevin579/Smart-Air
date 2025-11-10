package com.example.SmartAirGroup2.auth.data.repo;

public interface AuthRepository {
    ProfileCheck Check_if_User_exist(String email) throws Exception;
    boolean CheckPassword(String email, String password) throws Exception;

}
