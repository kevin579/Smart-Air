package com.example.SmartAirGroup2.auth.data.repo;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class PasswordRecovery {
    private static final DatabaseReference usersRoot =
            FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/")
                    .getReference("categories")
                    .child("users");

    // Callback interface
    public interface AccountCheckCallback {
        void onResult(String password);
        void onError(String errorMessage);
    }

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
