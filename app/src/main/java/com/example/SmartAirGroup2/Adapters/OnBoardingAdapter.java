package com.example.SmartAirGroup2.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewpager.widget.PagerAdapter;


import com.example.SmartAirGroup2.R;
public class OnBoardingAdapter extends PagerAdapter{
    Context context;
    LayoutInflater layoutinflater;

    public OnBoardingAdapter(Context context) {
        this.context = context;
    }

    int[] onboard_titles = {R.string.title1, R.string.title2, R.string.title3, R.string.title4};
    int[] onboard_subtitles = {R.string.subtitle1, R.string.subtitle2, R.string.subtitle3, R.string.subtitle4};
    int[] onboard_images = {R.drawable.on_boarding_vector_1, R.drawable.on_boarding_vector_2, R.drawable.on_boarding_vector_3, R.drawable.on_boarding_vector_4};
    int[] onboard_bg = {R.drawable.bg1, R.drawable.bg2, R.drawable.bg3, R.drawable.bg4};

    int[] help_titles = {R.string.triage_title1, R.string.triage_title2, R.string.triage_title3, R.string.triage_title4};
    int[] help_subtitles = {R.string.triage_subtitle1, R.string.triage_subtitle2, R.string.triage_subtitle3, R.string.triage_subtitle4};
    int[] help_images = {R.drawable.triage_vector_1, R.drawable.triage_vector_2, R.drawable.triage_vector_3, R.drawable.triage_vector_4};
    int[] help_bg = {R.drawable.bg5, R.drawable.bg5, R.drawable.bg5, R.drawable.bg5}; // Re-use backgrounds



    int[] titles;

    int[] subtitles;

    int[] images;

    int[] bg;

    public OnBoardingAdapter(Context context, boolean isHelpTutorial) {
        this.context = context;

        if (isHelpTutorial) {
            // Load the "help" content arrays
            this.titles = help_titles;
            this.subtitles = help_subtitles;
            this.images = help_images;
            this.bg = help_bg;
        } else {
            // Load the default "onboarding" content arrays
            this.titles = onboard_titles;
            this.subtitles = onboard_subtitles;
            this.images = onboard_images;
            this.bg = onboard_bg;
        }
    }

    @Override
    public int getCount() {
        return titles.length;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        layoutinflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = layoutinflater.inflate(R.layout.slide,container,false);

        ImageView image = v.findViewById(R.id.slideImg);
        TextView title = v.findViewById(R.id.sliderTitle);
        TextView subtitle = v.findViewById(R.id.sliderSubtitle);
        ConstraintLayout layout = v.findViewById(R.id.sliderLayout);

        image.setImageResource(images[position]);
        title.setText(titles[position]);
        subtitle.setText(subtitles[position]);
        layout.setBackgroundResource(bg[position]);

        container.addView(v);

        return v;

    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((ConstraintLayout) object);
    }
}
