package com.compressionfeedback.hci.pressurefeedback;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public class VibrationConfigurationActivity extends Activity {

    private Spinner spinner;
    private SharedPreferences preferences;
    private TextView appName;
    private String appPack;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Context context=getApplicationContext();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vibration_configuration);
        spinner=(Spinner) findViewById(R.id.pattern_choice_v);
        ArrayAdapter<CharSequence> adapter=ArrayAdapter.createFromResource(this, R.array.patterns_v_array,android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        Intent intent=getIntent();
        appName = (TextView)findViewById(R.id.app_name_v);
        appName.setText(intent.getStringExtra("ApplicationName"));
        appPack = intent.getStringExtra("ApplicationPack");
        spinner.setSelection(preferences.getInt(appPack+"pattern_v",1));

    }

    public void acceptChanges(View view) {
        SharedPreferences.Editor editor=preferences.edit();
        editor.putInt(appPack+"pattern_v",spinner.getSelectedItemPosition());
        editor.apply();
        finish();
    }

    public void cancelChanges(View view) {
        finish();
    }

    public void testVibrationFeedback(View view) {
        Intent i = new  Intent(getString(R.string.testFilter));
        i.putExtra("mode","vibration");
        i.putExtra("patternChoice",spinner.getSelectedItemPosition());
        sendBroadcast(i);
    }
}
