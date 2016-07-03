package com.compressionfeedback.hci.pressurefeedback;


import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.Calendar;

public class MainActivity extends Activity{

    private TextView alarmTimeText;
    private  SharedPreferences preferences;
    private long alarmTime=0;
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu);
        Context context =getApplicationContext();
        alarmTimeText = (TextView) findViewById(R.id.alarmTime);
        preferences= PreferenceManager.getDefaultSharedPreferences(context);
        alarmTimeText.setText(preferences.getString("alarmTime","00:00"));
    }

    public void goToAppConfiguration(View view) {
        Intent intent=new Intent(this,AppListActivity.class);
        startActivity(intent);
    }

    @Override
    public void onResume(){
        super.onResume();
        updateTextView();
    }

    public void updateTextView(){
        TextView alarmReminder=(TextView)findViewById(R.id.alarmReminder);
        if(alarmTime<System.currentTimeMillis()){
            alarmReminder.setText(R.string.alarm_reminder_instruction);
        }else {
            alarmReminder.setText(R.string.alarm_reminder_text);
        }
    }


    public void goToScanDevices(View view) {
        Intent intent=new Intent(this,DeviceScanActivity.class);
        startActivity(intent);
    }

    public void setAlarm(int hour, int minute){
        Calendar cal=Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY,hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND,0);
        if(cal.getTimeInMillis()<System.currentTimeMillis()){
            cal.add(Calendar.DAY_OF_WEEK,1);
        }


        Intent intent = new Intent(getString(R.string.testFilter));
        intent.putExtra("mode","remember");
        PendingIntent pintent = PendingIntent.getBroadcast(this, 0, intent, 0);

        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmTime=cal.getTimeInMillis();
        alarm.set(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),pintent);
    }

    public void setAlarmTime(View view) {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        TimePickerDialog mTimePicker;
        mTimePicker = new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                String minute=""+selectedMinute;
                String hour = ""+selectedHour;
                if(selectedMinute<10){
                    minute="0"+selectedMinute;
                }
                if(selectedHour<10){
                    hour="0"+selectedHour;
                }
                String time=hour+":"+minute;
                alarmTimeText.setText(time);
                SharedPreferences.Editor editor=preferences.edit();
                editor.putString("alarmTime",time);
                editor.commit();
                setAlarm(selectedHour,selectedMinute);
                updateTextView();
            }
        }, hour, minute, true);
        mTimePicker.setTitle("Select Time");
        mTimePicker.show();

    }

    public void testQuestionnaire(View view) {
        Intent intent=new Intent(this, QuestionnaireActivity.class);
        startActivity(intent);
    }
}
