package com.nicolai.alarm_clock;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.nicolai.alarm_clock.pojos.FB;
import com.nicolai.alarm_clock.pojos.WeckerPOJO;
import com.nicolai.alarm_clock.receiver_service.ForegroundService_AlarmSound;
import com.nicolai.alarm_clock.util.AlarmUtil;
import com.nicolai.alarm_clock.util.NotificationUtil;
import com.nicolai.alarm_clock.viewmodels.FactoryWakeUp;
import com.nicolai.alarm_clock.viewmodels.ViewModel_WakeUp;

import net.gotev.speech.GoogleVoiceTypingDisabledException;
import net.gotev.speech.Speech;
import net.gotev.speech.SpeechDelegate;
import net.gotev.speech.SpeechRecognitionNotAvailable;
import net.gotev.speech.SpeechUtil;
import net.gotev.speech.ui.SpeechProgressView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProviders;


public class WakeUp extends AppCompatActivity implements SpeechDelegate{

    private ImageButton snoozeButton;
    private FloatingActionButton turnOffButton;

    private Handler turnOffHandler = new Handler();
    private boolean noDays, appMessage = false, smsMessage = false, justSnoozed;
    protected int iD, progressVolume, alarmAnzahl, duration, alarmCount;
    private long intervall;
    private WeckerPOJO klingelnderWecker;
    private TextView weckString;

    private HashMap<String, String> appMessageMap = new HashMap<>();
    private HashMap<String, String> smsMessageMap = new HashMap<>();
    private char iAmEmpfaenger;

    private Speech speech;
    private androidx.constraintlayout.widget.Group g;
    private Button bt5;
    private Handler handlerSpeech, handlerSpeech2, handlerShutdown;

    private Window win;
    private boolean speechControl;

    private SpeechProgressView speechProgressView;
    private List<String> offWords = new ArrayList<>();
    private List<String> snoozeWords = new ArrayList<>();

    private ViewModel_WakeUp viewModel;

