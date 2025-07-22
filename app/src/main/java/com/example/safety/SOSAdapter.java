package com.example.safety;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SOSAdapter extends RecyclerView.Adapter<SOSAdapter.SOSViewHolder> {

    private final List<SOSModel> sosList;
    private final Context context;

    public SOSAdapter(List<SOSModel> sosList, Context context) {
        this.sosList = sosList;
        this.context = context;
    }

    @NonNull
    @Override
    public SOSViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.sos_item, parent, false);
        return new SOSViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SOSViewHolder holder, int position) {
        SOSModel sos = sosList.get(position);
        holder.sosName.setText(sos.getName());
        holder.sosNumber.setText(sos.getNumber());

        holder.itemView.setOnClickListener(v -> makePhoneCall(sos.getNumber()));
    }

    private void makePhoneCall(String number) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            String dial = "tel:" + number;
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse(dial));
            context.startActivity(intent);
        } else {
            Toast.makeText(context, "Call permission not granted", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return sosList.size();
    }

    static class SOSViewHolder extends RecyclerView.ViewHolder {
        TextView sosName, sosNumber;

        public SOSViewHolder(@NonNull View itemView) {
            super(itemView);
            sosName = itemView.findViewById(R.id.sosName);
            sosNumber = itemView.findViewById(R.id.sosNumber);
        }
    }
}
