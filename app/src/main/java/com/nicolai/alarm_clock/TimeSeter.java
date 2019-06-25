package com.nicolai.alarm_clock;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.AlarmClock;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;
import com.nicolai.alarm_clock.pojos.WeckerPOJO;
import com.nicolai.alarm_clock.room_database.WeckerDatabase;
import com.nicolai.alarm_clock.util.AlarmUtil;
import com.nicolai.alarm_clock.viewmodels.FactoryTimeseter;
import com.nicolai.alarm_clock.viewmodels.ViewModel_TimeSeter;

import java.util.List;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProviders;

public class TimeSeter extends AppCompatActivity {

    private int weckerID;
    private int hour = 7;
    private int minute = 30;
    private String days = "xxxxxxxxxxxxxx";
    private boolean repeat;

    private boolean neu, assisted;

    private Alarm_Clock alarm_clock;
    private FragmentTransaction fragmentTransaction;

    private WeckerDatabase database;
    private ViewModel_TimeSeter viewModel;

    private WeckerPOJO currentWecker;

    private static final String ALARM_CLOCK_TAG = "alarm_clock_fragment";

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //Remove Overdraw with Them background by activity
        getWindow().setBackgroundDrawable(null);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_seter);

        //TabLayout
        TabLayout tl = (TabLayout) findViewById(R.id.tab_Layout);
        tl.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            //Speichern
            public void onTabSelected(TabLayout.Tab tab) {
               save();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            //Abbrechen
            public void onTabReselected(TabLayout.Tab tab) {
                alertDialog();
            }
        });


        //Daten für Alarm laden
        AppExecutor.getInstance().diskI0().execute(new Runnable() {
            @Override
            public void run() {

                //calming_alarm database instance
                database = WeckerDatabase.getInstance(getApplicationContext());

                //ID FÜR FRAGMENT
                //Falls neuer Wecker mit floating Actionbutton
                if(getIntent().getAction() != null && getIntent().getAction().equals(AlarmClock.ACTION_SET_ALARM)){

                    //Stunde schon angegeben?!
                    if (getIntent().hasExtra(AlarmClock.EXTRA_HOUR)) {
                        hour = getIntent().getIntExtra(AlarmClock.EXTRA_HOUR, 7);
                    }
                    //Minute schon angegeben?!
                    if (getIntent().hasExtra(AlarmClock.EXTRA_MINUTES)) {
                        minute = getIntent().getIntExtra(AlarmClock.EXTRA_MINUTES, 30);
                    }

                    days = "xxxxxxxxxxxxxx";
                    //Tage und wöchentliche Wiederholung angegeben?!
                    if (getIntent().hasExtra(AlarmClock.EXTRA_DAYS)) {
                        //Sunday = 1, ...
                        List<Integer> tage = getIntent().getIntegerArrayListExtra(AlarmClock.EXTRA_DAYS);

                        StringBuilder editDays = new StringBuilder(days);

                        for(int day : tage){
                            switch(day){
                                case 1: editDays.setCharAt(12, 's'); editDays.setCharAt(13, 'o'); break;//Sonntag
                                case 2: editDays.setCharAt(0, 'm'); editDays.setCharAt(1, 'o'); break;
                                case 3: editDays.setCharAt(2, 'd'); editDays.setCharAt(3, 'i'); break;
                                case 4: editDays.setCharAt(4, 'm'); editDays.setCharAt(5, 'i'); break;
                                case 5: editDays.setCharAt(6, 'd'); editDays.setCharAt(7, 'o'); break;
                                case 6: editDays.setCharAt(8, 'f'); editDays.setCharAt(9, 'r'); break;
                                case 7: editDays.setCharAt(10, 's'); editDays.setCharAt(11, 'a'); break;//Samstag
                                default: break;
                            }
                        }

                        //Gebautetagesangabe wieder zu String
                        days = editDays.toString();
                        //Wöchentliche Wiederholung
                        repeat = true;

                        assisted = true;
                    }

                    neu = true;

                }else if(getIntent().getExtras() != null){
                    Bundle extras = getIntent().getExtras();
                    if(extras.containsKey("neu")){
                        neu = true;
                    }else{
                        weckerID = extras.getInt("alt", 0);
                    }
                }


                if(neu){
                    //Initialisierung SharedPreferences plus Editor
                    SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

                    //Standard-/Defaulteinstellungen
                    long intervall = mSharedPreferences.getLong(getString(R.string.sharedIntervall), 0);
                    int anzahl = mSharedPreferences.getInt(getString(R.string.sharedAnzahl), 1);

                    //Default volume
                    AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                    int halfVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING) / 2;
                    int volume = mSharedPreferences.getInt(getString(R.string.sharedVolume), halfVolume);

                    //Default Ringtone
                    Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                    String defaultTone = uri.toString();
                    String alarmSound = mSharedPreferences.getString(getString(R.string.sharedAlarmsound), defaultTone);

                    boolean voiceCtrl = false;
                    //Hat zumindest beim testen mit app löschen und mehreren accounts rumgebackt
                    if(ContextCompat.checkSelfPermission(TimeSeter.this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED){
                        voiceCtrl = mSharedPreferences.getBoolean(getString(R.string.sharedVoicecontrol), false);
                    }

                    int duration = mSharedPreferences.getInt(getString(R.string.sharedDuration), 30);

                    String newMessageW = getString(R.string.sms_aufgestanden, hour, String.valueOf(minute));
                    if(minute< 10){
                        newMessageW = getString(R.string.sms_aufgestanden, hour, "0" + minute);
                    }
                    String newMessageV = getString(R.string.sms_verschlafen, hour, String.valueOf(minute));
                    if(minute< 10){
                        newMessageV = getString(R.string.sms_verschlafen, hour, "0" + minute);
                    }

                    int secondsBis = mSharedPreferences.getInt(getString(R.string.sharedTimeBisVerschlafen), 60);

                    //onOff a steht für aus, l für laufend #### shared l steht für "local", s für "sharedNew", u für "sharedUpdated", d für "sharedDeleted", b für "bestätigt", v für "von mir", t für shared+Message, m only message
                    //forme 1 = ja auch für mich, 0 Nein
                    //speechControl "n" == Nein, "y" == Ja
                    currentWecker = new WeckerPOJO(0, "", days, repeat, hour, minute, anzahl, intervall, duration, volume, alarmSound,
                            voiceCtrl, 'l', "{}", "{}", "{}", true, "",
                            "", "", "", true,
                            newMessageV, newMessageW, secondsBis);

                }else {
                    currentWecker = database.weckerDao().findRowByID(weckerID);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //ViewModel/Factory
                        FactoryTimeseter factory = new FactoryTimeseter(currentWecker, neu, assisted);
                        viewModel = ViewModelProviders.of(TimeSeter.this, factory).get(ViewModel_TimeSeter.class);

                        alarm_clock = new Alarm_Clock();
                        //Speichern teilen = new Speichern();
                        fragmentTransaction = getSupportFragmentManager().beginTransaction();
                        fragmentTransaction.add(R.id.alarmClock_container, alarm_clock, ALARM_CLOCK_TAG);
                        fragmentTransaction.commit();
                    }
                });
            }
        });
    }

    public void save(){

        AlarmUtil.saveAlarm(getApplicationContext(), currentWecker, viewModel.isNeu());

        Alarm_Clock myFragment = (Alarm_Clock) getSupportFragmentManager().findFragmentByTag(ALARM_CLOCK_TAG);
        if (myFragment != null) {
            String klingelIn = myFragment.getKlingeltIn();
            if(!klingelIn.isEmpty()){
                Toast.makeText(TimeSeter.this, getString(R.string.klingeltInToast, klingelIn), Toast.LENGTH_LONG).show();
            }
        }

        //Back to MainActivity
        //fragmentTransaction = getSupportFragmentManager().beginTransaction();
        //fragmentTransaction.remove(alarm_clock).commit();
        NavUtils.navigateUpFromSameTask(TimeSeter.this);
    }

    //Alertdialog zum verwerfen der Aenderungen oder des Alarms bei Ersterstellung
    public void alertDialog(){
        String alertMessage = getString(R.string.realy2);
        if(neu) {
            alertMessage = getString(R.string.realy);
        }

        new AlertDialog.Builder(this)
                .setMessage(alertMessage)
                .setPositiveButton(R.string.Ja, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //Backe to homeScreen
                        //fragmentTransaction = getSupportFragmentManager().beginTransaction();
                        //fragmentTransaction.remove(alarm_clock).commit();
                        NavUtils.navigateUpFromSameTask(TimeSeter.this);
                    }
                }).setNegativeButton(R.string.Nein, null).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override //Homebutton --> Alertdialog
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //case R.id.action_settings:
            //User chose the "Settings" item, show the app settings UI...
            //return true;

            case android.R.id.home:
                alertDialog();
                return true;

            //case R.id.action_favorite:
            //save();

            //return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    //Hardwear backButton
    public void onBackPressed(){
        alertDialog();
    }

}
