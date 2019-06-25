package com.nicolai.alarm_clock.pojos;

public class ShareContactsPOJO {

    private String ID;
    private String name;
    private boolean inApp;
    private boolean trueShared;

    public ShareContactsPOJO(){
        //Empty Constructor
    }

    public ShareContactsPOJO(String ID, String name, boolean inApp, boolean trueShared) {
        this.ID = ID;
        this.name = name;
        this.inApp = inApp;
        this.trueShared = trueShared;
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isInApp() {
        return inApp;
    }

    public void setInApp(boolean inApp) {
        this.inApp = inApp;
    }

    public boolean isTrueShared() {
        return trueShared;
    }

    public void setTrueShared(boolean trueShared) {
        this.trueShared = trueShared;
    }
}
