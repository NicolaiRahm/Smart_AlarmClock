package com.nicolai.alarm_clock.adapter_helper;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.nicolai.alarm_clock.util.SoundUtil;
import com.nicolai.alarm_clock.util.TimeUtil;

import androidx.databinding.BindingAdapter;
import androidx.databinding.BindingConversion;

public class DataBinding_CustomeAdapter {

    @BindingAdapter("soundTitle")
    public static void soundTitle(TextView textView, String soundUri){
        String title = SoundUtil.soundTitle(textView.getContext(), soundUri);
        textView.setText(title);
    }

    @BindingAdapter("timerTimeView")
    public static void timerTimeView(TextView textView, long time){
        textView.setText(TimeUtil.timerTimeView(textView.getContext(), time));
    }

    @BindingAdapter("app:srcCompat")
    public static void fabImg(FloatingActionButton fab, int resource){
        fab.setImageResource(resource);
    }

    @BindingAdapter("ends")
    public static void ends(TextView textView, long endsAt){
        textView.setText(TimeUtil.endsAt(textView.getContext(), endsAt));
    }

    //Bsp.
    @BindingConversion
    public static ColorDrawable convertColorToDrawable(int drawableId) {
        return new ColorDrawable(drawableId);
    }

    //For databinding with images
    @BindingAdapter("imageUrl")
    public static void setIImageResource(ImageView imageView, int imageUrl){

        Context context = imageView.getContext();

        /*RequestOptions options = new RequestOptions()
                .placeholder(androidx.core.R.drawable.notification_icon_background)
                .error(androidx.core.R.drawable.notification_icon_background);

        Glide.with(context)
                .setDefaultRequestOption(option)
                .load(imageUrl)
                .into(view);*/
    }
}
