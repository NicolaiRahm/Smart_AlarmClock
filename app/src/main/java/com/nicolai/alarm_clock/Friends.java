package com.nicolai.alarm_clock;

public class Friends {
    private String name;
    private boolean trustedByMe;

    public Friends(){
       //Empty Constructor
    }

    public Friends(String date, String name, boolean trustedByMe) {
        this.name = name;
        this.trustedByMe = trustedByMe;
    }

    public boolean isTrustedByMe() {
        return trustedByMe;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
