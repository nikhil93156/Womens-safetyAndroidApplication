package com.example.safety;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import android.util.Log;

public class LocalVideoGallery extends AppCompatActivity {

    private ListView videoListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_video_gallery);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Saved Emergency Videos");
        }

        videoListView = findViewById(R.id.videoListView);

        loadSavedVideos();
    }

    // Inside LocalVideoGalleryActivity.java

    private void loadSavedVideos() {
        // ... (unchanged setup code for videoDir and files array)

        File videoDir = getExternalFilesDir(null);
        if (videoDir == null) {
            Toast.makeText(this, "Storage not accessible.", Toast.LENGTH_LONG).show();
            return;
        }

        File[] files = videoDir.listFiles();
        List<VideoItem> videoItems = new ArrayList<>();

        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().startsWith("safety_video") && file.getName().endsWith(".mp4")) {
                    Date lastModifiedDate = new Date(file.lastModified());
                    String locationLink = "No location recorded."; // Default value

                    // --- 1. EXTRACT COORDINATES FROM FILENAME ---
                    String nameWithoutExtension = file.getName().replace(".mp4", "");
                    String[] parts = nameWithoutExtension.split("_");

                    // Expected format parts: [safety, video, LAT, LON, TIMESTAMP]
                    if (parts.length >= 4) {
                        try {
                            // Revert "dot" back to "." for valid coordinates
                            String lat = parts[2].replace("dot", ".");
                            String lon = parts[3].replace("dot", ".");

                            // 2. CONSTRUCT THE CLICKABLE GOOGLE MAPS URL
                            // Standard Google Maps URL format for coordinates: geo:LAT,LON?q=LAT,LON
                            locationLink = "https://maps.google.com/maps?q=" + lat + "," + lon;

                        } catch (Exception e) {
                            Log.e("VideoGallery", "Failed to parse coordinates from filename: " + nameWithoutExtension);
                            // Keep locationLink as default error message
                        }
                    }
                    // --- END EXTRACT ---

                    videoItems.add(new VideoItem(
                            file,
                            lastModifiedDate,
                            file.getName(),
                            locationLink) // Pass the real map link here
                    );
                }
            }
        }

        // ... (rest of loadSavedVideos() remains the same: sorting, adapter setup, item click)
        // ... (Rest of the method, including sorting and setting the adapter, remains the same)
        Collections.sort(videoItems, new Comparator<VideoItem>() {
            @Override
            public int compare(VideoItem o1, VideoItem o2) {
                return o2.getDate().compareTo(o1.getDate());
            }
        });

        if (videoItems.isEmpty()) {
            Toast.makeText(this, "No emergency videos found.", Toast.LENGTH_LONG).show();
        }

        VideoItemAdapter adapter = new VideoItemAdapter(
                this,
                R.layout.video_list_item,
                videoItems
        );
        videoListView.setAdapter(adapter);

        videoListView.setOnItemClickListener((parent, view, position, id) -> {
            VideoItem selectedItem = videoItems.get(position);
            openVideoFile(selectedItem.getFile());
        });
    }

    private void openVideoFile(File videoFile) {
        try {
            // Get the secure FileProvider URI for the file
            android.net.Uri fileUri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    videoFile
            );

            // Create an Intent to view the media
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, "video/mp4");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Crucial for media viewer apps

            startActivity(intent);
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, "Error: Could not open video file.", Toast.LENGTH_LONG).show();
            Log.e("VideoGallery", "Failed to open video file.", e);
        }
    }
}