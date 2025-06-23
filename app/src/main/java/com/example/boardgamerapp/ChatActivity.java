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
import android.view.MenuItem;

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
        setContentView(R.layout.activity_chat);
        
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Chat");
        
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        
        recyclerView = findViewById(R.id.recyclerView);
        etMessage = findViewById(R.id.editText);
        btnSend = findViewById(R.id.sendButton);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatAdapter(this);
        recyclerView.setAdapter(adapter);
        
        btnSend.setOnClickListener(v -> sendMessage());
        
        loadMessages();
    }

    private void sendMessage() {
        String message = etMessage.getText().toString().trim();
        if (!message.isEmpty()) {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String userName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
            
            Map<String, Object> chatMessage = new HashMap<>();
            chatMessage.put("text", message);
            chatMessage.put("userId", userId);
            chatMessage.put("userName", userName);
            chatMessage.put("timestamp", System.currentTimeMillis());
            
            FirebaseFirestore.getInstance().collection("messages")
                .add(chatMessage)
                .addOnSuccessListener(documentReference -> {
                    etMessage.setText("");
                });
        }
    }

    private void loadMessages() {
        FirebaseFirestore.getInstance().collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    return;
                }
                
                List<Message> messages = new ArrayList<>();
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
                
                adapter.setMessages(messages);
                recyclerView.scrollToPosition(adapter.getItemCount() - 1);
            });
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.delete) {
            String messageId = adapter.getSelectedMessageId();
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            
            FirebaseFirestore.getInstance().collection("messages")
                .document(messageId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Message message = documentSnapshot.toObject(Message.class);
                    if (message != null && message.userId.equals(currentUserId)) {
                        documentSnapshot.getReference().delete();
                    }
                });
        }
        return super.onContextItemSelected(item);
    }

    private class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {
        private static final int VIEW_TYPE_SENT = 1;
        private static final int VIEW_TYPE_RECEIVED = 2;
        private List<Message> messages = new ArrayList<>();

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

        void setMessages(List<Message> messages) {
            this.messages.clear();
            this.messages.addAll(messages);
            notifyDataSetChanged();
        }

        String getSelectedMessageId() {
            return messages.get(recyclerView.getChildAdapterPosition(recyclerView.findChildViewUnder(0, recyclerView.getY()))).id;
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
            FirebaseFirestore.getInstance().collection("messages")
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