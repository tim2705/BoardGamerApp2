package com.example.boardgamerapp;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.*;
public class FirebaseHelper {
    private static FirebaseHelper instance;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private FirebaseHelper() {}
    public static FirebaseHelper getInstance(){ if(instance==null) instance=new FirebaseHelper(); return instance; }
    public Task<DocumentSnapshot> getNextEvent(){ return db.collection("events").orderBy("date").limit(1).get().continueWith(t->t.getResult().getDocuments().get(0)); }
    public Task<Void> proposeGame(String eventId,String gameName){ Map<String,Object> d=new HashMap<>(); d.put("game",gameName); d.put("votes",new HashMap<String,Boolean>());
        return db.collection("gameProposals").document(eventId).collection("proposals").add(d).continueWith(t->null);
    }
    public Task<Void> vote(String eventId,String proposalId,String uid,boolean up){ DocumentReference r=db.collection("gameProposals").document(eventId).collection("proposals").document(proposalId);
        return r.update("votes."+uid,up);
    }
    public Task<List<GameProposal>> getProposals(String eventId){ return db.collection("gameProposals").document(eventId).collection("proposals").get().continueWith(task->{
        List<GameProposal> list=new ArrayList<>(); for(var doc:task.getResult().getDocuments()){ GameProposal p=doc.toObject(GameProposal.class); p.id=doc.getId(); list.add(p);} return list; }); }
    public Task<Void> sendMessage(String eventId,ChatMessage msg){ return db.collection("chat").document(eventId).collection("msgs").add(msg).continueWith(t->null);}
    public void saveNewUser(String uid,String email){ Map<String,Object> u=new HashMap<>(); u.put("email",email); u.put("favoriteCuisine","\""); db.collection("users").document(uid).set(u);}
    public Task<Void> rateEvent(String eventId, String uid, float rating) {
        return db.collection("events").document(eventId)
            .update("ratings." + uid, rating);
    }
}