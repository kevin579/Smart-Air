package com.example.SmartAirGroup2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.example.SmartAirGroup2.models.User;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EnterInviteCodeFragment extends Fragment {
    private ImageButton goBack;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_enter_invite_code, container, false);
        EditText codeInput = view.findViewById(R.id.invite_code_input);
        Button submitButton = view.findViewById(R.id.submit_code_button);
        goBack = view.findViewById(R.id.back_button);

        submitButton.setOnClickListener(v -> {
            String code = codeInput.getText().toString().trim().toUpperCase();
            if (!code.isEmpty()) {
                validateAndCreateProvider(code);
            } else {
                Toast.makeText(getContext(), "Please enter an invite code.", Toast.LENGTH_SHORT).show();
            }
        });

        goBack.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), MainActivity.class);
            startActivity(intent);
        });

        return view;
    }


    private void validateAndCreateProvider(final String code) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        DatabaseReference codeRef = rootRef.child("provider_invites").child(code);

        codeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (getContext() == null || !isAdded()) {
                    return;
                }

                if (!snapshot.exists()) {
                    Toast.makeText(getContext(), "Invalid or expired invite code.", Toast.LENGTH_LONG).show();
                    return;
                }

                String parentUname = snapshot.child("parentUname").getValue(String.class);
                Long expiryTimestamp = snapshot.child("expiryTimestamp").getValue(Long.class);

                if (parentUname == null || expiryTimestamp == null || System.currentTimeMillis() > expiryTimestamp) {
                    Toast.makeText(getContext(), "Invalid or expired invite code.", Toast.LENGTH_LONG).show();
                    codeRef.removeValue();
                    return;
                }

                String providerUname = "dl" + (int)(Math.random() * 1000);
                String providerPassword = UUID.randomUUID().toString().substring(0, 8);
                long creationTimestamp = System.currentTimeMillis();

                Map<String, Object> parentsNode = new HashMap<>();
                parentsNode.put(parentUname, parentUname);

                Map<String, Object> newProviderData = new HashMap<>();
                newProviderData.put("uname", providerUname);
                newProviderData.put("name", "Provider");
                newProviderData.put("email", "provider@smart-air.com");
                newProviderData.put("password", providerPassword);
                newProviderData.put("type", "Provider");
                newProviderData.put("onboarded", false);
                newProviderData.put("creationTimestamp", creationTimestamp);
                newProviderData.put("parents", parentsNode);

                Map<String, Object> updates = new HashMap<>();

                updates.put("categories/users/provider/" + providerUname, newProviderData);

                updates.put("categories/users/parents/" + parentUname + "/providers/" + providerUname, providerUname);

                updates.put("provider_invites/" + code, null);

                final String finalProviderUname = providerUname;
                final String finalProviderPassword = providerPassword;

                rootRef.updateChildren(updates).addOnCompleteListener(task -> {

                    if (getContext() == null || !isAdded()) {
                        return;
                    }

                    if (task.isSuccessful()) {
                        showCredentialsAndLogin(providerUname, providerPassword);
                    } else {
                        Toast.makeText(getContext(), "Database update failed. Please try again.", Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() == null || !isAdded()) {
                    return;
                }
                Toast.makeText(getContext(), "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCredentialsAndLogin(String username, String password) {

        if (getContext() == null || !isAdded()) {
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Account Created!")
                .setMessage("Your temporary login details:\n\nUsername: " + username + "\nPassword: " + password + "\nEmail: provider@smart-air.com" + "\n\nPlease save these details. You will be logged in automatically.")
                .setPositiveButton("OK", (dialog, which) -> {
                    if (getActivity() == null) return;
                    SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
                    prefs.edit()
                            .putString("last_logged_in_user", username)
                            .putString("last_logged_in_role", "provider")
                            .apply();

                    User newUser = new User(username, "Provider", "", password, "Provider");
                    CurrentUser.set(newUser);

                    Intent intent = new Intent(getActivity(), ProviderDashboardActivity.class);

                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.putExtra("username", username);
                    startActivity(intent);
                })
                .setCancelable(false)
                .show();
    }
}