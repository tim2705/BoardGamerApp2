package com.example.boardgamerapp;

import com.google.firebase.Timestamp;

public class ChatMessage {
    public String senderId;
    public String text;
    public Timestamp time;

    public ChatMessage() {} // Leerer Konstruktor für Firestore

    public ChatMessage(String senderId, String text) {
        this.senderId = senderId;
        this.text = text;
        this.time = Timestamp.now();
    }
}
