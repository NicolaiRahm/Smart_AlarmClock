package com.nicolai.alarm_clock.pojos;

public class FriendRequestPOJO {

    private String request_type;
    private String name;
    private String number;
    private String image;

    public FriendRequestPOJO(){
        //Empty Constructor
    }

    public FriendRequestPOJO(String request_type, String name, String number, String image) {
        this.request_type = request_type;
        this.name = name;
        this.number = number;
        this.image = image;
    }

    public FriendRequestPOJO(String request_type, String name, String image) {
        this.request_type = request_type;
        this.name = name;
        this.image = image;
    }

    public String getRequest_type() {
        return request_type;
    }

    public String getName() {
        return name;
    }

    public String getNumber() {
        return number;
    }

    public String getImage() {
        return image;
    }

    public void setRequest_type(String request_type) {
        this.request_type = request_type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
