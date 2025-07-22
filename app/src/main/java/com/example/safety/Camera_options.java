package com.example.safety;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

public class Camera_options extends AppCompatActivity {
    LinearLayout nightVisionButton, magnetometerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_detector_options);

        // You will add functionality to these buttons later
        nightVisionButton = findViewById(R.id.night_vision_button);
        magnetometerButton = findViewById(R.id.magnetometer_button);

        nightVisionButton.setOnClickListener(v -> {
            Intent intent = new Intent(Camera_options.this, NightVisionActivity.class);
            startActivity(intent);
        });
        magnetometerButton.setOnClickListener(v -> {
            Intent intent = new Intent(Camera_options.this, MagnetometerActivity.class);
            startActivity(intent);
        });
    }
}