package com.example.SmartAirGroup2.Helpers;

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

import android.net.Uri;
import android.widget.MediaController;
import android.widget.VideoView;

/**
 * A {@link PagerAdapter} for displaying different onboarding, help, and instructional flows.
 * This adapter dynamically changes its content (titles, images, videos, and input fields)
 * based on the 'type' provided in the constructor. It supports multiple flows:
 * - "initial": The standard app onboarding experience.
 * - "help": A triage flow to collect incident data (e.g., PEF, red flags).
 * - "technique": An instructional flow with videos to demonstrate inhaler technique.
 */
public class OnBoardingAdapter extends PagerAdapter{
    Context context;
    LayoutInflater layoutinflater;
    /**
     * A data object to hold and persist data collected during the 'help' (triage) flow.
     */
    TriageIncident incidentData;

    // --- Content for the 'initial' onboarding flow ---
    int[] onboard_titles = {R.string.title1, R.string.title2, R.string.title3, R.string.title4};
    int[] onboard_subtitles = {R.string.subtitle1, R.string.subtitle2, R.string.subtitle3, R.string.subtitle4};
    int[] onboard_images = {R.drawable.on_boarding_vector_1, R.drawable.on_boarding_vector_2, R.drawable.on_boarding_vector_3, R.drawable.on_boarding_vector_4};
    int[] onboard_bg = {R.drawable.bg1, R.drawable.bg2, R.drawable.bg3, R.drawable.bg4};

    // --- Content for the 'help' (triage) flow ---
    int[] help_titles = {R.string.triage_title1, R.string.triage_title2, R.string.triage_title3, R.string.triage_title4};
    int[] help_subtitles = {R.string.triage_subtitle1, R.string.triage_subtitle2, R.string.triage_subtitle3, R.string.triage_subtitle4};
    int[] help_images = {R.drawable.triage_vector_1, R.drawable.triage_vector_2, R.drawable.triage_vector_3, R.drawable.triage_vector_4};
    int[] help_bg = {R.drawable.bg5, R.drawable.bg5, R.drawable.bg5, R.drawable.bg5};

    // --- Content for the 'technique' flow ---
    int[] child_titles = {R.string.technique_title1, R.string.technique_title2, R.string.technique_title3, R.string.technique_title4};
    int[] child_subtitles = {R.string.technique_subtitle1, R.string.technique_subtitle2, R.string.technique_subtitle3, R.string.technique_subtitle4};
    int[] child_images = {R.drawable.triage_vector_1, R.drawable.triage_vector_2, R.drawable.triage_vector_3, R.drawable.triage_vector_4};
    int[] child_bg = {R.drawable.bg1, R.drawable.bg2, R.drawable.bg3, R.drawable.bg4};

    /**
     * Video resources for the 'technique' flow.
     * The first item is 0 as there is no video on the first slide.
     */
    int[] technique_videos = {
            0,
            R.raw.technique_step_1,
            R.raw.technique_step_2,
            R.raw.technique_step_3
    };

    // --- Arrays to hold the currently active content ---
    int[] titles;
    int[] subtitles;
    int[] images;
    int[] bg;

    /**
     * Constructs the adapter for a specific onboarding flow.
     *
     * @param context The application context.
     * @param type The type of flow to display ("initial", "help", or "technique").
     * @param incidentData A data object for pre-filling and storing data in the "help" flow.
     */
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
            default:
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

    /**
     * Creates the page for the given position. This method inflates the base slide layout
     * and then dynamically adds content (like videos or input forms) using a {@link ViewStub}
     * based on the current flow type and position.
     *
     * @param container The containing View in which the page will be shown.
     * @param position The page position to be instantiated.
     * @return An Object representing the new page. This is the View itself.
     */
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
        ViewStub inputStub = v.findViewById(R.id.input_stub);

        image.setImageResource(images[position]);
        title.setText(titles[position]);
        subtitle.setText(subtitles[position]);
        layout.setBackgroundResource(bg[position]);


        String currentType = "initial";
        if (this.titles == help_titles) {
            currentType = "help";
        } else if (this.titles == child_titles) {
            currentType = "technique";
        }

        switch (currentType) {
            case "technique":
                image.setVisibility(View.GONE);

                // Inflate video player for slides that have a video resource.
                if (position > 0 && position < technique_videos.length && technique_videos[position] != 0) {
                    inputStub.setLayoutResource(R.layout.slide_video_player);
                    View inflatedVideoLayout = inputStub.inflate();

                    VideoView videoView = inflatedVideoLayout.findViewById(R.id.onboard_video_player);

                    String videoPath = "android.resource://" + context.getPackageName() + "/" + technique_videos[position];
                    Uri uri = Uri.parse(videoPath);
                    videoView.setVideoURI(uri);

                    MediaController mediaController = new MediaController(context);
                    videoView.setMediaController(mediaController);
                    mediaController.setAnchorView(videoView);
                }
                break;

            case "help":
                image.setVisibility(View.VISIBLE);
                image.setImageResource(images[position]);

                // Inflate specific input layouts for the triage flow.
                switch (position) {
                    case 0: // PEF Input
                        inputStub.setLayoutResource(R.layout.triage_input_pef);
                        View inflatedPef = inputStub.inflate();
                        if (incidentData != null && incidentData.PEF != null) {
                            EditText pefText = inflatedPef.findViewById(R.id.edit_text_pef);
                            pefText.setText(incidentData.PEF);
                        }
                        break;
                    case 1: // Red Flags Input
                        inputStub.setLayoutResource(R.layout.triage_input_redflags);
                        View inflatedRedFlags = inputStub.inflate();
                        // Pre-fill checkboxes if data already exists.
                        if (incidentData != null) {
                            ((CheckBox) inflatedRedFlags.findViewById(R.id.checkbox_chest_pulling)).setChecked(incidentData.redflags.getOrDefault("Chest Pulling or Retraction", false));
                            ((CheckBox) inflatedRedFlags.findViewById(R.id.checkbox_blue_lips)).setChecked(incidentData.redflags.getOrDefault("Grey or Blue lips", false));
                            ((CheckBox) inflatedRedFlags.findViewById(R.id.checkbox_blue_nails)).setChecked(incidentData.redflags.getOrDefault("Grey or Blue nails", false));
                            ((CheckBox) inflatedRedFlags.findViewById(R.id.checkbox_breathing)).setChecked(incidentData.redflags.getOrDefault("Trouble breathing", false));
                            ((CheckBox) inflatedRedFlags.findViewById(R.id.checkbox_speaking)).setChecked(incidentData.redflags.getOrDefault("Trouble speaking", false));
                        }
                        break;
                }
                break;

            case "initial":
            default:
                image.setVisibility(View.VISIBLE);
                image.setImageResource(images[position]);
                break;
        }

        container.addView(v);

        return v;

    }

    /**
     * Removes a page for the given position from the container.
     *
     * @param container The containing View from which the page will be removed.
     * @param position The page position to be removed.
     * @param object The same object that was returned by {@link #instantiateItem(ViewGroup, int)}.
     */
    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((ConstraintLayout) object);
    }
}
