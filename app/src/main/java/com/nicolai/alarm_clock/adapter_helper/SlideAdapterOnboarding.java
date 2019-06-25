package com.nicolai.alarm_clock.adapter_helper;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.jinatonic.confetti.CommonConfetti;
import com.nicolai.alarm_clock.R;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewpager.widget.PagerAdapter;

public class SlideAdapterOnboarding extends PagerAdapter {

    private Context context;
    private LayoutInflater layoutInflater;

    private int[] slide_images = {

            R.mipmap.ic_launcher_round,
            R.drawable.ic_message_white_100dp,
            R.drawable.ic_share_white_100dp,
            R.drawable.ic_mic_white_100dp
    };

    private int[] slide_headings = {
            R.string.oHeader1,
            R.string.oHeader2,
            R.string.oHeader3,
            R.string.oHeader4
    };

    private int[] slide_descs = {
            R.string.oDesc1,
            R.string.oDesc2,
            R.string.oDesc3,
            R.string.oDesc4
    };

    public SlideAdapterOnboarding(Context context){
        this.context = context;
    }

    @Override
    public int getCount() {
        return slide_headings.length;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {

        layoutInflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.onboarding_content, container, false);

        ImageView imageView = view.findViewById(R.id.onboarding_roundImg);
        TextView headerView = view.findViewById(R.id.ob_header);
        TextView descView = view.findViewById(R.id.ob_descs);

        if(position == 0){
            imageView.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
        }

        imageView.setImageResource(slide_images[position]);
        headerView.setText(context.getString(slide_headings[position]));
        descView.setText(context.getText(slide_descs[position]));

        container.addView(view);

        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {

        container.removeView((ConstraintLayout) object);
    }
}
