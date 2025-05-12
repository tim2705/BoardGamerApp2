package com.example.boardgamerapp;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import androidx.activity.OnBackPressedCallback;

public class ProposalActivity extends AppCompatActivity {
    private TextInputEditText etGameName;
    private MaterialButton btnSubmitProposal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proposal);

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

        etGameName = findViewById(R.id.etGameName);
        btnSubmitProposal = findViewById(R.id.btnSubmitProposal);

        btnSubmitProposal.setOnClickListener(v -> {
            String gameName = etGameName.getText().toString().trim();
            
            if (gameName.isEmpty()) {
                Toast.makeText(this, "Bitte geben Sie einen Spielnamen ein", Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(this)
                .setTitle("Spiel vorschlagen")
                .setMessage("Möchten Sie \"" + gameName + "\" wirklich vorschlagen?")
                .setPositiveButton("Ja", (dialog, which) -> submitProposal(gameName))
                .setNegativeButton("Nein", null)
                .show();
        });
    }

    private void submitProposal(String gameName) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Map<String, Object> proposal = new HashMap<>();
        proposal.put("gameName", gameName);
        proposal.put("userId", uid);
        proposal.put("timestamp", System.currentTimeMillis());
        proposal.put("upvotes", 0);

        FirebaseFirestore.getInstance()
            .collection("events")
            .document("current")
            .collection("proposals")
            .document()
            .set(proposal)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Spiel erfolgreich vorgeschlagen", Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> 
                Toast.makeText(this, "Fehler: " + e.getMessage(), Toast.LENGTH_LONG).show()
            );
    }
} 