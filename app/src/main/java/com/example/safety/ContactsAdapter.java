package com.example.safety;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactViewHolder> {

    private final List<Contact> contactList;
    private final OnContactDeleteListener deleteListener;

    // FIXED: Created an interface to send the delete event back to the Register activity.
    // The adapter should not handle database logic directly.
    public interface OnContactDeleteListener {
        void onContactDelete(Contact contact, int position);
    }

    // FIXED: The constructor now requires the listener.
    public ContactsAdapter(List<Contact> contactList, OnContactDeleteListener listener) {
        this.contactList = contactList;
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        Contact contact = contactList.get(position);
        holder.contactName.setText(contact.getName());
        holder.contactPhone.setText(contact.getPhoneNumber());

        // FIXED: Added a check to prevent a crash if the contact name is empty.
        if (contact.getName() != null && !contact.getName().isEmpty()) {
            holder.contactInitials.setText(contact.getName().substring(0, 1).toUpperCase());
        } else {
            holder.contactInitials.setText("#");
        }

        // FIXED: The delete button now uses the listener interface.
        // This lets the Register activity handle the database deletion.
        holder.deleteButton.setOnClickListener(v -> {
            if (deleteListener != null) {
                // We pass the contact and its position to the activity.
                deleteListener.onContactDelete(contact, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    public static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView contactInitials, contactName, contactPhone;
        ImageButton deleteButton;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            contactInitials = itemView.findViewById(R.id.contact_initials);
            contactName = itemView.findViewById(R.id.contact_name);
            contactPhone = itemView.findViewById(R.id.contact_phone);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }
}