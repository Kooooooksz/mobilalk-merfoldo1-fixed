package com.example.videosharinggood;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

public class MainActivity extends AppCompatActivity {

    private Button buttonGoToRegister;
    private Button buttonGoToLogin;
    private TextView textViewHello;
    private BottomNavigationView bottomNavigationView;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore db;

    @Override
    protected void onStop() {
        super.onStop();
        if (isFinishing()) {
            FirebaseAuth.getInstance().signOut();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        buttonGoToRegister = findViewById(R.id.buttonGoToRegister);
        buttonGoToLogin = findViewById(R.id.buttonGoToLogin);
        textViewHello = findViewById(R.id.textViewHello);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        if (user != null) {
            String userId = user.getUid();
            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Felhasználói adatok lekérése (pl. 'username')
                            String username = documentSnapshot.getString("username");
                            if (username != null) {
                                textViewHello.setText("Bejelentkezve: " + username);
                            } else {
                                textViewHello.setText("Bejelentkezve: Név nem elérhető");
                            }
                        } else {
                            textViewHello.setText("Bejelentkezve: Nincs adat a felhasználóról");
                        }
                    })
                    .addOnFailureListener(e -> {
                        textViewHello.setText("Hiba történt a felhasználó adatainak lekérésekor");
                    });
        } else {
            textViewHello.setText("Nem vagy bejelentkezve.");
        }

        buttonGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        buttonGoToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        NavigationActivity navigationHelper = new NavigationActivity(this);
        navigationHelper.setupNavigation(bottomNavigationView);
    }
}
