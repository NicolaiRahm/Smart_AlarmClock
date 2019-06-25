package com.nicolai.alarm_clock.pojos;

public class FirebaseMessagePOJO {
    private String sender_id;
    private String wecker_id;
    private String date;
    private String status;

    public FirebaseMessagePOJO(){
        //Empty Constructor
    }

    public FirebaseMessagePOJO(String sender_id, String wecker_id, String status, String date){
        this.sender_id = sender_id;
        this.wecker_id = wecker_id;
        this.status = status;
        this.date = date;
    }

    public String getSender_id() {
        return sender_id;
    }

    public void setSender_id(String sender_id) {
        this.sender_id = sender_id;
    }

    public String getWecker_id() {
        return wecker_id;
    }

    public void setWecker_id(String wecker_id) {
        this.wecker_id = wecker_id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
