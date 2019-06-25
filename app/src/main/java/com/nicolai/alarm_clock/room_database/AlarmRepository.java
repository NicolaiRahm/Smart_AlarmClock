package com.nicolai.alarm_clock.room_database;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.nicolai.alarm_clock.Invite;
import com.nicolai.alarm_clock.R;
import com.nicolai.alarm_clock.pojos.FB;
import com.nicolai.alarm_clock.pojos.WeckerPOJO;
import com.nicolai.alarm_clock.util.FullVersionUtil;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.lifecycle.LiveData;

public class AlarmRepository {

    private WeckerDao mDao;
    private DatabaseReference firebaseDB;
    private String myFB_Id;
    private LiveData<List<WeckerPOJO>> mAlarmsLive;

    public AlarmRepository(Context context){
        WeckerDatabase db = WeckerDatabase.getInstance(context);
        mDao = db.weckerDao();

        firebaseDB = FirebaseDatabase.getInstance().getReference();
        myFB_Id = FirebaseAuth.getInstance().getUid();

        mAlarmsLive = mDao.loadAllWeckers();
    }

    public LiveData<List<WeckerPOJO>> getAllAlarms() {
        return mAlarmsLive;
    }

    //Boot list
    public List<WeckerPOJO> getBootList () {
        bootListAsyncTask asyncTask = new bootListAsyncTask(mDao);
        try {
            return asyncTask.execute().get();
        }catch (java.util.concurrent.ExecutionException e){
            return null;
        }catch (java.lang.InterruptedException e){
            return null;
        }
    }

    private static class bootListAsyncTask extends AsyncTask<Void, Void, List<WeckerPOJO>> {

        private WeckerDao mAsyncTaskDao;

        bootListAsyncTask(WeckerDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected List<WeckerPOJO> doInBackground(final Void... params) {
            return mAsyncTaskDao.getBootList();
        }
    }

    //Insert new alarm clock
    public long insert (WeckerPOJO alarm) {
        insertAsyncTask asyncTask = new insertAsyncTask(mDao);
        try {
            return asyncTask.execute(alarm).get();
        }catch (java.util.concurrent.ExecutionException e){
            return 0;
        }catch (java.lang.InterruptedException e){
            return 0;
        }
    }

    private static class insertAsyncTask extends AsyncTask<WeckerPOJO, Void, Long> {

        private WeckerDao mAsyncTaskDao;

        insertAsyncTask(WeckerDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Long doInBackground(final WeckerPOJO... params) {
            return mAsyncTaskDao.insertWecker(params[0]);
        }
    }


    //delete by id
    public void deleteById(int id){
        new deleteByIdAsyncTask(mDao).execute(id);
    }

    private static class deleteByIdAsyncTask extends AsyncTask<Integer, Void, Void> {

        private WeckerDao mAsyncTaskDao;

        deleteByIdAsyncTask(WeckerDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final Integer... params) {
            mAsyncTaskDao.deleteById(params[0]);
            return null;
        }
    }


    //Get by id
    public WeckerPOJO getById(int id){
        getByIdAsyncTask alarm =  new getByIdAsyncTask(mDao);
        try {
            return alarm.execute(id).get();
        }catch (java.util.concurrent.ExecutionException e){
            return null;
        }catch (java.lang.InterruptedException e){
            return null;
        }
    }

    private static class getByIdAsyncTask extends AsyncTask<Integer, Void, WeckerPOJO> {

        private WeckerDao mAsyncTaskDao;

        getByIdAsyncTask(WeckerDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected WeckerPOJO doInBackground(final Integer... params) {
            return mAsyncTaskDao.findRowByID(params[0]);
        }
    }


    //Update by id
    public void update(WeckerPOJO updatedAlarm){
        new updateAsyncTask(mDao).execute(updatedAlarm);
    }

    private static class updateAsyncTask extends AsyncTask<WeckerPOJO, Void, Void> {

        private WeckerDao mAsyncTaskDao;

