package com.nicolai.alarm_clock.util;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.preference.PreferenceManager;

import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nicolai.alarm_clock.AppExecutor;
import com.nicolai.alarm_clock.MainActivity;
import com.nicolai.alarm_clock.R;
import com.nicolai.alarm_clock.WakeUp;
import com.nicolai.alarm_clock.pojos.FB;
import com.nicolai.alarm_clock.pojos.WeckerPOJO;
import com.nicolai.alarm_clock.receiver_service.AlarmIntervallReceiver;
import com.nicolai.alarm_clock.receiver_service.Alarm_Receiver;
import com.nicolai.alarm_clock.receiver_service.OversleptReceiver;
import com.nicolai.alarm_clock.receiver_service.upload;
import com.nicolai.alarm_clock.room_database.AlarmRepository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import androidx.core.app.JobIntentService;
import androidx.core.content.ContextCompat;

import static android.content.Context.AUDIO_SERVICE;
import static com.nicolai.alarm_clock.receiver_service.serviceUserProfile.JOB_ID;

public class AlarmUtil {

    public static String OVERSLEPT = "overslept";
    public static String REPEAT = "repeat";
    public static String ALARM_COUNT = "alarmCount";
    public static long FIVE_MIN_SNOOZE = 300000;
    public static long TEN_MIN_SNOOZE = 600000;

