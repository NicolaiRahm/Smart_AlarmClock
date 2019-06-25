package com.nicolai.alarm_clock.pojos;

public class SearchPOJO {
    private String userId;
    private String name;
    private String image;

    public SearchPOJO(){
        //Empty Constructor
    }

    public SearchPOJO(String userId, String name, String image) {
        this.userId = userId;
        this.name = name;
        this.image = image;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
