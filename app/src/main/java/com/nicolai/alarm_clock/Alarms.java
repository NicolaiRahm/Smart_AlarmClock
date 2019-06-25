package com.nicolai.alarm_clock;

import android.Manifest;
import android.app.AlertDialog;

import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.app.JobIntentService;
import androidx.core.content.ContextCompat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nicolai.alarm_clock.adapter_helper.RecyclerItemTouchHelper;
import com.nicolai.alarm_clock.adapter_helper.RecyclerListAdapter;
import com.nicolai.alarm_clock.pojos.FB;
import com.nicolai.alarm_clock.pojos.WeckerPOJO;
import com.nicolai.alarm_clock.receiver_service.DeleteAllAlarms;
import com.nicolai.alarm_clock.room_database.WeckerDatabase;
import com.nicolai.alarm_clock.util.AlarmUtil;
import com.nicolai.alarm_clock.util.ContactUtil;
import com.nicolai.alarm_clock.util.DaysUtil;
import com.nicolai.alarm_clock.util.FullVersionUtil;
import com.nicolai.alarm_clock.util.TimeUtil;
import com.nicolai.alarm_clock.viewmodels.MainViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import tyrantgit.explosionfield.ExplosionField;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;

import com.nicolai.alarm_clock.databinding.FragmentAlarmsBinding;



/**
 * A simple {@link Fragment} subclass.
 */
public class Alarms extends Fragment implements RecyclerItemTouchHelper.RecyclerItemTouchHelperListener, PurchasesUpdatedListener, BillingClientStateListener {

    private FragmentAlarmsBinding mBinding;
    private MainViewModel viewModel;

    private RecyclerListAdapter mAdapter;
    private DatabaseReference detailsNew;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;

    private DatabaseReference forMe, mTrusted;
    private ChildEventListener childEventListener;

    private final int JOB_ID = 1001;

    private String weekDay;
    private int sharedTotal = 0, sharedNew = 0, sharedUpdated = 0, halfVolume, scCount;
    protected int newNextID = -1, newDistanceNextAlarm, explodeWecker, alarmsbyMeCount, countNextMessage, countAnnaeherung, distanceNextMessage;

    protected List<Integer> herausforedererDay = new ArrayList<Integer>(), herausfordererHour = new ArrayList<Integer>(), herausfordererMinute = new ArrayList<Integer>();
    protected List<String> userIds = new ArrayList<String>(), messageWeckerIds = new ArrayList<String>(), herausfordererDaysString = new ArrayList<String>();

    private static WaitForAsync2 waitForAsync;
    private TextView sharedFeed, countByMe, countForMe, countByMe1, countForMe1, nextMessageFeed;
    private MotionLayout ml;
    private boolean justCreated, purchased;
    private static NextAlarm nextAlarm;
    private List<WeckerPOJO> weckerList = new ArrayList<WeckerPOJO>();
    private BillingClient mBillingClient;
    private boolean mock;
    private MaterialShowcaseSequence sequence;

    public Alarms() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(LayoutInflater.from(getContext()), R.layout.fragment_alarms, null, false);

        //Is used for guide? --> mock == true
        if(getArguments() != null && getArguments().getString("mock") != null) {mock = true; showCase();}

        //Damit die Connection besteht bevor user was Klickt
        mBillingClient = BillingClient.newBuilder(getActivity()).setListener(Alarms.this).build();
        mBillingClient.startConnection(Alarms.this);

        mAdapter = new RecyclerListAdapter(new RecyclerListAdapter.EditAlarmClock() {
            @Override
            public void deleteAll(){
                if(!mock) dialogDeleteAll();
            }

            @Override
            public void itemClicked(int position) {
                if(!mock){
                    Intent editWecker = new Intent(getActivity(), TimeSeter.class);
                    editWecker.putExtra("alt", mAdapter.getID(position));
                    getActivity().startActivity(editWecker);
                }
            }

            @Override
            public void onOffButton(final int position) {
                if(!mock) {
                    WeckerPOJO alarmOnOff = viewModel.getById(mAdapter.getID(position));

                    if (!alarmOnOff.isOn_off()) {
                        //Damit list element gleich und animation flüssiger läuft
                        mAdapter.setOnOff(position, true);
                        AlarmUtil.setAlarm(getContext(), alarmOnOff);
                    } else {
                        AlarmUtil.unsetAlarm(getContext(), alarmOnOff, true);
                    }
                }else{
                    final TextView time = mBinding.nextTime;
                    if(time.getText() != null && !time.getText().toString().isEmpty()){
                        fillNextWecker(0, -5);
                    }else{
                        fillNextWecker(0, -1);
                    }
                }
            }

            @Override
            public void byMessageBuddyDeleted(final int position) {
                AppExecutor.getInstance().diskI0().execute(() -> {
                    int id = mAdapter.getID(position);
                    WeckerPOJO currentMessageWecker = viewModel.getById(id);

                    String newAppMessageJason = mAdapter.getAppMessageMap(id);
                    String smsJason = currentMessageWecker.getShared_sms_con();
                    String contactJason = currentMessageWecker.getShared_alarm_con();


                    //Wenn jetzt keine Message Kontakte mehr vorhanden sind sharedStatus endern
                    if((newAppMessageJason.equals("{}")) && (smsJason.equals("0") || smsJason.equals("{}"))){
                        //Message image view:GONE
                        if(contactJason.equals("0") || contactJason.equals("{}")){
                            currentMessageWecker.setShared('l');
                        }else{
                            currentMessageWecker.setShared('v');
                        }
                    }

                    currentMessageWecker.setShared_message_con(newAppMessageJason);
                    //geupdateten Wecker aus Adapter holen und in database
                    viewModel.update(currentMessageWecker);

                    //Delete "delete" folder from my messages
                    viewModel.messagesDeletedFld(id);
                });
            }

            @Override
            public void acceptSharedAction(final int position) {
                final WeckerPOJO currentWecker = viewModel.getById(mAdapter.getID(position));

                if (currentWecker.getShared() != 'd') {

                    currentWecker.setOn_off(true);
                    currentWecker.setShared('b');
                    viewModel.update(currentWecker);

                    String sender_id = currentWecker.getSender_id();
                    String sender_wecker_id = currentWecker.getSender_wecker_id();

                    //Alarm clock was accepted -> sender_to_myId_bestaetigt + timeStamp
                    viewModel.alarmApproved(sender_id, sender_wecker_id);

                    if(getActivity() != null && isAdded()){
                        getActivity().runOnUiThread(() -> {
                            //Update sharedFeed
                            switch (currentWecker.getShared()) {
                                case 's': updateSharedFeed(0, -1, 0); break;
                                case 'u': updateSharedFeed(0, 0, -1); break;
                            }
                        });
                    }
                } else {
                    currentWecker.setOn_off(true);
                    currentWecker.setShared('l');
                    currentWecker.setSender_id("");
                    currentWecker.setSender_wecker_id("");
                    currentWecker.setThumb_img("");
                    viewModel.update(currentWecker);
                }

                //Set alarm clock
                AlarmUtil.setAlarm(getContext(), currentWecker);
            }
        });

