package com.nicolai.alarm_clock;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nicolai.alarm_clock.adapter_helper.SlideAdapterDetailsGuide;

public class Details_Guide extends AppCompatActivity {

    private LinearLayout mLinearLayout;
    private static int lastClicked;
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details__guide);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            Window window = this.getWindow();
            // clear FLAG_TRANSLUCENT_STATUS flag:
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            // finally change the color
            window.setStatusBarColor(ContextCompat.getColor(this,R.color.OnbordingPD));
        }

        mViewPager = findViewById(R.id.dg_viewpager);
        mLinearLayout = findViewById(R.id.dg_linearLayout);
        Button getStarted = findViewById(R.id.dg_done);
        SlideAdapterDetailsGuide sliderAdapter = new SlideAdapterDetailsGuide(this);

        mViewPager.setAdapter(sliderAdapter);
        mViewPager.addOnPageChangeListener(viewListener);

        addDotsIndicator(0);

        //Buttons
        getStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Details_Guide.this, MainActivity.class));
            }
        });
    }

    public void addDotsIndicator(int position){

        TextView[] nDots = new TextView[4];
        mLinearLayout.removeAllViews();

        for(int i = 0; i < nDots.length; i++){
            nDots[i] = new TextView(this);
            nDots[i].setText(Html.fromHtml("&#8226;"));
            nDots[i].setTextColor(getResources().getColor(R.color.colorTransparentWhite));

            nDots[i].setTextSize(30);

            mLinearLayout.addView(nDots[i]);
        }

        if(position != 0){
            nDots[position - 1].setTextColor(getResources().getColor(R.color.next_alarm_white));
        }
    }

    ViewPager.OnPageChangeListener viewListener = new ViewPager.OnPageChangeListener(){

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            //Damit wenn man z.B. von alarms zurück kommt nicht wieder bis dahin scrollen muss.
            lastClicked = position;
        }

        @Override
        public void onPageSelected(int position) {

            addDotsIndicator(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

    @Override
    protected void onPostResume() {
        super.onPostResume();
        String i = getIntent().getAction();
        if((lastClicked == 1 || lastClicked == 2 || lastClicked == 3) && getIntent().getAction() != null){
            mViewPager.setCurrentItem(lastClicked, true);
        }
    }
}
