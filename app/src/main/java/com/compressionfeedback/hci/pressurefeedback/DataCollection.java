package com.compressionfeedback.hci.pressurefeedback;



import android.app.Notification;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DataCollection {

    private static HashMap<String,DataSingleton> dataForTheDay;
    private static HashMap<String,DataSingleton> completeDataSet;
    private static DataCollection instance;

    public static void initInstance()
    {
        if (instance == null)
        {
            instance = new DataCollection();
        }
    }

    private DataCollection(){
        completeDataSet=new HashMap<String, DataSingleton>();
        dataForTheDay=new HashMap<String, DataSingleton>();
    }

    public int getNotificationAmount(){
        return dataForTheDay.size();
    }

    public HashMap<String, DataSingleton> getRelevantDataForTheDay() {
        HashMap<String, DataSingleton> relevantData=new HashMap<String, DataSingleton>();
        for(Map.Entry<String,DataSingleton> pair : dataForTheDay.entrySet()){
            if((pair.getValue()).getResponseTime()>0){
                relevantData.put(pair.getKey(),pair.getValue());
            }
        }

        return relevantData;
    }

    public void addData(String key, DataSingleton data){
        dataForTheDay.put(key,data);
    }

    public void clear(){
        completeDataSet.putAll(dataForTheDay);
        dataForTheDay.clear();
    }

    public static DataCollection getInstance(){
        return instance;
    }

    public HashMap<String, DataSingleton> getDataForTheDay() {
        return dataForTheDay;
    }
}
