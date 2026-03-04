package com.prateek.syncm;

public class ChatMessage {
    public String sender;
    public String message;
    public long timestamp;

    public ChatMessage() {}

    public ChatMessage(String sender, String message) {
        this.sender = sender;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }
}