package com.example.SmartAirGroup2.auth.data.repo;

public class ProfileCheck {
        public final String uid;
        public final boolean exists; // true: represents user exists in the database
        public ProfileCheck(String uid, boolean exists) {
            this.uid = uid;
            this.exists = exists;
        }

}
