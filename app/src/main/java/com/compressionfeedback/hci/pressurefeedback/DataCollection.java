package com.compressionfeedback.hci.pressurefeedback;




import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DataCollection extends Service{

    private HashMap<String,DataSingleton> dataForTheDay;
    private ArrayList<DataForOnePerson> completeDataSet;
    private String userActions="";
    private String currentUser;
    private boolean collectingData=false;
    private File dir;
    private File fileActions;
    private File fileNotification;


    @Override
    public void onCreate(){
        super.onCreate();
        completeDataSet= new ArrayList<>();
        dataForTheDay=new HashMap<>();
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
        if(collectingData){
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
        Log.i("startCollectingData: ", ""+collectingData);
    }

    public void clear(){
        writeNotificationDataIntoFile();
        DataForOnePerson person=new DataForOnePerson(dataForTheDay,userActions,currentUser);
        completeDataSet.add(person);
        dataForTheDay.clear();
        userActions="";
    }

    public void stopCollectingData(){
        collectingData=false;
        clear();
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
            fileActions = new File(dir, Calendar.getInstance().getDisplayName(Calendar.DAY_OF_WEEK,Calendar.LONG, Locale.getDefault())+"_userActions_"+currentUser+".txt");
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
            File sdCard = Environment.getExternalStorageDirectory();
            dir = new File (sdCard.getPath() + "/logs/");
            if(!dir.exists()){
                dir.mkdirs();
            }
            fileNotification = new File(dir, Calendar.getInstance().getDisplayName(Calendar.DAY_OF_WEEK,Calendar.LONG, Locale.getDefault())+"_notificationData.txt");
            if(!fileNotification.exists()){
                fileNotification.createNewFile();
            }

            FileOutputStream f = new FileOutputStream(fileNotification);
            for(Map.Entry<String,DataSingleton> pair : getDataForTheDay().entrySet()){
                data=data+"Notification Package: " +pair.getValue().getAppPackage()+"\n"+"Dismissal Time: "+pair.getValue().getResponseTime()+"\n\n";
            }
            FileWriter fileWriter=new FileWriter(fileNotification,true);
            fileWriter.write(data);
            fileWriter.close();;
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
}
