package com.compressionfeedback.hci.pressurefeedback;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.SeekBar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class AppListActivity extends ListActivity{
    private List<ApplicationInfo> apps = new ArrayList<ApplicationInfo>();
    private List<ApplicationInfo> installedApps = new ArrayList<ApplicationInfo>();
    private List<Integer> priorityList = new ArrayList<Integer>();
    private ApplicationAdapter listAdapter = null;
    private RadioButton currentRadioButtonChecked;
    private Button acceptStrength;
    private Button testStrength;
    private SeekBar strengthSlider;
    SharedPreferences savedPairValues;
    PackageManager pm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Context context=getApplicationContext();
        super.onCreate(savedInstanceState);
        savedPairValues= PreferenceManager.getDefaultSharedPreferences(context);
        setContentView(R.layout.app_list);
        pm = getPackageManager();
        apps = pm.getInstalledApplications(0);
        new LoadApplications().execute();
        strengthSlider=(SeekBar)findViewById(R.id.strength_slider);
        acceptStrength=(Button)findViewById(R.id.accept_strength);
        testStrength=(Button)findViewById(R.id.test_strength);

    }

    @Override
    protected void onResume(){
        super.onResume();
        String mode=savedPairValues.getString("appMode", "sound");
        RadioButton radioButton;
        if(mode == "vibration"){
            int strength=savedPairValues.getInt("vibration_strength",100);
            strengthSlider.setEnabled(true);
            strengthSlider.setFocusable(true);
            strengthSlider.setProgress(strength);
            acceptStrength.setEnabled(true);
            acceptStrength.setFocusable(true);
            testStrength.setEnabled(true);
            testStrength.setFocusable(true);
            radioButton=(RadioButton)findViewById(R.id.vibrationRadio);
        }else if(mode == "compression"){
            radioButton=(RadioButton)findViewById(R.id.compressionRadio);
            int strength=savedPairValues.getInt("compression_strength",100);
            strengthSlider.setEnabled(true);
            strengthSlider.setFocusable(true);
            strengthSlider.setProgress(strength);
            acceptStrength.setEnabled(true);
            acceptStrength.setFocusable(true);
            testStrength.setEnabled(true);
            testStrength.setFocusable(true);
        }else{
            radioButton=(RadioButton)findViewById(R.id.soundRadio);
            strengthSlider.setEnabled(false);
            strengthSlider.setFocusable(false);
            acceptStrength.setEnabled(false);
            acceptStrength.setFocusable(false);
            testStrength.setEnabled(false);
            testStrength.setFocusable(false);
        }
        currentRadioButtonChecked=radioButton;
        radioButton.setChecked(true);
        try {
            listAdapter.notifyDataSetChanged();
        }catch (Exception e){

        }
    }


    @Override
    protected void onListItemClick(ListView l, View v, final int position, long id) {
        super.onListItemClick(l, v, position, id);
        ApplicationInfo app = installedApps.get(position);
        String mode=savedPairValues.getString("appMode","compression");
        Intent intent;
        if(mode == "vibration"){
            intent= new Intent(this, VibrationConfigurationActivity.class);
        }else if(mode == "compression"){
            intent= new Intent(this, CompressionConfigurationActivity.class);
        }else{
            intent= new Intent(this, SoundConfigurationActivity.class);
        }
        String appName=app.loadLabel(pm).toString();
        intent.putExtra("ApplicationName",appName);
        intent.putExtra("ApplicationPack",app.packageName);
        startActivity(intent);

    }



    private List<ApplicationInfo> checkForLaunchIntent(List<ApplicationInfo> list) {
        ArrayList<ApplicationInfo> applist = new ArrayList<ApplicationInfo>();
        SharedPreferences.Editor editor=savedPairValues.edit();
        for (ApplicationInfo info : list) {
            try {
                if (null != pm.getLaunchIntentForPackage(info.packageName)) {
                    applist.add(info);
                    int priority=savedPairValues.getInt(info.packageName,100);
                    priorityList.add(priority);
                    editor.putInt(info.packageName,priority);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            editor.apply();
        }
        Collections.sort(applist, new Comparator<ApplicationInfo>(){

            @Override
            public int compare(ApplicationInfo lhs, ApplicationInfo rhs) {
                return lhs.loadLabel(pm).toString().compareTo(rhs.loadLabel(pm).toString());
            }
        });

        return applist;
    }

    public void onRadioButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();

        SharedPreferences.Editor editor=savedPairValues.edit();
        switch(view.getId()) {
            case R.id.vibrationRadio:
                if (checked) {
                    if(!savedPairValues.getString("connectedDeviceAdress","unknown").equals("unknown")) {
                        currentRadioButtonChecked = (RadioButton) findViewById(R.id.vibrationRadio);
                        int strength = savedPairValues.getInt("compression_strength", 100);
                        strengthSlider.setEnabled(true);
                        strengthSlider.setFocusable(true);
                        strengthSlider.setProgress(strength);
                        acceptStrength.setEnabled(true);
                        acceptStrength.setFocusable(true);
                        testStrength.setEnabled(true);
                        testStrength.setFocusable(true);
                        editor.putString("appMode", "vibration");
                        editor.apply();
                    }else {
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(AppListActivity.this);

                        alertDialogBuilder
                                .setMessage("Zurzeit mit keinen kompatiblen Gerät verbunden!")
                                .setCancelable(false)
                                .setPositiveButton("Okay",new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,int id) {
                                        dialog.cancel();
                                    }
                                });

                        AlertDialog alertDialog = alertDialogBuilder.create();
                        alertDialog.show();
                        currentRadioButtonChecked.setChecked(true);
                    }
                    break;
                }
            case R.id.soundRadio:
                if (checked) {
                    currentRadioButtonChecked=(RadioButton)findViewById(R.id.soundRadio);
                    strengthSlider.setEnabled(false);
                    strengthSlider.setFocusable(false);
                    acceptStrength.setEnabled(false);
                    acceptStrength.setFocusable(false);
                    testStrength.setEnabled(false);
                    testStrength.setFocusable(false);
                    editor.putString("appMode", "sound");
                    editor.apply();
                    break;
                }
            case R.id.compressionRadio:
                if (checked) {
                    if(!savedPairValues.getString("connectedDeviceAdress","unknown").equals("unknown")){
                        currentRadioButtonChecked=(RadioButton)findViewById(R.id.compressionRadio);
                        int strength=savedPairValues.getInt("compression_strength",100);
                        strengthSlider.setEnabled(true);
                        strengthSlider.setFocusable(true);
                        strengthSlider.setProgress(strength);
                        acceptStrength.setEnabled(true);
                        acceptStrength.setFocusable(true);
                        testStrength.setEnabled(true);
                        testStrength.setFocusable(true);
                        editor.putString("appMode", "compression");
                        editor.apply();
                    }else{
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(AppListActivity.this);

                        alertDialogBuilder
                                .setMessage("Zurzeit mit keinen kompatiblen Gerät verbunden!")
                                .setCancelable(false)
                                .setPositiveButton("Okay",new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,int id) {
                                        dialog.cancel();
                                    }
                                });

                        AlertDialog alertDialog = alertDialogBuilder.create();
                        alertDialog.show();
                        currentRadioButtonChecked.setChecked(true);
                    }
                    break;
                }
        }
    }

    public void acceptStrength(View view) {
        SharedPreferences.Editor editor=savedPairValues.edit();
        if(savedPairValues.getString("appMode", "sound").equals("compression")){
            editor.putInt("compression_strength",strengthSlider.getProgress());
            editor.apply();
            Intent i = new  Intent(getString(R.string.testFilter));
            i.putExtra("mode","accept_compression_strength");
            i.putExtra("strength",strengthSlider.getProgress());
            sendBroadcast(i);
        }else if(savedPairValues.getString("appMode", "sound").equals("vibration")){
            editor.putInt("compression_strength",strengthSlider.getProgress());
            editor.apply();
            Intent i = new  Intent(getString(R.string.testFilter));
            i.putExtra("mode","accept_vibration_strength");
            i.putExtra("strength",strengthSlider.getProgress());
            sendBroadcast(i);
        }
    }

    public void testStrength(View view) {
        if(savedPairValues.getString("appMode", "sound").equals("compression")){
            Intent i = new  Intent(getString(R.string.testFilter));
            i.putExtra("mode","test_compression_strength");
            i.putExtra("strength",strengthSlider.getProgress());
            sendBroadcast(i);
        }else if(savedPairValues.getString("appMode", "sound").equals("vibration")){
            Intent i = new  Intent(getString(R.string.testFilter));
            i.putExtra("mode","test_vibration_strength");
            i.putExtra("strength",strengthSlider.getProgress());
            sendBroadcast(i);
        }
    }


    private class LoadApplications extends AsyncTask<Void, Void, Void> {
        private ProgressDialog progress = null;

        @Override
        protected Void doInBackground(Void... params) {
            installedApps = checkForLaunchIntent(pm.getInstalledApplications(PackageManager.GET_META_DATA));
            listAdapter = new ApplicationAdapter(AppListActivity.this,
                    R.layout.snippet_list_row, installedApps);

            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        @Override
        protected void onPostExecute(Void result) {
            setListAdapter(listAdapter);
            progress.dismiss();
            super.onPostExecute(result);
        }

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(AppListActivity.this, null,
                    "Loading application info...");
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }
    }

}
