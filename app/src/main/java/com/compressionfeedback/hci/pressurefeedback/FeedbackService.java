package com.compressionfeedback.hci.pressurefeedback;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.Random;


public class FeedbackService extends NotificationListenerService {

    Context context;
    BluetoothLeService mBluetoothLeService;

    GeneralReceiver receiver;
    private DataCollection dataCollectionInstance;
    boolean wasRinging=false;
    private int studyCurrentPattern;
    private int studyCurrentStrength;
    private SampleCompressionPatterns sampleCompressionPatterns;
    private SampleVibrationPatterns sampleVibrationPatterns;
    private android.os.Handler mHandler=new android.os.Handler();
    private SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    long[][] vibrationPatterns= {{0},{0, 1000},
                        {0, 500, 200, 500},
                        {0,200,100,200,200,200},
                        {0,1500,200,2000}};
    private final ServiceConnection mBLEServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final ServiceConnection mDataCollectionServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            dataCollectionInstance = ((DataCollection.LocalBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            dataCollectionInstance = null;
        }
    };


    @Override
    public void onCreate() {

        super.onCreate();
        context = getApplicationContext();
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mBLEServiceConnection, BIND_AUTO_CREATE);

        Intent dataCollectionServiceIntent = new Intent(this, DataCollection.class);
        bindService(dataCollectionServiceIntent, mDataCollectionServiceConnection, BIND_AUTO_CREATE);

        receiver = new GeneralReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(getString(R.string.testFilter));
        registerReceiver(receiver, filter);

        TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        PhoneStateListener callStateListener = new PhoneStateListener() {
            public void onCallStateChanged(int state, String incomingNumber)
            {
                if(state==TelephonyManager.CALL_STATE_RINGING)
                {
                    if(!wasRinging) {
                        wasRinging = true;
                        SharedPreferences preferences=PreferenceManager.getDefaultSharedPreferences(context);
                        String mode = preferences.getString("appMode", "sound");
                        switch (mode) {
                            case "compression":

                                mBluetoothLeService.writePressureCharacteristic(6, sampleCompressionPatterns.getStrongPressure());
                                mBluetoothLeService.executeStrengthPatternAction();

                                break;
                            case "sound":
                                vibrateRunnable.run();


                                break;
                            case "vibration":
                                mBluetoothLeService.writePressureCharacteristic(8, 1);
                                mBluetoothLeService.executeStrengthPatternAction();
                                break;
                        }
                    }
                }
                // If incoming call received
                else{
                    if(wasRinging){
                        wasRinging=false;
                        SharedPreferences preferences=PreferenceManager.getDefaultSharedPreferences(context);
                        String mode = preferences.getString("appMode", "sound");
                        switch (mode) {
                            case "compression":

                                mBluetoothLeService.writePressureCharacteristic(7,1);
                                mBluetoothLeService.executeStrengthPatternAction();

                                break;
                            case "sound":
                                mHandler.removeCallbacks(vibrateRunnable);


                                break;
                            case "vibration":
                                mBluetoothLeService.writePressureCharacteristic(9, 1);
                                mBluetoothLeService.executeStrengthPatternAction();
                                break;
                        }
                    }
                }
            }
        };
        sampleCompressionPatterns=new SampleCompressionPatterns();
        sampleVibrationPatterns=new SampleVibrationPatterns();
        telephonyManager.listen(callStateListener,PhoneStateListener.LISTEN_CALL_STATE);


    }

    @Override

    public void onNotificationPosted(StatusBarNotification sbn) {
        String pack = sbn.getPackageName();
        if(!dataCollectionInstance.isLabStudy()) {
            if (!wasRinging) {
                if (!notificationsFlooding(sbn)) {
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                    dataCollectionInstance.addAction("Notification von " + pack + " tritt ein. Es wird Feedback generiert");
                    String mode = preferences.getString("appMode", "sound");
                    switch (mode) {
                        case "compression":
                            int pattern = preferences.getInt(pack + "pattern", 1);
                            int strength = preferences.getInt(pack + "strength", 0);
                            if (pattern == 0) {
                                dataCollectionInstance.addAction("Notification von " + pack + " tritt ein. Es wird wie eingestellt kein Feedback generiert.");
                                return;
                            }
                            sendCompressionFeedback(pattern, strength);

                            break;
                        case "sound":
                            int choice = preferences.getInt(pack + "choice_s_int", 1);
                            sendSoundFeedback(choice);


                            break;
                        case "vibration":
                            int patternChoice = preferences.getInt(pack + "pattern_v", 1);
                            sendVibrationFeedback(patternChoice);
                            break;
                    }
                    dataCollectionInstance.addNotificationData(String.valueOf(sbn.getPostTime()), new DataSingleton(sbn.getNotification(), pack, true));
                    return;
                } else {
                    dataCollectionInstance.addAction("Notification von" + pack + " tritt ein. Es wird aufgrund vón Flooding kein Feedback generiert.");
                }
            } else {
                dataCollectionInstance.addAction("Notification von " + pack + " tritt ein. Es wird kein Feedback generiert, da das Telefon klinkelt.");
            }
            dataCollectionInstance.addNotificationData(String.valueOf(sbn.getPostTime()), new DataSingleton(sbn.getNotification(), pack, false));
        }else {
            if(context.getPackageName().equals(sbn.getPackageName())){
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                dataCollectionInstance.addAction("Notification von " + pack + " tritt ein. Es wird Feedback generiert");
                String mode = preferences.getString("appMode", "sound");
                switch (mode) {
                    case "compression":
                        int pattern = studyCurrentPattern;
                        int strength = studyCurrentStrength;
                        if (pattern == 0) {
                            return;
                        }
                        sendCompressionFeedback(pattern, strength);

                        break;

                    case "vibration":
                        sendVibrationFeedback(studyCurrentPattern);
                        break;
                    case "sound":
                        sendSoundFeedback(studyCurrentPattern);
                }
            }
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        String pack=sbn.getPackageName();
        dataCollectionInstance.addAction("Notification von "+pack+ " wurde entfernt.");
        DataSingleton dataSingleton = dataCollectionInstance.getDataForTheDay().get(String.valueOf(sbn.getPostTime()));

        long responseTime = Calendar.getInstance().getTimeInMillis() - sbn.getPostTime();
        if(dataSingleton!=null){
            dataSingleton.setResponseTime(responseTime);
        }
    }

    public boolean notificationsFlooding(StatusBarNotification sbn) {
        for(Map.Entry<String,DataSingleton> pair : dataCollectionInstance.getDataForTheDay().entrySet()){
            if (sbn.getPackageName().equals(pair.getValue().getAppPackage()) && (sbn.getPostTime() != Long.parseLong(pair.getKey())) && ((sbn.getPostTime() - Long.parseLong(pair.getKey())) < 5000) && pair.getValue().isFeedbackSent()) {
                return true;
            }
        }

        return false;
    }


    public void sendCompressionFeedback(int pattern, int strength) {
        NumberPair[] samplePattern = sampleCompressionPatterns.getSampleCompressionPatterns().get(strength).get(pattern - 1);
        for (NumberPair aSamplePattern : samplePattern) {
            mBluetoothLeService.writePressureCharacteristic(aSamplePattern.getX(), aSamplePattern.getY());
        }
        mBluetoothLeService.executeStrengthPatternAction();

    }

    public void sendSoundFeedback(int patternChoice) {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(vibrationPatterns[patternChoice], -1);
    }

    public void sendVibrationFeedback(int patternChoice) {
        NumberPair[] samplePattern = sampleVibrationPatterns.sampleVibrationPatterns.get(patternChoice - 1);
        for (NumberPair aSamplePattern : samplePattern) {
            mBluetoothLeService.writePressureCharacteristic(aSamplePattern.getX(), aSamplePattern.getY());
        }
        mBluetoothLeService.executeStrengthPatternAction();


    }

    class GeneralReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            SharedPreferences sharedPreferences=PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            if (intent.getStringExtra("mode").equals("vibration")) {
                sendVibrationFeedback(intent.getIntExtra("patternChoice", 0));
            } else if (intent.getStringExtra("mode").equals("sound")) {
                sendSoundFeedback(intent.getIntExtra("audioTitle",1));
            } else if (intent.getStringExtra("mode").equals("compression")) {
                sendCompressionFeedback(intent.getIntExtra("pattern", 1), intent.getIntExtra("strength", 1));
            }else if (intent.getStringExtra("mode").equals("test_compression_strength")) {
                sampleCompressionPatterns.changePressure(intent.getIntExtra("strength",100));
                sendCompressionFeedback(1, 1);
                sampleCompressionPatterns.changePressure(sharedPreferences.getInt("compression_strength",100));


            }else if (intent.getStringExtra("mode").equals("accept_compression_strength")) {
                sampleCompressionPatterns.changePressure(intent.getIntExtra("strength",100));
                SharedPreferences.Editor editor=sharedPreferences.edit();
                editor.putInt("compression_strength",intent.getIntExtra("strength",100));
                editor.apply();


            }else if (intent.getStringExtra("mode").equals("test_vibration_strength")) {
                sampleVibrationPatterns.setVibrationStrength(intent.getIntExtra("strength",100));
                sendVibrationFeedback(1);
                sampleVibrationPatterns.setVibrationStrength(sharedPreferences.getInt("vibration_strength",100));


            }else if (intent.getStringExtra("mode").equals("accept_vibration_strength")) {
                sampleVibrationPatterns.setVibrationStrength(intent.getIntExtra("strength",100));
                SharedPreferences.Editor editor=sharedPreferences.edit();
                editor.putInt("vibration_strength",intent.getIntExtra("strength",100));
                editor.apply();


            }
            else if (intent.getStringExtra("mode").equals("randomFeedback")) {
                if(dataCollectionInstance.isCollectingData()) {
                    Random random = new Random();
                    studyCurrentPattern = random.nextInt(4) + 1;
                    studyCurrentStrength = 1;
                    String mode = sharedPreferences.getString("appMode", "sound");
                    long id = System.currentTimeMillis();
                    notifyUser(id, mode);
                    dataCollectionInstance.addToControlledStudyData(id, false, mode, true, getRightanswerForStudyNotification(),sdf.format(Calendar.getInstance().getTime()));

                }
            }
            else if(intent.getStringExtra("mode").equals("stopRandomFeedback")){
                if(dataCollectionInstance.isCollectingData()) {
                    Toast.makeText(getApplicationContext(), "Laborstudie wurde gestoppt", Toast.LENGTH_SHORT).show();
                    Intent stopIntent = new Intent(getString(R.string.testFilter));
                    stopIntent.putExtra("mode", "randomFeedback");
                    PendingIntent pIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, stopIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                    AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                    alarm.cancel(pIntent);
                    dataCollectionInstance.writeDownStudyResult();
                    dataCollectionInstance.stopCollectingData();
                    Log.i("onReceive: ", " STOPPPP!!!");
                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(getApplicationContext())
                                    .setSmallIcon(R.drawable.ic_notification)
                                    .setContentTitle("Pressure-Feedback")
                                    .setTicker("Ende")
                                    .setPriority(Notification.PRIORITY_HIGH)
                                    .setContentText("Studie ist vorbei!")
                                    .setAutoCancel(true);

                    NotificationManager mNotifyMgr =
                            (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    mNotifyMgr.notify(3, mBuilder.build());
                }
            }

        }
    }

    public static boolean isNotificationAccessEnabled = false;

    @Override
    public IBinder onBind(Intent mIntent) {
        IBinder mIBinder = super.onBind(mIntent);
        isNotificationAccessEnabled = true;
        return mIBinder;
    }

    @Override
    public boolean onUnbind(Intent mIntent) {
        boolean mOnUnbind = super.onUnbind(mIntent);
        isNotificationAccessEnabled = false;
        return mOnUnbind;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(mDataCollectionServiceConnection);
        dataCollectionInstance = null;
    }

    Runnable vibrateRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                sendSoundFeedback(1);
            } finally {
                mHandler.postDelayed(vibrateRunnable, 1500);
            }
        }
    };


    public String getRightanswerForStudyNotification(){
        String pattern="";
        if(studyCurrentPattern==1){
            pattern="A";
        }else if(studyCurrentPattern==2){
            pattern="B";
        }else if(studyCurrentPattern==3){
            pattern="C";
        }else if(studyCurrentPattern==4){
            pattern="D";
        }
        return pattern;
    }

    private void notifyUser(long id, String mode) {
        Intent resultIntent = new Intent(this, QuestionActivity.class);
        String pattern=getRightanswerForStudyNotification();
        resultIntent.putExtra("rightAnswer",pattern);
        resultIntent.putExtra("mode",mode);
        resultIntent.putExtra("id",id);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle("Pressure-Feedback")
                        .setTicker("Welches Pattern?")
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setContentText("Bitte geben sie an welches Pattern sie gespürt haben!")
                        .setAutoCancel(true)
                        .setContentIntent(resultPendingIntent);

        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(0, mBuilder.build());
}

}