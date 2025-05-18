package com.example.videosharinggood;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.google.firebase.firestore.Query;
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
    private Spinner spinnerSort;

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
        setupSpinner();
        fetchVideosFromFirestoreOrdered("date"); // alapértelmezett rendezés dátum szerint
    }

    private void initializeViews() {
        editTitle = findViewById(R.id.editTitle);
        btnChooseVideo = findViewById(R.id.btnChooseVideo);
        btnUpload = findViewById(R.id.btnUpload);
        btnRecordVideo = findViewById(R.id.btnRecordVideo);
        txtVideoPath = findViewById(R.id.txtVideoPath);
        recyclerView = findViewById(R.id.recyclerView);
        spinnerSort = findViewById(R.id.spinnerSort); // Spinner ID a layoutban legyen ez
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

    private void setupSpinner() {
        String[] sortOptions = {"Date", "Title", "Username"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sortOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSort.setAdapter(adapter);

        spinnerSort.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                String selected = sortOptions[position];
                String orderField = "date"; // default

                switch (selected) {
                    case "Date":
                        orderField = "uploadDate";  // Ez legyen a Firestore mező neve, vagy ha kell, módosítsd
                        break;
                    case "Title":
                        orderField = "title";
                        break;
                    case "Username":
                        orderField = "uploaderUsername";
                        break;
                }

                fetchVideosFromFirestoreOrdered(orderField);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
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

        String fileName = "videos/" + System.currentTimeMillis() + getFileExtension(videoUri);
        StorageReference videoRef = storageRef.child(fileName);

        // Get the actual MIME type of the file
        String mimeType = getMimeType(videoUri);

        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType(mimeType != null ? mimeType : "video/*")
                .build();

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

    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        String extension = mime.getExtensionFromMimeType(contentResolver.getType(uri));
        return extension != null ? "." + extension : ".mp4"; // default to .mp4 if unknown
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
                                fetchVideosFromFirestoreOrdered(spinnerSort.getSelectedItem().toString().toLowerCase());
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to save video info", Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error retrieving user", Toast.LENGTH_SHORT).show());
    }

    private void fetchVideosFromFirestoreOrdered(String orderField) {
        Query query;
        if (orderField.equals("uploadDate")) {
            query = db.collection("videos").orderBy("uploadDate", Query.Direction.DESCENDING);
        } else if (orderField.equals("title")) {
            query = db.collection("videos").orderBy("title", Query.Direction.ASCENDING);
        } else if (orderField.equals("uploaderUsername")) {
            query = db.collection("videos").orderBy("uploaderUsername", Query.Direction.ASCENDING);
        } else {
            query = db.collection("videos");
        }

        query.get()
                .addOnSuccessListener(snapshot -> {
                    videoList.clear();
                    for (DocumentSnapshot doc : snapshot) {
                        Video video = doc.toObject(Video.class);
                        if (video != null) videoList.add(video);
                    }
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
        if (path == null) return "Selected video";
        int cut = path.lastIndexOf('/');
        if (cut != -1) {
            return path.substring(cut + 1);
        }
        return path;
    }

    private String getMimeType(Uri uri) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
    }

    private void deleteVideoFromFirestore(String title) {
        db.collection("videos")
                .whereEqualTo("title", title)
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot) {
                        doc.getReference().delete();
                    }
                    Toast.makeText(this, "Video deleted", Toast.LENGTH_SHORT).show();
                    fetchVideosFromFirestoreOrdered(spinnerSort.getSelectedItem().toString().toLowerCase());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete video", Toast.LENGTH_SHORT).show());
    }
}
