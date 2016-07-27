/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.compressionfeedback.hci.pressurefeedback;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private android.os.Handler mHandler=new android.os.Handler();


    private int mConnectionState = STATE_DISCONNECTED;
    private BluetoothGattCharacteristic readableCharacteristic;

    private int mInterval=200;
    private int timer=0;
    private int timeOut=30000;
    private boolean taskRunning=false;
    private boolean manualDisconnect=false;
    private boolean isRinging=false;
    private boolean automaticReconnect=false;

    private DataCollection dataCollectionInstance;


    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_CONNECTING =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTING";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";

    public final static UUID UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);

    private final LinkedList<ServiceAction> mQueueCharacteristics = new LinkedList<>();        // list of actions to execute
    private final LinkedList<ServiceAction> mQueueStrengthPattern = new LinkedList<>();

    private volatile ServiceAction mCurrentAction;

    private String state="0";



    public interface ServiceAction {

        /**
         * Executes action.
         *
         * @return true - if action was executed instantly. false if action is waiting for feedback.
         */
        boolean execute();
    }

    private BluetoothLeService.ServiceAction serviceWriteAction(final BluetoothGattCharacteristic characteristic) {
        return new BluetoothLeService.ServiceAction() {

            @Override
            public boolean execute() {

                if (characteristic != null) {
                    mBluetoothGatt.writeCharacteristic(characteristic);
                    return false;
                } else {
                    Log.w(TAG, "write: characteristic not found: ");
                    return true;
                }
            }
        };
    }

    private BluetoothLeService.ServiceAction servicePressureWriteAction(final int pattern, final int strength) {
        return new BluetoothLeService.ServiceAction() {

            @Override
            public boolean execute() {
                List<BluetoothGattService> gattServices = getSupportedGattServices();
                for (BluetoothGattService gattService : gattServices) {
                    if (gattService.getUuid().equals(UUID.fromString(SampleGattAttributes.PRESSURE_SERVICE))) {


                        BluetoothGattCharacteristic writableStrengthCharacteristic = gattService.getCharacteristic(UUID.fromString(SampleGattAttributes.WRITABLE_PRESSURE_STRENGTH_CHARACTERISTIC));
                        byte[] value2 = new byte[1];
                        value2[0] = (byte) (strength & 0xff);
                        writableStrengthCharacteristic.setValue(value2);
                        writeCharacteristic(writableStrengthCharacteristic);

                        BluetoothGattCharacteristic writablePatternCharacteristic = gattService.getCharacteristic(UUID.fromString(SampleGattAttributes.WRITABLE_PRESSURE_PATTERN_CHARACTERISTIC));
                        byte[] value1 = new byte[1];
                        value1[0] = (byte) (pattern & 0xff);
                        writablePatternCharacteristic.setValue(value1);
                        writeCharacteristic(writablePatternCharacteristic);
                        final BluetoothGattCharacteristic readableCharacteristic = gattService.getCharacteristic(UUID.fromString(SampleGattAttributes.READABLE_PRESSURE_CHARACTERISTIC));
                        setReadableCharacteristic(readableCharacteristic);
                        startWriteCharacteristicTask();
                        break;
                        }

                }return false;
            }
        };
    }
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


    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            Log.i(TAG, "onConnectionStateChange: State changed"+status+ " "+newState);

            String intentAction;
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor editor=sharedPreferences.edit();
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                editor.putString("connectedDeviceAdress",mBluetoothDeviceAddress);
                editor.apply();
                if(automaticReconnect){
                    dataCollectionInstance.addAction("Erfolgreich Verbindung automatisch wiederaufgenommen.");
                    automaticReconnect=false;
                }
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                mBluetoothAdapter=null;
                mBluetoothManager=null;
                initialize();
                broadcastUpdate(intentAction);
                editor.putString("connectedDeviceAdress","unknown");
                editor.putString("appMode", "sound");
                editor.apply();
                if(!manualDisconnect){
                    dataCollectionInstance.addAction("Verbindung zum Gerät verloren.");
                    try{
                        if(connect(sharedPreferences.getString("connectedDeviceAdress","unknown"))){
                            automaticReconnect=true;
                            return;
                        }
                    }catch (Exception e){
                        Log.i(TAG, "Could not reconnect!");
                    }
                    notifyUser();
                }
                manualDisconnect=false;
                Log.i(TAG, "Disconnected from GATT server.");
            }
        }




        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if(state.equals("0")&& String.valueOf(characteristic.getValue()[0] & 0xff).equals("0")){
                    state="1";
                    timer=0;
                    executeWriteAction();
                }else if(state.equals("1")&& String.valueOf(characteristic.getValue()[0] & 0xff).equals("1")) {
                    state = "2";
                    timer=0;
                    executeWriteAction();
                }else if(state.equals("2")&& String.valueOf(characteristic.getValue()[0] & 0xff).equals("0")) {
                    state = "0";
                    timer = 0;
                    if(mQueueCharacteristics.isEmpty()){
                        stopWriteCharacteristicTask();
                    }
                }else if(state.equals("2")&& isRinging) {
                    timer = 0;
                    executeWriteAction();
                }
                else {
                    timer+=mInterval;
                    if(timer>timeOut){
                        mQueueCharacteristics.clear();
                        mQueueStrengthPattern.clear();
                        stopWriteCharacteristicTask();
                        timer=0;
                    }
                }

            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
        }

        @Override
        public void onCharacteristicWrite (BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
            if(String.valueOf(characteristic.getValue()[0]).equals("6") && !isRinging){
                isRinging=true;
            }else if(String.valueOf(characteristic.getValue()[0]).equals("7") && isRinging){
                isRinging=false;
            }
            Log.i(TAG, "onCharacteristicWrite: "+ status+ "value: " + String.valueOf(characteristic.getValue()[0] & 0xff));
        }
    };

    @Override
    public void onCreate(){
        super.onCreate();
        Intent dataCollectionServiceIntent = new Intent(this, DataCollection.class);
        bindService(dataCollectionServiceIntent, mDataCollectionServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy(){
        dataCollectionInstance.addAction("Verbindung zum Gerät verloren. Service wurde zerstört");
        notifyUser();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor=sharedPreferences.edit();
        editor.putString("connectedDeviceAdress","unknown");
        editor.putString("appMode", "sound");
        editor.apply();
        super.onDestroy();

    }

    private void notifyUser() {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle("Pressure-Feedback")
                        .setTicker("Verbindung verloren")
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setContentText("Verbindung zum Gerät verloren!")
                        .setAutoCancel(true);

        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(0, mBuilder.build());
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }





    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        Intent intent=new Intent(ACTION_GATT_CONNECTING);
        sendBroadcast(intent);
        Log.d(TAG, "Connecting");
        return true;
    }


    Runnable writeCharacteristicRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                readCharacteristic(readableCharacteristic);
            } finally {
                mHandler.postDelayed(writeCharacteristicRunnable, mInterval);
            }
        }
    };




    protected void executeWriteAction() {
        if (mCurrentAction == null) {
            if (!mQueueCharacteristics.isEmpty()) {
                final BluetoothLeService.ServiceAction action = mQueueCharacteristics.pop();
                mCurrentAction = action;
                action.execute();
                mCurrentAction = null;
            }
        }
    }

    protected void executeStrengthPatternAction() {
        final BluetoothLeService.ServiceAction action = mQueueStrengthPattern.pop();
        action.execute();
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */

    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        manualDisconnect=true;
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        //boolean bool = mBluetoothGatt.writeCharacteristic(characteristic);
        ServiceAction action = serviceWriteAction(characteristic);
        mQueueCharacteristics.add(action);
        Log.i(TAG, "writeCharacteristic: added");
    }

    public void writePressureCharacteristic(int pattern, int strength) {
        ServiceAction action = servicePressureWriteAction(pattern,strength);
        mQueueStrengthPattern.add(action);
    }


    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        // This is specific to Heart Rate Measurement.
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    public void setReadableCharacteristic(BluetoothGattCharacteristic characteristic){
        readableCharacteristic=characteristic;
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    public void startWriteCharacteristicTask(){
        if(!taskRunning){
            writeCharacteristicRunnable.run();
            taskRunning=true;
        }
    }

    public void stopWriteCharacteristicTask(){
        mHandler.removeCallbacks(writeCharacteristicRunnable);
        taskRunning=false;
        if(!mQueueStrengthPattern.isEmpty()){
            executeStrengthPatternAction();
        }
    }

    public void setAutomaticReconnect(boolean automaticReconnect) {
        this.automaticReconnect = automaticReconnect;
    }

    public int getmConnectionState() {
        return mConnectionState;
    }

}
