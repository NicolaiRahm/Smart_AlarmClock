package com.nicolai.alarm_clock;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.dynamiclinks.DynamicLink;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.ShortDynamicLink;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class Invite extends AppCompatActivity implements PurchasesUpdatedListener, BillingClientStateListener {

    private Uri mInvitationUrl;
    private TextView linkView, invitesView, plural;
    private SeekBar seekBar;
    private SharedPreferences mSharedPreferences;
    private int maxProgress;

    private int invitesCount;

    private FirebaseRemoteConfig remoteConfig;
    private TextView forFree;

    private BillingClient mBillingClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite);

        //Remove Overdraw with Them background by activity
        getWindow().setBackgroundDrawable(null);

        linkView = findViewById(R.id.linkView);

        //Pay disablen?!
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if(mSharedPreferences.getBoolean(getString(R.string.sharedFullVersion), false)){
            FloatingActionButton fabPay = findViewById(R.id.fabPay);
            fabPay.setEnabled(false);
        }

        //Initialisierung Toolbar
        Toolbar toolbar = findViewById(R.id.toolbarShare);
        setSupportActionBar(toolbar);
        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();
        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setTitle(getString(R.string.invite));

        //Wurd schon ein Link erstellt?
        String savedUri = mSharedPreferences.getString(getString(R.string.sharedInviteLink), "");

        if(!savedUri.isEmpty()){
            mInvitationUrl = Uri.parse(savedUri);
            linkView.setText(savedUri);
        }else {
            //Create the link
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            String uid = user.getUid();
            String link = "https://com.nicolai.alarm_clock/?invitedby=" + uid;

            DynamicLink dynamicLink = FirebaseDynamicLinks.getInstance().createDynamicLink()
                    .setLink(Uri.parse(link))
                    .setDynamicLinkDomain("tucktack.page.link")
                    // Open links with this app on Android
                    .setAndroidParameters(new DynamicLink.AndroidParameters.Builder(BuildConfig.APPLICATION_ID).build())
                    // Open links with com.example.ios on iOS
                    .setIosParameters(new DynamicLink.IosParameters.Builder("com.example.ios").build())
                    .buildDynamicLink();

            Uri dynamicLinkUri = dynamicLink.getUri();

            //Könnt man auch direkt shorten aber wegen bugg in 16.1.1
            FirebaseDynamicLinks.getInstance().createDynamicLink()
                    .setLongLink(dynamicLinkUri)
                    .buildShortDynamicLink()
                    .addOnCompleteListener(this, new OnCompleteListener<ShortDynamicLink>() {
                        @Override
                        public void onComplete(@NonNull Task<ShortDynamicLink> task) {
                            if (task.isSuccessful()) {
                                // Short link created
                                mInvitationUrl = task.getResult().getShortLink();
                                SharedPreferences.Editor mEditor = mSharedPreferences.edit();
                                mEditor.putString(getString(R.string.sharedInviteLink), mInvitationUrl.toString());
                                mEditor.apply();

                                Uri flowchartLink = task.getResult().getPreviewLink();
                                linkView.setText(mInvitationUrl.toString());
                            } else {
                                linkView.setText(task.getException().toString());
                            }
                        }
                    });
        }

        //RemoteConfig für needed invites
        remoteConfig = FirebaseRemoteConfig.getInstance();
        if(mSharedPreferences.getInt("inviteCountRef", 0) == 0){
            remoteConfig.setDefaults(R.xml.remote_config_defaults);
            maxProgress = 2;
        }else{
            remoteConfig.setDefaults(mSharedPreferences.getInt("inviteCountRef", 0));
            maxProgress = mSharedPreferences.getInt("inviteCountRef", 0);
        }

        //ForFree Hinweis
        forFree = findViewById(R.id.forFree);
        if(mSharedPreferences.getBoolean(getString(R.string.sharedFullVersion), false)){
            forFree.setText(getString(R.string.bereitsFreigeschaltet));
        }else {
            forFree.setText(getString(R.string.inviteExplanation, maxProgress));
        }

        //Successful invites view
        invitesView = findViewById(R.id.invitesView);

        //Invites Progress
        seekBar = findViewById(R.id.invitesProgress);
        seekBar.getThumb().mutate().setAlpha(0);
        seekBar.setMax(maxProgress);
        //to disable dragging
        seekBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });

        //"erfolgreiche Einladung(en)"
        plural = findViewById(R.id.successfulInvitesPlural);
        plural.setText(getResources().getQuantityString(R.plurals.inviteSuccessful, 0));

        //Get invites count
        DatabaseReference shared = FirebaseDatabase.getInstance().getReference().child("shared")
                .child(FirebaseAuth.getInstance().getUid());
        shared.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    invitesCount = (int) dataSnapshot.getChildrenCount();

                    setDataAfterCount();

                    //von 5 auf 2 geht nur wenn der aktuelle count < 2 ist weil sonst alle mit 2 hochgezogen werden würden
                    if(invitesCount < mSharedPreferences.getInt("inviteCountRef", 0) || mSharedPreferences.getInt("inviteCountRef", 0) == 0){
                        //Get maxProgess with remote
                        setDataText();
                    }else{
                        forFree.setText(getString(R.string.unlocked, maxProgress));
                    }
                }else{
                    //Get maxProgess with remote
                    setDataText();
                }

                //Show no full version yet dialog explanation
                String goal = getIntent().getAction();
                if(goal != null){
                    switch (goal){
                        case "CC": noFullVersionYet(getString(R.string.control_center));break;
                        case "WeckerSchicken": noFullVersionYet(getString(R.string.sendWeckerUnlocked));break;
                        case "Aufpasser": noFullVersionYet(getString(R.string.aufpasserUnlocked));break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                seekBar.setMax(2);
                forFree.setText(getString(R.string.inviteExplanation, 2));

                //Show no full version yet dialog explanation
                String goal = getIntent().getAction();
                if(goal != null){
                    switch (goal){
                        case "CC": noFullVersionYet(getString(R.string.control_center)); break;
                        case "WeckerSchicken": noFullVersionYet(getString(R.string.sendWeckerUnlocked)); break;
                        case "Aufpasser": noFullVersionYet(getString(R.string.aufpasserUnlocked));break;
                    }
                }
            }
        });


        //Billing set up
        mBillingClient = BillingClient.newBuilder(this).setListener(this).build();
        mBillingClient.startConnection(this);

    }

    public void setDataText(){
        if(!mSharedPreferences.getBoolean(getString(R.string.sharedFullVersion), false)){
            DatabaseReference fullVersion = FirebaseDatabase.getInstance().getReference().child("users")
                    .child(FirebaseAuth.getInstance().getUid()).child("full_version");
            fullVersion.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(!dataSnapshot.exists()){
                        //Wenn schon passende invitescount hatte nicht erhöhen
                        //Fetch from firebase
                        remoteConfig.activateFetched();
                        remoteConfig.fetch().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                //Anzahl der Einladungen bis free mit remoteConfig
                                maxProgress = (int) remoteConfig.getLong("invite_count");

                                if(invitesCount < maxProgress){
                                    if(maxProgress == 0){
                                        seekBar.setMax(2);
                                        forFree.setText(getString(R.string.inviteExplanation, 2));
                                        maxProgress = 2;
                                    }else{
                                        seekBar.setMax(maxProgress);
                                        forFree.setText(getString(R.string.inviteExplanation, maxProgress));
                                    }

                                    SharedPreferences.Editor editor = mSharedPreferences.edit();
                                    editor.putInt("inviteCountRef", maxProgress);
                                    editor.apply();
                                }
                            }
                        });
                    }else{
                        SharedPreferences.Editor mEditor = mSharedPreferences.edit();
                        mEditor.putBoolean(getString(R.string.sharedFullVersion), true);
                        mEditor.apply();

                        forFree.setText(R.string.bereitsFreigeschaltet);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    //Wenn schon passende invitescount hatte nicht erhöhen
                    //Fetch from firebase
                    remoteConfig.activateFetched();
                    remoteConfig.fetch().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            //Anzahl der Einladungen bis free mit remoteConfig
                            maxProgress = (int) remoteConfig.getLong("invite_count");

                            if(invitesCount < maxProgress){
                                if(maxProgress == 0){
                                    seekBar.setMax(2);
                                    forFree.setText(getString(R.string.inviteExplanation, 2));
                                    maxProgress = 2;
                                }else{
                                    seekBar.setMax(maxProgress);
                                    forFree.setText(getString(R.string.inviteExplanation, maxProgress));
                                }

                                SharedPreferences.Editor editor = mSharedPreferences.edit();
                                editor.putInt("inviteCountRef", maxProgress);
                                editor.apply();
                            }
                        }
                    });
                }
            });
        }else{
            forFree.setText(R.string.bereitsFreigeschaltet);
        }
    }

    public void setDataAfterCount(){
        //Count view
        invitesView.setText(String.valueOf(invitesCount));

        //Plural text View
        plural.setText(getResources().getQuantityString(R.plurals.inviteSuccessful, invitesCount));

        //Progress view
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            if(invitesCount > maxProgress){
                seekBar.setProgress(maxProgress, true);
            }else{
                seekBar.setProgress(invitesCount, true);
            }

        }else {
            if(invitesCount > maxProgress){
                seekBar.setProgress(maxProgress);
            }else{
                seekBar.setProgress(invitesCount);
            }
        }
    }

    public void chooser(View view){
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.inviteMessage,  mInvitationUrl));
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, getString(R.string.inviteChooser)));
    }

    public void fullVersionInfo(View view){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.fullVersion)
            .setMessage(R.string.fullVersionText)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {}
            })
            .show();
    }

    public void noFullVersionYet(String goal){
        String text = String.format(getString(R.string.noFullVersionText), goal, maxProgress);
        CharSequence styledText = Html.fromHtml(text);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.noFullVersion)
                .setMessage(styledText)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {}
                })
                .show();
    }

    public void purchase(View view){
        if(mBillingClient.isReady()){
            //Purchase flow
            BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                    .setSku("full_version_20082016")
                    .setType(BillingClient.SkuType.INAPP)
                    .build();

            mBillingClient.launchBillingFlow(this, flowParams);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBillingSetupFinished(int billingResponseCode) {
        /*if (billingResponseCode == BillingClient.BillingResponse.OK) {
            /*List skuList = new ArrayList<>();
            skuList.add("premium_upgrade");
            skuList.add("gas");
            SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
            params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
            mBillingClient.querySkuDetailsAsync(params.build(),
                    new SkuDetailsResponseListener() {
                        @Override
                        public void onSkuDetailsResponse(int responseCode, List skuDetailsList) {
                            if (responseCode == BillingClient.BillingResponse.OK && skuDetailsList != null) {
                                for (Object skuDetails : skuDetailsList) {
                                    SkuDetails skuDetails1 = (SkuDetails) skuDetails;
                                    String sku = skuDetails1.getSku();
                                    String price = skuDetails1.getPrice();
                                    if ("premium_upgrade".equals(sku)) {
                                        String mPremiumUpgradePrice = price;
                                    }
                                }
                            }
                        }
                    });
        }else {
            //Toast.makeText(this, "ERROR", Toast.LENGTH_SHORT).show();
        }*/
    }

    @Override
    public void onBillingServiceDisconnected() {
        mBillingClient.startConnection(this);
    }

    @Override
    public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchases) {
        if (responseCode == BillingClient.BillingResponse.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                //purchases.get(0).
            }
            //In der datenbank freischalten
            DatabaseReference users = FirebaseDatabase.getInstance().getReference().child("users").child(FirebaseAuth.getInstance().getUid());
            users.child("full_version").setValue(true);
            //In die sharedPref eintragen
            SharedPreferences.Editor mEditor = mSharedPreferences.edit();
            mEditor.putBoolean(getString(R.string.sharedFullVersion), true);
            mEditor.apply();

        } else if (responseCode == BillingClient.BillingResponse.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
        } else {
            int r = responseCode;
            if(r == 7){
                Toast.makeText(this, "Failure to purchase since item is already owned", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
