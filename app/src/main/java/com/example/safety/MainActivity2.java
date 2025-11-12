package com.example.safety;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.MediaStore;
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
import androidx.core.content.FileProvider; // REQUIRED for FileProvider

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

// IMPLEMENT SensorEventListener INTERFACE
public class MainActivity2 extends AppCompatActivity implements SensorEventListener {
    Button b2, policeButton, sosOption, Camera_button;
    MaterialButton contactsButton, videoRecordButton;
    MaterialButton galleryButton;

    private FusedLocationProviderClient client;
    DatabaseHandler myDB;
    String x = "", y = "";
    private static final int REQUEST_LOCATION = 1;
    private static final int CALL_PERMISSION_REQUEST_CODE_ALERT = 2;
    LocationManager locationManager;
    MediaPlayer mp;
    private static final int SMS_PERMISSION_REQUEST_CODE = 100;
    private LocationCallback locationCallback;

    // SHAKE DETECTION VARIABLES
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private long lastShakeTime = 0;

    // NEW/ADJUSTED CONSTANTS FOR HIGHER RELIABILITY
    private static final float SHAKE_THRESHOLD_GRAVITY = 2.5F;
    private static final int SHAKE_TIME_LAPSE = 500;
    private static final int SHAKE_SLOP_TIME_MS = 2500;
    private static final int SHAKE_COUNT = 3;

    private int mShakeCount = 0;
    private long mFirstShakeTime = 0;

    // NEW VARIABLES AND CONSTANTS FOR VIDEO & SHARING
    private static final int VIDEO_CAPTURE_REQUEST_CODE = 3;
    // PICK_IMAGE_REQUEST is no longer needed since we start a new Activity for result.
    private static final int WRITE_STORAGE_PERMISSION_REQUEST_CODE = 5;

    private Uri videoUri;
    private File videoFileToScan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        b2 = findViewById(R.id.button2);
        policeButton = findViewById(R.id.policeStationButton);
        sosOption = findViewById(R.id.sosOptionButton);
        Camera_button = findViewById(R.id.Spy_camera_detect);
        contactsButton = findViewById(R.id.button);

        // --- GALLERY BUTTON INITIALIZATION ---
        galleryButton = findViewById(R.id.galleryButton);
        if (galleryButton != null) {
            // Updated to call new method for direct video list access
            galleryButton.setOnClickListener(v -> openMediaGalleryActivity());
        }
        // -----------------------------------------

