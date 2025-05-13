package com.example.videosharinggood;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.videosharinggood.adapters.VideoAdapter;
import com.example.videosharinggood.models.Video;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class VideoActivity extends AppCompatActivity {

    private static final String TAG = "VideoActivity";
    private static final String VIDEO_MIME_TYPE = "video/mp4";

    private EditText editTitle;
    private Button btnChooseVideo, btnUpload, btnRecordVideo;
    private TextView txtVideoPath;
    private RecyclerView recyclerView;

    private Uri videoUri;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageRef;

    private final List<Video> videoList = new ArrayList<>();
    private VideoAdapter videoAdapter;

    // Activity Result Launchers
    private final ActivityResultLauncher<Intent> pickVideoLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    videoUri = result.getData().getData();
                    txtVideoPath.setText(getFileNameFromUri(videoUri));
                }
            });

    private final ActivityResultLauncher<Intent> captureVideoLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && videoUri != null) {
                    txtVideoPath.setText(getFileNameFromUri(videoUri));
                } else {
                    Toast.makeText(this, "Video recording cancelled", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) startVideoRecording();
                else Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        initializeViews();
        initializeFirebase();
        initializeRecyclerView();
        setupListeners();
        fetchVideosFromFirestore();
    }

    private void initializeViews() {
        editTitle = findViewById(R.id.editTitle);
        btnChooseVideo = findViewById(R.id.btnChooseVideo);
        btnUpload = findViewById(R.id.btnUpload);
        btnRecordVideo = findViewById(R.id.btnRecordVideo);
        txtVideoPath = findViewById(R.id.txtVideoPath);
        recyclerView = findViewById(R.id.recyclerView);
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();
    }

    private void initializeRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        videoAdapter = new VideoAdapter(videoList, new VideoAdapter.OnVideoActionListener() {
            @Override
            public void onEdit(Video video) {
                Intent editIntent = new Intent(VideoActivity.this, EditVideoActivity.class);
                editIntent.putExtra("title", video.getTitle());
                startActivity(editIntent);
            }

            @Override
            public void onDelete(Video video) {
                deleteVideoFromFirestore(video.getTitle());
            }
        });

        recyclerView.setAdapter(videoAdapter);
    }

    private void setupListeners() {
        btnChooseVideo.setOnClickListener(v -> chooseVideo());
        btnUpload.setOnClickListener(v -> uploadVideo());
        btnRecordVideo.setOnClickListener(v -> checkCameraPermission());
    }

    private void chooseVideo() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        pickVideoLauncher.launch(Intent.createChooser(intent, "Select a video"));
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startVideoRecording();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startVideoRecording() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.TITLE, "video_" + System.currentTimeMillis());
        values.put(MediaStore.Video.Media.MIME_TYPE, VIDEO_MIME_TYPE);

        videoUri = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);

        if (videoUri != null) {
            Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);
            intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
            captureVideoLauncher.launch(intent);
        } else {
            Toast.makeText(this, "Failed to create video file", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadVideo() {
        if (!validateInputs()) return;

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = "videos/" + System.currentTimeMillis() + ".mp4";
        StorageReference videoRef = storageRef.child(fileName);

        String mimeType = getMimeType(videoUri);
        if (!VIDEO_MIME_TYPE.equals(mimeType)) {
            Toast.makeText(this, "Only MP4 videos are allowed", Toast.LENGTH_SHORT).show();
            return;
        }

        StorageMetadata metadata = new StorageMetadata.Builder().setContentType(mimeType).build();

        videoRef.putFile(videoUri, metadata)
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    Toast.makeText(this, "Uploading: " + (int) progress + "%", Toast.LENGTH_SHORT).show();
                })
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return videoRef.getDownloadUrl();
                })
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        saveVideoToFirestore(task.getResult().toString(), user.getEmail());
                    } else {
                        Toast.makeText(this, "Upload failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean validateInputs() {
        if (editTitle.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Enter a title", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (videoUri == null) {
            Toast.makeText(this, "No video selected", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void saveVideoToFirestore(String url, String email) {
        String date = DateFormat.getDateTimeInstance().format(new Date());

        db.collection("users").whereEqualTo("email", email).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String username = snapshot.getDocuments().get(0).getString("username");
                    Video video = new Video(editTitle.getText().toString().trim(), date, username != null ? username : "Unknown", email, url);

                    db.collection("videos").add(video)
                            .addOnSuccessListener(docRef -> {
                                resetForm();
                                Toast.makeText(this, "Video uploaded", Toast.LENGTH_SHORT).show();
                                fetchVideosFromFirestore();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to save video info", Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error retrieving user", Toast.LENGTH_SHORT).show());
    }

    private void fetchVideosFromFirestore() {
        db.collection("videos").get()
                .addOnSuccessListener(snapshot -> {
                    videoList.clear();
                    snapshot.forEach(doc -> {
                        Video video = doc.toObject(Video.class);
                        videoList.add(video);
                    });
                    videoAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load videos", Toast.LENGTH_SHORT).show());
    }

    private void resetForm() {
        editTitle.setText("");
        txtVideoPath.setText("No file selected");
        videoUri = null;
    }

    private String getFileNameFromUri(Uri uri) {
        String path = uri.getPath();
        if (path != null) {
            int lastSlash = path.lastIndexOf('/');
            return lastSlash != -1 ? path.substring(lastSlash + 1) : path;
        }
        return "video.mp4";
    }

    private String getMimeType(Uri uri) {
        if ("content".equals(uri.getScheme())) {
            return getContentResolver().getType(uri);
        } else {
            String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
    }

    // Firebase video deletion logic
    private void deleteVideoFromFirestore(String videoTitle) {
        db.collection("videos")
                .whereEqualTo("title", videoTitle) // Search by title
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        String filePath = document.getString("videoUrl"); // Assume the file path is stored in 'filePath' field
                        if (filePath != null) {
                            // Get the reference to the file in Firebase Storage
                            StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(filePath);

                            // Delete the file from Storage
                            storageReference.delete()
                                    .addOnSuccessListener(aVoid -> {
                                        // Delete the video document from Firestore after successfully deleting the file
                                        document.getReference().delete()
                                                .addOnSuccessListener(aVoid1 -> {
                                                    Toast.makeText(this, videoTitle + " Video deleted successfully", Toast.LENGTH_SHORT).show();
                                                    fetchVideosFromFirestore(); // Refresh video list
                                                })
                                                .addOnFailureListener(e -> Toast.makeText(this, "Error deleting video document", Toast.LENGTH_SHORT).show());
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(this, "Error deleting video file", Toast.LENGTH_SHORT).show());
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error fetching video", Toast.LENGTH_SHORT).show());
    }


}
