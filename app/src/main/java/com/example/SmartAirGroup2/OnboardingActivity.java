package com.example.SmartAirGroup2;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager.widget.ViewPager;

import com.example.SmartAirGroup2.Adapters.OnBoardingAdapter;
import com.example.SmartAirGroup2.Helpers.SaveState;

public class OnboardingActivity extends AppCompatActivity {

    CardView nextCard;
    LinearLayout dotsLayout;
    ViewPager viewPager;
    TextView[] dots;
    int currentPosition;
    SaveState saveState;

    Button skipButton; // Add a Skip/Close button variable

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

        boolean isRepeatable = getIntent().getBooleanExtra("isRepeatable", false);

        if (isRepeatable) {
            // This is a tutorial launched from the dashboard.
            // Change the button text to "Close" and make it close the activity.
            skipButton.setText("Close");
            skipButton.setOnClickListener(v -> finish()); // Closes the activity and returns to the dashboard
        } else {
            // This is the first-time onboarding.
            // "Skip" should save the state and go to MainActivity.
            skipButton.setText("Skip");
            skipButton.setOnClickListener(v -> {
                saveState.setState(1); // Mark as completed
                startMainActivity();
            });
        }

        // Setup dots and ViewPager
        dotsFunction(0);
        OnBoardingAdapter adapter = new OnBoardingAdapter(this, isRepeatable);
        viewPager.setAdapter(adapter);

        nextCard.setOnClickListener(view -> viewPager.setCurrentItem(currentPosition + 1, true));
        viewPager.addOnPageChangeListener(onPageChangeListener);
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
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

        @Override
        public void onPageSelected(int position) {
            dotsFunction(position);
            currentPosition = position;

            boolean isRepeatable = getIntent().getBooleanExtra("isRepeatable", false);

            if (currentPosition < 3) { // Use < 3 for 4 pages (0, 1, 2)
                nextCard.setOnClickListener(view -> viewPager.setCurrentItem(currentPosition + 1));
            } else {
                // This is the final screen
                nextCard.setOnClickListener(view -> {
                    if (!isRepeatable) {
                        saveState.setState(1); // Mark onboarding as complete
                        startMainActivity();
                    }
                    finish();
                });
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {}
    };
}