        // Assuming you kept the original Camera_button functionality for a different purpose (e.g., "Camera_options")
        Camera_button.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), Camera_options.class);
            startActivity(i);
        });

        // INITIALIZE AND SET LISTENER FOR THE NEW VIDEO BUTTON
        videoRecordButton = findViewById(R.id.videoRecordButton);
        if (videoRecordButton != null) {
            videoRecordButton.setOnClickListener(v -> launchVideoRecorder());
        }
        // END OF VIDEO BUTTON SETUP

        sosOption.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity2.this, SOSActivity.class);
            startActivity(intent);
        });

        myDB = new DatabaseHandler(this);
        client = LocationServices.getFusedLocationProviderClient(this);
        mp = MediaPlayer.create(getApplicationContext(), R.raw.police_siren);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // INITIALIZE SENSOR MANAGER
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager != null) {
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

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

        // This is the button that triggers loadData()
        b2.setOnClickListener(v -> loadData());

        policeButton.setOnClickListener(v -> findNearbyPoliceStations());
    }

    /**
     * MODIFIED METHOD: Launches a new activity to display locally saved videos.
     * NOTE: You must create LocalVideoGalleryActivity.java and register it in the manifest.
     */
    private void openMediaGalleryActivity() {
        // Renaming to match previous instruction: LocalVideoGalleryActivity.class
        Intent intent = new Intent(MainActivity2.this, LocalVideoGallery.class);
        startActivity(intent);
    }

    // NEW METHOD: Launch Video Recorder (using FileProvider for secure URI)
    private void launchVideoRecorder() {
        // 1. Check/Request only necessary permissions (CAMERA and RECORD_AUDIO)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
            }, WRITE_STORAGE_PERMISSION_REQUEST_CODE);
            return;
        }

        // CHECK FOR LOCATION DATA BEFORE RECORDING
        if (x.isEmpty() || y.isEmpty()) {
            Toast.makeText(this, "Location data not ready. Please wait a moment.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a unique identifier incorporating coordinates and timestamp
        // Format: safety_video_LAT_LON_TIMESTAMP.mp4
        String fileNameBase = String.format("safety_video_%s_%s_%d", x.replace(".", "dot"), y.replace(".", "dot"), System.currentTimeMillis());

        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        try {
            // Create a temporary file in the app's private external directory
            File videoFile = File.createTempFile(fileNameBase, ".mp4", getExternalFilesDir(null));
            videoFileToScan = videoFile; // STORE THE FILE REFERENCE FOR GALLERY SCAN LATER

            // Use FileProvider to get a secure URI
            videoUri = FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    videoFile
            );

            takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);

            // Grant the camera app temporary permission to write to this URI
            takeVideoIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            startActivityForResult(takeVideoIntent, VIDEO_CAPTURE_REQUEST_CODE);
            Toast.makeText(this, "Video recording started. Record your video and press stop.", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Could not create video file.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    /**
     * Sends a broadcast to the Media Scanner to index the new video file.
     */
    private void makeVideoGalleryVisible() {
        if (videoFileToScan != null && videoFileToScan.exists()) {
            try {
                // This is the modern, more reliable way to trigger a media scan for a single file
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(videoFileToScan);
                mediaScanIntent.setData(contentUri);
                sendBroadcast(mediaScanIntent);
                Log.d("MediaScan", "Media scan triggered for: " + videoFileToScan.getAbsolutePath());
                Toast.makeText(this, "Video saved to Gallery.", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e("MediaScan", "Failed to broadcast media scan.", e);
            }
        }
    }

    /**
     * MODIFIED METHOD: Sends the initial alert SMS with location and instructions. CALL DISABLED.
     * This is used ONLY by the video feature.
     */
    private void sendInitialAlertSMS() {
        if (x.isEmpty() || y.isEmpty()) {
            loadData(); // Fallback to standard alert if location is missing
            return;
        }

        ArrayList<String> phoneNumbers = new ArrayList<>();
        Cursor data = myDB.getListContents();
        try {
            if (data.getCount() == 0) {
                Toast.makeText(this, "No emergency contacts saved.", Toast.LENGTH_SHORT).show();
                return;
            }
            while (data.moveToNext()) {
                phoneNumbers.add(data.getString(2));
            }
        } finally {
            if (data != null) {
                data.close();
            }
        }

        if (!phoneNumbers.isEmpty()) {
            // NOTE: The coordinates used here are for the SMS alert link, matching the recorded location.
            String msg = "I need help! My current location is: http://maps.google.com/?q=" + x + "," + y +
                    "\n\n🚨 EMERGENCY VIDEO: The share screen is open now. Please upload the video via Google Drive/WhatsApp and send the resulting link/file to this thread!";

            String numberString = String.join(";", phoneNumbers);
            sendSms(numberString, msg);
            // CALL DISABLED FOR VIDEO FEATURE
        }
    }

    /**
     * Launches the system share sheet for the user to manually upload the video.
     */
    private void launchShareSheet() {
        if (videoUri == null) {
            Toast.makeText(this, "Cannot share: Video URI is missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create the system share intent
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("video/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, videoUri);
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Emergency video recorded with location: http://maps.google.com/?q=" + x + "," + y);

        // Grant read permission to the receiving app (essential for FileProvider URI)
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Start the chooser, prompting the user to select an app (e.g., Drive, WhatsApp, Email)
        startActivity(Intent.createChooser(shareIntent, "Upload and Share Emergency Video via..."));
    }

    // METHOD REQUIRED BY SensorEventListener - Used for shake detection
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // Calculate gForce
            float gX = x / SensorManager.GRAVITY_EARTH;
            float gY = y / SensorManager.GRAVITY_EARTH;
            float gZ = z / SensorManager.GRAVITY_EARTH;

            float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);
            final long now = System.currentTimeMillis();

            if (gForce > SHAKE_THRESHOLD_GRAVITY) {
                // 1. Damping/Debounce: Check if enough time has passed since the last *valid* shake reading
                if (lastShakeTime + SHAKE_TIME_LAPSE > now) {
                    return; // Ignore this shake, it's too soon
                }

                // Reset the count if too much time has passed since the first shake in the sequence
                if (mFirstShakeTime == 0) {
                    mFirstShakeTime = now;
                    mShakeCount = 0; // Start the counter
                } else if (now - mFirstShakeTime > SHAKE_SLOP_TIME_MS) {
                    mFirstShakeTime = now;
                    mShakeCount = 0; // Time limit exceeded, restart the sequence
                }

                // 2. Register a successful shake
                lastShakeTime = now;
                mShakeCount++;

                Toast.makeText(this, "Shake: " + mShakeCount + " of " + SHAKE_COUNT, Toast.LENGTH_SHORT).show();

                // 3. Trigger the alarm if the count is reached
                if (mShakeCount >= SHAKE_COUNT) {
                    // Reset counter immediately to prevent immediate re-trigger
                    mShakeCount = 0;
                    mFirstShakeTime = 0;

                    Toast.makeText(this, "Shake Gesture Complete! Sending Alert...", Toast.LENGTH_LONG).show();
                    loadData(); // Triggers SMS and CALL
                }
            }
        }
    }

    // METHOD REQUIRED BY SensorEventListener - Not used for shake detection
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Can be left empty
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register the listener to start monitoring shake events
        if (mSensorManager != null && mAccelerometer != null) {
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener to save battery when the app is not in the foreground
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
    }

    private void findNearbyPoliceStations() {
        if (x.isEmpty() || y.isEmpty()) {
            Toast.makeText(this, "Location not available yet. Please wait.", Toast.LENGTH_SHORT).show();
            return;
        }
        // Corrected the geo URI for searching nearby police stations.
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

    // sendAlertSMSWithLink is now obsolete, but kept here for function completeness.
    private void sendAlertSMSWithLink(String driveLink) {
        loadData();
    }

    /**
     * MODIFIED METHOD: ORIGINAL loadData used for SOS Button and Shake Gesture.
     * This now performs SMS and CALL.
     */
    private void loadData() {
        // Added a check to ensure location is available before sending an alert.
        if (x.isEmpty() || y.isEmpty()) {
            Toast.makeText(this, "Location not ready. Please wait a moment and try again.", Toast.LENGTH_LONG).show();
            return;
        }

        ArrayList<String> phoneNumbers = new ArrayList<>();
        // Correctly close the cursor using a try-finally block.
        Cursor data = myDB.getListContents();
        try {
            if (data.getCount() == 0) {
                Toast.makeText(this, "No emergency contacts saved.", Toast.LENGTH_SHORT).show();
                return;
            }
            while (data.moveToNext()) {
                // Get the phone number (column index 2).
                phoneNumbers.add(data.getString(2));
            }
        } finally {
            if (data != null) {
                data.close();
            }
        }

        if (!phoneNumbers.isEmpty()) {
            // Corrected the Google Maps URL format.
            String msg = "I need help! My current location is: http://maps.google.com/?q=" + x + "," + y;
            String numberString = String.join(";", phoneNumbers);
            sendSms(numberString, msg);

            // CALL ENABLED FOR SOS/SHAKE
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

    // The call method remains, but is now unused by the alert functions.
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == VIDEO_CAPTURE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Video saved! Sending alert SMS and opening share menu...", Toast.LENGTH_LONG).show();

                // 0. Make the saved video visible in the user's gallery
                makeVideoGalleryVisible();

                // 1. Send the initial alert SMS with location and instructions (SMS ONLY)
                sendInitialAlertSMS();

                // 2. Launch the system's Share Sheet for the user to upload/share the video
                launchShareSheet();

            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Video recording cancelled. Sending standard alert...", Toast.LENGTH_SHORT).show();
                loadData(); // Sends SMS + CALL (Fallback to general alert, ensure this is intended behavior on cancel)
            } else {
                Toast.makeText(this, "Video recording failed. Sending standard alert...", Toast.LENGTH_SHORT).show();
                loadData(); // Sends SMS + CALL (Fallback to general alert, ensure this is intended behavior on fail)
            }
        }
        // Removed PICK_IMAGE_REQUEST handling as we now open a dedicated Activity.
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                recreate();
            } else {
                Toast.makeText(this, "Location permission is required for safety features.", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadData();
            } else {
                Toast.makeText(this, "SMS permission is required to alert contacts.", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == CALL_PERMISSION_REQUEST_CODE_ALERT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadData();
            } else {
                Toast.makeText(this, "Call permission is required to alert contacts.", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == WRITE_STORAGE_PERMISSION_REQUEST_CODE) {
            // Check if ALL requested permissions were granted (Camera, Audio)
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                // If granted, re-launch the recorder
                launchVideoRecorder();
            } else {
                Toast.makeText(this, "Camera and Audio permissions are required for video recording.", Toast.LENGTH_LONG).show();
            }
        }
    }
}