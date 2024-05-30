package com.example.venomvision.mainPageActivity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.Toast;

import com.example.venomvision.R;
import com.example.venomvision.SnakeData;
import com.example.venomvision.SnakeDataAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SnakeDataAdapter adapter;
    private List<SnakeData> snakeList;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading data...");
        progressDialog.show();

        recyclerView = findViewById(R.id.snake_data_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        snakeList = new ArrayList<>();
        adapter = new SnakeDataAdapter(snakeList);
        recyclerView.setAdapter(adapter);

        // Retrieve data from Firebase Realtime Database
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("snake_detection_data");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                snakeList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    SnakeData snake = dataSnapshot.getValue(SnakeData.class);
                    snakeList.add(snake);
                }
                adapter.notifyDataSetChanged();

                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle errors
                Toast.makeText(HistoryActivity.this, "Failed to retrieve data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }
}