package com.example.SmartAirGroup2.ChildDashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.SmartAirGroup2.R;

/**
 * A {@link Fragment} that displays the user's badge library.
 * This screen shows the badges earned by the child.
 */
public class BadgeLibrary extends Fragment {
    private ImageButton btnBack;

    /**
     * Default constructor for the BadgeLibrary fragment.
     * Required empty public constructor.
     */
    public BadgeLibrary() {
    }

    /**
     * Called to create the view for the fragment. This method inflates the layout
     * for the badge library, initializes the back button, and sets up its click listener
     * to navigate back to the previous screen.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.streak_format, container, false);
        btnBack = view.findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> requireActivity()
                .getSupportFragmentManager()
                .popBackStack()
        );

        return view;
    }
}
