package com.example.b07project;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.QuerySnapshot;

public class ProviderUseInviteCodeActivity extends AppCompatActivity{
    //invite code usage should check if the code exists, if code is used and if the expiry date hasnt passed in that order.
    //If its allowed it will add the child's id as a field in an array of 2-tuples (only provider
    //users will have access to it) where the first entry is the id of the child and the second
    //is the id of the associated viewing permissions (will be added later.
    private Button UICUseCode, UICGoBack;
    private EditText IcInviteCode;
    private TextView UICError;
    private ProgressBar UICprogress;
    private BackToParent bh = new BackToParent();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //always load screen first
        setContentView(R.layout.activity_provider_use_invite_code);
        initializeViews();
        setupListeners();

    }

    private void initializeViews(){
        UICUseCode = findViewById(R.id.UICUseCode);
        UICGoBack = findViewById(R.id.UICGoBack);
        IcInviteCode = findViewById(R.id.IcInviteCode);
        UICError = findViewById(R.id.UICError);
        UICprogress = findViewById(R.id.UICprogress);
    }
    private void setupListeners(){
            UICUseCode.setOnClickListener( e->attemptCodeRead());
            UICGoBack.setOnClickListener(e->bh.backTo(this));
    }
    //

    private void attemptCodeRead(){
        //get the current code in the IcInviteCode
         String code = IcInviteCode.getText().toString();
        //try to work with it
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        checkIfCodeExists(code, new InviteCodeInterface2() {
            @Override
            public void onResult(boolean exists, DocumentSnapshot findCode, DocumentSnapshot findProvider) {
                if (!exists || findCode == null || findProvider == null) {
                    runOnUiThread(() -> {
                        showError("Invite code not valid or user not found.");
                        UICError.requestFocus();
                    });
                    return;
                }
                if(exists){
                    validateInput(code, new InviteCodeInterface2() {
                        @Override
                        public void onResult(boolean exists, DocumentSnapshot findCode, DocumentSnapshot findProvider) {
                            //here we give provider perms and delete the code after.
                            //If its allowed it will add the child's id as a field in an array of 2-tuples (only provider
                            //users will have access to it) where the first entry is the id of the child and the second
                            //is the id of the associated viewing permissions (will be added later.

                            //invite code has child name and parent UID so, just search for those:
                            db.collection("children").whereEqualTo("name", findCode.get("child"))
                                    .whereEqualTo("parentId", findCode.get("uid")).get()
                                    .addOnSuccessListener(a->{
                                        if (a.isEmpty()) {
                                            showError("Child not found for this invite code.");
                                            UICError.requestFocus();
                                            return;
                                        }
                                        DocumentSnapshot doc = a.getDocuments().get(0);
                                        String childName = (String) findCode.get("child");
                                        String parentUid = (String) findCode.get("uid");
                                        if (childName == null || parentUid == null) {
                                            runOnUiThread(() -> {
                                                showError("Invite code missing required fields.");
                                                UICError.requestFocus();
                                            });
                                            return;
                                        }
                                        //If the array in childPerms already exists just add another entry.
                                        //
                                        List<String> permissions = Arrays.asList("placeholder viewing permissions");
                                        findProvider.getReference().update(
                                                "childId", FieldValue.arrayUnion(doc.getId()),
                                                "childPermissions", FieldValue.arrayUnion(permissions)
                                        );
                                        findCode.getReference().delete();
                                        Toast.makeText(ProviderUseInviteCodeActivity.this, "Success!", Toast.LENGTH_SHORT).show();
                                        bh.backTo(ProviderUseInviteCodeActivity.this
                                        );
                                    }).addOnFailureListener(e->{
                                        showError("Something Went Wrong");
                                        UICError.requestFocus();

                                    });

                        }
                    });
                }
            }
        });
    }

    //Literally checks if the code exists in the database.
    private void checkIfCodeExists(String code, InviteCodeInterface2 inter){
        //get code from input box
        //check if code exists
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        db.collection("invite_codes").document(code).get()
                .addOnSuccessListener(a->{
                    inter.onResult(a.exists(), null, null);
                }).addOnFailureListener(e->{
                    inter.onResult(false, null, null);
                });
        //If its allowed it will add the child's id as a field in an array of 2-tuples (only provider
        //users will have access to it) where the first entry is the id of the child and the second
        //is the id of the associated viewing permissions (will be added later.

    }
    private void validateInput(String code, InviteCodeInterface2 inter){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String user = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getEmail();
//invite code usage should check if the code exists, if code is used and if the expiry date hasnt passed in that order.

        Task<QuerySnapshot> t1 = db.collection("invite_codes").whereEqualTo("code", code).get();
        Task<QuerySnapshot> t2 = db.collection("users").whereEqualTo("email", user).get();

        Tasks.whenAllSuccess(t1,t2)
                .addOnSuccessListener(a->{
                    QuerySnapshot qs1 = (QuerySnapshot) a.get(0);
                    QuerySnapshot qs2 = (QuerySnapshot) a.get(1);
                    DocumentSnapshot findCode = qs1.getDocuments().isEmpty() ? null : qs1.getDocuments().get(0);
                    DocumentSnapshot findProvider = qs2.getDocuments().isEmpty() ? null : qs2.getDocuments().get(0);
                    if (findCode == null || !findCode.exists()) {
                        runOnUiThread(() -> {
                            showError("Code Used Already / Code Not Found");
                            UICError.requestFocus();
                            inter.onResult(false, findCode, findProvider);
                        });
                        return;
                    }
                    boolean codeUsed = findCode.getBoolean("codeUsed");
                    Timestamp expiryDate = findCode.getTimestamp("expiresAt");
                    if (expiryDate.toDate().before(  new Date())) {
                        runOnUiThread(() -> {
                            showError("Code Has Expired.");
                            //Delete the code from the database
                            findCode.getReference().delete();
                            UICError.requestFocus();
                            inter.onResult(false, findCode, findProvider);
                            return;
                        });
                    }else{
                        inter.onResult(true, findCode, findProvider);
                    }

                });

    }

    private void showLoading(boolean show) {
        UICprogress.setVisibility(show ? View.VISIBLE : View.GONE);
        UICUseCode.setEnabled(!show);
        UICGoBack.setEnabled(!show);
        IcInviteCode.setEnabled(!show);
    }

    //showError
    private void showError(String message) {
        UICError.setText(message);
        UICError.setVisibility(View.VISIBLE);
    }
}
