package com.example.boardgamerapp;

import android.os.Bundle;
import android.widget.RatingBar;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import androidx.activity.OnBackPressedCallback;

public class RatingActivity extends AppCompatActivity {
    RatingBar ratingBar, ratingBarFood, ratingBarHost;
    MaterialButton btnSubmitRating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rating);

        // Toolbar Setup
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // Moderner Back-Button Handler
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        ratingBar = findViewById(R.id.ratingBar);
        ratingBarFood = findViewById(R.id.ratingBarFood);
        ratingBarHost = findViewById(R.id.ratingBarHost);
        btnSubmitRating = findViewById(R.id.btnSubmitRating);

        btnSubmitRating.setOnClickListener(v -> {
            float rating = ratingBar.getRating();
            float foodRating = ratingBarFood.getRating();
            float hostRating = ratingBarHost.getRating();

            if (rating == 0 || foodRating == 0 || hostRating == 0) {
                Toast.makeText(this, "Bitte alle Kategorien bewerten", Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(this)
                .setTitle("Bewertung absenden")
                .setMessage("Möchten Sie Ihre Bewertung wirklich absenden?")
                .setPositiveButton("Ja", (dialog, which) -> submitRating(rating, foodRating, hostRating))
                .setNegativeButton("Nein", null)
                .show();
        });
    }

    private void submitRating(float rating, float foodRating, float hostRating) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Map<String, Object> ratings = new HashMap<>();
        ratings.put("event", rating);
        ratings.put("food", foodRating);
        ratings.put("host", hostRating);
        ratings.put("userId", uid);
        ratings.put("timestamp", System.currentTimeMillis());

        FirebaseFirestore.getInstance()
            .collection("events")
            .document("current")
            .collection("ratings")
            .document(uid)
            .set(ratings)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Bewertung erfolgreich gespeichert", Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> 
                Toast.makeText(this, "Fehler: " + e.getMessage(), Toast.LENGTH_LONG).show()
            );
    }
} 