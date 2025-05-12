package com.example.boardgamerapp;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VotingActivity extends AppCompatActivity {
    private static final String TAG = "VotingActivity";
    private static final int MAX_VOTES = 3;
    private RecyclerView recyclerView;
    private VotingAdapter adapter;
    private final List<Proposal> proposals = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Set<String> votedProposals = new HashSet<>();
    private int currentVotes = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_voting);

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

            recyclerView = findViewById(R.id.recyclerVoting);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));

            // Aktuellen User holen
            String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

            loadUserVotes(uid);
            loadProposals(uid);
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Erstellen der Activity", e);
            Toast.makeText(this, "Fehler beim Starten: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void loadUserVotes(String uid) {
        if (uid.isEmpty()) {
            Log.w(TAG, "Keine User-ID verfügbar");
            return;
        }

        db.collection("events")
            .document("current")
            .collection("votes")
            .document(uid)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    @SuppressWarnings("unchecked")
                    List<String> votes = (List<String>) documentSnapshot.get("votedProposals");
                    if (votes != null) {
                        votedProposals.addAll(votes);
                        currentVotes = votes.size();
                    }
                }
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Fehler beim Laden der Stimmen", e);
                Toast.makeText(this, "Fehler beim Laden der Stimmen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void loadProposals(String uid) {
        try {
            db.collection("events")
                .document("current")
                .collection("proposals")
                .orderBy("upvotes", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Fehler beim Laden der Vorschläge", error);
                        Toast.makeText(this, "Fehler beim Laden der Vorschläge: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    proposals.clear();
                    if (value != null && !value.isEmpty()) {
                        for (var doc : value) {
                            try {
                                String gameName = doc.getString("gameName");
                                String userId = doc.getString("userId");
                                Long upvotes = doc.getLong("upvotes");
                                
                                if (gameName != null && userId != null) {
                                    Proposal proposal = new Proposal(
                                        doc.getId(),
                                        gameName,
                                        userId,
                                        upvotes != null ? upvotes : 0
                                    );
                                    proposals.add(proposal);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Fehler beim Verarbeiten eines Vorschlags", e);
                            }
                        }
                    }

                    if (adapter == null) {
                        adapter = new VotingAdapter(proposals, "current", uid);
                        recyclerView.setAdapter(adapter);
                    } else {
                        adapter.updateProposals(proposals);
                    }
                });
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Laden der Vorschläge", e);
            Toast.makeText(this, "Fehler beim Laden der Vorschläge: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void voteForProposal(String proposalId, String uid) {
        if (uid.isEmpty()) {
            Toast.makeText(this, "Bitte melden Sie sich an, um abzustimmen", Toast.LENGTH_SHORT).show();
            return;
        }

        if (votedProposals.contains(proposalId)) {
            Toast.makeText(this, "Sie haben bereits für dieses Spiel gestimmt", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentVotes >= MAX_VOTES) {
            Toast.makeText(this, "Sie haben bereits " + MAX_VOTES + " Stimmen abgegeben", Toast.LENGTH_SHORT).show();
            return;
        }

        // Stimme in der Datenbank speichern
        db.collection("events")
            .document("current")
            .collection("votes")
            .document(uid)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                List<String> votes = new ArrayList<>();
                if (documentSnapshot.exists()) {
                    @SuppressWarnings("unchecked")
                    List<String> existingVotes = (List<String>) documentSnapshot.get("votedProposals");
                    if (existingVotes != null) {
                        votes.addAll(existingVotes);
                    }
                }
                votes.add(proposalId);

                Map<String, Object> voteData = new HashMap<>();
                voteData.put("votedProposals", votes);
                voteData.put("timestamp", FieldValue.serverTimestamp());

                db.collection("events")
                    .document("current")
                    .collection("votes")
                    .document(uid)
                    .set(voteData)
                    .addOnSuccessListener(aVoid -> {
                        // Upvote für den Vorschlag
                        db.collection("events")
                            .document("current")
                            .collection("proposals")
                            .document(proposalId)
                            .update("upvotes", FieldValue.increment(1))
                            .addOnSuccessListener(aVoid2 -> {
                                votedProposals.add(proposalId);
                                currentVotes++;
                                Toast.makeText(VotingActivity.this, 
                                    "Stimme abgegeben. Noch " + (MAX_VOTES - currentVotes) + " Stimmen übrig.", 
                                    Toast.LENGTH_SHORT).show();
                                adapter.notifyDataSetChanged();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Fehler beim Hochstufen des Vorschlags", e);
                                Toast.makeText(VotingActivity.this, "Fehler: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Fehler beim Speichern der Stimme", e);
                        Toast.makeText(VotingActivity.this, "Fehler: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
            });
    }

    private static class Proposal {
        final String id;
        final String gameName;
        final String userId;
        final long upvotes;

        Proposal(String id, String gameName, String userId, long upvotes) {
            this.id = id;
            this.gameName = gameName;
            this.userId = userId;
            this.upvotes = upvotes;
        }
    }

    private class VotingAdapter extends RecyclerView.Adapter<VotingAdapter.ProposalViewHolder> {
        private List<Proposal> proposals;
        private final String eventId;
        private final String userId;

        VotingAdapter(List<Proposal> proposals, String eventId, String userId) {
            this.proposals = proposals;
            this.eventId = eventId;
            this.userId = userId;
        }

        void updateProposals(List<Proposal> newProposals) {
            this.proposals = newProposals;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ProposalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_proposal, parent, false);
            return new ProposalViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ProposalViewHolder holder, int position) {
            Proposal proposal = proposals.get(position);
            holder.tvGameName.setText(proposal.gameName);
            holder.tvUpvotes.setText(String.valueOf(proposal.upvotes));

            // Visuelles Feedback für bereits abgegebene Stimmen
            if (votedProposals.contains(proposal.id)) {
                holder.itemView.setAlpha(0.5f);
                holder.tvUpvotes.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.darker_gray));
            } else {
                holder.itemView.setAlpha(1.0f);
                holder.tvUpvotes.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.purple_500));
            }

            holder.itemView.setOnClickListener(v -> {
                if (!votedProposals.contains(proposal.id) && currentVotes < MAX_VOTES) {
                    new AlertDialog.Builder(VotingActivity.this)
                        .setTitle("Für Spiel stimmen")
                        .setMessage("Möchten Sie für \"" + proposal.gameName + "\" stimmen?")
                        .setPositiveButton("Ja", (dialog, which) -> voteForProposal(proposal.id, userId))
                        .setNegativeButton("Nein", null)
                        .show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return proposals.size();
        }

        class ProposalViewHolder extends RecyclerView.ViewHolder {
            final TextView tvGameName;
            final TextView tvUpvotes;

            ProposalViewHolder(View itemView) {
                super(itemView);
                tvGameName = itemView.findViewById(R.id.tvGameName);
                tvUpvotes = itemView.findViewById(R.id.tvUpvotes);
            }
        }
    }
} 