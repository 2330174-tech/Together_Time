package com.prateek.syncm;

public class VideoItem {

    public String videoId = "";
    public String title = "";
    public String addedBy = "";
    public int votes = 0;

    // 🔥 Required empty constructor for Firebase
    public VideoItem() {
    }

    // Constructor used when adding new video
    public VideoItem(String videoId, String title, String addedBy) {
        this.videoId = videoId;
        this.title = title;
        this.addedBy = addedBy;
        this.votes = 0;
    }

    // Optional full constructor (useful if needed later)
    public VideoItem(String videoId, String title, String addedBy, int votes) {
        this.videoId = videoId;
        this.title = title;
        this.addedBy = addedBy;
        this.votes = votes;
    }
}