        updateAsyncTask(WeckerDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final WeckerPOJO... params) {
            mAsyncTaskDao.updateWecker(params[0]);
            return null;
        }
    }




    /*
    **************************************** FIREBASE ACTION ***************************************
     */

    //Delete "delete" folder from my messages
    public void messagesDeletedFld(int id){
        firebaseDB.child(FB.MESSAGES).child(myFB_Id).child(FB.BY_ME)
                .child(String.valueOf(id)).child(FB.MESSAGES_DELETED).removeValue();
    }

    //Alarm clock was accepted -> sender_to_myId_bestaetigt + timeStamp
    public void alarmApproved(String sender_id, String sender_alarm_id ){
        DatabaseReference db = firebaseDB.child(FB.ALARMS).child(sender_id).child(FB.BY_ME).child(sender_alarm_id).child(FB.TO).child(myFB_Id);

        db.child(FB.STATUS).child(FB.bestaetigt);

        long timeStamp = Calendar.getInstance().getTimeInMillis();
        db.child(FB.DATE).setValue(timeStamp);
    }

    //Fullversion true
    public void fullVersion(){
        firebaseDB.child(FB.USERS).child(myFB_Id).child(FB.FULL_VERSION).setValue(true);
    }

    //Cahnge status to "downloaded" + (my) weckerID and in ByMe of the sender
    public void downloaded(String sender_id, String wecker_id, int sqLite_id, boolean senderExists){
        //Cahnge status to "downloaded" and (my) weckerID
        DatabaseReference forMe = firebaseDB.child(FB.ALARMS).child(myFB_Id).child(FB.FOR_ME);
        forMe.child(sender_id + wecker_id).child(FB.STATUS).setValue(FB.downloaded);
        forMe.child(sender_id + wecker_id).child(FB.MY_WECKER_ID).setValue(String.valueOf(sqLite_id));

        if(senderExists){
            //Change status in ByMe/to/empfänger/status
            DatabaseReference BySender = firebaseDB.child(FB.ALARMS).child(sender_id).child(FB.BY_ME).child(wecker_id).child(FB.TO).child(myFB_Id);
            BySender.child(FB.STATUS).setValue(FB.downloaded);

            long timeStamp = Calendar.getInstance().getTimeInMillis();
            BySender.child(FB.DATE).setValue(timeStamp);
        }
    }

    //Change status in ForMe and + timestamp for sender
    public void updateDownloaded(String sender_id, String senderAlarmId){
        DatabaseReference forMe = firebaseDB.child(FB.ALARMS).child(myFB_Id).child(FB.FOR_ME);
        forMe.child("" + sender_id + senderAlarmId).child(FB.STATUS).setValue(FB.update_downloaded);

        DatabaseReference BySender = FirebaseDatabase.getInstance().getReference()
                .child(FB.ALARMS).child(sender_id).child(FB.BY_ME).child(senderAlarmId).child(FB.TO).child(myFB_Id);
        BySender.child(FB.STATUS).setValue(FB.update_downloaded);
        BySender.child(FB.DATE).setValue(Calendar.getInstance().getTimeInMillis());
    }

