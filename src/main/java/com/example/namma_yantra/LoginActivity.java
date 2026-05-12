package com.example.namma_yantra;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class LoginActivity extends AppCompatActivity {

    EditText email, password;
    Button loginBtn, registerBtn;

    FirebaseAuth auth;
    DatabaseReference userDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        loginBtn = findViewById(R.id.loginBtn);
        registerBtn = findViewById(R.id.registerBtn);

        auth = FirebaseAuth.getInstance();
        userDB = FirebaseDatabase.getInstance().getReference("users");

        loginBtn.setOnClickListener(v -> {

            String e = email.getText().toString();
            String p = password.getText().toString();

            if(e.isEmpty() || p.isEmpty()){
                Toast.makeText(this, "Enter email & password", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.signInWithEmailAndPassword(e, p)
                    .addOnSuccessListener(result -> {

                        String uid = auth.getCurrentUser().getUid();

                        userDB.child(uid).get().addOnSuccessListener(snapshot -> {

                            String role = snapshot.child("role").getValue(String.class);

                            if (role == null) {
                                Toast.makeText(this, "User data missing", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            if (role.equals("owner")) {

                                Intent i = new Intent(LoginActivity.this, OwnerActivity.class);
                                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(i);

                            } else {

                                Intent i = new Intent(LoginActivity.this, MainActivity.class);
                                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(i);
                            }
                        });
                    })
                    .addOnFailureListener(e1 ->
                            Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT).show());
        });

        registerBtn.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }
}