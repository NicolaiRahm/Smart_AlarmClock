package com.nicolai.alarm_clock.adapter_helper;

import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nicolai.alarm_clock.ControlCenter;
import com.nicolai.alarm_clock.MockActivity;
import com.nicolai.alarm_clock.R;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.viewpager.widget.PagerAdapter;

public class SlideAdapterDetailsGuide extends PagerAdapter {

    private Context context;
    private LayoutInflater layoutInflater;

    private int[] slide_images = {

            R.drawable.main_page,
            R.drawable.main_page,
            R.drawable.set_alarm,
            R.drawable.control_center,
            R.drawable.main_page
    };

    private int[] slide_headings = {
            R.string.dgHeader0,
            R.string.dgHeader1,
            R.string.dgHeader2,
            R.string.dgHeader3,
            R.string.dgHeader4
    };

    private int[] slide_desc = {
            R.string.dgWelcomeDesc,
            R.string.dgClickImage,
            R.string.dgClickImage,
            R.string.dgClickImage,
            R.string.fullVersionText
    };

    public SlideAdapterDetailsGuide(Context context){
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
    public Object instantiateItem(@NonNull final ViewGroup container, final int position) {

        layoutInflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.details_guide_content, container, false);

        final ImageView imageView = view.findViewById(R.id.dg_imageView);
        final TextView headerView = view.findViewById(R.id.dg_header);
        final TextView desc = view.findViewById(R.id.dgDesc);

        imageView.setImageResource(slide_images[position]);

        //Descriptions under header
        if(position == 0){
            desc.setText(slide_desc[position]);
        }else{
            desc.setText(slide_desc[position]);
        }

        //Set image gone and desc TextView attributes for welcome and full version
        if(position == 0 || position == 4){
            imageView.setVisibility(View.GONE);
            desc.setTextSize(TypedValue.COMPLEX_UNIT_SP,18);

            ConstraintLayout constraintLayout = view.findViewById(R.id.dgConstraintLayout);

            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(constraintLayout);

            constraintSet.connect(desc.getId(), ConstraintSet.TOP, headerView.getId(), ConstraintSet.BOTTOM, 200);
            constraintSet.applyTo(constraintLayout);

            if(position == 4){
                desc.setMovementMethod(new ScrollingMovementMethod());
            }else{
                desc.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }

        //Set Username and headers
        if(position == 0){
            headerView.setText(context.getString(slide_headings[position], ""));
            DatabaseReference username = FirebaseDatabase.getInstance().getReference().child("users").child(FirebaseAuth.getInstance().getUid());
            username.child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    headerView.setText(context.getString(slide_headings[position], dataSnapshot.getValue().toString()));
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    headerView.setText(context.getString(slide_headings[position], ""));
                }
            });
        }else{
            headerView.setText(context.getString(slide_headings[position]));
        }

        //On image clicked
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(position == 1 || position == 2){
                    Intent intent = new Intent(context, MockActivity.class);
                    intent.putExtra("type", slide_headings[position]);
                    context.startActivity(intent);
                }else if(position == 3){
                    Intent intent = new Intent(context, ControlCenter.class);
                    intent.putExtra("type", slide_headings[position]);
                    context.startActivity(intent);
                }
            }
        });

        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {

        container.removeView((ConstraintLayout) object);
    }
}

