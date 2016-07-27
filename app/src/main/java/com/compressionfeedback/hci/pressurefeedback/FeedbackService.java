package com.compressionfeedback.hci.pressurefeedback;

import android.app.AlarmManager;
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
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.Calendar;
import java.util.Map;
import java.util.Random;


public class FeedbackService extends NotificationListenerService {

    Context context;
    BluetoothLeService mBluetoothLeService;

    TestReceiver receiver;
    private boolean study= false;
    private DataCollection dataCollectionInstance;
    boolean wasRinging=false;
    long[][] vibrationPatterns = {{0}, {0, 100, 1000, 300, 200, 100, 500, 200, 100},
            {0, 250, 200, 250, 150, 150, 75, 150, 75, 150},
            {0, 150, 50, 75, 50, 75, 50, 150, 50, 75, 50, 75, 50, 300},
            {0, 100, 200, 100, 100, 100, 100, 100, 200, 100, 500, 100, 225, 100}};
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

        receiver = new TestReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(getString(R.string.testFilter));
        registerReceiver(receiver, filter);

        TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        PhoneStateListener callStateListener = new PhoneStateListener() {
            public void onCallStateChanged(int state, String incomingNumber)
            {
                if(state==TelephonyManager.CALL_STATE_RINGING)
                {
                    wasRinging=true;
                    sendCompressionFeedback(7,0);
                }
                // If incoming call received
                else{
                    if(wasRinging){
                        wasRinging=false;
                        sendCompressionFeedback(8,0);
                    }
                }
            }
        };
        telephonyManager.listen(callStateListener,PhoneStateListener.LISTEN_CALL_STATE);


    }

    @Override

    public void onNotificationPosted(StatusBarNotification sbn) {
        if(!study) {
            String pack = sbn.getPackageName();
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

                            String audioTitle = preferences.getString(pack + "choice_s", "Standard");
                            int volume = preferences.getInt("volumeOfNotification", 100);
                            sendSoundFeedback(audioTitle, volume);


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

    public Uri getUri(String title) {
        RingtoneManager manager = new RingtoneManager(this);
        manager.setType(RingtoneManager.TYPE_NOTIFICATION);
        Cursor cursor = manager.getCursor();

        while (cursor.moveToNext()) {
            if (cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX).equals(title)) {
                String notificationUri = cursor.getString(RingtoneManager.URI_COLUMN_INDEX) + "/" + cursor.getString(RingtoneManager.ID_COLUMN_INDEX);
                return Uri.parse(notificationUri);
            }
        }
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

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
        if(pattern==6){
            pattern=(new Random().nextInt(5) +1);
        }
        NumberPair[] samplePattern = SampleCompressionPatterns.sampleCompressionPatterns.get(strength).get(pattern - 1);
        for (NumberPair aSamplePattern : samplePattern) {
            mBluetoothLeService.writePressureCharacteristic(aSamplePattern.getX(), aSamplePattern.getY());
        }
        mBluetoothLeService.executeStrengthPatternAction();

    }

    public void sendSoundFeedback(String audioTitle, int volume) {
        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        if (!audioTitle.equals("No Feedback")) {
            try {
                mediaPlayer.setVolume(volume / 100f, volume / 100f);
                mediaPlayer.setDataSource(getApplicationContext(), getUri(audioTitle));
                mediaPlayer.prepare();
            } catch (Exception e) {
                Log.i("FeedbackService", "Couldn't give Sound-Feedback");
            }
            mediaPlayer.start();
        }
    }

    public void sendVibrationFeedback(int patternChoice) {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(vibrationPatterns[patternChoice], -1);
    }

    class TestReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getStringExtra("mode").equals("vibration")) {
                sendVibrationFeedback(intent.getIntExtra("patternChoice", 0));
            } else if (intent.getStringExtra("mode").equals("sound")) {
                sendSoundFeedback(intent.getStringExtra("audioTitle"), intent.getIntExtra("volume", 100));
            } else if (intent.getStringExtra("mode").equals("compression")) {
                sendCompressionFeedback(intent.getIntExtra("pattern", 0), intent.getIntExtra("strength", 1));
            }



            else if (intent.getStringExtra("study").equals("day")) {
                dataCollectionInstance.clear();
                if(dataCollectionInstance.isCollectingData()){
                    Calendar cal=Calendar.getInstance();
                    cal.add(Calendar.DAY_OF_WEEK,1);
                    Intent intentNew = new Intent(getString(R.string.testFilter));
                    intent.putExtra("study","day");
                    PendingIntent pintent = PendingIntent.getBroadcast(getApplicationContext(), 0, intentNew, 0);
                    AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                    alarm.set(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),pintent);
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

}