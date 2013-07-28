package com.opentok.android.demo;

public interface NetApiCallback {
    public void queueCallback(QueueData queueData);
    public void gamesCallback(String someHash);
}
