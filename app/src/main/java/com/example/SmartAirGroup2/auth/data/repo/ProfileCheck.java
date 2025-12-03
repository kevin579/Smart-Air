package com.example.SmartAirGroup2.auth.data.repo;

/**
 * Represents the result of a check to see if a user's profile exists in the database.
 * This is an immutable data class.
 */
public class ProfileCheck {
        public final String uid;
        public final boolean exists; // true: represents user exists in the database
        public ProfileCheck(String uid, boolean exists) {
            this.uid = uid;
            this.exists = exists;
        }

}
