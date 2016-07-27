package com.compressionfeedback.hci.pressurefeedback;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;

public class SoundConfigurationActivity extends Activity {
    ArrayList<String> sound_array=new ArrayList<String>();
    private Spinner spinner;
    private SharedPreferences preferences;
    private TextView appName;
    private String appPack;
    private SeekBar volumeSlider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Context context=getApplicationContext();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sound_configuration);
        fillSoundsArray();

        spinner=(Spinner) findViewById(R.id.pattern_choice_s);
        ArrayAdapter<CharSequence> adapter=new ArrayAdapter(this, android.R.layout.simple_spinner_item,new ArrayList());
        adapter.add("No Feedback");
        adapter.addAll(sound_array);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        Intent intent=getIntent();
        appName = (TextView)findViewById(R.id.app_name_s);
        appName.setText(intent.getStringExtra("ApplicationName"));
        appPack = intent.getStringExtra("ApplicationPack");
        spinner.setSelection(preferences.getInt(appPack+"choice_s_int",1));

        volumeSlider=(SeekBar)findViewById(R.id.volumeSlider);
        volumeSlider.setProgress(preferences.getInt("volumeOfNotification",100));

       // Intent intent2 = new Intent(this, NotificationService.class);
       // bindService(intent2, mConnection, Context.BIND_AUTO_CREATE);
    }

    public void acceptChanges(View view) {
        SharedPreferences.Editor editor=preferences.edit();
        editor.putString(appPack+"choice_s",(String)spinner.getSelectedItem());
        editor.putInt(appPack+"choice_s_int",spinner.getSelectedItemPosition());
        editor.putInt("volumeOfNotification",volumeSlider.getProgress());
        editor.commit();
        finish();
    }

    public void cancelChanges(View view) {
        finish();
    }

    public void fillSoundsArray() {
        RingtoneManager manager = new RingtoneManager(this);
        manager.setType(RingtoneManager.TYPE_NOTIFICATION);
        Cursor cursor = manager.getCursor();

        while (cursor.moveToNext()) {
            String soundTitle = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);
            sound_array.add(soundTitle);
        }

    }

    public void testSoundFeedback(View view) {
        Intent i = new  Intent(getString(R.string.testFilter));
        i.putExtra("mode","sound");
        i.putExtra("audioTitle",(String)spinner.getSelectedItem());
        i.putExtra("volume",volumeSlider.getProgress());
        sendBroadcast(i);
    }

}
