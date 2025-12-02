package com.example.SmartAirGroup2.Main;

import com.example.SmartAirGroup2.Helpers.User;

public class CurrentUser {
    private static User user;

    public static void set(User u) { user = u; }
    public static User get() { return user; }
}

