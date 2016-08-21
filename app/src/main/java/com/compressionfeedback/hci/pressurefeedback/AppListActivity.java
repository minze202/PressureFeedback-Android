package com.compressionfeedback.hci.pressurefeedback;

import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class AppListActivity extends Activity {
    private List<ApplicationInfo> apps = new ArrayList<ApplicationInfo>();
    private List<ApplicationInfo> installedApps = new ArrayList<ApplicationInfo>();
    private List<String> group=new ArrayList<>();
    private List<Integer> priorityList = new ArrayList<Integer>();
    private ExpandableListAdapter listAdapter = null;
    private RadioButton currentRadioButtonChecked;
    private ExpandableListView expandableListView;
    private HashMap<String,List<ApplicationInfo>> appList=new HashMap<>();
    private boolean addAppsMode=false;
    private String addingAppsToCategory="";
    SharedPreferences savedPairValues;
    PackageManager pm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Context context=getApplicationContext();
        super.onCreate(savedInstanceState);
        savedPairValues= PreferenceManager.getDefaultSharedPreferences(context);
        setContentView(R.layout.app_list);
        expandableListView=(ExpandableListView)findViewById(R.id.expand_list);
        pm = getPackageManager();
        apps = pm.getInstalledApplications(0);
        new LoadApplications().execute();

    }

    @Override
    protected void onResume(){
        super.onResume();
        String mode=savedPairValues.getString("appMode", "sound");
        RadioButton radioButton;
        if(mode == "vibration"){
            radioButton=(RadioButton)findViewById(R.id.vibrationRadio);
        }else if(mode == "compression"){
            radioButton=(RadioButton)findViewById(R.id.compressionRadio);
        }else{
            radioButton=(RadioButton)findViewById(R.id.soundRadio);
        }
        currentRadioButtonChecked=radioButton;
        radioButton.setChecked(true);
        try {
            listAdapter.notifyDataSetChanged();
        }catch (Exception e){

        }
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
                    editor.putString("appMode", "sound");
                    editor.apply();
                    break;
                }
            case R.id.compressionRadio:
                if (checked) {
                    if(!savedPairValues.getString("connectedDeviceAdress","unknown").equals("unknown")){
                        currentRadioButtonChecked=(RadioButton)findViewById(R.id.compressionRadio);
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


    public void orderAppsIntoCategories(){
        Set<String> categorySet=savedPairValues.getStringSet(getString(R.string.app_categories),null);
        if(categorySet!=null){
            if(!((HashSet)categorySet).contains(UNDEFINED)){
                categorySet.add(UNDEFINED);
            }
            for (int i=0;i<categorySet.toArray().length;i++){
                group.add((String)categorySet.toArray()[i]);
                ArrayList<ApplicationInfo> categoryApps=new ArrayList<>();
                for(int j=0;j<installedApps.size();j++){
                    if(savedPairValues.getString(installedApps.get(j).packageName+"category",UNDEFINED).equals(categorySet.toArray()[i])){
                        categoryApps.add(installedApps.get(j));
                    }
                }
                appList.put((String)categorySet.toArray()[i],categoryApps);
            }
        }else {
            appList.put(UNDEFINED,installedApps);
            if(!((HashSet)group).contains(UNDEFINED)){
                group.add(UNDEFINED);
            }
            SharedPreferences.Editor editor=savedPairValues.edit();
            categorySet=new HashSet<>();
            categorySet.add(UNDEFINED);
            editor.putStringSet(getString(R.string.app_categories),categorySet);
            editor.apply();
        }
    }
    private final ExpandableListView.OnChildClickListener appListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    ApplicationInfo app = appList.get(group.get(groupPosition)).get(childPosition);
                    if(!addAppsMode) {
                        String mode = savedPairValues.getString("appMode", "compression");
                        Intent intent;
                        if (mode == "vibration") {
                            intent = new Intent(getApplicationContext(), VibrationConfigurationActivity.class);
                        } else if (mode == "compression") {
                            intent = new Intent(getApplicationContext(), CompressionConfigurationActivity.class);
                        } else {
                            intent = new Intent(getApplicationContext(), SoundConfigurationActivity.class);
                        }
                        String appName = app.loadLabel(pm).toString();
                        intent.putExtra("ApplicationName", appName);
                        intent.putExtra("ApplicationPack", app.packageName);
                        startActivity(intent);

                        return false;
                    }else{
                        String currentCategory=savedPairValues.getString(app.packageName+"category",UNDEFINED);
                        appList.get(currentCategory).remove(app);
                        appList.get(addingAppsToCategory).add(app);
                        SharedPreferences.Editor editor=savedPairValues.edit();
                        editor.putString(app.packageName+"category",addingAppsToCategory);
                        editor.apply();
                        listAdapter.notifyDataSetChanged();
                        return false;
                    }
                }
            };

    public final static String GOOGLE_URL = "https://play.google.com/store/apps/details?id=";
    public static final String UNDEFINED = "Undefiniert";

    private class FetchCategoryTask extends AsyncTask<Void, Void, Void> {

        private final String TAG = FetchCategoryTask.class.getSimpleName();
        private ProgressDialog progress = null;

        @Override
        protected Void doInBackground(Void... errors) {
            String category;
            ArrayList<String> categories=new ArrayList<>();
            categories.add(UNDEFINED);
            SharedPreferences.Editor editor=savedPairValues.edit();
            Iterator<ApplicationInfo> iterator = installedApps.iterator();
            while (iterator.hasNext()) {
                ApplicationInfo packageInfo = iterator.next();
                String query_url = GOOGLE_URL + packageInfo.packageName;
                Log.i(TAG, query_url);
                category = getCategory(query_url);
                editor.putString(packageInfo.packageName+"category",category);
                editor.apply();
                if(!categoryAlreadyExist(category,categories)){
                    categories.add(category);
                }
            }
            Set<String> categorySet=new HashSet<String>();
            categorySet.addAll(categories);
            editor.putStringSet(getString(R.string.app_categories),categorySet);
            editor.apply();
            return null;
        }

        public boolean categoryAlreadyExist(String category,ArrayList<String> categories){
            for(int i=0; i<categories.size();i++){
                if(categories.get(i).equals(category)){
                    return true;
                }
            }return false;
        }


        private String getCategory(String query_url) {
            if (!isNetworkAvailable()) {
                return UNDEFINED;
            } else {
                try {
                    Document doc = Jsoup.connect(query_url).get();
                    Element link = doc.select("span[itemprop=genre]").first();
                    return link.text();
                } catch (Exception e) {
                    return UNDEFINED;
                }
            }
        }

        @Override
        protected void onPostExecute(Void result) {

            progress.dismiss();
            super.onPostExecute(result);
        }

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(AppListActivity.this, null,
                    "Apps werden sortiert...");
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }
    }

    public boolean isNetworkAvailable() {
        Context context = getApplicationContext();
        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) {
            return false;
        } else {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null) {
                for (NetworkInfo anInfo : info) {
                    if (anInfo.getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.addCategory:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Kategorie Name");
                final EditText input = new EditText(this);
                builder.setView(input);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        addCategory(input.getText().toString());
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                AlertDialog dialog=builder.create();
                dialog.show();
                return true;
            case R.id.deleteCategory:
                AlertDialog.Builder builderSecond = new AlertDialog.Builder(this);
                builderSecond.setTitle("Zu löschende Kategorie auswählen");

                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                        this,
                        android.R.layout.select_dialog_singlechoice);
                for(String category:appList.keySet()){
                    arrayAdapter.add(category);
                }
                builderSecond.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String strName = arrayAdapter.getItem(which);
                        deleteCategory(strName);
                        AlertDialog.Builder builderInner = new AlertDialog.Builder(
                                AppListActivity.this);
                        builderInner.setMessage(strName+" wurde gelöscht");
                        builderInner.setTitle("Löschvorgang erfolgreich");
                        builderInner.setPositiveButton(
                                "Ok",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(
                                            DialogInterface dialog,
                                            int which) {
                                        dialog.dismiss();
                                    }
                                });
                        builderInner.show();
                    }
                });
                builderSecond.setNegativeButton(
                        "Abbruch",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                AlertDialog dialogSecond=builderSecond.create();
                dialogSecond.show();
                return true;
            case R.id.sortApps:
                if(!isNetworkAvailable()){
                    AlertDialog.Builder builderThird = new AlertDialog.Builder(this);
                    builderThird.setMessage("Keine Internetverbindung vorhanden! Apps werden nicht automatisch sortiert.");
                    builderThird.setNegativeButton("Abbruch", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    AlertDialog dialogThird=builderThird.create();
                    dialogThird.show();
                }else{
                    new FetchCategoryTask().execute();
                    orderAppsIntoCategories();
                    listAdapter.notifyDataSetChanged();
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void deleteCategory(String category) {
        group.remove(category);
        Set<String> set=new HashSet<>(savedPairValues.getStringSet(getString(R.string.app_categories),null));
        set.remove(category);
        SharedPreferences.Editor editor= savedPairValues.edit();
        List<ApplicationInfo> apps=new ArrayList<>();
        for(ApplicationInfo applicationInfo:appList.get(category)){
            editor.putString(applicationInfo.packageName+"category",UNDEFINED);
            editor.apply();
        }
        apps.addAll(appList.get(UNDEFINED));
        apps.addAll(appList.get(category));
        appList.remove(category);
        appList.put(UNDEFINED,apps);
        editor.putStringSet(getString(R.string.app_categories),set);
        editor.apply();
        listAdapter.notifyDataSetChanged();
    }

    public void addCategory(String category) {
        group.add(category);
        Set<String> set=new HashSet<>(savedPairValues.getStringSet(getString(R.string.app_categories),null));
        set.add(category);
        appList.put(category,new ArrayList<ApplicationInfo>());
        SharedPreferences.Editor editor= savedPairValues.edit();
        editor.putStringSet(getString(R.string.app_categories),set);
        editor.apply();
        listAdapter.notifyDataSetChanged();

    }

    public void addAppsMode(View view){
        addAppsMode=true;
        ImageView imageView=(ImageView)view;
        imageView.setImageResource(R.drawable.cancel);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopAddApps();
            }
        });
        for(View groupView:listAdapter.getGroupViews()){
            if(groupView.findViewById(R.id.add_icon)!=view){
                ((ImageView)groupView.findViewById(R.id.add_icon)).setImageResource(R.drawable.blank);
                groupView.findViewById(R.id.add_icon).setOnClickListener(null);
            }else {
                addingAppsToCategory=((TextView)groupView.findViewById(R.id.lblListHeader)).getText().toString();
            }
            ((ImageView)groupView.findViewById(R.id.configure_icon)).setImageResource(R.drawable.blank);
            groupView.findViewById(R.id.configure_icon).setOnClickListener(null);

        }
    }

    public void stopAddApps(){
        addAppsMode=false;
        for(View groupView:listAdapter.getGroupViews()){
            ((ImageView)groupView.findViewById(R.id.add_icon)).setImageResource(R.drawable.add_icon);
            groupView.findViewById(R.id.add_icon).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addAppsMode(v);
                }
            });
            ((ImageView)groupView.findViewById(R.id.configure_icon)).setImageResource(R.drawable.tools);
            groupView.findViewById(R.id.configure_icon).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    configureCategory(v);
                }
            });

        }
    }

    public void configureCategory(View view){
        AlertDialog.Builder builderThird = new AlertDialog.Builder(this);
        builderThird.setMessage("BEEEEE");
        builderThird.setNegativeButton("Abbruch", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        AlertDialog dialogThird=builderThird.create();
        dialogThird.show();
    }

    private class LoadApplications extends AsyncTask<Void, Void, Void> {
        private ProgressDialog progress = null;

        @Override
        protected Void doInBackground(Void... params) {
            installedApps = checkForLaunchIntent(pm.getInstalledApplications(PackageManager.GET_META_DATA));
            orderAppsIntoCategories();
            listAdapter = new ExpandableListAdapter(AppListActivity.this,
                    group, appList);

            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        @Override
        protected void onPostExecute(Void result) {
            expandableListView.setAdapter(listAdapter);
            expandableListView.setOnChildClickListener(appListClickListner);
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
