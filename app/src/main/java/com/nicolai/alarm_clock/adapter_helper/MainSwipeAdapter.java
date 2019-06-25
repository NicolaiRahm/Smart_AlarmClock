package com.nicolai.alarm_clock.adapter_helper;

import android.content.Context;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.nicolai.alarm_clock.Alarms;
import com.nicolai.alarm_clock.BuildConfig;
import com.nicolai.alarm_clock.FriendsFragment;
import com.nicolai.alarm_clock.R;
import com.nicolai.alarm_clock.Timer;

import java.util.HashMap;
import java.util.Map;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;


/**
 * Created by Nicolai on 04.08.2017.
 */

public class MainSwipeAdapter extends FragmentPagerAdapter {

    public Context context;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    private static final String TABLAYOU_GETEILT = "tablayout_geteilt";
    CharSequence geteilt;

    public MainSwipeAdapter(FragmentManager fm, Context c) {
        super(fm);
        context = c;

        //Seitentitel
        geteilt = context.getText(R.string.geteilt);

        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        //RemoteConfig via Firebase
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        mFirebaseRemoteConfig.setConfigSettings(configSettings);

        Map<String, Object> defaultConfigMap = new HashMap<>();
        defaultConfigMap.put(TABLAYOU_GETEILT, "Blub");//context.getText(R.string.geteilt));
        mFirebaseRemoteConfig.setDefaults(defaultConfigMap);

        //fetchConfig();
    }

    //RemoteConfig Daten laden TODO
    /*private void fetchConfig(){
        long cachExpiration = 3600;

        if(mFirebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()){
            cachExpiration = 0;
        }
        mFirebaseRemoteConfig.fetch(cachExpiration)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        mFirebaseRemoteConfig.activateFetched();
                        applyRetrievedPageTitle();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        applyRetrievedPageTitle();
                    }
                });
    }

    private void applyRetrievedPageTitle(){
        geteilt = mFirebaseRemoteConfig.getString(TABLAYOU_GETEILT);
    }*/

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position){
        switch (position) {
            case 0:
                return  context.getText(R.string.Alarme);
            case 1:
                return  context.getText(R.string.Timer);
            case 2:
                return  context.getText(R.string.FriendsFragment);
            default:
                return null;
        }
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new Alarms();
            case 1:
                return new Timer();
            case 2:
                return new FriendsFragment();
            default:
                return null;
        }
    }
}