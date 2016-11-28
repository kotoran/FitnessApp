package com.ajemian.fitnessapp;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Kudo.
 */
public class FitnessSingleton {
    public final int DEFAULT_POUNDS = 130;
    public final String DEFAULT_GENDER = "Male";
    public final String DEFAULT_NAME = "John Doe";

    private static FitnessSingleton ourInstance = new FitnessSingleton();

    public static FitnessSingleton getInstance() {
        return ourInstance;
    }
    ArrayList<LatLng> geoPoints;
    ArrayList<ILocationReceiver> receivers;
    ArrayList<IStepReceiver> stepReceivers;
    ArrayList<IStatsReceiver> statsReceivers;
    ArrayList<WorkoutSession> sessions;
    private WorkoutSession currentSession;

    public ArrayList<Polyline> polylines;
    public Location lastLocation;

    public LinkedList<Integer> stepsPer5Minutes;
    private Thread stepThread;
    private int currentStepsIn5Minutes;
    private double currentCaloriesAMinute;
    private double currentMinCaloriesAMinute;
    private double currentMaxCaloriesAMinute;
    private int currentStepsInAMinute;
    private String name;
    private int pounds;
    private String gender;

    private SQLiteDatabase database;

    public void setDefaultInformation(){
        this.name = DEFAULT_NAME;
        this.gender = DEFAULT_GENDER;
        this.pounds = DEFAULT_POUNDS;
    }

    public void setDatabase(SQLiteDatabase database){
        this.database = database;
        database.execSQL("CREATE TABLE IF NOT EXISTS user (name VARCHAR(200), gender VARCHAR(20), pounds int(11));");
        database.execSQL("CREATE TABLE IF NOT EXISTS sessions (steps int(11), start int(8), end int(8));");
    }

    public void loadFromDatabase(){
        //loads all sessions from the database
        sessions.clear();

        Cursor userCursor = database.rawQuery("SELECT * FROM " + "user;" , null);
        int columnName = userCursor.getColumnIndex("name");
        int columnGender = userCursor.getColumnIndex("gender");
        int columnPounds = userCursor.getColumnIndex("pounds");
        userCursor.moveToFirst();



        if(userCursor != null) {
            if (userCursor.getCount() > 0) {
                do {
                    this.name = userCursor.getString(columnName);
                    this.gender = userCursor.getString(columnGender);
                    this.pounds = userCursor.getInt(columnPounds);
                } while (userCursor.moveToNext());
            } else {
                setDefaultInformation();
            }
        }

        Cursor c = database.rawQuery("SELECT * FROM " + "sessions;" , null);
        int columnSteps = c.getColumnIndex("steps");
        int columnStart = c.getColumnIndex("start");
        int columnEnd = c.getColumnIndex("end");
        c.moveToFirst();

        if (c != null){
            if(c.getCount() > 0) {
                do {
                    WorkoutSession session = new WorkoutSession();
                    String url = c.getString(columnSteps);
                    session.setSteps(c.getInt(columnSteps));
                    session.changeStartDate(c.getLong(columnStart));
                    session.changeEndDate(c.getLong(columnEnd));
                    sessions.add(session);
                } while (c.moveToNext());
            }
        }

    }
    public void saveToDatabase(){
        database.execSQL("INSERT INTO sessions VALUES(" + currentSession.getSteps() + ", " + currentSession.getStartDate().getTime() + ", "
        + currentSession.getEndDate().getTime() + ");");
        //saves the current session into the database
    }

    public void saveInformation(){
        database.execSQL("DELETE FROM user;");
        database.execSQL("INSERT INTO user VALUES('" + this.name + "', '" + this.gender + "', " + this.pounds + ");");
    }

    public String getName(){
        return name;

    }
    public int getPounds(){
        return pounds;
    }
    public String getGender(){
        return gender;
    }
    public void setName(String name){
        this.name = name;
    }

    public void setPounds(int pounds){
        this.pounds = pounds;
    }

    public void setGender(String gender){
        this.gender = gender;
    }

    public void setSessions(ArrayList<WorkoutSession> sessions){
        this.sessions = sessions;
    }
    public ArrayList<WorkoutSession> getSessions(){
        return sessions;
    }
    //only 12 allowed in queue

    public double getCaloriesMinute(){
        return currentCaloriesAMinute;
    }

    public double getMinCalories(){
        return currentMinCaloriesAMinute;
    }
    public double getMaxCalories(){
        return currentMaxCaloriesAMinute;
    }

