package com.nicolai.alarm_clock;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;

import android.app.AlertDialog;
import android.content.DialogInterface;
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

import com.nicolai.alarm_clock.adapter_helper.SlideAdapterOnboarding;

public class Onboarding extends AppCompatActivity {

    private LinearLayout mLinearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);


        //Dialog
        //first500();

        //Rest in onResume()

        //######### End first 500 ###################

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            Window window = this.getWindow();
            // clear FLAG_TRANSLUCENT_STATUS flag:
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            // finally change the color
            window.setStatusBarColor(ContextCompat.getColor(this,R.color.OnbordingPD));
        }

        ViewPager mViewPager = findViewById(R.id.ob_viewpager);
        mLinearLayout = findViewById(R.id.ob_linearLayout);
        Button createAccount = findViewById(R.id.ob_done);
        TextView logIn = findViewById(R.id.ob_logIn);
        SlideAdapterOnboarding sliderAdapter = new SlideAdapterOnboarding(this);

        mViewPager.setAdapter(sliderAdapter);
        mViewPager.addOnPageChangeListener(viewListener);

        addDotsIndicator(0);

        //Buttons
        createAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Onboarding.this, MainActivity.class));
                //startActivity(new Intent(Onboarding.this, Details_Guide.class));
            }
        });

        logIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Onboarding.this, MainActivity.class));
            }
        });
    }

    public void first500(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.first500)
                .setMessage(R.string.first500Text)
                .setPositiveButton(R.string.nice, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {}
                })
                .show();
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

        if(nDots.length > 0){
            nDots[position].setTextColor(getResources().getColor(R.color.next_alarm_white));
        }
    }

    ViewPager.OnPageChangeListener viewListener = new ViewPager.OnPageChangeListener(){

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

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

        /*Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                ConstraintLayout cl = findViewById(R.id.clOnboarding);
                //TODO Konfetti in content view
                CommonConfetti.rainingConfetti(cl, new int[] { Color.YELLOW, Color.BLUE})
                .stream(4000);

                /*final List<Bitmap> allPossibleConfetti = Utils.generateConfettiBitmaps(new int[] { R.color.colorPrimary, R.color.colorAccent  }, 40); // size
                // Alternatively, we provide some helper methods inside `Utils` to generate square, circle,
                // and triangle bitmaps.
                // Utils.generateConfettiBitmaps(new int[] { Color.BLACK }, 20); // size

                final int numConfetti = allPossibleConfetti.size();
                final ConfettoGenerator confettoGenerator = new ConfettoGenerator() {
                    @Override
                    public Confetto generateConfetto(Random random) {
                        final Bitmap bitmap = allPossibleConfetti.get(random.nextInt(numConfetti));
                        return new BitmapConfetto(bitmap);
                    }
                };

                //Source
                final int containerMiddleX = cl.getWidth() / 2;
                final int containerMiddleY = cl.getHeight() / 2;
                final ConfettiSource confettiSource = new ConfettiSource(containerMiddleX, containerMiddleY);

                new ConfettiManager(Onboarding.this, confettoGenerator, confettiSource, cl)
                        .setEmissionDuration(3000)
                        .setEmissionRate(200)
                        .setVelocityX(40, 30)
                        .setVelocityY(200)
                        .setRotationalVelocity(180, 180)
                        //.enableFadeOut(Utils.getDefaultAlphaInterpolator())
                        //.setTouchEnabled(true)
                        .animate();
            }

        }, 500);*/ // 5000ms delay
    }

    protected void onPause(){
        super.onPause();
    }
}
