package com.example.safety;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class SOSActivity extends AppCompatActivity {
    private RecyclerView sosRecyclerView;
    private SOSAdapter sosAdapter;
    private List<SOSModel> sosList;

    private static final int CALL_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos);

        sosRecyclerView = findViewById(R.id.sosRecyclerView);
        sosRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Check permission on activity start
        checkCallPermission();

        sosList = new ArrayList<>();
        sosList.add(new SOSModel("Police", "100"));
        sosList.add(new SOSModel("Fire Brigade", "101"));
        sosList.add(new SOSModel("Ambulance", "102"));
        sosList.add(new SOSModel("Disaster Management", "108"));
        sosList.add(new SOSModel("Women Helpline", "1091"));
        sosList.add(new SOSModel("Child Helpline", "1098"));
        sosList.add(new SOSModel("Road Accident Emergency Service", "1073"));
        sosList.add(new SOSModel("Railways Enquiry", "139"));
        sosList.add(new SOSModel("Tourist Helpline", "1363"));
        sosList.add(new SOSModel("Blood Requirement Helpline", "104"));
        sosList.add(new SOSModel("Cyber Crime Helpline", "1930"));
        sosList.add(new SOSModel("Senior Citizen Helpline", "14567"));

        sosAdapter = new SOSAdapter(sosList, this);  // passing context
        sosRecyclerView.setAdapter(sosAdapter);
    }

    private void checkCallPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CALL_PHONE},
                    CALL_PERMISSION_REQUEST_CODE);
        }
    }

    // This method will be called after user responds to permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CALL_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Call permission granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Call permission denied. Cannot make calls.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
