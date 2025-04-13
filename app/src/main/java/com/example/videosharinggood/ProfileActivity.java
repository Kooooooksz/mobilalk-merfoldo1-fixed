package com.example.videosharinggood;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class ProfileActivity extends AppCompatActivity {

    private TextView textViewProfile;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Firebase Firestore és Auth
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = mAuth.getCurrentUser();

        textViewProfile = findViewById(R.id.textViewProfile);

        if (user != null) {
            String userId = user.getUid();

            // Firestore adatlekérés
            db.collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String displayName = documentSnapshot.getString("username");
                            String email = documentSnapshot.getString("email");
                            String phoneNumber = documentSnapshot.getString("phoneNumber");

                            textViewProfile.setText("Felhasználó adatai:\n" +
                                    "Felhasználóév: " + (displayName != null ? displayName : "Nincs név") + "\n" +
                                    "Email: " + email + "\n" +
                                    "Telefonszám: " + phoneNumber + "\n"
                                    );
                        } else {
                            Toast.makeText(ProfileActivity.this, "Nincs ilyen felhasználó", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ProfileActivity.this, "Hiba történt az adatok betöltésekor.", Toast.LENGTH_SHORT).show();
                    });
        } else {
            textViewProfile.setText("Nincs bejelentkezett felhasználó.");
        }
    }
}
