package com.nicolai.alarm_clock;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProviders;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
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
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.transition.ChangeBounds;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;

import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nicolai.alarm_clock.adapter_helper.MessagesRecyclerAdapter;
import com.nicolai.alarm_clock.adapter_helper.ShareRecyclerAdapter;
import com.nicolai.alarm_clock.dialog.IntervallPickerFragment;
import com.nicolai.alarm_clock.dialog.SongDialog;
import com.nicolai.alarm_clock.dialog.TimeBisVerschlafenPicker;
import com.nicolai.alarm_clock.dialog.TimePickerFragment;
import com.nicolai.alarm_clock.pojos.FB;
import com.nicolai.alarm_clock.pojos.ShareContactsPOJO;
import com.nicolai.alarm_clock.pojos.SongPOJO;
import com.nicolai.alarm_clock.pojos.WeckerPOJO;
import com.nicolai.alarm_clock.util.AlarmUtil;
import com.nicolai.alarm_clock.util.FullVersionUtil;
import com.nicolai.alarm_clock.viewmodels.ViewModel_TimeSeter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.AUDIO_SERVICE;
import static com.nicolai.alarm_clock.R.id.checkBoxDi;
import static com.nicolai.alarm_clock.R.id.checkBoxDo;
import static com.nicolai.alarm_clock.R.id.checkBoxFr;
import static com.nicolai.alarm_clock.R.id.checkBoxMi;
import static com.nicolai.alarm_clock.R.id.checkBoxMo;
import static com.nicolai.alarm_clock.R.id.checkBoxRe;
import static com.nicolai.alarm_clock.R.id.checkBoxSa;
import static com.nicolai.alarm_clock.R.id.checkBoxSo;
import static com.nicolai.alarm_clock.R.id.textView10;
import static com.nicolai.alarm_clock.R.id.textView2;
import static com.nicolai.alarm_clock.R.id.textView3;
import static com.nicolai.alarm_clock.R.id.textView5;
import static com.nicolai.alarm_clock.R.id.textView6;
import static com.nicolai.alarm_clock.R.id.textView7;
import static com.nicolai.alarm_clock.R.id.textView8;
import static com.nicolai.alarm_clock.R.id.textView9;

/**
 * Fragment von TimeSeter.
 * Zum setzen von Uhrzeit und weiteren normalen Einstellungen.
 * Daten werden bei Klick auf wecker in appActionBar von static Attributen in TimeSeter in die Datenbank überführt.
 */
