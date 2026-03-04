package com.prateek.syncm;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.prateek.syncm.databinding.ActivityPlayerBinding;

import java.util.ArrayList;
import java.util.List;

public class PlayerActivity extends AppCompatActivity {

    private ActivityPlayerBinding binding;

    private String roomId, userName;
    private boolean isHost;

    private DatabaseReference roomRef;
    private YouTubePlayer activePlayer;

    private List<ChatMessage> chatList = new ArrayList<>();
    private ChatAdapter chatAdapter;

    private List<VideoItem> queueList = new ArrayList<>();
    private QueueAdapter queueAdapter;

    private boolean isSyncing = false;
    private String currentVideoId = "";
    private long lastSyncTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        roomId = getIntent().getStringExtra("roomId");
        userName = getIntent().getStringExtra("userName");
        isHost = getIntent().getBooleanExtra("isHost", false);

        if (roomId == null || roomId.isEmpty()) {
            Toast.makeText(this, "Invalid Room", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        roomRef = FirebaseDatabase.getInstance()
                .getReference("rooms")
                .child(roomId);

        // 🔥 Share Room Button
        binding.btnShareRoom.setOnClickListener(v -> shareRoom());

        // Only host sees share button
        if (!isHost) {
            binding.btnShareRoom.setVisibility(View.GONE);
        }

        setupTabs();
        setupYouTubePlayer();
        setupChat();
        setupQueue();
        listenToRoomChanges();
    }

    private void setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(
                new TabLayout.OnTabSelectedListener() {

                    @Override
                    public void onTabSelected(TabLayout.Tab tab) {
                        if (tab.getPosition() == 0) {
                            binding.llChatContainer.setVisibility(View.VISIBLE);
                            binding.llQueueContainer.setVisibility(View.GONE);
                        } else {
                            binding.llChatContainer.setVisibility(View.GONE);
                            binding.llQueueContainer.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override public void onTabUnselected(TabLayout.Tab tab) {}
                    @Override public void onTabReselected(TabLayout.Tab tab) {}
                });
    }

    private void setupYouTubePlayer() {

        getLifecycle().addObserver(binding.youtubePlayerView);

        binding.youtubePlayerView.addYouTubePlayerListener(
                new AbstractYouTubePlayerListener() {

                    @Override
                    public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                        activePlayer = youTubePlayer;
                    }

                    @Override
                    public void onStateChange(@NonNull YouTubePlayer youTubePlayer,
                                              @NonNull PlayerConstants.PlayerState state) {

                        if (isHost && !isSyncing) {

                            if (state == PlayerConstants.PlayerState.PLAYING) {
                                roomRef.child("isPlaying").setValue(true);
                            } else if (state == PlayerConstants.PlayerState.PAUSED) {
                                roomRef.child("isPlaying").setValue(false);
                            }
                        }
                    }

                    @Override
                    public void onCurrentSecond(@NonNull YouTubePlayer youTubePlayer,
                                                float second) {

                        if (isHost && !isSyncing) {

                            long currentTime = System.currentTimeMillis();

                            if (currentTime - lastSyncTime > 3000) {
                                roomRef.child("currentPosition")
                                        .setValue((long) second);
                                lastSyncTime = currentTime;
                            }
                        }
                    }
                });
    }

    private void listenToRoomChanges() {

        // Video ID
        roomRef.child("currentVideoId")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        String videoId = snapshot.getValue(String.class);

                        if (videoId != null &&
                                activePlayer != null &&
                                !videoId.equals(currentVideoId)) {

                            currentVideoId = videoId;
                            activePlayer.loadVideo(videoId, 0);
                        }
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });

        // Play / Pause
        roomRef.child("isPlaying")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        Boolean playing = snapshot.getValue(Boolean.class);

                        if (!isHost && playing != null && activePlayer != null) {
                            if (playing) activePlayer.play();
                            else activePlayer.pause();
                        }
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });

        // Seek
        roomRef.child("currentPosition")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        Long position = snapshot.getValue(Long.class);

                        if (!isHost && position != null && activePlayer != null) {
                            activePlayer.seekTo(position.floatValue());
                        }
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });

        // Chat
        roomRef.child("chat")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        List<ChatMessage> newChat = new ArrayList<>();

                        for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                            ChatMessage msg =
                                    postSnapshot.getValue(ChatMessage.class);
                            if (msg != null) newChat.add(msg);
                        }

                        updateChat(newChat);
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });

        // Queue
        roomRef.child("queue")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        List<VideoItem> newQueue = new ArrayList<>();

                        for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                            VideoItem item =
                                    postSnapshot.getValue(VideoItem.class);
                            if (item != null) newQueue.add(item);
                        }

                        updateQueue(newQueue);
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void setupChat() {

        chatAdapter = new ChatAdapter(chatList);
        binding.rvChat.setLayoutManager(new LinearLayoutManager(this));
        binding.rvChat.setAdapter(chatAdapter);

        binding.btnSendMessage.setOnClickListener(v -> {

            String msgText = binding.etChatMessage
                    .getText()
                    .toString()
                    .trim();

            if (!msgText.isEmpty()) {

                ChatMessage message =
                        new ChatMessage(userName, msgText);

                roomRef.child("chat").push().setValue(message);
                binding.etChatMessage.setText("");
            }
        });
    }

    private void setupQueue() {

        queueAdapter = new QueueAdapter(queueList, roomRef, isHost);
        binding.rvQueue.setLayoutManager(new LinearLayoutManager(this));
        binding.rvQueue.setAdapter(queueAdapter);

        binding.btnAddQueue.setOnClickListener(v -> {

            String videoId =
                    binding.etVideoId.getText().toString().trim();

            if (!videoId.isEmpty()) {

                VideoItem item = new VideoItem(
                        videoId,
                        "Video " + videoId,
                        userName
                );

                roomRef.child("queue").push().setValue(item);
                binding.etVideoId.setText("");

                Toast.makeText(this,
                        "Added to queue",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateChat(List<ChatMessage> newChat) {
        chatList.clear();
        chatList.addAll(newChat);
        chatAdapter.notifyDataSetChanged();

        if (!chatList.isEmpty()) {
            binding.rvChat.scrollToPosition(chatList.size() - 1);
        }
    }

    private void updateQueue(List<VideoItem> newQueue) {
        queueList.clear();
        queueList.addAll(newQueue);
        queueAdapter.notifyDataSetChanged();
    }

    private void shareRoom() {

        String shareText = "🎬 Join my SyncM Room!\n\n"
                + "Room ID: " + roomId + "\n\n"
                + "Open SyncM app and enter this Room ID.";

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

        startActivity(Intent.createChooser(shareIntent, "Share Room via"));
    }

    @Override
    public void onBackPressed() {
        leaveRoom();
        super.onBackPressed();
    }

    private void leaveRoom() {
        if (isHost) {
            roomRef.removeValue();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding.youtubePlayerView.release();
    }
}