package com.example.boardgamerapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {
    private static final String TAG = "HistoryActivity";
    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private List<Event> events = new ArrayList<>();
    private FirebaseFirestore db;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_history);

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
            recyclerView = findViewById(R.id.recyclerView);
            swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
            tvEmpty = findViewById(R.id.tvEmpty);

            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new HistoryAdapter();
            recyclerView.setAdapter(adapter);

            swipeRefreshLayout.setOnRefreshListener(this::loadEvents);

            loadEvents();
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Erstellen der Activity", e);
            Toast.makeText(this, "Fehler beim Starten: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void loadEvents() {
        try {
            db.collection("events")
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    swipeRefreshLayout.setRefreshing(false);
                    
                    if (error != null) {
                        Log.e(TAG, "Fehler beim Laden der Events", error);
                        Toast.makeText(this, "Fehler beim Laden der Events: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    events.clear();
                    if (value != null && !value.isEmpty()) {
                        for (var doc : value) {
                            try {
                                Event event = new Event(
                                    doc.getId(),
                                    doc.getLong("date"),
                                    doc.getString("host"),
                                    doc.getDouble("avgEventRating"),
                                    doc.getDouble("avgFoodRating"),
                                    doc.getDouble("avgHostRating")
                                );
                                events.add(event);
                            } catch (Exception e) {
                                Log.e(TAG, "Fehler beim Verarbeiten eines Events", e);
                            }
                        }
                    }
                    
                    adapter.notifyDataSetChanged();
                    updateEmptyView();
                });
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Laden der Events", e);
            Toast.makeText(this, "Fehler beim Laden der Events: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void updateEmptyView() {
        if (events.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.EventViewHolder> {
        @NonNull
        @Override
        public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_history, parent, false);
            return new EventViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
            Event event = events.get(position);
            holder.tvDate.setText(formatDate(event.date));
            holder.tvHost.setText("Gastgeber: " + event.host);
            
            holder.ratingBarEvent.setRating(event.avgEventRating != null ? event.avgEventRating.floatValue() : 0);
            holder.ratingBarFood.setRating(event.avgFoodRating != null ? event.avgFoodRating.floatValue() : 0);
            holder.ratingBarHost.setRating(event.avgHostRating != null ? event.avgHostRating.floatValue() : 0);
        }

        @Override
        public int getItemCount() {
            return events.size();
        }

        class EventViewHolder extends RecyclerView.ViewHolder {
            TextView tvDate, tvHost;
            RatingBar ratingBarEvent, ratingBarFood, ratingBarHost;

            EventViewHolder(View itemView) {
                super(itemView);
                tvDate = itemView.findViewById(R.id.tvDate);
                tvHost = itemView.findViewById(R.id.tvHost);
                ratingBarEvent = itemView.findViewById(R.id.ratingBarEvent);
                ratingBarFood = itemView.findViewById(R.id.ratingBarFood);
                ratingBarHost = itemView.findViewById(R.id.ratingBarHost);
            }
        }
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd.MM.yyyy", Locale.GERMAN);
        return sdf.format(new Date(timestamp));
    }

    private static class Event {
        String id;
        long date;
        String host;
        Double avgEventRating;
        Double avgFoodRating;
        Double avgHostRating;

        Event(String id, long date, String host, Double avgEventRating, Double avgFoodRating, Double avgHostRating) {
            this.id = id;
            this.date = date;
            this.host = host;
            this.avgEventRating = avgEventRating;
            this.avgFoodRating = avgFoodRating;
            this.avgHostRating = avgHostRating;
        }
    }
} 