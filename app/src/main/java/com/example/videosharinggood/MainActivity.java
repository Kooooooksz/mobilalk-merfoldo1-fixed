package com.example.videosharinggood;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

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

        MenuItem profileItem = bottomNavigationView.getMenu().findItem(R.id.nav_profile);

        if (user != null) {
            String email = user.getEmail();
            textViewHello.setText("Bejelentkezve mint: " + email);
            profileItem.setTitle(email.split("@")[0]);
        } else {
            textViewHello.setText("Nem vagy bejelentkezve.");
            profileItem.setTitle("Bejelentkezés");
            profileItem.setOnMenuItemClickListener(menuItem -> {
                String title = menuItem.getTitle().toString();
                switch (title) {
                    case "Bejelentkezés":
                        startActivity(new Intent(this, LoginActivity.class));
                        return true;
                }
                return false;
            });
        }

        buttonGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                Toast.makeText(this, "Kezdőlap", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_all_videos) {
                Toast.makeText(this, "Összes videó", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_profile) {
                showProfilePopup(bottomNavigationView);
                return true;
            }
            return false;
        });
    }

    private void showProfilePopup(View anchorView) {
        PopupMenu popupMenu = new PopupMenu(this, anchorView);

        if (user != null) {
            popupMenu.getMenu().add("Profil");
            popupMenu.getMenu().add("Kijelentkezés");
        }

        popupMenu.setOnMenuItemClickListener(menuItem -> {
            String title = menuItem.getTitle().toString();
            switch (title) {
                case "Profil":
                    Intent intent = new Intent(this, ProfileActivity.class);
                    startActivity(intent);
                    return true;
                case "Kijelentkezés":
                    mAuth.signOut();
                    Toast.makeText(this, "Sikeres kijelentkezés", Toast.LENGTH_SHORT).show();
                    recreate(); // újratölti az activity-t
                    return true;
                case "Bejelentkezés":
                    startActivity(new Intent(this, LoginActivity.class));
                    return true;
            }
            return false;
        });

        popupMenu.show();
    }

}
