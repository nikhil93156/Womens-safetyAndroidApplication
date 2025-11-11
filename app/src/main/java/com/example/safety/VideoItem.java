package com.example.safety;

import java.io.File;
import java.util.Date;

public class VideoItem {
    private File file;
    private Date date;
    private String name;
    // Location link is stored here if available, or we derive it from the file name/time.
    private String locationLink;

    public VideoItem(File file, Date date, String name, String locationLink) {
        this.file = file;
        this.date = date;
        this.name = name;
        this.locationLink = locationLink;
    }

    public File getFile() {
        return file;
    }

    public Date getDate() {
        return date;
    }

    public String getName() {
        return name;
    }

    public String getLocationLink() {
        return locationLink;
    }
}