package com.leap.accidentalert;

import static android.Manifest.permission.CALL_PHONE;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
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
    private static final int REQUEST_LOCATION = 1;

    LocationManager locationManager;
    Intent mIntent;
    public static MainActivity activity;
    LocationManager mLocationManager;
    public static int i = 0;
        public static final int REQUEST_CODE=10;
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            activity=MainActivity.this;

            startService(new Intent(this,NotifyService.class));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel("TrikkuAlert", "Triku Emergency Alert", NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("routine notify chanel");
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }

            Button addContact = findViewById(R.id.button);
            Button accident = findViewById(R.id.accident);
            WebSocketButton=findViewById(R.id.websocketbutton1);
            WebSocketButton.setOnClickListener(v->{
                WebSocketClientL client = new WebSocketClientL("ws://192.168.1.1/ws");
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
        loadData();
    }
    public void NotifyConnected(){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,"TrikkuAlert")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Notification")
                .setContentText("hello world")
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

    private void loadData() {
        ArrayList<String> thelist = new ArrayList<>();
        Cursor data = myDB.getListContents();
        String msg = "please help me.\nI met with an accident.\n My location is  https://maps.google.com/maps?q=loc:" + x + "," + y;
        if (data.getCount()!=0) {
            Long fnum=null;
            while (data.moveToNext()) {
                if(fnum==null) fnum = Long.parseLong(data.getString(1));
                sendSMS("+91"+data.getString(1),msg);
            }
            if(fnum!=null) call(fnum);
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