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

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ParentAddNewExistingChildActivity extends AppCompatActivity {
    //
    private EditText childEmail, childDateOfBirth2;
    private EditText childPassword2;

    private Button btnAddChildProfile, btnCancelAddChild2;

    //An account created by a child will have a copy in users
    //with role child and parentId set to null and selfId set to something.
    //then when trying to add a child with an existing account,
    //we check if the email's associated id is found in the children collection.

    private FirebaseAuth auth;

    private BackToParent bh = new BackToParent();
    private TextView nceError;
    private ProgressBar nceProgress;
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_add_new_child_existing);
        auth = FirebaseAuth.getInstance();

        initializeViews();
        setupListeners();
    }


    //initializeViews initializes all the views on the screen.
    private void initializeViews(){
        childEmail = findViewById(R.id.childEmail);
        childPassword2 = findViewById(R.id.childPassword2);
        btnAddChildProfile = findViewById(R.id.btnAddChildProfile);
        btnCancelAddChild2 = findViewById(R.id.btnCancelAddChild2);
        nceError = findViewById(R.id.nceError);
        nceProgress = findViewById(R.id.nceProgress);
        childDateOfBirth2 = findViewById(R.id.childDateOfBirth2);


    }

    //setupListeners sets up eventlisteners for the relevant views in initializeViews.
    //InitializeViews should always be called before setupListeners.
    private void setupListeners(){
        btnAddChildProfile.setOnClickListener(v -> attemptAddChild());
        btnCancelAddChild2.setOnClickListener((v -> bh.backTo(this)));

        CalendarPicker cp = new CalendarPicker();
        childDateOfBirth2.setOnClickListener(v -> cp.openCalendar(this, childDateOfBirth2));

    }

    private void attemptAddChild(){
        String email= childEmail.getText().toString().trim();
        String password = childPassword2.getText().toString().trim();
        String age = childDateOfBirth2.getText().toString().trim();

        validateInput(email,password, age, isValid ->{
           if(isValid){
               Map<String, Object> child = new HashMap<>();
               child.put("name", email);
               child.put("dateOfBirth", age);
               child.put("parentId", FirebaseAuth.getInstance().getCurrentUser().getUid());
               child.put("createdAt", System.currentTimeMillis());
               db.collection("users").whereEqualTo("email", email).get()
                       .addOnSuccessListener(querySnapshot->{
                           DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                            doc.getReference().update("hasParent", true);
                       });

               db.collection("children").add(child)
                       .addOnSuccessListener(documentReference -> {
                           bh.backTo(this);
                       })
                       .addOnFailureListener(e -> Toast.makeText(this,
                               "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());;
           }

        });




    }

    private void validateInput(String email, String password, String age, Consumer<Boolean> callback){
          db.collection("users").whereEqualTo("email", email).get()
                  .addOnSuccessListener(querySnapshot->{
                      boolean valid = true;
                    Toast.makeText(ParentAddNewExistingChildActivity.this, "DEBUG: reached this point", Toast.LENGTH_SHORT).show();
                    if (querySnapshot.isEmpty() ) {
                runOnUiThread(() -> {
                    showError("Email may be incorrect");
                    nceError.requestFocus();
                });
                valid = false;

             } else if (Objects.equals(age, "") || Objects.equals(age, null)) {
                runOnUiThread(() -> {
                    showError("Please enter child's date of birth");
                    nceError.requestFocus();
                });
                valid = false;
            } else{
                DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                if(!Objects.equals(email, doc.getString("email"))) {
                    runOnUiThread(() -> {
                        showError("Child's email is not registered; are you sure the email is correct?");
                        nceError.requestFocus();
                    });
                    valid = false;
                } else if (!Objects.equals("child", doc.getString("role"))
                        || doc.getBoolean("hasParent") == true) {
                    runOnUiThread(() -> {
                                showError("Child is already registered with another parent.");
                                nceError.requestFocus();
                            });
                    valid = false;
                } else if (!Objects.equals(doc.getString("password"), password )) {
                    runOnUiThread(() -> {
                                showError("Password is incorrect.");
                                nceError.requestFocus();
                            });
                    valid = false;
                }
            }
            callback.accept(valid);
        })
                .addOnFailureListener(querySnapshot->{
                    runOnUiThread(() -> {
                        showError("Child's email doesn't exist.");
                        nceError.requestFocus();
                    });
            Toast.makeText(ParentAddNewExistingChildActivity.this, "Firestore fials", Toast.LENGTH_SHORT).show();
                    callback.accept(false);
        });

    }
    private void showLoading(boolean show) {
        nceProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        btnAddChildProfile.setEnabled(!show);
        btnCancelAddChild2.setEnabled(!show);
        childEmail.setEnabled(!show);
        childPassword2.setEnabled(!show);
    }

    //showError
    private void showError(String message) {
        nceError.setText(message);
        nceError.setVisibility(View.VISIBLE);
    }
}
