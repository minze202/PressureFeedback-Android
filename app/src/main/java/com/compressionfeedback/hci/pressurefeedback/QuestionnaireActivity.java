package com.compressionfeedback.hci.pressurefeedback;


import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;

public class QuestionnaireActivity extends ListActivity {
    private DataCollection datas;
    private NotificationAdapter listadapter = null;


    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.questionnaire_layout);
        datas=DataCollection.getInstance();
        new LoadNotifications().execute();
    }

    @Override
    public void onResume(){
        super.onResume();
    }

    public void saveAnswers(View view) {
        datas.clear();
        finish();
    }

    @Override
    public void onBackPressed(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(QuestionnaireActivity.this);

        alertDialogBuilder
                .setMessage("Bitte drücken sie auf Antwort speichern, um diese Aktivität zu beenden.")
                .setCancelable(false)
                .setPositiveButton("Ich habe verstanden",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private class LoadNotifications extends AsyncTask<Void, Void, Void> {
        private ProgressDialog progress = null;

        @Override
        protected Void doInBackground(Void... params) {
            listadapter = new NotificationAdapter(QuestionnaireActivity.this,
                    R.layout.question_layout, datas.getRelevantDataForTheDay());

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
            progress = ProgressDialog.show(QuestionnaireActivity.this, null,
                    "Loading notification info...");
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
