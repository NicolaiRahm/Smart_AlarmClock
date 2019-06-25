package com.nicolai.alarm_clock;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

public class Legal extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_legal);

        //Initialisierung Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarLegal);
        setSupportActionBar(toolbar);
        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();
        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setTitle(R.string.impressum);
    }

    public void displayLegal(View view){
        /*Intent i = new Intent(this, DisplayLegal.class);
        if(view.getId() == R.id.button){
            i.putExtra("title", getString(R.string.impressum));
        }else if(view.getId() == R.id.button2){
            i.putExtra("title", getString(R.string.terms));
        }else*/ if(view.getId() == R.id.button3){
            String url = "https://nicolairahm.wixsite.com/apparently/tucktack-datenschutzbestimmungen";
            Intent i2 = new Intent(Intent.ACTION_VIEW);
            i2.setData(Uri.parse(url));
            startActivity(i2);
            //i.putExtra("title", getString(R.string.ploicy));
        }

        //startActivity(i);
    }
}
