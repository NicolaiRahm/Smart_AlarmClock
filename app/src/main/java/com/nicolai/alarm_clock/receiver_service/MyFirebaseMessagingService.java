package com.nicolai.alarm_clock.receiver_service;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.ContactsContract;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.nicolai.alarm_clock.R;
import com.nicolai.alarm_clock.TimeSeter;
import com.nicolai.alarm_clock.pojos.WeckerPOJO;
import com.nicolai.alarm_clock.room_database.WeckerDatabase;
import com.nicolai.alarm_clock.util.AlarmUtil;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private String title;
    private String body;
    private String click_action;

    private SharedPreferences mSharedPreferences;
    private DatabaseReference forMe, mTrusted, detailsNew;
    private WeckerDatabase database;
    private String current_user_id;

    static int lastNewId;




    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        //Zu was ist die Notification
        String type = remoteMessage.getData().get("type");
        String number = remoteMessage.getData().get("number");
        String user_name = remoteMessage.getData().get("user_name");



        //Look for number in contacts
        if (!number.isEmpty() && ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            String contactName = getContactDisplayNameByNumber(number);
            if (!contactName.equals("?")) {
                user_name = contactName;
            }
        }

        //Passende texte für Notification
        switch (type){
            case "freundschaftsanfrage":
                title = getString(R.string.Freundschaftsanfrage);
                body = getString(R.string.FreundschaftsanfrageBody, user_name);
                click_action = "com.nicolai.CalmingAlarm_FRIENDREQ_NOTIFICATION";
                break;

            case "new_alarm":
                title = getString(R.string.Wecker);
                body = getString(R.string.WeckerBody, user_name);
                break;

            case "updated_alarm":
                title = getString(R.string.Weckerupdate);
                body = getString(R.string.WeckerupdateBody, user_name);
                break;

            case "deleted_alarm":
                title = getString(R.string.WeckerDeleted);
                body = getString(R.string.WeckerDeletedBody, user_name);
                break;

            case "uascontact_deleted_alarm":
                title = getString(R.string.WeckerDeleted);
                body = getString(R.string.FromWeckerDeletedBody, user_name);
                break;



            case "new_message":
                title = getString(R.string.Weckernachricht);
                body = getString(R.string.WeckernachrichtBody, user_name);
                break;

            case "updated_message":
                title = getString(R.string.Weckernachrichtupdate);
                body = getString(R.string.WeckernachrichtupdateBody, user_name);
                break;

            case "deleted_message":
                title = getString(R.string.WeckernachrichtDeleted);
                body = getString(R.string.WeckernachrichtDeletedBody, user_name);
                break;

            case "uascontact_deleted_message":
                title = getString(R.string.NachrichtDeleted);
                body = getString(R.string.FromNachrichtDeletedBody, user_name);
                break;

            case "ur_message_deleted":
                title = getString(R.string.MyMessageDeleted);
                body = getString(R.string.MyMessageDeletedBody, user_name);
                break;
        }



        //super.onMessageReceived(remoteMessage);
        String channelId = "notification";//getString(R.string.default_notification_channel_id);
        //Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        //TODO mit dem aus string für übersetzung?!
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.mipmap.ic_launcher_round)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setAutoCancel(true);
                        //.setSound(defaultSoundUri);

        //Intent
        if(click_action == null || click_action.isEmpty()){
            click_action = "com.nicolai.CalmingAlarm_SIMPLE_NOTIFICATION";
        }

        Intent resultIntent = new Intent();
        resultIntent.setAction(click_action);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, -1, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(resultPendingIntent);

        //Manager
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "AlarmCloud", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, notificationBuilder.build());


        //######################## Trusted #####################################

        //Initialisierung SharedPreferences
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        //Damit Service sich nicht mit sharedAlarm() in Alarms.java doppelt
        if(mSharedPreferences.getBoolean("background", true)){

            if (type.equals("new_alarm")) {
                String sender_id = remoteMessage.getData().get("sender_id");
                String wecker_id = remoteMessage.getData().get("weckerId");

                sharedAlarm(true, sender_id, wecker_id);
            }else if(type.equals("updated_alarm")){
                String sender_id = remoteMessage.getData().get("sender_id");
                String wecker_id = remoteMessage.getData().get("weckerId");

                sharedAlarm(false, sender_id, wecker_id);
            }
        }
    }

    public void sharedAlarm(final boolean newAlarm, final String sender_id, final String wecker_id){
        current_user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
        forMe = FirebaseDatabase.getInstance().getReference().child("alarms").child(current_user_id).child("ForMe");
        database = WeckerDatabase.getInstance(getApplicationContext());
        //Trusted?!
        mTrusted = FirebaseDatabase.getInstance().getReference().child("Trusted").child(current_user_id);
        mTrusted.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.hasChild(sender_id)) {

                    if (newAlarm) {
                        //Neuen Alarm einfügen
                        insertSharedAlarm(sender_id, wecker_id);

                    } else{
                        insertUpdatedAlarm(sender_id, wecker_id, current_user_id);
                    }
                }
                /*else if(status.equals("deleted") || status.equals("UASContactDeleted")){
                    if(!myWecker_id.isEmpty()){
                        dataSource.open();
                        dataSource.updateShared(Integer.parseInt(myWecker_id), "d");
                        dataSource.close();

                        //Alarm aussetzen bis neue details bestätigt sind
                        TimeSeter.off(Integer.parseInt(myWecker_id), getActivity());

                        //Update in RecyclerView
                        populateTHIS();
                    }
                    //Remove auch für mich
                    forMe.child(dataSnapshot1.getKey()).removeValue();
                }*/
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //Neu Alarm? in Room
    public void insertSharedAlarm(final String sender_id, final String wecker_id){
        //Alarm Details
        detailsNew = FirebaseDatabase.getInstance().getReference()
                .child("alarms").child(sender_id).child("ByMe").child(wecker_id);

        detailsNew.child("details").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot2) {
                //Sharedalarmdaten
                long hour = (long) dataSnapshot2.child("hour").getValue();
                long minute = (long) dataSnapshot2.child("minute").getValue();
                long intervall = (long) dataSnapshot2.child("intervall").getValue();
                long anzahl = (long) dataSnapshot2.child("anzahl").getValue();
                String days = dataSnapshot2.child("days").getValue().toString();
                boolean repeat = (boolean) dataSnapshot2.child("repeat").getValue();
                String name = dataSnapshot2.child("name").getValue().toString();

                //Initialisierung SharedPreferences plus Editor
                SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

                //Standard-/Defaulteinstellungen
                //Default volume
                AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(AUDIO_SERVICE);
                int halfVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING) / 2;
                final int volume = mSharedPreferences.getInt("volume", halfVolume);
                //Default Ringtone
                Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                String defaultTone = uri.toString();
                final String alarmSound = mSharedPreferences.getString("sound", defaultTone);
                boolean voiceCtrl = mSharedPreferences.getBoolean(getString(R.string.sharedVoicecontrol), false);
                int duration = mSharedPreferences.getInt("duration", 30);

                int secondsBis = mSharedPreferences.getInt(getString(R.string.sharedTimeBisVerschlafen), 60);

                final WeckerPOJO trustedWecker = new WeckerPOJO(name, days, repeat, (int) hour, (int) minute, (int) anzahl, (int) intervall,
                        duration, volume, alarmSound, voiceCtrl, 'l', "", "", "", true, sender_id, wecker_id, "", "",
                        true, "", "", secondsBis);
                if(database.weckerDao().bySender_id(sender_id, wecker_id) == null) {
                    //User name and image
                    addUserDetails(sender_id, wecker_id, trustedWecker);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void addUserDetails(final String sender_id, final String wecker_id, final WeckerPOJO trustedWecker){
        //User Details
        DatabaseReference user_details = FirebaseDatabase.getInstance().getReference()
                .child("users").child(sender_id);

        user_details.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChild("name")){
                    String vonXY = dataSnapshot.child("name").getValue().toString();
                    String thumb_image = dataSnapshot.child("thumb_image").getValue().toString();

                    trustedWecker.setVon_xy(vonXY);
                    trustedWecker.setThumb_img(thumb_image);

                    //Neuen Wecker einfügen
                    final int sqLite_id = (int) database.weckerDao().insertWecker(trustedWecker);

                    AlarmUtil.setAlarm(getApplicationContext(), trustedWecker);

                    //Status plus weckerID in ForMe ändern
                    forMe.child("" + sender_id + wecker_id).child("status").setValue("trusted_bestaetigt");
                    forMe.child("" + sender_id + wecker_id).child("weckerID").setValue("" + sqLite_id);
                    //Status und date unter ByMe/to/empfänger/status
                    DatabaseReference BySender = FirebaseDatabase.getInstance().getReference()
                            .child("alarms").child(sender_id).child("ByMe").child(wecker_id).child("to").child(current_user_id);
                    BySender.child("status").setValue("downloaded");
                    long timeStamp = Calendar.getInstance().getTimeInMillis();
                    BySender.child("date").setValue(timeStamp);

                    stopSelf();
                }else{
                    String vonXY = getResources().getString(R.string.user_deleted);
                    String thumb_image = "";

                    trustedWecker.setVon_xy(vonXY);
                    trustedWecker.setThumb_img(thumb_image);

                    //Neuen Wecker einfügen
                    final int sqLite_id = (int) database.weckerDao().insertWecker(trustedWecker);

                    AlarmUtil.setAlarm(getApplicationContext(), trustedWecker);

                    //Status plus weckerID in ForMe ändern
                    forMe.child("" + sender_id + wecker_id).child("status").setValue("downloaded");
                    forMe.child("" + sender_id + wecker_id).child("weckerID").setValue("" + sqLite_id);

                    stopSelf();
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                stopSelf();
            }
        });
    }

    //Updated Alarm? in SQLite
    public void insertUpdatedAlarm(final String sender_id, final String wecker_id, final String current_user_id){
        //Alarm Details
        DatabaseReference detailsUpdatedAlarm = FirebaseDatabase.getInstance().getReference()
                .child("alarms").child(sender_id).child("ByMe").child(wecker_id).child("details");
        detailsUpdatedAlarm.keepSynced(true);

        detailsUpdatedAlarm.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    //Sharedalarmdaten
                    long hour = (long) dataSnapshot.child("hour").getValue();
                    long minute = (long) dataSnapshot.child("minute").getValue();
                    long intervall = (long) dataSnapshot.child("intervall").getValue();
                    long anzahl = (long) dataSnapshot.child("anzahl").getValue();
                    String days = dataSnapshot.child("days").getValue().toString();
                    boolean repeat = (boolean) dataSnapshot.child("repeat").getValue();
                    String name = dataSnapshot.child("name").getValue().toString();

                    WeckerPOJO updatedWecker = database.weckerDao().bySender_id(sender_id, wecker_id);
                    updatedWecker.setHour((int) hour);
                    updatedWecker.setMinute((int) minute);
                    updatedWecker.setIntervall((int) intervall);
                    updatedWecker.setAnzahl((int) anzahl);
                    updatedWecker.setDays(days);
                    updatedWecker.setWeekly_repeat(repeat);
                    updatedWecker.setName(name);
                    //Mit neuem Wecker updaten
                    database.weckerDao().updateWecker(updatedWecker);

                    AlarmUtil.setAlarm(getApplicationContext(), updatedWecker);

                    //Status in ForMe ändern
                    forMe.child("" + sender_id + wecker_id).child("status").setValue("trusted_update_bestaetigt");
                    //Status unter ByMe/to/empfänger/status
                    DatabaseReference BySender = FirebaseDatabase.getInstance().getReference()
                            .child("alarms").child(sender_id).child("ByMe").child(wecker_id).child("to").child(current_user_id);
                    BySender.child("status").setValue("update_downloaded");
                    long timeStamp = Calendar.getInstance().getTimeInMillis();
                    BySender.child("date").setValue(timeStamp);
                    stopSelf();
                }else{
                    stopSelf();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                stopSelf();
            }
        });
    }

    //Get contact name for number
    public String getContactDisplayNameByNumber(String number) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        String name = "?";

        ContentResolver contentResolver = getApplicationContext().getContentResolver();
        Cursor contactLookup = contentResolver.query(uri, new String[] {BaseColumns._ID,
                ContactsContract.PhoneLookup.DISPLAY_NAME }, null, null, null);

        try {
            if (contactLookup != null && contactLookup.getCount() > 0) {
                contactLookup.moveToNext();
                name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                //String contactId = contactLookup.getString(contactLookup.getColumnIndex(BaseColumns._ID));
            }
        } finally {
            if (contactLookup != null) {
                contactLookup.close();
            }
        }

        return name;
    }


    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if(user != null){
            String userId = user.getUid();

            FirebaseDatabase mFirebaseDatabase = FirebaseDatabase.getInstance();
            //Update FCM Token alias registrationToken
            DatabaseReference mAlarmsDatabaseReference = mFirebaseDatabase.getReference().child("users");
            Map<String, Object> deviceToken = new HashMap<String,Object>();
            deviceToken.put("deviceToken", s);

            mAlarmsDatabaseReference.child(userId).updateChildren(deviceToken);
        }
    }
}