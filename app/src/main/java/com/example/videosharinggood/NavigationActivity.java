package com.example.videosharinggood;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class NavigationActivity {

    private final Context context;
    private final FirebaseAuth mAuth;
    private final FirebaseUser user;
    private final FirebaseFirestore db;

    public NavigationActivity(Context context) {
        this.context = context;
        this.mAuth = FirebaseAuth.getInstance();
        this.user = mAuth.getCurrentUser();
        this.db = FirebaseFirestore.getInstance();
    }

    public void setupNavigation(BottomNavigationView bottomNavigationView) {
        MenuItem profileItem = bottomNavigationView.getMenu().findItem(R.id.nav_profile);

        if (user != null) {
            String userId = user.getUid();

            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            profileItem.setTitle(username != null ? username : "Felhasználó");
                        } else {
                            profileItem.setTitle("Felhasználó");
                        }
                    })
                    .addOnFailureListener(e -> profileItem.setTitle("Hiba történt"));
        } else {
            profileItem.setTitle("Bejelentkezés");
            profileItem.setOnMenuItemClickListener(item -> {
                context.startActivity(new Intent(context, LoginActivity.class));
                return true;
            });
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                context.startActivity(new Intent(context, MainActivity.class));
                return true;
            } else if (id == R.id.nav_all_videos) {
                context.startActivity(new Intent(context, VideoActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                showProfilePopup(bottomNavigationView);
                return true;
            }
            return false;
        });
    }

    private void showProfilePopup(View anchorView) {
        PopupMenu popupMenu = new PopupMenu(context, anchorView);

        if (user != null) {
            popupMenu.getMenu().add("Profil");
            popupMenu.getMenu().add("Kijelentkezés");
        } else {
            popupMenu.getMenu().add("Bejelentkezés");
        }

        popupMenu.setOnMenuItemClickListener(menuItem -> {
            String title = menuItem.getTitle().toString();
            switch (title) {
                case "Profil":
                    context.startActivity(new Intent(context, ProfileActivity.class));
                    return true;
                case "Kijelentkezés":
                    mAuth.signOut();
                    Toast.makeText(context, "Sikeres kijelentkezés", Toast.LENGTH_SHORT).show();

                    // 🔐 Engedély ellenőrzése Android 13+ előtt értesítés
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                        NotificationHelper.showLogoutNotification(context);
                    } else {
                        // Android 13+: kérjük az engedélyt, ha Activity kontextus
                        if (context instanceof Activity) {
                            ActivityCompat.requestPermissions(
                                    (Activity) context,
                                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                                    101 // egyedi request code
                            );
                        }
                    }

                    ((MainActivity) context).recreate();
                    return true;
                case "Bejelentkezés":
                    context.startActivity(new Intent(context, LoginActivity.class));
                    return true;
            }
            return false;
        });

        popupMenu.show();
    }
}
