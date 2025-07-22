package com.example.safety;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

public class MainActivity2 extends AppCompatActivity {
    Button b2, policeButton, sosOption, Camera_button;
    MaterialButton contactsButton;

    private FusedLocationProviderClient client;
    DatabaseHandler myDB;
    String x = "", y = "";
    private static final int REQUEST_LOCATION = 1;
    private static final int CALL_PERMISSION_REQUEST_CODE_ALERT = 2;
    LocationManager locationManager;
    MediaPlayer mp;
    private static final int SMS_PERMISSION_REQUEST_CODE = 100;
    private LocationCallback locationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        b2 = findViewById(R.id.button2);
        policeButton = findViewById(R.id.policeStationButton);
        sosOption = findViewById(R.id.sosOptionButton);
        Camera_button = findViewById(R.id.Spy_camera_detect);
        contactsButton = findViewById(R.id.button);

        sosOption.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity2.this, SOSActivity.class);
            startActivity(intent);
        });

        myDB = new DatabaseHandler(this);
        client = LocationServices.getFusedLocationProviderClient(this);
        mp = MediaPlayer.create(getApplicationContext(), R.raw.police_siren);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(2000)
                .setMaxUpdateDelayMillis(100)
                .build();

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            onGPS();
        } else {
            startLocationUpdates(locationRequest);
        }

        contactsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity2.this, Register.class);
            startActivity(intent);
        });

        // FIXED: The siren can now be toggled on and off.
        b2.setOnLongClickListener(v -> {
            if (mp.isPlaying()) {
                mp.pause();
                Toast.makeText(getApplicationContext(), "Panic Siren stopped", Toast.LENGTH_SHORT).show();
            } else {
                mp.start();
                Toast.makeText(getApplicationContext(), "Panic Siren started", Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        b2.setOnClickListener(v -> loadData());
        Camera_button.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), Camera_options.class);
            startActivity(i);
        });
        policeButton.setOnClickListener(v -> findNearbyPoliceStations());
    }

    private void findNearbyPoliceStations() {
        if (x.isEmpty() || y.isEmpty()) {
            Toast.makeText(this, "Location not available yet. Please wait.", Toast.LENGTH_SHORT).show();
            return;
        }
        // FIXED: Corrected the geo URI for searching nearby police stations.
        String geoUri = "geo:" + x + "," + y + "?q=police station";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(geoUri));
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mp != null) {
            mp.release();
        }
        stopLocationUpdates();
    }

    private void loadData() {
        // FIXED: Added a check to ensure location is available before sending an alert.
        if (x.isEmpty() || y.isEmpty()) {
            Toast.makeText(this, "Location not ready. Please wait a moment and try again.", Toast.LENGTH_LONG).show();
            return;
        }

        ArrayList<String> phoneNumbers = new ArrayList<>();
        // FIXED: Correctly close the cursor using a try-finally block.
        Cursor data = myDB.getListContents();
        try {
            if (data.getCount() == 0) {
                Toast.makeText(this, "No emergency contacts saved.", Toast.LENGTH_SHORT).show();
                return;
            }
            while (data.moveToNext()) {
                // FIXED: Get the phone number (column index 2), not the name (index 1).
                phoneNumbers.add(data.getString(2));
            }
        } finally {
            if (data != null) {
                data.close();
            }
        }

        if (!phoneNumbers.isEmpty()) {
            // FIXED: Corrected the Google Maps URL format.
            String msg = "I need help! My current location is: http://maps.google.com/?q=" + x + "," + y;
            String numberString = String.join(";", phoneNumbers);
            sendSms(numberString, msg);

            // FIXED: Call only the first emergency contact, not a hardcoded number inside a loop.
            call(phoneNumbers.get(0));
        }
    }

    private void sendSms(String numbers, String msg) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                SmsManager smsManager = SmsManager.getDefault();
                // The numbers string is already semicolon-separated
                for (String number : numbers.split(";")) {
                    smsManager.sendTextMessage(number, null, msg, null, null);
                }
                Toast.makeText(getApplicationContext(), "Alert SMS sent to contacts.", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "SMS failed to send.", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_REQUEST_CODE);
        }
    }

    // FIXED: The call method now takes a phone number as a parameter.
    private void call(String number) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            Intent i = new Intent(Intent.ACTION_CALL);
            i.setData(Uri.parse("tel:" + number));
            startActivity(i);
        } else {
            // Use a different request code to distinguish from other permission requests.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, CALL_PERMISSION_REQUEST_CODE_ALERT);
        }
    }

    private void startLocationUpdates(LocationRequest locationRequest) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
            return;
        }

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    x = String.valueOf(location.getLatitude());
                    y = String.valueOf(location.getLongitude());
                    Log.d("LocationUpdate", "Lat: " + x + ", Lon: " + y);
                }
            }
        };
        client.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        if (locationCallback != null) {
            client.removeLocationUpdates(locationCallback);
        }
    }

    private void onGPS() {
        new AlertDialog.Builder(this)
                .setMessage("Please enable GPS to use this app's safety features.")
                .setCancelable(false)
                .setPositiveButton("Enable GPS", (dialog, which) -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .create()
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Re-attempt to start location updates after permission is granted
                recreate(); // Simple way to re-initialize the activity
            } else {
                Toast.makeText(this, "Location permission is required for safety features.", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Re-attempt to send the alert after permission is granted
                loadData();
            } else {
                Toast.makeText(this, "SMS permission is required to alert contacts.", Toast.LENGTH_LONG).show();
            }
        }
        // FIXED: Added handling for the call permission result.
        else if (requestCode == CALL_PERMISSION_REQUEST_CODE_ALERT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Re-attempt to send the alert after permission is granted
                loadData();
            } else {
                Toast.makeText(this, "Call permission is required to alert contacts.", Toast.LENGTH_LONG).show();
            }
        }
    }
}