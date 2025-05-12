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

import com.example.videosharinggood.models.Video;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.Date;

public class VideoActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;
    private static final String VIDEO_MIME_TYPE = "video/mp4";
    private static final String TAG = "VideoActivity"; // Added TAG for logging

    private EditText editTitle;
    private Button btnChooseVideo, btnUpload, btnRecordVideo;
    private TextView txtVideoPath;
    private Uri videoUri;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    private final ActivityResultLauncher<Intent> pickVideoLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    videoUri = result.getData().getData();
                    if (videoUri != null) {
                        txtVideoPath.setText(getFileNameFromUri(videoUri));
                    }
                }
            });

    private final ActivityResultLauncher<Intent> captureVideoLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && videoUri != null) {
                    txtVideoPath.setText(getFileNameFromUri(videoUri));
                } else if (result.getResultCode() != RESULT_OK) {
                    Toast.makeText(this, "Video recording cancelled", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startVideoRecording();
                } else {
                    Toast.makeText(this, "Camera permission is required to record videos", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        initializeViews();
        initializeFirebase();
        setupButtonListeners();
    }

    private void initializeViews() {
        editTitle = findViewById(R.id.editTitle);
        btnChooseVideo = findViewById(R.id.btnChooseVideo);
        btnUpload = findViewById(R.id.btnUpload);
        btnRecordVideo = findViewById(R.id.btnRecordVideo);
        txtVideoPath = findViewById(R.id.txtVideoPath);
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
    }

    private void setupButtonListeners() {
        btnChooseVideo.setOnClickListener(v -> chooseVideo());
        btnUpload.setOnClickListener(v -> uploadVideo());
        btnRecordVideo.setOnClickListener(v -> checkCameraPermissionAndRecord());
    }

    private void chooseVideo() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        pickVideoLauncher.launch(Intent.createChooser(intent, "Select a video"));
    }

    private void checkCameraPermissionAndRecord() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startVideoRecording();
        } else {
            if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                Toast.makeText(this, "Camera permission is needed to record videos", Toast.LENGTH_SHORT).show();
            }
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startVideoRecording() {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Video.Media.TITLE, "video_" + System.currentTimeMillis());
            values.put(MediaStore.Video.Media.MIME_TYPE, VIDEO_MIME_TYPE);


            videoUri = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
            if (videoUri == null) {
                Toast.makeText(this, "Error creating video file", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);
            intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); // High quality
            captureVideoLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Error starting camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadVideo() {
        if (!validateInputs()) return;

        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null || firebaseUser.getEmail() == null) {
            Toast.makeText(this, "Authentication required", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = "videos/" + System.currentTimeMillis() + ".mp4";
        StorageReference videoRef = storageRef.child(fileName);

        // Get the actual MIME type
        String actualMimeType = getMimeType(videoUri);
        Log.d(TAG, "Actual MIME Type: " + actualMimeType); // Log the MIME type

        // Set metadata with correct MIME type
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType(actualMimeType) // Use the actual MIME type
                .build();

        videoRef.putFile(videoUri, metadata)
                .addOnProgressListener(taskSnapshot -> {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    Toast.makeText(this, "Uploading: " + (int) progress + "%", Toast.LENGTH_SHORT).show();
                })
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return videoRef.getDownloadUrl();
                })
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        saveVideoToFirestore(task.getResult().toString(), firebaseUser.getEmail());
                    } else {
                        Toast.makeText(this, "Upload failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean validateInputs() {
        if (editTitle.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (videoUri == null) {
            Toast.makeText(this, "Please select or record a video first", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void saveVideoToFirestore(String videoUrl, String userEmail) {
        String date = DateFormat.getDateTimeInstance().format(new Date());

        db.collection("users")
                .whereEqualTo("email", userEmail)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String userName = querySnapshot.getDocuments().get(0).getString("username");
                    if (userName == null) userName = "Unknown";

                    Video video = new Video(
                            editTitle.getText().toString().trim(),
                            date,
                            userName,
                            userEmail,
                            videoUrl
                    );

                    db.collection("videos").add(video)
                            .addOnSuccessListener(documentReference -> {
                                resetForm();
                                Toast.makeText(this, "Video uploaded successfully", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to save video info", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading user data", Toast.LENGTH_SHORT).show();
                });
    }

    private void resetForm() {
        editTitle.setText("");
        txtVideoPath.setText("No file selected");
        videoUri = null;
    }

    private String getFileNameFromUri(Uri uri) {
        String path = uri.getPath();
        if (path != null) {
            int cut = path.lastIndexOf('/');
            if (cut != -1) {
                return path.substring(cut + 1);
            }
        }
        return "Video file";
    }

    private String getMimeType(Uri uri) {
        String mimeType = null;
        if (uri.getScheme().equals("content")) {
            try {
                mimeType = getContentResolver().getType(uri);
            } catch (Exception e) {
                Log.e(TAG, "Error getting MIME type: " + e.getMessage());
            }
        } else {
            String fileExtension = getFileExtensionFromUri(uri);
            if (fileExtension != null) {
                mimeType = getMimeTypeFromExtension(fileExtension);
            }
        }
        return mimeType;
    }

    private String getFileExtensionFromUri(Uri uri) {
        String extension = null;
        if (uri.getScheme().equals("content")) {
            try {
                android.content.ContentResolver cR = getContentResolver();
                MimeTypeMap mime = MimeTypeMap.getSingleton();
                extension = mime.getExtensionFromMimeType(cR.getType(uri));
            } catch (Exception e) {
                Log.e(TAG, "Error getting file extension: " + e.getMessage());
            }
        } else {
            extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(uri.toString());
        }
        return extension;
    }

    private String getMimeTypeFromExtension(String extension) {
        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            return mime.getMimeTypeFromExtension(extension);
        }
        return null;
    }
}