    public static void setAlarm(Context context, WeckerPOJO alarmClock){
        //TODO noch weg
        AlarmRepository mRepository = new AlarmRepository(context);

        int hour = alarmClock.getHour();
        int minute = alarmClock.getMinute();
        String days = alarmClock.getDays();
        boolean onOff = alarmClock.isOn_off();

        if(!days.equals("xxxxxxxxxxxxxx")){
            //Initialisierung der Kalender
            Calendar calendar = Calendar.getInstance();

            //Initialisierung AlarmManager
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            //update shared ++ Nur wenn vorher nich auch schon an war Database updaten --> z.B. wenn trustedBudddy alarm is updatet
            if(!onOff){
                alarmClock.setOn_off(true);
                mRepository.update(alarmClock);
            }

            int current_hour = calendar.get(Calendar.HOUR_OF_DAY);
            int current_minute = calendar.get(Calendar.MINUTE);
            int weekDayInt = calendar.get(Calendar.DAY_OF_WEEK);//Sonnatg = 1

            //Kalender mit eingegebener Stunde und Minute füllen
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);

            int distance = 0;

            //Wenn der nächste Termin nich mehr heute wäre
            if ((hour < current_hour) || (hour == current_hour && minute <= current_minute)) {
                weekDayInt++;

                distance++;
            }

            //Wenn Sonntag 1-2 -> 6  also weekdayInt = 8
            if(weekDayInt == 1){
                weekDayInt = 8;
            }

            //Solange der nächste Tag nicht geklickt ist distance erhöhen
            while (days.charAt((weekDayInt - 2) *2) == 'x'){
                if(weekDayInt == 8){
                    weekDayInt = 2;
                }else{
                    weekDayInt++;
                }

                distance++;
            }

            //Gestellten Wecker mit Stunden, Minuten, Sekund mit dem nächsten Tag verrechnen
            long actualTime = calendar.getTimeInMillis() + distance * 24*60*60*1000;
            calendar.setTimeInMillis(actualTime);

            //Initialisierung Intent fuer die Alarm_Receiver Klasse
            Intent alarmIntent = new Intent(context, Alarm_Receiver.class);
            alarmIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
            alarmIntent.putExtra("ID", alarmClock.getId());
            //FROM DESK_CLOCK CODE: better use service because of briding fail due to memory between receiver and service/activity
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, alarmClock.getId(), alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            //Den AlarmManager setzen. Pending_intent wird bis zu der Zeit zurückgehalten.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent i2 = new Intent(context, MainActivity.class);
                PendingIntent pi2 = PendingIntent.getActivity(context, alarmClock.getId(), i2, PendingIntent.FLAG_UPDATE_CURRENT);

                AlarmManager.AlarmClockInfo ac = new AlarmManager.AlarmClockInfo(calendar.getTimeInMillis(), pi2);
                alarmManager.setAlarmClock(ac, pendingIntent); //Holt handy aus doze kurz vor alarm
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);//TODO für api <23 auch Doze umgehen
            }

        }else if(onOff){
            //Wenn der an is jez aber keine Tage mehr hat ausmachen
            alarmClock.setOn_off(false);
            mRepository.update(alarmClock);
            unsetAlarm(context, alarmClock, true);
        }
    }
    public static void unsetAlarm(Context context, WeckerPOJO alarmClock, boolean updateRoom){
        if(updateRoom){
            AlarmRepository mRepository = new AlarmRepository(context);

            alarmClock.setOn_off(false);
            mRepository.update(alarmClock);
        }

        Intent alarmIntent = new Intent(context, Alarm_Receiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, alarmClock.getId(), alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        //cancel the alarm
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
    }

    public static void setRepeating(Context context, int id, long time){
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();

        calendar.setTimeInMillis(calendar.getTimeInMillis() + time);

        Intent alarmIntent = new Intent(context, AlarmIntervallReceiver.class);
        alarmIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, id+100, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent i2 = new Intent(context, MainActivity.class);
            PendingIntent pi2 = PendingIntent.getActivity(context, id+100, i2, PendingIntent.FLAG_UPDATE_CURRENT);

            AlarmManager.AlarmClockInfo ac = new AlarmManager.AlarmClockInfo(calendar.getTimeInMillis(), pi2);
            alarmManager.setAlarmClock(ac, pendingIntent);
        }else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }
    public static void cancelRepeating(Context context, int id){
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent alarmIntent = new Intent(context, AlarmIntervallReceiver.class);
        PendingIntent pending_intent = PendingIntent.getBroadcast(context, id+100, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.cancel(pending_intent);
        pending_intent.cancel();
    }

    public static void setOverslept(Context context, int id, long time){
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(calendar.getTimeInMillis() + time);

        Intent alarmIntent = new Intent(context, OversleptReceiver.class);
        alarmIntent.setAction(OVERSLEPT);
        alarmIntent.putExtra("ID", id);
        alarmIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, id+200, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }
    public static void cancelOverslept(Context context, int id){
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent alarmIntent = new Intent(context, OversleptReceiver.class);
        PendingIntent pending_intent = PendingIntent.getBroadcast(context, id+200, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.cancel(pending_intent);
        pending_intent.cancel();
    }

    public static void saveAlarm(final Context context, WeckerPOJO alarm, boolean neu){
        //TODO noch weg
        AlarmRepository mRepository = new AlarmRepository(context);

        int id = alarm.getId();

        //Shared updaten
        final boolean alarmS = !alarm.getShared_alarm_con().equals("{}");
        final boolean messageS = !alarm.getShared_message_con().equals("{}");
        final boolean smsS = !alarm.getShared_sms_con().equals("{}");

        if(alarmS || messageS || smsS){
            if(alarmS && (messageS || smsS)){
                alarm.setShared('t');
            }else if(alarmS){
                alarm.setShared('v');
            }else {
                alarm.setShared('m');
            }

            //Teilt nicht mehr ist aber noch so gekennzeichnet
        }else {
            char shared = alarm.getShared();

            if(shared == 't' || shared == 'm' || shared == 'v'){
                alarm.setShared('l');
            }
        }

        if(neu){
            id = (int) mRepository.insert(alarm);
        }else{
            mRepository.update(alarm);
        }

        //Alarm setzen wenn er auch für mich ist
        if(alarm.isFor_me()){
            alarm.setId(id);
            AlarmUtil.setAlarm(context, alarm);
        }

        //Wenn nicht für mich aber mit keinem mehr geteilt löschen
        if(!alarmS && !alarm.isFor_me()){
            alarm.setFor_me(true);
            alarm.setOn_off(false);
            //Falls neu
            alarm.setId(id);

            mRepository.update(alarm);
        }

        //Service zum Teilen starten
        if(alarmS || messageS){
            Intent uploadI = new Intent();
            uploadI.putExtra("iD", id);
            //upload.enqueueWork(TimeSeter.this, uploadI);
            JobIntentService.enqueueWork(context, upload.class, JOB_ID, uploadI);
        }
    }

    public static int minutesLeft(int hour, int minute){
        //Distance
        //Symulate the circle with 7 steps 0-12
        Calendar calendar = Calendar.getInstance();

        int hourDay = calendar.get(Calendar.HOUR_OF_DAY);
        int minuteDay = calendar.get(Calendar.MINUTE);

        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.GERMAN);
        String weekDay = dayFormat.format(calendar.getTime());
        int weekDayInt = DaysUtil.intForDay(weekDay);
        int realDistance = 0;

        while(weekDayInt != 4){
            weekDayInt = weekDayInt + 1;
            realDistance++;

            if(weekDayInt > 6){
                weekDayInt = 0;
            }
        }

        return realDistance * 24 * 60 - (hourDay - hour) * 60 - (minuteDay - minute);
    }
    public static void minutesLeft2(int hour, int minute, String days, TaskCompletionSource<Integer> source){
        AppExecutor.getInstance().diskI0().execute(new Runnable() {
            @Override
            public void run() {
                //Berechnung der verbleibenden Zeit
                Calendar calendar = Calendar.getInstance();
                final int hourDay = calendar.get(Calendar.HOUR_OF_DAY);
                final int minuteDay = calendar.get(Calendar.MINUTE);
                SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.GERMAN);
                final String weekDay = dayFormat.format(calendar.getTime());

                int distance = 0;
                int weekDayInt = DaysUtil.intForDay(weekDay);
                int herausfordererNextDay = DaysUtil.nextDay(weekDay, days);

                if (weekDayInt == herausfordererNextDay && ((hour < hourDay) || (hour == hourDay && minute <= minuteDay))) {
                    //Gibts noch n anderen Tag?!
                    int xCount = 0;
                    for (int i = 0; i < 14; i++) {

                        if (days.charAt(i) == 'x') {
                            xCount++;
                        }
                    }

                    if (xCount == 12) {
                        distance = 7;
                    } else {
                        //Symulate the circle with 7 steps 0-12
                        String withoutToday = days;
                        StringBuilder cut = new StringBuilder(withoutToday);
                        cut.setCharAt(herausfordererNextDay * 2, 'x');
                        cut.setCharAt(herausfordererNextDay * 2 + 1, 'x');
                        withoutToday = cut.toString();
                        herausfordererNextDay = DaysUtil.nextDay(weekDay, withoutToday);

                        while (weekDayInt != herausfordererNextDay) {
                            weekDayInt++;
                            distance++;

                            if (weekDayInt > 6) {
                                weekDayInt = 0;
                            }
                        }
                    }
                } else {
                    //Symulate the circle with 7 steps 0-12
                    while (weekDayInt != herausfordererNextDay) {
                        weekDayInt++;
                        distance++;

                        if (weekDayInt > 6) {
                            weekDayInt = 0;
                        }
                    }
                }


                source.setResult(distance * 24 * 60 - (hourDay - hour) * 60 - (minuteDay - minute));
            }
        });
    }

    public static String defaultDay(int hour, int minute){
        Calendar calendar = Calendar.getInstance();

        int current_hour = calendar.get(Calendar.HOUR_OF_DAY);
        int current_minute = calendar.get(Calendar.MINUTE);

        //day als Tage des Jahres
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        //calendar.set(Calendar.DAY_OF_MONTH, day);//damit Kalendartag wenn erhöht zurück gesetzt wird
        if ((hour < current_hour) || (hour == current_hour && minute < current_minute)) {
            day = day + 1;
            calendar.set(Calendar.DAY_OF_MONTH, day);
        }

        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.GERMAN);
        return dayFormat.format(calendar.getTime());
    }

    public static void killDay(Context context, WeckerPOJO klingelnderWecker, TaskCompletionSource<Boolean> source){
        AppExecutor.getInstance().diskI0().execute(() -> {
            AlarmRepository mRepository = new AlarmRepository(context);

            if (!klingelnderWecker.isWeekly_repeat()){
                //Initialisierung der Kalender zum löschen von dem heutigen Tag
                Calendar calendar = Calendar.getInstance();
                String weekDay;
                SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.GERMAN);
                weekDay = dayFormat.format(calendar.getTime());

                String days = klingelnderWecker.getDays();
                StringBuilder day = new StringBuilder(days);

                switch (weekDay){
                    case "Montag":
                        day.setCharAt(0, 'x');
                        day.setCharAt(1, 'x');
                        days = day.toString();
                        break;
                    case  "Dienstag":
                        day.setCharAt(2, 'x');
                        day.setCharAt(3, 'x');
                        days = day.toString();
                        break;
                    case "Mittwoch":
                        day.setCharAt(4, 'x');
                        day.setCharAt(5, 'x');
                        days = day.toString();
                        break;
                    case "Donnerstag":
                        day.setCharAt(6, 'x');
                        day.setCharAt(7, 'x');
                        days = day.toString();
                        break;
                    case "Freitag":
                        day.setCharAt(8, 'x');
                        day.setCharAt(9, 'x');
                        days = day.toString();
                        break;
                    case "Samstag":
                        day.setCharAt(10, 'x');
                        day.setCharAt(11, 'x');
                        days = day.toString();
                        break;
                    case "Sonntag":
                        day.setCharAt(12, 'x');
                        day.setCharAt(13, 'x');
                        days = day.toString();
                        break;
                }

                //Keine Tage mehr übrig
                if (days.equals("xxxxxxxxxxxxxx")) {
                    klingelnderWecker.setOn_off(false);
                    klingelnderWecker.setDays(days);

                    mRepository.update(klingelnderWecker);
                    source.setResult(true);

                    //Neu auf nächsten Tag stellen
                } else {
                    klingelnderWecker.setDays(days);
                    mRepository.update(klingelnderWecker);

                    source.setResult(false);
                    AlarmUtil.setAlarm(context, klingelnderWecker);
                }

                //Wird wöchentlich wiederholt
            }else{
                AlarmUtil.setAlarm(context, klingelnderWecker);
                source.setResult(false);
            }
        });
    }

    public static void setAfterBoot(Context context){

        AlarmRepository mRepository = new AlarmRepository(context);
        List<WeckerPOJO> list = mRepository.getBootList();

        for(WeckerPOJO alarm : list){
            if(alarm.isOn_off()) setAlarm(context, alarm);
        }
    }

    /*
    * Shared
    */

    public static void insertSharedAlarm(Context context, DataSnapshot dataSnapshot, final char shared){
        final String sender_id = dataSnapshot.child(FB.SENDER_ID).getValue().toString();
        final String wecker_id = dataSnapshot.child(FB.SENDER_WECKER_ID).getValue().toString();

        //Alarm Details
        DatabaseReference detailsNew = FirebaseDatabase.getInstance().getReference();
        detailsNew.child(FB.ALARMS).child(sender_id).child(FB.BY_ME).child(wecker_id).child(FB.DETAILS)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(final DataSnapshot dataSnapshot2) {
                        //Sharedalarmdaten
                        long hour = (long) dataSnapshot2.child(FB.HOUR).getValue();
                        long minute = (long) dataSnapshot2.child(FB.MINUTE).getValue();
                        long intervall = (long) dataSnapshot2.child(FB.INTERVALL).getValue();
                        long anzahl = (long) dataSnapshot2.child(FB.ANZAHL).getValue();
                        String days = dataSnapshot2.child(FB.DAYS).getValue().toString();
                        boolean repeat = (boolean) dataSnapshot2.child(FB.REPEAT).getValue();
                        String name = dataSnapshot2.child(FB.NAME_ALARM).getValue().toString();

                        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

                        //Standard-/Defaulteinstellungen
                        //Default volume
                        AudioManager audioManager = (AudioManager) context.getSystemService(AUDIO_SERVICE);
                        int halfVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING) / 2;
                        //Default Ringtone
                        String defaultTone = SoundUtil.defaultUri(context, true);

                        int volume = mSharedPreferences.getInt("volume", halfVolume);
                        String alarmSound = mSharedPreferences.getString("sound", defaultTone);
                        boolean voiceControl = mSharedPreferences.getBoolean("voiceControl", false);
                        int duration = mSharedPreferences.getInt("duration", 30);

                        int secondsBis = mSharedPreferences.getInt(context.getString(R.string.sharedTimeBisVerschlafen), 60);

                        //New Wecker
                        final WeckerPOJO newWecker = new WeckerPOJO(name, days, repeat, (int)hour, (int)minute,
                                (int)anzahl, intervall, duration, volume, alarmSound, voiceControl,
                                shared, "{}", "{}", "{}", true,
                                sender_id, wecker_id, "", "", shared == 'b', "", "", secondsBis);

                        //User name and image
                        addUserDetails(context, sender_id, wecker_id, newWecker);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    private static void addUserDetails(Context context, String sender_id, String wecker_id, WeckerPOJO newWecker){
        AlarmRepository mRepository = new AlarmRepository(context);

        //User Details
        DatabaseReference user_details = FirebaseDatabase.getInstance().getReference()
                .child(FB.USERS).child(sender_id);

        user_details.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChild(FB.USERNAME)){
                    String vonXY = dataSnapshot.child(FB.USERNAME).getValue().toString();
                    String number = dataSnapshot.child(FB.MOBILE_NUMBER).getValue().toString();
                    String thumb_image = dataSnapshot.child(FB.THUMB_IMG).getValue().toString();

                    if(!number.isEmpty() && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED){
                        String contactName = ContactUtil.getContactDisplayNameByNumber(context, number);
                        vonXY = contactName.equals("?") ? vonXY : contactName;
                    }

                    //New Alarm direkt an adapter
                    newWecker.setVon_xy(vonXY);
                    newWecker.setThumb_img(thumb_image);

                    //Neuen Wecker einfügen
                    final int sqLite_id = (int) mRepository.insert(newWecker);

                    if(newWecker.isOn_off()) AlarmUtil.setAlarm(context, newWecker);

                    //Cahnge status to "downloaded" + (my) weckerID and in ByMe of the sender
                    mRepository.downloaded(sender_id, wecker_id, sqLite_id, true);

                }else{
                    //New Alarm direkt an adapter
                    newWecker.setVon_xy(context.getResources().getString(R.string.user_deleted));
                    newWecker.setThumb_img("");

                    final int sqLite_id = (int) mRepository.insert(newWecker);

                    mRepository.downloaded(sender_id, wecker_id, sqLite_id, false);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public static void updateSharedAlarm(Context context, DataSnapshot dataSnapshot, final char shared){
        final String sender_id = dataSnapshot.child(FB.SENDER_ID).getValue().toString();
        final String wecker_id = dataSnapshot.child(FB.SENDER_WECKER_ID).getValue().toString();
        String myWecker_id = dataSnapshot.child(FB.MY_WECKER_ID).getValue().toString();

        //myWecker_id to int
        final int myWecker_int = Integer.parseInt(myWecker_id);

        AlarmRepository mRepository = new AlarmRepository(context);

        //Alarm Details
        DatabaseReference detailsUpdatedAlarm = FirebaseDatabase.getInstance().getReference()
                .child(FB.ALARMS).child(sender_id).child(FB.BY_ME).child(wecker_id).child(FB.DETAILS);
        //TODO braucht man hier keepSynced?!
        detailsUpdatedAlarm.keepSynced(true);

        detailsUpdatedAlarm.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){

                    WeckerPOJO updatedWecker = mRepository.getById(myWecker_int);
                    updatedWecker.setHour((int) dataSnapshot.child(FB.HOUR).getValue());
                    updatedWecker.setMinute((int) dataSnapshot.child(FB.MINUTE).getValue());
                    updatedWecker.setIntervall((long) dataSnapshot.child(FB.INTERVALL).getValue());
                    updatedWecker.setAnzahl((int) dataSnapshot.child(FB.ANZAHL).getValue());
                    updatedWecker.setDays(dataSnapshot.child(FB.DAYS).getValue().toString());
                    updatedWecker.setWeekly_repeat((boolean) dataSnapshot.child(FB.REPEAT).getValue());
                    updatedWecker.setName(dataSnapshot.child(FB.NAME_ALARM).getValue().toString());
                    updatedWecker.setShared(shared);
                    if(shared != 'b') updatedWecker.setOn_off(false);

                    mRepository.update(updatedWecker);

                    //Change status in ForMe and + timestamp for sender
                    mRepository.updateDownloaded(sender_id, wecker_id);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        //detailsUpdatedAlarm.addValueEventListener(vEL);
    }
}
