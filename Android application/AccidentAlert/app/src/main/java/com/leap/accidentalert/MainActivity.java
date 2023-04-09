package com.leap.accidentalert;

import static android.Manifest.permission.CALL_PHONE;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.arch.core.util.Function;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final Location TODO = null;
    private FusedLocationProviderClient client;
    public static Button WebSocketButton;
    DatabaseHandler myDB;
    private static final int REQUEST_CHECK_CODE = 8989;
    private LocationSettingsRequest.Builder builder;
    String x = "", y = "";
    static Function callback;
    private static final int REQUEST_LOCATION = 1;
    LocationManager locationManager;
    Intent mIntent;
    public static MainActivity activity;
    LocationManager mLocationManager;
    WebSocketClientL webSocketClientL;
    boolean ACCIDENT;
    boolean waiting;
    public static int i = 0;
        public static final int REQUEST_CODE=10;
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            activity=MainActivity.this;





            float[] gravityV = new float[3];
            final float[] x = new float[1];
            final float[] y = new float[1];
            final float[] z = new float[1];
            SensorEventListener accelerationListener = new SensorEventListener() {
                @Override
                public void onAccuracyChanged(Sensor sensor, int acc) {
                }
                @Override
                public void onSensorChanged(SensorEvent event) {
                    final float alpha = 0.8f;
//gravity is calculated here

                    if(webSocketClientL!=null){
                        gravityV[0] = alpha * gravityV[0] + (1 - alpha) * event.values[0];
                        gravityV[1] = alpha * gravityV[1] + (1 - alpha)* event.values[1];
                        gravityV[2] = alpha * gravityV[2] + (1 - alpha) * event.values[2];
    //acceleration retrieved from the event and the gravity is removed
                        x[0] = event.values[0] - gravityV[0];
                        y[0] = event.values[1] - gravityV[1];
                        z[0] = event.values[2] - gravityV[2];

                        if(!waiting && (Math.abs(x[0])>3 || Math.abs(x[0])>3|| Math.abs(x[0])>3)){
                            webSocketClientL.client.send("acc");
                            ACCIDENT=true;
                            waiting=true;
                            Log.d(">>>>>","accident sent");
                        }else{
                            Log.d(">>>>>","-------->");
                            if(ACCIDENT){
                                ACCIDENT=false;
                                new Thread(){
                                    public void run(){
                                        try {
                                            Log.d(">>>>>","-----jjjjjjjjjjj--->");
                                            Thread.sleep(60000);

                                            webSocketClientL.client.send("nacc");
                                            waiting =false;
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }.start();
                            }
                        }
                        if(Math.abs(x[0])>20 || Math.abs(x[0])>20|| Math.abs(x[0])>20)
                                MainActivity.this.NotifyOverspeed();
                    }

                }
            };

            SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            Sensor sensor = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);

            sensorManager.registerListener(accelerationListener,sensor, SensorManager.SENSOR_DELAY_GAME);








            startService(new Intent(this,NotifyService.class));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel("TrikkuAlert", "Triku Emergency Alert", NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("routine notify chanel");
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }

            Button addContact = findViewById(R.id.button);
            Button accident = findViewById(R.id.accident);
            Button minoraccident = findViewById(R.id.minoraccident);
            minoraccident.setOnClickListener(v->{
                    if(webSocketClientL!=null){
                        webSocketClientL.client.send("minor accident");
                        loadData("I am safe .\nNo problems.\nSorry for the interuption");
                    }
                    else Toast.makeText(MainActivity.this, "ESP32 not connected", Toast.LENGTH_SHORT).show();
            });
            WebSocketButton=findViewById(R.id.websocketbutton1);
            WebSocketButton.setOnClickListener(v->{
                webSocketClientL = new WebSocketClientL("ws://192.168.1.1/ws");
                Toast.makeText(this, "Connecting....", Toast.LENGTH_SHORT).show();
            });
            if (ContextCompat.checkSelfPermission(this,android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,  new String[]{Manifest.permission.CALL_PHONE,Manifest.permission.SEND_SMS,Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_CODE);
            }



            myDB = new DatabaseHandler(this);

            final MediaPlayer mp = MediaPlayer.create(getApplicationContext(), R.raw.mixkit_retro_emergency_notification_alarm_2970);

            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) onGPS();
            else startTrack();

            addContact.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), Register.class)));
            accident.setOnClickListener(v -> accident());
        }
    public void accident() {
        loadData("please help me.\nI met with an accident.\n My location is  https://maps.google.com/maps?q=loc:" + x + "," + y);
    }
    public void NotifyConnected(){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,"TrikkuAlert")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Trikku Alert")
                .setContentText("Connected to Trikku Emergency Alert System")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(new Random().nextInt(), builder.build());
    }

    public void NotifyAccident(){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,"TrikkuAlert")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Alert")
                .setContentText("Accident occured")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(new Random().nextInt(), builder.build());
        accident();
    }

    public void NotifyOverspeed(){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,"TrikkuAlert")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Alert")
                .setContentText("Overspeed alert")
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(new Random().nextInt(), builder.build());
    }

    private void loadData(String msg) {
        ArrayList<String> thelist = new ArrayList<>();
        Cursor data = myDB.getListContents();
        if (data.getCount()!=0) {
            Long fnum=null;
            while (data.moveToNext()) {
                if(fnum==null) fnum = Long.parseLong(data.getString(1));
                sendSMS("+91"+data.getString(1),msg);
            }
            if(fnum!=null ) call(fnum);
            else Toast.makeText(this, "No contacts available", Toast.LENGTH_SHORT).show();
        }
        else Toast.makeText(MainActivity.this, "No contacts available", Toast.LENGTH_SHORT).show();
    }

    private void sendSMS(String number, String msg) {
        SmsManager.getDefault().sendTextMessage(number, null,msg, null, null);
    }

    private void call(long call) {
        Intent i = new Intent(Intent.ACTION_CALL);
        i.setData(Uri.parse("tel:" + call));
        if (ContextCompat.checkSelfPermission(getApplicationContext(), CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            startActivity(i);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{CALL_PHONE}, 1);
            }
        }
    }
    
    

    private void startTrack() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location locationGPS) {
                            // GPS location can be null if GPS is switched off
                            if (locationGPS != null) {
                                double lat = locationGPS.getLatitude();
                                double lon = locationGPS.getLongitude();
                                x = String.valueOf(lat);
                                y = String.valueOf(lon);
                            } else {
                                Toast.makeText(MainActivity.this, "UNABLE TO FIND LOCATION", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d("MapDemoActivity", "Error trying to get last GPS location");
                            e.printStackTrace();
                        }
                    });
        }
    }

    private void onGPS() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Enable GPS").setCancelable(false).setPositiveButton("yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

    }

    private ArrayList<String> loadContact() {
        ArrayList<String> theList = new ArrayList<>();
        Cursor data = myDB.getListContents();
        if (data.getCount() == 0) {
            Toast.makeText(MainActivity.this, "There is no content", Toast.LENGTH_SHORT).show();
        } else {
            while (data.moveToNext()) {
                theList.add(data.getString(1));
            }
        }
        return theList;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_POWER) {
            i++;
            if (i == 2) {
                //do something
                Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + "102301"));
                startActivity(intent);
                i = 0;
            }

        }
        return super.onKeyDown(keyCode, event);
    }

}