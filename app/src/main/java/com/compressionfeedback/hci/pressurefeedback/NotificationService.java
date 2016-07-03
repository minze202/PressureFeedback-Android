package com.compressionfeedback.hci.pressurefeedback;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
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
import android.util.Log;

import java.util.Calendar;
import java.util.List;
import java.util.UUID;


public class NotificationService extends NotificationListenerService {

    Context context;
    BluetoothLeService mBluetoothLeService;
    TestReceiver receiver;
    long[][] vibrationPatterns= {{0},{0, 100, 1000, 300, 200, 100, 500, 200, 100},
            {0, 250, 200, 250, 150, 150, 75, 150, 75, 150},
            {0,150,50,75,50,75,50,150,50,75,50,75,50,300},
            {0,100,200,100,100,100,100,100,200,100,500,100,225,100}};
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };



    @Override
    public void onCreate() {

        super.onCreate();
        context = getApplicationContext();
        DataCollection.initInstance();
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        receiver=new TestReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(getString(R.string.testFilter));
        registerReceiver(receiver,filter);


    }
    @Override

    public void onNotificationPosted(StatusBarNotification sbn) {

        String pack =sbn.getPackageName();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String mode = preferences.getString("appMode","sound");
        Log.i("Mode",""+mode.equals("compression"));
        switch (mode) {
            case "compression":
                int pattern = preferences.getInt(pack + "pattern", 1);
                int strength = preferences.getInt(pack + "strength", 0);
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

        DataCollection.getInstance().addData(String.valueOf(sbn.getPostTime()),new DataSingleton(sbn.getNotification(),pack));
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        DataSingleton dataSingleton=DataCollection.getInstance().getDataForTheDay().get(String.valueOf(sbn.getPostTime()));

        long responseTime=Calendar.getInstance().getTimeInMillis()-sbn.getPostTime();
        dataSingleton.setResponseTime(responseTime);
        if(responseTime<90000){
            dataSingleton.setResponded(true);
        }
    }

    public Uri getUri(String title) {
        RingtoneManager manager = new RingtoneManager(this);
        manager.setType(RingtoneManager.TYPE_NOTIFICATION);
        Cursor cursor = manager.getCursor();

        while (cursor.moveToNext()) {
            if(cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX).equals(title)){
                String notificationUri = cursor.getString(RingtoneManager.URI_COLUMN_INDEX)+"/"+cursor.getString(RingtoneManager.ID_COLUMN_INDEX);
                return Uri.parse(notificationUri);
            }
        }
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

    }

    public boolean notificationsFlooding(StatusBarNotification sbn){
        StatusBarNotification[] allActiveNotifications=getActiveNotifications();
        for(StatusBarNotification statusBarNotification : allActiveNotifications){
            if(sbn.getPackageName().equals(statusBarNotification.getPackageName())&& (sbn!=statusBarNotification) && ((sbn.getPostTime()-statusBarNotification.getPostTime())<10000)){
                return true;
            }
        }
        return false;
    }


    public void sendCompressionFeedback(int pattern, int strength){
        List<BluetoothGattService> gattServices = mBluetoothLeService.getSupportedGattServices();
        for (BluetoothGattService gattService : gattServices) {
            if (gattService.getUuid().equals(UUID.fromString(SampleGattAttributes.PRESSURE_SERVICE))) {
                final BluetoothGattCharacteristic writableStrengthCharacteristic = gattService.getCharacteristic(UUID.fromString(SampleGattAttributes.WRITABLE_PRESSURE_STRENGTH_CHARACTERISTIC));
                byte[] value2 = new byte[1];
                value2[0] = (byte) (strength + 1 & 0xff);
                writableStrengthCharacteristic.setValue(value2);
                mBluetoothLeService.writeCharacteristic(writableStrengthCharacteristic);

                final BluetoothGattCharacteristic writablePatternCharacteristic = gattService.getCharacteristic(UUID.fromString(SampleGattAttributes.WRITABLE_PRESSURE_PATTERN_CHARACTERISTIC));
                byte[] value1 = new byte[1];
                value1[0] = (byte) (pattern & 0xff);
                writablePatternCharacteristic.setValue(value1);
                mBluetoothLeService.writeCharacteristic(writablePatternCharacteristic);

                final BluetoothGattCharacteristic readableCharacteristic = gattService.getCharacteristic(UUID.fromString(SampleGattAttributes.READABLE_PRESSURE_CHARACTERISTIC));
                mBluetoothLeService.setReadableCharacteristic(readableCharacteristic);
                mBluetoothLeService.startRunningTask();


                break;
            }
        }
    }

    public void sendSoundFeedback(String audioTitle, int volume){
        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        if(!audioTitle.equals("No Feedback")) {
            try {
                mediaPlayer.setVolume(volume / 100f, volume / 100f);
                mediaPlayer.setDataSource(getApplicationContext(), getUri(audioTitle));
                mediaPlayer.prepare();
            } catch (Exception e) {

            }
            mediaPlayer.start();
        }
    }

    public void sendVibrationFeedback(int patternChoice){
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(vibrationPatterns[patternChoice], -1);
    }

    class TestReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getStringExtra("mode").equals("vibration")) {
                sendVibrationFeedback(intent.getIntExtra("patternChoice", 0));
            }else if(intent.getStringExtra("mode").equals("sound")){
                sendSoundFeedback(intent.getStringExtra("audioTitle"),intent.getIntExtra("volume",100));
            }else if(intent.getStringExtra("mode").equals("compression")){
                sendCompressionFeedback(intent.getIntExtra("pattern", 0), intent.getIntExtra("strength", 1));
            }else if(intent.getStringExtra("mode").equals("remember")){
                int mNotificationId = 001;
                Intent notificationIntent = new Intent(context, QuestionnaireActivity.class);
                PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(context)
                                .setSmallIcon(R.drawable.ic_notification)
                                .setContentTitle("Pressure-Feedback")
                                .setTicker("HW")
                                .setPriority(Notification.PRIORITY_HIGH)
                                .setContentText("Start Questionnaire")
                                .setAutoCancel(true)
                                .setContentIntent(contentIntent);

                NotificationManager mNotifyMgr =
                        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                mNotifyMgr.notify(mNotificationId, mBuilder.build());
            }
        }
    }

}