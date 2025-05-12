package com.example.boardgamerapp;

import java.util.Map;

public class GameProposal {
    public String id;
    public String game;
    public Map<String, Boolean> votes;

    public GameProposal() {} // Leerer Konstruktor für Firestore
}
