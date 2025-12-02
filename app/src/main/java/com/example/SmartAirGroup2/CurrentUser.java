package com.example.SmartAirGroup2;

import com.example.SmartAirGroup2.models.User;

public class CurrentUser {
    private static User user;

    public static void set(User u) { user = u; }
    public static User get() { return user; }
}

