// FULL CLEAN FINAL VERSION

package com.example.namma_yantra;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.core.app.ActivityCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.location.*;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    RadioGroup radioGroup;
    EditText hours, date;
    TextView totalPrice;
    LinearLayout outputContainer;
    Button bookBtn, showMachinesBtn, showRequestsBtn;

    DatabaseReference machineDB, bookingDB;

    int selectedPrice = 0;
    String selectedMachine = "";

    // 🔥 GPS
    FusedLocationProviderClient fusedLocationClient;
    double userLat = 0, userLng = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        radioGroup = findViewById(R.id.radioGroup);
        hours = findViewById(R.id.hours);
        date = findViewById(R.id.date);
        totalPrice = findViewById(R.id.totalPrice);
        outputContainer = findViewById(R.id.outputContainer);
        bookBtn = findViewById(R.id.bookBtn);
        showMachinesBtn = findViewById(R.id.showMachinesBtn);
        showRequestsBtn = findViewById(R.id.showRequestsBtn);

        machineDB = FirebaseDatabase.getInstance().getReference("machines");
        bookingDB = FirebaseDatabase.getInstance().getReference("bookings");

        // 🔥 GPS INIT
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        getUserLocation();

        deleteOldBookings();

        radioGroup.setVisibility(View.GONE);

        // 📅 DATE
        date.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            DatePickerDialog picker = new DatePickerDialog(this,
                    (view, y, m, d) -> date.setText(d + "/" + (m + 1) + "/" + y),
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH));

            picker.getDatePicker().setMinDate(System.currentTimeMillis());
            picker.show();
        });

        // SHOW MACHINES
        showMachinesBtn.setOnClickListener(v ->
                radioGroup.setVisibility(radioGroup.getVisibility() == View.GONE ? View.VISIBLE : View.GONE)
        );

        // LOAD MACHINES
        machineDB.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                radioGroup.removeAllViews();

                for (DataSnapshot item : snapshot.getChildren()) {

                    String name = item.child("name").getValue(String.class);
                    String price = item.child("price").getValue(String.class);

                    if (name == null || price == null) continue;

                    RadioButton rb = new RadioButton(MainActivity.this);
                    rb.setText(name + " ₹" + price + "/hr");
                    rb.setTag(price);

                    radioGroup.addView(rb);
                }
            }

            @Override public void onCancelled(DatabaseError error) {}
        });

        // SELECT MACHINE
        radioGroup.setOnCheckedChangeListener((g, id) -> {
            RadioButton selected = findViewById(id);
            if (selected != null) {
                selectedMachine = selected.getText().toString();
                try {
                    selectedPrice = Integer.parseInt(selected.getTag().toString());
                } catch (Exception e) {
                    selectedPrice = 0;
                }
            }
        });

        // PRICE
        hours.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    int total = (!s.toString().isEmpty() && selectedPrice > 0)
                            ? Integer.parseInt(s.toString()) * selectedPrice : 0;
                    totalPrice.setText("Total Price: ₹" + total);
                } catch (Exception e) {
                    totalPrice.setText("Total Price: ₹0");
                }
            }
        });

        // BOOK
        bookBtn.setOnClickListener(v -> {

            String h = hours.getText().toString();
            String d = date.getText().toString();

            if (selectedMachine.isEmpty() || h.isEmpty() || d.isEmpty()) {
                Toast.makeText(this, "Fill all details", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;

            DatabaseReference userDB = FirebaseDatabase.getInstance().getReference("users");

            userDB.child(user.getUid()).get().addOnSuccessListener(userSnap -> {

                String userName = userSnap.child("name").getValue(String.class);

                // 🔥 NOW GET MACHINE DATA
                machineDB.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        for (DataSnapshot m : snapshot.getChildren()) {

                            String name = m.child("name").getValue(String.class);
                            String price = m.child("price").getValue(String.class);

                            if ((name + " ₹" + price + "/hr").equals(selectedMachine)) {

                                // ✅ DECLARE HERE
                                String lat = m.child("lat").getValue(String.class);
                                String lng = m.child("lng").getValue(String.class);
                                String phone = m.child("phone").getValue(String.class);

                                HashMap<String, String> map = new HashMap<>();
                                map.put("machine", selectedMachine);
                                map.put("hours", h);
                                map.put("date", d);
                                map.put("status", "pending");
                                map.put("userName", userName);
                                map.put("userEmail", user.getEmail());

                                // ✅ NOW IT WORKS
                                map.put("lat", lat);
                                map.put("lng", lng);
                                map.put("ownerPhone", phone);
                                String location = m.child("location").getValue(String.class);
                                map.put("location", location);

                                bookingDB.push().setValue(map);

                                Toast.makeText(MainActivity.this, "Booking Sent", Toast.LENGTH_SHORT).show();
                                break;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
            });
        });


        // 📜 REQUESTS
        showRequestsBtn.setOnClickListener(v -> {

            outputContainer.removeAllViews();

            bookingDB.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {

                    outputContainer.removeAllViews();

                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    String currentUserEmail = user.getEmail();

                    // 🔥 STEP 1: CREATE LISTS
                    ArrayList<DataSnapshot> pending = new ArrayList<>();
                    ArrayList<DataSnapshot> accepted = new ArrayList<>();
                    ArrayList<DataSnapshot> rejected = new ArrayList<>();

                    // 🔥 STEP 2: FILTER + SORT
                    for (DataSnapshot item : snapshot.getChildren()) {

                        String userEmail = item.child("userEmail").getValue(String.class);

                        if (userEmail == null || !userEmail.equals(currentUserEmail)) continue;

                        String status = item.child("status").getValue(String.class);

                        if ("pending".equals(status)) pending.add(item);
                        else if ("accepted".equals(status)) accepted.add(item);
                        else rejected.add(item);
                    }

                    // 🔥 STEP 3: MERGE CORRECTLY
                    ArrayList<DataSnapshot> finalList = new ArrayList<>();
                    finalList.addAll(pending);
                    finalList.addAll(accepted);
                    finalList.addAll(rejected);

                    // 🔥 STEP 4: DISPLAY
                    for (DataSnapshot item : finalList) {

                        String machine = item.child("machine").getValue(String.class);
                        String status = item.child("status").getValue(String.class);
                        String date = item.child("date").getValue(String.class);
                        String phone = item.child("ownerPhone").getValue(String.class);
                        String loc = item.child("location").getValue(String.class);
                        String lat = item.child("lat").getValue(String.class);
                        String lng = item.child("lng").getValue(String.class);

                        LinearLayout card = new LinearLayout(MainActivity.this);
                        card.setOrientation(LinearLayout.VERTICAL);
                        card.setPadding(20,20,20,20);

                        TextView txt = new TextView(MainActivity.this);
                        txt.setText(machine +
                                "\n📅 " + date +
                                "\nStatus: " + status.toUpperCase());

                        // 🔥 COLOR LOGIC
                        if ("pending".equals(status)) {
                            txt.setTextColor(0xFFFFA000); // Yellow
                        } else if ("accepted".equals(status)) {
                            txt.setTextColor(0xFF2E7D32); // Green
                        } else if ("rejected".equals(status)) {
                            txt.setTextColor(0xFFD32F2F); // Red
                        }
                        card.addView(txt);

                        // LOCATION
                        if (loc != null) {
                            TextView locationTxt = new TextView(MainActivity.this);
                            locationTxt.setText("📍 Location: " + loc);
                            card.addView(locationTxt);
                        }

                        // CONTACT
                        if ("accepted".equals(status) && phone != null) {
                            TextView contact = new TextView(MainActivity.this);
                            contact.setText("📞 Contact owner: " + phone + " for exact location");
                            contact.setTextColor(0xFF0000FF);

                            contact.setOnClickListener(v1 -> {
                                Intent i = new Intent(Intent.ACTION_DIAL);
                                i.setData(Uri.parse("tel:" + phone));
                                startActivity(i);
                            });

                            card.addView(contact);
                        }

                        // DISTANCE
                        if (lat != null && lng != null && userLat != 0) {
                            try {
                                Location userLoc = new Location("");
                                userLoc.setLatitude(userLat);
                                userLoc.setLongitude(userLng);

                                Location machineLoc = new Location("");
                                machineLoc.setLatitude(Double.parseDouble(lat));
                                machineLoc.setLongitude(Double.parseDouble(lng));

                                float dist = userLoc.distanceTo(machineLoc) / 1000;

                                TextView dTxt = new TextView(MainActivity.this);
                                dTxt.setText("📏 Distance: " + String.format("%.2f", dist) + " km");
                                card.addView(dTxt);

                            } catch (Exception e) {
                                // ignore crash
                            }
                        }

                        outputContainer.addView(card);
                    }
                }

                @Override public void onCancelled(DatabaseError error) {}
            });
        });

        // BACK
        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        finish();
                    }
                });
    }

    // GPS
    private void getUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(loc -> {
            if (loc != null) {
                userLat = loc.getLatitude();
                userLng = loc.getLongitude();
            }
        });
    }

    // DELETE OLD BOOKINGS
    private void deleteOldBookings() {
        bookingDB.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                Calendar today = Calendar.getInstance();

                for (DataSnapshot item : snapshot.getChildren()) {

                    String dateStr = item.child("date").getValue(String.class);
                    if (dateStr == null) continue;

                    try {
                        String[] p = dateStr.split("/");
                        Calendar b = Calendar.getInstance();
                        b.set(Integer.parseInt(p[2]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[0]));

                        if (b.before(today)) item.getRef().removeValue();

                    } catch (Exception ignored) {}
                }
            }

            @Override public void onCancelled(DatabaseError error) {}
        });
    }
}