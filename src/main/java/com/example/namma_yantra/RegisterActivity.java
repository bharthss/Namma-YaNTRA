package com.example.namma_yantra;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    EditText name, email, password;
    Button registerBtn;

    FirebaseAuth auth;
    DatabaseReference userDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // BACK BUTTON ENABLE
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        name = findViewById(R.id.name);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        registerBtn = findViewById(R.id.registerBtn);

        auth = FirebaseAuth.getInstance();
        userDB = FirebaseDatabase.getInstance().getReference("users");

        registerBtn.setOnClickListener(v -> {

            String n = name.getText().toString();
            String e = email.getText().toString();
            String p = password.getText().toString();

            if(n.isEmpty() || e.isEmpty() || p.isEmpty()){
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.createUserWithEmailAndPassword(e, p)
                    .addOnSuccessListener(result -> {

                        String uid = auth.getCurrentUser().getUid();

                        HashMap<String, String> map = new HashMap<>();
                        map.put("name", n);
                        map.put("email", e);
                        map.put("role", "farmer");

                        userDB.child(uid).setValue(map);

                        Toast.makeText(this, "Registered Successfully", Toast.LENGTH_SHORT).show();

                        // GO BACK TO LOGIN
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    })
                    .addOnFailureListener(e1 ->
                            Toast.makeText(this, e1.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    // BACK BUTTON CLICK
    @Override
    public boolean onSupportNavigateUp() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
        return true;
    }
}