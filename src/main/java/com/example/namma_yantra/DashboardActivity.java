package com.example.namma_yantra;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class DashboardActivity extends AppCompatActivity {

    Button ownerBtn, farmerBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        ownerBtn = findViewById(R.id.ownerBtn);
        farmerBtn = findViewById(R.id.farmerBtn);

        ownerBtn.setOnClickListener(v ->
                startActivity(new Intent(this, OwnerActivity.class)));

        farmerBtn.setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class)));
    }
}