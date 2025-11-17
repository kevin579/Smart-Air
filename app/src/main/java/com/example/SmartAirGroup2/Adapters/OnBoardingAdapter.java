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

    int[] titles = {
            R.string.title1,
            R.string.title2,
            R.string.title3,
            R.string.title4
    };

    int[] subtitles = {
            R.string.subtitle1,
            R.string.subtitle2,
            R.string.subtitle3,
            R.string.subtitle4
    };

    int[] images = {
            R.drawable.on_boarding_vector_1,
            R.drawable.on_boarding_vector_2,
            R.drawable.on_boarding_vector_3,
            R.drawable.on_boarding_vector_4
    };

    int[] bg = {
            R.drawable.bg1,
            R.drawable.bg2,
            R.drawable.bg3,
            R.drawable.bg4
    };

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
