package com.example.SmartAirGroup2.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.ViewStub;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewpager.widget.PagerAdapter;
import com.example.SmartAirGroup2.R;
import com.example.SmartAirGroup2.models.TriageIncident;


import com.example.SmartAirGroup2.R;
public class OnBoardingAdapter extends PagerAdapter{
    Context context;
    LayoutInflater layoutinflater;
    TriageIncident incidentData;

    int[] onboard_titles = {R.string.title1, R.string.title2, R.string.title3, R.string.title4};
    int[] onboard_subtitles = {R.string.subtitle1, R.string.subtitle2, R.string.subtitle3, R.string.subtitle4};
    int[] onboard_images = {R.drawable.on_boarding_vector_1, R.drawable.on_boarding_vector_2, R.drawable.on_boarding_vector_3, R.drawable.on_boarding_vector_4};
    int[] onboard_bg = {R.drawable.bg1, R.drawable.bg2, R.drawable.bg3, R.drawable.bg4};

    int[] help_titles = {R.string.triage_title1, R.string.triage_title2, R.string.triage_title3, R.string.triage_title4};
    int[] help_subtitles = {R.string.triage_subtitle1, R.string.triage_subtitle2, R.string.triage_subtitle3, R.string.triage_subtitle4};
    int[] help_images = {R.drawable.triage_vector_1, R.drawable.triage_vector_2, R.drawable.triage_vector_3, R.drawable.triage_vector_4};
    int[] help_bg = {R.drawable.bg5, R.drawable.bg5, R.drawable.bg5, R.drawable.bg5};

    int[] child_titles = {R.string.triage_title1, R.string.triage_title2, R.string.triage_title3, R.string.triage_title4};
    int[] child_subtitles = {R.string.triage_subtitle1, R.string.triage_subtitle2, R.string.triage_subtitle3, R.string.triage_subtitle4};
    int[] child_images = {R.drawable.triage_vector_1, R.drawable.triage_vector_2, R.drawable.triage_vector_3, R.drawable.triage_vector_4};
    int[] child_bg = {R.drawable.bg5, R.drawable.bg5, R.drawable.bg5, R.drawable.bg5}; // Re-use backgrounds




    int[] titles;

    int[] subtitles;

    int[] images;

    int[] bg;

    public OnBoardingAdapter(Context context, String type, TriageIncident incidentData) {
        this.context = context;
        this.incidentData = incidentData;

        switch (type) {
            case "technique":
            case "child":
                this.titles = child_titles;
                this.subtitles = child_subtitles;
                this.images = child_images;
                this.bg = child_bg;
                break;

            case "help":
                this.titles = help_titles;
                this.subtitles = help_subtitles;
                this.images = help_images;
                this.bg = help_bg;
                break;

            case "initial":
            default: // Default to the initial onboarding
                this.titles = onboard_titles;
                this.subtitles = onboard_subtitles;
                this.images = onboard_images;
                this.bg = onboard_bg;
                break;
        }
    }

    @Override
    public int getCount() {
        return titles.length;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == (ConstraintLayout) object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        layoutinflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = layoutinflater.inflate(R.layout.slide,container,false);
        v.setTag("page" + position);

        ImageView image = v.findViewById(R.id.slideImg);
        TextView title = v.findViewById(R.id.sliderTitle);
        TextView subtitle = v.findViewById(R.id.sliderSubtitle);
        ConstraintLayout layout = v.findViewById(R.id.sliderLayout);

        image.setImageResource(images[position]);
        title.setText(titles[position]);
        subtitle.setText(subtitles[position]);
        layout.setBackgroundResource(bg[position]);

        ViewStub inputStub = v.findViewById(R.id.input_stub);


        boolean isHelp = (this.titles == help_titles); // Simple check to see if it's the help tutorial
        if (isHelp) {
            switch (position) {
                case 0:
                    inputStub.setLayoutResource(R.layout.triage_input_pef);
                    View inflatedPef = inputStub.inflate();
                    if (incidentData != null && incidentData.PEF != null) {
                        EditText pefText = inflatedPef.findViewById(R.id.edit_text_pef);
                        pefText.setText(incidentData.PEF);
                    }
                    break;
                case 1:
                    inputStub.setLayoutResource(R.layout.triage_input_redflags);
                    View inflatedRedFlags = inputStub.inflate();
                    if (incidentData != null) {
                        ((CheckBox) inflatedRedFlags.findViewById(R.id.checkbox_chest_pulling)).setChecked(incidentData.redflags.getOrDefault("Chest Pulling or Retraction", false));
                        ((CheckBox) inflatedRedFlags.findViewById(R.id.checkbox_blue_lips)).setChecked(incidentData.redflags.getOrDefault("Grey or Blue lips", false));
                        ((CheckBox) inflatedRedFlags.findViewById(R.id.checkbox_blue_nails)).setChecked(incidentData.redflags.getOrDefault("Grey or Blue nails", false));
                        ((CheckBox) inflatedRedFlags.findViewById(R.id.checkbox_breathing)).setChecked(incidentData.redflags.getOrDefault("Trouble breathing", false));
                        ((CheckBox) inflatedRedFlags.findViewById(R.id.checkbox_speaking)).setChecked(incidentData.redflags.getOrDefault("Trouble speaking", false));
                    }
                    break;

            }
        }

        container.addView(v);

        return v;

    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((ConstraintLayout) object);
    }
}
