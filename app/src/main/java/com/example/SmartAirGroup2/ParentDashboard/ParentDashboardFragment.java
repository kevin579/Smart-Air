package com.example.SmartAirGroup2.ParentDashboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.SmartAirGroup2.Main.CurrentUser;
import com.example.SmartAirGroup2.Helpers.MenuHelper;
import com.example.SmartAirGroup2.R;
import com.example.SmartAirGroup2.Main.TermsDialogFragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * A fragment that serves as the main dashboard for a parent user.
 * It provides navigation to manage children and healthcare providers.

 */
public class ParentDashboardFragment extends Fragment {

    private String uname;

    private Toolbar toolbar;

    /**
     * CardView for the "Add Child" button.
     * Always displayed at the bottom of the children list.
     */
    private CardView cardChildren,cardProvider;

    private FirebaseDatabase data;
    private boolean safetyAlertShown = false;



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
        data = FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/");

        checkChildrenPefAndMaybeShowAlert();
        return view;
    }

    private void checkChildrenPefAndMaybeShowAlert() {
        if (data == null || uname == null || uname.trim().isEmpty()) {
            return;
        }

        DatabaseReference Parent_child_Ref = data.getReference("categories/users/parents")
                .child(uname)
                .child("children");

        Parent_child_Ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists() || safetyAlertShown) {
                    return;
                }

                for (DataSnapshot childSnap : snapshot.getChildren()) {
                    String childUname = childSnap.getValue(String.class);
                    if (childUname == null || childUname.trim().isEmpty()) {
                        continue;
                    }

                    DatabaseReference childRef = data.getReference("categories/users/children")
                            .child(childUname);

                    childRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (!snapshot.exists() || safetyAlertShown || !isAdded()) {
                                return;
                            }

                            String show_name = snapshot.child("name").getValue(String.class);
                            if (show_name == null || show_name.trim().isEmpty()) {
                                return;
                            }

                            DataSnapshot statusSnap = snapshot.child("status");
                            Integer pefZone = statusSnap.child("pefZone").getValue(Integer.class);

                            if (pefZone != null && pefZone == 2 && !safetyAlertShown) {
                                safetyAlertShown = true;
                                showSafetyAlertDialog(childUname, show_name);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void showSafetyAlertDialog(String childUname, String childDisplayName) {
        if (!isAdded() || getContext() == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Safety Alert")
                .setMessage("Your child is in the red PEF zone. Please check their asthma status.")
                .setPositiveButton("View alerts", (dialog, which) -> {
                    AlertCenterFragment alertFrag = new AlertCenterFragment();
                    Bundle args = new Bundle();
                    args.putString("parentUname", uname);
                    alertFrag.setArguments(args);

                    getParentFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, alertFrag)
                            .addToBackStack(null)
                            .commit();
                })
                .setNegativeButton("Dismiss", null)
                .show();
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
        MenuHelper.setupNotification(this,menu,inflater);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return MenuHelper.handleMenuSelection(item, this) || super.onOptionsItemSelected(item);
    }
}
