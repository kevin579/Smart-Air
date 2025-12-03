package com.example.SmartAirGroup2.Main;

import com.example.SmartAirGroup2.Helpers.User;

/**
 * A static class to hold the current logged-in user.
 */
public class CurrentUser {
    private static User user;

    /**
     * Sets the current user.
     *
     * @param u The user to set as the current user.
     */
    public static void set(User u) { user = u; }

    /**
     * Gets the current user.
     *
     * @return The current user.
     */
    public static User get() { return user; }
}
