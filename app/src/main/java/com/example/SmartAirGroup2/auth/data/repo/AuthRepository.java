package com.example.SmartAirGroup2.auth.data.repo;

public interface AuthRepository {
    ProfileCheck Check_if_User_exist(String role,String username,String email) throws Exception;
    boolean CheckPassword(String role, String username, String email, String password) throws Exception;
//    boolean CheckUsername(String uname) throws Exception;
}