    public void wakeUp_shared(Context context, boolean appMessage, char iAmEmpfaenger, WeckerPOJO klingelnderWecker, String message){
        //Message über App verschicken
        if (appMessage) {

            DatabaseReference mUserDatabase = firebaseDB.child(FB.MESSAGES).child(myFB_Id)
                    .child(FB.BY_ME).child(String.valueOf(klingelnderWecker.getId())).child(FB.DETAILS);

            Map<String, Object> messagesChilds = new HashMap<>();
            messagesChilds.put(FB.STATUS, message);

            long timeStamp = Calendar.getInstance().getTimeInMillis();
            messagesChilds.put(FB.DATE, timeStamp);

            mUserDatabase.updateChildren(messagesChilds);

            //Update number of free messages if the user doesn't has the full version
            SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            if(!mSharedPreferences.getBoolean(context.getString(R.string.sharedFullVersion), false) &&
                    (message.equals(FB.verschlafen) || message.equals(FB.aufgestanden))){
                final TaskCompletionSource<Long> source = new TaskCompletionSource<>();
                Task<Long> task = source.getTask();
                task.addOnSuccessListener(freeLeft -> {
                    if(freeLeft > 1L){
                        DatabaseReference mUser = firebaseDB.child(FB.USERS).child(myFB_Id)
                                .child(FB.FREE_LEFT);
                        mUser.setValue(freeLeft - 1);
                    }else{
                        DatabaseReference mUser = firebaseDB.child(FB.USERS).child(myFB_Id)
                                .child(FB.FREE_LEFT);
                        mUser.setValue(null);
                    }
                });

                FullVersionUtil.freeLeft(source);
            }
        }

        //Alarm
        if(iAmEmpfaenger == 'b'){//Message for alarm sender

            String sender_id = klingelnderWecker.getSender_id();
            String senderWecker_id = klingelnderWecker.getSender_wecker_id();

            DatabaseReference sharedAlarm = firebaseDB.child(FB.ALARMS).child(sender_id)
                    .child(FB.BY_ME).child(senderWecker_id).child(FB.TO).child(myFB_Id);

            Map<String, Object> alarmChilds = new HashMap<>();
            alarmChilds.put(FB.STATUS, message);
            //Timestamp
            long timeStamp = Calendar.getInstance().getTimeInMillis();
            alarmChilds.put(FB.DATE, timeStamp);

            sharedAlarm.updateChildren(alarmChilds);
        }
    }

    public void deleteSafe(WeckerPOJO mAlarm){
        char shared = mAlarm.getShared();

        //Wenn Alarm für mich  ... ForMe löschen und Sender benachrichtigen
        if(shared == 'b'){
            String sender_id = mAlarm.getSender_id();
            String sender_wecker_id = mAlarm.getSender_wecker_id();

            //In ForMe
            DatabaseReference deleteAlarm = firebaseDB.child(FB.ALARMS).child(myFB_Id).child(FB.FOR_ME).child(sender_id + sender_wecker_id);
            deleteAlarm.removeValue();

            //Status und date unter "to" bei Sender ändern
            final DatabaseReference statusDeleted = firebaseDB.child(FB.ALARMS).child(sender_id).child(FB.BY_ME)
                    .child(sender_wecker_id).child(FB.TO).child(myFB_Id);

            Map<String, Object> messagesChilds = new HashMap<>();
            messagesChilds.put(FB.STATUS, FB.deleted);
            //Timestamp
            long timeStamp = Calendar.getInstance().getTimeInMillis();
            messagesChilds.put(FB.DATE, timeStamp);

            statusDeleted.updateChildren(messagesChilds);

            //Alarm von mir geteilt
        }else if(shared == 'v' || shared == 't' || shared == 'm'){

            if(shared == 'v' || shared == 't'){
                DatabaseReference deleteAlarm = firebaseDB.child(FB.ALARMS).child(myFB_Id).child(FB.BY_ME)
                        .child(String.valueOf(mAlarm.getId())).child(FB.DETAILS);
                deleteAlarm.removeValue();
            }

            if(shared == 'm' || shared == 't'){
                //Firebase message löschen / by empfänger als gelöscht anzeigen
                DatabaseReference deleteMessage = firebaseDB.child(FB.MESSAGES).child(myFB_Id).
                        child(FB.BY_ME).child(String.valueOf(mAlarm.getId())).child(FB.DETAILS).child(FB.deleted);
                deleteMessage.setValue(FB.deleted);
                /*DatabaseReference deleteMessage = FirebaseDatabase.getInstance().getReference()
                        .child("messages").child(current_user_id).child("ByMe").child("" + id).child("details").child("status");
                deleteMessage.setValue("delete");*/
            }
        }

        //Delete
        deleteById(mAlarm.getId());
    }
}
