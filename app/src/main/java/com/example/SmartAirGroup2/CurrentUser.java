package com.example.SmartAirGroup2;

public class CurrentUser {
    private static User user;

    public static void set(User u) { user = u; }
    public static User get() { return user; }
}

