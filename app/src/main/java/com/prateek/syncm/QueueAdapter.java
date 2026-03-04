package com.prateek.syncm;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;

import java.util.List;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.QueueViewHolder> {

    private final List<VideoItem> queue;
    private final DatabaseReference roomRef;
    private boolean isHost;

    public QueueAdapter(List<VideoItem> queue,
                        DatabaseReference roomRef,
                        boolean isHost) {
        this.queue = queue;
        this.roomRef = roomRef;
        this.isHost = isHost;
    }

    public void setHost(boolean isHost) {
        this.isHost = isHost;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public QueueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);

        return new QueueViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QueueViewHolder holder, int position) {

        if (position < 0 || position >= queue.size()) return;

        VideoItem item = queue.get(position);

        if (item == null) return;

        // 🔥 Safe title
        String title = (item.title != null && !item.title.isEmpty())
                ? item.title
                : item.videoId;

        holder.tvTitle.setText(title);

        String addedBy = (item.addedBy != null) ? item.addedBy : "Unknown";
        holder.tvDetails.setText(
                "Added by: " + addedBy +
                        " | Votes: " + item.votes
        );

        holder.itemView.setOnClickListener(v -> {

            if (!isHost) return;

            String cleanVideoId = extractVideoId(item.videoId);

            if (cleanVideoId.isEmpty()) {
                Toast.makeText(v.getContext(),
                        "Invalid video ID",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // 🔥 Update room safely
            roomRef.child("currentVideoId").setValue(cleanVideoId);
            roomRef.child("currentPosition").setValue(0L);
            roomRef.child("isPlaying").setValue(true);

            Toast.makeText(v.getContext(),
                    "Playing: " + title,
                    Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return queue != null ? queue.size() : 0;
    }

    // 🔥 Extract only video ID from URL or raw input
    private String extractVideoId(String input) {

        if (input == null) return "";

        input = input.trim();

        try {
            // Full YouTube URL
            if (input.contains("v=")) {
                String id = input.substring(input.indexOf("v=") + 2);
                int ampIndex = id.indexOf("&");
                if (ampIndex != -1) {
                    id = id.substring(0, ampIndex);
                }
                return id;
            }

            // Short link
            if (input.contains("youtu.be/")) {
                return input.substring(input.lastIndexOf("/") + 1);
            }

            // Embed format
            if (input.contains("embed/")) {
                return input.substring(input.lastIndexOf("/") + 1);
            }

            // Already clean ID
            return input;

        } catch (Exception e) {
            return "";
        }
    }

    static class QueueViewHolder extends RecyclerView.ViewHolder {

        TextView tvTitle;
        TextView tvDetails;

        public QueueViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTitle = itemView.findViewById(android.R.id.text1);
            tvDetails = itemView.findViewById(android.R.id.text2);
        }
    }
}
