package com.example.safety;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MagnetometerActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor magnetometer;
    private TextView magnetometerReading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_magnetometer);

        magnetometerReading = findViewById(R.id.magnetometer_reading);

        // Get the SensorManager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Check if the magnetometer sensor exists
        if (sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null) {
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        } else {
            // Device doesn't have a magnetometer
            Toast.makeText(this, "Your device does not have a magnetometer.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register the sensor listener to start receiving updates
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener to save battery when the app is paused
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            // Get the magnetic field strength on x, y, and z axes
            float magX = event.values[0];
            float magY = event.values[1];
            float magZ = event.values[2];

            // Calculate the magnitude of the magnetic field vector
            double magnitude = Math.sqrt((magX * magX) + (magY * magY) + (magZ * magZ));

            // Display the reading in the TextView, formatted to two decimal places
            magnetometerReading.setText(String.format("%.2f µT", magnitude));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // We can ignore this for this application
    }
}








