package com.example.SmartAirGroup2;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.database.FirebaseDatabase;

public class ParentDashboardFragment extends Fragment {

    private String uname,type;

    private Toolbar toolbar;

    /**
     * CardView for the "Add Child" button.
     * Always displayed at the bottom of the children list.
     */
    private CardView cardChildren,cardProvider;


    public static ParentDashboardFragment newInstance(String username) {
        ParentDashboardFragment fragment = new ParentDashboardFragment();
        Bundle args = new Bundle();
        args.putString("username", username);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve parent username passed as argument
        if (getArguments() != null) {
            uname = getArguments().getString("username");
        }
        type="parent";
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_parent_dashboard, container, false);

        // Get SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);

        // Get the current logged-in user's unique identifier
        String userId = CurrentUser.get().getUname();  // or email, or a unique ID
        String userEmail = CurrentUser.get().getEmail();
        String userType = CurrentUser.get().getType();

        // Check if this user has accepted terms
        boolean hasAcceptedTerms = prefs.getBoolean("accepted_terms_" + userType + userId + userEmail, false);

        if (!hasAcceptedTerms) {
            // Show the TermsDialogFragment
            TermsDialogFragment dialog = new TermsDialogFragment();
            dialog.show(getParentFragmentManager(), "terms_dialog");
        }


        // Toolbar setup
        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        setHasOptionsMenu(true);

        // ─────────────────────────────────────────────────────────────────
        // UI Component Initialization
        // ─────────────────────────────────────────────────────────────────
        cardChildren = view.findViewById(R.id.cardChildren);
        cardProvider = view.findViewById(R.id.cardProvider);


        cardChildren.setOnClickListener(v -> {
            ParentManageChildrenFragment PMCFrag = new ParentManageChildrenFragment();
            loadFragment(PMCFrag);
        });

        cardProvider.setOnClickListener(v -> {
            ParentManageProviderFragment PMCFrag = new ParentManageProviderFragment();
            loadFragment(PMCFrag);
        });

        return view;
    }

    /**
     * Navigates to another fragment within the same activity.
     * Adds the transaction to the back stack for back button support.
     *
     * @param fragment The fragment to navigate to
     */
    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    // ───────────────────────────────
    // MENU HANDLING
    // ───────────────────────────────
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        MenuHelper.setupMenu(menu, inflater, requireContext());
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return MenuHelper.handleMenuSelection(item, this) || super.onOptionsItemSelected(item);
    }
}
