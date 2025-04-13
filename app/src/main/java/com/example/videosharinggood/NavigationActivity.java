package com.example.videosharinggood;

import android.content.Context;
import android.content.Intent;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class NavigationActivity {

    private final Context context;
    private final FirebaseAuth mAuth;
    private final FirebaseUser user;

    public NavigationActivity(Context context) {
        this.context = context;
        this.mAuth = FirebaseAuth.getInstance();
        this.user = mAuth.getCurrentUser();
    }

    public void setupNavigation(BottomNavigationView bottomNavigationView) {
        // Profil menü beállítása
        MenuItem profileItem = bottomNavigationView.getMenu().findItem(R.id.nav_profile);

        if (user != null) {
            String email = user.getEmail();
            profileItem.setTitle(email.split("@")[0]);
        } else {
            profileItem.setTitle("Bejelentkezés");
        }

        // Bottom navigation listener
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                Toast.makeText(context, "Kezdőlap", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_all_videos) {
                Toast.makeText(context, "Összes videó", Toast.LENGTH_SHORT).show();
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
