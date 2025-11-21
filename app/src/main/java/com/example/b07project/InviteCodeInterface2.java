package com.example.b07project;

import com.google.firebase.firestore.DocumentSnapshot;

public interface InviteCodeInterface2{
    void onResult(boolean exists, DocumentSnapshot findCode, DocumentSnapshot findProvider);

}
