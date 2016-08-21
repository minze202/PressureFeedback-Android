package com.compressionfeedback.hci.pressurefeedback;


import android.app.Notification;

public class DataSingleton {

    private Notification notification;

    private boolean feedbackSent;


    private long responseTime=-1;


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

    public DataSingleton(Notification notification, String appPackage, boolean feedbackSent){
        this.notification=notification;
        this.appPackage=appPackage;
        this.feedbackSent=feedbackSent;

    }
    public boolean isFeedbackSent() {
        return feedbackSent;
    }

    public void setResponseTime(long responseTime) {
        this.responseTime = responseTime/1000;
    }
}
