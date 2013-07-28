package com.opentok.android.demo;

import java.util.List;

public interface NetApiCallback {
    public void queueCallback(QueueData queueData);

    public void gamesCallback(String someHash);

    public void leaderboardCallback(List<NetApi.User> users);
}
