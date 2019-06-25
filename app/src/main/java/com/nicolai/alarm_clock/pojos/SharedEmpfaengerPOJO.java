package com.nicolai.alarm_clock.pojos;

public class SharedEmpfaengerPOJO {

    private String status;
    private long date;

    public SharedEmpfaengerPOJO(){

    }

    public SharedEmpfaengerPOJO(String status, long date){
        this.status = status;
        this.date = date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }
}
