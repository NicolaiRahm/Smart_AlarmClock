package com.nicolai.alarm_clock.receiver_service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.nicolai.alarm_clock.receiver_service.Alarm_Receiver;
import com.nicolai.alarm_clock.receiver_service.upload;
import com.nicolai.alarm_clock.room_database.AlarmRepository;
import com.nicolai.alarm_clock.room_database.WeckerDatabase;
import com.nicolai.alarm_clock.util.AlarmUtil;

import java.util.Calendar;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

/**
 * Nicolai Rahm
 * 25.07.2018
 */
public class DeleteAllAlarms extends JobIntentService {

    private String current_user_id;
    private List<String> senderIdList, senderWeckerIdList;
    private List<Long> ids;

    /**
     * Unique job ID for this service.
     */
    static final int JOB_ID = 1001;

    /**
     * Convenience method for enqueuing work in to this service.
     */
    static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, upload.class, JOB_ID, work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        current_user_id = FirebaseAuth.getInstance().getUid();
        WeckerDatabase database = WeckerDatabase.getInstance(getApplicationContext());
        ids = database.weckerDao().idList();
        List<Character> sharedList = database.weckerDao().sharedList();
        senderIdList = database.weckerDao().senderIdList();
        senderWeckerIdList = database.weckerDao().senderWeckerIdList();
        List<Boolean> onOffList = database.weckerDao().onOffList();

        //Durch alle alarme loopen. Wenn an aus schalten und wenn Alarm von mir / für mich oder Message von mir Firebase entsprechen updaten.
        for(long id : ids){
            //Pending intent löschen
            if(onOffList.get(ids.indexOf(id))){
                AlarmRepository mRepository = new AlarmRepository(getApplication());
                AlarmUtil.unsetAlarm(getApplicationContext(), mRepository.getById((int) id), false);
            }

            //Wenn shared
            final char shared = sharedList.get(ids.indexOf(id));

            if(shared != 'l'){
                updateFirebase(id, shared);
            }
        }

        //Alle aus database löschen
        database.weckerDao().deleteAll();
    }


    //Update Firebase
    public void updateFirebase(long id, char shared){
        //Alarm von mir und / oder Message von mir
        if(shared == 't' || shared == 'v' || shared == 'm'){
            if(shared == 'v' || shared == 't'){
                //Firebase alarm löschen
                DatabaseReference deleteAlarm = FirebaseDatabase.getInstance().getReference()
                        .child("alarms").child(current_user_id).child("ByMe").child("" + id).child("details");
                deleteAlarm.removeValue();
            }

            if(shared == 'm' || shared == 't'){
                //Firebase message löschen
                DatabaseReference deleteMessage = FirebaseDatabase.getInstance().getReference()
                        .child("messages").child(current_user_id).child("ByMe").child("" + id).child("details");
                deleteMessage.removeValue();
                        /*DatabaseReference deleteMessage = FirebaseDatabase.getInstance().getReference()
                                .child("messages").child(current_user_id).child("ByMe").child("" + id).child("details").child("status");
                        deleteMessage.setValue("delete");*/
            }

            //Alarm für mich
        }else if (shared == 's' || shared == 'u' || shared == 'b'){
            String sender_id = senderIdList.get(ids.indexOf(id));
            String sender_wecker_id = senderWeckerIdList.get(ids.indexOf(id));

            //In ForMe rest in cloud damit nicht byMe beschrieben wird wenn der schon den Alarm gelöscht hat
            DatabaseReference deleteAlarm = FirebaseDatabase.getInstance().getReference()
                    .child("alarms").child(current_user_id).child("ForMe").child(sender_id + sender_wecker_id).child("deleteDate");
            long timeStamp = Calendar.getInstance().getTimeInMillis();
            deleteAlarm.setValue(timeStamp);

            //Alarm für mich von Sender gelöscht.
        }else if(shared == 'd'){
            String sender_id = senderIdList.get(ids.indexOf(id));
            String sender_wecker_id = senderWeckerIdList.get(ids.indexOf(id));

            //In ForMe
            DatabaseReference deleteAlarm = FirebaseDatabase.getInstance().getReference()
                    .child("alarms").child(current_user_id).child("ForMe").child(sender_id + sender_wecker_id);
            deleteAlarm.removeValue();
        }
    }
}
