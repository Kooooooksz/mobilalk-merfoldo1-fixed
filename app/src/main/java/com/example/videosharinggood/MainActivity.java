package com.example.videosharinggood;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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

public class MainActivity extends AppCompatActivity {

    private Button buttonGoToRegister;
    private TextView textViewHello;
    private BottomNavigationView bottomNavigationView;
    private FirebaseAuth mAuth;
    private FirebaseUser user;

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

        buttonGoToRegister = findViewById(R.id.buttonGoToRegister);
        textViewHello = findViewById(R.id.textViewHello);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        if (user != null) {
            String email = user.getEmail();
            textViewHello.setText("Bejelentkezve: " + email);
        } else {
            textViewHello.setText("Nem vagy bejelentkezve.");
        }

        buttonGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // NavigationActivity használata
        NavigationActivity navigationHelper = new NavigationActivity(this);
        navigationHelper.setupNavigation(bottomNavigationView);
    }
}
