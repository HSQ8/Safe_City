package com.example.shotspotter;

/*
 * adapted from https://github.com/halibobo/SoundMeter
 */


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.gigamole.library.PulseView;
// import com.dnkilic.waveform.WaveView;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private SoundMediaHandler mySoundMediaHandler;
    private static final int soundMSG = 0x1001;
    private static final int refreshTime = 10;

    private static String fileName = null;
    private int counter = 0, clearcounter = 201;

    private double latitude, longitude, altitude;
    private long time;

    private LocationManager mylocationmanager;
    private LocationListener mylocationListener;
    private RequestQueue queue;
    private LinearLayoutCompat baselayout;

    private float volume = 10000;
    private TextView volumeText;
    private TextView gpsText;
    private TextView httpText;
    // private WaveView waveview;
    private PulseView mypulse;

    public final int MY_PERMISSIONS_RECORD_AUDIO = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View decorView = getWindow().getDecorView();
        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);


        // add record permission
        get_permission(Manifest.permission.RECORD_AUDIO);
        get_permission(Manifest.permission.ACCESS_FINE_LOCATION);
        get_permission(Manifest.permission.ACCESS_COARSE_LOCATION);
        get_permission(Manifest.permission.INTERNET);


        volumeText = (TextView) findViewById(R.id.volumeText);
        gpsText = (TextView) findViewById(R.id.gpsText);
        httpText = (TextView) findViewById(R.id.httpText);
        mySoundMediaHandler = new SoundMediaHandler();

        fileName = this.getCacheDir().getAbsolutePath();
        fileName += "/audiorecordtest.3gp";
        startRecord(fileName);
        initialize_pulse();


        baselayout = (LinearLayoutCompat) findViewById(R.id.baselayout);
        mylocationmanager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        mylocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (location != null) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    altitude = location.getAltitude();

                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        mylocationmanager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mylocationListener);
        queue = Volley.newRequestQueue(this);



    }

    private void initialize_pulse(){
        /*
        waveview = findViewById(R.id.waveView);
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        waveview.initialize(dm);
        waveview.speechStarted();

        waveview.speechStarted();
*/
        mypulse = (PulseView) findViewById(R.id.pulseview);
        mypulse.setPulseColor(Color.WHITE);
        mypulse.setIconHeight(400);
        mypulse.setIconWidth(400);
        mypulse.setPulseCount(3);
        mypulse.setPulseAlpha(70);
        mypulse.startPulse();

    }


    private void get_permission(String permission){
        if (ActivityCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    permission
            },MY_PERMISSIONS_RECORD_AUDIO);
        }
    }


    public void startRecord(String filename){
        System.out.println("starting recorder");
        try{

            if (mySoundMediaHandler.startRecorder(filename)) {
                startListenAudio();
            }else{
                Toast.makeText(this, "Failed start Sound Recorder", Toast.LENGTH_SHORT).show();
            }
        }catch(Exception e){
            Toast.makeText(this, "failed to acquire lock on recorder", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            Log.v(SoundData.APP_TAG, "STACKTRACE");
            Log.v(SoundData.APP_TAG, Log.getStackTraceString(e));
        }
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (this.hasMessages(soundMSG)) {
                return;
            }
            volume = mySoundMediaHandler.getMaxAmplitude();
            time = System.currentTimeMillis();
            if(volume > 0 && volume < 1000000) {
                SoundData.setDbCount(20 * (float) (Math.log10(volume))); // convert to decibel
                // refresh textview here
                volumeText.setText(Float.toString(SoundData.dbCount));
            }
            if (counter == 500){
                get_coords();
                counter = 0;
                counter++;
            }else{
                counter++;
            }
            if (clearcounter < 200){
            clearcounter++;
            }else if(clearcounter == 200) {
                mypulse.setPulseColor(Color.WHITE);
                clearcounter ++;
            }

            String stringLatitude = String.valueOf(latitude);
            String stringLongitude = String.valueOf(longitude);
            gpsText.setText(stringLatitude + " : " + stringLongitude);

            if(SoundData.dbCount > 85f && latitude != 0){
                send_coords(SoundData.dbCount);
            }

            handler.sendEmptyMessageDelayed(soundMSG, refreshTime);
        }
    };

    private void get_coords(){

        String url ="https://jasonli.us/cgi-bin/hacktech2019_return.cgi?";
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        httpText.setText("Response is: "+ response);
                        if (response.equals(",")){
                            return;
                        }

                        List<String> coords = Arrays.asList(response.split(","));
                        float lat = Float.parseFloat(coords.get(0));
                        float longi = Float.parseFloat(coords.get(1));


                        String label = "Danger Here!";
                        String uriBegin = "geo:" + response;
                        String query = lat + "," + longi + "(" + label + ")";
                        String encodedQuery = Uri.encode(query);
                        String uriString = uriBegin + "?q=" + encodedQuery + "&z=16";



                        SoundData.coords = uriString;


                        Snackbar mysnackbar = Snackbar.make(baselayout,"Gunshot Detected",Snackbar.LENGTH_INDEFINITE).setAction("laucnhing", new View.OnClickListener()
                                {
                                    @Override
                                    public void onClick(View v) {
                                        Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                                                Uri.parse("geo:"+SoundData.coords));
                                        startActivity(intent);
                                    }

                                }
                        );
                        mysnackbar.show();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                httpText.setText("That didn't work!");
            }
        });

// Add the request to the RequestQueue.
        queue.add(stringRequest);

    }
    private void send_coords(float magnitude){

        mypulse.setPulseColor(Color.BLACK);
        clearcounter = 1;

        String url ="https://jasonli.us/cgi-bin/hacktech2019.cgi?";
        url = url + "m=" + String.valueOf(magnitude) + "&lat=" + latitude + "&long=" + longitude + "&t=" + String.valueOf(time) + "&alt=" + String.valueOf(altitude);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        httpText.setText("Response is: "+ response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                httpText.setText("That didn't work!");
            }
        });

// Add the request to the RequestQueue.
        queue.add(stringRequest);
        Snackbar alert = Snackbar.make(baselayout,"Threat Detected",Snackbar.LENGTH_LONG);
        alert.show();

    }

    private void startListenAudio() {
        handler.sendEmptyMessageDelayed(soundMSG, refreshTime);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mySoundMediaHandler.stopRecording();
    }
}
