package com.compressionfeedback.hci.pressurefeedback;


import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Random;


public class MainActivity extends Activity{

    private EditText participantsName;
    private Button studyButton;
    private Button labStudyButton;
    private DataCollection dataCollectionInstance;
    private final ServiceConnection mDataCollectionServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            dataCollectionInstance = ((DataCollection.LocalBinder) service).getService();
            Log.i("onResume: ", ""+dataCollectionInstance.isCollectingData());
            if(!dataCollectionInstance.isCollectingData()){
                participantsName.setEnabled(true);
                participantsName.setFocusable(true);
                labStudyButton.setEnabled(true);
                labStudyButton.setFocusable(true);
                studyButton.setText("Studie starten");
            }else {
                participantsName.setEnabled(false);
                participantsName.setFocusable(false);
                labStudyButton.setEnabled(false);
                labStudyButton.setFocusable(false);
                studyButton.setText("Studie stoppen");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            dataCollectionInstance = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu);
        participantsName=(EditText)findViewById(R.id.participantsName);
        Intent dataCollectionServiceIntent = new Intent(this, DataCollection.class);
        bindService(dataCollectionServiceIntent, mDataCollectionServiceConnection, BIND_AUTO_CREATE);
        startService(dataCollectionServiceIntent);

        if (!FeedbackService.isNotificationAccessEnabled){
            startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
        }
        studyButton=(Button)findViewById(R.id.studyButton);
        labStudyButton=(Button)findViewById(R.id.startStudyControlledButton);
    }

    public void goToAppConfiguration(View view) {
        Intent intent=new Intent(this,AppListActivity.class);
        startActivity(intent);
    }



    public void goToScanDevices(View view) {
        dataCollectionInstance.addAction("Es wird nach Geräten gescannt.");
        Intent intent=new Intent(this,DeviceScanActivity.class);
        startActivity(intent);
    }


    public void startStudy(View view) {
        if(!dataCollectionInstance.isCollectingData()){
            dataCollectionInstance.startCollectingData(participantsName.getText().toString());
            Toast.makeText(getApplicationContext(), "Studie fängt nun an.", Toast.LENGTH_SHORT).show();
            participantsName.setEnabled(false);
            participantsName.setFocusable(false);

            studyButton.setText(R.string.stop_study);

        }else {
            dataCollectionInstance.stopCollectingData();
            participantsName.setEnabled(true);
            participantsName.setFocusable(true);
            studyButton.setText(R.string.start_study);
            Toast.makeText(getApplicationContext(), "Studie wurde gestoppt", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mDataCollectionServiceConnection);
        dataCollectionInstance = null;
    }

    public void startStudyControlled(View view) {
        if(!dataCollectionInstance.isCollectingData()){
            dataCollectionInstance.startLabCollectingData(participantsName.getText().toString());
            participantsName.setEnabled(false);
            participantsName.setFocusable(false);
            labStudyButton.setEnabled(false);
            labStudyButton.setFocusable(false);
            studyButton.setText(R.string.stop_study);
            Toast.makeText(getApplicationContext(), "Laborstudie wurde gestartet", Toast.LENGTH_SHORT).show();

        }
    }


}
