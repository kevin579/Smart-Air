package com.example.SmartAirGroup2;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.widget.Button;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.example.SmartAirGroup2.models.TriageIncident;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager.widget.ViewPager;
import android.widget.EditText;
import android.widget.CheckBox;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.example.SmartAirGroup2.Adapters.OnBoardingAdapter;
import com.example.SmartAirGroup2.Helpers.SaveState;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import androidx.annotation.NonNull;

public class OnboardingActivity extends AppCompatActivity {

    CardView nextCard;
    LinearLayout dotsLayout;
    ViewPager viewPager;
    TextView[] dots;
    int currentPosition;
    SaveState saveState;

    Button skipButton;

    private TriageIncident incidentData;
    private String onboardingType = "initial";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_onboarding);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        nextCard = findViewById(R.id.nextCard);
        dotsLayout = findViewById(R.id.dotsLayout);
        viewPager = findViewById(R.id.slider);

        skipButton = findViewById(R.id.skip_button);

        // Initialize SaveState helper
        saveState = new SaveState(this, "0B");

        String typeFromIntent = getIntent().getStringExtra("onboardingType");
        if (typeFromIntent == null) {
            onboardingType = getIntent().getBooleanExtra("isRepeatable", false) ? "help" : "initial";
        } else {
            onboardingType = typeFromIntent;
        }

        // Configure activity based on onboarding type
        switch (onboardingType) {
            case "help": // This is the Triage tutorial
                incidentData = new TriageIncident();
                skipButton.setText("Close");
                skipButton.setOnClickListener(v -> handleTriageExit());
                break;
            case "child":
                skipButton.setText("Close");
                skipButton.setOnClickListener(v -> finish());
                break;
            case "initial":
            default:
                incidentData = null; // No data collection for these types
                skipButton.setText(onboardingType.equals("initial") ? "Skip" : "Close");
                skipButton.setOnClickListener(v -> {
                    if (onboardingType.equals("initial")) {
                        saveState.setState(1);
                        startMainActivity();
                    } else {
                        finish();
                    }
                });
                break;
        }

        // Setup dots and ViewPager
        dotsFunction(0);
        OnBoardingAdapter adapter = new OnBoardingAdapter(this, onboardingType, incidentData);
        viewPager.setAdapter(adapter);

        nextCard.setOnClickListener(view -> viewPager.setCurrentItem(currentPosition + 1, true));
        viewPager.addOnPageChangeListener(onPageChangeListener);
    }

    @Override
    public void onBackPressed() {
        // If we are in the triage flow, use the special exit handler. Otherwise, default behavior.
        if ("help".equals(onboardingType)) {
            handleTriageExit();
        } else {
            super.onBackPressed();
        }
    }

    private void handleTriageExit() {
        new AlertDialog.Builder(this)
                .setTitle("Discard Triage Report?")
                .setMessage("If you exit now, the information you've entered will be lost.")
                .setPositiveButton("Discard", (dialog, which) -> {
                    incidentData = null; // Explicitly discard data
                    finish(); // Close the activity
                })
                .setNegativeButton("Cancel", null) // Do nothing on cancel
                .show();
    }

    private void collectDataFromPage(int position) {
        if (incidentData == null) return; // Don't do anything if not in triage mode

        View page = viewPager.findViewWithTag("page" + position);
        if (page == null) return;

        switch (position) {
            case 0: // PEF slide
                EditText pefText = page.findViewById(R.id.edit_text_pef);
                if (pefText != null) {
                    incidentData.PEF = pefText.getText().toString();
                }
                break;
            case 1: // Red flags slide
                incidentData.redflags.put("Chest Pulling or Retraction", ((CheckBox) page.findViewById(R.id.checkbox_chest_pulling)).isChecked());
                incidentData.redflags.put("Grey or Blue lips", ((CheckBox) page.findViewById(R.id.checkbox_blue_lips)).isChecked());
                incidentData.redflags.put("Grey or Blue nails", ((CheckBox) page.findViewById(R.id.checkbox_blue_nails)).isChecked());
                incidentData.redflags.put("Trouble breathing", ((CheckBox) page.findViewById(R.id.checkbox_breathing)).isChecked());
                incidentData.redflags.put("Trouble speaking", ((CheckBox) page.findViewById(R.id.checkbox_speaking)).isChecked());
                break;
        }
    }

    private void saveAndFinishTriage() {
        // Collect data from the final slide one last time before saving.
        collectDataFromPage(currentPosition);

        // 1. Get the username that was passed from TriagelogDashboardFragment
        final String currentUserId = getIntent().getStringExtra("username");

        // Critical check to ensure we have a user to save data for.
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "Error: Could not identify user to save data for.", Toast.LENGTH_LONG).show();
            finish(); // Exit to prevent errors.
            return;
        }

        // 2. Get the specific reference to the user's "triages" branch.
        final DatabaseReference triagesRef = FirebaseDatabase.getInstance().getReference()
                .child(currentUserId) // This uses the username like "randy1122"
                .child("data")
                .child("triages");

        // 3. READ FIRST: Query Firebase to count how many incidents already exist.
        //    We use .addListenerForSingleValueEvent() because we only need this data once.
        triagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // 4. Calculate the next incident number.
                long incidentCount = 0;
                if (snapshot.exists()) {
                    // snapshot.getChildrenCount() returns the number of direct children (e.g., incident1, incident2)
                    incidentCount = snapshot.getChildrenCount();
                }
                // Create the new key, e.g., "incident" + (1 + 1) becomes "incident2"
                String nextIncidentKey = "incident" + (incidentCount + 1);

                // 5. Prepare the final data object with timestamp and placeholder values.
                incidentData.time = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(new Date());
                incidentData.guidance = "cpr"; // Example placeholder
                incidentData.response = "died"; // Example placeholder

                // 6. WRITE DATA: Save the new incident using the calculated sequential key.
                //    We use .child(nextIncidentKey) instead of .push().
                triagesRef.child(nextIncidentKey).setValue(incidentData)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(OnboardingActivity.this, "Triage Report Saved as " + nextIncidentKey, Toast.LENGTH_SHORT).show();
                            finish(); // Success, so close the activity.
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(OnboardingActivity.this, "Failed to save data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // This method is called if the app doesn't have permission to read the data
                // or if another error occurs during the read operation.
                Toast.makeText(OnboardingActivity.this, "Database read failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startMainActivity() {
        Intent i = new Intent(OnboardingActivity.this, MainActivity.class);
        startActivity(i);
        finish();
    }

    private void dotsFunction(int pos){
        dots = new TextView[4];
        dotsLayout.removeAllViews();

        for (int i = 0; i < dots.length; i++) {
            dots[i] = new TextView(this);
            dots[i].setText(Html.fromHtml("●"));
            dots[i].setTextColor(getColor(R.color.white));
            dots[i].setTextSize(30);

            dotsLayout.addView(dots[i]);
        }

        if (dots.length > 0){
            dots[pos].setTextColor(getColor(R.color.teal));
            dots[pos].setTextSize(40);
        }
    }

    ViewPager.OnPageChangeListener onPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            if (positionOffset > 0) {
                collectDataFromPage(position);
            }
        }

        @Override
        public void onPageSelected(int position) {
            dotsFunction(position);
            currentPosition = position;

            final String finalOnboardingType = onboardingType;

            // Check if we are on the last page (e.g., position 3 for 4 pages)
            if (position == dots.length - 1) { // Final screen
                nextCard.setOnClickListener(view -> {

                    String username = getIntent().getStringExtra("username");
                    String field = getIntent().getStringExtra("type");

                    DatabaseReference stateRef = FirebaseDatabase.getInstance()
                            .getReference("categories/users")
                            .child(field)
                            .child(username);

                    stateRef.child("onboarded").setValue(true);
                    Log.d(":DB",field);


                    Intent intent;

                    if (field.equals("children")) {
                        intent = new Intent(OnboardingActivity.this, ChildDashboardActivity.class);
                        intent.putExtra("username", username);
                    } else if (field.equals("parents")) {
                        intent = new Intent(OnboardingActivity.this, ParentDashboardActivity.class);
                        intent.putExtra("username", username);

                    } else {
                        intent = new Intent(OnboardingActivity.this, Parent_Provider_Dashboard.class);
                        intent.putExtra("username", username);
                    }

                    startActivity(intent);
                    finish();
                    switch (finalOnboardingType) {
                        case "help":
                            saveAndFinishTriage();
                            break;
                        case "initial":
                            saveState.setState(1);
                            startMainActivity();
                            break;
                        default:
                            finish();
                            break;
                    }
                });
            } else { // Not the final screen
                nextCard.setOnClickListener(view -> viewPager.setCurrentItem(currentPosition + 1, true));
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {}
    };
}