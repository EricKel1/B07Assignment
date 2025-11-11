package com.example.b07project.auth;

import com.google.firebase.auth.FirebaseAuth;

public class FirebaseAuthRepo implements AuthRepo {
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    @Override
    public void signIn(String email, String password, Callback cb) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }
}
