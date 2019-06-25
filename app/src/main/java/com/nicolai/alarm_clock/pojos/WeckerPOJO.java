package com.nicolai.alarm_clock.pojos;

/*
* Nicolai Rahm
* 20.07.2018
*POJO -> plain old Java object
*/

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity( tableName = "wecker")
public class WeckerPOJO {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private String days;
    private boolean weekly_repeat;
    private int hour;
    private int minute;
    private int anzahl;
    private long intervall;
    private int duration;
    private int volume;
    private String alarm_sound;
    private boolean speech_control;

    private char shared;
    private String shared_alarm_con;
    private String shared_sms_con;
    private String shared_message_con;
    private boolean for_me;

    private String sender_id;
    private String sender_wecker_id;
    private String von_xy;
    private String thumb_img;

    private boolean on_off;

    private String sms_verschlafen;
    private String sms_aufgestanden;

    private int seconds_bis_verschlafen;

    //Constructors
    @Ignore
    //For New Wecker object
    public WeckerPOJO(String name, String days, boolean weekly_repeat, int hour, int minute, int anzahl, long intervall,
                      int duration, int volume, String alarm_sound, boolean speech_control, char shared, String shared_alarm_con,
                      String shared_sms_con, String shared_message_con, boolean for_me, String sender_id, String sender_wecker_id,
                      String von_xy, String thumb_img, boolean on_off, String sms_verschlafen, String sms_aufgestanden,
                      int seconds_bis_verschlafen) {
        this.name = name;
        this.days = days;
        this.weekly_repeat = weekly_repeat;
        this.hour = hour;
        this.minute = minute;
        this.anzahl = anzahl;
        this.intervall = intervall;
        this.duration = duration;
        this.volume = volume;
        this.alarm_sound = alarm_sound;
        this.speech_control = speech_control;
        this.shared = shared;
        this.shared_alarm_con = shared_alarm_con;
        this.shared_sms_con = shared_sms_con;
        this.shared_message_con = shared_message_con;
        this.for_me = for_me;
        this.sender_id = sender_id;
        this.sender_wecker_id = sender_wecker_id;
        this.von_xy = von_xy;
        this.thumb_img = thumb_img;
        this.on_off = on_off;
        this.sms_verschlafen = sms_verschlafen;
        this.sms_aufgestanden = sms_aufgestanden;
        this.seconds_bis_verschlafen = seconds_bis_verschlafen;
    }

    @Ignore
    //For delete All in recyclerview
    public WeckerPOJO(int id, String name) {
        this.id = id;
        this.name = name;
    }

    //For write to Room
    public WeckerPOJO(int id, String name, String days, boolean weekly_repeat, int hour, int minute, int anzahl,
                      long intervall, int duration, int volume, String alarm_sound, boolean speech_control, char shared,
                      String shared_alarm_con, String shared_sms_con, String shared_message_con, boolean for_me,
                      String sender_id, String sender_wecker_id, String von_xy, String thumb_img, boolean on_off,
                      String sms_verschlafen, String sms_aufgestanden, int seconds_bis_verschlafen) {
        this.id = id;
        this.name = name;
        this.days = days;
        this.weekly_repeat = weekly_repeat;
        this.hour = hour;
        this.minute = minute;
        this.anzahl = anzahl;
        this.intervall = intervall;
        this.duration = duration;
        this.volume = volume;
        this.alarm_sound = alarm_sound;
        this.speech_control = speech_control;
        this.shared = shared;
        this.shared_alarm_con = shared_alarm_con;
        this.shared_sms_con = shared_sms_con;
        this.shared_message_con = shared_message_con;
        this.for_me = for_me;
        this.sender_id = sender_id;
        this.sender_wecker_id = sender_wecker_id;
        this.von_xy = von_xy;
        this.thumb_img = thumb_img;
        this.on_off = on_off;
        this.sms_verschlafen = sms_verschlafen;
        this.sms_aufgestanden = sms_aufgestanden;
        this.seconds_bis_verschlafen = seconds_bis_verschlafen;
    }

