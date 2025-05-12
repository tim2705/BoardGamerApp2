package com.example.boardgamerapp;
import android.content.Intent; import android.os.Bundle;
import android.widget.Button; import android.widget.EditText; import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {
    FirebaseAuth auth; EditText etEmail, etPassword; Button btnRegister;
    @Override protected void onCreate(Bundle s) {
        super.onCreate(s); setContentView(R.layout.activity_register);
        auth = FirebaseAuth.getInstance();
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnRegister.setOnClickListener(v -> {
            String email=etEmail.getText().toString(), pass=etPassword.getText().toString();
            if(email.isEmpty()||pass.length()<6){ Toast.makeText(this,"Bitte gültige Daten",Toast.LENGTH_SHORT).show(); return; }
            auth.createUserWithEmailAndPassword(email,pass).addOnCompleteListener(task->{
                if(task.isSuccessful()){ FirebaseHelper.getInstance().saveNewUser(auth.getCurrentUser().getUid(),email);
                    startActivity(new Intent(this, MainActivity.class)); finish(); }
                else Toast.makeText(this,task.getException().getMessage(),Toast.LENGTH_LONG).show();
            });
        });
    }
}