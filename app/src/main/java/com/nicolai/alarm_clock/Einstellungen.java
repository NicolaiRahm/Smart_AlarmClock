package com.nicolai.alarm_clock;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

/*import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;*/

import com.nicolai.alarm_clock.dialog.IntervallPickerFragment;
import com.nicolai.alarm_clock.dialog.SongDialog;
import com.nicolai.alarm_clock.dialog.TimeBisVerschlafenPicker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
//import kaaes.spotify.webapi.android.SpotifyApi;
//import kaaes.spotify.webapi.android.SpotifyService;

public class Einstellungen extends AppCompatActivity implements IntervallPickerFragment.OnIntervallChoosen, SongDialog.OnSoundTypeChoosen,
        TimeBisVerschlafenPicker.OnTimeBisChoosen/*, SpotifyPlayer.NotificationCallback, ConnectionStateCallback*/{

    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;

    private AudioManager audioManager;
    private Ringtone r;
    private Handler handler;

    private Switch switchVoiceControl, switchWifi;
    private TextView alarmSound, intervall, voiceControlView, einstellungenWifiView, timeBisVView;
    private ImageView playPauseMusic;
    private String result;
    private boolean randomSong = false, recordAudio = false, changeWifi = false;
    protected boolean avoidedStateLoss = false;
    private int volume, duration, maxvolume;

    // Requesting permission to RECORD_AUDIO
    private String [] permissionsAudio = {Manifest.permission.RECORD_AUDIO};
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 201;

    // Requesting permission to RECORD_AUDIO
    private String [] permissionsWifi = {Manifest.permission.CHANGE_WIFI_STATE};
    private static final int REQUEST_CHANGE_WIFI_PERMISSION = 198;

    // Requesting permission to Choose alarmTone from music / Play it later from there
    private String [] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};
    private static final int REQUEST_PLAY_MUSIC_PERMISSION = 200;
    //Für liste von 10 songs für vorwärts/rückwärts clicks
    private static final int REQUEST_GET_MUSIC_PERMISSION = 199;

    private static final int DISPLAY_RINGTONES = 1, RINGTONE_CHOOSEN = 2, DISPLAY_MUSIC = 3, MUSIC_CHOOSEN = 4, DISPLAY_SPOTIFY = 5, SPOTIFY_CHOOSEN = 6;

    //Spotify clientID
    private static final String CLIENT_ID = "c01dee3be65b4a0780915d557d388a8e";
    // TODO: Replace with your redirect URI
    private static final String REDIRECT_URI = "alarmclock://callback";
    //RequestCode
    private static final int REQUEST_SPOTIFY = 1337;
    //private Player mPlayer;

    //Interface IntervallPicker
    @Override
    public void sendIntervall(int anzahl, long inter) {
        mEditor.putInt(getString(R.string.sharedAnzahl), anzahl);
        mEditor.putLong(getString(R.string.sharedIntervall), inter);
        mEditor.apply();

        //TextView updaten
        String Intervall = getResources().getString(R.string.alarmIntervallManuel, anzahl, inter / 60000);
        if(inter != 0){
            intervall.setText(Intervall);
        }else {
            intervall.setText(R.string.AlarmIntervallNo);
        }
    }

    //Interface Time bis verschlafen Picker
    @Override
    public void sendTimeBis(int minutes, int seconds) {
        mEditor.putInt(getString(R.string.sharedTimeBisVerschlafen), minutes * 60 + seconds);
        mEditor.apply();

        if(seconds < 10){
            timeBisVView.setText(getString(R.string.timeBisVerschlafenNach, minutes + ":0" + seconds));
        }else{
            timeBisVView.setText(getString(R.string.timeBisVerschlafenNach, minutes + ":" + seconds));
        }
    }

    //Interface SongDialog
    @Override
    public void sendSoundType(int soundType) {
        switch (soundType){
            case DISPLAY_RINGTONES://Musik in Dialog angeklickt
                Intent i = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                i.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, R.string.AlarmTon);
                i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
                i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                i.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,RingtoneManager.TYPE_ALARM);
                this.startActivityForResult(i,RINGTONE_CHOOSEN);
                break;

            case DISPLAY_MUSIC://Musik in Dialog angeklickt
                /*Intent i2 = new Intent();
                i2.setAction(android.content.Intent.ACTION_GET_CONTENT);
                i2.setDataAndType(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "audio/*");
                startActivityForResult(i2, MUSIC_CHOOSEN);*/

                Intent musicIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(Intent.createChooser(musicIntent, getString(R.string.chooseAlarmSound)), MUSIC_CHOOSEN);
                break;

            case DISPLAY_SPOTIFY://Spotify in Dialog angeklickt
                //SPOTIFY
                // The only thing that's different is we added the 5 lines below.
                try {
                    final Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.setAction(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH);
                    intent.setComponent(new ComponentName("com.spotify.mobile.android.ui", "com.spotify.mobile.android.ui.Launcher"));
                    intent.putExtra(SearchManager.QUERY, "Ben E. King" + " " + "Stand By Me" );
                    startActivity(intent);
                } catch ( ActivityNotFoundException e ) {
                    final Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.setAction(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH);
                    intent.setComponent(new ComponentName("com.spotify.music", "com.spotify.music.MainActivity"));
                    intent.putExtra(SearchManager.QUERY, "Ben E. King" + " " + "Stand By Me" );
                    startActivity(intent);
                }

                /*AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI);
                builder.setScopes(new String[]{"user-read-private", "streaming", });
                AuthenticationRequest request = builder.build();

                AuthenticationClient.openLoginActivity(this, REQUEST_SPOTIFY, request);
                //AuthenticationClient.openLoginInBrowser(this, request);

                break;*/
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Response of FirebaseUI
        switch (requestCode) {

            case RINGTONE_CHOOSEN://Musik in Dialog angeklickt
                if (resultCode == Activity.RESULT_OK) {
                    Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    mEditor.putString(getString(R.string.sharedAlarmsound), uri.toString());
                    mEditor.apply();
                    result = RingtoneManager.getRingtone(this, uri).getTitle(this);
                    alarmSound.setText(result);
                }
                break;

            case MUSIC_CHOOSEN://Musik in Dialog angeklickt
                if (resultCode == Activity.RESULT_OK) {
                    Uri audio = data.getData();
                    mEditor.putString(getString(R.string.sharedAlarmsound), audio.toString());
                    mEditor.apply();

                    //SongPOJO Titel
                    Cursor cursor = getContentResolver().query(audio, null, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        result = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
                        cursor.close();
                    }
                    alarmSound.setText(result);
                }
                break;

                //TODO Aus Gradel löschen wenn verboten
            case REQUEST_SPOTIFY:
                /*AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, data);
                if (response.getType() == AuthenticationResponse.Type.TOKEN) {

                    //Spotify WebApi
                    SpotifyApi api = new SpotifyApi();
                    api.setAccessToken(response.getAccessToken());
                    SpotifyService spService = api.getService();

                    //Spotify Player
                    Config playerConfig = new Config(this, response.getAccessToken(), CLIENT_ID);
                    Spotify.getPlayer(playerConfig, this, new SpotifyPlayer.InitializationObserver() {
                        @Override
                        public void onInitialized(SpotifyPlayer spotifyPlayer) {
                            mPlayer = spotifyPlayer;
                            //mPlayer.addConnectionStateCallback(Einstellungen.this);
                            //mPlayer.addNotificationCallback(Einstellungen.this);
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
                        }
                    });
                }*/
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_einstellungen);

        //Initialisierung Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarEinstellungen);
        setSupportActionBar(toolbar);
        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();
        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setTitle(R.string.einstellung);

        //Initialisierung SharedPreferences plus Editor
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mEditor = mSharedPreferences.edit();

        //###################################### Wifi ##############################################################
        switchWifi = findViewById(R.id.switchWifi);
        einstellungenWifiView = findViewById(R.id.einstellungenWifiView);

        if(mSharedPreferences.getBoolean(getString(R.string.sharedWifi), false)){
            switchWifi.setChecked(true);
            einstellungenWifiView.setText(R.string.wifiAutoYes);
        }

        //CHANGE_WIFI is bereits erlaubt
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE) == PackageManager.PERMISSION_GRANTED ){
            changeWifi = true;
        }

        switchWifi.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    //Permission granted?!
                    if(changeWifi){
                        new AlertDialog.Builder(Einstellungen.this)
                                .setMessage(R.string.wifi_disclaimer)
                                .setCancelable(false)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                }).show();

                        einstellungenWifiView.setText(R.string.wifiAutoYes);
                        mEditor.putBoolean(getString(R.string.sharedWifi), true);
                        mEditor.apply();
                    }else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                        requestPermissions(permissionsWifi, REQUEST_CHANGE_WIFI_PERMISSION);
                    }
                }else {
                    einstellungenWifiView.setText(R.string.wifiAutoNo);
                    mEditor.putBoolean(getString(R.string.sharedWifi), false);
                    mEditor.apply();
                }
            }
        });

        //##################################### Mobiledaten / Mobilesnetz ##############################################

        /*Switch switchMobile = findViewById(R.id.switchMobil);
        final TextView einstellungenMobileView = findViewById(R.id.einstellungenMobileView);

        if(mSharedPreferences.getBoolean("mobil", false)){
            switchMobile.setChecked(true);
            einstellungenMobileView.setText(R.string.mobilAutoYes);
        }

        switchMobile.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    einstellungenMobileView.setText(R.string.mobilAutoYes);
                    mEditor.putBoolean("mobil", true);
                    mEditor.apply();
                }else {
                    einstellungenMobileView.setText(R.string.mobilAutoNo);
                    mEditor.putBoolean("mobil", false);
                    mEditor.apply();
                }
            }
        });*/

        //################################### Intervall ###################################################

        intervall = findViewById(R.id.einstellungenIntervall);

        if(mSharedPreferences.getLong(getString(R.string.sharedIntervall), 0) != 0){
            long inter = mSharedPreferences.getLong(getString(R.string.sharedIntervall), 0);
            int anzahl = mSharedPreferences.getInt(getString(R.string.sharedAnzahl), 1);
            intervall.setText(getResources().getString(R.string.alarmIntervallManuel, anzahl, inter / 60000));
        }

        //#################################### AlarmSound ##############################################################

        //READ_EXTERNAL is bereits erlaubt
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            randomSong = true;
            //randomSong();
        }

        alarmSound = findViewById(R.id.einstellungenToneView);

        //Default Ringtone
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        final String defaultTone = uri.toString();

        Uri songUri = Uri.parse(mSharedPreferences.getString(getString(R.string.sharedAlarmsound), defaultTone));
        String SongTitle = RingtoneManager.getRingtone(this, songUri).getTitle(this);

        alarmSound.setText(SongTitle);

        //#################################### Lautstärke ##############################################################
        SeekBar seekBarVolume = findViewById(R.id.einstellungenSeekBarVolume);
        playPauseMusic = findViewById(R.id.einstellungenPauseMusic);

        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        maxvolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);

        //Max und default Volume
        volume = mSharedPreferences.getInt(getString(R.string.sharedVolume), maxvolume / 2);
        seekBarVolume.setMax(maxvolume);
        seekBarVolume.setProgress(volume);

        seekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                volume = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if(r != null && r.isPlaying()) {
                    handler.removeCallbacksAndMessages(null);
                    r.stop();
                    playPauseMusic.setImageResource(R.drawable.ic_play_circle_outline_black_24dp);
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mEditor.putInt("volume", volume);
                mEditor.apply();
                playSong(mSharedPreferences.getString(getString(R.string.sharedAlarmsound), defaultTone));
                playPauseMusic.setImageResource(R.drawable.ic_pause_circle_outline_black_24dp);
            }
        });

        playPauseMusic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(r != null && r.isPlaying()) {
                    handler.removeCallbacksAndMessages(null);
                    r.stop();
                    playPauseMusic.setImageResource(R.drawable.ic_play_circle_outline_black_24dp);
                }else{
                    playSong(mSharedPreferences.getString(getString(R.string.sharedAlarmsound), defaultTone));
                    playPauseMusic.setImageResource(R.drawable.ic_pause_circle_outline_black_24dp);
                }
            }
        });

        //#################################### Duration ################################################################
        SeekBar seekBarDuration = findViewById(R.id.einstellungenSeekBarDuration);
        final TextView durationView = findViewById(R.id.einstellungenDurationView);

        seekBarDuration.setMax(60);
        seekBarDuration.setProgress(mSharedPreferences.getInt(getString(R.string.sharedDuration), 30));
        durationView.setText("" + mSharedPreferences.getInt(getString(R.string.sharedDuration), 30));

        seekBarDuration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                durationView.setText("" + progress);
                duration = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mEditor.putInt(getString(R.string.sharedDuration), duration);
                mEditor.apply();
            }
        });


        //#################################### Voicecontrole ############################################################

        //READ_EXTERNAL is bereits erlaubt
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED ){
            recordAudio = true;
        }

        switchVoiceControl = findViewById(R.id.einstellungSwitchVoice);
        voiceControlView = findViewById(R.id.einstellungenVoiceState);

        if(mSharedPreferences.getBoolean(getString(R.string.sharedVoicecontrol), false)){
            switchVoiceControl.setChecked(true);
            voiceControlView.setText(R.string.On);
        }

        switchVoiceControl.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !recordAudio){
                        requestPermissions(permissionsAudio, REQUEST_RECORD_AUDIO_PERMISSION);
                    }else {
                        voiceControlView.setText(R.string.On);
                        mEditor.putBoolean(getString(R.string.sharedVoicecontrol), true);
                        mEditor.apply();
                    }
                }else {
                    voiceControlView.setText(R.string.Off);
                    mEditor.putBoolean(getString(R.string.sharedVoicecontrol), false);
                    mEditor.apply();
                }
            }
        });

        ImageView speechInfo = findViewById(R.id.speechInfoS);
        speechInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSpeechDialog();
            }
        });

        //#########################################################################################################################


        //############################### Time bis verschlafen #########################################
        timeBisVView = findViewById(R.id.einstellungenBisVerschlafen);
        int minutesBis = mSharedPreferences.getInt(getString(R.string.sharedTimeBisVerschlafen), 60) / 60;
        int secondsBis = mSharedPreferences.getInt(getString(R.string.sharedTimeBisVerschlafen), 60) - minutesBis * 60;

        if(secondsBis < 10){
            timeBisVView.setText(getString(R.string.timeBisVerschlafenNach, minutesBis + ":0" + secondsBis));
        }else{
            timeBisVView.setText(getString(R.string.timeBisVerschlafenNach, minutesBis + ":" + secondsBis));
        }
    }

    //OnClick Method zum Intervallaendern
    public void changeIntervall(View view){
        DialogFragment newFragment = new IntervallPickerFragment();
        newFragment.show(getSupportFragmentManager(),"IntervallPicker");
    }

    //OnClick Method zum Intervallaendern
    public void changeTimeBis(View view){
        DialogFragment newFragment = new TimeBisVerschlafenPicker();
        newFragment.show(getSupportFragmentManager(),"TimeBisPicker");
    }

    //OnClick Method zum Songaendern
    public void changeSong(View view){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (!randomSong){
                requestPermissions(permissions, REQUEST_PLAY_MUSIC_PERMISSION);
            }else {
                SongDialog newFragment = new SongDialog();
                newFragment.show(getSupportFragmentManager(),"SongDialog");
            }

        }else{
            SongDialog newFragment = new SongDialog();
            newFragment.show(getSupportFragmentManager(),"SongDialog");
        }

    }

    public void playSong(String uri){
        final int ov = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);//Um danach wieder auf vorherige Lautstärke zu setzen
        playPauseMusic.setImageResource(android.R.drawable.ic_media_pause);

        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, mSharedPreferences.getInt("volume", maxvolume/2), 0);//WEnn als STREAM_ALARM mus r .setUsage(AudioAttributes.USAGE_ALARM)

        //Rintone Manager
        r = RingtoneManager.getRingtone(this, Uri.parse(uri));

        if (Build.VERSION.SDK_INT >= 21) {
            AudioAttributes aa = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            r.setAudioAttributes(aa);
        } else {
            r.setStreamType(AudioManager.STREAM_ALARM);
        }

        r.play();

        handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                audioManager.setStreamVolume(AudioManager.STREAM_RING, ov, 0);
                if(r.isPlaying()){
                    r.stop();
                }
                playPauseMusic.setImageResource(android.R.drawable.ic_media_play);
            }
        };
        handler.postDelayed(runnable, 60000);
    }

    //Anzeigen der Wörter für die Speechrecognition
    public void showSpeechDialog(){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
// ...Irrelevant code for customizing the buttons and title
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.alert_speech_info, null);
        dialogBuilder.setView(dialogView);

        //EditText editText = (EditText) dialogView.findViewById(R.id.label_field);
        //editText.setText("test label");
        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PLAY_MUSIC_PERMISSION: {//Um Musik auswählen zu dürfen
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Dialog is called in onPostResume to avoide StateLoss
                    avoidedStateLoss = true;
                    randomSong = true;
                    //randomSong();
                }
                break;
            }

            case REQUEST_GET_MUSIC_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    randomSong = true;
                    //randomSong();
                }
                break;
            }

            case REQUEST_RECORD_AUDIO_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    voiceControlView.setText(R.string.On);
                    mEditor.putBoolean(getString(R.string.sharedVoicecontrol), true);
                    mEditor.apply();
                }else{
                    switchVoiceControl.setChecked(false);
                }
                break;

            case REQUEST_CHANGE_WIFI_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    new AlertDialog.Builder(Einstellungen.this)
                            .setMessage(R.string.wifi_disclaimer)
                            .setCancelable(false)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            }).show();

                    einstellungenWifiView.setText(R.string.wifiAutoYes);
                    mEditor.putBoolean(getString(R.string.sharedWifi), true);
                    mEditor.apply();
                }else{
                    switchWifi.setChecked(false);
                }
                break;
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if(avoidedStateLoss){
            SongDialog newFragment = new SongDialog();
            newFragment.show(getSupportFragmentManager(),"SongDialog");
            avoidedStateLoss = false;
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        if(r != null){
            if(r.isPlaying()){
                playPauseMusic.setImageResource(android.R.drawable.ic_media_play);
                handler.removeCallbacksAndMessages(null);
                r.stop();
            }
        }
        //Kill SpotifyPlayer
        //if(mPlayer != null){
            //Spotify.destroyPlayer(this);
        //}
    }



    //SPOTIFY
/*
    @Override
    public void onPlaybackEvent(PlayerEvent playerEvent) {
        Log.d("MainActivity", "Playback event received: " + playerEvent.name());
        switch (playerEvent) {
            default:
                break;
        }
    }

    @Override
    public void onPlaybackError(Error error) {
        Log.d("MainActivity", "Playback error received: " + error.name());
        switch (error) {
            // Handle error type as necessary
            default:
                break;
        }
    }

        //Spotify Connection
    @Override
    public void onLoggedIn() {
        Log.d("MainActivity", "User logged in");

        // This is the line that plays a song.
        mPlayer.playUri(null, "spotify:track:2TpxZ7JUBn3uw46aR7qd6V", 0, 0);

        String spotifyUri = mPlayer.getMetadata().contextName;
        Toast.makeText(Einstellungen.this, spotifyUri, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLoggedOut() {
        Log.d("MainActivity", "User logged out");
    }

    @Override
    public void onLoginFailed(Error var1) {
        Log.d("MainActivity", "Login failed");
    }

    @Override
    public void onTemporaryError() {
        Log.d("MainActivity", "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d("MainActivity", "Received connection message: " + message);
    }*/
}
