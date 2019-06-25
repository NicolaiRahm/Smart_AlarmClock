package com.nicolai.alarm_clock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.AlarmClock;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.nicolai.alarm_clock.adapter_helper.MainSwipeAdapter;
import com.nicolai.alarm_clock.receiver_service.FirstLogIn_service;
import com.nicolai.alarm_clock.room_database.WeckerDatabase;
import com.nicolai.alarm_clock.viewmodels.ViewModel_MainTimer;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.app.JobIntentService;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;

public class MainActivity extends AppCompatActivity implements FriendsFragment.RequestCount {

    public ViewModel_MainTimer vmTimer;

    public ViewPager viewPager;
    public TabLayout tabLayout;
    private FloatingActionButton fab;
    private Toolbar toolbar;

    protected String number, email, referrerUid, token;
    protected boolean bereitsGestartet = false;
    private boolean triedLogIn = false, justCreated;

    private FirebaseAuth mFirebseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    private View viewTab;
    private TextView buddies;

    private MainSwipeAdapter swipeAdapter;

    private ViewPager.OnPageChangeListener onPageChangeListener;

    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;

    private static final int RC_SIGN_IN = 123;
    public static final String ADDITIONAL_INFO_NUMBER = "number";
    public static final String ADDITIONAL_INFO_NAME = "name";

    public static final int RINGTONE_CHOSEN_TIMER = 11;

