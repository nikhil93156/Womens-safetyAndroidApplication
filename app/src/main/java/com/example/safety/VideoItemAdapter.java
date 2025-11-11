package com.example.safety;

import android.content.Context;
import android.text.method.LinkMovementMethod; // <--- NEW IMPORT
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class VideoItemAdapter extends ArrayAdapter<VideoItem> {

    private Context mContext;
    private int mResource;

    public VideoItemAdapter(@NonNull Context context, int resource, @NonNull List<VideoItem> objects) {
        super(context, resource, objects);
        this.mContext = context;
        this.mResource = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        VideoItem item = getItem(position);

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(mResource, parent, false);
        }

        TextView title = convertView.findViewById(R.id.videoTitleTextView);
        TextView details = convertView.findViewById(R.id.videoDetailsTextView);

        if (item != null) {
            // 1. Format Date and Time
            String dateFormat = DateFormat.getDateFormat(mContext).format(item.getDate());
            String timeFormat = DateFormat.getTimeFormat(mContext).format(item.getDate());

            // 2. Set Title (shortened name)
            title.setText(item.getName());

            // 3. Set Details (Date, Time, and Location Link)
            String detailText = dateFormat + " " + timeFormat +
                    " | Location: " + item.getLocationLink();
            details.setText(detailText);

            // 4. *** THE CRUCIAL FIX ***
            details.setMovementMethod(LinkMovementMethod.getInstance()); // This enables the link click handling
        }

        return convertView;
    }
}