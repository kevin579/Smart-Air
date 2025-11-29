package com.example.SmartAirGroup2.auth.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.SmartAirGroup2.ChildDashboard;
import com.example.SmartAirGroup2.CurrentUser;
import com.example.SmartAirGroup2.OnboardingActivity;
import com.example.SmartAirGroup2.ParentDashboardActivity;
//import com.example.SmartAirGroup2.Parent_Provider_Dahsboard;
import com.example.SmartAirGroup2.ProviderDashboardActivity;
import com.example.SmartAirGroup2.R;
import com.example.SmartAirGroup2.User;
import com.example.SmartAirGroup2.auth.data.repo.AuthRepository;
import com.example.SmartAirGroup2.auth.data.repo.FirebaseRtdbAuthRepository;
import com.example.SmartAirGroup2.create_account;
import com.example.SmartAirGroup2.password_recover;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * LoginFragment acts as the View in the MVP architecture for the login feature.
 *
 * It is responsible for handling all UI components, capturing user input,
 * and displaying messages or navigating based on instructions from the Presenter.
 * It strictly delegates all business logic (validation, authentication, and navigation decisions)
 * to the {@link LoginPresenter}.
 */
public class LoginFragment extends Fragment implements LoginContract.View {

    // The Presenter instance that contains the login business logic.
    private LoginPresenter presenter;

    // UI input components.
    private EditText  emailInput, passwordInput,usernameInput ;

    // UI component for selecting the user role.
    private Spinner roleSpinner;

    // Auxiliary fields used for post-login processing and navigation.
    private String field, username, selectedRole, email, password;


    /**
     * Inflates the login layout and initializes UI components, listeners, and the Presenter.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.activity_login_page, container, false);

        // --- Initialize UI Views ---
        usernameInput         = v.findViewById(R.id.username_input);
        emailInput       = v.findViewById(R.id.email_input);
        passwordInput    = v.findViewById(R.id.password_input);
        View btnLogin    = v.findViewById(R.id.login_button);
        View btnCreate   = v.findViewById(R.id.new_account_button);
        View btnRecover  = v.findViewById(R.id.password_recover);
        roleSpinner = v.findViewById(R.id.role_spinner);

        // --- MVP Setup ---
        AuthRepository repo = new FirebaseRtdbAuthRepository();
        presenter = new LoginPresenter(repo);
        presenter.attach(this); // Attach the View to the Presenter

        setupRoleSpinner();

        // --- Listener for Login Button ---
        btnLogin.setOnClickListener(view -> {
            String role     = selectedRole;
            String username = usernameInput.getText()  == null ? "" : usernameInput.getText().toString();
            String email = emailInput.getText() == null ? "" : emailInput.getText().toString();
            String password   = passwordInput.getText()== null ? "" : passwordInput.getText().toString();

            // Delegate the login action to the Presenter
            presenter.onLoginClicked(role, username, email, password);

        });

        // --- Listener for Create Account Button ---
        btnCreate.setOnClickListener(view -> {
            Intent intent = new Intent(getActivity(), create_account.class);
            startActivity(intent);
        });

        // --- Listener for Password Recovery Button ---
        btnRecover.setOnClickListener(view -> {
            Intent intent = new Intent(getActivity(), password_recover.class);
            startActivity(intent);
        });

        return v;
    }


    /**
     * Called when the view is being destroyed.
     * Detaches the View from the Presenter to prevent memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        presenter.detach();
    }


    /**
     * Implements the LoginContract.View method to show login failure.
     *
     * Displays a uniform error message across the username, email, and password fields.
     */
    @Override
    public void showLoginFailed() {
        if (usernameInput != null) usernameInput.setError("User not found or invalid password");
        if (emailInput != null)    emailInput.setError("User not found or invalid password");
        if (passwordInput != null) passwordInput.setError("User not found or invalid password");
    }

