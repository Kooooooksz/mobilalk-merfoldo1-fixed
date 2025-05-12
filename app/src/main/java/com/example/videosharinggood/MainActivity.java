package com.example.videosharinggood;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    private Button buttonGoToRegister;
    private Button buttonGoToLogin;
    private TextView textViewHello;
    private TextView textViewLoginPrompt;
    private TextView textViewLocation;
    private BottomNavigationView bottomNavigationView;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;

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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        buttonGoToRegister = findViewById(R.id.buttonGoToRegister);
        buttonGoToLogin = findViewById(R.id.buttonGoToLogin);
        textViewHello = findViewById(R.id.textViewHello);
        textViewLoginPrompt = findViewById(R.id.textViewLoginPrompt);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        textViewLocation = findViewById(R.id.textViewLocation);

        if (user != null) {
            String userId = user.getUid();
            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            textViewHello.setText("Üdvözöllek, " + (username != null ? username : "Név nem elérhető"));
                        } else {
                            textViewHello.setText("Bejelentkezve: Nincs adat a felhasználóról");
                        }
                    })
                    .addOnFailureListener(e -> textViewHello.setText("Hiba történt a felhasználó adatainak lekérésekor"));

            buttonGoToRegister.setVisibility(Button.GONE);
            buttonGoToLogin.setVisibility(Button.GONE);
            textViewLoginPrompt.setVisibility(TextView.GONE);
        } else {
            buttonGoToRegister.setVisibility(Button.VISIBLE);
            buttonGoToLogin.setVisibility(Button.VISIBLE);
            textViewLoginPrompt.setVisibility(TextView.VISIBLE);
        }

        buttonGoToRegister.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, RegisterActivity.class)));
        buttonGoToLogin.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, LoginActivity.class)));

        NavigationActivity navigationHelper = new NavigationActivity(this);
        navigationHelper.setupNavigation(bottomNavigationView);

        checkLocationPermission();
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            getLocation();
        } else {
            Toast.makeText(this, "A helyhozzáférés megtagadva", Toast.LENGTH_SHORT).show();
        }
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();

                            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                            try {
                                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                                if (addresses != null && !addresses.isEmpty()) {
                                    Address address = addresses.get(0);
                                    String country = address.getCountryName();
                                    String city = address.getLocality();
                                    String locationText = "Tartózkodási hely: " + (city != null ? city + ", " : "") + country;
                                    textViewLocation.setText(locationText);
                                } else {
                                    textViewLocation.setText("Hely nem található");
                                }
                            } catch (IOException e) {
                                textViewLocation.setText("Hiba a cím lekérésekor");
                                e.printStackTrace();
                            }
                        } else {
                            textViewLocation.setText("Lokáció nem elérhető");
                        }
                    });
        }
    }
}
