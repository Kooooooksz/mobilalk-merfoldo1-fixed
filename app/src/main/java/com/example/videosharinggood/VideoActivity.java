package com.example.videosharinggood;

import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.videosharinggood.models.Video;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.Locale;

public class VideoActivity extends AppCompatActivity {

    private static final int PICK_VIDEO_REQUEST = 1;

    private EditText editTitle;
    private Button btnChooseVideo, btnUpload;
    private TextView txtVideoPath;
    private Uri videoUri;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        editTitle = findViewById(R.id.editTitle);
        btnChooseVideo = findViewById(R.id.btnChooseVideo);
        btnUpload = findViewById(R.id.btnUpload);
        txtVideoPath = findViewById(R.id.txtVideoPath);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Button click listeners
        btnChooseVideo.setOnClickListener(v -> chooseVideo());
        btnUpload.setOnClickListener(v -> uploadVideo());
    }

    private void chooseVideo() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        startActivityForResult(Intent.createChooser(intent, "Válassz videót"), PICK_VIDEO_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_VIDEO_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            videoUri = data.getData();
            txtVideoPath.setText(videoUri.getLastPathSegment());
        }
    }

    private void uploadVideo() {
        String title = editTitle.getText().toString().trim();
        if (title.isEmpty() || videoUri == null) {
            Toast.makeText(this, "Adj meg címet és válassz videót!", Toast.LENGTH_SHORT).show();
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

        // Firestore-ból lekérdezzük a felhasználót az email alapján
        db.collection("users")
                .whereEqualTo("email", userEmail)
                .get()
                .addOnSuccessListener(task -> {
                    if (!task.getDocuments().isEmpty()) {  // Ellenőrizzük, hogy vannak-e dokumentumok
                        // Ha a felhasználó megtalálható, kiolvassuk a felhasználói nevet
                        String userName = task.getDocuments().get(0).getString("username");

                        if (userName == null) {
                            userName = "Ismeretlen felhasználó";  // Ha nincs userName, akkor alapértelmezett név
                        }

                        // Az aktuális dátum formázása
                        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

                        // Példa videó URL, amit cserélhetsz az igazi tároló URL-jére
                        String videoUrl = "https://example.com/placeholder-url";

                        // Létrehozzuk a Video objektumot
                        Video video = new Video(title, date, userName, userEmail, videoUrl);

                        // Adatok hozzáadása a Firestore-hoz
                        db.collection("videos").add(video)
                                .addOnSuccessListener(documentReference -> {
                                    Toast.makeText(this, "Feltöltés sikeres", Toast.LENGTH_SHORT).show();
                                    editTitle.setText("");
                                    txtVideoPath.setText("Nincs fájl kiválasztva");
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Sikertelen adatfeltöltés", Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(this, "Nem található a felhasználó adatai!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Hiba történt a felhasználói adatok lekérdezésekor", Toast.LENGTH_SHORT).show());

    }
}
