package com.nicolai.alarm_clock.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.Purchase;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nicolai.alarm_clock.R;
import com.nicolai.alarm_clock.pojos.FB;
import com.nicolai.alarm_clock.room_database.AlarmRepository;

import androidx.annotation.NonNull;

public class FullVersionUtil {

    public static void checkFullVersion(Context context, BillingClient mBillingClient, TaskCompletionSource<Boolean> source){
        //Hat der google user es gekauft? --> hat der app user es gekauft --> SharedPref
        Purchase.PurchasesResult purchasesResult = mBillingClient.queryPurchases(BillingClient.SkuType.INAPP);
        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        boolean purchased = false;

        //Has the user payed??
        if(purchasesResult.getPurchasesList() != null && !purchasesResult.getPurchasesList().isEmpty()){
            for (Purchase purchase : purchasesResult.getPurchasesList()) {
                if(purchase.getPackageName().equals(context.getPackageName())){
                    purchased = true;
                    break;
                }
            }
        }

        if(purchased){
            if(!mSharedPreferences.getBoolean(context.getString(R.string.sharedFullVersion), false)){
                SharedPreferences.Editor mEditor = mSharedPreferences.edit();
                mEditor.putBoolean(context.getString(R.string.sharedFullVersion), true);
                mEditor.apply();

                AlarmRepository mRepository = new AlarmRepository(context);
                mRepository.fullVersion();
            }

            source.setResult(true);
        }else {
            DatabaseReference users = FirebaseDatabase.getInstance().getReference().child(FB.USERS).child(FirebaseAuth.getInstance().getUid());
            users.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(!dataSnapshot.child(FB.FULL_VERSION).exists()){

                        source.setResult(false);
                    }else{
                        //Wenn SharedPref gelöscht updaten
                        if(!mSharedPreferences.getBoolean(context.getString(R.string.sharedFullVersion), false)){
                            SharedPreferences.Editor mEditor = mSharedPreferences.edit();
                            mEditor.putBoolean(context.getString(R.string.sharedFullVersion), true);
                            mEditor.apply();
                        }

                        //wenn value equals "payed" online gucken ob das der eingeloggte google Account war
                        if(!dataSnapshot.child(FB.FULL_VERSION).equals(FB.payed)){
                            source.setResult(true);
                        }else{
                            mBillingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.INAPP, (responseCode, purchasesList) ->{
                                if (responseCode == BillingClient.BillingResponse.OK && purchasesList != null) {
                                    for (Object purchase : purchasesList) {
                                        Purchase p = (Purchase) purchase;
                                        if(p.getSku().equals("full_version_20082016")){
                                            source.setResult(true);
                                        }else{
                                            source.setResult(false);
                                        }
                                    }
                                }
                            });
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    if(mSharedPreferences.getBoolean(context.getString(R.string.sharedFullVersion), false)){
                        source.setResult(true);
                    }else {
                        source.setResult(false);
                    }
                }
            });
        }
    }

    public static void freeLeft(TaskCompletionSource<Long> source){
        DatabaseReference freeLeft = FirebaseDatabase.getInstance().getReference().child(FB.USERS).child(FirebaseAuth.getInstance().getUid())
                .child(FB.FREE_LEFT);
        freeLeft.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    source.setResult((long) dataSnapshot.getValue());
                }else{
                    source.setResult(0L);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                source.setResult(0L);
            }
        });
    }
}
