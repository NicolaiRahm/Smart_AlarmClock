package com.nicolai.alarm_clock.pojos;

public class State {
    private boolean RUNNING, PAUSED, FINISHED, IDLE;

    public State(){
        IDLE = true;
    }

    public State(boolean running, boolean paused, boolean finished){
        RUNNING = running;
        PAUSED = paused;
        FINISHED = finished;
    }

    public boolean isRunning() {
        return RUNNING;
    }

    public void setRunning() {
        RUNNING = true;
        PAUSED = false;
        FINISHED = false;
        IDLE = false;
    }

    public boolean isPaused() {
        return PAUSED;
    }

    public void setPaused() {
        PAUSED = true;
        RUNNING = false;
    }

    public boolean isFinished() {
        return FINISHED;
    }

    public void setFinished() {
        RUNNING = false;
        FINISHED = true;
    }

    public boolean isIdle() {
        return IDLE;
    }

    public void setIdle() {
        IDLE = true;
        RUNNING = false;
        PAUSED = false;
        FINISHED = false;
    }
}
