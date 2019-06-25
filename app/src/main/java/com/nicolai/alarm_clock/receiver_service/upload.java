package com.nicolai.alarm_clock.receiver_service;

import android.content.Context;
import android.content.Intent;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nicolai.alarm_clock.pojos.FirebaseAlarmPOJO;
import com.nicolai.alarm_clock.pojos.WeckerPOJO;
import com.nicolai.alarm_clock.room_database.WeckerDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

/**
 *04.04.2018
 */
public class upload extends JobIntentService {

    private int hour, minute, anzahl;
    private long intervall;
    private boolean repeat, foreME;
    private String days, nameWecker;
    private HashMap<String, String> contactMap, appMessageMap;
    private String mCurrent_user_id;
    private int weckerID;

    private DatabaseReference mAlarmsDatabaseReference;

    /**
     * Unique job ID for this service.
     */
    static final int JOB_ID = 1000;

    /**
     * Convenience method for enqueuing work in to this service.
     */
    static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, upload.class, JOB_ID, work);
    }


    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        weckerID = intent.getIntExtra("iD", 0);

        //create database instance
        WeckerDatabase database = WeckerDatabase.getInstance(getApplication());

        WeckerPOJO wecker = database.weckerDao().findRowByID(weckerID);

        if(wecker != null){
            hour = wecker.getHour();
            minute = wecker.getMinute();
            intervall = wecker.getIntervall();
            anzahl = wecker.getAnzahl();
            days = wecker.getDays();
            repeat = wecker.isWeekly_repeat();
            nameWecker = wecker.getName();
            String contactJason = wecker.getShared_alarm_con();
            String appMessageJason = wecker.getShared_message_con();
            foreME = wecker.isFor_me();

            //Jason der Kontakte für Alarme in hashMap
            contactMap = new HashMap<String, String>();
            if (!contactJason.equals("0") && !contactJason.equals("{}")) {
                try {
                    JSONObject json = new JSONObject(contactJason);
                    JSONArray names = json.names();
                    for (int i = 0; i < names.length(); i++) {
                        String key = names.getString(i);
                        contactMap.put(key, json.optString(key));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            //Jason der Kontakte für Message in hashMap
            appMessageMap = new HashMap<String, String>();
            if (!appMessageJason.equals("0") && !appMessageJason.equals("{}")) {
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

            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            mCurrent_user_id = mAuth.getCurrentUser().getUid();

            //Alarm soll geteilt werden
            if (!contactMap.isEmpty()){
                createAlarm();

                //Message soll versendet werden
            }else if(!appMessageMap.isEmpty()){
                createMessage();
            }
        }
    }


    private void createAlarm(){
        long timeStamp = Calendar.getInstance().getTimeInMillis();
        //Inhalte Alarm
        //STATUS für alarm in detail egal, da aber selbe Klasse auch für message drin behalten
        FirebaseAlarmPOJO createAlarm = new FirebaseAlarmPOJO(nameWecker, hour, minute, anzahl, intervall, days, repeat, true, "new", weckerID, foreME, timeStamp);

        //Neuen Alarm in ByMe erstellen (Name wird quasi zu push)
        mAlarmsDatabaseReference = FirebaseDatabase.getInstance().getReference()
                .child("alarms").child(mCurrent_user_id).child("ByMe").child("" + weckerID);

        //Details für Alarm
        mAlarmsDatabaseReference.child("details").setValue(createAlarm);

        mAlarmsDatabaseReference.child("to").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(HashMap.Entry<String, String> entry : contactMap.entrySet()){//TODO MAybe in cloud
                    if(!dataSnapshot.hasChild(entry.getKey())){
                        mAlarmsDatabaseReference.child("to").child(entry.getKey()).child("status").setValue("not downloaded yet");
                    }

                    if(!appMessageMap.isEmpty()){
                        createMessage();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                if(!appMessageMap.isEmpty()){
                    createMessage();
                }
            }
        });
    }

    private void createMessage(){
        long timeStamp = Calendar.getInstance().getTimeInMillis();
        //Inhalte Message
        FirebaseAlarmPOJO createMessage = new FirebaseAlarmPOJO(nameWecker, hour, minute, anzahl, intervall, days, repeat, true, "not used yet", weckerID, foreME, timeStamp);

        //Neue Message in ByMe
        DatabaseReference mMessagesDatabaseReference = FirebaseDatabase.getInstance().getReference()
                .child("messages").child(mCurrent_user_id).child("ByMe").child("" + weckerID);

        //Details für Alarm
        mMessagesDatabaseReference.child("details").setValue(createMessage);

        /*//Status online || deletedByEmpfaenger || aweake || downloaded
        HashMap<String, String> toStatus = new HashMap<>();
        toStatus.put("downloaded", "not_yet");*/

        Map<String, Object> to = new HashMap<String, Object>();
        //Alle Kontakte als childs von "to"
        for(HashMap.Entry<String, String> entry : appMessageMap .entrySet()){
            to.put(entry.getKey(),"");//toStatus);
        }

        mMessagesDatabaseReference.child("to").updateChildren(to);

        /*mMessagesDatabaseReference = mAlarmsDatabaseReference.child("to");

        //Alle Kontakte als childs von "to"
        for(HashMap.Entry<String, String> entry : appMessageMap.entrySet()){
            mAlarmsDatabaseReference.child(entry.getKey()).child("downloaded").setValue("not_yet");//TODO so oder bei Alarm mit updateChildren
        }*/
    }
}
