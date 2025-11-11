package com.example.SmartAirGroup2;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
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

        // Initialize SaveState helper
        saveState = new SaveState(this, "0B");

        // Setup dots and ViewPager
        dotsFunction(0);
        OnBoardingAdapter adapter = new OnBoardingAdapter(this);
        viewPager.setAdapter(adapter);

        nextCard.setOnClickListener(view -> viewPager.setCurrentItem(currentPosition + 1, true));
        viewPager.addOnPageChangeListener(onPageChangeListener);
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
            if (currentPosition < 3) { // Use < 3 for 4 pages (0, 1, 2)
                nextCard.setOnClickListener(view -> viewPager.setCurrentItem(currentPosition + 1));
            } else {
                // This is the final screen
                nextCard.setOnClickListener(view -> {
                    saveState.setState(1); // Mark onboarding as complete
                    Intent i = new Intent(OnboardingActivity.this, MainActivity.class);
                    startActivity(i);
                    finish(); // Finish OnboardingActivity so user can't go back
                });
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {}
    };
}