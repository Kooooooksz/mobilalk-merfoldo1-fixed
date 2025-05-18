package com.example.videosharinggood.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.videosharinggood.R;
import com.example.videosharinggood.models.Video;

import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private List<Video> videoList;
    private OnVideoActionListener listener;

    public enum SortType {
        TITLE_ASC, TITLE_DESC,
        DATE_ASC, DATE_DESC,
    }

    public interface OnVideoActionListener {
        void onEdit(Video video);
        void onDelete(Video video);
    }

    public VideoAdapter(List<Video> videoList, OnVideoActionListener listener) {
        this.videoList = videoList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.video_item, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        Video video = videoList.get(position);
        holder.titleTextView.setText(video.getTitle());
        holder.uploadDateTextView.setText(video.getUploadDate());
        holder.uploaderTextView.setText(video.getUploaderUsername());

        // Videó útvonal beállítása
        holder.videoView.setVideoPath(video.getVideoUrl());

        // Előnézethez az első képkockára állítjuk a videót
        holder.videoView.seekTo(1);

        // Kattintásra indítás/szüneteltetés
        holder.videoView.setOnClickListener(v -> {
            if (holder.videoView.isPlaying()) {
                holder.videoView.pause();
            } else {
                holder.videoView.start();
            }
        });

        holder.editButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEdit(video);
            }
        });

        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(video);
            }
        });
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    // Lista frissítése új adatokkal
    public void updateData(List<Video> newVideos) {
        this.videoList.clear();
        this.videoList.addAll(newVideos);
        notifyDataSetChanged();
    }

    public static class VideoViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, uploadDateTextView, uploaderTextView;
        Button editButton, deleteButton;
        VideoView videoView;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            videoView = itemView.findViewById(R.id.videoPreview);
            titleTextView = itemView.findViewById(R.id.videoTitle);
            uploadDateTextView = itemView.findViewById(R.id.videoUploadDate);
            uploaderTextView = itemView.findViewById(R.id.videoUploader);
            editButton = itemView.findViewById(R.id.btnEdit);
            deleteButton = itemView.findViewById(R.id.btnDelete);
        }
    }
}
