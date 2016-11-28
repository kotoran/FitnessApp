package com.ajemian.fitnessapp;

import android.util.Log;

/**
 * Created by Kudo.
 */

public class StepsConverter {
    public static double getCaloriesFromSteps(int pounds, int steps){
        double caloriesPerMile = (double) 0.57 * (double) pounds;
        Log.d("Andrew", Double.toString(caloriesPerMile * (double) (steps / 2000)));
        return caloriesPerMile * ((double)steps / 2000);
//2000 steps is one mile
    }
    public static double getKilometersFromSteps(int steps){
        return (((double)steps) / ((double)1320)); /// 1320;
    }
}
