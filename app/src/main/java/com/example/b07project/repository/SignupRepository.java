package com.example.b07project.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

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

    public enum EmailCheckResult { AVAILABLE, EXISTS, ERROR }

    public interface OnSignupCompleteListener {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public SignupRepository() {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
    }

    public LiveData<EmailCheckResult> checkIfEmailExists(String email) {
        MutableLiveData<EmailCheckResult> emailCheckResult = new MutableLiveData<>();
        if (email == null || email.trim().isEmpty()) {
            emailCheckResult.setValue(EmailCheckResult.ERROR);
            return emailCheckResult;
        }

        firebaseAuth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.getException() != null) {
                        Log.e(TAG, "Email check failed with an exception.", task.getException());
                        emailCheckResult.setValue(EmailCheckResult.ERROR);
                        return;
                    }

                    if (task.isSuccessful()) {
                        boolean isNewUser = task.getResult().getSignInMethods().isEmpty();
                        Log.d(TAG, "Email check successful. Is new user? " + isNewUser);
                        if (isNewUser) {
                            emailCheckResult.setValue(EmailCheckResult.AVAILABLE);
                        } else {
                            emailCheckResult.setValue(EmailCheckResult.EXISTS);
                        }
                    } else {
                        Log.w(TAG, "Email check was not successful, but did not throw an exception.");
                        emailCheckResult.setValue(EmailCheckResult.ERROR);
                    }
                });

        return emailCheckResult;
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
                            saveParentDataAndSendVerification(user, displayName, children, listener);
                        }
                    } else {
                        Log.w(TAG, "Auth account creation failed", task.getException());
                        listener.onFailure(task.getException().getMessage());
                    }
                });
    }

    private void saveParentDataAndSendVerification(FirebaseUser user, String displayName, List<ChildDraft> children, OnSignupCompleteListener listener) {
        WriteBatch batch = firestore.batch();

        DocumentReference userRef = firestore.collection("users").document(user.getUid());
        Map<String, Object> userData = new HashMap<>();
        userData.put("role", "parent");
        userData.put("displayName", displayName);
        userData.put("email", user.getEmail());
        userData.put("createdAt", new Date());
        batch.set(userRef, userData);

        for (ChildDraft child : children) {
            DocumentReference childRef = firestore.collection("children").document();
            Map<String, Object> childData = new HashMap<>();
            childData.put("parentId", user.getUid());
            childData.put("name", child.name);
            childData.put("dob", child.dob);
            childData.put("notes", child.notes);
            batch.set(childRef, childData);
        }

        batch.commit().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Firestore batch write successful.");
                sendVerificationEmail(user, listener);
            } else {
                Log.e(TAG, "Firestore batch write failed.", task.getException());
                listener.onFailure(task.getException().getMessage());
            }
        });
    }

    private void sendVerificationEmail(FirebaseUser user, OnSignupCompleteListener listener) {
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Verification email sent successfully.");
                        listener.onSuccess();
                    } else {
                        Log.e(TAG, "Failed to send verification email.", task.getException());
                        listener.onFailure("Couldn't send verification email. Please try again.");
                    }
                });
    }

    public void createProviderAccount(String email, String password, String displayName, OnSignupCompleteListener listener) {
        // This method remains unchanged
    }

    private void saveProviderData(String uid, String email, String displayName, OnSignupCompleteListener listener) {
        // This method remains unchanged
    }
}