public class Alarm_Clock  extends Fragment implements DeleteMessageContact, IntervallPickerFragment.OnIntervallChoosen,
        TimePickerFragment.OnTimeChoosen, TimeBisVerschlafenPicker.OnTimeBisChoosen, PurchasesUpdatedListener, BillingClientStateListener {

    private Alarm_Clock getFragment;

    private boolean purchaseChaecked, freeLeft;
    private BillingClient mBillingClient;

    protected CheckBox moCh, diCh, miCh, doCh, frCh, saCh, soCh, repeat, forme;
    private SeekBar seekBarVolume;
    protected CardView musicNavigation;
    private ImageButton play_pause, nextSong, lastSong;
    private Switch speechControl;

    private AudioManager audioManager;
    private SongDialog songDialog;
    private Ringtone r;
    private Handler handler;
    private ViewFlipper vf;
    private EditText et;
    private List<SongPOJO> mItems;
    private TextView switchText;
    private TextView timeLeft;
    private TextView timeBisVView;

    private String klingeltIn;
    private int whichDurationType;

    private String result, mon, die, mit, donne, fre, sam, son, mCurrentUserID;

    private SharedPreferences mSharedPreferences;

    private int maxvolume, nextSongInt, showCaseCount;
    public static final int MUSIC_CLICKED = 1, MUSIC_CHOOSEN = 2, RINGTONE_CLICKED = 3, RINGTONE_CHOOSEN = 7, SEND_SMS = 2016, REQ_CODE_SPEECH_INPUT = 299;;

    private boolean randomSong = false, avoidedStateLoss = false, smsTextVisible = false;

    private TextView showsSong, mo, di, mi, donn, fr, sa, so, uhrzeit, alarmIntervall, Nameview;

    private MessagesRecyclerAdapter mAdapter;
    private ShareRecyclerAdapter mSharedAdapter;
    private List<ShareContactsPOJO> messagesContacts, shareContacts;

    private TabLayout tabLayout;
    private TabLayout.OnTabSelectedListener onTabSelectedListener;

    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false, seamToStep = false;
    private String [] permissionsAudio = {Manifest.permission.RECORD_AUDIO};
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 201;

    // Requesting permission to Choose alarmTone from music / Play it later from there
    private String [] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};
    private static final int REQUEST_PLAY_MUSIC_PERMISSION = 200;
    //Für liste von 10 songs für vorwärts/rückwärts clicks
    private static final int REQUEST_GET_MUSIC_PERMISSION = 199;

    private ViewModel_TimeSeter timeSeterVM;
    private MutableLiveData<WeckerPOJO> liveWecker;
    private WeckerPOJO observedWecker;

    private boolean neu, assisted;
    private HashMap<String, String> contactMap = new HashMap<String, String>();
    private HashMap<String, String> smsMap = new HashMap<String, String>();
    private HashMap<String, String> messageMap = new HashMap<String, String>();

    //Wecker values
    private int id, hour, minute, anzahl, duration, volume, seconds_bis_verschlafen;
    private long intervall;
    private String name, days, alarm_sound, shared_alarm_con, shared_sms_con, shared_messages_con, sms_verschlafen_text, sms_aufgestanden_text;
    private char shared;
    private boolean weekly_repeat, speech_control, for_me;

    private EditText editSmsMessageW, editSmsMessageV;

    private boolean mock;


    public Alarm_Clock() {
        // Required empty public constructor
    }


    //Interface
    @Override
    public void sendIntervall(int anzahlH, long intervallH) {
        observedWecker.setAnzahl(anzahlH);
        observedWecker.setIntervall(intervallH);
        liveWecker.setValue(observedWecker);

        //TextView updaten
        String Intervall = getResources().getString(R.string.alarmIntervallManuel, anzahl, intervall / 60000);
        if(intervallH != 0){
            alarmIntervall.setText(Intervall);
        }else {
            alarmIntervall.setText(R.string.AlarmIntervallNo);
        }
    }

    @Override
    public void sendTimeBis(int minutes, int seconds) {
        observedWecker.setSeconds_bis_verschlafen(minutes*60 + seconds);
        liveWecker.setValue(observedWecker);

        if(seconds < 10){
            timeBisVView.setText(getString(R.string.timeBisVerschlafenNach, minutes + ":0" + seconds));
        }else{
            timeBisVView.setText(getString(R.string.timeBisVerschlafenNach, minutes + ":" + seconds));
        }
    }

    // Austausch zwischen Fragment_Message und Dialogfragment für Kontakt Auswahl
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    String newName = result.get(0);

                    observedWecker.setName(newName);
                    liveWecker.setValue(observedWecker);

                    if(!newName.isEmpty()){
                        Nameview.setText(newName);
                        et.setText(newName);
                    }else {
                        Nameview.setText(R.string.AlarmnameNo);
                    }
                }
                break;
            }

            case MUSIC_CLICKED://Musik in Dialog angeklickt
                if (resultCode == RESULT_OK) {
                    //Produziert external uri die in wakeup nicht funkt

                    /*Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    intent.setDataAndType(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "audio/*");
                    startActivityForResult(intent, MUSIC_CHOOSEN);*/

                    Intent musicIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(Intent.createChooser(musicIntent, getString(R.string.chooseAlarmSound)), MUSIC_CHOOSEN);

                    //Funkt aber retro
                    //Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
                    //startActivityForResult(intent, MUSIC_CHOOSEN);

                    /*Intent intent;
                    intent = new Intent();
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    intent.setType("audio/mpeg");
                    startActivityForResult(Intent.createChooser(intent, getString(R.string.chooseAlarmSound)), MUSIC_CHOOSEN);*/
                }
                break;

            case MUSIC_CHOOSEN: //Musik von Handy ausgewählt
                if (resultCode == RESULT_OK) {
                    Uri audio = data.getData();

                    observedWecker.setAlarm_sound(audio.toString());
                    liveWecker.setValue(observedWecker);

                   //SongPOJO Titel
                    Cursor cursor = getActivity().getContentResolver().query(audio, null, null, null, null);
                    try {
                        if (cursor != null && cursor.moveToFirst()) {
                            result = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));

                        }
                    } finally {
                        cursor.close();
                    }
                    showsSong.setText(result);
                }

                break;

            case RINGTONE_CLICKED://Klingelton in Dialog angeklickt TODO warum in api 19 keine samples????????
                if (resultCode == RESULT_OK) {
                    Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, R.string.AlarmTon);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,RingtoneManager.TYPE_ALARM);
                    this.startActivityForResult(intent,RINGTONE_CHOOSEN);
                }
                break;

            case RINGTONE_CHOOSEN://Name und uri vom ausgewählten Rintone
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    observedWecker.setAlarm_sound(uri.toString());
                    liveWecker.setValue(observedWecker);

                    result = RingtoneManager.getRingtone(getActivity(), uri).getTitle(getActivity());
                    showsSong.setText(result);
                }
                break;

             //############################### ShareHashmap ############################

            case 8://SharedContacts
                if (resultCode == RESULT_OK) {
                    HashMap <String, String> myMap = (HashMap<String, String>) data.getSerializableExtra("HashMap");

                    //Alte Sachen rauß
                    shareContacts.clear();

                    //Wenn keine Kontakte ausgewählt
                    if(myMap.isEmpty()){
                        shareContacts.add(new ShareContactsPOJO("", getString(R.string.keinKontakt), false, false));
                    }else{
                        //Kontakte Einfügen, wenn vorhanden
                        for(HashMap.Entry<String, String> entry : myMap.entrySet()){
                            shareContacts.add(new ShareContactsPOJO(entry.getKey(), entry.getValue(), true, true));
                        }
                    }

                    //RecyclerViewAdapter updaten
                    mSharedAdapter.update(shareContacts);

                    //Bearbeiteten Kontakte an Activity
                    contactMap = myMap;
                    observedWecker.setShared_alarm_con(new JSONObject(myMap).toString());
                    liveWecker.setValue(observedWecker);

                    vf.setDisplayedChild(2);

                }else if (resultCode == Activity.RESULT_CANCELED){
                    //Damit bei leer und klick auf teilenFloating nicht erst leeres include und dann auf Share.class
                    if(contactMap.isEmpty()) {
                        vf.setDisplayedChild(2);
                    }
                }
                break;

            case 9://AlarmDialog abgebrochen
                if (resultCode == RESULT_OK) {

                    //####################################### Kontaktdaten wurden erfolgreich abgefragt
                    Uri contactData = data.getData();
                    Cursor c = getActivity().getContentResolver().query(contactData, null, null, null, null);
                    if (c != null && c.getCount() > 0) {
                        c.moveToFirst();
                        String number = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        String name = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                        c.close();

                        //"Noch kein Kontakt ausgewählt!" aus recycler löschen
                        if(!messagesContacts.isEmpty()){
                            if(messagesContacts.get(0).getName().equals(getString(R.string.keinKontakt))){
                                messagesContacts.clear();
                            }
                        }

                        //Wenn Kontakt noch nicht ausgewählt is
                        if(!smsMap.containsKey(number)){
                            //Da Cursor ungleich null ---  alte Sachen einfach drin lassen und neue Sms hinten anfügen
                            messagesContacts.add(new ShareContactsPOJO(number, name, false, false));
                        }

                        //RecyclerViewAdapter updaten
                        mAdapter.update(messagesContacts);

                        smsMap.put(number, name);
                        observedWecker.setShared_sms_con(new JSONObject(smsMap).toString());
                        liveWecker.setValue(observedWecker);

                    }

                }else if (resultCode == Activity.RESULT_CANCELED){

                }
                break;

            case 10://Auswahl inApp MessageContacts
                if (resultCode == RESULT_OK) {
                    HashMap <String, String> myMap = (HashMap<String, String>) data.getSerializableExtra("HashMap");

                    //Alte Sachen rauß
                    messagesContacts.clear();

                    //Wenn keine Kontakte ausgewählt
                    if(myMap.isEmpty() && smsMap.isEmpty()){
                        messagesContacts.add(new ShareContactsPOJO("", getString(R.string.keinKontakt), false, false));
                    }else{
                        //inApp Kontakte Einfügen, wenn vorhanden
                        if(!myMap.isEmpty()){
                            for(HashMap.Entry<String, String> entry : myMap.entrySet()){
                                messagesContacts.add(new ShareContactsPOJO(entry.getKey(), entry.getValue(), true, false));
                            }
                        }

                        //SMS Contacte wieder einfügen, wenn vorhanden
                        if(!smsMap.isEmpty()){
                            for(HashMap.Entry<String, String> entry : smsMap.entrySet()){
                                messagesContacts.add(new ShareContactsPOJO(entry.getKey(), entry.getValue(), false, false));
                            }
                        }
                    }

                    //RecyclerViewAdapter updaten
                    mAdapter.update(messagesContacts);

                    //Bearbeiteten Kontakte an Activity
                    messageMap = myMap;
                    observedWecker.setShared_message_con(new JSONObject(myMap).toString());
                    liveWecker.setValue(observedWecker);

                }
                break;
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //Da Fragment brauch man das für z.B. view.findViewById
        final View view = inflater.inflate(R.layout.fragment_alarm__clock, container, false);

        //Damit die Connection besteht bevor user was Klickt
        mBillingClient = BillingClient.newBuilder(getActivity()).setListener(Alarm_Clock.this).build();
        mBillingClient.startConnection(Alarm_Clock.this);

        //ViewModel vom TimeSeter + Laden des aktuellen Weckers
        timeSeterVM = ViewModelProviders.of(getActivity()).get(ViewModel_TimeSeter.class);

        neu = timeSeterVM.isNeu();
        assisted = timeSeterVM.isAssisted();

        liveWecker = timeSeterVM.getCurrentWecker();
        liveWecker.observe(getActivity(), weckerPOJO -> {
            observedWecker = weckerPOJO;

            if(!neu){
                id = weckerPOJO.getId();
            }else{
                id = -1;
            }

            name = weckerPOJO.getName();
            days = weckerPOJO.getDays();
            weekly_repeat = weckerPOJO.isWeekly_repeat();
            hour = weckerPOJO.getHour();
            minute = weckerPOJO.getMinute();
            anzahl = weckerPOJO.getAnzahl();
            intervall = weckerPOJO.getIntervall();
            duration = weckerPOJO.getDuration();
            volume = weckerPOJO.getVolume();
            alarm_sound = weckerPOJO.getAlarm_sound();
            speech_control = weckerPOJO.isSpeech_control();

            shared = weckerPOJO.getShared();
            shared_alarm_con = weckerPOJO.getShared_alarm_con();
            shared_sms_con = weckerPOJO.getShared_sms_con();
            shared_messages_con = weckerPOJO.getShared_message_con();
            for_me = weckerPOJO.isFor_me();

            sms_verschlafen_text = weckerPOJO.getSms_verschlafen();
            sms_aufgestanden_text = weckerPOJO.getSms_aufgestanden();

            seconds_bis_verschlafen = weckerPOJO.getSeconds_bis_verschlafen();
        });

        //Daten für Kontake in HashMaps

        //shared Alarm contacts
        if(!shared_alarm_con.equals("{}")){
            try {
                JSONObject json = new JSONObject(shared_alarm_con);
                JSONArray names = json.names();
                for (int i = 0; i < names.length(); i++) {
                    String key = names.getString(i);
                    contactMap.put(key, json.optString(key));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        //sms contacts
        if(!shared_sms_con.equals("{}")){
            try {
                JSONObject json = new JSONObject(shared_sms_con);
                JSONArray names = json.names();
                for (int i = 0; i < names.length(); i++) {
                    String key = names.getString(i);
                    smsMap.put(key, json.optString(key));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        //message contacts
        if(!shared_messages_con.equals("{}")){
            try {
                JSONObject json = new JSONObject(shared_messages_con);
                JSONArray names = json.names();
                for (int i = 0; i < names.length(); i++) {
                    String key = names.getString(i);
                    messageMap.put(key, json.optString(key));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        //Damit Reference auf dieses Fragment in den Dialogs genutzt werden kann
        getFragment = this;

        //Firebase user_id
        mCurrentUserID = FirebaseAuth.getInstance().getUid();

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        //############################ TAB MENUE #############################################

        vf = (ViewFlipper) view.findViewById(R.id.vf);
        tabLayout = view.findViewById(R.id.tabLayoutSetAlarm);

        setUpTabLayout();


        //SeamToStep transition mit tabLayout
        final NestedScrollView scrollSettings = view.findViewById(R.id.scrollSettings);
        final ConstraintLayout constraintLayout = view.findViewById(R.id.constraintSetAlarm);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            scrollSettings.setOnScrollChangeListener(new View.OnScrollChangeListener() {
                @Override
                public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                    if (scrollSettings.getScrollY() != 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        constraintLayout.setElevation(10);
                    } else if (scrollSettings.getScrollY() == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        constraintLayout.setElevation(0);
                    }
                }
            });
        }

        //############################ MESSAGES ##############################################

        if(!mSharedPreferences.getBoolean(getString(R.string.sharedFullVersion), false)){
            final TaskCompletionSource<Long> source = new TaskCompletionSource<>();
            Task<Long> task = source.getTask();
            task.addOnSuccessListener(getActivity(), freeLeft -> {
                if(freeLeft > 0L){
                    TextView xLeft = view.findViewById(R.id.freeLeft);
                    xLeft.setText(getString(R.string.freeLeft, freeLeft));

                    view.findViewById(R.id.freeLeft_group).setVisibility(View.VISIBLE);

                    view.findViewById(R.id.getMore).setOnClickListener(v -> {
                        startActivity(new Intent(getContext(), Invite.class));
                    });

                    this.freeLeft = true;
                }
            });

            FullVersionUtil.freeLeft(source);
        }

        //Mit App
        TextView mitApp = view.findViewById(R.id.MitAPP);
        mitApp.setOnClickListener(v -> {
            if(freeLeft || purchaseChaecked){
                Intent intent = new Intent(getContext(), ChooseFriend.class);
                intent.setAction("Message");
                intent.putExtra("HashMap", observedWecker.getShared_message_con());
                startActivityForResult(intent, 10);
            }else{
                final TaskCompletionSource<Boolean> source2 = new TaskCompletionSource<>();
                Task<Boolean> task2 = source2.getTask();
                task2.addOnSuccessListener(getActivity(), hasFullVersion -> {

                    if(hasFullVersion){
                        Intent intent = new Intent(getContext(), ChooseFriend.class);
                        intent.setAction("Message");
                        intent.putExtra("HashMap", observedWecker.getShared_message_con());
                        startActivityForResult(intent, 10);

                        purchaseChaecked = true;
                    }else if(!mock){
                        Intent i = new Intent(getActivity(), Invite.class);
                        i.setAction("Aufpasser");
                        startActivity(i);
                    }
                });

                FullVersionUtil.checkFullVersion(getContext(), mBillingClient, source2);
            }
        });

        //Mit SMS
        TextView mitSMS= view.findViewById(R.id.MitSMS);
        mitSMS.setOnClickListener(v -> {
            //request the permission
            //requestPermissions(new String[]{android.Manifest.permission.SEND_SMS}, SEND_SMS);
            Toast.makeText(getContext(), getActivity().getString(R.string.SMS_NOTAVAILABLE), Toast.LENGTH_SHORT).show();
        });

        //Initialisierung RecyclerView
        RecyclerView mMessagesRecyclerList = view.findViewById(R.id.messageList);

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mMessagesRecyclerList.setLayoutManager(mLayoutManager);
        mMessagesRecyclerList.setNestedScrollingEnabled(false);

        //Daten an Adapterklasse
        messagesContacts = new ArrayList<>();

        //Wenn keine Kontakte ausgewählt
        if(messageMap.isEmpty() && smsMap.isEmpty()){
            messagesContacts.add(new ShareContactsPOJO("", getString(R.string.keinKontakt), false, false));
        }else{

            DatabaseReference messageContact = FirebaseDatabase.getInstance().getReference().child("messages").child(mCurrentUserID).child("ByMe")
                    .child("" + id).child("to");
            //inApp Kontakte Einfügen, wenn vorhanden
            if(!messageMap.isEmpty()){
                for(final HashMap.Entry<String, String> entry : messageMap.entrySet()){//TODO vlt. zu langsam
                    //Hat der Kontakt die Nachricht etwa schon gelöscht?!
                    messageContact.child(entry.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists()){
                                messagesContacts.add(new ShareContactsPOJO(entry.getKey(), entry.getValue(), true, false));
                            }else{
                                messageMap.remove(entry.getKey());
                                //ViewModel updaten
                                observedWecker.setShared_message_con(new JSONObject(messageMap).toString());
                                liveWecker.setValue(observedWecker);

                                //Wenn keiner mehr die Nachricht hat
                                if(messageMap.isEmpty()){

                                    if(smsMap.isEmpty() && contactMap.isEmpty()){
                                        observedWecker.setShared('l');
                                        liveWecker.setValue(observedWecker);
                                    }else if (smsMap.isEmpty()){
                                        observedWecker.setShared('v');
                                        liveWecker.setValue(observedWecker);
                                    }
                                }
                            }


                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }
            }

            //SMS Contacte wieder einfügen, wenn vorhanden
            if(!smsMap.isEmpty()){
                for(HashMap.Entry<String, String> entry : smsMap.entrySet()){
                    messagesContacts.add(new ShareContactsPOJO(entry.getKey(), entry.getValue(), false, false));
                }
            }
        }
        //Adapter initialisieren
        mAdapter = new MessagesRecyclerAdapter(messagesContacts, getActivity(), this);

        //Set Adapter to RecyclerView
        mMessagesRecyclerList.setAdapter(mAdapter);

        FloatingActionButton fabSmsText = view.findViewById(R.id.fabSmsText);

        editSmsMessageW = view.findViewById(R.id.editSmsText1);
        editSmsMessageV = view.findViewById(R.id.editSmsText);

        //EditText mit text füllen (Wenn neu wird das auch durch das zeitstellen gemacht)
        editSmsMessageW.setText(sms_aufgestanden_text);
        editSmsMessageV.setText(sms_verschlafen_text);


        //Fab animation show
        final ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(getActivity(), R.layout.alarms_message_animation);

        final Transition transition = new ChangeBounds();
        transition.setInterpolator(new AnticipateOvershootInterpolator(0.5f));
        transition.setDuration(500);

        //Fab animation hide
        final ConstraintSet constraintSetHide = new ConstraintSet();
        constraintSetHide.clone(getActivity(), R.layout.alarm_message);

        final Transition transitionHide = new ChangeBounds();
        transitionHide.setInterpolator(new AnticipateOvershootInterpolator(0.5f));
        transitionHide.setDuration(500);

        final ConstraintLayout constraintLayout1 = view.findViewById(R.id.includeAlarmMessage);

        fabSmsText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(smsTextVisible){
                    TransitionManager.beginDelayedTransition(constraintLayout1, transitionHide);
                    constraintSetHide.applyTo(constraintLayout1);

                    smsTextVisible = false;
                }else{

                    //TODO scrollto end : scrollSettings.scrollTo(0, editSmsMessageW.get);

                    TransitionManager.beginDelayedTransition(constraintLayout1 , transition);
                    constraintSet.applyTo(constraintLayout1);

                    smsTextVisible = true;
                }
            }
        });

        editSmsMessageV.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editSmsMessageV.setRawInputType(InputType.TYPE_CLASS_TEXT);
        editSmsMessageV.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    String newMessage = editSmsMessageV.getText().toString();
                    //WeckerPOJO updaten
                    observedWecker.setSms_verschlafen(newMessage);
                    liveWecker.setValue(observedWecker);

                    // hide virtual keyboard
                    InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(et.getWindowToken(), 0);

                    return true;
                }
                return false;
            }
        });

        editSmsMessageW.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editSmsMessageW.setRawInputType(InputType.TYPE_CLASS_TEXT);
        editSmsMessageW.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    String newMessage = editSmsMessageW.getText().toString();
                    //WeckerPOJO updaten
                    observedWecker.setSms_aufgestanden(newMessage);
                    liveWecker.setValue(observedWecker);

                    // hide virtual keyboard
                    InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(et.getWindowToken(), 0);

                    return true;
                }
                return false;
            }
        });





        //############################ ENDE MESSAGES #########################################

        //############################ SHARE ##############################################

        RecyclerView shareList = (RecyclerView) view.findViewById(R.id.sharedContactsList);
        //LinearLayoutManager
        shareList.setLayoutManager(new LinearLayoutManager(getActivity()));
        shareList.setNestedScrollingEnabled(false);


        //Daten an Adapterklasse
        shareContacts = new ArrayList<>();

        //Wenn keine Kontakte ausgewählt
        if(contactMap.isEmpty()){
            shareContacts.add(new ShareContactsPOJO("", getString(R.string.keinKontakt), false, true));
        }else{
            DatabaseReference alarmContacts = FirebaseDatabase.getInstance().getReference().child("alarms").child(mCurrentUserID).child("ByMe").child("" + id).child("to");
            //inApp Kontakte Einfügen, wenn vorhanden
            for(final HashMap.Entry<String, String> entry : contactMap.entrySet()){

                //Hat der Kontakt den Alarm gelöscht?!
                alarmContacts.child(entry.getKey()).child("status").addListenerForSingleValueEvent(new ValueEventListener() {//TODO vlt zu lang
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        //Wird aus to in cloudfunction gelöscht wenn wecker geupdatet wird, sonst erstmal nur status deleted
                        if(dataSnapshot.exists()){
                            if(dataSnapshot.getValue().equals("deleted")){
                                contactMap.remove(entry.getKey());
                                observedWecker.setShared_alarm_con(new JSONObject(messageMap).toString());
                                liveWecker.setValue(observedWecker);
                            }else{
                                shareContacts.add(new ShareContactsPOJO(entry.getKey(), entry.getValue(), true, true));
                            }
                        }else{
                            contactMap.remove(entry.getKey());
                            observedWecker.setShared_alarm_con(new JSONObject(messageMap).toString());
                            liveWecker.setValue(observedWecker);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }
        }

        mSharedAdapter = new ShareRecyclerAdapter(shareContacts, this);
        //Set Adapter to RecyclerView
        shareList.setAdapter(mSharedAdapter);

        //Contact hinzufügen
        TextView hinzufuegen = (TextView) view.findViewById(R.id.tvHinzufuegen);
        hinzufuegen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), ChooseFriend.class);
                intent.setAction("Share");
                intent.putExtra("HashMap", observedWecker.getShared_alarm_con());
                startActivityForResult(intent, 8);
            }
        });

        //SOLL der Alarm auch bei mir schellen?!
        forme = (CheckBox) view.findViewById(R.id.FORME);
        forme.setChecked(for_me);
        forme.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    observedWecker.setFor_me(true);
                    liveWecker.setValue(observedWecker);
                }else{
                    observedWecker.setFor_me(false);
                    liveWecker.setValue(observedWecker);
                }
            }
        });


        //############################ ENDE SHARE #########################################


        //############################# ANFANG NAMENSFELD ##########################################
        Nameview = view.findViewById(R.id.AlarmNameView);
        final TextView nameSubtitle = view.findViewById(R.id.nameSubtitle);


        Nameview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nameAendern();
            }
        });

        nameSubtitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nameAendern();
            }
        });

        //Initialisierung EditText zur Namensgebung
        et = view.findViewById(R.id.editText);
        et.setImeOptions(EditorInfo.IME_ACTION_DONE);
        et.setRawInputType(InputType.TYPE_CLASS_TEXT);

        if(!name.isEmpty()){
            Nameview.setText(name);
            et.setText(name);
        }

        et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    String newName = et.getText().toString();
                    observedWecker.setName(newName);
                    liveWecker.setValue(observedWecker);

                    if(!newName.isEmpty()){
                        Nameview.setText(newName);
                    }else {
                        Nameview.setText(R.string.AlarmnameNo);
                    }


                    // hide virtual keyboard
                    InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(et.getWindowToken(), 0);

                    et.setVisibility(View.GONE);
                    return true;
                }
                return false;
            }
        });

        //Spoken alarm name
        ImageView microphone = view.findViewById(R.id.speechAlarmNameImg);
        microphone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startVoiceInput();
            }
        });

        //############################# ENDE NAMENSFELD ############################################


        //############################# AUSWÄHLEN DER WOCHENTAGE UND DER WIEDERHOLUNG ##############

        //Uhrzeitcolor wenn alarm nicht von mir -> nicht bearbeitbar
        if(shared == 'b' || shared == 's' || shared == 'u' || shared == 'd'){
            ImageView closedDays = view.findViewById(R.id.closedDays);
            closedDays.setVisibility(View.VISIBLE);

            closedDays.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(getActivity(), getString(R.string.nichtVonDir), Toast.LENGTH_SHORT).show();
                }
            });
        }

        //Initialisierung TextViews Wochentage
        mo = view.findViewById(textView3);
        String montag = getString(R.string.Mo);
        mo.setText("" + montag.charAt(0) + montag.charAt(1));//TODO wenn string aus R.string ""vor begin!!!!!!!!!!!!!
        di = view.findViewById(textView5);
        String dienstag = getString(R.string.Di);
        di.setText("" + dienstag.charAt(0) + dienstag.charAt(1));
        mi = view.findViewById(textView6);
        String mittwoch = getString(R.string.Mi);
        mi.setText("" + mittwoch.charAt(0) + mittwoch.charAt(1));
        donn = view.findViewById(textView7);
        String donnerstag = getString(R.string.Do);
        donn.setText("" + donnerstag.charAt(0) + donnerstag.charAt(1));
        fr = view.findViewById(textView8);
        String freitag = getString(R.string.Fr);
        fr.setText("" + freitag.charAt(0) + freitag.charAt(1));
        sa = view.findViewById(textView9);
        String samstag = getString(R.string.Sa);
        sa.setText("" + samstag.charAt(0) + samstag.charAt(1));
        so = view.findViewById(textView10);
        String sonntag = getString(R.string.So);
        so.setText("" + sonntag.charAt(0) + sonntag.charAt(1));

        //Initialisierung der Tage anfangs mit xx
        mon = "xx";
        die = "xx";
        mit = "xx";
        donne = "xx";
        fre = "xx";
        sam = "xx";
        son = "xx";


        //OnCheckedChangeListener
        moCh = view.findViewById(checkBoxMo);
        moCh.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mon = "mo";
                } else {
                    mon = "xx";
                }
                observedWecker.setDays((mon + die + mit + donne + fre + sam + son));
                liveWecker.setValue(observedWecker);

                verbleibendeZeit();
            }
        });

        diCh = view.findViewById(checkBoxDi);
        diCh.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    die = "di";
                } else {
                    die = "xx";
                }
                observedWecker.setDays((mon + die + mit + donne + fre + sam + son));
                liveWecker.setValue(observedWecker);

                verbleibendeZeit();
            }
        });
        miCh = view.findViewById(checkBoxMi);
        miCh.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    mit = "mi";
                } else {
                    mit = "xx";
                }
                observedWecker.setDays((mon + die + mit + donne + fre + sam + son));
                liveWecker.setValue(observedWecker);

                verbleibendeZeit();
            }
        });
        doCh = view.findViewById(checkBoxDo);
        doCh.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    donne = "do";
                } else {
                    donne = "xx";
                }
                observedWecker.setDays((mon + die + mit + donne + fre + sam + son));
                liveWecker.setValue(observedWecker);

                verbleibendeZeit();
            }
        });
        frCh = view.findViewById(checkBoxFr);
        frCh.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    fre = "fr";
                } else {
                    fre = "xx";
                }
                observedWecker.setDays((mon + die + mit + donne + fre + sam + son));
                liveWecker.setValue(observedWecker);

                verbleibendeZeit();
            }
        });
        saCh = view.findViewById(checkBoxSa);
        saCh.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    sam = "sa";
                } else {
                    sam = "xx";
                }
                observedWecker.setDays((mon + die + mit + donne + fre + sam + son));
                liveWecker.setValue(observedWecker);

                verbleibendeZeit();
            }
        });
        soCh = view.findViewById(checkBoxSo);
        soCh.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    son = "so";
                } else {
                    son = "xx";
                }
                observedWecker.setDays((mon + die + mit + donne + fre + sam + son));
                liveWecker.setValue(observedWecker);

                verbleibendeZeit();
            }
        });

        repeat = view.findViewById(checkBoxRe);
        repeat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                observedWecker.setWeekly_repeat(isChecked);
                liveWecker.setValue(observedWecker);
            }
        });

        //Initialisierung Checkboxes
        final String TAGE = days;
        if(TAGE.charAt(0) == 'm'){
            moCh.setChecked(true);
        }
        if(TAGE.charAt(2) == 'd'){
            diCh.setChecked(true);
        }
        if(TAGE.charAt(4) == 'm'){
            miCh.setChecked(true);
        }
        if(TAGE.charAt(6) == 'd'){
            doCh.setChecked(true);
        }
        if(TAGE.charAt(8) == 'f'){
            frCh.setChecked(true);
        }
        if(TAGE.charAt(10) == 's'){
            saCh.setChecked(true);
        }
        if(TAGE.charAt(12) == 's'){
            soCh.setChecked(true);
        }

        repeat.setChecked(weekly_repeat);
        //############################# ENDE DAYS ##################################################

        //############################## ANFANG ALARMINTERVALL #####################################
        alarmIntervall = view.findViewById(R.id.settingsAlarmIntervallView);
        final TextView intervallSubtitle = view.findViewById(R.id.intervallSubtitle);
        if (intervall != 0) {
            //Text zum Alarmintervall.
            alarmIntervall.setText(getActivity().getResources().getString(R.string.alarmIntervallManuel, anzahl, intervall / 60000));
        }

        alarmIntervall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment newFragment = new IntervallPickerFragment();
                newFragment.setTargetFragment(getFragment, 0);
                newFragment.show(getFragmentManager(),"IntervallPicker");
            }
        });

        intervallSubtitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment newFragment = new IntervallPickerFragment();
                newFragment.setTargetFragment(getFragment, 0);
                newFragment.show(getFragmentManager(),"IntervallPicker");
            }
        });


        //############################## ENDE ALARMINTERVALL #######################################


        //############################## ANFANG VOLUME #############################################
        audioManager = (AudioManager) getActivity().getSystemService(AUDIO_SERVICE);
        maxvolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        musicNavigation = view.findViewById(R.id.musicNavigation);

        final ImageButton changeSongRight = view.findViewById(R.id.changeSongRight);
        final ImageButton changeSongLeft = view.findViewById(R.id.changeSongLeft);

        seekBarVolume = view.findViewById(R.id.seekBar);
        seekBarVolume.setMax(maxvolume);
        seekBarVolume.setProgress(volume);
        seekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                volume = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if(r != null && r.isPlaying()) {
                    handler.removeCallbacksAndMessages(null);
                    r.stop();
                    play_pause.setImageResource(android.R.drawable.ic_media_play);
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                observedWecker.setVolume(volume);
                liveWecker.setValue(observedWecker);

                if(musicNavigation.getVisibility() == View.GONE){
                    musicNavigation.setVisibility(View.VISIBLE);
                }
                playSong(alarm_sound);
            }
        });

        play_pause = view.findViewById(R.id.play_pause);
        nextSong = view.findViewById(R.id.nextSong);
        lastSong = view.findViewById(R.id.lastSong);

        play_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(r != null && r.isPlaying()) {
                    handler.removeCallbacksAndMessages(null);
                    r.stop();
                    play_pause.setImageResource(android.R.drawable.ic_media_play);
                }else if(r != null){
                    playSong(alarm_sound);
                    play_pause.setImageResource(android.R.drawable.ic_media_pause);
                }
            }
        });

        //READ_EXTERNAL is bereits erlaubt
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            randomSong = true;
            randomSong();
        }

        //Zählen der "nächster SongPOJO" Klicks
        nextSongInt = 0;
        nextSong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    //PERMISSION um Music zu laden granted?
                if(randomSong){

                    if(!mItems.isEmpty()) {
                        if (changeSongRight.getVisibility() == View.GONE) {
                            changeSongRight.setVisibility(View.VISIBLE);
                        }
                        if (changeSongLeft.getVisibility() == View.VISIBLE) {
                            changeSongLeft.setVisibility(View.GONE);
                        }

                        if (nextSongInt < 0) {
                            nextSongInt++;
                        }

                        if (nextSongInt < mItems.size()) {//Max. Size 10

                            String newUri = mItems.get(nextSongInt).getPath();
                            if (r.isPlaying()) {
                                handler.removeCallbacksAndMessages(null);
                                r.stop();
                            }
                            playSong(newUri);
                            nextSongInt++;
                        } else {
                            if (!r.isPlaying()) {
                                String newUri = mItems.get(mItems.size() - 1).getPath();
                                playSong(newUri);
                            }
                        }
                    }else {
                        Toast.makeText(getActivity(), getResources().getString(R.string.KeineMusik), Toast.LENGTH_SHORT).show();
                    }
                }else {
                    requestPermissions(permissions, REQUEST_GET_MUSIC_PERMISSION);
                }
            }
        });

        lastSong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //PERMISSION um Music zu laden granted?
                if(randomSong){

                    if(!mItems.isEmpty()) {
                        //Eigenen Button sichtbar anderen unsichtbar
                        if (changeSongLeft.getVisibility() == View.GONE) {
                            changeSongLeft.setVisibility(View.VISIBLE);
                        }
                        if (changeSongRight.getVisibility() == View.VISIBLE) {
                            changeSongRight.setVisibility(View.GONE);
                        }

                        if (nextSongInt == 9) {
                            nextSongInt--;
                        }

                        if (nextSongInt > 0) {
                            nextSongInt--;
                            String newUri = mItems.get(nextSongInt).getPath();
                            if (r.isPlaying()) {
                                handler.removeCallbacksAndMessages(null);
                                r.stop();
                            }
                            playSong(newUri);
                        } else {
                            if (changeSongLeft.getVisibility() == View.VISIBLE) {
                                changeSongLeft.setVisibility(View.GONE);
                            }
                            if (r.isPlaying()) {
                                handler.removeCallbacksAndMessages(null);
                                r.stop();
                            }
                            playSong(alarm_sound);
                        }
                    }else {
                        Toast.makeText(getActivity(), getResources().getString(R.string.KeineMusik), Toast.LENGTH_SHORT).show();
                    }
                }else {
                    requestPermissions(permissions, REQUEST_GET_MUSIC_PERMISSION);
                }
            }
        });


        //HäckchenButtons zum ändern von song
        changeSongRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeSongRight.setVisibility(View.GONE);
                changeSong(true);
            }
        });

        changeSongLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeSongLeft.setVisibility(View.GONE);
                changeSong(false);
            }
        });

        //############################## ENDE VOLUME ###############################################

        //Initialisierung der TextView zum Anzeigen des aktuellen songs
        showsSong = view.findViewById(textView2);
        Uri songUri = Uri.parse(alarm_sound);
        String SongTitle = RingtoneManager.getRingtone(getActivity(), songUri).getTitle(getActivity());
        //SongPOJO Titel
        /*cursor = getActivity().getContentResolver().query(songUri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                result = cursor.getString(cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE));
            }

        showsSong.setText(result);*/
        showsSong.setText(SongTitle);

        //Pop up zum Auswählen des Songs
        songDialog = new SongDialog();
        songDialog.setTargetFragment(this, MUSIC_CLICKED);

        //############################### ANFANG SONGAUSWAHL #######################################

       showsSong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseAlarmtone();
            }
        });

       TextView alarmtoneSubtitle = view.findViewById(R.id.alarmTonesubtitle);
       alarmtoneSubtitle.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               chooseAlarmtone();
           }
       });
        //############################### ENDE SONGAUSWAHL #########################################

        //############################### ANFANG SPEECHCONTROL #######################################

        speechControl = view.findViewById(R.id.switchSpeechControl);
        switchText = view.findViewById(R.id.textViewSpeechControl);

        if(speech_control){
            switchText.setText(R.string.On);
            speechControl.setChecked(true);
        }

        speechControl.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED ){
                        switchText.setText(R.string.On);
                        observedWecker.setSpeech_control(true);
                        liveWecker.setValue(observedWecker);
                    }else{
                        requestPermissions(permissionsAudio, REQUEST_RECORD_AUDIO_PERMISSION);//TODO vlt. mit threads gucken
                    }
                }else{
                    switchText.setText(R.string.Off);
                    observedWecker.setSpeech_control(false);
                    liveWecker.setValue(observedWecker);
                }
            }
        });

        ImageView speechInfo = view.findViewById(R.id.speechInfo);
        speechInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSpeechDialog();
            }
        });
        //############################### ENDE SPEECHCONTROL #########################################

        //############################### ANFANG DURATION #######################################
        final SeekBar durationSeekbar = view.findViewById(R.id.durationSeekbar);
        final TextView durationView = view.findViewById(R.id.durationView);
        final CheckBox cBSong = view.findViewById(R.id.cBSong);
        final CheckBox cBLoop = view.findViewById(R.id.cBInfinite);

        durationSeekbar.setMax(60);

        //1=seekbar, 2=Song, 3=Loop
        whichDurationType = 1;
        if(duration == 90){
            whichDurationType = 2;
            cBSong.setChecked(true);

            durationSeekbar.setProgress(30);
            durationView.setText(getString(R.string.Integer, 30));
        }else{
            durationSeekbar.setProgress(duration);
            durationView.setText(getString(R.string.Integer, duration));
        }

        durationSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                durationView.setText(getString(R.string.Integer, progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(whichDurationType == 1){
                    observedWecker.setDuration(seekBar.getProgress());
                    liveWecker.setValue(observedWecker);
                }
            }
        });

        cBSong.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    observedWecker.setDuration(90);
                    liveWecker.setValue(observedWecker);
                    whichDurationType = 2;

                    if(whichDurationType == 3){
                        cBLoop.setChecked(false);
                    }
                }else{
                    whichDurationType = 1;
                    observedWecker.setDuration(durationSeekbar.getProgress());
                    liveWecker.setValue(observedWecker);
                }
            }
        });

        cBLoop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    observedWecker.setDuration(111);
                    liveWecker.setValue(observedWecker);
                    whichDurationType = 3;

                    if(whichDurationType == 2){
                        cBSong.setChecked(false);
                    }
                }else{
                    whichDurationType = 1;
                    observedWecker.setDuration(durationSeekbar.getProgress());
                    liveWecker.setValue(observedWecker);
                }
            }
        });
        //############################### ENDE DURATION #########################################

        //############################### Time bis verschlafen ##################################
        TextView timeBisV = view.findViewById(R.id.timeBisVerschlafen);
        timeBisVView = view.findViewById(R.id.timeBisVerschlafenView);

        int minutesBis = seconds_bis_verschlafen / 60;
        int secondsBis = seconds_bis_verschlafen - minutesBis*60;

        if(secondsBis < 10){
            timeBisVView.setText(getString(R.string.timeBisVerschlafenNach, minutesBis + ":0" + secondsBis));
        }else{
            timeBisVView.setText(getString(R.string.timeBisVerschlafenNach, minutesBis + ":" + secondsBis));
        }


        timeBisV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment newFragment = new TimeBisVerschlafenPicker();
                newFragment.setTargetFragment(getFragment, 0);
                newFragment.show(getFragmentManager(),"TimeBisPicker");
            }
        });

        timeBisVView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment newFragment = new TimeBisVerschlafenPicker();
                newFragment.setTargetFragment(getFragment, 0);
                newFragment.show(getFragmentManager(),"TimeBisPicker");
            }
        });


        //############################ ANFANG ZEITSTELLEN #########################################

        uhrzeit = (TextView) view.findViewById(R.id.Uhrzeit);
        if(minute < 10){
            uhrzeit.setText(getString(R.string.choosenTime, hour, "0" + minute));
        }else {
            uhrzeit.setText(getString(R.string.choosenTime, hour, "" + minute));
        }

        //'''''''''''''''''''' In xy
        //Berechnung der verbleibenden Zeit
        timeLeft = view.findViewById(R.id.settingsTimeLeft);

        //Wenn neu und der Googel Assistent auch keine Zeit vorgibt
        if(neu && !assisted){
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    TimePickerFragment newFragment = TimePickerFragment.newInstance(hour, minute);
                    newFragment.setTargetFragment(getFragment, 0);
                    newFragment.show(getFragmentManager(),"TimePicker");
                }
            }, 300);

        }else{
            verbleibendeZeit();
        }

        //Uhrzeitcolor wenn alarm nicht von mir -> nicht bearbeitbar
        if(shared == 'b' || shared == 's' || shared == 'u' || shared == 'd'){
            ImageView closedTime = view.findViewById(R.id.closedTime);
            closedTime.setVisibility(View.VISIBLE);
            closedTime.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(getActivity(), getString(R.string.nichtVonDir), Toast.LENGTH_SHORT).show();
                }
            });
        }else {
            uhrzeit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    TimePickerFragment newFragment = TimePickerFragment.newInstance(hour, minute);
                    newFragment.setTargetFragment(getFragment, 0);
                    newFragment.show(getFragmentManager(),"TimePicker");
                }
            });
        }

        //Showcase
        Bundle arguments = getArguments();
        if(arguments != null && arguments.getString("mock") != null){
            mock = true;

            ImageView tiSettings = (ImageView) (((LinearLayout)((LinearLayout)tabLayout.getChildAt(0)).getChildAt(0)).getChildAt(0));
            ImageView tiMessage = (ImageView) (((LinearLayout)((LinearLayout)tabLayout.getChildAt(0)).getChildAt(1)).getChildAt(0));
            ImageView tiSend = (ImageView) (((LinearLayout)((LinearLayout)tabLayout.getChildAt(0)).getChildAt(2)).getChildAt(0));



            //int scrollTo = speechControl.getTop();
            //scrollSettings.scrollTo(0, scrollSettings.getBottom());
            scrollSettings.post(new Runnable() {
                @Override
                public void run() {
                    scrollSettings.fullScroll(View.FOCUS_DOWN);

                }
            });

            MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(getActivity());

            //################### Settings ########################
            MaterialShowcaseView.Builder tiSettingsBuilder = new MaterialShowcaseView.Builder(getActivity())
                    .setTarget(tiSettings)
                    .setDelay(500)
                    .setShapePadding(140)
                    .setTitleText(R.string.scTabSettings)
                    .setContentText(R.string.scTabSettingsDesc)
                    .setDismissText(R.string.gotIt);

            sequence.addSequenceItem(tiSettingsBuilder.build());

            //Speech Control
            MaterialShowcaseView.Builder speechCBuilder = new MaterialShowcaseView.Builder(getActivity())
                    .setTarget(speechControl)
                    .setDelay(500)
                    .setShapePadding(140)
                    .setTitleText(R.string.scSpeechControl)
                    .setContentText(R.string.scSpeechControlDesc)
                    .setDismissText(R.string.gotIt);

            sequence.addSequenceItem(speechCBuilder.build());

            //Speech Control
            MaterialShowcaseView.Builder oversleptBuilder = new MaterialShowcaseView.Builder(getActivity())
                    .setTarget(timeBisVView)
                    .setDelay(500)
                    .setShapePadding(40)
                    .setTitleText(R.string.scConsideredOverslept)
                    .setContentText(R.string.scConsideredOversleptDesc)
                    .setDismissText(R.string.gotIt);

            sequence.addSequenceItem(oversleptBuilder.build());

            //#################### Message #############################
            MaterialShowcaseView.Builder tiMessageBuilder = new MaterialShowcaseView.Builder(getActivity())
                    .setTarget(tiMessage)
                    .setDelay(500)
                    .setShapePadding(140)
                    .setTitleText(R.string.scTabMessage)
                    .setContentText(R.string.scTabMessageDesc)
                    .setDismissText(R.string.gotIt);

            sequence.addSequenceItem(tiMessageBuilder.build());

            MaterialShowcaseView.Builder listAufpasserBuilder = new MaterialShowcaseView.Builder(getActivity())
                    .setTarget(mMessagesRecyclerList)
                    .setDelay(500)
                    .setShapePadding(40)
                    .withRectangleShape()
                    .setTitleText(R.string.scMinderList)
                    .setContentText(R.string.scMinderListDesc)
                    .setDismissText(R.string.gotIt);

            sequence.addSequenceItem(listAufpasserBuilder.build());

            MaterialShowcaseView.Builder withSMSBuilder = new MaterialShowcaseView.Builder(getActivity())
                    .setTarget(mitSMS)
                    .setDelay(500)
                    .setShapePadding(40)
                    .setTitleText(R.string.scWithSms)
                    .setContentText(R.string.scWithSmsDesc)
                    .setDismissText(R.string.gotIt);

            sequence.addSequenceItem(withSMSBuilder.build());

            MaterialShowcaseView.Builder changeSMSTextBuilder = new MaterialShowcaseView.Builder(getActivity())
                    .setTarget(fabSmsText)
                    .setDelay(500)
                    .setShapePadding(40)
                    .setTitleText(R.string.scSmsText)
                    .setContentText(R.string.scSmsTextDesc)
                    .setDismissText(R.string.gotIt);

            sequence.addSequenceItem(changeSMSTextBuilder.build());

            MaterialShowcaseView.Builder withAPPBuilder = new MaterialShowcaseView.Builder(getActivity())
                    .setTarget(mitApp)
                    .setDelay(500)
                    .setShapePadding(140)
                    .setTitleText(R.string.scWithApp)
                    .setContentText(R.string.scWithApDesc)
                    .setDismissText(R.string.gotIt);

            sequence.addSequenceItem(withAPPBuilder.build());

            //#################### Send ################################

            MaterialShowcaseView.Builder tiSendBuilder = new MaterialShowcaseView.Builder(getActivity())
                    .setTarget(tiSend)
                    .setDelay(500)
                    .setShapePadding(140)
                    .setTitleText(R.string.scTabSend)
                    .setContentText(R.string.scTabSendDesc)
                    .setDismissText(R.string.gotIt);

            sequence.addSequenceItem(tiSendBuilder.build());

            MaterialShowcaseView.Builder fuerMichBuilder = new MaterialShowcaseView.Builder(getActivity())
                    .setTarget(forme)
                    .setDelay(500)
                    .setShapePadding(40)
                    .setTitleText(R.string.scForMe)
                    .setContentText(R.string.scForMeDesc)
                    .setDismissText(R.string.gotIt);

            sequence.addSequenceItem(fuerMichBuilder.build());


            sequence.setOnItemDismissedListener(new MaterialShowcaseSequence.OnSequenceItemDismissedListener() {
                @Override
                public void onDismiss(MaterialShowcaseView materialShowcaseView, int i) {
                    showCaseCount++;
                    if(showCaseCount == 3){
                        TabLayout.Tab tab = tabLayout.getTabAt(1);
                        tab.select();
                    }else if(showCaseCount == 8){
                        TabLayout.Tab tab = tabLayout.getTabAt(2);
                        tab.select();
                    }
                }
            });
            //Start ShowCase sequence
            sequence.start();
        }

        return view;
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        //Language
        if(!getString(R.string.locale).equals("default")){
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, getResources().getConfiguration().locale);
        }else{
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH);
        }
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.spokenAlarmName));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {

        }
    }

    private void nameAendern(){
        if(et.getVisibility() == View.GONE){
            et.setVisibility(View.VISIBLE);
        }else{
            et.setVisibility(View.GONE);
        }
    }

    //Tablayout mit Listener ausstatten
    private void setUpTabLayout(){
        onTabSelectedListener = new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                switch (position){
                    case 0: vf.setDisplayedChild(0); break;
                    case 1: vf.setDisplayedChild(1); break;
                    case 2:
                        if(contactMap.isEmpty() && !mock) {
                            if(!purchaseChaecked){
                                final TaskCompletionSource<Boolean> source = new TaskCompletionSource<>();
                                Task<Boolean> task = source.getTask();
                                task.addOnSuccessListener(getActivity(), hasFullVersion -> {

                                    if(hasFullVersion){
                                        Intent intent = new Intent(getContext(), ChooseFriend.class);
                                        intent.setAction("Share");
                                        intent.putExtra("HashMap", observedWecker.getShared_alarm_con());
                                        startActivityForResult(intent, 8);

                                        purchaseChaecked = true;
                                    }else if(!mock){
                                        Intent i = new Intent(getActivity(), Invite.class);
                                        i.setAction("WeckerSchicken");
                                        startActivity(i);
                                    }
                                });

                                FullVersionUtil.checkFullVersion(getContext(), mBillingClient, source);
                            }else {
                                Intent intent = new Intent(getContext(), ChooseFriend.class);
                                intent.setAction("Share");
                                intent.putExtra("HashMap", observedWecker.getShared_alarm_con());
                                startActivityForResult(intent, 8);
                            }
                        }else{
                            vf.setDisplayedChild(2);
                        }
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        };
    }

    @Override
    public void sendTime(int hourNew, int minuteNew) {
        //Display the user changed time on TextView
        if(minuteNew < 10){
            uhrzeit.setText(getString(R.string.choosenTime, hourNew, "0" + minuteNew));
        }else {
            uhrzeit.setText(getString(R.string.choosenTime, hourNew, "" + minuteNew));
        }

        //Sms Texte
        if(sms_verschlafen_text.equals(getString(R.string.sms_verschlafen, hour, String.valueOf(minute)))){
            String newMessage = getString(R.string.sms_verschlafen, hourNew, String.valueOf(minuteNew));
            if(minuteNew < 10){
                newMessage = getString(R.string.sms_verschlafen, hourNew, "0" + minuteNew);
            }
            editSmsMessageV.setText(newMessage);

            observedWecker.setSms_verschlafen(newMessage);
            liveWecker.setValue(observedWecker);
        }

        if(sms_aufgestanden_text.equals(getString(R.string.sms_aufgestanden, hour, String.valueOf(minute)))){
            String newMessage = getString(R.string.sms_aufgestanden, hourNew, String.valueOf(minuteNew));
            if(minuteNew < 10){
                newMessage = getString(R.string.sms_aufgestanden, hourNew, "0" + minuteNew);
            }
            editSmsMessageW.setText(newMessage);

            observedWecker.setSms_aufgestanden(newMessage);
            liveWecker.setValue(observedWecker);
        }


        //Übergabe Stunde und Minute an TimeSeter
        observedWecker.setHour(hourNew);
        observedWecker.setMinute(minuteNew);
        liveWecker.setValue(observedWecker);

        if(days.equals("xxxxxxxxxxxxxx")){
            firstDay();
        }else{
            //Damit sich das bei checkbox nicht doppelt weil firstDay da ja schon clickt
            verbleibendeZeit();
        }
    }

    //Wenn Alarm eingeschaltet wird
    private void firstDay() {
        switch (AlarmUtil.defaultDay(hour, minute)){
            case "Montag":  moCh.setChecked(true); break;
            case "Dienstag": diCh.setChecked(true); break;
            case "Mittwoch": miCh.setChecked(true); break;
            case "Donnerstag": doCh.setChecked(true); break;
            case "Freitag": frCh.setChecked(true); break;
            case "Samstag": saCh.setChecked(true); break;
            case "Sonntag": soCh.setChecked(true); break;
        }
    }

    //Verbleibende Zeit
    private void verbleibendeZeit(){
        if(days.equals("xxxxxxxxxxxxxx")) {
            klingeltIn = "";
            timeLeft.setText(klingeltIn);
        }else{
            final TaskCompletionSource<Integer> source = new TaskCompletionSource<>();
            Task<Integer> task = source.getTask();
            task.addOnSuccessListener(getActivity(), minutesLeft -> {
                final int houresLeft = minutesLeft / 60;
                final int minutesLeftover = minutesLeft - houresLeft * 60;

                klingeltIn = getString(R.string.klingeltInShort, houresLeft, minutesLeftover);
                timeLeft.setText(klingeltIn);
            });

            AlarmUtil.minutesLeft2(hour, minute, days, source);
        }
    }

    public String getKlingeltIn(){
        return klingeltIn;
    }

    //Anzeigen der Wörter für die Speechrecognition
    private void showSpeechDialog(){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
// ...Irrelevant code for customizing the buttons and title
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.alert_speech_info, null);
        dialogBuilder.setView(dialogView);

        //EditText editText = (EditText) dialogView.findViewById(R.id.label_field);
        //editText.setText("test label");
        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
    }


    private void playSong(String uri){
        final int ov = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);//Um danach wieder auf vorherige Lautstärke zu setzen
        play_pause.setImageResource(android.R.drawable.ic_media_pause);

        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, volume, 0);//WEnn als STREAM_ALARM mus r .setUsage(AudioAttributes.USAGE_ALARM)
        String d = uri;
        //Ringtone manager
        r = RingtoneManager.getRingtone(getActivity(), Uri.parse(uri));

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
                play_pause.setImageResource(android.R.drawable.ic_media_play);
            }
        };
        handler.postDelayed(runnable, 60000);
    }

    private void changeSong(boolean next){
        String newPath = "";
        String newTitle = "";
        if(next){
            newPath = mItems.get(nextSongInt - 1).getPath();
            newTitle = mItems.get(nextSongInt - 1).getTitle();
        }else{
            newPath = mItems.get(nextSongInt).getPath();
            newTitle = mItems.get(nextSongInt).getTitle();
        }



        //Convert Path to uri and then to String
        observedWecker.setAlarm_sound(Uri.parse("file:///" + newPath).toString());
        liveWecker.setValue(observedWecker); //TODO Ringtonemanager kriegt komischen titel

        showsSong.setText(newTitle);
    }



    //Interface Method, wenn MessageContact deleted
    @Override
    public void onDeleted (String id_user, boolean inApp, boolean trueShared){
        if(trueShared){//AlarmShared
            final DatabaseReference alarms = FirebaseDatabase.getInstance().getReference().child("alarms")
                    .child(mCurrentUserID).child("ByMe").child("" + id).child("to").child("" + id_user);

            //to contact aus FirebaseDatabase
            alarms.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()){
                        alarms.child("contactDeleted").setValue("ContactDeleted");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

            contactMap.remove(id_user);
            observedWecker.setShared_alarm_con(new JSONObject(contactMap).toString());
            liveWecker.setValue(observedWecker);

            //Wenn das der letzte Contact war
            if(contactMap.isEmpty()){
                if(messageMap.isEmpty() && smsMap.isEmpty()){
                    observedWecker.setShared('l');
                    liveWecker.setValue(observedWecker);
                }else {
                    observedWecker.setShared('m');
                    liveWecker.setValue(observedWecker);
                }
            }
        }else{

            if(inApp){
                final DatabaseReference messages = FirebaseDatabase.getInstance().getReference().child("messages")
                        .child(mCurrentUserID).child("ByMe").child("" + id).child("to").child("" + id_user);
                messages.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()){//TODO funkt nich wenn message komplett by last receiver deleted, aber OFFLINE der Kotakt gelöscht wird
                            messages.setValue("deleted");
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

                messageMap.remove(id_user);
                observedWecker.setShared_message_con(new JSONObject(messageMap).toString());
                liveWecker.setValue(observedWecker);
            }else{
                smsMap.remove(id_user);
                observedWecker.setShared_sms_con(new JSONObject(smsMap).toString());
                liveWecker.setValue(observedWecker);
            }

            if(messageMap.isEmpty() && smsMap.isEmpty()){
                if(contactMap.isEmpty()){
                    observedWecker.setShared('l');
                    liveWecker.setValue(observedWecker);
                }else{
                    observedWecker.setShared('v');
                    liveWecker.setValue(observedWecker);
                }
            }
        }
    }




    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {

            case REQUEST_PLAY_MUSIC_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //To avoide state Loss dialog is shown in onResume
                        avoidedStateLoss = true;
                        randomSong = true;
                        randomSong();
                }
                break;
            }

            case REQUEST_GET_MUSIC_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    randomSong = true;
                    randomSong();
                }
                break;
            }

            case SEND_SMS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    Intent pickContact = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                    pickContact.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
                    startActivityForResult(pickContact, 9);
                }
                break;
            }

            case REQUEST_RECORD_AUDIO_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    switchText.setText(R.string.On);
                    observedWecker.setSpeech_control(true);
                    liveWecker.setValue(observedWecker);
                }else {
                    speechControl.setChecked(false);
                }
                break;
        }
    }

    public void chooseAlarmtone(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (!randomSong){
                requestPermissions(permissions, REQUEST_PLAY_MUSIC_PERMISSION);
            }else {
                songDialog.show(getActivity().getSupportFragmentManager(), "Message Dialog");
            }

        }else{
            songDialog.show(getActivity().getSupportFragmentManager(), "Message Dialog");
        }
    }


    @Override
    public void onPause(){
        super.onPause();
        if(r != null){
            if(r.isPlaying()){
                play_pause.setImageResource(android.R.drawable.ic_media_play);
                handler.removeCallbacksAndMessages(null);
                r.stop();
            }
        }

        //Listener vom Tablayout entfernen
        tabLayout.removeOnTabSelectedListener(onTabSelectedListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        //TODO sicher ??
        if(avoidedStateLoss){
            songDialog.show(getActivity().getSupportFragmentManager(), "Message Dialog");
            avoidedStateLoss = false;
        }

        tabLayout.addOnTabSelectedListener(onTabSelectedListener);
    }

    public void randomSong(){
        //Sharedalarmdaten
        AppExecutor.getInstance().diskI0().execute(new Runnable() {
            @Override
            public void run() {
                ContentResolver mContentResolver = getActivity().getContentResolver();
                // the items (songs) we have queried
                mItems = new ArrayList<SongPOJO>();

                Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                Cursor cur = mContentResolver.query(uri, null, MediaStore.Audio.Media.DURATION + ">=59999", null, null);

                if (cur != null && cur.moveToFirst()) {

                    int pathColumn = cur.getColumnIndex(MediaStore.Audio.Media.DATA);
                    int titleColumn = cur.getColumnIndex(MediaStore.Audio.Media.TITLE);


                    //Zählen der "gespeicherten Songs"
                    int i = 0;
                    // add each song to mItems
                    do {
                        mItems.add(new SongPOJO(cur.getString(pathColumn), cur.getString(titleColumn)));
                        i++;
                    } while (cur.moveToNext() && i < 10);
                }
            }
        });
    }


    //Billing Library
    @Override
    public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchases) {

    }

    @Override
    public void onBillingSetupFinished(int responseCode) {

    }

    @Override
    public void onBillingServiceDisconnected() {

    }
}