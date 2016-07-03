package com.compressionfeedback.hci.pressurefeedback;


import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NotificationAdapter extends ArrayAdapter{

    private List<DataSingleton> notificationList = null;
    private Context context;
    public NotificationAdapter(Context context, int textViewResourceId,
                              HashMap<String,DataSingleton> notificationList) {
        super(context, textViewResourceId, new ArrayList<DataSingleton>(notificationList.values()));
        this.context = context;
        ArrayList<DataSingleton> notificationArray= new ArrayList<DataSingleton>(notificationList.values());
        this.notificationList = notificationArray;
    }

    @Override
    public int getCount() {
        return ((null != notificationList) ? notificationList.size() : 0);
    }

    @Override
    public DataSingleton getItem(int position) {
        return ((null != notificationList) ? notificationList.get(position) : null);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (null == view) {
            LayoutInflater layoutInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = layoutInflater.inflate(R.layout.question_layout, null);
        }

        final DataSingleton data = notificationList.get(position);
        if (null != data) {
            TextView notificationAppName = (TextView)view.findViewById(R.id.notificationApp);
            PackageManager packageManager= context.getPackageManager();
            try {
                String appName = (String) packageManager.getApplicationLabel(packageManager.getApplicationInfo(data.getAppPackage(), PackageManager.GET_META_DATA));
                notificationAppName.setText(appName);
            }catch (Exception e){

            }
            TextView notificationTitle = (TextView)view.findViewById(R.id.notificationTitle);
            notificationTitle.setText(data.getNotification().extras.getString(Notification.EXTRA_TITLE));
            TextView notificationContent = (TextView)view.findViewById(R.id.notificationContent);
            notificationContent.setText(data.getNotification().extras.getString(Notification.EXTRA_TEXT));
            TextView question=(TextView)view.findViewById(R.id.question);
            question.setText("Sie haben für diese Notification "+(data.getResponseTime()/1000) +" Sekunden gebraucht bis sie darauf reagiert haben. Bitte antworten sie darauf warum es länger als durchschnittlich gedauert hat." );
            final RadioGroup radioGroup=(RadioGroup) view.findViewById(R.id.answers);
            radioGroup.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    if(v instanceof RadioButton){
                        data.setReason(((RadioButton)v).getText()+"");
                    }
                }
            });
        }
        return view;
    }
}
