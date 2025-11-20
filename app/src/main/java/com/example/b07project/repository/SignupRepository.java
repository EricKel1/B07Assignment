package com.example.b07project.repository;

import android.util.Log;

import com.example.b07project.models.ChildDraft;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SignupRepository {
    private static final String TAG = "SignupRepository";
    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;

    public interface OnSignupCompleteListener {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public SignupRepository() {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
    }

    public void createParentAccount(String email, String password, String displayName, List<ChildDraft> children, OnSignupCompleteListener listener) {
        if (email == null || password == null || email.isEmpty() || password.isEmpty()) {
            listener.onFailure("Email and password cannot be empty.");
            return;
        }

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            Log.d(TAG, "Auth account created successfully. UID: " + user.getUid());
                            saveParentData(user.getUid(), email, displayName, children, listener);
                        }
                    } else {
                        Log.w(TAG, "Auth account creation failed", task.getException());
                        listener.onFailure(task.getException().getMessage());
                    }
                });
    }

    private void saveParentData(String uid, String email, String displayName, List<ChildDraft> children, OnSignupCompleteListener listener) {
        WriteBatch batch = firestore.batch();

        // 1. Create the parent user document in the 'users' collection
        DocumentReference userRef = firestore.collection("users").document(uid);
        Map<String, Object> userData = new HashMap<>();
        userData.put("role", "parent");
        userData.put("displayName", displayName);
        userData.put("email", email);
        userData.put("createdAt", new Date());
        batch.set(userRef, userData);

        // 2. Create a new document for each child in the 'children' collection
        for (ChildDraft child : children) {
            DocumentReference childRef = firestore.collection("children").document();
            Map<String, Object> childData = new HashMap<>();
            childData.put("parentId", uid);
            childData.put("name", child.name);
            childData.put("dob", child.dob);
            childData.put("notes", child.notes);
            batch.set(childRef, childData);
        }

        // 3. Commit the batch
        batch.commit().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Firestore batch write successful.");
                listener.onSuccess();
            } else {
                Log.e(TAG, "Firestore batch write failed.", task.getException());
                listener.onFailure(task.getException().getMessage());
            }
        });
    }

    public void createProviderAccount(String email, String password, String displayName, OnSignupCompleteListener listener) {
        if (email == null || password == null || email.isEmpty() || password.isEmpty()) {
            listener.onFailure("Email and password cannot be empty.");
            return;
        }

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            Log.d(TAG, "Provider auth account created successfully. UID: " + user.getUid());
                            saveProviderData(user.getUid(), email, displayName, listener);
                        }
                    } else {
                        Log.w(TAG, "Provider auth account creation failed", task.getException());
                        listener.onFailure(task.getException().getMessage());
                    }
                });
    }

    private void saveProviderData(String uid, String email, String displayName, OnSignupCompleteListener listener) {
        DocumentReference userRef = firestore.collection("users").document(uid);
        Map<String, Object> userData = new HashMap<>();
        userData.put("role", "provider");
        userData.put("displayName", displayName);
        userData.put("email", email);
        userData.put("createdAt", new Date());

        userRef.set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Provider Firestore data saved.");
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Provider Firestore save failed.", e);
                    listener.onFailure(e.getMessage());
                });
    }
}
