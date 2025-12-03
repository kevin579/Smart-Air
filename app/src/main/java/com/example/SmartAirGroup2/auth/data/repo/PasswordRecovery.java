package com.example.SmartAirGroup2.auth.data.repo;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

/**
 * Handles the password recovery process by interacting with the Firebase Realtime Database.
 * This class provides a mechanism to verify if a user account exists based on username,
 * email, and role, and retrieves the password if the account is found.
 */
public class PasswordRecovery {
    private static final DatabaseReference usersRoot =
            FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/")
                    .getReference("categories")
                    .child("users");

    /**
     * Callback interface for handling the result of the account existence check.
     */
    public interface AccountCheckCallback {
        /**
         * Called when the account check is successful.
         * @param password The user's password if the account exists, or "null" otherwise.
         */
        void onResult(String password);
        /**
         * Called when an error occurs during the account check.
         * @param errorMessage A message describing the error.
         */
        void onError(String errorMessage);
    }

    /**
     * Checks if a user account exists in the Firebase Realtime Database.
     *
     * @param username The username to check.
     * @param email The email address to check.
     * @param role The role of the user (e.g., "child", "parent", "provider").
     * @param callback The callback to be invoked with the result.
     */
    public static void accountExists(String username, String email, String role, AccountCheckCallback callback) {
        DatabaseReference userBranch;

        if (role.equalsIgnoreCase("child")) {
            userBranch = usersRoot.child("children");
        } else if (role.equalsIgnoreCase("parent")) {
            userBranch = usersRoot.child("parents");
        } else {
            userBranch = usersRoot.child("provider");
        }

        // Use addListenerForSingleValueEvent for one-time read
        userBranch.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Loop through each user
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        // Get individual values
                        String userEmail = userSnapshot.child("email").getValue(String.class);
                        String userType = userSnapshot.child("type").getValue(String.class);
                        String userName = userSnapshot.child("uname").getValue(String.class);
                        String userPassword = userSnapshot.child("password").getValue(String.class);

                        if (username.equals(userName) && email.equals(userEmail) && role.equalsIgnoreCase(userType)) {
                            callback.onResult(userPassword);
                            return; // Exit early
                        }
                    }
                }
                // If we get here, account doesn't exist
                callback.onResult("null");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onError(databaseError.getMessage());
            }
        });
    }

}
