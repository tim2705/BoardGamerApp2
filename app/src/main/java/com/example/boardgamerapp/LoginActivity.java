package com.example.boardgamerapp;
import android.content.Intent; import android.os.Bundle;
import android.widget.Button; import android.widget.EditText; import android.widget.TextView; import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {
    FirebaseAuth auth; EditText etEmail, etPassword; Button btnLogin; TextView tvGoRegister;
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_login);
        auth = FirebaseAuth.getInstance();
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoRegister = findViewById(R.id.tvGoRegister);
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString();
            String pass = etPassword.getText().toString();
            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Bitte alle Felder ausfüllen", Toast.LENGTH_SHORT).show();
                return;
            }
            auth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
        });
        tvGoRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }
    @Override protected void onStart() { super.onStart(); if(auth.getCurrentUser()!=null){ startActivity(new Intent(this, MainActivity.class)); finish(); }}
}