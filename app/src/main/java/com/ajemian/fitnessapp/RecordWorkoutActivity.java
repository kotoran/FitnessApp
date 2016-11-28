package com.ajemian.fitnessapp;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RecordWorkoutActivity extends AppCompatActivity implements View.OnClickListener, ILocationReceiver, IStepReceiver, IStatsReceiver {

    private Button profileButton;
    private Button workoutButton;
    private SQLiteDatabase database;
    private boolean refresh;

    private GoogleMap mMap;
    private LineChart lineChart;

    private ArrayList<String> labels;

    //portrait objects
    private TextView distanceTextView;
    private TextView durationTextView;

    //Landscape objects
    private TextView averageTextView;
    private TextView minTextView;
    private TextView maxTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_workout);

        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){

            distanceTextView = (TextView)findViewById(R.id.distanceTextView);
            durationTextView = (TextView)findViewById(R.id.durationTextView);

            distanceTextView.setText("N/A");
            durationTextView.setText("N/A");

            FitnessSingleton.getInstance().registerLocationReceiver(this);

            profileButton = (Button) findViewById(R.id.profileButton);
            profileButton.setOnClickListener(this);

            workoutButton = (Button) findViewById(R.id.startWorkoutButton);
            workoutButton.setOnClickListener(this);
            if(FitnessSingleton.getInstance().getWorkout()){
                startUpdateOfDistanceAndDuration();
                workoutButton.setText("Stop Workout");
            }



            WorkoutMapFragment workoutMapFragment = new WorkoutMapFragment();
            FragmentManager fragmentManager = this.getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.mapFragmentLinearLayout, workoutMapFragment);
            fragmentTransaction.commit();

            initMapWithExistingPoints();

            workoutMapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    mMap = googleMap;
                    mMap.setMyLocationEnabled(true);
                    initMapWithExistingPoints();

                }
            });
        }else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            minTextView = (TextView) findViewById(R.id.minTextView);
            maxTextView = (TextView) findViewById(R.id.maxTextView);
            averageTextView = (TextView) findViewById(R.id.averageTextView);

            updateCalories();
            FitnessSingleton.getInstance().registerStepListener(this);
            FitnessSingleton.getInstance().registerStatsListener(this);

            labels = new ArrayList<String>();
            labels.add("5 min");
            labels.add("10 min");
            labels.add("15 min");
            labels.add("20 min");
            labels.add("25 min");
            labels.add("30 min");
            labels.add("35 min");
            labels.add("40 min");
            labels.add("45 min");
            labels.add("50 min");
            labels.add("55 min");
            labels.add("60 min");

            lineChart = (LineChart)this.findViewById(R.id.workoutChart);
            applyEntriesToChart(getEntriesFromCurrentSteps());

        }
        Log.d("meow", "still meow?");


    }
    protected void applyEntriesToChart(ArrayList<Entry> entries){
        LineDataSet dataset = new LineDataSet(entries, "Steps");
        dataset.setDrawCubic(true);
        dataset.setDrawFilled(true);
        LineData lineData = new LineData(labels, dataset);
        lineChart.setData(lineData);
        lineChart.setScrollX(0);
        //lineChart.invalidate();

    }
    protected ArrayList<Entry> getEntriesFromCurrentSteps(){
        FitnessSingleton singleton = FitnessSingleton.getInstance();
        ArrayList<Entry> r = new ArrayList();
        ArrayList<Integer> copySteps = new ArrayList<Integer>(singleton.stepsPer5Minutes);
        for(int i=0; i<copySteps.size(); i++){
            r.add(new Entry(copySteps.get(i), i));
        }
        return r;
    }
    protected void initMapWithExistingPoints(){
        if(mMap != null) {
            if(FitnessSingleton.getInstance().lastLocation != null){
                CameraUpdate center=
                        CameraUpdateFactory.newLatLng(new LatLng(FitnessSingleton.getInstance().lastLocation.getLatitude(),
                                FitnessSingleton.getInstance().lastLocation.getLongitude()));


                CameraUpdate zoom= CameraUpdateFactory.zoomTo(18);

                mMap.moveCamera(center);
                mMap.animateCamera(zoom);
            }
            if (FitnessSingleton.getInstance().polylines.size() > 0) {
                for (int i = 0; i < FitnessSingleton.getInstance().polylines.size(); i++) {
                    List<LatLng> points = FitnessSingleton.getInstance().polylines.get(i).getPoints();
                    if (points.size() > 1) {
                        mMap.addPolyline(new PolylineOptions()
                                .add(points.get(0), points.get(1))
                                .width(7)
                                .color(Color.RED));
                    }
                }
            }
        }
    }
    @Override
    protected void onResume(){
        super.onResume();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        FitnessSingleton.getInstance().removeLocationReceiver(this);
        FitnessSingleton.getInstance().removeStepListener(this);
        FitnessSingleton.getInstance().removeStatsReceiver(this);

    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.profileButton){
            Intent profileIntent = new Intent(this, UserProfileActivity.class);
            startActivity(profileIntent);
        }else if (view.getId() == R.id.startWorkoutButton){
            if(!FitnessSingleton.getInstance().getWorkout()){
                for(int i=0; i<FitnessSingleton.getInstance().polylines.size(); i++){
                    FitnessSingleton.getInstance().polylines.get(i).remove();
                }
                FitnessSingleton.getInstance().polylines.clear();
                FitnessSingleton.getInstance().startWorkout();
                startUpdateOfDistanceAndDuration();
                workoutButton.setText("Stop Workout");

            }else{
                FitnessSingleton.getInstance().stopWorkout();
                workoutButton.setText("Start Workout");

            }
        }
    }
    protected void updateCalories(){
        Log.d("Andrew", "Updating calories");

        FitnessSingleton singleton = FitnessSingleton.getInstance();
        Log.d("Andrew", "Hello " +  Double.toString(singleton.getCaloriesMinute()));
        Log.d("Andrew", "Hello " + Double.toString(singleton.getMinCalories()));
        Log.d("Andrew", "Hello " +  Double.toString(singleton.getMaxCalories()));
        averageTextView.setText("AVERAGE: " + new DecimalFormat("#.##").format(singleton.getCaloriesMinute()) + " cal/min");
        minTextView.setText("MIN: " + new DecimalFormat("#.##").format(singleton.getMinCalories()) + " cal/min");
        maxTextView.setText("MAX: " + new DecimalFormat("#.##").format(singleton.getMaxCalories()) + " cal/min");

    }

    protected void startUpdateOfDistanceAndDuration(){
        final FitnessSingleton singleton = FitnessSingleton.getInstance();
        //for distance
        new Thread(new Runnable(){
            @Override
            public void run(){
                while(singleton.getWorkout()) {
                    RecordWorkoutActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            distanceTextView.setText(new DecimalFormat("#.##").format(StepsConverter.getKilometersFromSteps(singleton.getStepsOfCurrentSession())));


                        }
                    });
                    try {
                        Thread.sleep(1000); //updates every 5 seconds
                    }catch(Exception e){

                    }
                }

            }
        }).start();
        //for duration
        new Thread(new Runnable(){
            @Override
            public void run(){
                while(singleton.getWorkout()) {
                    RecordWorkoutActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            durationTextView.setText(Long.toString( TimeUnit.MILLISECONDS.toMinutes(new Date().getTime() -
                                    singleton.getStartDateOfCurrentSession().getTime())));
                        }
                    });
                    try {
                        Thread.sleep(60000); //updates every 1 second
                    }catch(Exception e){

                    }
                }

            }
        }).start();
    }

    @Override
    public void handleLocation(final Location location) {

        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            Log.d("meow", "I'm handling it!");
            if(FitnessSingleton.getInstance().lastLocation == null){
                new Thread(new Runnable(){
                    @Override
                    public void run(){
                        while(mMap == null){
                            try {
                                Thread.sleep(200);
                            }catch(Exception e){

                            }
                        }

                        FitnessSingleton.getInstance().lastLocation = location;


                        RecordWorkoutActivity.this.runOnUiThread(new Runnable(){
                            @Override
                            public void run(){
                                CameraUpdate center=
                                        CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(),
                                                location.getLongitude()));

                                //Polyline line = mMap.addPolyline(new PolylineOptions()
                                // .add(new LatLng(37.250133,
                                //         -121.849374), new LatLng(37.250133,
                                //     -120.302040))
                                //  .width(5)
                                //.color(Color.RED));


                                CameraUpdate zoom= CameraUpdateFactory.zoomTo(18);

                                mMap.moveCamera(center);
                                mMap.animateCamera(zoom);
                            }
                        });


                    }
                }).start();

            }
            else
            {
                try {

                    CameraUpdate center =
                            CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(),
                                    location.getLongitude()));
                    if (FitnessSingleton.getInstance().getWorkout()) {
                        Polyline line = mMap.addPolyline(new PolylineOptions()
                                .add(new LatLng(FitnessSingleton.getInstance().lastLocation.getLatitude(),
                                        FitnessSingleton.getInstance().lastLocation.getLongitude()), new LatLng(location.getLatitude(),
                                        location.getLongitude()))
                                .width(7)
                                .color(Color.RED));
                        FitnessSingleton.getInstance().polylines.add(line);
                    }


                    CameraUpdate zoom = CameraUpdateFactory.zoomTo(18);
                    mMap.moveCamera(center);
                    mMap.animateCamera(zoom);
                    FitnessSingleton.getInstance().lastLocation = location;
                }catch(Exception e){
                    Log.d("meow", "meow?");
                }
            }
        }


    }

    @Override
    public void handleStep() {
        applyEntriesToChart(getEntriesFromCurrentSteps());
        lineChart.invalidate();
    }

    @Override
    public void handleStats() {
        final FitnessSingleton singleton = FitnessSingleton.getInstance();
        RecordWorkoutActivity.this.runOnUiThread(new Runnable(){
            @Override
            public void run(){
                updateCalories();
            }
        });

    }
}
