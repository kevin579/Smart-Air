package com.example.SmartAirGroup2.auth.data.repo;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Repository for handling new user authentication and creation with Firebase Realtime Database.
 */
public class newUserAuth extends FirebaseRtdbAuthRepository {

    private static final DatabaseReference usersRoot =
            FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/")
                    .getReference("categories")
                    .child("users");

    /**
     * Checks if a password meets the security requirements.
     * The password must be at least 8 characters long, contain at least one uppercase letter,
     * one lowercase letter, one digit, and one special character (@#$%^&+=!*).
     *
     * @param password The password to check.
     * @return true if the password is valid, false otherwise.
     */
    public static boolean CheckPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }
        if (password.length() < 8) {
            return false;
        }

        // Check for at least one uppercase letter
        boolean hasUppercase = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUppercase = true;
                break;
            }
        }
        if (!hasUppercase) {
            return false;
        }

        // Check for at least one lowercase letter
        boolean hasLowercase = false;
        for (char c : password.toCharArray()) {
            if (Character.isLowerCase(c)) {
                hasLowercase = true;
                break;
            }
        }
        if (!hasLowercase) {
            return false;
        }

        // Check for at least one digit
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isDigit(c)) {
                hasDigit = true;
                break;
            }
        }
        if (!hasDigit) {
            return false;
        }

        // Check for at least one special character
        String specialChars = "@#$%^&+=!*";
        boolean hasSpecial = false;
        for (char c : password.toCharArray()) {
            if (specialChars.indexOf(c) >= 0) {
                hasSpecial = true;
                break;
            }
        }
        if (!hasSpecial) {
            return false;
        }

        return true;
    }

    /**
     * Creates a new user in the Firebase Realtime Database.
     *
     * @param userType The type of user (e.g., "child", "parent", "provider").
     * @param username The username for the new user.
     * @param password The password for the new user.
     * @param email The email address for the new user.
     */
    public static void createUser(String userType, String username, String password, String email) {
        // Decide which branch (children / parents / provider)
        DatabaseReference userBranch;
        if (userType.equalsIgnoreCase("child")) {
            userBranch = usersRoot.child("children");
        } else if (userType.equalsIgnoreCase("parent")) {
            userBranch = usersRoot.child("parents");
        } else {
            userBranch = usersRoot.child("provider");
        }


        // Create a user data object inline
        UserData userData = new UserData(username, email, password, userType);

        //  Write to the database under categories/users/{type}/{username}
        userBranch.child(username).setValue(userData)
                .addOnSuccessListener(aVoid -> {
                    System.out.println("User " + username + " successfully added to Firebase!");
                })
                .addOnFailureListener(e -> {
                    System.err.println("Failed to add user: " + e.getMessage());
                });
    }

    /**
     * Inner static class for user data model.
     */
    public static class UserData {
        public String uname;
        public String name;
        public String email;
        public String password;
        public String type;

        /**
         * Empty constructor required by Firebase.
         */
        public UserData() {}

        /**
         * Full constructor for creating a UserData object.
         * @param uname The username.
         * @param email The email address.
         * @param password The password.
         * @param type The user type.
         */
        public UserData(String uname, String email, String password, String type) {
            this.uname = uname;
            this.name = extractName(uname);
            this.email = email;
            this.password = password;
            this.type = type;
        }

        /**
         * Helper method to extract the name portion from a username (e.g., "parry6677" → "parry").
         * @param uname The username.
         * @return The extracted name.
         */
        private static String extractName(String uname) {
            return uname.replaceAll("\\d+$", ""); // e.g., "parry6677" → "parry"
        }
    }
}
