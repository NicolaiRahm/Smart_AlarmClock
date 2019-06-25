package com.nicolai.alarm_clock;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nicolai.alarm_clock.adapter_helper.MockRecyclerAdapter_Receiver;
import com.nicolai.alarm_clock.adapter_helper.RecyclerAdapterSharedAlarms;
import com.nicolai.alarm_clock.adapter_helper.RecyclerItemTouchHelper2;
import com.nicolai.alarm_clock.pojos.FirebaseMessagePOJO;
import com.nicolai.alarm_clock.pojos.SharedEmpfaengerPOJO;
import com.nicolai.alarm_clock.pojos.WeckerPOJO;
import com.nicolai.alarm_clock.receiver_service.Alarm_Receiver;
import com.nicolai.alarm_clock.room_database.AlarmRepository;
import com.nicolai.alarm_clock.room_database.WeckerDatabase;
import com.nicolai.alarm_clock.util.AlarmUtil;
import com.nicolai.alarm_clock.util.DaysUtil;
import com.nicolai.alarm_clock.viewmodels.MainViewModel;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.constraintlayout.widget.Group;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.hdodenhof.circleimageview.CircleImageView;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;

public class ControlCenter extends AppCompatActivity implements RecyclerItemTouchHelper2.RecyclerItemTouchHelperListener, RecyclerAdapterSharedAlarms.SharedByMeClicked {

    private RecyclerView messageRecylerView;

    private CCAktuell ccAktuell;

    private WeckerDatabase database;

    private RecyclerView empfaenger;

    private RecyclerAdapterSharedAlarms mAdapter;

    private DatabaseReference mUsersDatabase, mMessagesDatabase;
    private FirebaseAuth mAuth;
    private String mCurrent_user_id, weekDay;

    private FirebaseRecyclerAdapter<SharedEmpfaengerPOJO, SharedFriendsViewHolder> firebaseRecyclerAdapter;
    private ValueEventListener messageDetailsListener;
    private DatabaseReference mMessageDetails;

    private ImageButton delete;
    private TextView lastSend, repeatView, alarmView, statusView, Mo, Di, Mi, Do, Fr, Sa, So;
    private TextView CCname, CCvon_or_day, CCin;
    private CircleImageView circleImage;
    private ImageView sharImage;

    private Group noAufpassenV, getNoAufpassenG;

    private int distance;
    private ViewFlipper viewFlipper;
    private boolean mock;

    private RecyclerView mRecyclerView;

    protected int countAnnaeherung, countNextMessage, showCaseCount;
    private static WaitForAsync waitForAsync;
    protected List<Integer> herausforedererDay = new ArrayList<Integer>(), herausfordererHour = new ArrayList<Integer>(), herausfordererMinute = new ArrayList<Integer>();
    protected List<String> userIds = new ArrayList<String>(), messageWeckerIds = new ArrayList<String>(), herausfordererDaysString = new ArrayList<String>();

