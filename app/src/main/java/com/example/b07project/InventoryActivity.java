package com.example.b07project;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.b07project.models.MedicineInventory;
import com.example.b07project.repository.InventoryRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.HashMap;
import java.util.Map;

public class InventoryActivity extends AppCompatActivity {

    private RecyclerView rvInventory;
    private FloatingActionButton fabAdd;
    private ProgressBar progressBar;
    private InventoryRepository repository;
    private InventoryAdapter adapter;
    private String userId;
    private Map<String, String> childrenMap = new HashMap<>(); // ID -> Name

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        repository = new InventoryRepository(this);

        rvInventory = findViewById(R.id.rvInventory);
        fabAdd = findViewById(R.id.fabAdd);
        progressBar = findViewById(R.id.progressBar);

        rvInventory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InventoryAdapter();
        rvInventory.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> showAddDialog());

        fetchChildren();
        loadInventory();
    }

    private void fetchChildren() {
        FirebaseFirestore.getInstance().collection("children")
                .whereEqualTo("parentId", userId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    childrenMap.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        childrenMap.put(doc.getId(), doc.getString("name"));
                    }
                });
    }

    private void loadInventory() {
        progressBar.setVisibility(View.VISIBLE);
        repository.getMedicines(userId, new InventoryRepository.LoadCallback() {
            @Override
            public void onSuccess(List<MedicineInventory> medicines) {
                progressBar.setVisibility(View.GONE);
                adapter.setMedicines(medicines);
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(InventoryActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_medicine, null);
        builder.setView(view);
        builder.setTitle("Add Medicine");

        EditText etName = view.findViewById(R.id.etName);
        Spinner spChild = view.findViewById(R.id.spChild);
        Spinner spType = view.findViewById(R.id.spType);
        EditText etTotalDoses = view.findViewById(R.id.etTotalDoses);
        TextView tvExpiry = view.findViewById(R.id.tvExpiry);
        
        // Setup Child Spinner
        List<String> childNames = new ArrayList<>();
        List<String> childIds = new ArrayList<>();
        childNames.add("Assign to: Me (Parent)");
        childIds.add(null);
        
        for (Map.Entry<String, String> entry : childrenMap.entrySet()) {
            childNames.add("Assign to: " + entry.getValue());
            childIds.add(entry.getKey());
        }
        
        ArrayAdapter<String> childAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, childNames);
        childAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spChild.setAdapter(childAdapter);

        // Setup Type Spinner
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Rescue", "Controller"});
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spType.setAdapter(typeAdapter);

        final Calendar calendar = Calendar.getInstance();
        final Date[] expiryDate = {null};

        tvExpiry.setOnClickListener(v -> {
            new DatePickerDialog(this, (view1, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                expiryDate[0] = calendar.getTime();
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
                tvExpiry.setText(sdf.format(expiryDate[0]));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String type = spType.getSelectedItem().toString();
            String dosesStr = etTotalDoses.getText().toString().trim();
            int selectedChildIndex = spChild.getSelectedItemPosition();
            String selectedChildId = childIds.get(selectedChildIndex);
            String selectedChildName = selectedChildIndex == 0 ? "Parent" : childrenMap.get(selectedChildId);

            if (name.isEmpty() || dosesStr.isEmpty() || expiryDate[0] == null) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int doses = Integer.parseInt(dosesStr);
            MedicineInventory medicine = new MedicineInventory(userId, name, type, doses, doses, expiryDate[0], new Date());
            medicine.setChildId(selectedChildId);
            medicine.setChildName(selectedChildName);

            repository.addMedicine(medicine, new InventoryRepository.SaveCallback() {
                @Override
                public void onSuccess() {
                    loadInventory();
                    Toast.makeText(InventoryActivity.this, "Medicine added", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(InventoryActivity.this, "Failed to add: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {
        private List<MedicineInventory> medicines = new ArrayList<>();
        private SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

        public void setMedicines(List<MedicineInventory> medicines) {
            this.medicines = medicines;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_inventory, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MedicineInventory med = medicines.get(position);
            String owner = med.getChildName() != null ? med.getChildName() : "Parent";
            holder.tvName.setText(med.getName() + " (" + owner + ")");
            holder.tvType.setText(med.getType());
            holder.tvExpiry.setText("Exp: " + sdf.format(med.getExpiryDate()));
            
            holder.pbDoses.setMax(med.getTotalDoses());
            holder.pbDoses.setProgress(med.getRemainingDoses());
            holder.tvDosesLeft.setText(med.getRemainingDoses() + "/" + med.getTotalDoses() + " doses left");

            if (med.isLow()) {
                holder.tvAlert.setVisibility(View.VISIBLE);
                holder.tvAlert.setText("LOW!");
            } else if (med.isExpired()) {
                holder.tvAlert.setVisibility(View.VISIBLE);
                holder.tvAlert.setText("EXPIRED!");
            } else {
                holder.tvAlert.setVisibility(View.GONE);
            }
            
            holder.itemView.setOnLongClickListener(v -> {
                new AlertDialog.Builder(InventoryActivity.this)
                    .setTitle("Delete Medicine")
                    .setMessage("Are you sure you want to delete " + med.getName() + "?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        repository.deleteMedicine(med.getId(), new InventoryRepository.SaveCallback() {
                            @Override
                            public void onSuccess() {
                                loadInventory();
                            }
                            @Override
                            public void onFailure(String error) {
                                Toast.makeText(InventoryActivity.this, "Failed to delete", Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return medicines.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvType, tvExpiry, tvDosesLeft, tvAlert;
            ProgressBar pbDoses;

            ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvName);
                tvType = itemView.findViewById(R.id.tvType);
                tvExpiry = itemView.findViewById(R.id.tvExpiry);
                tvDosesLeft = itemView.findViewById(R.id.tvDosesLeft);
                tvAlert = itemView.findViewById(R.id.tvAlert);
                pbDoses = itemView.findViewById(R.id.pbDoses);
            }
        }
    }
}
