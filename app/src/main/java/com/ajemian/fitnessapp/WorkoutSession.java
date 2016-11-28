package com.ajemian.fitnessapp;

import java.util.Date;

/**
 * Created by Kudo.
 */

public class WorkoutSession {
    private Date start;
    private Date end;
    public WorkoutSession(){
        start = new Date();

    }

    public void setEndDate(){
        end = new Date();
    }
    public Date getEndDate(){
        return end;
    }
    public int getSteps(){
        return steps;
    }

    public Date getStartDate(){
        return start;
    }
    private int steps;

    public void incrementSteps(){
        steps++;
    }

    public void setSteps(int steps){
        this.steps = steps;
    }
    public void changeStartDate(long time){
        start = new Date(time);
    }

    public void changeEndDate(long time){
        end = new Date(time);
    }


}
