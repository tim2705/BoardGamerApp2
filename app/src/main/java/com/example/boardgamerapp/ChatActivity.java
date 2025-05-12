package com.example.boardgamerapp;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";
    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private List<Message> messages = new ArrayList<>();
    private FirebaseFirestore db;
    private TextInputEditText etMessage;
    private MaterialButton btnSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_chat);

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

            db = FirebaseFirestore.getInstance();
            recyclerView = findViewById(R.id.recyclerChat);
            etMessage = findViewById(R.id.etMessage);
            btnSend = findViewById(R.id.btnSend);

            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new ChatAdapter();
            recyclerView.setAdapter(adapter);

            btnSend.setOnClickListener(v -> {
                String message = etMessage.getText().toString().trim();
                if (message.isEmpty()) {
                    Toast.makeText(this, "Bitte geben Sie eine Nachricht ein", Toast.LENGTH_SHORT).show();
                    return;
                }

                new AlertDialog.Builder(this)
                    .setTitle("Nachricht senden")
                    .setMessage("Möchten Sie diese Nachricht wirklich senden?")
                    .setPositiveButton("Ja", (dialog, which) -> sendMessage(message))
                    .setNegativeButton("Nein", null)
                    .show();
            });

            loadMessages();
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Erstellen der Activity", e);
            Toast.makeText(this, "Fehler beim Starten: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void loadMessages() {
        try {
            db.collection("events")
                .document("current")
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Fehler beim Laden der Nachrichten", error);
                        Toast.makeText(this, "Fehler beim Laden der Nachrichten: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    messages.clear();
                    if (value != null) {
                        for (var doc : value) {
                            try {
                                Message message = new Message(
                                    doc.getId(),
                                    doc.getString("text"),
                                    doc.getString("userId"),
                                    doc.getString("userName"),
                                    doc.getLong("timestamp")
                                );
                                messages.add(message);
                            } catch (Exception e) {
                                Log.e(TAG, "Fehler beim Verarbeiten einer Nachricht", e);
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                    if (!messages.isEmpty()) {
                        recyclerView.scrollToPosition(messages.size() - 1);
                    }
                });
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Laden der Nachrichten", e);
            Toast.makeText(this, "Fehler beim Laden der Nachrichten: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void sendMessage(String message) {
        try {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String userName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
            
            Map<String, Object> chatMessage = new HashMap<>();
            chatMessage.put("text", message);
            chatMessage.put("userId", uid);
            chatMessage.put("userName", userName);
            chatMessage.put("timestamp", System.currentTimeMillis());

            db.collection("events")
                .document("current")
                .collection("messages")
                .add(chatMessage)
                .addOnSuccessListener(aVoid -> {
                    etMessage.setText("");
                    Toast.makeText(this, "Nachricht gesendet", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Fehler beim Senden der Nachricht", e);
                    Toast.makeText(this, "Fehler: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Senden der Nachricht", e);
            Toast.makeText(this, "Fehler beim Senden der Nachricht: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {
        private static final int VIEW_TYPE_SENT = 1;
        private static final int VIEW_TYPE_RECEIVED = 2;

        @Override
        public int getItemViewType(int position) {
            Message message = messages.get(position);
            return message.userId.equals(FirebaseAuth.getInstance().getCurrentUser().getUid()) 
                ? VIEW_TYPE_SENT 
                : VIEW_TYPE_RECEIVED;
        }

        @NonNull
        @Override
        public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(viewType == VIEW_TYPE_SENT ? R.layout.item_message_sent : R.layout.item_message_received, 
                    parent, false);
            return new MessageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
            Message message = messages.get(position);
            holder.tvMessage.setText(message.text);
            holder.tvUserName.setText(message.userName);
            holder.tvTime.setText(formatTime(message.timestamp));

            // Nur eigene Nachrichten können gelöscht werden
            if (message.userId.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                holder.itemView.setOnLongClickListener(v -> {
                    new AlertDialog.Builder(ChatActivity.this)
                        .setTitle("Nachricht löschen")
                        .setMessage("Möchten Sie diese Nachricht wirklich löschen?")
                        .setPositiveButton("Ja", (dialog, which) -> deleteMessage(message))
                        .setNegativeButton("Nein", null)
                        .show();
                    return true;
                });
            }
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        class MessageViewHolder extends RecyclerView.ViewHolder {
            TextView tvMessage, tvUserName, tvTime;

            MessageViewHolder(View itemView) {
                super(itemView);
                tvMessage = itemView.findViewById(R.id.tvMessage);
                tvUserName = itemView.findViewById(R.id.tvUserName);
                tvTime = itemView.findViewById(R.id.tvTime);
            }
        }
    }

    private void deleteMessage(Message message) {
        try {
            db.collection("events")
                .document("current")
                .collection("messages")
                .document(message.id)
                .delete()
                .addOnSuccessListener(aVoid -> 
                    Toast.makeText(this, "Nachricht gelöscht", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Fehler beim Löschen der Nachricht", e);
                    Toast.makeText(this, "Fehler: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Löschen der Nachricht", e);
            Toast.makeText(this, "Fehler beim Löschen der Nachricht: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.GERMAN);
        return sdf.format(new Date(timestamp));
    }

    private static class Message {
        String id;
        String text;
        String userId;
        String userName;
        long timestamp;

        Message(String id, String text, String userId, String userName, long timestamp) {
            this.id = id;
            this.text = text;
            this.userId = userId;
            this.userName = userName;
            this.timestamp = timestamp;
        }
    }
} 