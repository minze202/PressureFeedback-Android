package com.compressionfeedback.hci.pressurefeedback;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;


public class QuestionActivity extends Activity{

    private String rightAnswer;
    private String chosenAnswer;
    private boolean answeredRight=false;
    private String mode;
    private long id;
    private boolean missed=true;
    private DataCollection dataCollectionInstance;

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
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        Bundle b = getIntent().getExtras();
        rightAnswer=b.getString("rightAnswer");
        mode=b.getString("mode");
        id=b.getLong("id");
        setContentView(R.layout.question_layout);
        if(!mode.equals("compression")){
            ((ImageView)findViewById(R.id.imageView2)).setImageResource(R.drawable.pattern1_v);
            ((ImageView)findViewById(R.id.imageView3)).setImageResource(R.drawable.pattern2_v);
            ((ImageView)findViewById(R.id.imageView4)).setImageResource(R.drawable.pattern3_v);
            ((ImageView)findViewById(R.id.imageView6)).setImageResource(R.drawable.pattern5_v);
        }
        Intent dataCollectionServiceIntent = new Intent(this, DataCollection.class);
        bindService(dataCollectionServiceIntent, mDataCollectionServiceConnection, BIND_AUTO_CREATE);
        startService(dataCollectionServiceIntent);

    }

    public void chooseAnswer(View view){
        missed=false;
        chosenAnswer=((Button)view).getText().toString();
        if(chosenAnswer.equals(rightAnswer)){
            answeredRight=true;
        }
        dataCollectionInstance.updateStudyData(id,answeredRight,missed,chosenAnswer);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mDataCollectionServiceConnection);
        dataCollectionInstance = null;
    }

}
