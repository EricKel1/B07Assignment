package com.example.b07project;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ParentAddNewChildActivity extends AppCompatActivity {
    //This class serves to serve the screen to
    //add a new child to a parent's account,
    //specifically with the underlying logic of the adding.

    private EditText childName, childDateOfBirth;

    private Button btnAddChild, btnCancelAddChild;
    private TextView ncError2;
    private ProgressBar progress3;
    private FirebaseAuth auth;

    private BackToParent bh = new BackToParent();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_add_new_child);

        auth = FirebaseAuth.getInstance();

        initializeViews();
        setupListeners();
    }



    //initializeViews initializes all the views on the screen.
    private void initializeViews(){
        childName = findViewById(R.id.childName);
        childDateOfBirth = findViewById(R.id.childDateOfBirth);
        btnAddChild = findViewById(R.id.btnAddChild);
        btnCancelAddChild = findViewById(R.id.btnCancelAddChild);
        ncError2 = findViewById(R.id.ncError2);
        progress3 = findViewById(R.id.progress3);
    }

    private void setupListeners(){
        btnAddChild.setOnClickListener(v -> attemptAddChild());
        btnCancelAddChild.setOnClickListener((v -> bh.backTo(this)));

        CalendarPicker cp = new CalendarPicker();
        childDateOfBirth.setOnClickListener(v -> cp.openCalendar(this, childDateOfBirth));
    }
    private void attemptAddChild(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String name = childName.getText().toString().trim();
        String age = childDateOfBirth.getText().toString().trim();


        if(!validateInput(name, age)) {
            return;
        }

        Map<String, Object> child = new HashMap<>();
        child.put("name", name);
        child.put("dateOfBirth", age);
        child.put("parentId", FirebaseAuth.getInstance().getCurrentUser().getUid());
        child.put("createdAt", System.currentTimeMillis());


        db.collection("children").add(child)
                .addOnSuccessListener(documentReference -> {
                    bh.backTo(this);
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());;


    }

    private boolean validateInput(String name, String age){
        if(Objects.equals(name, "") || name == null){
            showError("Please enter your child's name");
            ncError2.requestFocus();
            return false;
        }
        if(age == null || age.equals("")){
            showError("Please enter your child's date of birth");
            ncError2.requestFocus();
            return false;
        }
        return true;
    }
    //showLoading
    private void showLoading(boolean show) {
        progress3.setVisibility(show ? View.VISIBLE : View.GONE);
        btnAddChild.setEnabled(!show);
        btnCancelAddChild.setEnabled(!show);
        childName.setEnabled(!show);
        childDateOfBirth.setEnabled(!show);
    }

    //showError
    private void showError(String message) {
        ncError2.setText(message);
        ncError2.setVisibility(View.VISIBLE);
    }


}
