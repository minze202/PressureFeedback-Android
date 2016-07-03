package com.compressionfeedback.hci.pressurefeedback;


import android.app.Notification;

public class DataSingleton {

    private Notification notification;

    public void setReason(String reason) {
        this.reason = reason;
    }

    private long responseTime=-1;
    private boolean responded=false;
    private String reason="";

    public void setAppPackage(String appPackage) {
        this.appPackage = appPackage;
    }

    public String getAppPackage() {

        return appPackage;
    }

    private String appPackage="";

    public long getResponseTime() {
        return responseTime;
    }

    public Notification getNotification() {

        return notification;
    }

    public DataSingleton(Notification notification, String appPackage){
        this.notification=notification;
        this.appPackage=appPackage;

    }

    public void setResponseTime(long responseTime) {
        this.responseTime = responseTime;
    }

    public void setResponded(boolean responded) {
        this.responded = responded;
    }
}
