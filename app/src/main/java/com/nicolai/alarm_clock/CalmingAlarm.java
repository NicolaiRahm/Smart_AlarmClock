package com.nicolai.alarm_clock;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;

public class CalmingAlarm extends MultiDexApplication {


    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(context);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        KeyguardManager myKM = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

        boolean islocked = true;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1){
            islocked = myKM.isDeviceLocked();
        }else {
            islocked = myKM.inKeyguardRestrictedInputMode();
        }
        if(!islocked) {
            FirebaseApp.initializeApp(this);
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);

            /*Weil user und messages for me in Profile gebraucht werden und das nur wenn man gezielt
            *drauf ist die daten läd. Wär blöd wenn das dann offline nicht funkt!
            */
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if(user != null){
                FirebaseDatabase.getInstance().getReference().child("users").child(user.getUid()).keepSynced(true);
                FirebaseDatabase.getInstance().getReference().child("messages").child(user.getUid()).child("ForMe").keepSynced(true);
            }

            //Vectordrawables
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

            //Picasso
            Picasso.Builder builder = new Picasso.Builder(this);
            builder.downloader(new OkHttp3Downloader(this, Integer.MAX_VALUE));
            Picasso built = builder.build();
            built.setIndicatorsEnabled(true);
            built.setLoggingEnabled(true);
            Picasso.setSingletonInstance(built);
        }

    }
}