    //Getter and Setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDays() {
        return days;
    }

    public void setDays(String days) {
        this.days = days;
    }

    public boolean isWeekly_repeat() {
        return weekly_repeat;
    }

    public void setWeekly_repeat(boolean weekly_repeat) {
        this.weekly_repeat = weekly_repeat;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public int getAnzahl() {
        return anzahl;
    }

    public void setAnzahl(int anzahl) {
        this.anzahl = anzahl;
    }

    public long getIntervall() {
        return intervall;
    }

    public void setIntervall(long intervall) {
        this.intervall = intervall;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public String getAlarm_sound() {
        return alarm_sound;
    }

    public void setAlarm_sound(String alarm_sound) {
        this.alarm_sound = alarm_sound;
    }

    public boolean isSpeech_control() {
        return speech_control;
    }

    public void setSpeech_control(boolean speech_control) {
        this.speech_control = speech_control;
    }

    public char getShared() {
        return shared;
    }

    public void setShared(char shared) {
        this.shared = shared;
    }

    public String getShared_alarm_con() {
        return shared_alarm_con;
    }

    public void setShared_alarm_con(String shared_alarm_con) {
        this.shared_alarm_con = shared_alarm_con;
    }

    public String getShared_sms_con() {
        return shared_sms_con;
    }

    public void setShared_sms_con(String shared_sms_con) {
        this.shared_sms_con = shared_sms_con;
    }

    public String getShared_message_con() {
        return shared_message_con;
    }

    public void setShared_message_con(String shared_message_con) {
        this.shared_message_con = shared_message_con;
    }

    public boolean isFor_me() {
        return for_me;
    }

    public void setFor_me(boolean for_me) {
        this.for_me = for_me;
    }

    public String getSender_id() {
        return sender_id;
    }

    public void setSender_id(String sender_id) {
        this.sender_id = sender_id;
    }

    public String getSender_wecker_id() {
        return sender_wecker_id;
    }

    public void setSender_wecker_id(String sender_wecker_id) {
        this.sender_wecker_id = sender_wecker_id;
    }

    public String getVon_xy() {
        return von_xy;
    }

    public void setVon_xy(String von_xy) {
        this.von_xy = von_xy;
    }

    public String getThumb_img() {
        return thumb_img;
    }

    public void setThumb_img(String thumb_img) {
        this.thumb_img = thumb_img;
    }

    public boolean isOn_off() {
        return on_off;
    }

    public void setOn_off(boolean on_off) {
        this.on_off = on_off;
    }

    public String getSms_verschlafen() {
        return sms_verschlafen;
    }

    public void setSms_verschlafen(String sms_verschlafen) {
        this.sms_verschlafen = sms_verschlafen;
    }

    public String getSms_aufgestanden() {
        return sms_aufgestanden;
    }

    public void setSms_aufgestanden(String sms_aufgestanden) {
        this.sms_aufgestanden = sms_aufgestanden;
    }

    public int getSeconds_bis_verschlafen() {
        return seconds_bis_verschlafen;
    }

    public void setSeconds_bis_verschlafen(int seconds_bis_verschlafen) {
        this.seconds_bis_verschlafen = seconds_bis_verschlafen;
    }

    //equals method for RecyclerView ListAdapter

    @Override
    public boolean equals(Object obj) {
        WeckerPOJO newWecker = (WeckerPOJO) obj;

        if(this.id != newWecker.id){
            return false;
        }else if(this.on_off != newWecker.on_off){
            return false;
        }else if (!this.name.equals(newWecker.name)){
            return false;
        }else if (!this.days.equals(newWecker.days)){
            return false;
        }else if (this.weekly_repeat != newWecker.weekly_repeat){
            return false;
        }else if (this.hour != newWecker.hour){
            return false;
        }else if (this.minute != newWecker.minute){
            return false;
        }else if (this.shared != newWecker.shared){
            return false;
        }else if (!this.shared_message_con.equals(newWecker.shared_message_con)){
            return false;
        }

        return this.for_me == newWecker.for_me;

        //this.von_xy = von_xy;
        //this.thumb_img = thumb_img;
    }

}