        mAdapter.submitList(weckerList);

        //Initialisierung Recyclerview
        RecyclerView mRecyclerView = mBinding.recyclerView;
        mRecyclerView.setNestedScrollingEnabled(false);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), RecyclerView.VERTICAL, false));
        mRecyclerView.setAdapter(mAdapter);

        //Damit SwipeDelete funkt
        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new RecyclerItemTouchHelper(0, ItemTouchHelper.LEFT, Alarms.this);
        ItemTouchHelper swipeHelper = new ItemTouchHelper(itemTouchHelperCallback);
        swipeHelper.attachToRecyclerView(mRecyclerView);

        //Wem vertraue ich?!
        mTrusted = FirebaseDatabase.getInstance().getReference().child("Trusted").child(FirebaseAuth.getInstance().getUid());

        //Initialisierung SharedPreferences plus Editor
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mEditor = mSharedPreferences.edit();

        //MotionLayout
        ml = mBinding.motionLayout;
        ml.setTransitionListener(new MotionLayout.TransitionListener() {
            @Override
            public void onTransitionStarted(MotionLayout motionLayout, int i, int i1) {}

            @Override
            public void onTransitionChange(MotionLayout motionLayout, int i, int i1, float v) {
                final TextView name = mBinding.nextName;
                final TextView shared = mBinding.nextShared;
                final float progress = motionLayout.getProgress();

                if(name.getVisibility() != View.GONE && progress != 0.0f) {
                    name.setVisibility(View.GONE);
                    shared.setVisibility(View.GONE);
                }

                if(sharedFeed.getVisibility() != View.GONE && (sharedNew > 0 || sharedUpdated > 0) && progress != 0.0f){
                    sharedFeed.setVisibility(View.GONE);
                }
            }

            @Override
            public void onTransitionCompleted(MotionLayout motionLayout, int i) {
                final TextView name = mBinding.nextName;
                final TextView shared = mBinding.nextShared;

                float progress = motionLayout.getProgress();
                if(name.getVisibility() != View.GONE && progress != 0.0f){
                    name.setVisibility(View.GONE);
                    shared.setVisibility(View.GONE);
                }else if(progress == 0.0f){
                    name.setVisibility(View.VISIBLE);
                    shared.setVisibility(View.VISIBLE);
                }

                //Vectorwecker nicht mehr clickable damit da gescrollt werden kann
                if(progress == 1.0f){
                    ImageView vectorWecker = mBinding.imageView8;
                    vectorWecker.setClickable(false);
                }else{
                    ImageView vectorWecker = mBinding.imageView8;
                    vectorWecker.setClickable(true);
                }

                //New/updated Wecker anzeige je nach progress visibility anpassen
                if((sharedNew > 0 || sharedUpdated > 0) && progress != 0.0f){
                    sharedFeed.setVisibility(View.GONE);
                }else{
                    if(sharedNew > 0 || sharedUpdated > 0){
                        sharedFeed.setText(getString(R.string.sharedFeed, sharedNew, sharedUpdated));
                        sharedFeed.setVisibility(View.VISIBLE);
                    }else if(progress == 0.0f){
                        mBinding.handle.setImageResource(R.drawable.ic_drag_handle_accent_24dp);
                    }
                }
            }

            @Override
            public void onTransitionTrigger(MotionLayout motionLayout, int i, boolean b, float v) {}
        });

        //Control Center Button
        ConstraintLayout toVerwalten = mBinding.cardView;
        toVerwalten.setOnClickListener(v -> {
            if(!mock) getActivity().startActivity(new Intent(getContext(), ControlCenter.class));
        });

        //CC preview
        countByMe1 = mBinding.aByYou;
        countForMe1 = mBinding.mForYou;

        countByMe = mBinding.aByYou1;
        countForMe = mBinding.mForYou1;
        nextMessageFeed = mBinding.nextMessageTrailer;

        //Weekday
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.GERMAN);
        weekDay = dayFormat.format(calendar.getTime());

        return mBinding.getRoot();
    }

    /*
     *Lifecycle methods
     */

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = ViewModelProviders.of(this).get(MainViewModel.class);
        //LiveData anheften
        populateTHIS();

        justCreated = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        //Damit der NotificationListenerService sich nicht mit sharedAlarm() doppelt
        mEditor.putBoolean("background", false);
        mEditor.apply();

        //Wenn nicht gerade erst created mit vorhandenen Sachen arbeiten
        if(!justCreated) {
            //Den Adapter neu laden wenn paused gewesen --> wecker ist empty / einfach zu laden wenn aus destroyed gewesen,
            //weshalb sich das upgrade mit dem populateThis nicht schlimm doppelt
            mAdapter.submitList(weckerList);
            //mAdapter.upgrade(weckerList);

            if(weckerList.size() == 2 && weckerList.get(1).getId() == -1 && weckerList.get(0).getId() == -9) {
                //Nur Zeit des nächsten Alarms ermitteln da bei Enderungen in populateThis() neu gesucht wird
                fillNextWecker(newNextID, -1);
            }else{
                //Nur Zeit des nächsten Alarms ermitteln da bei Enderungen in populateThis() neu gesucht wird
                fillNextWecker(newNextID, newDistanceNextAlarm);
            }
        }else{
            justCreated = false;
        }

        //CCfeed
        countByMe.setText(R.string.weckerVonDir);
        countByMe1.setText(getString(R.string.countCCFeed, alarmsbyMeCount));

        //Hidden function count
        explodeWecker = 0;

        //Daten zu Alarme und Messages FÜR mich
        sharedAlarmFeed();

        //Get new, updated and deleted Alarms
        sharedAlarm();
        //Next Message für CCfeed
        nextMessage();
    }

    @Override
    public void onPause() {
        super.onPause();

        if(nextAlarm != null){
            nextAlarm.cancel(true);
        }
        if(waitForAsync != null){
            waitForAsync.cancel(true);
        }

        //Listener für ForMe alarms lösen
        if(forMe != null && childEventListener != null){
            forMe.removeEventListener(childEventListener);
        }

        //Damit der NotificationListenerService sich nicht mit sharedAlarm() doppelt
        mEditor.putBoolean("background", true);
        mEditor.apply();

        //Wenn RcyclerView in Sichtfeld blurren
        //parentNsv.getViewTreeObserver().removeOnScrollChangedListener(onScrollChangeListener);
    }

    //If this fragment is currently used in guide
    private void showCase(){
        final TextView nextTime = mBinding.nextTime;
        ConstraintLayout ccFeed = mBinding.cardView;
        ImageView handle = mBinding.handle;

        MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(getActivity());

        MaterialShowcaseView.Builder roundBuilder = new MaterialShowcaseView.Builder(getActivity())
                .setTarget(nextTime)
                .setDelay(500)
                .setShapePadding(140)
                .setTitleText(R.string.scNextAlarm)
                .setContentText(R.string.scNextAlarmDesc)
                .setDismissText(R.string.gotIt);

        sequence.addSequenceItem(roundBuilder.build());

        MaterialShowcaseView.Builder rectangleBuilder = new MaterialShowcaseView.Builder(getActivity())
                .setTarget(ccFeed)
                .withRectangleShape()
                .setDelay(500)
                .setShapePadding(40)
                .setTitleText(R.string.scCC)
                .setContentText(R.string.scCCDesc)
                .setDismissText(R.string.gotIt);

        sequence.addSequenceItem(rectangleBuilder.build());

        MaterialShowcaseView.Builder handelerBuilder = new MaterialShowcaseView.Builder(getActivity())
                .setTarget(handle)
                .setDelay(500)
                .setTitleText(R.string.scList)
                .setContentText(R.string.scListDesc)
                .setDismissText(R.string.gotIt);

        sequence.addSequenceItem(handelerBuilder.build());

        //Listener
        sequence.setOnItemDismissedListener((materialShowcaseView, i) -> {
            if(scCount == 2){
                //ml.setProgress(1);
                ml.transitionToEnd();
            }else{
                scCount++;
            }
        });

        MaterialShowcaseView.Builder listBuilder = new MaterialShowcaseView.Builder(getActivity())
                .setTarget(ccFeed)
                .setDelay(800)
                .setShapePadding(50)
                .withRectangleShape()
                .setTitleText(R.string.scRecycler)
                .setContentText(R.string.scRecyclerDesc)
                .setDismissText(R.string.gotIt);

        sequence.addSequenceItem(listBuilder.build());

        sequence.start();
    }

    //Called after AsyncTask in background and STARTS sharedAlarms()
    private void populateTHIS(){
        //Da sich was geendert hat neuen Alarm ermitteln
        nextAlarm = new NextAlarm(this);

        //Set up ViewModel
        viewModel.getLiveWeckers().observe(this, weckerPOJOS -> {

            //LiveData updates umsetzen
            AppExecutor.getInstance().diskI0().execute( () -> {
                //Alle die nich für mich sind rausschmeißen
                Iterator<WeckerPOJO> i = weckerPOJOS.iterator();
                while (i.hasNext()) {
                    WeckerPOJO tescht = i.next();
                    if(!tescht.isFor_me()){
                        i.remove();
                    }
                    char s = tescht.getShared();
                    if(tescht.getShared() == 'v' || tescht.getShared() == 't'){
                        alarmsbyMeCount++;
                    }
                }

                //Damit in onRsume der Adapter upgedated werden kann
                weckerList = weckerPOJOS;

                if(isAdded() && getActivity() != null){
                    getActivity().runOnUiThread(() -> {
                        if(weckerPOJOS.size() == 0){
                            //Wenn showcase
                            if(mock){
                                WeckerPOJO testWecker = new WeckerPOJO(0, getString(R.string.dgMockAlarmName), "xxxxxxxxfrxxxx", true,
                                        7, 30,
                                        3, 300000, 30, 1, "",
                                        true, 'l', "{}", "{}", "{}", true, "",
                                        "", "", "", true,
                                        "", "", 30);
                                weckerPOJOS.add(testWecker);
                                weckerPOJOS.add(new WeckerPOJO(-1, getString(R.string.NoWecker)));
                                fillNextWecker(newNextID, -1);
                            }else{
                                fillNextWecker(newNextID, -1);
                                //Hier wär ne liste mit deinen Weckern
                                weckerPOJOS.add(new WeckerPOJO(-9, "Mock alarm"));
                                weckerPOJOS.add(new WeckerPOJO(-1, getString(R.string.NoWecker)));
                            }
                        }else {
                            //Da sich was geendert hat neuen Alarm ermitteln
                            nextAlarm = new NextAlarm(Alarms.this);
                            nextAlarm.execute();

                            //Delete all als letztes Element
                            weckerPOJOS.add(new WeckerPOJO(-2, getString(R.string.deleteAll, weckerPOJOS.size())));
                        }

                        //Den Adapter upgraden
                        mAdapter.submitList(weckerPOJOS);

                        //CCfeed
                        countByMe.setText(R.string.weckerVonDir);
                        countByMe1.setText(getString(R.string.countCCFeed, alarmsbyMeCount));
                    });
                }
            });
        });
    }

    private void sharedAlarm(){
        AppExecutor.getInstance().diskI0().execute(() -> {
                forMe = FirebaseDatabase.getInstance().getReference().child(FB.ALARMS).child(FirebaseAuth.getInstance().getUid()).child(FB.FOR_ME);

                childEventListener = new ChildEventListener() {

                    @Override
                    public void onChildAdded(final DataSnapshot dataSnapshot1, String s) {
                        updateORinsertSharedAlarm(dataSnapshot1);
                    }

                    @Override
                    public void onChildChanged(final DataSnapshot dataSnapshot1, String s) {
                        updateORinsertSharedAlarm(dataSnapshot1);
                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {}

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {}

                    @Override
                    public void onCancelled(DatabaseError databaseError) {}
                };

                forMe.addChildEventListener(childEventListener);
        });
    }

    private void updateORinsertSharedAlarm(DataSnapshot dataSnapshot1){
        if (dataSnapshot1.child(FB.MY_WECKER_ID).exists()) {

            final String myWecker_id = dataSnapshot1.child(FB.MY_WECKER_ID).getValue().toString();
            final String status = dataSnapshot1.child(FB.STATUS).getValue().toString();
            final String senderID = dataSnapshot1.child(FB.SENDER_ID).getValue().toString();

            mTrusted.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    char shared = dataSnapshot.hasChild(senderID) ? 'b' : 's';

                    switch (status){
                        case FB.New: insertSharedAlarm(dataSnapshot1, shared); break;
                        case FB.updated:
                            Toast.makeText(getActivity(), "Updated Alarm", Toast.LENGTH_SHORT).show();

                            if (!myWecker_id.isEmpty()) {
                                if(shared != 'b') shared = 'u';

                                //update shared alarm in room with new settings by the sender
                                AlarmUtil.updateSharedAlarm(getContext(), dataSnapshot, shared);
                                if(shared != 'b') updateSharedFeed(0, 0, 1);
                                break;
                            }

                            insertSharedAlarm(dataSnapshot1, shared);
                            break;
                    }

                    if (status.equals(FB.deleted) || status.equals(FB.UASContactDeleted)) {
                        if(!myWecker_id.isEmpty()){
                            WeckerPOJO alarm = viewModel.getById(Integer.parseInt(myWecker_id));
                            alarm.setShared('d');
                            viewModel.update(alarm);

                            //Alarm aussetzen bis neue details bestätigt sind
                            AlarmUtil.unsetAlarm(getContext(), viewModel.getById(Integer.parseInt(myWecker_id)), true);
                        }

                        //Remove auch für mich
                        forMe.child(dataSnapshot1.getKey()).removeValue();

                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

        }
    }

    //Inserte new shared alarms and add information of the sender
    private void insertSharedAlarm(DataSnapshot dataSnapshot, char shared){
        Toast.makeText(getActivity(), getString(R.string.newAlarm), Toast.LENGTH_SHORT).show();

        AlarmUtil.insertSharedAlarm(getContext(), dataSnapshot, shared);

        //Set the alarm automatically if the sender is trusted
        if(shared == 'b'){
            updateSharedFeed(1, 0, 0);
        }else{
            updateSharedFeed(1, 1, 0);
        }
    }

    private void fillNextWecker(final int newNextId, final int distance){
        final TextView name = mBinding.nextName;
        final TextView shared = mBinding.nextShared;
        final TextView time = mBinding.nextTime;
        final TextView timeLeft = mBinding.nextTimeLeft;
        final TextView intervall = mBinding.nextIntervall;

        //If showcase
        if(mock){
            //OnOff button clicked --> off
            if(distance == -5){
                noNextAlarmUI();
            }else{
                final String nextName = getString(R.string.dgMockAlarmName);
                int hour = 7;
                int minute = 30;
                final int anzahl = 3;
                final long nextIntervall = 5;
                final String hourStr = "7";
                final String minuteStr = "30";
                final String fullShared = getString(R.string.onlyMe);

                int minutesLeft = AlarmUtil.minutesLeft(hour, minute);
                final int houresLeft = minutesLeft / 60;
                final int minutesLeftover = minutesLeft - houresLeft * 60;

                name.setText(getString(R.string.nextAlarm, nextName));
                shared.setText(fullShared);
                time.setText(getString(R.string.nextTime, hourStr, minuteStr));
                timeLeft.setText(getString(R.string.klingeltIn, houresLeft, minutesLeftover));
                intervall.setText(getString(R.string.nextIntervall, anzahl, nextIntervall));

                //Animate alarm clock vector graphic
                alarmVectorG_Animation();
            }

        }else if(distance != -1){
            //Sharedalarmdaten
            AppExecutor.getInstance().diskI0().execute(() -> {
                WeckerPOJO nextWecker = viewModel.getById(newNextId);

                //Falls irgendwie von TurnOff kommt
                if(nextWecker != null){
                    final String nextName = nextWecker.getName();
                    char nextShared = nextWecker.getShared();
                    int hour = nextWecker.getHour();
                    int minute = nextWecker.getMinute();
                    final int anzahl = nextWecker.getAnzahl();
                    final long nextIntervall = nextWecker.getIntervall() / 60000;

                    //Daten upsizen
                    final String hourStr = TimeUtil.under10(hour);
                    final String minuteStr = TimeUtil.under10(minute);

                    final String fullShared;
                    switch (nextShared){
                        case 'l': fullShared = getString(R.string.onlyMe); break;
                        case 'b':
                            String from = nextWecker.getVon_xy();
                            fullShared = getString(R.string.weckerVon) + from;
                            break;
                        case 'v': fullShared = getString(R.string.vonMir); break;
                        case 't': fullShared = getString(R.string.vonMir); break;
                        default: fullShared = getString(R.string.mitNachricht); break;
                    }

                    //Berechnung der verbleibenden Zeit
                    int hourDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                    int minuteDay = Calendar.getInstance().get(Calendar.MINUTE);

                    int minutesLeft = distance * 24 * 60 - (hourDay - hour) * 60 - (minuteDay - minute);

                    final int houresLeft = minutesLeft / 60;
                    final int minutesLeftover = minutesLeft - houresLeft * 60;

                    if(isAdded() && getActivity() != null){
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //Daten einsetzen
                                name.setText(getString(R.string.nextAlarm, nextName));
                                shared.setText(fullShared);
                                time.setText(getString(R.string.nextTime, hourStr, minuteStr));
                                timeLeft.setText(getString(R.string.klingeltIn, houresLeft, minutesLeftover));
                                if(anzahl == 1){
                                    intervall.setText(R.string.AlarmIntervallNo);
                                }else {
                                    intervall.setText(getString(R.string.nextIntervall, anzahl, nextIntervall));
                                }

                                //Animate alarm clock vector graphic
                                alarmVectorG_Animation();
                            }
                        });
                    }
                }else{
                    if(isAdded() && getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                noNextAlarmUI();
                            }
                        });
                    }
                }
            });
        }else{
            noNextAlarmUI();
        }
    }

    private void noNextAlarmUI(){
        final TextView name = mBinding.nextName;
        final TextView shared = mBinding.nextShared;
        final TextView time = mBinding.nextTime;
        final TextView timeLeft = mBinding.nextTimeLeft;
        final TextView intervall = mBinding.nextIntervall;
        final ImageView imageView = mBinding.imageView8;

        //name.setText(getString(R.string.keiner_gestellt));
        name.setText("");
        shared.setText("");
        time.setText("");
        timeLeft.setText("");
        intervall.setText("");

        imageView.setImageResource(R.drawable.ic_alarm_off_white_24dp);
    }

    private void alarmVectorG_Animation(){
        final ImageView imageView = mBinding.imageView8;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            final AnimatedVectorDrawable animation = (AnimatedVectorDrawable) getActivity().getDrawable(R.drawable.ic_alarm_white_24px);
            imageView.setImageDrawable(animation);
            animation.start();

            imageView.setOnClickListener(viewObject -> {
                animation.start();

                //Hidden function explodierender Wecker
                if(explodeWecker == 3){
                    ExplosionField explosionField = ExplosionField.attach2Window(getActivity());
                    explosionField.expandExplosionBound(80, 220);
                    explosionField.explode(imageView);

                    //TODO vlt. witziger ton

                    explodeWecker = 0;
                }else {
                    explodeWecker++;
                }
            });
        }else{
            imageView.setImageResource(R.drawable.ic_alarm_white_24px);
        }
    }




    //Wenn neuer oder upgedateter Alarm für mich als handle anzeigen
    private void sharedAlarmFeed(){
        AppExecutor.getInstance().diskI0().execute(() -> {
            List<WeckerPOJO> sharedList = viewModel.getSharedList();

            if(sharedList != null){
                int size = sharedList.size();

                sharedTotal = 0;
                sharedNew = 0;
                sharedUpdated = 0;

                for(int i = 0; i < size; i++){
                    switch (sharedList.get(i).getShared()){
                        case 'b': sharedTotal++; break;
                        case 's': sharedNew++; sharedTotal++; break;
                        case 'u': sharedUpdated++; sharedTotal++; break;
                    }
                }
            }


            if(isAdded() && getActivity() != null){
                getActivity().runOnUiThread(() -> {
                    sharedFeed = mBinding.newSharedAlarm;
                    ImageView handle = mBinding.handle;

                    if(sharedNew > 0 || sharedUpdated > 0){
                        handle.setImageResource(R.drawable.ic_share_realy_accent_24dp);
                        sharedFeed.setVisibility(View.VISIBLE);
                        sharedFeed.setText(getString(R.string.sharedFeed, sharedNew, sharedUpdated));
                        //totalSharedAlarms.setText("" + sharedTotal);
                    }else{
                        handle.setImageResource(R.drawable.ic_drag_handle_accent_24dp);
                        sharedFeed.setVisibility(View.GONE);
                    }
                });
            }
        });
    }

    private void updateSharedFeed(int total, int newS, int updated){
        sharedTotal += total;
        sharedNew += newS;
        sharedUpdated += updated;

        ImageView handle = mBinding.handle;
        //Damit die bei ml progress 1.0 gone textView nicht visible gemacht wird und danach ml koplett hakt
        //if(ml.getProgress() == 0.0f){
            if(sharedNew > 0 || sharedUpdated > 0){
                handle.setImageResource(R.drawable.ic_share_realy_accent_24dp);
                sharedFeed.setVisibility(View.VISIBLE);
                sharedFeed.setText(getString(R.string.sharedFeed, sharedNew, sharedUpdated));
                //totalSharedAlarms.setText("" + sharedTotal);
            }else{
                handle.setImageResource(R.drawable.ic_drag_handle_accent_24dp);
                sharedFeed.setVisibility(View.GONE);
            }
        //}
    }

    private static class NextAlarm extends AsyncTask<Void, Void, Integer[]>{
        private Alarms parentFragment;
        protected Integer [] result = new Integer[] {1, -1, -1};

        public NextAlarm(Alarms alarms){
            parentFragment = alarms;
        }

        @Override
        protected Integer[] doInBackground(Void... voids) {
            //Aktueller Tag: Montag...
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.GERMAN);
            String weekDay = dayFormat.format(calendar.getTime());
            int hourDay = calendar.get(Calendar.HOUR_OF_DAY);
            int minuteDay = calendar.get(Calendar.MINUTE);

            //DatabaseInstance
            WeckerDatabase database = WeckerDatabase.getInstance(parentFragment.getActivity().getApplicationContext());

            long newNextID;

            int weekDayInt = DaysUtil.intForDay(weekDay);

            //Nächster Tag und Uhrzeit des Alarms + is on?
            List<Long> idsList = database.weckerDao().idList();

            //Wenn überhaupt Alarme vorhanden sind
            if(idsList != null && idsList.size() > 0){


                List<String> tageList = database.weckerDao().tageList();
                List<Integer> houresList = database.weckerDao().hourList();
                List<Integer> minutesList = database.weckerDao().minuteList();
                List<Boolean> onOffList = database.weckerDao().onOffList();

                //Liste mit ids aller zu beachtender Wecker: On (und tage nicht alle x, wobei das ja das selbe ist)
                List<Integer> countableList = new ArrayList<>();

                //Erster Wecker der Tage hat
                for (int i = 0; i < idsList.size(); i++){
                    if(onOffList.get(i)){
                        countableList.add(i);
                    }
                }

                if(countableList.size() > 0){
                    //Ersten Wecker als Start nehmen und gucken, ob einer eher schellt
                    //TODO nimmt an das der erste getellt ist onOff = 'l'
                    newNextID = idsList.get(countableList.get(0));
                    int newNextsDayInt = DaysUtil.nextDay(weekDay, tageList.get(countableList.get(0)));
                    int newDistance = 0;

                    //Distance des erst besten Alarms
                    int weekDayIntCopy = weekDayInt;

                    //Wenn heute in einer Woche
                    //Auch gucken ob sonst danach noch n Tag markiert ist
                    if(weekDayInt == newNextsDayInt && ((houresList.get(countableList.get(0)) < hourDay) || (houresList.get(countableList.get(0)) == hourDay && minutesList.get(countableList.get(0)) <= minuteDay))){
                        String days = tageList.get(countableList.get(0));
                        int xCount = 0;
                        for(int i = 0; i < 14; i++){

                            if (days.charAt(i) == 'x'){
                                xCount ++;
                            }
                        }

                        if(xCount == 12){
                            newDistance = 7;
                        }else{
                            String withoutToday = tageList.get(countableList.get(0));
                            StringBuilder cut = new StringBuilder(withoutToday);
                            cut.setCharAt(newNextsDayInt*2, 'x');
                            cut.setCharAt(newNextsDayInt*2+1, 'x');
                            withoutToday = cut.toString();

                            newNextsDayInt = DaysUtil.nextDay(weekDay, withoutToday);

                            //Symulate the circle with 7 steps 0-12
                            while(weekDayIntCopy != newNextsDayInt){
                                weekDayIntCopy = weekDayIntCopy + 1;
                                newDistance++;

                                if(weekDayIntCopy > 6){
                                    weekDayIntCopy = 0;
                                }

                                //Wenn FragmentLifecycle dem AsyncTask in die quere kommt
                                if (isCancelled()) break;
                            }
                        }
                    }else{
                        //Symulate the circle with 7 steps 0-12
                        while(weekDayIntCopy != newNextsDayInt){
                            weekDayIntCopy = weekDayIntCopy + 1;
                            newDistance++;

                            if(weekDayIntCopy > 6){
                                weekDayIntCopy = 0;
                            }

                            //Wenn FragmentLifecycle dem AsyncTask in die quere kommt
                            if (isCancelled()) break;
                        }
                    }

                    //Alle Alarme testen ob früher und wenn ja ob gestellt
                    for(int j = 1; j < countableList.size(); j++) {

                        int position_of_wecker_id = countableList.get(j);

                        int herausfordererNextDay = DaysUtil.nextDay(weekDay, tageList.get(position_of_wecker_id));

                        int distance = 0;
                        weekDayIntCopy = weekDayInt;

                        //Wenn heute in einer Woche
                        if(weekDayInt == herausfordererNextDay && (houresList.get(position_of_wecker_id) < hourDay || (houresList.get(position_of_wecker_id) == hourDay && minutesList.get(position_of_wecker_id) <= minuteDay))){
                            //Gibts noch n anderen Tag?!
                            String days = tageList.get(position_of_wecker_id);
                            int xCount = 0;
                            for(int i = 0; i < 14; i++){

                                if (days.charAt(i) == 'x'){
                                    xCount ++;
                                }
                            }

                            if(xCount == 12){
                                distance = 7;
                            }else{
                                //Symulate the circle with 7 steps 0-12
                                String withoutToday = tageList.get(position_of_wecker_id);
                                StringBuilder cut = new StringBuilder(withoutToday);
                                cut.setCharAt(herausfordererNextDay*2, 'x');
                                cut.setCharAt(herausfordererNextDay*2+1, 'x');
                                withoutToday = cut.toString();
                                herausfordererNextDay = DaysUtil.nextDay(weekDay, withoutToday);

                                while (weekDayIntCopy != herausfordererNextDay) {
                                    weekDayIntCopy++;
                                    distance++;

                                    if (weekDayIntCopy > 6) {
                                        weekDayIntCopy = 0;
                                    }

                                    //Wenn FragmentLifecycle dem AsyncTask in die quere kommt
                                    if (isCancelled()) break;
                                }
                            }
                        }else {
                            //Symulate the circle with 7 steps 0-12
                            while (weekDayIntCopy != herausfordererNextDay) {
                                weekDayIntCopy++;
                                distance++;

                                if (weekDayIntCopy > 6) {
                                    weekDayIntCopy = 0;
                                }

                                //Wenn FragmentLifecycle dem AsyncTask in die quere kommt
                                if (isCancelled()) break;
                            }
                        }

                        //Wenn die Entfernung zum aktuellen Tag kürzer als die letzte ist nearest erneuern
                        if(newDistance > distance){
                            newNextID = idsList.get(position_of_wecker_id);
                            newDistance = distance;
                        }else if (newDistance == distance){
                            int houre = houresList.get(idsList.indexOf(newNextID));
                            int minute = minutesList.get(idsList.indexOf(newNextID));

                            //Uhrzeitenvergleich
                            if (houre > houresList.get(position_of_wecker_id)){
                                newNextID = idsList.get(position_of_wecker_id);
                                newDistance = distance;
                            }else if (houre == houresList.get(position_of_wecker_id) && minute > minutesList.get(position_of_wecker_id)){
                                newNextID = idsList.get(position_of_wecker_id);
                                newDistance = distance;
                            }

                        }else{
                            //newDistance = 8;
                        }
                        //Wenn FragmentLifecycle dem AsyncTask in die quere kommt
                        if (isCancelled()) break;
                    }


                    result [0] = 2;
                    result [1] = (int) newNextID;
                    result [2] = newDistance;
                    return result;
                }else{
                    //Wenn es keinen gültigen Wecker gab
                    return result;
                }

                //Kein Alarm vorhanden
            }else{
                return result;
            }
        }

        @Override
        protected void onPostExecute(Integer[] nextAlarmCase) {
            super.onPostExecute(nextAlarmCase);

            parentFragment.newNextID = nextAlarmCase[1];
            parentFragment.newDistanceNextAlarm = nextAlarmCase[2];

            switch (nextAlarmCase[0]){
                case 1: parentFragment.fillNextWecker(-1, -1); break;
                case 2: parentFragment.fillNextWecker(nextAlarmCase[1], nextAlarmCase[2]); break;
            }
        }
    }


    //CCfeed Message details----------------------------------------

    //Snapshot plus count for wFFirebaseMessages
    private void nextMessage(){
        String mCurrent_user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference mMessagesForMeDatabase = FirebaseDatabase.getInstance().getReference().child("messages").child(mCurrent_user_id).child("ForMe");

        mMessagesForMeDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    //Anzahl der Messages ForMe
                    countNextMessage = (int) dataSnapshot.getChildrenCount();
                    countForMe.setText(R.string.messageForYou);
                    countForMe1.setText(getString(R.string.countCCFeed, countNextMessage));

                    //Alle Messagedaten aus Firebase laden
                    wFFirebaseMessages(dataSnapshot);

                    //datasnapshot alias ForMe existiert nicht
                }else {
                    countForMe.setText(R.string.messageForYou);
                    countForMe1.setText(getString(R.string.countCCFeed, 0));
                    nextMessageFeed.setText(R.string.keineSharedMessage);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                countForMe.setText(R.string.messageForYou);
                countForMe1.setText(getString(R.string.countCCFeed, 0));
                nextMessageFeed.setText(R.string.keineSharedMessage);
            }
        });
        /*}else{TODO Datasparen
            totalSharedMessage.setText("" + countNextMessage);

            Calendar calendar = Calendar.getInstance();
            int hourDay = calendar.get(Calendar.HOUR_OF_DAY);
            int minuteDay = calendar.get(Calendar.MINUTE);
            DateFormat df = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT);
            //Aktuelle Stunde und Minute updaten
            hourDay = calendar.get(Calendar.HOUR_OF_DAY);
            minuteDay = calendar.get(Calendar.MINUTE);
            //Jetzt Zeit plus differenz zum Fälligkeitszeitpunkt der nächsten SharedMesage
            long differnce = newDistance * 24 * 3600000 - (hourDay - hourNextMessage) * 3600000 - (minuteDay - minuteNextMessage) * 60000;
            String timeString = df.format(calendar.getTimeInMillis() + differnce);

            calendar.setTimeInMillis(calendar.getTimeInMillis() + differnce);
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
            String dateString = dayFormat.format(calendar.getTime());

            sharedFeedMessage.setText(getString(R.string.nextMessage, dateString, timeString));
        }*/
    }

    //Holt alle stunden, minuten ... zu allen Messages befor WaitForAsync gestartet wird
    private void wFFirebaseMessages(final DataSnapshot nextMessagesSnap) {

        AppExecutor.getInstance().diskI0().execute(new Runnable() {
            @Override
            public void run() {
                DatabaseReference mActualMessages = FirebaseDatabase.getInstance().getReference().child("messages");

                for (DataSnapshot messageForMe : nextMessagesSnap.getChildren()) {
                    String fromUserId = messageForMe.child("sender_id").getValue().toString();
                    String weckerID = messageForMe.child("wecker_id").getValue().toString();

                    userIds.add(fromUserId);
                    messageWeckerIds.add(weckerID);

                    //Get Messages Data
                    mActualMessages.child(fromUserId).child("ByMe").child(weckerID).child("details").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(final DataSnapshot dataSnapshot) {
                            countAnnaeherung++;

                            //Wenn der nicht nach WakeUp gelöscht
                            if(dataSnapshot.exists() && !dataSnapshot.child("deleted").exists()){
                                //Sharedalarmdaten
                                String herausfordererDays = dataSnapshot.child("days").getValue().toString();
                                int herausfordererNextDay = DaysUtil.nextDay(weekDay, herausfordererDays);

                                if (herausfordererNextDay != 8){
                                    int herausFordererHour = Integer.parseInt(dataSnapshot.child("hour").getValue().toString());
                                    int herausFordererMinute = Integer.parseInt(dataSnapshot.child("minute").getValue().toString());
                                    //Add to Lists
                                    herausfordererDaysString.add(herausfordererDays);
                                    herausforedererDay.add(herausfordererNextDay);
                                    herausfordererHour.add(herausFordererHour);
                                    herausfordererMinute.add(herausFordererMinute);

                                    if (countAnnaeherung == countNextMessage) {
                                        countAnnaeherung = 0;
                                        //execute calculation in AsyncTask
                                        //Initialisieren / erneuern der AsyncTask plus starten von nextAlarm
                                        if(isAdded() && getActivity() != null) {
                                            getActivity().runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    waitForAsync = new Alarms.WaitForAsync2(Alarms.this);
                                                    waitForAsync.execute();
                                                }
                                            });
                                        }
                                    }
                                }else if(countAnnaeherung == countNextMessage && !herausfordererHour.isEmpty()){
                                    countAnnaeherung = 0;

                                    if(isAdded() && getActivity() != null) {
                                        getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                waitForAsync = new Alarms.WaitForAsync2(Alarms.this);
                                                waitForAsync.execute();
                                            }
                                        });
                                    }
                                }else if(countAnnaeherung == countNextMessage){
                                    countAnnaeherung = 0;
                                    nextMessageFeed.setText(R.string.keineSharedMessage);
                                }

                            }else if(countAnnaeherung == countNextMessage && !herausfordererHour.isEmpty()){
                                countAnnaeherung = 0;
                                if(isAdded() && getActivity() != null) {
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            waitForAsync = new Alarms.WaitForAsync2(Alarms.this);
                                            waitForAsync.execute();
                                        }
                                    });
                                }
                            }else if(countAnnaeherung == countNextMessage){
                                countAnnaeherung = 0;
                                nextMessageFeed.setText(R.string.keineSharedMessage);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            countAnnaeherung = 0;
                            nextMessageFeed.setText(getString(R.string.noSharedMessageConnection));
                        }
                    });
                }
            }
        });
    }

    //Ermitteln der nächsten Nachricht
    private static class WaitForAsync2 extends AsyncTask<Void, Void, Boolean> {
        private Alarms parentActivity;
        private int hourDay, minuteDay, newDistance = -1, hourNextMessage, minuteNextMessage, weekDayInt, zahlFueri;

        public WaitForAsync2(Alarms alarms){
            parentActivity = alarms;

            //Next Wecker
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.GERMAN);
            final String weekDay = dayFormat.format(calendar.getTime());
            weekDayInt = DaysUtil.intForDay(weekDay);
            hourDay = calendar.get(Calendar.HOUR_OF_DAY);
            minuteDay = calendar.get(Calendar.MINUTE);//calendar.get(calendar.get(Calendar.MINUTE));
        }

        @Override
        protected Boolean doInBackground(Void... v) {
            //countAnnaeherung -> nummer der messages von so und so vielen (countNextMessage), die gerade betrachtet werden soll / bis zu der die for Schleife laufen soll

            for(int i = 0; i < parentActivity.herausfordererHour.size(); i++){
                int herausfordererNextDay = parentActivity.herausforedererDay.get(i);
                int herausFordererHour = parentActivity.herausfordererHour.get(i);
                int herausFordererMinute = parentActivity.herausfordererMinute.get(i);;

                int distance = 0;
                int weekDayIntCopy = weekDayInt;

                //Wenn heute in einer Woche
                if(weekDayInt == herausfordererNextDay && (herausFordererHour < hourDay || (herausFordererHour == hourDay && herausFordererMinute <= minuteDay))){


                    //Gibts noch n anderen Tag?!
                    String days = parentActivity.herausfordererDaysString.get(i);
                    int xCount = 0;
                    for(int j = 0; j < 14; j++){

                        if (days.charAt(i) == 'x'){
                            xCount ++;
                        }
                    }

                    if(xCount == 12){
                        distance = 7;
                    }else{
                        //Symulate the circle with 7 steps 0-12
                        String withoutToday = days;
                        StringBuilder cut = new StringBuilder(withoutToday);
                        cut.setCharAt(herausfordererNextDay*2, 'x');
                        cut.setCharAt(herausfordererNextDay*2+1, 'x');
                        withoutToday = cut.toString();
                        herausfordererNextDay = DaysUtil.nextDay(parentActivity.weekDay, withoutToday);

                        if(herausfordererNextDay != 20){
                            while (weekDayIntCopy != herausfordererNextDay) {
                                weekDayIntCopy++;
                                distance++;

                                if (weekDayIntCopy > 6) {
                                    weekDayIntCopy = 0;
                                }
                            }
                        }
                    }
                }else{
                    //Symulate the circle with 7 steps 0-12
                    if(herausfordererNextDay != 20){
                        while (weekDayIntCopy != herausfordererNextDay) {
                            weekDayIntCopy++;
                            distance++;

                            if (weekDayIntCopy > 6) {
                                weekDayIntCopy = 0;
                            }
                        }
                    }
                }


                if(newDistance == -1){
                    hourNextMessage = herausFordererHour;
                    minuteNextMessage = herausFordererMinute;
                    newDistance = distance;
                    zahlFueri = i;
                }else{
                    //Wenn die Entfernung zum aktuellen Tag kürzer als die letzte ist nearest erneuern
                    if(newDistance > distance){
                        hourNextMessage = herausFordererHour;
                        minuteNextMessage = herausFordererMinute;
                        newDistance = distance;
                        zahlFueri = i;
                    }else if (newDistance == distance){

                        //Uhrzeitenvergleich
                        if (hourNextMessage > herausFordererHour){
                            hourNextMessage = herausFordererHour;
                            minuteNextMessage = herausFordererMinute;
                            newDistance = distance;
                            zahlFueri = i;
                        }else if (hourNextMessage == herausFordererHour && minuteNextMessage > herausFordererMinute){
                            hourNextMessage = herausFordererHour;
                            minuteNextMessage = herausFordererMinute;
                            newDistance = distance;
                            zahlFueri = i;
                        }
                    }
                }

                //Wenn FragmentLifecycle dem AsyncTask in die quere kommt
                if (isCancelled()) break;
            }

            return newDistance != -1;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if(result){
                parentActivity.distanceNextMessage = newDistance;
                parentActivity.nextMessageUser(zahlFueri);
            }else{
                parentActivity.nextMessageFeed.setText(R.string.noSharedMessageConnection);
            }
        }
    }

    private void nextMessageUser(int zahlFueri){
        final String byUserId = userIds.get(zahlFueri);
        final String foreignWeckerId = messageWeckerIds.get(zahlFueri);

        //Get Username
        DatabaseReference userDatabase = FirebaseDatabase.getInstance().getReference().child("users");
        userDatabase.child(byUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    String userName = dataSnapshot.child("name").getValue().toString();
                    final String number = dataSnapshot.child("mobile_number").getValue().toString();

                    if(!number.isEmpty() && ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED){
                        String contactName = ContactUtil.getContactDisplayNameByNumber(getContext(), number);
                        if(!contactName.equals("?")){
                            userName = contactName;
                        }
                    }

                    //View mit details füllen
                    fillNextMessageTrailer(byUserId, foreignWeckerId, number, userName);
                }else{
                    nextMessageFeed.setText(R.string.keineSharedMessage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                nextMessageFeed.setText(R.string.keineSharedMessage);
            }
        });
    }

    private void fillNextMessageTrailer(String byUserId, String foreignWeckerId, String number, final String username){
        //Get Username
        DatabaseReference detailsWecker = FirebaseDatabase.getInstance().getReference().child("messages").
                child(byUserId).child("ByMe").child(foreignWeckerId).child("details");
        detailsWecker.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    String hour = dataSnapshot.child("hour").getValue().toString();
                    String minute = dataSnapshot.child("minute").getValue().toString();
                    String status = dataSnapshot.child("status").getValue().toString();

                    String time = hour + ":" + minute;
                    if(Integer.parseInt(minute) < 10){
                        time = hour + ":0" + minute;
                    }

                    //Text übersetzen
                    switch (status){
                        case "not used yet": status = getString(R.string.statusM_not_used_yet); break;

                        case "online": status = getString(R.string.status_online); break;
                        case "verschlafen": status = getString(R.string.status_verschlafen); break;
                        case "aufgestanden": status = getString(R.string.status_aufgestanden); break;
                        case "schlummert": status = getString(R.string.status_schlummert); break;

                        default: status = ""; break;
                    }

                    nextMessageFeed.setText(getString(R.string.nextMessageFeed, username, time, status));
                }else {
                    nextMessageFeed.setText(R.string.keineSharedMessage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                nextMessageFeed.setText(R.string.keineSharedMessage);
            }
        });
    }


    //##################### Runtime Methods

    @Override
    public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction, final int position) {
        if (viewHolder instanceof RecyclerListAdapter.WeckerHolder) {

            if(!mock){
                //Wenn das letzte / mock alarm wird das schon in RecyclerItemTouchHelper gestoppt

                //Aus database löschen
                AppExecutor.getInstance().diskI0().execute(() -> {
                    //Weckerdaten
                    final int id = mAdapter.getID(position);
                    WeckerPOJO alarm = viewModel.getById(id);

                    final char shared = alarm.getShared();
                    String sender_id = alarm.getSender_id();
                    String sender_wecker_id = alarm.getSender_wecker_id();

                    //Alertdiaolg
                    if(shared == 't' || shared == 'v' || shared == 'm'){
                        if(isAdded() && getActivity() != null){
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    alertDialog(shared, id, viewHolder);
                                }
                            });
                        }

                        //Wenn ich empfänger bin sender über löschen informieren
                    }else if(shared == 's' || shared == 'u' || shared == 'b'){
                        String current_user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();

                        //In ForMe rest in cloud damit nicht byMe beschrieben wird wenn der schon den Alarm gelöscht hat
                        DatabaseReference deleteAlarm = FirebaseDatabase.getInstance().getReference()
                                .child("alarms").child(current_user_id).child("ForMe").child(sender_id + sender_wecker_id).child("deleteDate");
                        long timeStamp = Calendar.getInstance().getTimeInMillis();
                        deleteAlarm.setValue(timeStamp);

                        deleteWecker(id);

                        if(isAdded() && getActivity() != null){
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //Update sharedFeed
                                    switch (shared){
                                        case 's': updateSharedFeed(-1, -1, 0); break;
                                        case 'u': updateSharedFeed(-1, 0, -1);
                                        case 'b': updateSharedFeed(-1, 0, 0);
                                    }
                                }
                            });
                        }

                    }else if(shared == 'd'){
                        String current_user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();

                        //In ForMe
                        DatabaseReference deleteAlarm = FirebaseDatabase.getInstance().getReference()
                                .child("alarms").child(current_user_id).child("ForMe").child(sender_id + sender_wecker_id);
                        deleteAlarm.removeValue();

                        deleteWecker(id);

                        //Wenn nicht geteilt von mir oder message von mir, hire löschen sonst in alertdialog
                    }else if(shared == 'l'){
                        deleteWecker(id);
                    }
                });


               /* //showing snack bar with Undo option
                Snackbar snackbar = Snackbar.make(view.findViewById(R.id.FrameLayout), name + " removed from cart!", Snackbar.LENGTH_LONG); //TODO für andere snackbars: findViewById(R.id.coordinatorLayout) statt Layouttype
                snackbar.setAction("UNDO", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // undo is selected, restore the deleted item
                        mAdapter.restoreItem(deletedItem, position);
                        removed = true;
                    }
                });
                snackbar.setActionTextColor(Color.YELLOW);
                snackbar.show();*/
            }
        }
    }

    //Alertdialog wenn nachricht von dem Alarm und / oder er selbst geteilt wurde
    private void alertDialog(final char shared, final int id, final RecyclerView.ViewHolder viewHolder){

        int setMessage = R.string.SharedWeckerDeleted;
        if(shared == 't'){
            setMessage = R.string.SharedWeckerANDDeleted;
        }else if(shared == 'm'){
            setMessage = R.string.SharedMessagesDeleted;
        }

        new AlertDialog.Builder(getActivity())
                .setMessage(setMessage)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                    if(shared == 'v' || shared == 't'){
                        //Firebase alarm löschen
                        String current_user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        DatabaseReference deleteAlarm = FirebaseDatabase.getInstance().getReference()
                                .child("alarms").child(current_user_id).child("ByMe").child(String.valueOf(id)).child("details");
                        deleteAlarm.removeValue();
                    }

                    if(shared == 'm' || shared == 't'){
                        //Firebase message löschen
                        DatabaseReference deleteMessage = FirebaseDatabase.getInstance().getReference()
                                .child("messages").child(FirebaseAuth.getInstance().getUid()).child("ByMe").child(String.valueOf(id)).child("details");
                        deleteMessage.removeValue();
                    /*DatabaseReference deleteMessage = FirebaseDatabase.getInstance().getReference()
                            .child("messages").child(current_user_id).child("ByMe").child("" + id).child("details").child("status");
                    deleteMessage.setValue("delete");*/
                    }

                    //Sharedalarmdaten
                    AppExecutor.getInstance().diskI0().execute(new Runnable() {
                        @Override
                        public void run() {
                            //Methode um Wecker zu löschen
                            deleteWecker(id);
                        }
                    });
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
                    }
                }).show();
    }

    //Called from onSwiped und Alertdialog
    private void deleteWecker(final int id){
        WeckerPOJO alarm = viewModel.getById(id);

        if(alarm.getShared() == 'v' || alarm.getShared() == 't'){
            getActivity().runOnUiThread(() -> {
                alarmsbyMeCount -= alarmsbyMeCount - 1;
                countByMe1.setText(getString(R.string.countCCFeed, alarmsbyMeCount));
            });
        }

        //Pending intent löschen
        if(alarm.isOn_off()) AlarmUtil.unsetAlarm(getContext(), alarm, false);

        viewModel.deleteById(id);
        //sharedAlarmFeed();
    }

    private void SentByMe0(){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Keine Alarme --> keine shared Alarms für mich
                countByMe1.setText(getString(R.string.countCCFeed, 0));
            }
        });
    }

    //Alertdialog wenn deleteAll
    private void dialogDeleteAll(){
        new AlertDialog.Builder(getActivity())
                .setMessage(getString(R.string.dialogDeleteAll))
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //getActivity().startService(i);
                        //TODO warum klappt sent by me auf 0 setzen nich ???
                        SentByMe0();
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateSharedFeed(-sharedTotal, -sharedNew, -sharedUpdated);
                            }
                        });

                        //Intent service zum ausschalten und löschen aus der datenbank, falls user zu schnell auf homeButton geht oder so
                        Intent i = new Intent(getActivity(), DeleteAllAlarms.class);
                        JobIntentService.enqueueWork(getActivity(), DeleteAllAlarms.class, JOB_ID, i);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).show();
    }

    //Necessary for BillingClient
    @Override
    public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchases) {}

    @Override
    public void onBillingSetupFinished(int responseCode) {}

    @Override
    public void onBillingServiceDisconnected() {}
}