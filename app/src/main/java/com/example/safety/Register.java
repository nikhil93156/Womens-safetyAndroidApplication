package com.example.safety;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

// FIXED: Implemented the OnContactDeleteListener interface from the adapter.
public class Register extends AppCompatActivity implements ContactsAdapter.OnContactDeleteListener {

    private final ArrayList<Contact> contacts = new ArrayList<>();
    private ContactsAdapter adapter;
    private RecyclerView recyclerView;
    private DatabaseHandler myDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        recyclerView = findViewById(R.id.list);
        myDB = new DatabaseHandler(this);

        // FIXED: The adapter is now initialized correctly with 'this' as the listener.
        adapter = new ContactsAdapter(contacts, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Load initial data from the database
        loadContactsFromDB();

        FloatingActionButton fab = findViewById(R.id.add);
        fab.setOnClickListener(v -> showAddContactDialog());
    }

    private void loadContactsFromDB() {
        // FIXED: The cursor is now properly closed in a finally block to prevent memory leaks.
        Cursor data = myDB.getListContents();
        try {
            if (data.getCount() == 0) {
                contacts.clear(); // Clear list if DB is empty
                return;
            }
            contacts.clear();
            while (data.moveToNext()) {
                // Column 1 is NAME, Column 2 is PHONE_NUMBER
                contacts.add(new Contact(data.getString(1), data.getString(2)));
            }
        } finally {
            if (data != null) {
                data.close();
            }
        }
        // Refresh the adapter
        adapter.notifyDataSetChanged();
    }

    private void showAddContactDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_add_contact, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        EditText nameInput = dialogView.findViewById(R.id.name);
        EditText phoneInput = dialogView.findViewById(R.id.phone);
        Button saveButton = dialogView.findViewById(R.id.save_button);
        Button cancelButton = dialogView.findViewById(R.id.cancel_button);

        saveButton.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String phone = phoneInput.getText().toString().trim();

            if (!name.isEmpty() && !phone.isEmpty()) {
                if (myDB.addData(name, phone)) {
                    // Refresh the entire list from the database to ensure consistency
                    loadContactsFromDB();
                    recyclerView.scrollToPosition(contacts.size() - 1);
                    Toast.makeText(this, "Contact Saved", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(this, "Error: Could not save contact", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
            }
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }


    @Override
    public void onContactDelete(Contact contact, int position) {
        // 1. Delete from the database using the new method in DatabaseHandler
        myDB.deleteContact(contact.getPhoneNumber());

        // 2. Remove from the list in the UI
        contacts.remove(position);

        // 3. Notify the adapter that an item was removed
        adapter.notifyItemRemoved(position);
        adapter.notifyItemRangeChanged(position, contacts.size());

        Toast.makeText(this, "Contact Deleted", Toast.LENGTH_SHORT).show();
    }
}