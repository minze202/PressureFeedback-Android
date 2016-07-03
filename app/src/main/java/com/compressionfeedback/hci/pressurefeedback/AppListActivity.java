package com.compressionfeedback.hci.pressurefeedback;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.RadioButton;

import java.util.ArrayList;
import java.util.List;


public class AppListActivity extends ListActivity{
    private List<ApplicationInfo> apps = new ArrayList<ApplicationInfo>();
    private List<ApplicationInfo> installedApps = new ArrayList<ApplicationInfo>();
    private List<Integer> priorityList = new ArrayList<Integer>();
    private ApplicationAdapter listadapter = null;
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
        String mode=savedPairValues.getString("appMode", "sound");
        RadioButton radioButton;
        if(mode == "vibration"){
            radioButton=(RadioButton)findViewById(R.id.vibrationRadio);
        }else if(mode == "compression"){
            radioButton=(RadioButton)findViewById(R.id.compressionRadio);
        }else{
            radioButton=(RadioButton)findViewById(R.id.soundRadio);
        }
        radioButton.setChecked(true);

    }

    @Override
    protected void onResume(){
        super.onResume();
        try {
            listadapter.notifyDataSetChanged();
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
            editor.commit();
        }

        return applist;
    }

    public void onRadioButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();

        SharedPreferences.Editor editor=savedPairValues.edit();
        switch(view.getId()) {
            case R.id.vibrationRadio:
                if (checked) {
                    editor.putString("appMode", "vibration");
                    editor.commit();
                    break;
                }
            case R.id.soundRadio:
                if (checked) {
                    editor.putString("appMode", "sound");
                    editor.commit();
                    break;
                }
            case R.id.compressionRadio:
                if (checked) {
                    editor.putString("appMode", "compression");
                    editor.commit();
                    break;
                }
        }
    }


    private class LoadApplications extends AsyncTask<Void, Void, Void> {
        private ProgressDialog progress = null;

        @Override
        protected Void doInBackground(Void... params) {
            installedApps = checkForLaunchIntent(pm.getInstalledApplications(PackageManager.GET_META_DATA));
            listadapter = new ApplicationAdapter(AppListActivity.this,
                    R.layout.snippet_list_row, installedApps);

            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        @Override
        protected void onPostExecute(Void result) {
            setListAdapter(listadapter);
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

    public void go_to_connection(View view){
        finish();
    }
}
