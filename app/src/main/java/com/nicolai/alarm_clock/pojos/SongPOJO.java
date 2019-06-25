package com.nicolai.alarm_clock.pojos;

public class SongPOJO {
    private String path;
    private String title;

    public SongPOJO(String path, String title){
        this.path = path;
        this.title = title;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
