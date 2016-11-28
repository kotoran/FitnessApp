package com.ajemian.fitnessapp;

import android.location.Location;

/**
 * Created by Kudo.
 */

public interface ILocationReceiver {
    void handleLocation(Location location);
}
