package com.nicolai.alarm_clock.receiver_service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.nicolai.alarm_clock.R;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

/**
 *04.04.2018
 */
public class FirstLogIn_service extends JobIntentService {

    /**
     * Unique job ID for this service.
     */
    static final int JOB_ID = 1000;
    private int tries;

    /**
     * Convenience method for enqueuing work in to this service.
     */
    static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, FirstLogIn_service.class, JOB_ID, work);
    }

    private String token;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        //User Objekt einrichten
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference userObject = FirebaseDatabase.getInstance().getReference().child("users").child(user.getUid());
        userObject.child("secondPlus").setValue("1");
        userObject.child("thumb_image").setValue("");
        if(user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()){
            userObject.child("mobile_number").setValue(user.getPhoneNumber());
            userObject.child("name").setValue("");
        }else{
            userObject.child("mobile_number").setValue("");
            userObject.child("name").setValue(user.getDisplayName());
        }

        takeTokenToDatabase();
    }

    //Weilen onRefreshToken in service vor login gestoppt wird
    private void takeTokenToDatabase(){
        //Initialisierung SharedPreferences plus Editor
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mEditor = mSharedPreferences.edit();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        final String userId = user.getUid();

        FirebaseDatabase mFirebaseDatabase = FirebaseDatabase.getInstance();
        //Update FCM Token alias registrationToken
        final DatabaseReference mAlarmsDatabaseReference = mFirebaseDatabase.getReference().child("users");

        //Wenn erstellt mit google account
        if(user.getDisplayName() != null && !user.getDisplayName().isEmpty()){
            mAlarmsDatabaseReference.child(userId).child("name").setValue(user.getDisplayName());
        }

        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                token = "mock123";
                if(!instanceIdResult.getToken().isEmpty()){
                    token = instanceIdResult.getToken();
                }

                mAlarmsDatabaseReference.child(userId).child("deviceToken").setValue(token).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //Kommt wohl eigentlich nicht vor aber man weiß ja nie
                        /*if(!token.equals("mock123")) {
                            mEditor.putBoolean(getString(R.string.sharedTokenSend), true);
                            mEditor.apply();
                        }*/
                    }
                });
            }
        });
    }

    public void resentToken(){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        final String userId = user.getUid();
        FirebaseDatabase mFirebaseDatabase = FirebaseDatabase.getInstance();
        final DatabaseReference mAlarmsDatabaseReference = mFirebaseDatabase.getReference().child("users");
        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                token = "mock123";
                if(!instanceIdResult.getToken().isEmpty()){
                    token = instanceIdResult.getToken();
                }

                mAlarmsDatabaseReference.child(userId).child("deviceToken").setValue(token).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //Kommt wohl eigentlich nicht vor aber man weiß ja nie
                        if(!token.equals("mock123")) {
                            mEditor.putBoolean(getString(R.string.sharedTokenSend), true);
                            mEditor.apply();
                        }else{
                            tries +=1;
                            if(tries <=3){
                                resentToken();
                            }
                        }
                    }
                });
            }
        });
    }
}

