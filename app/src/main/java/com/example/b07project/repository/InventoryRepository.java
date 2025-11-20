package com.example.b07project.repository;

import com.example.b07project.models.MedicineInventory;
import com.example.b07project.utils.NotificationHelper;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class InventoryRepository {
    private final FirebaseFirestore db;
    private static final String COLLECTION = "inventory";
    private Context context; // Needed for notifications

    public InventoryRepository() {
        this.db = FirebaseFirestore.getInstance();
    }
    
    public InventoryRepository(Context context) {
        this.db = FirebaseFirestore.getInstance();
        this.context = context;
    }

    public interface LoadCallback {
        void onSuccess(List<MedicineInventory> medicines);
        void onFailure(String error);
    }

    public interface SaveCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public void addMedicine(MedicineInventory medicine, SaveCallback callback) {
        db.collection(COLLECTION)
                .add(medicine)
                .addOnSuccessListener(documentReference -> {
                    medicine.setId(documentReference.getId());
                    // Update the ID in the document
                    documentReference.update("id", documentReference.getId());
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void getMedicines(String userId, LoadCallback callback) {
        db.collection(COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<MedicineInventory> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        MedicineInventory med = doc.toObject(MedicineInventory.class);
                        med.setId(doc.getId());
                        list.add(med);
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void updateMedicine(MedicineInventory medicine, SaveCallback callback) {
        if (medicine.getId() == null) {
            callback.onFailure("Medicine ID is null");
            return;
        }
        db.collection(COLLECTION).document(medicine.getId())
                .set(medicine)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void deleteMedicine(String medicineId, SaveCallback callback) {
        db.collection(COLLECTION).document(medicineId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // Decrement doses for all medicines of a specific type for a user/child
    public void decrementDose(String userId, String childId, String type, int amount, SaveCallback callback) {
        com.google.firebase.firestore.Query query = db.collection(COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", type);
        
        if (childId != null) {
            query = query.whereEqualTo("childId", childId);
        } else {
            // If childId is null, we might want to find unassigned ones? 
            // Or maybe we just look for any matching the user if childId isn't provided?
            // For now, let's assume if childId is null, we look for items where childId is null or missing
            // But Firestore queries with null are tricky. Let's just query by userId/type and filter in code if needed.
            // Actually, let's stick to the query we have. If childId is passed, use it.
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        callback.onSuccess(); 
                        return;
                    }

                    // Find the best candidate to decrement
                    MedicineInventory target = null;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        MedicineInventory med = doc.toObject(MedicineInventory.class);
                        med.setId(doc.getId());
                        
                        // Strict matching for childId if provided
                        if (childId != null && !childId.equals(med.getChildId())) continue;
                        // If childId is null, prefer items with null childId (shared/parent items)
                        if (childId == null && med.getChildId() != null) continue;

                        if (med.getRemainingDoses() > 0) {
                            target = med;
                            break;
                        }
                    }

                    if (target != null) {
                        int newRemaining = Math.max(0, target.getRemainingDoses() - amount);
                        target.setRemainingDoses(newRemaining);
                        
                        // Check for alerts
                        if (context != null) {
                            if (target.isLow()) {
                                String owner = target.getChildName() != null ? target.getChildName() : "You";
                                NotificationHelper.sendAlert(context, userId, "Low Medicine Alert", 
                                    owner + "'s " + target.getName() + " is running low (" + newRemaining + " doses left).");
                            }
                            if (target.isExpired()) {
                                String owner = target.getChildName() != null ? target.getChildName() : "You";
                                NotificationHelper.sendAlert(context, userId, "Expired Medicine Alert", 
                                    owner + "'s " + target.getName() + " has expired.");
                            }
                        }

                        updateMedicine(target, callback);
                    } else {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}
