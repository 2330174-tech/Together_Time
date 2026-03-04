package com.prateek.syncm;

import java.util.HashMap;

public class Room {

    public String roomId;
    public String host;
    public String currentVideoId;
    public long currentPosition;
    public boolean isPlaying;

    // IMPORTANT: Use HashMap (NOT List)
    public HashMap<String, ChatMessage> chat;
    public HashMap<String, VideoItem> queue;

    // 🔥 Required empty constructor for Firebase
    public Room() {
    }

    public Room(String roomId, String host) {
        this.roomId = roomId;
        this.host = host;
        this.currentVideoId = "";
        this.currentPosition = 0;
        this.isPlaying = false;
        this.chat = new HashMap<>();
        this.queue = new HashMap<>();
    }
}