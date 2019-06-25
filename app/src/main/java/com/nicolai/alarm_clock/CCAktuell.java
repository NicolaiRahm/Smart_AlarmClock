package com.nicolai.alarm_clock;

public class CCAktuell {
    private String sender_id;
    private int wecker_id;

    public CCAktuell(String sender_id, int wecker_id) {
        this.sender_id = sender_id;
        this.wecker_id = wecker_id;
    }

    public String getSender_id() {
        return sender_id;
    }

    public void setSender_id(String sender_id) {
        this.sender_id = sender_id;
    }

    public int getWecker_id() {
        return wecker_id;
    }

    public void setWecker_id(int wecker_id) {
        this.wecker_id = wecker_id;
    }
}
