package com.example.boardgamerapp;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.recyclerview.widget.RecyclerView;
import java.util.*;
public class VotingAdapter extends RecyclerView.Adapter<VotingAdapter.VoteHolder> {
    private final List<GameProposal> items; private final String eventId,uid;
    public VotingAdapter(List<GameProposal> items,String eventId,String uid){ this.items=items; this.eventId=eventId; this.uid=uid; }
    @NonNull @Override public VoteHolder onCreateViewHolder(@NonNull ViewGroup p,int v){ return new VoteHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_proposal,p,false)); }
    @Override public void onBindViewHolder(@NonNull VoteHolder h,int pos){
        GameProposal p=items.get(pos);
        h.tvGame.setText(p.game);
        h.tvVotes.setText(String.valueOf(p.votes));
        // Top-3 farblich hervorheben
        if (pos == 0) {
            h.itemView.setBackgroundColor(Color.parseColor("#FFD700")); // Gold
        } else if (pos == 1) {
            h.itemView.setBackgroundColor(Color.parseColor("#C0C0C0")); // Silber
        } else if (pos == 2) {
            h.itemView.setBackgroundColor(Color.parseColor("#CD7F32")); // Bronze
        } else {
            h.itemView.setBackgroundColor(Color.WHITE); // Standard
        }
        h.btnUpvote.setOnClickListener(v->FirebaseHelper.getInstance().vote(eventId,p.id,uid,true).addOnSuccessListener(a->h.btnUpvote.setEnabled(false)));
    }
    @Override public int getItemCount(){ return items.size(); }
    static class VoteHolder extends RecyclerView.ViewHolder{
        TextView tvGame, tvVotes; Button btnUpvote;
        VoteHolder(View iv){ super(iv); tvGame=iv.findViewById(R.id.tvGameName); tvVotes=iv.findViewById(R.id.tvVotes); btnUpvote=iv.findViewById(R.id.btnUpvote); }
    }
}