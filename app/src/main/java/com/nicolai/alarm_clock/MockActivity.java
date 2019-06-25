package com.nicolai.alarm_clock;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProviders;

import android.os.Bundle;

import com.nicolai.alarm_clock.pojos.WeckerPOJO;
import com.nicolai.alarm_clock.viewmodels.FactoryTimeseter;
import com.nicolai.alarm_clock.viewmodels.ViewModel_TimeSeter;

public class MockActivity extends AppCompatActivity {

    private Fragment fragment;
    private WeckerPOJO testWecker;
    private FragmentTransaction transaction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mock);

        final int titleID = getIntent().getIntExtra("type", R.string.dgHeader0);

        //Initialisierung Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.ma_toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setTitle(titleID);

        //Daten für Alarm laden
        //TODO warum klappt das nur mit thread (Viewmodel.observe in alarm_clock)
        AppExecutor.getInstance().diskI0().execute(new Runnable() {
            @Override
            public void run() {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        switch (titleID){
                            case R.string.dgHeader1: fragment = new Alarms(); break;
                            case R.string.dgHeader2:
                                testWecker = new WeckerPOJO(5, getString(R.string.dgMockAlarmName), "xxxxxxxxfrxxxx", true,
                                        7, 30, 3, 300000, 30, 1, "",
                                        true, 'l', "{}", "{}", "{}",
                                        true, "", "", "", "", true,
                                        "", "", 30);

                                FactoryTimeseter factory = new FactoryTimeseter(testWecker, false, false);
                                ViewModelProviders.of(MockActivity.this, factory).get(ViewModel_TimeSeter.class);

                                fragment = new Alarm_Clock(); break;
                            case R.string.dgHeader3: fragment = new Alarms(); break;
                            default: fragment = new Alarms(); break;
                        }

                        Bundle arguments = new Bundle();
                        arguments.putString( "mock", "mock");
                        fragment.setArguments(arguments);

                        transaction = getSupportFragmentManager().beginTransaction();
                        transaction.add(R.id.ma_container, fragment, "MOCK");
                        transaction.commit();
                    }
                });
            }});
    }

    @Override
    protected void onStop() {
        super.onStop();

        transaction.remove(fragment);
    }
}
