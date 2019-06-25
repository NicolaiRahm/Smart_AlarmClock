package com.nicolai.alarm_clock.pojos;

public class FirebaseAlarmPOJO {
    private String name;
    private int hour;
    private int minute;
    private long anzahl;
    private long intervall;
    private String days;
    private boolean repeat;
    private boolean onOff;
    private String status;
    private int weckerID;
    private boolean foreMe;
    private long date;

    public FirebaseAlarmPOJO(){
        // Default constructor required for calls to DataSnapshot.getValue(FirebaseAlarmPOJO.class)
    }

    public FirebaseAlarmPOJO(String name, int hour, int minute, long anzahl, long intervall, String days, boolean repeat,
                      boolean onOff, String status, int weckerID, boolean foreMe, long date) {
        this.name = name;
        this.hour = hour;
        this.minute = minute;
        this.anzahl = anzahl;
        this.intervall = intervall;
        this.days = days;
        this.repeat = repeat;
        this.onOff = onOff;
        this.status = status;
        this.weckerID = weckerID;
        this.foreMe = foreMe;
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public long getAnzahl() {
        return anzahl;
    }

    public void setAnzahl(long anzahl) {
        this.anzahl = anzahl;
    }

    public long getIntervall() {
        return intervall;
    }

    public void setIntervall(long intervall) {
        this.intervall = intervall;
    }

    public String getDays() {
        return days;
    }

    public void setDays(String days) {
        this.days = days;
    }

    public boolean isRepeat() {
        return repeat;
    }

    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
    }

    public boolean isOnOff() {
        return onOff;
    }

    public void setOnOff(boolean onOff) {
        this.onOff = onOff;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getWeckerID() {
        return weckerID;
    }

    public void setWeckerID(int weckerID) {
        this.weckerID = weckerID;
    }

    public boolean isForeMe() {
        return foreMe;
    }

    public void setForeMe(boolean foreMe) {
        this.foreMe = foreMe;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }
}

