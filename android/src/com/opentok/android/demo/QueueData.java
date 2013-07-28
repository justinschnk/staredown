package com.opentok.android.demo;

public class QueueData {
    private String mStatus;
    private String mSessionId;
    private String mToken;
    private String mFirebase;

    public QueueData(String mStatus, String mToken, String mSessionId, String mFirebase) {
        this.mStatus = mStatus;
        this.mToken = mToken;
        this.mSessionId = mSessionId;
        this.mFirebase = mFirebase;
    }

    public String getmStatus() {
        return mStatus;
    }

    public String getmSessionId() {
        return mSessionId;
    }

    public String getmToken() {
        return mToken;
    }

    public String getmFirebase() {
        return mFirebase;
    }

    @Override
    public String toString() {
        return "QueueData{" +
                "mStatus='" + mStatus + '\'' +
                ", mSessionId='" + mSessionId + '\'' +
                ", mToken='" + mToken + '\'' +
                ", mFirebase='" + mFirebase + '\'' +
                '}';
    }
}