    private FirebaseRecyclerAdapter<FirebaseMessagePOJO, ControlCenter.MessagesViewHolder> friendsRecyclerViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_center);

        if(getIntent() != null && getIntent().hasExtra("type")){
            mock = true;
        }

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbarVerwalten);
        setSupportActionBar(mToolbar);
        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();
        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setTitle(getString(R.string.control_center));

        //Weekday
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.GERMAN);
        weekDay = dayFormat.format(calendar.getTime());

        //Aktuelles Objekt
        ccAktuell = new CCAktuell("", -1);

        //Initialisierung Recyclerview
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerViewSharedAlarms);
        mRecyclerView.setNestedScrollingEnabled(false);
        messageRecylerView = (RecyclerView) findViewById(R.id.recyclerViewMassenges);
        messageRecylerView.setNestedScrollingEnabled(false);
        //messageRecylerView.setHasFixedSize(true);
        //mRecyclerView.setHasFixedSize(true);
        //Initialisierung Manager zum Anzeigen in Listenform
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        LinearLayoutManager mLinearLayoutManager2 = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        messageRecylerView.setLayoutManager(mLinearLayoutManager2);

        //VorläufigerAdapter
        List<WeckerPOJO> erstmalList = new ArrayList<>();
        //erstmalList.add(erstmalWecker);
        //Initialisierung RecyclerAdapter zum Raussuchen der passenden Objekte
        mAdapter = new RecyclerAdapterSharedAlarms(erstmalList, this, this);
        mRecyclerView.setAdapter(mAdapter);
        //Damit "Noch kein Wecker vorhanden" nicht swipebar!
        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new RecyclerItemTouchHelper2(0, ItemTouchHelper.LEFT, this);
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(mRecyclerView);

        //create database instance
        database = WeckerDatabase.getInstance(getApplicationContext());

        //Set up ViewModel
        if(!mock){
            final MainViewModel mainViewModel = ViewModelProviders.of(this).get(MainViewModel.class);
            mainViewModel.getLiveWeckers().observe(this, new Observer<List<WeckerPOJO>>() {
                @Override
                public void onChanged(@Nullable final List<WeckerPOJO> weckerPOJOS) {
                    //Damit delete über notify gehen kann
                    mainViewModel.getLiveWeckers().removeObserver(this);

                    //LiveData updates umsetzen
                    AppExecutor.getInstance().diskI0().execute(new Runnable() {
                        @Override
                        public void run() {

                            Iterator<WeckerPOJO> i = weckerPOJOS.iterator();
                            while (i.hasNext()) {
                                WeckerPOJO tescht = i.next();
                                if(tescht.getShared() != 'v' && tescht.getShared() != 't'){
                                    i.remove();
                                }
                            }

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //Den Adapter upgraden
                                    mAdapter.upgrade(weckerPOJOS);
                                }
                            });
                        }
                    });
                }
            });
        }else{
            //Mock alarm --> Adapter weiß, dass er auch mock message anzeigen muss
            WeckerPOJO mockWecker = new WeckerPOJO(0, getString(R.string.dgMockAlarmName), "xxxxxxxxfrxxxx", true,
                    7, 30,
                    3, 300000, 30, 1, "",
                    true, 'l', "{}", "{}", "{}", true, "",
                    "", "", "", true,
                    "", "", 30);
            WeckerPOJO platzhalterFuerMessage = new WeckerPOJO(-10, getString(R.string.dgMockAlarmName), "xxxxxxxxfrxxxx", true,
                    7, 30,
                    3, 300000, 30, 1, "",
                    true, 'l', "{}", "{}", "{}", true, "",
                    "", "", "", true,
                    "", "", 30);
            erstmalList.add(mockWecker);
            erstmalList.add(platzhalterFuerMessage);
            mAdapter.upgrade(erstmalList);
        }

        //################################# MESSAGE AUS-/EINFAHREN

        mAuth = FirebaseAuth.getInstance();
        mCurrent_user_id = mAuth.getCurrentUser().getUid();
        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("users");
        mMessagesDatabase = FirebaseDatabase.getInstance().getReference().child("messages").child(mCurrent_user_id).child("ForMe");

        //Big view
        CCname = findViewById(R.id.CCname);
        CCvon_or_day = findViewById(R.id.CCvon);
        CCin = findViewById(R.id.CCIn);
        circleImage = findViewById(R.id.CCcircle_image);

        sharImage = findViewById(R.id.CCshare);


        //Detail views
        delete = findViewById(R.id.message_delete);

        lastSend = findViewById(R.id.message_lastSend);
        repeatView = findViewById(R.id.message_alarmRepeat);
        alarmView = findViewById(R.id.message_alarmAlarm);
        statusView = findViewById(R.id.message_status);

        Mo = findViewById(R.id.detailsMo);
        Di = findViewById(R.id.detailsDi);
        Mi = findViewById(R.id.detailsMi);
        Do = findViewById(R.id.detailsDo);
        Fr = findViewById(R.id.detailsFr);
        Sa = findViewById(R.id.detailsSa);
        So = findViewById(R.id.detailsSo);

        final MotionLayout ml = findViewById(R.id.motionLayoutCC);

        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!mock){
                    //Get data vom aktuellen Objekt
                    final String from = ccAktuell.getSender_id();
                    final String messageWeckerID = String.valueOf(ccAktuell.getWecker_id());
                    //Sender über löschung informieren
                    final DatabaseReference messageSender = FirebaseDatabase.getInstance().getReference().child("messages").child(from)
                            .child("ByMe").child(messageWeckerID);

                    //Sicherung übernimmt cloudfunction
                    messageSender.child("deleted").child(mCurrent_user_id).setValue("");

                    //In ForMe löschen
                    final DatabaseReference forMe = FirebaseDatabase.getInstance().getReference().child("messages").child(mCurrent_user_id)
                            .child("ForMe");
                    forMe.child(from + messageWeckerID).removeValue();

                    //Layout zurücksetzen
                    ml.setProgress(0.0f);

                    nextMessage();
                }else{
                    //mAdapter TODO delete at first position when mock
                    //Layout zurücksetzen
                    Toast.makeText(ControlCenter.this, getString(R.string.testMessage), Toast.LENGTH_SHORT).show();
                    mAdapter.delete();
                    ml.setProgress(0.0f);
                }
            }
        });

        empfaenger = findViewById(R.id.sharedAlarm_recyclerVie);

        TextView editSharedAlarm = findViewById(R.id.editSharedAlarm);
        editSharedAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mock){
                    Intent editWecker = new Intent(ControlCenter.this, TimeSeter.class);
                    editWecker.putExtra("alt", ccAktuell.getWecker_id());
                    ControlCenter.this.startActivity(editWecker);
                }else{
                    Toast.makeText(ControlCenter.this, getString(R.string.testAlarm), Toast.LENGTH_SHORT).show();
                }
            }
        });

        //MessageDetails Groups
        noAufpassenV = findViewById(R.id.cc_noMessage_group);
        getNoAufpassenG = findViewById(R.id.cc_message_group);

        if(!mock){
            //Methods
            displayMessages();

            nextMessage();
        }else{
            messageDetails(true, "", "-10", getString(R.string.mockMessageSender), "");
        }


        //Flip layouts
        viewFlipper = findViewById(R.id.includeDetails);

        if(mock){
            //ShowCase View
            showCase();
        }
    }

    public void showCase(){
        //findViewById(mAdapter.getItemId(0));

        MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(this);

        MaterialShowcaseView.Builder roundBuilder = new MaterialShowcaseView.Builder(this)
                .setTarget(CCvon_or_day)
                .setDelay(500)
                .withRectangleShape()
                .setShapePadding(140)
                .setTitleText(R.string.scCCstart)
                .setContentText(R.string.scCCstartDesc)
                .setDismissText(R.string.gotIt);
        sequence.addSequenceItem(roundBuilder.build());

        MaterialShowcaseView.Builder rectangleBuilder = new MaterialShowcaseView.Builder(this)
                .setTarget(mRecyclerView)
                .withRectangleShape()
                .setDelay(500)
                .setShapePadding(40)
                .setTitleText(R.string.scCCList)
                .setContentText(R.string.scCCListDesc)
                .setDismissText(R.string.gotIt);
        sequence.addSequenceItem(rectangleBuilder.build());

        MaterialShowcaseView.Builder fabBuilder = new MaterialShowcaseView.Builder(this)
                .setTarget(findViewById(R.id.fabSharedDetails))
                .setDelay(500)
                .setShapePadding(40)
                .setTitleText(R.string.scCCFab)
                .setContentText(R.string.scCCFabDesc)
                .setDismissText(R.string.gotIt);
        sequence.addSequenceItem(fabBuilder.build());

        //DismissListener
        sequence.setOnItemDismissedListener(new MaterialShowcaseSequence.OnSequenceItemDismissedListener() {
            @Override
            public void onDismiss(MaterialShowcaseView materialShowcaseView, int i) {
                showCaseCount++;
                if(showCaseCount == 3){
                    MotionLayout ml = findViewById(R.id.motionLayoutCC);
                    ml.transitionToEnd();
                }
            }
        });

        MaterialShowcaseView.Builder ViewFliperBuilder = new MaterialShowcaseView.Builder(this)
                .setTarget(findViewById(R.id.includeDetails))
                .setDelay(500)
                .setShapePadding(40)
                .withRectangleShape()
                .setTitleText(R.string.scCCDetails)
                .setContentText(R.string.scCCDetailsDesc)
                .setDismissText(R.string.gotIt);
        sequence.addSequenceItem(ViewFliperBuilder.build());

        sequence.start();
    }

    /**
     * callback when recycler view is swiped
     * item will be removed on swiped
     * undo option will be provided in snackbar to restore the item
     */

    @Override
    public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction, final int position) {
        if (viewHolder instanceof RecyclerAdapterSharedAlarms.WeckerHolder) {
                final int id = (int) viewHolder.itemView.getTag(R.id.KEY1);

                //Sharedalarmdaten
                AppExecutor.getInstance().diskI0().execute(new Runnable() {
                    @Override
                    public void run() {
                        final char shared = database.weckerDao().findSharedById(id);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                alertDialog(shared, id, viewHolder);
                                //Wenn der Wecker das aktuelle Objekt ist
                                if(ccAktuell.getWecker_id() == id && ccAktuell.getSender_id().isEmpty()){
                                    sharImage.setVisibility(View.GONE);
                                    viewFlipper.setDisplayedChild(0);
                                    nextMessage();
                                }
                            }
                        });
                    }
                });
        }
    }

    //Alertdialog zum verwerfen der Aenderungen oder des Alarms bei Ersterstellung
    public void alertDialog(final char shared, final int id, final RecyclerView.ViewHolder viewHolder){

        int setMessage = R.string.SharedWeckerDeleted;
        if(shared == 't'){
            setMessage = R.string.SharedWeckerANDDeleted;
        }else if(shared == 'm'){
            setMessage = R.string.SharedMessagesDeleted;
        }

        new AlertDialog.Builder(this)
                .setMessage(setMessage)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        String current_user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();

                        if(shared == 'v' || shared == 't'){
                            //Firebase alarm löschen
                            DatabaseReference deleteAlarm = FirebaseDatabase.getInstance().getReference()
                                    .child("alarms").child(current_user_id).child("ByMe").child("" + id).child("details");
                            deleteAlarm.removeValue();
                        }

                        if(shared == 'v' || shared == 'm'){
                            //Firebase message löschen
                            DatabaseReference deleteMessage = FirebaseDatabase.getInstance().getReference()
                                    .child("messages").child(current_user_id).child("ByMe").child("" + id).child("details");
                            deleteMessage.removeValue();
                        /*DatabaseReference deleteMessage = FirebaseDatabase.getInstance().getReference()
                                .child("messages").child(current_user_id).child("ByMe").child("" + id).child("details").child("status");
                        deleteMessage.setValue("delete");*/
                        }

                        //Methode um Wecker zu löschen
                        deleteWecker(id, viewHolder);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
                    }
                }).show();
    }

    public void deleteWecker(final int id, final RecyclerView.ViewHolder viewHolder){
        if(!mock){
            //Sharedalarmdaten
            AppExecutor.getInstance().diskI0().execute(new Runnable() {
                @Override
                public void run() {
                    //Pending intent löschen
                    AlarmRepository mRepository = new AlarmRepository(getApplication());
                    AlarmUtil.unsetAlarm(getApplicationContext(), mRepository.getById(id), false);

                    database.weckerDao().deleteById(id);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mAdapter.removeItem(viewHolder);
                        }
                    });
                }
            });
        }else{
            Toast.makeText(ControlCenter.this, getString(R.string.testAlarm), Toast.LENGTH_SHORT).show();
        }
    }

    private void displayMessages (){

        FirebaseRecyclerOptions<FirebaseMessagePOJO> options = new FirebaseRecyclerOptions.Builder<FirebaseMessagePOJO>()
                .setQuery(mMessagesDatabase, FirebaseMessagePOJO.class)
                .build();

        friendsRecyclerViewAdapter = new FirebaseRecyclerAdapter<FirebaseMessagePOJO, MessagesViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final MessagesViewHolder holder, int position, @NonNull final FirebaseMessagePOJO model) {

                //Bin values to viewObject
                //holder.setDate(model.getDate());

                //id of the (clicked) user
                //final String list_user_id = getRef(position).getKey();
                final String sender_id = model.getSender_id() == null ? "EMPTY" : model.getSender_id();
                final String wecker_id = model.getWecker_id() == null ? "EMPTY" : model.getSender_id();

                //Query users Database
                mUsersDatabase.child(sender_id).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()){
                            String userName = dataSnapshot.child("name").getValue().toString();
                            String number = dataSnapshot.child("mobile_number").getValue().toString();
                            String thumbImage = dataSnapshot.child("thumb_image").getValue().toString();

                            if(!number.isEmpty() && ContextCompat.checkSelfPermission(ControlCenter.this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED){
                                String contactName = getContactDisplayNameByNumber(number);
                                if(!contactName.equals("?")){
                                    userName = contactName;
                                }
                            }

                            holder.setDetails(thumbImage, userName);
                        }else{
                            //Wenn Account gelöscht
                            mMessagesDatabase.child(sender_id + wecker_id).removeValue();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });


                //Message preview
                final DatabaseReference mMessageDetails = FirebaseDatabase.getInstance().getReference().child("messages")
                        .child(sender_id).child("ByMe").child(wecker_id).child("details");

                mMessageDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()){
                            String alarmName = dataSnapshot.child("name").getValue().toString();
                            String alarmHour = dataSnapshot.child("hour").getValue().toString();
                            String alarmMinute = dataSnapshot.child("minute").getValue().toString();

                            //Wenn Message nach WakeUp von sender gelöscht "(Gelöscht an Alarmnamen anhängen)"
                            if(dataSnapshot.child("deleted").exists()){
                                alarmName += getString(R.string.deletedAfterWakeUp);
                            }

                            String alarmTime;
                            if(Integer.parseInt(alarmMinute) < 10){
                                alarmTime = alarmHour + ":0" + alarmMinute;
                            }else{
                                alarmTime = alarmHour + ":" + alarmMinute;
                            }

                            holder.setDetails2(alarmName, alarmTime);

                        }else{

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

                //OnClick
                holder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //Wenn das nicht das Aktuelle Object ist
                        if(!ccAktuell.getSender_id().equals(sender_id) || ccAktuell.getWecker_id() != Integer.parseInt(wecker_id)) {
                            if(viewFlipper.getDisplayedChild() == 1){
                                viewFlipper.setDisplayedChild(0);
                            }
                            if(sharImage.getVisibility() == View.VISIBLE){
                                sharImage.setVisibility(View.GONE);
                            }
                            if(noAufpassenV.getVisibility() == View.VISIBLE){
                                noAufpassenV.setVisibility(View.GONE);
                                getNoAufpassenG.setVisibility(View.VISIBLE);
                            }
                            messageDetails(false, sender_id, wecker_id, holder.getUserName(), holder.getThumb_image());
                        }
                    }
                });
            }

            @NonNull
            @Override
            public MessagesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.recyclerview_item_rowmessages, parent, false);
                return new MessagesViewHolder(view);
            }
        };

        //Firebase Ui in gang setzen
        messageRecylerView.setAdapter(friendsRecyclerViewAdapter);
        friendsRecyclerViewAdapter.startListening();
    }

    public static class MessagesViewHolder extends RecyclerView.ViewHolder {

        private View mView;
        private TextView userName, alarmName, alarmTime;
        private ImageView profileImage;
        private String userNamed, thumb_image;

        public MessagesViewHolder(View itemView) {
            super(itemView);

            mView = itemView;
            userName = mView.findViewById(R.id.message_userName);
            alarmName = mView.findViewById(R.id.message_alarmName);
            alarmTime = mView.findViewById(R.id.message_alarmTime);
            profileImage = mView.findViewById(R.id.CCcircle_image);
        }

        public void setDetails(final String bitmapUrl, final String user_name){
            thumb_image = bitmapUrl;
            //Image mit Picasso library laden && offline sichern
            if(!bitmapUrl.isEmpty()){
                Picasso.get().load(bitmapUrl).networkPolicy(NetworkPolicy.OFFLINE).into(profileImage, new Callback() {
                    @Override
                    public void onSuccess() {
                        //Wurde offline gefunden
                    }

                    @Override
                    public void onError(Exception e) {
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                            //Muss online geladen werden
                            Picasso.get().load(bitmapUrl).placeholder(R.drawable.ic_account_circle_primary_24dp).into(profileImage);
                        }else{
                            //Muss online geladen werden
                            Picasso.get().load(bitmapUrl).into(profileImage);
                        }
                    }
                });
            }else{
                profileImage.setImageResource(R.drawable.ic_account_circle_primary_24dp);
            }

            //UserName
            userNamed = user_name;
            userName.setText(user_name);
        }

        public String getUserName(){
            return userNamed;
        }

        public String getThumb_image(){
            return thumb_image;
        }

        public void setDetails2(final String alarm_name, final String alarm_time){
            //AlarmName
            alarmName.setText(alarm_name);

            //AlarmTime
            alarmTime.setText(alarm_time);
        }
    }


    //#################Next Message ermitteln
    //Snapshot plus count for wFFirebaseMessages
    public void nextMessage(){
        //SharedMessageFeed
        //sharedFeedMessage = view.findViewById(R.id.sharedFeedMessage);
        //totalSharedMessage = view.findViewById(R.id.totalSharedMessage);

        //Wiederholtes berechnen von viewpager abfangen
        //if(countAnnaeherung == 0){

        String mCurrent_user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference mMessagesForMeDatabase = FirebaseDatabase.getInstance().getReference().child("messages").child(mCurrent_user_id).child("ForMe");

        mMessagesForMeDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    //Anzahl der Messages ForMe
                    countNextMessage = (int) dataSnapshot.getChildrenCount();
                    //totalSharedMessage.setText("" + countNextMessage);

                    //Alle Messagedaten aus Firebase laden
                    wFFirebaseMessages(dataSnapshot);

                    //datasnapshot alias ForMe existiert nicht
                }else {
                    //totalSharedMessage.setText("" + 0);
                    CCname.setText(R.string.keineSharedMessage);
                    CCvon_or_day.setText("");
                    CCin.setText("");
                    circleImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_account_circle_primary_24dp));

                    noAufpassenV.setVisibility(View.VISIBLE);
                    getNoAufpassenG.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                //totalSharedMessage.setText("?");
                //sharedFeedMessage.setText(getString(R.string.noSharedMessageConnection));
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
    public void wFFirebaseMessages(final DataSnapshot nextMessagesSnap) {

        AppExecutor.getInstance().diskI0().execute(new Runnable() {
            @Override
            public void run() {
                DatabaseReference mActualMessages = FirebaseDatabase.getInstance().getReference().child("messages");
                if (countAnnaeherung > 0) {
                    countAnnaeherung = 0;
                }

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
                                        //execute calculation in AsyncTask
                                        //Initialisieren / erneuern der AsyncTask plus starten von nextAlarm
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                waitForAsync = new ControlCenter.WaitForAsync(ControlCenter.this);
                                                waitForAsync.execute();
                                            }
                                        });
                                    }
                                }else if(countAnnaeherung == countNextMessage && !herausfordererHour.isEmpty()){
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            waitForAsync = new ControlCenter.WaitForAsync(ControlCenter.this);
                                            waitForAsync.execute();

                                            CCvon_or_day.setText(R.string.no_day_choosen);
                                            CCin.setText("");
                                        }
                                    });
                                }

                            }else if(countAnnaeherung == countNextMessage && !herausfordererHour.isEmpty()){
                                //execute calculation in AsyncTask
                                //Initialisieren / erneuern der AsyncTask plus starten von nextAlarm
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        waitForAsync = new ControlCenter.WaitForAsync(ControlCenter.this);
                                        waitForAsync.execute();
                                    }
                                });
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            CCname.setText(getString(R.string.noSharedMessageConnection));
                            CCvon_or_day.setText("");
                            CCin.setText("");
                            circleImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_account_circle_primary_24dp));

                            noAufpassenV.setVisibility(View.VISIBLE);
                            getNoAufpassenG.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
    }

    //Ermitteln der nächsten Nachricht
    private static class WaitForAsync extends AsyncTask<Void, Void, Boolean> {
        private ControlCenter parentActivity;
        private int hourDay, minuteDay, newDistance = -1, hourNextMessage, minuteNextMessage, weekDayInt, zahlFueri;

        private WaitForAsync(ControlCenter alarms){
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
                parentActivity.distance = newDistance;
                parentActivity.fillNextMessageTime(parentActivity.userIds.get(zahlFueri), parentActivity.messageWeckerIds.get(zahlFueri));
            }else{
                parentActivity.CCname.setText(parentActivity.getString(R.string.noSharedMessageConnection));
                parentActivity.CCvon_or_day.setText("");
                parentActivity.CCin.setText("");
                parentActivity.circleImage.setImageDrawable(parentActivity.getResources().getDrawable(R.drawable.ic_account_circle_primary_24dp));

                parentActivity.noAufpassenV.setVisibility(View.VISIBLE);
                parentActivity.getNoAufpassenG.setVisibility(View.GONE);
            }
        }
    }

    //Abholen aller Details der nächsten Nachricht
    public void fillNextMessageTime(final String byUserId, final String foreignWeckerId){
        //final String from = userIds.get(zahlFueri);
        //final String messageWeckerId = messageWeckerIds.get(zahlFueri);

        if(!mock){
            //Get Username
            DatabaseReference userDatabase = FirebaseDatabase.getInstance().getReference().child("users");
            userDatabase.child(byUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String userName = dataSnapshot.child("name").getValue().toString();
                    final String number = dataSnapshot.child("mobile_number").getValue().toString();
                    final String image = dataSnapshot.child("thumb_image").getValue().toString();

                    if(!number.isEmpty() && ContextCompat.checkSelfPermission(ControlCenter.this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED){
                        String contactName = getContactDisplayNameByNumber(number);
                        if(!contactName.equals("?")){
                            userName = contactName;
                        }
                    }

                    //View mit details füllen
                    messageDetails(true, byUserId, foreignWeckerId, userName, image);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }else{
            //TODO
        }
    }

    //In
    public void in(final int hour, final int minute){
        AppExecutor.getInstance().diskI0().execute(new Runnable() {
            @Override
            public void run() {
                //Berechnung der verbleibenden Zeit
                Calendar calendar = Calendar.getInstance();
                final int hourDay = calendar.get(Calendar.HOUR_OF_DAY);
                final int minuteDay = calendar.get(Calendar.MINUTE);

                int minutesLeft = distance * 24 * 60 - (hourDay - hour) * 60 - (minuteDay - minute);

                final int houresLeft = minutesLeft / 60;
                final int minutesLeftover = minutesLeft - houresLeft * 60;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String klingeltIn = getString(R.string.shared_in, houresLeft, minutesLeftover);
                            CCin.setText(klingeltIn);
                        }
                    });
            }
        });
    }


    //###################message details
    public void messageDetails(final boolean nextMessage, final String from, final String weckerId, final String userName, final String thumb_image){
        //Als Aktuelles Objekt
        ccAktuell.setSender_id(from);
        ccAktuell.setWecker_id(Integer.parseInt(weckerId));

        if(mMessageDetails != null && messageDetailsListener != null){
            mMessageDetails.removeEventListener(messageDetailsListener);
        }


        if(mock){
            if(nextMessage){
                CCname.setText(getString(R.string.next_message_name, getString(R.string.mockMessageName)));
            }else{
                CCname.setText(R.string.mockMessageName);
            }
            circleImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_account_circle_primary_24dp));
            Calendar calendar = Calendar.getInstance();
            int hourDay = calendar.get(Calendar.HOUR_OF_DAY);
            int minuteDay = calendar.get(Calendar.MINUTE);
            DateFormat df = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT);
            //Jetzt Zeit plus differenz zum Fälligkeitszeitpunkt der nächsten SharedMesage
            newDistance(9, 30, "xxxxmixxxxxxxx");

            long differnce = distance * 24 * 3600000 - ((hourDay - 9) * 3600000) - ((minuteDay - 30) * 60000);
            final String timeString = df.format(calendar.getTimeInMillis() + differnce);

            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
            calendar.setTimeInMillis(calendar.getTimeInMillis() + differnce);
            final String dateString = dayFormat.format(calendar.getTime());
            CCvon_or_day.setText(getString(R.string.message_von_um, userName, dateString, timeString));
            in(9, 30);

            //----------------------Details-----------------------
            //Alarmdetails alarm
            String alarm = getString(R.string.intervall_details, 3, 5);


            calendar.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
            long dateInMillis = calendar.getTimeInMillis() - 7*24*60*60*1000+(5*69*1000);


            DateFormat df2 = SimpleDateFormat.getTimeInstance(SimpleDateFormat.MEDIUM, Locale.getDefault());
            String dateS = df2.format(dateInMillis);
            String dateFull = getString(R.string.tag_time, dayFormat.format(dateInMillis), dateS);
            //Zuletzt aktiv
            lastSend.setText(getString(R.string.message_lastSend, dateFull));

            //Alarm wiederholung
            repeatView.setText(getString(R.string.message_alarmRepeatYes));

            //Alarmintervall und Anzahl
            alarmView.setText(alarm);

            //Status
            statusView.setText(getString(R.string.message_turnedOff, getString(R.string.statusM_not_used_yet)));

            //Days
            Mi.setBackground(getResources().getDrawable(R.drawable.message_alarm_days));
        }



        if(!mock){
            mMessageDetails = FirebaseDatabase.getInstance().getReference().child("messages")
                    .child(from).child("ByMe").child(weckerId).child("details");

            messageDetailsListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()){

                        //##CircleImage##
                        if(!thumb_image.isEmpty()){//TODO PLACEHOLDER: load().placeholder
                            //Picasso.get().load(image).into(circleProfilePicture);
                            Picasso.get().load(thumb_image).networkPolicy(NetworkPolicy.OFFLINE).into(circleImage, new Callback() {
                                @Override
                                public void onSuccess() {
                                    //Wurde offline gefunden
                                }

                                @Override
                                public void onError(Exception e) {
                                    //Muss online geladen werden
                                    Picasso.get().load(thumb_image).into(circleImage);
                                }
                            });
                        }else{
                            circleImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_account_circle_primary_24dp));
                        }

                        //###Name##
                        String alarmName = dataSnapshot.child("name").getValue().toString();
                        //Wenn Message nach WakeUp von sender gelöscht "(Gelöscht an Alarmnamen anhängen)"
                        if(dataSnapshot.child("deleted").exists()){
                            alarmName += getString(R.string.deletedAfterWakeUp);
                        }

                        if(!nextMessage){
                            CCname.setText(alarmName);
                        }else{
                            CCname.setText(getString(R.string.next_message_name, dataSnapshot.child("name").getValue().toString()));
                        }

                        //##Von am##
                        int alarmHour = Integer.parseInt(dataSnapshot.child("hour").getValue().toString());
                        int alarmMinute = Integer.parseInt(dataSnapshot.child("minute").getValue().toString());
                        String daysXformat = dataSnapshot.child("days").getValue().toString();

                        Calendar calendar = Calendar.getInstance();
                        int hourDay = calendar.get(Calendar.HOUR_OF_DAY);
                        int minuteDay = calendar.get(Calendar.MINUTE);
                        DateFormat df = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT);
                        //Jetzt Zeit plus differenz zum Fälligkeitszeitpunkt der nächsten SharedMesage
                        newDistance(alarmHour, alarmMinute, daysXformat);

                        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());

                        //TODO brauh man nicht zwingend
                        if(distance != 8){
                            long differnce = distance * 24 * 3600000 - ((hourDay - alarmHour) * 3600000) - ((minuteDay - alarmMinute) * 60000);
                            final String timeString = df.format(calendar.getTimeInMillis() + differnce);

                            calendar.setTimeInMillis(calendar.getTimeInMillis() + differnce);
                            final String dateString = dayFormat.format(calendar.getTime());

                            CCvon_or_day.setText(getString(R.string.message_von_um, userName, dateString, timeString));

                            //##In##
                            in(alarmHour, alarmMinute);
                        }else{
                            CCin.setText("");
                            CCvon_or_day.setText(R.string.no_day_choosen);

                        }


                        //----------------------Details-----------------------

                        //Alarmdetails Repeat
                        boolean repeat = (boolean) dataSnapshot.child("repeat").getValue();
                        //Alarmdetails Days
                        String days = dataSnapshot.child("days").getValue().toString();

                        //Alarmdetails alarm
                        String alarm = getResources().getString(R.string.intervall_details_once, getString(R.string.alarmOnce));
                        long alarmanzahl = (long) dataSnapshot.child("anzahl").getValue();
                        if(alarmanzahl > 1) {
                            long intervall = (long) dataSnapshot.child("intervall").getValue();
                            alarm = getString(R.string.intervall_details, alarmanzahl, intervall / 60000);
                        }

                        //Status mit string resources verbinden
                        String status = dataSnapshot.child("status").getValue().toString();
                        switch (status){
                            case "not used yet": status = getString(R.string.statusM_not_used_yet); break;

                            case "online": status = getString(R.string.status_online); break;
                            case "verschlafen": status = getString(R.string.status_verschlafen); break;
                            case "aufgestanden": status = getString(R.string.status_aufgestanden); break;
                            case "schlummert": status = getString(R.string.status_schlummert); break;

                            default: status = ""; break;
                        }


                        long dateInMillis = (long) dataSnapshot.child("date").getValue();


                        DateFormat df2 = SimpleDateFormat.getTimeInstance(SimpleDateFormat.MEDIUM, Locale.getDefault());
                        String dateS = df2.format(dateInMillis);
                        String dateFull = getString(R.string.tag_time, dayFormat.format(dateInMillis), dateS);
                        //Zuletzt aktiv
                        lastSend.setText(getString(R.string.message_lastSend, dateFull));

                        //Alarm wiederholung
                        if(repeat){
                            repeatView.setText(getString(R.string.message_alarmRepeatYes));
                        }else{
                            repeatView.setText(getString(R.string.message_alarmRepeatNo));
                        }

                        //Alarmintervall und Anzahl
                        alarmView.setText(alarm);

                        //Status
                        statusView.setText(getString(R.string.message_turnedOff, status));

                        //Days
                        if(days.charAt(0) != 'x'){
                            Mo.setBackground(getResources().getDrawable(R.drawable.message_alarm_days));
                        }else{
                            Mo.setBackground(getResources().getDrawable(R.drawable.search_layout));
                        }

                        if(days.charAt(2) != 'x'){
                            Di.setBackground(getResources().getDrawable(R.drawable.message_alarm_days));
                        }else{
                            Di.setBackground(getResources().getDrawable(R.drawable.search_layout));
                        }

                        if(days.charAt(4) != 'x'){
                            Mi.setBackground(getResources().getDrawable(R.drawable.message_alarm_days));
                        }else{
                            Mi.setBackground(getResources().getDrawable(R.drawable.search_layout));
                        }

                        if(days.charAt(6) != 'x'){
                            Do.setBackground(getResources().getDrawable(R.drawable.message_alarm_days));
                        }else{
                            Do.setBackground(getResources().getDrawable(R.drawable.search_layout));
                        }

                        if(days.charAt(8) != 'x'){
                            Fr.setBackground(getResources().getDrawable(R.drawable.message_alarm_days));
                        }else{
                            Fr.setBackground(getResources().getDrawable(R.drawable.search_layout));
                        }

                        if(days.charAt(10) != 'x'){
                            Sa.setBackground(getResources().getDrawable(R.drawable.message_alarm_days));
                        }else{
                            Sa.setBackground(getResources().getDrawable(R.drawable.search_layout));
                        }

                        if(days.charAt(12) != 'x'){
                            So.setBackground(getResources().getDrawable(R.drawable.message_alarm_days));
                        }else{
                            So.setBackground(getResources().getDrawable(R.drawable.search_layout));
                        }


                    }else{

                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            };
            //Add Listener
            mMessageDetails.addValueEventListener(messageDetailsListener);
        }
    }

    public void newDistance(final int hour, final int minute, final String dayXformat){
        //Next Wecker
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.GERMAN);
        final String weekDay = dayFormat.format(calendar.getTime());
        int weekDayInt = DaysUtil.intForDay(weekDay);
        int hourDay = calendar.get(Calendar.HOUR_OF_DAY);
        int minuteDay = calendar.get(Calendar.MINUTE);//calendar.get(calendar.get(Calendar.MINUTE));

        int nextDay = DaysUtil.nextDay(weekDay, dayXformat);

        //Wenn es einen nächsten Tag gibt
        if(nextDay != 20){
            distance = 0;
            int weekDayIntCopy = weekDayInt;

            //Wenn heute in einer Woche
            if(weekDayInt == nextDay && (hour < hourDay || (hour == hourDay && minute <= minuteDay))){
                //Gibts noch n anderen Tag?!
                int xCount = 0;
                for(int j = 0; j < 14; j++){

                    if (dayXformat.charAt(j) == 'x'){
                        xCount ++;
                    }
                }

                if(xCount == 12){
                    distance = 7;
                }else{
                    //Symulate the circle with 7 steps 0-12
                    String withoutToday = dayXformat;
                    StringBuilder cut = new StringBuilder(withoutToday);
                    cut.setCharAt(nextDay*2, 'x');
                    cut.setCharAt(nextDay*2+1, 'x');
                    withoutToday = cut.toString();
                    nextDay = DaysUtil.nextDay(weekDay, withoutToday);

                    while (weekDayIntCopy != nextDay) {
                        weekDayIntCopy++;
                        distance++;

                        if (weekDayIntCopy > 6) {
                            weekDayIntCopy = 0;
                        }
                    }
                }
            }else{
                //Symulate the circle with 7 steps 0-12
                while (weekDayIntCopy != nextDay) {
                    weekDayIntCopy++;
                    distance++;

                    if (weekDayIntCopy > 6) {
                        weekDayIntCopy = 0;
                    }
                }
            }
        }else {
            distance = 8;
        }
    }


    //##################Alarmdetails
    @Override
    public void itemClicked(int id) {

        if(!mock){
            if(!ccAktuell.getSender_id().isEmpty() || ccAktuell.getWecker_id() != id){

                //Als Aktuelles Objekt setzen
                ccAktuell.setSender_id("");
                ccAktuell.setWecker_id(id);

                if(viewFlipper.getDisplayedChild() == 0){
                    viewFlipper.setDisplayedChild(1);
                }

                if(sharImage.getVisibility() == View.GONE){
                    circleImage.setImageDrawable(getResources().getDrawable(R.drawable.empty_alarm_80dp));
                    sharImage.setVisibility(View.VISIBLE);
                }
            }

            alarmDetails(id);
        }else{
            //Mock Message
            if(id == -10){
                if(viewFlipper.getDisplayedChild() == 1){
                    viewFlipper.setDisplayedChild(0);
                }

                if(sharImage.getVisibility() == View.VISIBLE){
                    circleImage.setImageResource(R.drawable.ic_account_circle_primary_24dp);
                    sharImage.setVisibility(View.GONE);
                }

                messageDetails(false, "", "-10", getString(R.string.mockMessageSender), "");
            }else{
                //Mock alarm
                if(viewFlipper.getDisplayedChild() == 0){
                    viewFlipper.setDisplayedChild(1);
                }

                if(sharImage.getVisibility() == View.GONE){
                    circleImage.setImageResource(R.drawable.empty_alarm_80dp);
                    sharImage.setVisibility(View.VISIBLE);
                }

                alarmDetails(id);
            }
        }
    }

    public void alarmDetails(final int weckerID){
        if(!mock){
            if(firebaseRecyclerAdapter != null){
                firebaseRecyclerAdapter.stopListening();
            }

            firebaseRecyclerAdapter = displaySearch(weckerID);

            //##Name##
            WeckerPOJO currentWecker = WeckerDatabase.getInstance(getApplicationContext()).weckerDao().findRowByID(weckerID);
            CCname.setText(currentWecker.getName());

            //##Am##
            Calendar calendar = Calendar.getInstance();
            int hourDay = calendar.get(Calendar.HOUR_OF_DAY);
            int minuteDay = calendar.get(Calendar.MINUTE);

            //Distanz des nächsten Tages zum heutigen
            newDistance(currentWecker.getHour(), currentWecker.getMinute(), currentWecker.getDays());

            //Wenn tag markiert
            if(distance != 8){
                long differnce = distance * 24 * 3600000 - (hourDay - currentWecker.getHour()) * 3600000 - (minuteDay - currentWecker.getMinute()) * 60000;

                DateFormat df = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT);
                final String timeString = df.format(calendar.getTimeInMillis() + differnce);

                calendar.setTimeInMillis(calendar.getTimeInMillis() + differnce);
                SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
                final String dateString = dayFormat.format(calendar.getTime());

                CCvon_or_day.setText(getString(R.string.shared_am, dateString, timeString));

                //##In##
                in(currentWecker.getHour(), currentWecker.getMinute());
            }else{
                CCvon_or_day.setText(R.string.no_day_choosen);
                CCin.setText("");
            }

            //---------------------Details
            empfaenger.setLayoutManager(new LinearLayoutManager(this));
            empfaenger.setAdapter(firebaseRecyclerAdapter);
            firebaseRecyclerAdapter.startListening();
        }else{
            if(weckerID == -10){
                messageDetails(false, "", "-10", getString(R.string.mockMessageSender), "");
            }else{
                //##Name##
                CCname.setText(R.string.dgMockAlarmName);

                //##Am##
                Calendar calendar = Calendar.getInstance();
                int hourDay = calendar.get(Calendar.HOUR_OF_DAY);
                int minuteDay = calendar.get(Calendar.MINUTE);

                //Distanz des nächsten Tages zum heutigen
                newDistance(7, 30, "xxxxxxxxfrxxxx");

                //Wenn tag markiert
                if(distance != 8){
                    long differnce = distance * 24 * 3600000 - (hourDay - 7) * 3600000 - (minuteDay - 30) * 60000;

                    DateFormat df = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT);
                    final String timeString = df.format(calendar.getTimeInMillis() + differnce);

                    calendar.setTimeInMillis(calendar.getTimeInMillis() + differnce);
                    SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
                    final String dateString = dayFormat.format(calendar.getTime());

                    CCvon_or_day.setText(getString(R.string.shared_am, dateString, timeString));

                    //##In##
                    in(7, 30);
                }

                //---------------------Details
                MockRecyclerAdapter_Receiver adapter = new MockRecyclerAdapter_Receiver(true);
                empfaenger.setLayoutManager(new LinearLayoutManager(this));
                empfaenger.setAdapter(adapter);//TODO recyclerAdapter für mock empfänger
            }
        }
    }



    private FirebaseRecyclerAdapter<SharedEmpfaengerPOJO, SharedFriendsViewHolder> displaySearch (int weckerId){

        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("users");
        DatabaseReference mAlarmByMeDatabase = FirebaseDatabase.getInstance().getReference().child("alarms").child(mCurrent_user_id).child("ByMe");

        FirebaseRecyclerOptions<SharedEmpfaengerPOJO>  options = new FirebaseRecyclerOptions.Builder<SharedEmpfaengerPOJO>()
                .setQuery(mAlarmByMeDatabase.child("" + weckerId).child("to"), SharedEmpfaengerPOJO.class)
                .build();

        FirebaseRecyclerAdapter<SharedEmpfaengerPOJO, SharedFriendsViewHolder> sharedWithAdapter = new FirebaseRecyclerAdapter<SharedEmpfaengerPOJO, SharedFriendsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final SharedFriendsViewHolder holder, int position, @NonNull final SharedEmpfaengerPOJO model) {

                //Bin values to viewObject
                //holder.setDate(model.getDate());

                //id of the (clicked) user
                final String list_user_id = getRef(position).getKey();

                //Query users Database
                mUsersDatabase.child(list_user_id).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        String userName = dataSnapshot.child("name").getValue().toString();
                        String number = dataSnapshot.child("mobile_number").getValue().toString();
                        String thumbImage = dataSnapshot.child("thumb_image").getValue().toString();

                        if(!number.isEmpty() && ContextCompat.checkSelfPermission(ControlCenter.this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED){
                            String contactName = getContactDisplayNameByNumber(number);
                            if(!contactName.equals("?")){
                                userName = contactName;
                            }
                        }

                        holder.setDetails(userName, thumbImage, model.getStatus(), model.getDate());
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }

            @NonNull
            @Override
            public SharedFriendsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_row_shared_recyclerview, parent, false);
                return new SharedFriendsViewHolder(view);
            }
        };

        return sharedWithAdapter;
    }

    public static class SharedFriendsViewHolder extends RecyclerView.ViewHolder {

        View mView;
        ImageView userImage;
        TextView userName, sendOn, status;

        public SharedFriendsViewHolder(View itemView) {
            super(itemView);

            mView = itemView;
            userImage = mView.findViewById(R.id.CCcircle_image);
            userName = mView.findViewById(R.id.sharedAlarm_empfaengerName);
            sendOn = mView.findViewById(R.id.sharedAlarm_empfängerSendOn);
            status = mView.findViewById(R.id.sharedAlarm_empfaengerStatus);
        }

        public void setDetails(final String name, final String bitmapUrl, String statusUser, long date){
            //SET NAME
            userName.setText(name);
            switch (statusUser){
                case "not downloaded yet": statusUser = itemView.getContext().getString(R.string.statusA_not_downloaded_yet); break;
                case "downloaded": statusUser = itemView.getContext().getString(R.string.statusA_downloaded); break;
                case "not updated yet": statusUser = itemView.getContext().getString(R.string.statusA_not_updated_yet); break;
                case "update_downloaded": statusUser = itemView.getContext().getString(R.string.statusA_update_downloaded); break;
                case "deleted": statusUser = itemView.getContext().getString(R.string.statusA_deleted); break;
                case "bestaetigt": statusUser = itemView.getContext().getString(R.string.statusA_bestaetigt); break;

                case "online": statusUser = itemView.getContext().getString(R.string.status_online); break;
                case "verschlafen": statusUser = itemView.getContext().getString(R.string.status_verschlafen); break;
                case "aufgestanden": statusUser = itemView.getContext().getString(R.string.status_aufgestanden); break;
                case "schlummert": statusUser = itemView.getContext().getString(R.string.status_schlummert); break;

                default: statusUser = ""; break;
            }

            status.setText(statusUser);

            //Wenn gerade von mir erstellt kann der empfänger noch kein datum geposted haben
            if(!statusUser.equals(itemView.getContext().getString(R.string.statusA_not_downloaded_yet)) && date > 0){

                SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
                final String dateString = dayFormat.format(date);

                DateFormat df = SimpleDateFormat.getTimeInstance(SimpleDateFormat.MEDIUM, Locale.getDefault());
                String dateS = df.format(date);
                sendOn.setText(itemView.getContext().getString(R.string.tag_time, dateString, dateS));
            }

            //Image mit Picasso library laden && offline sichern
            if(!bitmapUrl.isEmpty()){
                Picasso.get().load(bitmapUrl).networkPolicy(NetworkPolicy.OFFLINE).into(userImage, new Callback() {
                    @Override
                    public void onSuccess() {
                        //Wurde offline gefunden
                    }

                    @Override
                    public void onError(Exception e) {
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                            //Muss online geladen werden
                            Picasso.get().load(bitmapUrl).placeholder(R.drawable.ic_account_circle_primary_24dp).into(userImage);
                        }else{
                            //Muss online geladen werden
                            Picasso.get().load(bitmapUrl).into(userImage);
                        }
                    }
                });
            }else{
                userImage.setImageResource(R.drawable.ic_account_circle_primary_24dp);
            }
        }
    }



    //TODO vlt Async
    //Get contact name for number
    public String getContactDisplayNameByNumber(String number) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        String name = "?";

        ContentResolver contentResolver = getContentResolver();
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
    public void onPause(){
        super.onPause();
        //FirebaseListener ausschalten
        if(friendsRecyclerViewAdapter != null){
            friendsRecyclerViewAdapter.stopListening();
        }

        //Listener für Alarmempfänger
        if(firebaseRecyclerAdapter != null){
            firebaseRecyclerAdapter.stopListening();
        }

        if(mMessageDetails != null && messageDetailsListener != null){
            mMessageDetails.removeEventListener(messageDetailsListener);
        }

        /*if(mMessagesDatabase != null){
            mMessagesDatabase.removeEventListener();
        }*/
    }
}
