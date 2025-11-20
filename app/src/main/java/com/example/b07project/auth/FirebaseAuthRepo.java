package com.example.b07project.auth;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseAuthRepo implements AuthRepo {
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    public void signIn(String email, String password, Callback cb) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

    @Override
    public void getUserRole(String uid, RoleCallback cb) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        cb.onRole(role != null ? role : "parent"); // Default to parent if no role
                    } else {
                        cb.onRole("parent"); // Default to parent if no doc
                    }
                })
                .addOnFailureListener(cb::onError);
    }
}
