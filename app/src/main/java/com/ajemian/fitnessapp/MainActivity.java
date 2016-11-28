package com.ajemian.fitnessapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * Created by Kudo.
 */

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        FitnessSingleton.getInstance().setDatabase(this.openOrCreateDatabase("fitness.db", Context.MODE_PRIVATE, null));
        FitnessSingleton.getInstance().loadFromDatabase();

        Intent serviceIntent = new Intent(this, WorkoutService.class);
        startService(serviceIntent);

        Intent activityIntent = new Intent(this, RecordWorkoutActivity.class);
        startActivity(activityIntent);



    }
}