    static final int JOB_ID = 1000;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Response of FirebaseUI
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK) {

                // Successfully signed in
                final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if(user.getDisplayName() != null && !user.getDisplayName().isEmpty()){
                    Toast.makeText(MainActivity.this, getString(R.string.greating, user.getDisplayName()), Toast.LENGTH_SHORT).show();
                }

                //Wenn neu installiert ist der name noch nicht drin --> dem Sender ein Invite gutschreiben!
                DatabaseReference userName = FirebaseDatabase.getInstance().getReference().child("users").child(user.getUid())
                        .child("secondPlus");
                userName.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
                        if(!dataSnapshot.exists()){
                            //Is already payed?
                            //User Object
                            //Token
                            Intent firstLogIn = new Intent();
                            JobIntentService.enqueueWork(MainActivity.this, FirstLogIn_service.class, JOB_ID, firstLogIn);

                            //Ist die app über einen deep link neu geöffnet worden? --> Share gut schreiben --> name und token
                            checkForDeepLink();
                        }else{
                            mEditor.putBoolean("new", false);
                            mEditor.apply();

                            //Weil bei logout die has full version sharedPref gelöscht wird
                            dataSnapshot.getRef().getParent().child("full_version").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if(dataSnapshot.exists()){
                                        mEditor.putBoolean(getString(R.string.sharedFullVersion), true);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });
            } else if(resultCode == RESULT_CANCELED){
                //wenn error --> nicht durch backbutton abgebrochen
                if(response != null){

                    AlertDialog.Builder ad = new AlertDialog.Builder(this)
                            .setTitle(R.string.loginFailed)
                            .setMessage(getString(R.string.tryAgain, response.getError().toString()))
                            .setPositiveButton(R.string.Ja, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    signIn();
                                }
                            }).setNegativeButton(R.string.Nein, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            });
                            ad.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    finish();
                                }
                            });

                            ad.show();

                }else {
                    finish();
                }
            }
        }else if(requestCode == RINGTONE_CHOSEN_TIMER && resultCode == RESULT_OK){
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            vmTimer.setSound(uri.toString());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setTheme(R.style.AppThemeDark);
        setContentView(R.layout.activity_scrolling);

        //Remove Overdraw with Them background by activity
        getWindow().setBackgroundDrawable(null);

        //Damit aus AuthstateListener bei resume Alarms nich in Alarms und vo da resumed wird
        justCreated = true;

        //User loged in or not
        mFirebseAuth = FirebaseAuth.getInstance();

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        //changeToolbarFont(toolbar, this);

        //Initialisierung SharedPreferences plus Editor
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mEditor = mSharedPreferences.edit();

        //Initialisierung SwipeAdapter
        swipeAdapter = new MainSwipeAdapter(getSupportFragmentManager(), getApplicationContext());

        //Initialisierung ViewPager (für swipen zwischen Wecker und beruhigenden Einstellungen)
        viewPager = (ViewPager) findViewById(R.id.view_pagerMain);
        viewPager.setOffscreenPageLimit(3);

        tabLayout = (TabLayout) findViewById(R.id.tabLayout);

        vmTimer = ViewModelProviders.of(this).get(ViewModel_MainTimer.class);
        vmTimer.isChooseRingtone().observe(this, isChosen -> {
            if (isChosen){
                vmTimer.setChooseRingtone(false);
                ringtoneIntent(RINGTONE_CHOSEN_TIMER);
            }
        });

        //Initialisierung Floating Actionbutton + onClickListener
        fab = (FloatingActionButton) findViewById(R.id.fab_add);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //Wird zum add Buddies Button, wenn viewpager auf friendsFragment
                if(viewPager.getCurrentItem() == 2){
                    //Intent mit übergabe der ID der neuen Reihe an den TimeSeter
                    Intent myIntent = new Intent(MainActivity.this, AllUsers.class);
                    startActivity(myIntent);
                }else if(viewPager.getCurrentItem() == 0){
                    //Sinds schon 30 oder mehr?!
                    AppExecutor.getInstance().diskI0().execute(new Runnable() {
                        @Override
                        public void run() {
                            WeckerDatabase database = WeckerDatabase.getInstance(getApplicationContext());
                            final int anzahlWecker = database.weckerDao().idList().size();

                            //In UI Thread die guidline mit der ermittelten größe anpassen
                            runOnUiThread(new Runnable(){
                                @Override
                                public void run() {
                                    if(anzahlWecker < 30){
                                        //Intent mit übergabe der ID der neuen Reihe an den TimeSeter
                                        Intent myIntent = new Intent(MainActivity.this, TimeSeter.class);
                                        myIntent.putExtra("neu", 1);
                                        startActivity(myIntent);
                                    }else{
                                        Toast.makeText(MainActivity.this, getString(R.string.ue30), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                    });

                }else if (viewPager.getCurrentItem() == 1){
                   vmTimer.changeMotion();
                }
            }
        });


        //Bild vom floatigAction Button zwischen addFriends und addWecker wechseln
        onPageChangeListener = new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if(position == 2){
                    fab.setImageResource(R.drawable.ic_person_add_black_24dp);

                    //CustomView TextColor
                    buddies.setTextColor(getResources().getColor(android.R.color.white));
                }else if(position == 0){
                    fab.setImageResource(R.drawable.ic_add_alarm_black_24dp);

                    //CustomView TextColor
                    buddies.setTextColor(getResources().getColor(R.color.tab_text_color));
                }else{
                    fab.setImageResource(R.drawable.ic_timer_realyblack_24dp);

                    //CustomView TextColor
                    buddies.setTextColor(getResources().getColor(R.color.tab_text_color));

                    if(getIntent() != null && getIntent().hasExtra(AlarmClock.EXTRA_LENGTH) && !bereitsGestartet){
                        bereitsGestartet = true;
                        //Anzahl der Sekunden, die der Timer laufen soll
                        int seconds = getIntent().getIntExtra(AlarmClock.EXTRA_LENGTH, 600);
                        //Get current instance of Fragment
                        //Fragment timerFragment = swipeAdapter.getItem(viewPager.getCurrentItem());
                        Fragment timerFragment = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.view_pagerMain + ":" + viewPager.getCurrentItem());
                        Timer timerKOTLIN = (Timer) timerFragment;

                        //TODO Timer Assistent
                        //timerKOTLIN.assistentTime(seconds);
                    }
                }
            }

            @Override
            public void onPageSelected(int position) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        };


        //Guckt ob loged in oder nisch
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();

                if (user != null && justCreated) {
                    if(!mSharedPreferences.getBoolean("new", true)){
                        justCreated = false;
                        showUI(user);
                    }else{
                        mEditor.putBoolean("new", false);
                        mEditor.apply();

                        startActivity(new Intent(MainActivity.this, Onboarding.class));
                    }

                } else if(user == null) {
                    if(!mSharedPreferences.getBoolean("new", true)){
                        if(!triedLogIn){
                            triedLogIn = true;
                            //user is signed out
                            signIn();
                        }
                    }else{
                        mEditor.putBoolean("new", false);
                        mEditor.apply();

                        startActivity(new Intent(MainActivity.this, Onboarding.class));
                    }

                }
            }
        };
    }

    private void ringtoneIntent(int requestCode){
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, R.string.AlarmTon);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,RingtoneManager.TYPE_ALARM);
        this.startActivityForResult(intent, requestCode);
    }

    //Wenn der auser eingeloggt ist ui anzeigen
    public void showUI(FirebaseUser user){
        //user is signed in
        //SwipeAdapter an viewPager übergeben
        viewPager.setAdapter(swipeAdapter);
        tabLayout.setupWithViewPager(viewPager);

        //Anzeigen mit Punkt wenn neue Anfrage
        tabLayout.getTabAt(2).setCustomView(R.layout.buddies_tab);

        //TODO eigentlich infacher mit id = text1 bugg???
        viewTab = tabLayout.getTabAt(2).getCustomView();
        buddies = (TextView) viewTab.findViewById(R.id.text1);
        buddies.setText(R.string.FriendsFragment);


        //Get Action from NOTIFICATIONINTENT und SET_ALARM Intent
        Intent i = getIntent();
        if(i != null && i.getAction() != null){

            switch (i.getAction()){
                case "com.nicolai.CalmingAlarm_TARGET_NOTIFICATION": viewPager.setCurrentItem(2, true); break;
                case "show_timer": viewPager.setCurrentItem(1, true); break;
                case AlarmClock.ACTION_SET_TIMER:
                    viewPager.setCurrentItem(1, true);
                    break;
                //case AlarmClock.ACTION_DISMISS_TIMER: break;
                case AlarmClock.ACTION_SHOW_TIMERS: viewPager.setCurrentItem(1, true); break;

                case AlarmClock.ACTION_DISMISS_ALARM: break;
                case AlarmClock.ACTION_SHOW_ALARMS: break;
                default: break;
            }
        }

        //Wenn name und token durch absturz noch nicht in der database sind
        if(!mSharedPreferences.getBoolean(getString(R.string.sharedTokenSend), false)){
            takeTokenToDatabase();
        }

        /*Target target = new ViewTarget(toolbar.getId(), this);

        new ShowcaseView.Builder(this)
                .setTarget(target)
                .withMaterialShowcase()
                .setContentTitle("ShowcaseView")
                .setContentText("This is highlighting the Home button")
                .hideOnTouchOutside()
                .build();*/
    }

    //Weilen onRefreshToken in service vor login gestoppt wird
    private void takeTokenToDatabase(){
        FirebaseUser user = mFirebseAuth.getCurrentUser();
        final String userId = user.getUid();

        FirebaseDatabase mFirebaseDatabase = FirebaseDatabase.getInstance();
        //Update FCM Token alias registrationToken
        final DatabaseReference mAlarmsDatabaseReference = mFirebaseDatabase.getReference().child("users");

        //Wenn erstellt mit google account
        if(user.getDisplayName() != null && !user.getDisplayName().isEmpty()){
            mAlarmsDatabaseReference.child(userId).child("name").setValue(user.getDisplayName());
        }

        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener( new OnSuccessListener<InstanceIdResult>() {
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
                        }
                    }
                });
            }
        });
    }

    //Check for Invite deepLink
    public void checkForDeepLink(){
        //DynamicLink info
        FirebaseDynamicLinks.getInstance()
                .getDynamicLink(getIntent())
                .addOnCompleteListener(this, new OnCompleteListener<PendingDynamicLinkData>() {
                    @Override
                    public void onComplete(@NonNull Task<PendingDynamicLinkData> task) {
                        if (task.isSuccessful()) {
                            // Get deep link from result (may be null if no link is found)
                            Uri deepLink;

                            if (task.getResult() != null && task.getResult().getLink() != null) {
                                deepLink = task.getResult().getLink();

                                //FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                if (deepLink.getBooleanQueryParameter("invitedby", false)) {
                                    referrerUid = deepLink.getQueryParameter("invitedby");

                                    if(FirebaseAuth.getInstance().getCurrentUser() != null){
                                        DatabaseReference shared = FirebaseDatabase.getInstance().getReference().child("shared").child(referrerUid)
                                                .child(FirebaseAuth.getInstance().getUid());
                                        shared.setValue(Calendar.getInstance().getTimeInMillis());
                                        Toast.makeText(MainActivity.this, referrerUid, Toast.LENGTH_SHORT).show();

                                        Intent profileSetUp = new Intent(MainActivity.this, SettingsActivity.class);
                                        profileSetUp.setAction("SetUp");
                                        startActivity(profileSetUp);
                                    }else{
                                        Intent profileSetUp = new Intent(MainActivity.this, SettingsActivity.class);
                                        profileSetUp.setAction("SetUp");
                                        startActivity(profileSetUp);
                                    }
                                }else{
                                    Intent profileSetUp = new Intent(MainActivity.this, SettingsActivity.class);
                                    profileSetUp.setAction("SetUp");
                                    startActivity(profileSetUp);
                                }
                            }else{
                                Intent profileSetUp = new Intent(MainActivity.this, SettingsActivity.class);
                                profileSetUp.setAction("SetUp");
                                startActivity(profileSetUp);
                            }
                        } else {
                            //Toast.makeText(MainActivity.this, task.getException().toString(), Toast.LENGTH_SHORT).show();
                            Intent profileSetUp = new Intent(MainActivity.this, SettingsActivity.class);
                            profileSetUp.setAction("SetUp");
                            startActivity(profileSetUp);
                        }
                    }
                });
    }

    //Starts signIn Flow
    private void signIn(){
        // Choose authentication providers
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.GoogleBuilder().build(),
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.PhoneBuilder().build());

        // Create and launch sign-in intent
        startActivityForResult(AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setIsSmartLockEnabled(true)
                        .setAvailableProviders(providers)
                        //.setLogo(R.drawable.my_great_logo)
                        //.setTheme(R.style.MySuperAppTheme)
                        .build(),
                RC_SIGN_IN);
    }


    //Costum textview for the toolbar
    public void changeToolbarFont(Toolbar toolbar, Activity context) {
        for (int i = 0; i < toolbar.getChildCount(); i++) {
            View view = toolbar.getChildAt(i);
            if (view instanceof TextView) {
                TextView tv = (TextView) view;
                if (tv.getText().equals(toolbar.getTitle())) {
                    applyFont(tv);
                    break;
                }
            }
        }
    }

    public void applyFont(TextView tv) {
        try{
            Typeface typeface = ResourcesCompat.getFont(this, R.font.sonsie_one);
            tv.setTypeface(typeface);
            tv.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        }catch(Resources.NotFoundException e){
            tv.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_scrolling, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings://Settings
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                return true;

            case R.id.action_settings2://AllUsers
                Intent i3 = new Intent(this, Einstellungen.class);
                startActivity(i3);
                return true;

            case R.id.action_settings4://Invite new user
                Intent i4 = new Intent(this, Invite.class);
                startActivity(i4);
                return true;

            case R.id.action_settings7://Invite new user
                Intent i7 = new Intent(this, Guide.class);
                startActivity(i7);
                return true;

            case R.id.action_settings3://Ausloggen

                AuthUI.getInstance()
                        .signOut(this)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            public void onComplete(@NonNull Task<Void> task) {
                                triedLogIn = true;
                                //Has full version sharedPref löschen, damit z.B.pay fab button enabled wird
                                mEditor.putBoolean(getString(R.string.sharedFullVersion), false);
                                finish();
                            }
                        });

                return true;

            case R.id.action_settings5://Rechtliches zeug
                Intent i5 = new Intent(this, Legal.class);
                startActivity(i5);
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    protected void onResume(){
        super.onResume();
        //Loged in oder nisch
        mFirebseAuth.addAuthStateListener(mAuthStateListener);
        //Add PageChangelistener
        viewPager.addOnPageChangeListener(onPageChangeListener);
    }

    protected void onPause(){
        super.onPause();
        mFirebseAuth.removeAuthStateListener(mAuthStateListener);
        //Remove PageChangelistener
        viewPager.removeOnPageChangeListener(onPageChangeListener);
    }


    //Show friend requests in toolbar
    @Override
    public void setRequestCount(int count) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            if(viewTab == null) viewTab = tabLayout.getTabAt(2).getCustomView();
            TextView requestCount = viewTab.findViewById(R.id.requestCount);
            ImageView requestView = viewTab.findViewById(R.id.icon);

            if(count > 0){
                requestView.setVisibility(View.VISIBLE);
                requestView.setImageDrawable(getResources().getDrawable(R.drawable.ic_brightness_1_white_24dp));

                requestCount.setVisibility(View.VISIBLE);
                requestCount.setText(String.valueOf(count));
            }else{
                requestView.setVisibility(View.GONE);
                requestCount.setVisibility(View.GONE);
            }
        }
    }
}
//dataSource.deleteName("Elke");
//if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
//noWeckerName = Collections.singletonList("Noch kein Wecker vorhanden!");
//Toast.makeText(MainActivity.this, "FAILED", Toast.LENGTH_SHORT).show();

//if (ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {