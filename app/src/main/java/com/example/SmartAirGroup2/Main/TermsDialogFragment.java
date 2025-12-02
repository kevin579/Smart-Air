package com.example.SmartAirGroup2.Main;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.example.SmartAirGroup2.R;

public class TermsDialogFragment extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());

        // Inflate XML layout
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_terms_dialog, null);

        //handle button clicks
        Button btnAccept = view.findViewById(R.id.btnAccept);
        btnAccept.setOnClickListener(v -> {
            // Save acceptance in SharedPreferences for the current user
            SharedPreferences prefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
            String userId = CurrentUser.get().getUname();  // or email, or a unique ID
            String userEmail = CurrentUser.get().getEmail();
            String userType = CurrentUser.get().getType();
            prefs.edit()
                    .putBoolean("accepted_terms_" + userType + userId + userEmail, true)
                    .apply();

            dismiss(); // close the dialog
        });

        builder.setView(view);
        return builder.create();
    }
}

