package com.example.b07project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.b07project.adapters.ChildAdapter;
import com.example.b07project.main.WelcomeActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParentDashboardActivity extends AppCompatActivity {

    private RecyclerView rvChildren;
    private ProgressBar progressBar;
    private ChildAdapter adapter;
    private List<Map<String, String>> childrenList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_dashboard);

        rvChildren = findViewById(R.id.rvChildren);
        progressBar = findViewById(R.id.progressBar);
        Button btnLogout = findViewById(R.id.btnLogout);

        childrenList = new ArrayList<>();
        adapter = new ChildAdapter(childrenList, this::onGenerateCodeClicked);
        rvChildren.setLayoutManager(new LinearLayoutManager(this));
        rvChildren.setAdapter(adapter);

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(ParentDashboardActivity.this, WelcomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        loadChildren();
    }

    private void loadChildren() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        progressBar.setVisibility(View.VISIBLE);
        FirebaseFirestore.getInstance().collection("children")
                .whereEqualTo("parentId", user.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);
                    childrenList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Map<String, String> child = new HashMap<>();
                        child.put("id", document.getId());
                        child.put("name", document.getString("name"));
                        childrenList.add(child);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load children: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void onGenerateCodeClicked(String childName, String childId) {
        Intent intent = new Intent(this, ParentChildLogsActivity.class);
        intent.putExtra("childName", childName);
        // ParentChildLogsActivity seems to expect "childName" to generate the code.
        // It doesn't seem to use childId in the current implementation I saw, 
        // but passing it might be useful later.
        startActivity(intent);
    }
}