    //Wenn verschlafen
    private boolean sleeping;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wake_up);

        win = getWindow();
        win.setBackgroundDrawable(null);
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        viewModel = ViewModelProviders.of(this, new FactoryWakeUp(getApplication(), getIntent().getExtras().getInt("ID"))).get(ViewModel_WakeUp.class);

        //noch 5 und 10 button groupe
        //Noch 5 und noch 10 button
        g = findViewById(R.id.bt15Group);

        klingelnderWecker = viewModel.getKlingelderWecker();

        //Null ?! TODO besser, sollte nie null sein
        if(klingelnderWecker == null){
            weckString.setText("Error");
            //Ringtone starten
            foregroundService(ForegroundService_AlarmSound.ACTION_START);

            //Off Button
            turnOffButton = findViewById(R.id.wakeUp_offButton);
            turnOffButton.setOnClickListener(v -> {
                turnOffButton.setEnabled(false);
                //Wenn noch nicht verschlafen
                if(!sleeping){
                    wokeUp(true);
                }else{
                    closedAfterVer();
                }
            });

            //Remove Notification from receiver
            NotificationUtil.cancelAlarmNfc(getApplicationContext(), NotificationUtil.RECEIVER_NOTIFICATION_ID);

            return;
        }
        iD = klingelnderWecker.getId();

        //Alarmlautstärke setzen
        intervall = klingelnderWecker.getIntervall();
        alarmAnzahl = klingelnderWecker.getAnzahl();
        progressVolume = klingelnderWecker.getVolume();
        duration = klingelnderWecker.getDuration();

        //Ringtone starten
        foregroundService(ForegroundService_AlarmSound.ACTION_START);

        speechControl = klingelnderWecker.isSpeech_control();
        //Google Speechrecognizer
        if(speechControl){
            //Speech initialisieren
            Speech.init(this, getPackageName());
            speech = Speech.getInstance();
            //displaySpeechRecognizer();
            startSpeechRecog();
        }

        //Daten des Weckers abfragen und aufbereiten
        final String name = klingelnderWecker.getName();
        final int hour = klingelnderWecker.getHour();
        final int minute = klingelnderWecker.getMinute();

        String weckzeit;
        if(minute < 10){
            weckzeit = (hour + ":0" + minute);
        }else{
            weckzeit = (hour + ":" + minute);
        }

        String appMessageJason = klingelnderWecker.getShared_message_con();
        iAmEmpfaenger = klingelnderWecker.getShared();
        String smsMessageJason = klingelnderWecker.getShared_sms_con();

        if(!appMessageJason.isEmpty() && !appMessageJason.equals("{}")){
            appMessage = true;
        }
        if(!smsMessageJason.isEmpty() && !smsMessageJason.equals("{}")){
            smsMessage = true;
        }
        //MessageCon in HashMap
        if(appMessage){
            try {
                JSONObject json = new JSONObject(appMessageJason);
                JSONArray names = json.names();
                for (int i = 0; i < names.length(); i++) {
                    String key = names.getString(i);
                    appMessageMap.put(key, json.optString(key));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        //SmsCon in HashMap
       if(smsMessage){
           try {
               JSONObject json = new JSONObject(smsMessageJason);
               JSONArray names = json.names();
               for (int i = 0; i < names.length(); i++) {
                   String key = names.getString(i);
                   smsMessageMap.put(key, json.optString(key));
               }
           } catch (JSONException e) {
               e.printStackTrace();
           }
       }

       //WLAN an wenn Alarm für mich oder Message von mir und erlaubt
       wifi();

        //Update status to "online" if shared
        viewModel.messageTo_Minder_Sender(appMessage, iAmEmpfaenger, FB.online);

        final TaskCompletionSource<Boolean> source = new TaskCompletionSource<>();
        Task<Boolean> task = source.getTask();
        task.addOnSuccessListener(this, noDay -> {
            noDays = noDay;
        });

        AlarmUtil.killDay(getApplicationContext(), klingelnderWecker, source);

        //Off Button
        turnOffButton = findViewById(R.id.wakeUp_offButton);
        turnOffButton.setOnClickListener(v -> {
            turnOffButton.setEnabled(false);
            //Wenn noch nicht verschlafen
            if(!sleeping){
                wokeUp(true);
            }else{
                closedAfterVer();
            }
        });

       weckString = findViewById(R.id.wakeUp_textView);
       weckString.setText(name + "\n" + weckzeit);


       //Damit Screen nich die ganze Zeit an bleibt
        Runnable runnable = () -> {
            win.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        };

        if(intervall != 0){
            turnOffHandler.postDelayed(runnable, intervall * alarmAnzahl + 60000);
        }else{
            turnOffHandler.postDelayed(runnable, 180000);
        }


       //Bildschirm wird 3 min nach letztem Alarm ausgeschaltet ++ SnoozeButton
       if(intervall != 0 && alarmCount < alarmAnzahl){
            //Für noch 5||10 Min
           g.setVisibility(View.GONE);
           //SnoozeButton
           snoozeButton = findViewById(R.id.wakeUp_snooze);
           snoozeButton.setVisibility(View.VISIBLE);
           snoozeButton.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View v) {
                   snooze(0L);
               }
           });
       }

       //Noch 5 und noch 10 OnClick
        bt5 = findViewById(R.id.bt5);
        Button bt10 = findViewById(R.id.bt10);

        bt5.setOnClickListener(v -> {
            AlarmUtil.setRepeating(getApplicationContext(), klingelnderWecker.getId(), AlarmUtil.FIVE_MIN_SNOOZE);
            snooze(AlarmUtil.FIVE_MIN_SNOOZE);
        });

        bt10.setOnClickListener(v -> {
            AlarmUtil.setRepeating(getApplicationContext(), klingelnderWecker.getId(), AlarmUtil.TEN_MIN_SNOOZE);
            snooze(AlarmUtil.TEN_MIN_SNOOZE);
        });

        //Remove Notification from receiver
        NotificationUtil.cancelAlarmNfc(getApplicationContext(), NotificationUtil.RECEIVER_NOTIFICATION_ID);
    }

    public void snooze(long snoozeDuration){
        Intent i = new Intent(getApplicationContext(), ForegroundService_AlarmSound.class);
        i.setAction(ForegroundService_AlarmSound.ACTION_SNOOZE);
        i.putExtra("snoozeDuartion", snoozeDuration);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            startForegroundService(i);
        }else{
            startService(i);
        }

        if(speech != null && speechControl) speech.shutdown();
        viewModel.messageTo_Minder_Sender(appMessage, iAmEmpfaenger, FB.schlummert);
        moveTaskToBack (true);
    }

    private void foregroundService(String action){
        Intent i = new Intent(getApplicationContext(), ForegroundService_AlarmSound.class);
        i.setAction(action);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            startForegroundService(i);
        }else{
            startService(i);
        }
    }

    private void repeatingAlarm(){
        //Repeating Alarm
        if(alarmCount < alarmAnzahl && intervall != 0){
            //Calendar --> Alarmmanager an Broadcastreceiver (AlarmIntervallReceiver)
            AlarmUtil.setRepeating(getApplicationContext(), klingelnderWecker.getId(), intervall);
        }else if(intervall != 0){
            g.setVisibility(View.VISIBLE);
            snoozeButton.setVisibility(View.GONE);
        }
    }

    @SuppressLint("WifiManagerLeak")
    public void wifi() {
        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(WakeUp.this);
        //InternetConnection?!  !!Darf nicht in thread!!
        if ((appMessage || iAmEmpfaenger == 'b') && mSharedPreferences.getBoolean(getString(R.string.sharedWifi), false)) {
            WifiManager wifiManager;

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
            } else {
                wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            }

            if (!wifiManager.isWifiEnabled()) wifiManager.setWifiEnabled(true);//TODO vlt auch über setting
        }
    }

    //Speechrecognition with gotev
    private void startSpeechRecog(){

        //Speechvisualisierung
        speechProgressView = findViewById(R.id.progress);
        speechProgressView.setVisibility(View.VISIBLE);
        int[] colors = {
                ContextCompat.getColor(this, android.R.color.black),
                ContextCompat.getColor(this, android.R.color.darker_gray),
                ContextCompat.getColor(this, android.R.color.black),
                ContextCompat.getColor(this, android.R.color.holo_orange_light),
                ContextCompat.getColor(this, android.R.color.holo_red_dark)
        };
        speechProgressView.setColors(colors);

        //Gewollte Wörter
        offWords.add(getString(R.string.speechOff1));
        offWords.add(getString(R.string.speechOff2));
        offWords.add(getString(R.string.speechOff3));

        snoozeWords.add(getString(R.string.speechSnooze1));
        snoozeWords.add(getString(R.string.speechSnooze2));
        snoozeWords.add(getString(R.string.speechSnooze3));

        restartSpeechRecog();
    }

    //------------------------------------------------------
    @Override
    public void onStartOfSpeech() {

    }

    @Override
    public void onSpeechRmsChanged(float value) {

    }

    @Override
    public void onSpeechPartialResults(List<String> results) {
        //StringBuilder str = new StringBuilder();
        /*for (String res : results) {
            if(offWords.contains(res)){
                Toast.makeText(WakeUp.this, res, Toast.LENGTH_SHORT).show();
                Speech.getInstance().stopListening();
                wokeUp();
            }else if(snoozeWords.contains(res)){
                Toast.makeText(WakeUp.this, res, Toast.LENGTH_SHORT).show();
                Speech.getInstance().stopListening();
                snoozeButton.callOnClick();
            }
        }*/
    }

    @Override
    public void onSpeechResult(String result) {
        if(!result.isEmpty()){
            Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
        }

        if(offWords.contains(result)){
            wokeUp(true);
        }else if(snoozeWords.contains(result) && intervall != 0 && alarmCount < alarmAnzahl){
            snoozeButton.callOnClick();
        }else if(snoozeWords.contains(result)){
            bt5.callOnClick();
        }else if(!justSnoozed){
            speech.shutdown();
            //Damit wenn das manchmal ausrasted nicht thread blockiert
            handlerShutdown = new Handler();
            handlerShutdown.postDelayed(() -> restartSpeechRecog(), 150);
        }
    }

    //--------------------------------------------------------

    public void restartSpeechRecog(){
        Speech.init(this);
        //Damit Speech.init() zeit hat TODO
        /*while (Speech.getInstance() == null){
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                }
            }, 100);
        }*/

        //Damit wenn das manchmal ausrasted nicht thread blockiert
        handlerSpeech = new Handler();
        handlerSpeech.postDelayed(new Runnable() {
            public void run() {
                if(Speech.getInstance() != null){
                    speech = Speech.getInstance();
                    speech.setGetPartialResults(false);
                    speech.setStopListeningAfterInactivity(15000);

                    //Wenn es zu Locale.XY eine String resource gibt sonst default local
                    if(!getString(R.string.locale).equals("default")){
                        speech.setLocale(getResources().getConfiguration().locale);
                    }else{
                        speech.setLocale(Locale.ENGLISH);
                    }

                    try{
                        //Speech.getInstance().setPreferOffline(true);
                        speech.startListening(speechProgressView, WakeUp.this);
                    } catch (SpeechRecognitionNotAvailable exc) {
                        showSpeechNotSupportedDialog();
                    } catch (GoogleVoiceTypingDisabledException exc) {
                        showEnableGoogleVoiceTyping();
                    }
                }else {
                    //TODO SCHEIßE so
                    handlerSpeech2 = new Handler();
                    handlerSpeech2.postDelayed(new Runnable() {
                        public void run() {
                            if(Speech.getInstance() != null){
                                speech = Speech.getInstance();
                                speech.setGetPartialResults(false);
                                speech.setStopListeningAfterInactivity(15000);

                                //Wenn es zu Locale.XY eine String resource gibt sonst default local
                                if(!getString(R.string.locale).equals("default")){
                                    speech.setLocale(getResources().getConfiguration().locale);
                                }else{
                                    speech.setLocale(Locale.ENGLISH);
                                }

                                try{
                                    //Speech.getInstance().setPreferOffline(true);
                                    speech.startListening(speechProgressView, WakeUp.this);
                                } catch (SpeechRecognitionNotAvailable exc) {
                                    showSpeechNotSupportedDialog();
                                } catch (GoogleVoiceTypingDisabledException exc) {
                                    showEnableGoogleVoiceTyping();
                                }
                            }
                        }
                    }, 250);
                }
            }
        }, 330);
    }

    private void showSpeechNotSupportedDialog() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        SpeechUtil.redirectUserToGoogleAppOnPlayStore(WakeUp.this);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.speech_not_available)
                .setCancelable(false)
                .setPositiveButton(R.string.Ja, dialogClickListener)
                .setNegativeButton(R.string.Nein, dialogClickListener)
                .show();
    }

    private void showEnableGoogleVoiceTyping() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.enable_google_voice_typing)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // do nothing
                    }
                })
                .show();
    }

    //Wenn Wecker verworfen werden kann
    public void alertDialog(){
        final char shared = klingelnderWecker.getShared();
        //Message für Alertdialog
        String message = getString(R.string.noMore);
        if(shared == 'v' || shared == 't'){
            message = getString(R.string.noMoreV);
        }

        AlertDialog killAlarm = new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(R.string.Ja, (dialog, whichButton) -> {
                    //Sharedalarmdaten
                    AppExecutor.getInstance().diskI0().execute(() -> {
                        //Safe delete -> message to sender or my receivers/minders
                        viewModel.deleteSafe();
                        runOnUiThread(() -> killFlags());
                    });
                })
                .setNegativeButton(R.string.Nein, (dialog, whichButton) -> killFlags())
                .show();

        //Wurd neben dialog geklickt
        killAlarm.setOnDismissListener((dialog) -> killFlags());
    }

    public void killFlags(){
        win.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        win.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        win.clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        win.clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        finish();
    }

    public void wokeUp(final boolean showAlert){
        if(klingelnderWecker == null){//Klingelt einfach nur
            foregroundService(ForegroundService_AlarmSound.ACTION_STOP);
            killFlags();
        }

        if (speechControl && speech != null) speech.shutdown();

        AlarmUtil.cancelRepeating(getApplicationContext(), klingelnderWecker.getId());
        foregroundService(ForegroundService_AlarmSound.ACTION_STOP);
        viewModel.messageTo_Minder_Sender(appMessage, iAmEmpfaenger, FB.aufgestanden);

        if(noDays && showAlert){
            alertDialog();
        }else {
            killFlags();
        }
    }

    public void closedAfterVer(){
        //Hier foreground und nich in handler, damit nicht aus background versucht wird den Service zu starten
        foregroundService(ForegroundService_AlarmSound.ACTION_STOP);
        if(noDays){
            alertDialog();
        }else {
            //Keep Screen on usw. clearen und finish()
            killFlags();
        }
    }

    /*@Override
    public void onBackPressed() {
        super.onBackPressed();

        if(!sleeping){
            wokeUp(true);
        }else {
            closedAfterVer();
        }
    }*/

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        if (getIntent().getAction() != null && getIntent().getAction().equals(AlarmUtil.REPEAT)){
            alarmCount = intent.getIntExtra(AlarmUtil.ALARM_COUNT, 1);
            repeatingAlarm();
        }else if(getIntent().getAction() != null && getIntent().getAction().equals(AlarmUtil.OVERSLEPT)){
                g.setVisibility(View.GONE);
                //FloatingActionButton image und OnClick aendern
                sleeping = true;
                turnOffButton.setImageResource(R.drawable.ic_close_black_24dp);

                //Google Speechrecognizer
                if(speechControl && speech != null){
                    speech.shutdown();
                }

                weckString.setText(getResources().getString(R.string.verschlafen));

               /*if (!smsMessageMap.isEmpty()) {//Message über App verschicken
                   for (final Map.Entry<String, String> entry : smsMessageMap.entrySet()){

                       //Telefon nummer
                       String number = entry.getKey();

                       String message = klingelnderWecker.getSms_verschlafen();
                       //SMS verschicken
                       SmsManager sms = SmsManager.getDefault();
                       sms.sendTextMessage(number, null, message, null, null);
                   }
               }*/

                viewModel.messageTo_Minder_Sender(appMessage, iAmEmpfaenger, FB.verschlafen);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(handlerSpeech != null){
            handlerSpeech.removeCallbacks(null);
        }
        if(handlerSpeech2 != null){
            handlerSpeech2.removeCallbacks(null);
        }
        if(handlerShutdown != null){
            handlerShutdown.removeCallbacks(null);
        }
    }
}
