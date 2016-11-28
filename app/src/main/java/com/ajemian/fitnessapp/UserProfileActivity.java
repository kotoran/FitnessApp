package com.ajemian.fitnessapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by Kudo.
 */

public class UserProfileActivity extends AppCompatActivity implements View.OnClickListener{
    private TextView distanceWeekly;
    private TextView timeWeekly;
    private TextView workoutsWeekly;
    private TextView caloriesWeekly;

    private TextView distanceOverall;
    private TextView timeOverall;
    private TextView workoutsOverall;
    private TextView caloriesOverall;


    private EditText nameEditText;
    private Spinner genderSpinner;
    private EditText poundsEditText;

    private Button saveButton;


    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        saveButton = (Button) findViewById(R.id.saveButton);

        distanceWeekly = (TextView) findViewById(R.id.distanceWeekly);
        timeWeekly = (TextView) findViewById(R.id.timeWeekly);
        workoutsWeekly = (TextView) findViewById(R.id.workoutsWeekly);
        caloriesWeekly = (TextView) findViewById(R.id.caloriesWeekly);

        distanceOverall = (TextView) findViewById(R.id.distanceOverall);
        timeOverall = (TextView) findViewById(R.id.timeOverall);
        workoutsOverall = (TextView) findViewById(R.id.workoutsOverall);
        caloriesOverall = (TextView) findViewById(R.id.caloriesOverall);

        nameEditText = (EditText) findViewById(R.id.nameEditText);
        genderSpinner = (Spinner) findViewById(R.id.genderSpinner);
        poundsEditText = (EditText) findViewById(R.id.weightEditText);

        FitnessSingleton singleton = FitnessSingleton.getInstance();

        nameEditText.setText(singleton.getName());

        if(singleton.getGender().equals("Male")) genderSpinner.setSelection(0);
        if(singleton.getGender().equals("Female")) genderSpinner.setSelection(1);

        poundsEditText.setText(Integer.toString(singleton.getPounds()));

        Date sevenDaysPrior = new Date(System.currentTimeMillis() - (7 * 1000 * 60 * 60 * 24));

        ArrayList<WorkoutSession> copySessions = new ArrayList<WorkoutSession>(singleton.getSessions());
        long weeklyTime = 0;
        long overallTime = 0;

        double weeklyCalories = 0;
        double overallCalories = 0;

        double weeklyDistance = 0;
        double overallDistance = 0;

        int weeklyWorkouts = 0;
        int overallWorkouts = copySessions.size();

        int pounds = singleton.getPounds();
        for(int i=0; i<copySessions.size(); i++){
            if(copySessions.get(i).getStartDate().getTime() >= sevenDaysPrior.getTime()){

                weeklyTime = weeklyTime + TimeUnit.MILLISECONDS.toMinutes(copySessions.get(i).getEndDate().getTime() -
                        copySessions.get(i).getStartDate().getTime());

                weeklyWorkouts++;
                weeklyCalories = weeklyCalories + StepsConverter.getCaloriesFromSteps(pounds, copySessions.get(i).getSteps());
                weeklyDistance = weeklyDistance + StepsConverter.getKilometersFromSteps(copySessions.get(i).getSteps());
            }
            overallTime = overallTime + TimeUnit.MILLISECONDS.toMinutes(copySessions.get(i).getEndDate().getTime() -
                    copySessions.get(i).getStartDate().getTime());
            overallCalories = overallCalories + StepsConverter.getCaloriesFromSteps(pounds, copySessions.get(i).getSteps());
            overallDistance = overallDistance + StepsConverter.getKilometersFromSteps(copySessions.get(i).getSteps());
        }

        distanceWeekly.setText(new DecimalFormat("#.##").format(weeklyDistance) + " km(s)");
        distanceOverall.setText(new DecimalFormat("#.##").format(overallDistance) + " km(s)");

        caloriesWeekly.setText(new DecimalFormat("#.##").format(weeklyCalories) + " calories");
        caloriesOverall.setText(new DecimalFormat("#.##").format(overallCalories) + " calories");

        timeWeekly.setText(new DecimalFormat("#.##").format(weeklyTime) + " minutes(s)");
        timeOverall.setText(new DecimalFormat("#.##").format(overallTime) + " minutes(s)");

        workoutsWeekly.setText(weeklyWorkouts + " workout(s)");
        workoutsOverall.setText(overallWorkouts + " workout(s)");


        saveButton.setOnClickListener(this);





    }


    @Override
    public void onClick(View view) {
        try{
            String name = (String) nameEditText.getEditableText().toString();
            String gender = genderSpinner.getSelectedItem().toString();
            int pounds = Integer.parseInt(poundsEditText.getEditableText().toString());

            FitnessSingleton.getInstance().setName(name);
            FitnessSingleton.getInstance().setGender(gender);
            FitnessSingleton.getInstance().setPounds(pounds);

            Toast.makeText(this, "Saved information successfully!", Toast.LENGTH_LONG).show();


        }catch(Exception e){
            Toast.makeText(this, "There was an error, resetting to default values!", Toast.LENGTH_LONG).show();
            FitnessSingleton.getInstance().setDefaultInformation();

            nameEditText.setText(FitnessSingleton.getInstance().getName());

            if(FitnessSingleton.getInstance().getGender().equals("Male")) genderSpinner.setSelection(0);
            if(FitnessSingleton.getInstance().getGender().equals("Female")) genderSpinner.setSelection(1);

            poundsEditText.setText(Integer.toString(FitnessSingleton.getInstance().getPounds()));
        }
        FitnessSingleton.getInstance().saveInformation();



    }
}
