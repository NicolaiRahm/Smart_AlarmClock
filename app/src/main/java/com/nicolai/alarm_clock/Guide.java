package com.nicolai.alarm_clock;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class Guide extends AppCompatActivity {

    private ImageView iVoice, iTimer, iSound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);

        //Initialisierung Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarGuide);
        setSupportActionBar(toolbar);
        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();
        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setTitle(R.string.guidePlus);

        iVoice = findViewById(R.id.iVoice);
        iTimer = findViewById(R.id.iTimer);
        iSound = findViewById(R.id.iSound);

        Button button = findViewById(R.id.guide_to_show);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //startActivity(new Intent(Guide.this, Onboarding.class));
                startActivity(new Intent(Guide.this, Details_Guide.class));
            }
        });
    }

    public void abstimmung(View view){
        TextView clickedView = (TextView) view;

        switch (clickedView.getId()){
            case R.id.gUpdateVoice:
                iVoice.setVisibility(View.VISIBLE);
                iTimer.setVisibility(View.GONE);
                iSound.setVisibility(View.GONE);
                break;

            case R.id.gUpdateTimer:
                iTimer.setVisibility(View.VISIBLE);
                iSound.setVisibility(View.GONE);
                iVoice.setVisibility(View.GONE);
                break;

            case R.id.gUpdateShareSound:
                iSound.setVisibility(View.VISIBLE);
                iVoice.setVisibility(View.GONE);
                iTimer.setVisibility(View.GONE);
                break;
        }
    }
}
