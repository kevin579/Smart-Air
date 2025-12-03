package com.example.SmartAirGroup2;

import android.util.Log;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import androidx.annotation.NonNull;
import java.util.HashMap;
import java.util.Map;

/**
 * A helper class to handle the cleanup of expired temporary provider accounts.
 * This check should be run when the application starts.
 */
public class AccountCleanupHelper {

    private static final String TAG = "AccountCleanupHelper";
    // 7 days in milliseconds
    private static final long SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000;

    /**
     * Public method to initiate the cleanup process.
     */
    public static void runCleanup() {
        Log.d(TAG, "Starting expired provider cleanup check...");
        findAndCleanupExpiredProviders();
        findAndCleanupExpiredInviteCodes();
    }

    private static void findAndCleanupExpiredProviders() {
        DatabaseReference providersRef = FirebaseDatabase.getInstance()
                .getReference("categories/users/provider");

        // Calculate the timestamp for 7 days ago.
        long sevenDaysAgo = System.currentTimeMillis() - SEVEN_DAYS_MS;

        // Query for providers with a 'creationTimestamp' that is older than 7 days ago.
        // This targets only accounts created via the invite code system.
        providersRef.orderByChild("creationTimestamp").endAt(sevenDaysAgo)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            Log.d(TAG, "No expired providers found to clean up.");
                            return;
                        }

                        // Prepare a multi-path update to delete all expired accounts and their links atomically.
                        Map<String, Object> updates = new HashMap<>();

                        Log.d(TAG, "Found " + snapshot.getChildrenCount() + " expired provider(s). Preparing for deletion.");

                        for (DataSnapshot providerSnap : snapshot.getChildren()) {

                            if (!providerSnap.hasChild("creationTimestamp")) {
                                Log.w(TAG, "Skipping provider " + providerSnap.getKey() + " because it lacks a creationTimestamp. It is not a temporary account.");
                                continue; // Skip to the next provider in the loop
                            }

                            String providerId = providerSnap.getKey();
                            DataSnapshot parentsNode = providerSnap.child("parents");

                            // Path 1: Mark the main provider record for deletion by setting it to null.
                            updates.put("categories/users/provider/" + providerId, null);
                            Log.d(TAG, "- Marking provider for deletion: " + providerId);

                            // Check if the provider is linked to any parents.
                            if (parentsNode.exists()) {
                                for (DataSnapshot parentSnap : parentsNode.getChildren()) {
                                    String parentId = parentSnap.getKey();
                                    // Path 2: Mark the link from the parent to the provider for deletion.
                                    updates.put("categories/users/parents/" + parentId + "/providers/" + providerId, null);
                                    Log.d(TAG, "  - Marking link for deletion from parent: " + parentId);
                                }
                            }
                        }

                        // Execute the atomic deletion.
                        FirebaseDatabase.getInstance().getReference().updateChildren(updates)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Successfully deleted all expired provider accounts and links."))
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to perform cleanup.", e));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Database query for expired providers was cancelled.", error.toException());
                    }
                });
    }

    private static void findAndCleanupExpiredInviteCodes() {
        Log.d(TAG, "Checking for expired invite codes...");
        DatabaseReference invitesRef = FirebaseDatabase.getInstance().getReference("provider_invites");

        long sevenDaysAgo = System.currentTimeMillis() - SEVEN_DAYS_MS;

        // Query the 'provider_invites' branch for codes created more than 7 days ago.
        invitesRef.orderByChild("creationTimestamp").endAt(sevenDaysAgo)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            Log.d(TAG, "No expired invite codes found to clean up.");
                            return;
                        }

                        int deletedCount = 0;
                        Log.d(TAG, "Found " + snapshot.getChildrenCount() + " expired invite code(s). Deleting now.");

                        // Loop through each expired invite code and delete it.
                        for (DataSnapshot codeSnap : snapshot.getChildren()) {
                            Log.d(TAG, "- Deleting expired invite code: " + codeSnap.getKey());
                            codeSnap.getRef().removeValue(); // Remove the individual code
                            deletedCount++;
                        }

                        if (deletedCount > 0) {
                            Log.d(TAG, "Successfully deleted " + deletedCount + " expired invite code(s).");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Database query for expired invite codes was cancelled.", error.toException());
                    }
                });
    }

}