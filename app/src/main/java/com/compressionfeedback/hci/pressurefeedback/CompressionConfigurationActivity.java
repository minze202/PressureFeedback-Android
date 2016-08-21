package com.compressionfeedback.hci.pressurefeedback;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;

public class CompressionConfigurationActivity extends Activity{


    private Spinner spinnerStrength;
    private Spinner spinner;
    private SharedPreferences preferences;
    private TextView appName;
    private String appPack;
    private DataCollection dataCollectionInstance;
    private ArrayList<String> appPacks;
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
    protected void onCreate(Bundle savedInstanceState) {

        Context context=getApplicationContext();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.compression_configuration);
        spinner=(Spinner) findViewById(R.id.pattern_choice);
        ArrayAdapter<CharSequence> adapter=ArrayAdapter.createFromResource(this, R.array.patterns_array,android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinnerStrength=(Spinner) findViewById(R.id.strength_choice);
        ArrayAdapter<CharSequence> adapter_strength=ArrayAdapter.createFromResource(this, R.array.strength_array,android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStrength.setAdapter(adapter_strength);

        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        Intent intent=getIntent();
        appName = (TextView)findViewById(R.id.app_name2);
        appName.setText(intent.getStringExtra("ApplicationName"));
        appPacks=intent.getStringArrayListExtra("ApplicationPacks");
        if(appPacks==null){
            appPack = intent.getStringExtra("ApplicationPack");
            spinner.setSelection(preferences.getInt(appPack+"pattern",1));
            spinnerStrength.setSelection(preferences.getInt(appPack+"strength",0));
        }else {
            spinner.setSelection(preferences.getInt(appName.getText().toString()+"pattern",1));
            spinnerStrength.setSelection(preferences.getInt(appName.getText().toString()+"strength",0));
        }

        Intent dataCollectionServiceIntent = new Intent(this, DataCollection.class);
        bindService(dataCollectionServiceIntent, mDataCollectionServiceConnection, BIND_AUTO_CREATE);

    }

    public void acceptChanges(View view) {
        SharedPreferences.Editor editor=preferences.edit();
        if(appPacks==null){
            editor.putInt(appPack+"pattern",spinner.getSelectedItemPosition());
            editor.putInt(appPack+"strength",spinnerStrength.getSelectedItemPosition());
            editor.apply();
        }else {
            editor.putInt(appName.getText().toString()+"pattern",spinner.getSelectedItemPosition());
            editor.putInt(appName.getText().toString()+"strength",spinnerStrength.getSelectedItemPosition());
            editor.apply();
            for(String appPackage:appPacks){
                editor.putInt(appPackage+"pattern",spinner.getSelectedItemPosition());
                editor.putInt(appPackage+"strength",spinnerStrength.getSelectedItemPosition());
                editor.apply();
            }
        }

        finish();
    }

    public void cancelChanges(View view) {
        finish();
    }

    public void testCompressionFeedback(View view) {
        Intent i = new  Intent(getString(R.string.testFilter));
        i.putExtra("mode","compression");
        i.putExtra("pattern", spinner.getSelectedItemPosition());
        i.putExtra("strength",spinnerStrength.getSelectedItemPosition());
        sendBroadcast(i);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mDataCollectionServiceConnection);
        dataCollectionInstance = null;
    }



}
