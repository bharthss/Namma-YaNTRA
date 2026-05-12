package com.example.namma_yantra;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.core.app.ActivityCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.android.gms.location.*;
import android.annotation.SuppressLint;

import java.util.HashMap;

public class OwnerActivity extends AppCompatActivity {

    EditText machineName, price, location, phone;
    Button addBtn, viewMachinesBtn, viewBookingsBtn, viewUsersBtn, viewEarningsBtn, viewHistoryBtn;
    LinearLayout outputContainer;

    DatabaseReference machineDB, bookingDB, userDB;
    FirebaseAuth auth;

    FusedLocationProviderClient fusedLocationClient;
    double ownerLat = 0, ownerLng = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner);

        machineName = findViewById(R.id.machineName);
        price = findViewById(R.id.price);
        location = findViewById(R.id.location);
        phone = findViewById(R.id.phone);

        addBtn = findViewById(R.id.addBtn);
        viewMachinesBtn = findViewById(R.id.viewMachinesBtn);
        viewBookingsBtn = findViewById(R.id.viewBookingsBtn);
        viewUsersBtn = findViewById(R.id.viewUsersBtn);
        viewEarningsBtn = findViewById(R.id.viewEarningsBtn);
        viewHistoryBtn = findViewById(R.id.viewHistoryBtn);

        outputContainer = findViewById(R.id.outputContainer);

        machineDB = FirebaseDatabase.getInstance().getReference("machines");
        bookingDB = FirebaseDatabase.getInstance().getReference("bookings");
        userDB = FirebaseDatabase.getInstance().getReference("users");

        auth = FirebaseAuth.getInstance();

        // 🔥 GPS INIT
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        getOwnerLocation();

        // 🔹 ADD MACHINE
        addBtn.setOnClickListener(v -> {

            String n = machineName.getText().toString().trim();
            String p = price.getText().toString().trim();
            String l = location.getText().toString().trim();
            String ph = phone.getText().toString().trim();

            if (n.isEmpty() || p.isEmpty() || l.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            if (ph.isEmpty()) {
                Toast.makeText(this, "Enter phone number", Toast.LENGTH_SHORT).show();
                return;
            }

            String id = machineDB.push().getKey();

            HashMap<String, String> map = new HashMap<>();
            map.put("name", n);
            map.put("price", p);
            map.put("location", l);
            map.put("phone", ph);
            map.put("ownerId", auth.getCurrentUser().getUid());
            map.put("lat", String.valueOf(ownerLat));
            map.put("lng", String.valueOf(ownerLng));

            machineDB.child(id).setValue(map);

            Toast.makeText(this, "Machine Added with GPS", Toast.LENGTH_SHORT).show();

            machineName.setText("");
            price.setText("");
            location.setText("");
            phone.setText("");
        });

        // 🔹 VIEW MACHINES
        viewMachinesBtn.setOnClickListener(v -> {

            outputContainer.removeAllViews();
            String currentOwner = auth.getCurrentUser().getUid();

            machineDB.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {

                    for (DataSnapshot item : snapshot.getChildren()) {

                        String ownerId = item.child("ownerId").getValue(String.class);
                        if (ownerId == null || !ownerId.equals(currentOwner)) continue;

                        String n = item.child("name").getValue(String.class);
                        String p = item.child("price").getValue(String.class);
                        String l = item.child("location").getValue(String.class);

                        LinearLayout box = new LinearLayout(OwnerActivity.this);
                        box.setOrientation(LinearLayout.VERTICAL);
                        box.setPadding(20,20,20,20);

                        TextView txt = new TextView(OwnerActivity.this);
                        txt.setText("🚜 " + n + " ₹" + p + "/hr\n📍 " + l);

                        Button deleteBtn = new Button(OwnerActivity.this);
                        deleteBtn.setText("DELETE");

                        deleteBtn.setOnClickListener(v1 -> item.getRef().removeValue());

                        box.addView(txt);
                        box.addView(deleteBtn);

                        outputContainer.addView(box);
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {}
            });
        });

        // 🔹 VIEW PENDING BOOKINGS
        viewBookingsBtn.setOnClickListener(v -> {

            outputContainer.removeAllViews();

            bookingDB.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {

                    for (DataSnapshot item : snapshot.getChildren()) {

                        String status = item.child("status").getValue(String.class);
                        if (status == null || !status.equals("pending")) continue;

                        String machine = item.child("machine").getValue(String.class);
                        String hours = item.child("hours").getValue(String.class);
                        String date = item.child("date").getValue(String.class);
                        String userName = item.child("userName").getValue(String.class);

                        LinearLayout card = new LinearLayout(OwnerActivity.this);
                        card.setOrientation(LinearLayout.VERTICAL);
                        card.setPadding(20,20,20,20);

                        TextView txt = new TextView(OwnerActivity.this);
                        txt.setText("📦 " + machine +
                                "\n👤 " + userName +
                                "\n⏱ " + hours +
                                "\n📅 " + date);

                        Button accept = new Button(OwnerActivity.this);
                        accept.setText("ACCEPT");

                        Button reject = new Button(OwnerActivity.this);
                        reject.setText("REJECT");

                        accept.setOnClickListener(v1 -> {

                            String ownerPhone = phone.getText().toString().trim();

                            if (ownerPhone.isEmpty()) {
                                Toast.makeText(OwnerActivity.this, "Enter phone number above", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            item.getRef().child("status").setValue("accepted");
                            item.getRef().child("ownerPhone").setValue(phone.getText().toString());
                            Toast.makeText(OwnerActivity.this, "Accepted", Toast.LENGTH_SHORT).show();
                        });

                        reject.setOnClickListener(v1 -> {
                            item.getRef().child("status").setValue("rejected");
                        });

                        card.addView(txt);
                        card.addView(accept);
                        card.addView(reject);

                        outputContainer.addView(card);
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {}
            });
        });

        // 🔹 VIEW USERS
        viewUsersBtn.setOnClickListener(v -> {

            outputContainer.removeAllViews();

            userDB.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {

                    for (DataSnapshot user : snapshot.getChildren()) {

                        String role = user.child("role").getValue(String.class);
                        if (role == null || !role.equals("farmer")) continue;

                        String name = user.child("name").getValue(String.class);
                        String email = user.child("email").getValue(String.class);

                        TextView txt = new TextView(OwnerActivity.this);
                        txt.setText("👤 " + name + "\n📧 " + email);

                        outputContainer.addView(txt);
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {}
            });
        });

        // 🔥 EARNINGS
        viewEarningsBtn.setOnClickListener(v -> {

            outputContainer.removeAllViews();
            final int[] total = {0};

            bookingDB.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {

                    for (DataSnapshot item : snapshot.getChildren()) {

                        String status = item.child("status").getValue(String.class);
                        if (status == null || !status.equals("accepted")) continue;

                        String machine = item.child("machine").getValue(String.class);
                        String hoursStr = item.child("hours").getValue(String.class);

                        if (machine == null || hoursStr == null) continue;

                        try {
                            int hours = Integer.parseInt(hoursStr);
                            int price = Integer.parseInt(machine.replaceAll("[^0-9]", ""));
                            int earn = price * hours;

                            total[0] += earn;

                            TextView txt = new TextView(OwnerActivity.this);
                            txt.setText("💰 " + machine +
                                    "\nHours: " + hours +
                                    "\nEarned: ₹" + earn);

                            outputContainer.addView(txt);

                        } catch (Exception e) {}
                    }

                    TextView totalTxt = new TextView(OwnerActivity.this);
                    totalTxt.setText("\nTOTAL EARNINGS: ₹" + total[0]);
                    totalTxt.setTextSize(18);

                    outputContainer.addView(totalTxt);
                }

                @Override
                public void onCancelled(DatabaseError error) {}
            });
        });

        // 🔥 BOOKING HISTORY
        viewHistoryBtn.setOnClickListener(v -> {

            outputContainer.removeAllViews();

            bookingDB.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {

                    for (DataSnapshot item : snapshot.getChildren()) {

                        String status = item.child("status").getValue(String.class);
                        if (status == null || status.equals("pending")) continue;

                        String machine = item.child("machine").getValue(String.class);
                        String hours = item.child("hours").getValue(String.class);
                        String date = item.child("date").getValue(String.class);
                        String userName = item.child("userName").getValue(String.class);

                        TextView txt = new TextView(OwnerActivity.this);
                        txt.setText("📦 " + machine +
                                "\n👤 " + userName +
                                "\n⏱ " + hours +
                                "\n📅 " + date +
                                "\nStatus: " + status.toUpperCase());

                        outputContainer.addView(txt);
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {}
            });
        });

        // BACK
        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        startActivity(new Intent(OwnerActivity.this, LoginActivity.class));
                        finish();
                    }
                });
    }

    // 🔥 GPS
    @SuppressLint("MissingPermission")
    private void getOwnerLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY, null
        ).addOnSuccessListener(location -> {
            if (location != null) {
                ownerLat = location.getLatitude();
                ownerLng = location.getLongitude();

                Toast.makeText(this,
                        "Location: " + ownerLat + ", " + ownerLng,
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults); // 🔥 IMPORTANT

        if (requestCode == 100 && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getOwnerLocation(); // or getUserLocation()
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
        return true;
    }
}