    /**
     * Implements the LoginContract.View method to handle successful login.
     *
     * This method clears the UI, saves the user data locally, determines the Firebase
     * path based on the user role, checks the 'onboarded' status in the database,
     * and navigates to either the Onboarding activity or the corresponding Dashboard.
     *
     * @param role The successfully authenticated user's role.
     */
    @Override
    public void showLoginSuccess(String role) {
        // Clear any previous error states
        if (emailInput != null)    emailInput.setError(null);
        if (passwordInput != null) passwordInput.setError(null);

        // Capture credentials for local storage (CurrentUser)
        username = usernameInput.getText().toString().trim();
        email = emailInput.getText().toString();
        password = passwordInput.getText().toString();

        // Store the user object globally/locally
        User user = new User(username, username, email, password, role);
        CurrentUser.set(user);

        // Clear input fields and reset spinner selection
        if (usernameInput != null) usernameInput.setText("");
        if (emailInput != null)    emailInput.setText("");
        if (passwordInput != null) passwordInput.setText("");
        if (roleSpinner != null)   roleSpinner.setSelection(0);


//        String username = usernameInput.getText().toString();

//        Toast.makeText(getContext(), "Login success", Toast.LENGTH_SHORT).show();

        // Determine the Firebase path field name based on the role
        if(role.equals("Child")){
            field = "children";
        }else if(role.equals("Parent")){
            field = "parents";
        }
        else{
            field = "provider";
        }

        // Construct the reference to the user's specific database path
        DatabaseReference stateRef = FirebaseDatabase.getInstance()
                .getReference("categories/users")
                .child(field)
                .child(username);

        // Check the 'onboarded' status
        stateRef.child("onboarded").get()
                .addOnSuccessListener(snapshot -> {
                    boolean onboarded = snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class));

                    if (!onboarded) {
                        // Not yet onboarded, navigate to Onboarding
                        Intent intent = new Intent(getActivity(), OnboardingActivity.class);
                        intent.putExtra("username", username);
                        intent.putExtra("type", field);
                        startActivity(intent);
                    } else {
                        // Already onboarded, navigate to the correct Dashboard
                        if(role.equals("Child")){
                            Intent intent = new Intent(getActivity(), ChildDashboard.class);
                            startActivity(intent);
                        }else if(role.equals("Parent")){
                            Intent intent = new Intent(getActivity(), ParentDashboardActivity.class);
                            intent.putExtra("username", username);
                            startActivity(intent);
                        }
                        else{
                            Intent intent = new Intent(getActivity(), ProviderDashboardActivity.class);
                            intent.putExtra("username", username);
                            startActivity(intent);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // If the check fails (e.g., node doesn't exist or network error), assume not onboarded and go to Onboarding
                    Intent intent = new Intent(getActivity(), OnboardingActivity.class);
                    intent.putExtra("username", username);
                    intent.putExtra("type", field);
                    startActivity(intent);
                });

    }

    /**
     * Implements the LoginContract.View method to display a generic input error.
     *
     * Shows the error message provided by the Presenter in a short Toast notification.
     *
     * @param message The error message to display.
     */
    @Override
    public void showInputError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    //for the drop down menu

    /**
     * Configures the Role selection Spinner with an adapter and listener.
     *
     * Populates the spinner using the `roles_array` resource and updates the
     * {@code selectedRole} field when a new item is chosen. It ignores the first item
     * (assumed to be a placeholder like "Select a role").
     */
    private void setupRoleSpinner() {
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getContext(),  // Use getContext() instead of v.this
                R.array.roles_array,
                R.layout.spinner_item        );

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        roleSpinner.setAdapter(adapter);

        // Set up listener to get selected value
        roleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedRole = parent.getItemAtPosition(position).toString();

                // Don't process if "Select a role" is chosen
                if (position != 0) {
                    Toast.makeText(getContext(),  // Use getContext() instead of create_account.this
                            "Selected: " + selectedRole,
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }
}

