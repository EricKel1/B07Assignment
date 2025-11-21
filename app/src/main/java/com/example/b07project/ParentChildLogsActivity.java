package com.example.b07project;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import com.google.firebase.Timestamp;


public class ParentChildLogsActivity extends AppCompatActivity{
    private Button btnCreateCode;
    private Button btnChildInfoGoBack;
    private FirebaseAuth auth;


    private BackToParent bh = new BackToParent();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // in the page where you select a child you need
//        Intent intent = new Intent(CurrentActivity.this, NextActivity.class);
//        intent.putExtra("childName", childnamehere);
//        startActivity(intent);
        // in order to get the child's name for the hash.
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();

        //get child name from previous screen
        String childName = getIntent().getStringExtra("childName");

        setContentView(R.layout.activity_parent_child_logs);
        initializeViews();
        setupListeners(childName);
        //TEst with name john
        //setupListeners("john");

    }

    //initializeViews initializes all the views on the screen.
    private void initializeViews(){
        btnCreateCode = findViewById(R.id.btnCreateCode);
        btnChildInfoGoBack = findViewById(R.id.btnChildInfoGoBack);
        TextView tvTitle = findViewById(R.id.tvTitle2);
        TextView tvSubtitle = findViewById(R.id.textView5);
        
        String childName = getIntent().getStringExtra("childName");
        if (childName != null) {
            tvTitle.setText("Generate Code for " + childName);
            tvSubtitle.setText("Child: " + childName);
        }
    }

    //setupListeners sets up eventlisteners for the relevant views in initializeViews.
    //InitializeViews should always be called before setupListeners.
    private void setupListeners(String name){
        btnCreateCode.setOnClickListener(v -> attemptCreateCode(name));
        btnChildInfoGoBack.setOnClickListener((v -> bh.backTo(this)));
    }


    private void attemptCreateCode(String name){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }
        String email = user.getEmail();
        //try creating code
            //recursively calls itself until a unique code is made
        String code = generateInviteCode();
        makeCodeUnique(code, name, email);
    }

    private String generateInviteCode(){
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private void makeCodeUnique(String code, String name, String email){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        checkIfCodeExists(code, new InviteCodeInterface() {
            @Override
            public void onResult(boolean exists) {
                if (exists) {
                    // code exists
                    String newCode = generateInviteCode();
                    makeCodeUnique(newCode, name, email);
                } else {
                    // code does not exist
                    addToDb(code, name);
                }
            }
        });
    }

    private void checkIfCodeExists(String code, InviteCodeInterface inter){
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("invite_codes").document(code).get()
                .addOnSuccessListener(a->{
                    inter.onResult(a.exists());
                }).addOnFailureListener(e->{
                    inter.onResult(false);
                });

    }
    //name is child's name
    private void addToDb(String code, String name){
        //The code will use child's name hashed + current time + parent email hashed
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }
        String email = user.getEmail();        //should check if user auth is correct

        long expireTime = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000;
        Map<String, Object> codeData = new HashMap<>();
        codeData.put("code", code);
        codeData.put("expiresAt", new Timestamp(new Date(expireTime)));
        codeData.put("email", email);
        codeData.put("uid", user.getUid());
        codeData.put("child", name);

        db.collection("invite_codes").document(code).set(codeData)
                .addOnSuccessListener(a->{
                    //Code to go to the next page, with the code copied through
                    Intent intent = new Intent(ParentChildLogsActivity.this, ParentCreateNewCodeActivity.class);
                    intent.putExtra("providerCode", code);
                    startActivity(intent);
                }).addOnFailureListener(e->{
                    Toast.makeText(this, "Error creating code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    }



