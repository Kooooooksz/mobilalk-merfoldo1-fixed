package com.example.videosharinggood;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.videosharinggood.models.Video;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class EditVideoActivity extends AppCompatActivity {

    private EditText titleEditText;
    private Button saveButton;
    private String videoId;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_video);

        titleEditText = findViewById(R.id.editTitle);
        saveButton = findViewById(R.id.btnSave);
        db = FirebaseFirestore.getInstance();

        // Get the videoId from the Intent
        videoId = getIntent().getStringExtra("title");

        // Fetch the current video details from Firestore
        fetchVideoDetails(videoId);

        // Set up save button click listener
        saveButton.setOnClickListener(v -> {
            String newTitle = titleEditText.getText().toString().trim();
            if (!newTitle.isEmpty()) {
                modifyVideoInFirestore(videoId, newTitle);
            } else {
                Toast.makeText(EditVideoActivity.this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchVideoDetails(String videoId) {
        db.collection("videos").document(videoId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Video video = documentSnapshot.toObject(Video.class);
                        if (video != null) {
                            titleEditText.setText(video.getTitle());
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(EditVideoActivity.this, "Error fetching video details", Toast.LENGTH_SHORT).show());
    }

    private void modifyVideoInFirestore(String oldTitle, String newTitle) {
        db.collection("videos")
                .whereEqualTo("title", oldTitle) // Search for video by old title
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        // Update the video document with new title
                        document.getReference().update("title", newTitle)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Video updated successfully", Toast.LENGTH_SHORT).show();
                                    Intent videoIntent = new Intent(EditVideoActivity.this, VideoActivity.class);
                                    startActivity(videoIntent);
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Error updating video", Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error fetching video", Toast.LENGTH_SHORT).show());
    }
}
