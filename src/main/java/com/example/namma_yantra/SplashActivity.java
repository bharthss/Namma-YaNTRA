package com.example.namma_yantra;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import android.content.Intent;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // 🔥 THIS LINE FIXES DOUBLE SPLASH
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);

        // go directly to login (no delay)
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}