package com.nicolai.alarm_clock.receiver_service;

import android.content.Context;
import android.content.Intent;

import com.nicolai.alarm_clock.WakeUp;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

/**
 *04.04.2018
 */
public class JobServiceStartAlarm extends JobIntentService {

    /**
     * Unique job ID for this service.
     */
    static final int JOB_ID = 999;

    /**
     * Convenience method for enqueuing work in to this service.
     */
    static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, JobServiceStartAlarm.class, JOB_ID, work);
    }


    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        int weckerID = intent.getIntExtra("ID", 0);

        Intent wakeUp_intent = new Intent(getApplicationContext(), WakeUp.class);
        wakeUp_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        wakeUp_intent.putExtra("ID", weckerID);
        getApplicationContext().startActivity(wakeUp_intent);
    }
}

