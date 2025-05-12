package com.example.boardgamerapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import android.widget.Toast;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    TextView tvDate, tvLocation;
    private FirebaseFirestore db;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Firebase initialisieren (wichtig!)
        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();

        // Prüfen ob Benutzer eingeloggt ist
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Sicherstellen, dass die events-Collection existiert
        db.collection("events")
            .document("current")
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (!documentSnapshot.exists()) {
                    // Initiales Dokument erstellen
                    Map<String, Object> initialData = new HashMap<>();
                    initialData.put("date", System.currentTimeMillis());
                    initialData.put("host", "Noch nicht bestimmt");
                    initialData.put("avgEventRating", 0.0);
                    initialData.put("avgFoodRating", 0.0);
                    initialData.put("avgHostRating", 0.0);
                    
                    db.collection("events")
                        .document("current")
                        .set(initialData);
                }
            });

        setContentView(R.layout.activity_main);

        tvDate = findViewById(R.id.tvDate);
        tvLocation = findViewById(R.id.tvLocation);

        findViewById(R.id.btnPropose).setOnClickListener(v ->
                startActivity(new Intent(this, ProposalActivity.class))
        );
        findViewById(R.id.btnVoting).setOnClickListener(v ->
                startActivity(new Intent(this, VotingActivity.class))
        );
        findViewById(R.id.btnRating).setOnClickListener(v ->
                startActivity(new Intent(this, RatingActivity.class))
        );
        findViewById(R.id.btnChat).setOnClickListener(v ->
                startActivity(new Intent(this, ChatActivity.class))
        );
        findViewById(R.id.btnHistory).setOnClickListener(v -> {
            try {
                Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Fehler beim Öffnen des Verlaufs: " + e.getMessage(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        });

        // Logout mit Bestätigungsdialog
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Abmelden")
                .setMessage("Möchten Sie sich wirklich abmelden?")
                .setPositiveButton("Ja", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .setNegativeButton("Nein", null)
                .show();
        });

        // Nächsten Samstag, 20 Uhr berechnen
        Calendar cal = Calendar.getInstance();
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int daysUntilSaturday = (Calendar.SATURDAY - dayOfWeek + 7) % 7;
        if (daysUntilSaturday == 0 && cal.get(Calendar.HOUR_OF_DAY) >= 20) {
            daysUntilSaturday = 7; // Wenn heute Samstag nach 20 Uhr, dann nächsten Samstag
        }
        cal.add(Calendar.DAY_OF_YEAR, daysUntilSaturday);
        cal.set(Calendar.HOUR_OF_DAY, 20);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date nextSaturday = cal.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd.MM.yyyy 'um' HH:mm 'Uhr'", Locale.GERMAN);
        tvDate.setText("Nächster Spieleabend: " + sdf.format(nextSaturday));

        // Gastgeber:in automatisch bestimmen
        determineNextHost();

        findViewById(R.id.btnEndEvent).setOnClickListener(v -> endEvent());
    }

    private void determineNextHost() {
        db.collection("users")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<String> users = new ArrayList<>();
                for (var doc : queryDocumentSnapshots) {
                    users.add(doc.getString("name"));
                }
                
                if (!users.isEmpty()) {
                    // Letzten Gastgeber aus der Datenbank holen
                    db.collection("events")
                        .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .limit(1)
                        .get()
                        .addOnSuccessListener(eventDocs -> {
                            String lastHost = null;
                            if (!eventDocs.isEmpty()) {
                                lastHost = eventDocs.getDocuments().get(0).getString("host");
                            }
                            
                            // Nächsten Gastgeber bestimmen
                            String nextHost;
                            if (lastHost == null || !users.contains(lastHost)) {
                                nextHost = users.get(0);
                            } else {
                                int lastIndex = users.indexOf(lastHost);
                                nextHost = users.get((lastIndex + 1) % users.size());
                            }
                            
                            tvLocation.setText("Gastgeber:in: " + nextHost);
                            
                            // Nächsten Gastgeber in der Datenbank speichern
                            db.collection("events")
                                .document("current")
                                .update("host", nextHost);
                        });
                } else {
                    tvLocation.setText("Gastgeber:in: (noch nicht bestimmt)");
                }
            })
            .addOnFailureListener(e -> 
                tvLocation.setText("Gastgeber:in: (Fehler beim Laden)")
            );
    }

    private void endEvent() {
        try {
            // Bestätigungsdialog
            new AlertDialog.Builder(this)
                .setTitle("Spieleabend beenden")
                .setMessage("Möchten Sie den Spieleabend wirklich beenden? Alle Vorschläge und Abstimmungen werden zurückgesetzt.")
                .setPositiveButton("Ja", (dialog, which) -> {
                    // Event in der Datenbank aktualisieren
                    db.collection("events")
                        .document("current")
                        .update("status", "completed", "endTime", System.currentTimeMillis())
                        .addOnSuccessListener(aVoid -> {
                            // Vorschläge und Abstimmungen löschen
                            resetProposalsAndVotes();
                            
                            // UI aktualisieren
                            updateUI();
                            Toast.makeText(MainActivity.this, "Spieleabend beendet", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Fehler beim Beenden des Events", e);
                            Toast.makeText(MainActivity.this, "Fehler: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
                })
                .setNegativeButton("Nein", null)
                .show();
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Beenden des Events", e);
            Toast.makeText(this, "Fehler: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void resetProposalsAndVotes() {
        // Vorschläge löschen
        db.collection("events")
            .document("current")
            .collection("proposals")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                for (var doc : queryDocumentSnapshots) {
                    doc.getReference().delete();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Fehler beim Löschen der Vorschläge", e);
            });

        // Abstimmungen löschen
        db.collection("events")
            .document("current")
            .collection("votes")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                for (var doc : queryDocumentSnapshots) {
                    doc.getReference().delete();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Fehler beim Löschen der Abstimmungen", e);
            });
    }

    private void updateUI() {
        // Nächsten Samstag, 20 Uhr berechnen
        Calendar cal = Calendar.getInstance();
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int daysUntilSaturday = (Calendar.SATURDAY - dayOfWeek + 7) % 7;
        if (daysUntilSaturday == 0 && cal.get(Calendar.HOUR_OF_DAY) >= 20) {
            daysUntilSaturday = 7; // Wenn heute Samstag nach 20 Uhr, dann nächsten Samstag
        }
        cal.add(Calendar.DAY_OF_YEAR, daysUntilSaturday);
        cal.set(Calendar.HOUR_OF_DAY, 20);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date nextSaturday = cal.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd.MM.yyyy 'um' HH:mm 'Uhr'", Locale.GERMAN);
        tvDate.setText("Nächster Spieleabend: " + sdf.format(nextSaturday));

        // Gastgeber:in neu bestimmen
        determineNextHost();

        // Event-Datum in der Datenbank aktualisieren
        db.collection("events")
            .document("current")
            .update("date", nextSaturday.getTime())
            .addOnFailureListener(e -> {
                Log.e(TAG, "Fehler beim Aktualisieren des Event-Datums", e);
                Toast.makeText(this, "Fehler beim Aktualisieren des Datums: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
}