    private boolean workoutInProgress;
    public int getStepsOfCurrentSession(){
        if(currentSession == null) return 0;
        return currentSession.getSteps();
    }

    public Date getStartDateOfCurrentSession(){
        if(currentSession == null) new Date();
        return currentSession.getStartDate();
    }


    public void startWorkout(){
        currentSession = new WorkoutSession();
        currentStepsIn5Minutes = 0;
        currentStepsInAMinute = 0;
        stepThread = new Thread(new Runnable(){
            @Override
            public void run(){

                int timeWaited = 0;
                while(workoutInProgress){

                    try {
                        Thread.sleep(1);
                        timeWaited++;
                        if(timeWaited % 6000 == 0){

                            currentCaloriesAMinute = StepsConverter.getCaloriesFromSteps(pounds, currentStepsInAMinute);

                            if(currentCaloriesAMinute > currentMaxCaloriesAMinute)
                                currentMaxCaloriesAMinute = currentCaloriesAMinute;
                            if(currentCaloriesAMinute < currentMinCaloriesAMinute || currentMinCaloriesAMinute == 0)
                                currentMinCaloriesAMinute = currentCaloriesAMinute;
                            Log.d("Andrew", Double.toString(currentStepsInAMinute));
                            Log.d("Andrew", "Retrieved " + Double.toString(StepsConverter.getCaloriesFromSteps(pounds, currentStepsInAMinute)));
                            Log.d("Andrew", Double.toString(currentCaloriesAMinute));
                            Log.d("Andrew", Double.toString(currentMaxCaloriesAMinute));
                            Log.d("Andrew", Double.toString(currentMinCaloriesAMinute));

                            currentStepsInAMinute = 0;
                            notifyStatsListeners();
                        }
                        if(timeWaited > 299999){//299999
                            stepsPer5Minutes.add(0, currentStepsIn5Minutes);
                            if(stepsPer5Minutes.size() > 12)
                                stepsPer5Minutes.remove(11);


                            notifyStepListeners();
                            currentStepsIn5Minutes = 0;
                            timeWaited = 0;
                        }
                    }catch(Exception e){

                    }

                }


            }
        });

        stepThread.start();
        workoutInProgress = true;
    }

    public void incrementStepsInCurrentSession(){
        if(currentSession != null && getWorkout()){
            currentSession.incrementSteps();
            currentStepsIn5Minutes++;
            currentStepsInAMinute++;
        }
    }

    public void stopWorkout(){
        stepThread.interrupt();
        workoutInProgress = false;
        currentSession.setEndDate();
        sessions.add(currentSession);
        saveToDatabase();
        currentSession = new WorkoutSession();

    }

    public boolean getWorkout(){
        return workoutInProgress;
    }

    private FitnessSingleton() {
        geoPoints = new ArrayList<LatLng>();
        receivers = new ArrayList<>();
        sessions = new ArrayList<WorkoutSession>();
        lastLocation = null;
        polylines = new ArrayList<Polyline>();
        stepsPer5Minutes = new LinkedList<Integer>();
        stepReceivers = new ArrayList<>();
        statsReceivers = new ArrayList<>();
        currentCaloriesAMinute = 0;
        currentMinCaloriesAMinute = 0;
        currentMaxCaloriesAMinute = 0;
        gender = "Female";
    }
    public void addGeoPoint(LatLng geopoint){
        geoPoints.add(geopoint);
    }
    public void registerLocationReceiver(ILocationReceiver receiver){
        receivers.add(receiver);
    }
    public void handleLocation(Location location){
        if(location != null)
        {
        addGeoPoint(new LatLng(location.getLatitude(), location.getLongitude()));
            for(int i=0; i<receivers.size(); i++)
            {
                receivers.get(i).handleLocation(location);
            }
        }
    }
    public void removeLocationReceiver(ILocationReceiver receiver){
        receivers.remove(receiver);
    }

    public void registerStepListener(IStepReceiver receiver){
        stepReceivers.add(receiver);
    }
    public void removeStepListener(IStepReceiver receiver){
        stepReceivers.remove(receiver);
    }
    public void notifyStepListeners(){
        for(int i=0; i<stepReceivers.size(); i++){
            stepReceivers.get(i).handleStep();
        }
    }

    public void registerStatsListener(IStatsReceiver receiver){
        statsReceivers.add(receiver);
    }
    public void removeStatsReceiver(IStatsReceiver receiver){
        statsReceivers.remove(receiver);
    }
    public void notifyStatsListeners(){
        for(int i=0; i<statsReceivers.size(); i++){
            statsReceivers.get(i).handleStats();
        }
    }
}
