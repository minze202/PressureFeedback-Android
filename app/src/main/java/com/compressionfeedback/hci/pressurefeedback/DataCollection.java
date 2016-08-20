package com.compressionfeedback.hci.pressurefeedback;




import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class DataCollection extends Service{

    private HashMap<String,DataSingleton> dataForTheDay;
    private ArrayList<DataForOnePerson> completeDataSet;
    private ArrayList<DataForANotification>controlledStudyData;
    PowerManager.WakeLock wakeLock;
    private String userActions="";
    private long studyDuration=600000;
    private String currentUser;
    private android.os.Handler mHandler=new android.os.Handler();
    private boolean collectingData=false;
    private boolean labStudy=false;
    private long labStudyFinish=0;
    private boolean firstLabNotification=true;
    private File dir;
    private File fileActions;
    private SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private File fileNotification;
    private String CSV_HEADER_LAB_STUDY="Zeitstempel,Teilnehmer,NotificationId,Missed,AnsweredRight,DismissalTime,RightAnswer,ChosenAnswer,Feedback\n";
    private String CSV_HEADER_FIELD_STUDY="Teilnehmer,Timestamp,App,Event,Notificationid,Feedback,Pattern,Stärke,Reaktionszeit\n";


    @Override
    public void onCreate(){
        super.onCreate();
        completeDataSet= new ArrayList<>();
        dataForTheDay=new HashMap<>();
        controlledStudyData=new ArrayList<>();
    }


    private final IBinder mBinder = new LocalBinder();
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    public boolean isCollectingData() {
        return collectingData;
    }

    public void addAction(String action){
        if(collectingData &&!labStudy){
            Calendar cal=Calendar.getInstance();
            userActions=userActions+ "\n"+String.format("%1$tA %1$tb %1$td %1$tY at %1$tI:%1$tM %1$Tp", cal)+ " "+action;
            writeUserActionIntoFile("\n"+String.format("%1$tA %1$tb %1$td %1$tY at %1$tI:%1$tM %1$Tp", cal)+ " "+action);
        }
    }

    public void addNotificationData(String key, DataSingleton data){
        dataForTheDay.put(key,data);
    }

    public void startCollectingData(String user){
        currentUser=user;
        collectingData=true;
        labStudy=false;
        Log.i("startCollectingData: ", ""+collectingData);
    }

    public void startLabCollectingData(String user){
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakelockTag");
        wakeLock.acquire();
        currentUser=user;
        collectingData=true;
        labStudy=true;
        labStudyFinish=Calendar.getInstance().getTimeInMillis()+studyDuration;
        randomFeedbackRunnable.run();
        Intent stopIntent = new Intent(getString(R.string.testFilter));
        stopIntent.putExtra("mode","stopRandomFeedback");
        PendingIntent pIntentStop = PendingIntent.getBroadcast(this, 12, stopIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarm.setExact(AlarmManager.RTC_WAKEUP,labStudyFinish,pIntentStop);
    }

    Runnable randomFeedbackRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if(!firstLabNotification) {
                    if((System.currentTimeMillis()+30000)<labStudyFinish){
                        sendRandomFeedBack();
                    }
                }else{
                    firstLabNotification=false;
                }
            } finally {
                Random random=new Random();
                int randomNumber=random.nextInt(10000);
                mHandler.postDelayed(randomFeedbackRunnable, (40000+randomNumber));
            }
        }
    };

    public void stopSendRandomFeedback(){
        mHandler.removeCallbacks(randomFeedbackRunnable);
        Intent stopIntent = new Intent(getString(R.string.testFilter));
        stopIntent.putExtra("mode","stopRandomFeedback");
        sendBroadcast(stopIntent);
    }

    public void sendRandomFeedBack(){
        Random random=new Random();
        Intent intent = new Intent(getString(R.string.testFilter));
        intent.putExtra("mode","randomFeedback");
        long randomNumber = ((long)random.nextInt(10000)) + (long)100;
        PendingIntent pIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarm.setExact(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+randomNumber,pIntent);
    }

    public void clear(){
        if(collectingData && !labStudy){
            writeNotificationDataIntoFile();
            DataForOnePerson person=new DataForOnePerson(dataForTheDay,userActions,currentUser);
            completeDataSet.add(person);
            dataForTheDay.clear();
            userActions="";
        }
    }

    public void stopCollectingData(){
        clear();
        controlledStudyData.clear();
        collectingData=false;
        if(labStudy){
            wakeLock.release();
            labStudy=false;
            firstLabNotification=true;
            stopSendRandomFeedback();
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        clear();
    }

    public void writeUserActionIntoFile(String action){
        try {
            File sdCard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            dir = new File (sdCard.getPath() + "/logs/");
            if(!dir.exists()){
                dir.mkdirs();
            }
            fileActions = new File(dir, Calendar.getInstance().getDisplayName(Calendar.DAY_OF_WEEK,Calendar.LONG, Locale.getDefault())+"_userActions_"+currentUser+".csv");
            if(!fileActions.exists()){
                fileActions.createNewFile();
            }

            FileWriter fileWriter=new FileWriter(fileActions,true);
            fileWriter.write(System.getProperty("line.separator"));
            fileWriter.write(action);
            fileWriter.close();
        }
        catch (IOException e) {
            Log.e("writeUserActions", "File write failed: " + e.toString());
        }
    }

    public void writeNotificationDataIntoFile(){
        try {
            String data="";
            File sdCard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            dir = new File (sdCard.getPath() + "/logs/");
            if(!dir.exists()){
                dir.mkdirs();
            }
            fileNotification = new File(dir, Calendar.getInstance().getDisplayName(Calendar.DAY_OF_WEEK,Calendar.LONG, Locale.getDefault())+"_notificationData.csv");
            if(!fileNotification.exists()){
                fileNotification.createNewFile();
            }

            FileOutputStream f = new FileOutputStream(fileNotification);
            data="Anzahl Notifications: "+ dataForTheDay.size()+"\n\n";
            for(Map.Entry<String,DataSingleton> pair : getDataForTheDay().entrySet()){
                data=data+"Notification Package: " +pair.getValue().getAppPackage()+"\n"+"Dismissal Time: "+pair.getValue().getResponseTime()+"\n\n";
            }
            FileWriter fileWriter=new FileWriter(fileNotification,true);
            fileWriter.write(data);
            fileWriter.close();
        }
        catch (IOException e) {
            Log.e("writeNotification", "File write failed: " + e.toString());
        }
    }

    public void writeDownStudyResult(){
        try {
            String data="";
            File sdCard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);

            dir = new File (sdCard.getPath() + "/logs/");
            if(!dir.exists()){
                dir.mkdirs();
            }
            fileNotification = new File(dir, Calendar.getInstance().getDisplayName(Calendar.DAY_OF_WEEK,Calendar.LONG, Locale.getDefault())+"_notificationStudyData.csv");
            if(!fileNotification.exists()){
                fileNotification.createNewFile();
                FileWriter fileWriter=new FileWriter(fileNotification,true);
                fileWriter.write(CSV_HEADER_LAB_STUDY);
                fileWriter.close();
            }

            for(DataForANotification studyData: controlledStudyData){
                String mode= studyData.getFeedback();
                switch (mode) {
                    case "sound":
                        mode="Vibrationsfeedback(Handy)";
                        break;
                    case "vibration":
                        mode="Vibrationsfeedback(Arm)";
                        break;
                    case "compression":
                        mode="Kompressionsfeedback";
                        break;

                }
                data=data+studyData.getTimeStamp()+","+currentUser+ "," + studyData.getId() + "," +studyData.isMissed()+"," +studyData.isAnsweredRight()+","+ studyData.getResponseTime()+","
                        +studyData.getRightAnswer()+","+ studyData.getChosenAnswer()+","+mode +"\n";
            }
            FileWriter fileWriter=new FileWriter(fileNotification,true);
            fileWriter.write(data);
            fileWriter.close();
        }
        catch (IOException e) {
            Log.e("writeNotification", "File write failed: " + e.toString());
        }
    }

    public class LocalBinder extends Binder {
        DataCollection getService() {
            return DataCollection.this;
        }
    }


    public HashMap<String, DataSingleton> getDataForTheDay() {
        return dataForTheDay;
    }

    public class DataForOnePerson{
        HashMap<String,DataSingleton> data;
        String actions;
        String person;

        public DataForOnePerson( HashMap<String,DataSingleton> data, String actions, String person){
            this.data=data;
            this.actions=actions;
            this.person=person;
        }
    }

    public void addToControlledStudyData(long id, boolean answeredRight,String feedback, boolean missed, String rightAnswer,String timeStamp){
        controlledStudyData.add(new DataForANotification(id,answeredRight,feedback,missed, rightAnswer,timeStamp));
    }

    public void updateStudyData(long id, boolean answeredRight, boolean missed, String chosenAnswer){
        for (DataForANotification data : controlledStudyData){
            if(data.getId()==id){
                data.setAnsweredRight(answeredRight);
                data.setMissed(missed);
                data.setChosenAnswer(chosenAnswer);
                data.setResponseTime((System.currentTimeMillis()-id));
            }
        }
    }

    public class DataForANotification{
        private long id;
        private boolean answeredRight;
        private String feedback;
        private boolean missed;
        private long responseTime=-1;
        private String rightAnswer;
        private String timeStamp;
        private String chosenAnswer="None";

        public DataForANotification(long id, boolean answeredRight,String feedback, boolean missed, String rightAnswer,String timeStamp){
            this.id=id;
            this.answeredRight=answeredRight;
            this.feedback=feedback;
            this.missed=missed;
            this.rightAnswer=rightAnswer;
            this.timeStamp=timeStamp;
        }

        public long getId(){
            return id;
        }

        public void setAnsweredRight(boolean answeredRight) {
            this.answeredRight = answeredRight;
        }

        public void setFeedback(String feedback) {
            this.feedback = feedback;
        }

        public void setMissed(boolean missed) {
            this.missed = missed;
        }

        public boolean isMissed() {
            return missed;
        }

        public String getFeedback() {
            return feedback;
        }

        public boolean isAnsweredRight() {
            return answeredRight;
        }

        public long getResponseTime() {
            return responseTime;
        }

        public void setResponseTime(long responseTime) {
            this.responseTime = responseTime;
        }

        public String getRightAnswer() {
            return rightAnswer;
        }

        public void setRightAnswer(String rightAnswer) {
            this.rightAnswer = rightAnswer;
        }

        public String getChosenAnswer() {
            return chosenAnswer;
        }

        public void setChosenAnswer(String chosenAnswer) {
            this.chosenAnswer = chosenAnswer;
        }

        public String getTimeStamp() {
            return timeStamp;
        }
    }

    public boolean isLabStudy() {
        return labStudy;
    }

}
