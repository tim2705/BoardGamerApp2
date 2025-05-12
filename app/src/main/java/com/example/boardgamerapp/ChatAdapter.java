package com.example.boardgamerapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private final List<ChatMessage> messages;
    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }
    @NonNull
    @Override 
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ChatViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        holder.text1.setText(msg.senderId);
        holder.text2.setText(msg.text);
    }
    @Override
    public int getItemCount() {
        return messages.size();
    }
    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView text1, text2;
        ChatViewHolder(View itemView) {
            super(itemView);
            text1 = itemView.findViewById(android.R.id.text1);
            text2 = itemView.findViewById(android.R.id.text2);
        }
    }
} 