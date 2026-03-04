package com.prateek.syncm;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.prateek.syncm.databinding.ActivityRoomBinding;

import java.util.UUID;

public class RoomActivity extends AppCompatActivity {

    private ActivityRoomBinding binding;
    private String userName;
    private DatabaseReference mDatabase;
    private String createdRoomId; // 🔥 store created room ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityRoomBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        userName = getIntent().getStringExtra("userName");

        if (userName == null || userName.isEmpty()) {
            userName = "Guest";
        }

        binding.tvWelcome.setText("Welcome, " + userName + "!");

        mDatabase = FirebaseDatabase.getInstance().getReference("rooms");

        binding.btnCreateRoom.setOnClickListener(v -> createRoom());
        binding.btnJoinRoom.setOnClickListener(v -> joinRoom());

        // 🔥 Share Button Click
        binding.btnShareRoom.setOnClickListener(v -> shareRoom());
    }

    private void createRoom() {

        createdRoomId = UUID.randomUUID()
                .toString()
                .substring(0, 6)
                .toUpperCase();

        Room room = new Room(createdRoomId, userName);

        mDatabase.child(createdRoomId).setValue(room)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        Toast.makeText(this,
                                "Room Created! ID: " + createdRoomId,
                                Toast.LENGTH_LONG).show();

                        // Enable share button
                        binding.btnShareRoom.setEnabled(true);

                        navigateToPlayer(createdRoomId, true);

                    } else {
                        Toast.makeText(this,
                                "Failed to create room.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void joinRoom() {

        String roomId = binding.etRoomId.getText()
                .toString()
                .trim()
                .toUpperCase();

        if (roomId.isEmpty()) {
            Toast.makeText(this,
                    "Enter Room ID",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        mDatabase.child(roomId).get()
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful() && task.getResult().exists()) {

                        navigateToPlayer(roomId, false);

                    } else {
                        Toast.makeText(this,
                                "Room not found.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void shareRoom() {

        if (createdRoomId == null) {
            Toast.makeText(this,
                    "Create a room first!",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                "Join my SyncM Room!\nRoom ID: " + createdRoomId);

        startActivity(Intent.createChooser(shareIntent, "Share Room"));
    }

    private void navigateToPlayer(String roomId, boolean isHost) {

        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("roomId", roomId);
        intent.putExtra("userName", userName);
        intent.putExtra("isHost", isHost);
        startActivity(intent);
    }
}