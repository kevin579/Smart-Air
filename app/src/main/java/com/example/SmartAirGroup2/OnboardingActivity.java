package com.example.SmartAirGroup2;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager.widget.ViewPager;

import com.example.SmartAirGroup2.Adapters.OnBoardingAdapter;
import com.example.SmartAirGroup2.Helpers.SaveState;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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
                        intent = new Intent(OnboardingActivity.this, ChildDashboard.class);
                        intent.putExtra("childId", username);
                    } else if (field.equals("parents")) {
                        intent = new Intent(OnboardingActivity.this, ParentDashboardActivity.class);
                        intent.putExtra("username", username);

                    } else {
                        intent = new Intent(OnboardingActivity.this, Parent_Provider_Dashboard.class);
                    }

                    startActivity(intent);
                    finish();
                });
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {}
    };
}