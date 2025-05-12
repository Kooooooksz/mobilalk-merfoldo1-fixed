package com.example.videosharinggood;

import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.videosharinggood.models.Video;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;

public class VideoActivity extends AppCompatActivity {

    private static final int PICK_VIDEO_REQUEST = 1;
    private static final int CAPTURE_VIDEO_REQUEST = 2;

    private EditText editTitle;
    private Button btnChooseVideo, btnUpload, btnRecordVideo;
    private TextView txtVideoPath;
    private Uri videoUri;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    private String currentVideoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        editTitle = findViewById(R.id.editTitle);
        btnChooseVideo = findViewById(R.id.btnChooseVideo);
        btnUpload = findViewById(R.id.btnUpload);
        btnRecordVideo = findViewById(R.id.btnRecordVideo);
        txtVideoPath = findViewById(R.id.txtVideoPath);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        btnChooseVideo.setOnClickListener(v -> chooseVideo());
        btnUpload.setOnClickListener(v -> uploadVideo());
        btnRecordVideo.setOnClickListener(v -> recordVideo());
    }

    private void chooseVideo() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        startActivityForResult(Intent.createChooser(intent, "Válassz videót"), PICK_VIDEO_REQUEST);
    }

    private void recordVideo() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            File videoFile;
            try {
                videoFile = createVideoFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Nem sikerült létrehozni a fájlt", Toast.LENGTH_SHORT).show();
                return;
            }

            if (videoFile != null) {
                videoUri = FileProvider.getUriForFile(this,
                        "com.example.videosharinggood.fileprovider",
                        videoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);
                startActivityForResult(intent, CAPTURE_VIDEO_REQUEST);
            }
        }
    }

    private File createVideoFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String videoFileName = "VIDEO_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        File video = File.createTempFile(videoFileName, ".mp4", storageDir);
        currentVideoPath = video.getAbsolutePath();
        return video;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ((requestCode == PICK_VIDEO_REQUEST || requestCode == CAPTURE_VIDEO_REQUEST)
                && resultCode == RESULT_OK) {
            if (requestCode == PICK_VIDEO_REQUEST && data != null) {
                videoUri = data.getData();
            }
            txtVideoPath.setText(videoUri.getLastPathSegment());
        }
    }

    private void uploadVideo() {
        String title = editTitle.getText().toString().trim();
        if (title.isEmpty() || videoUri == null) {
            Toast.makeText(this, "Adj meg címet és válassz vagy rögzíts videót!", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(this, "Nem vagy bejelentkezve!", Toast.LENGTH_SHORT).show();
            return;
        }

        String userEmail = firebaseUser.getEmail();
        if (userEmail == null) {
            Toast.makeText(this, "Email nem található!", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = "videos/" + System.currentTimeMillis() + ".mp4";
        StorageReference videoRef = storageRef.child(fileName);

        UploadTask uploadTask = videoRef.putFile(videoUri);
        uploadTask.addOnSuccessListener(taskSnapshot ->
                videoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String videoUrl = uri.toString();
                    String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

                    db.collection("users")
                            .whereEqualTo("email", userEmail)
                            .get()
                            .addOnSuccessListener(task -> {
                                if (!task.getDocuments().isEmpty()) {
                                    String userName = task.getDocuments().get(0).getString("username");
                                    if (userName == null) userName = "Ismeretlen felhasználó";

                                    Video video = new Video(title, date, userName, userEmail, videoUrl);

                                    db.collection("videos").add(video)
                                            .addOnSuccessListener(documentReference -> {
                                                Toast.makeText(this, "Videó feltöltve", Toast.LENGTH_SHORT).show();
                                                editTitle.setText("");
                                                txtVideoPath.setText("Nincs fájl kiválasztva");
                                            })
                                            .addOnFailureListener(e -> Toast.makeText(this, "Sikertelen adatfeltöltés", Toast.LENGTH_SHORT).show());
                                } else {
                                    Toast.makeText(this, "Felhasználó nem található!", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Hiba a felhasználó lekérdezésénél", Toast.LENGTH_SHORT).show());
                })
        ).addOnFailureListener(e ->
                Toast.makeText(this, "Videó feltöltése sikertelen", Toast.LENGTH_SHORT).show());
    }
}
