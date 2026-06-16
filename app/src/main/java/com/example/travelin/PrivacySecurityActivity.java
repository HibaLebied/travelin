package com.example.travelin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class PrivacySecurityActivity extends AppCompatActivity {
    private ProfilePreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_security);
        preferences = new ProfilePreferences(this);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        bindAction(findViewById(R.id.row_change_password), R.drawable.ic_settings_password, "Changer le mot de passe", "Mettez à jour votre mot de passe régulièrement.");
        bindSwitch(findViewById(R.id.row_biometric), R.drawable.ic_settings_fingerprint, "Authentification biométrique", "Utiliser votre empreinte ou Face ID", "biometric_auth", true);
        bindSwitch(findViewById(R.id.row_pin_lock), R.drawable.ic_settings_password, "Verrouillage par code", "Protéger l’accès à l’application", "pin_lock", true);
        bindAction(findViewById(R.id.row_permissions), R.drawable.ic_settings_shield, "Gérer les autorisations", "Voir et modifier les autorisations accordées");
        bindAction(findViewById(R.id.row_download_data), R.drawable.ic_settings_cloud, "Télécharger mes données", "Obtenez une copie de vos données");
        findViewById(R.id.row_change_password).setOnClickListener(v -> toast("Changer le mot de passe"));
        findViewById(R.id.row_permissions).setOnClickListener(v -> toast("Autorisations"));
        findViewById(R.id.row_download_data).setOnClickListener(v -> toast("Téléchargement des données"));
        findViewById(R.id.row_delete_account).setOnClickListener(v -> toast("Suppression du compte"));
        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation_static);
        bottomNavigationView.setSelectedItemId(R.id.nav_settings);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_alerts) {
                startActivity(new Intent(this, NotificationsActivity.class));
                return true;
            } else if (itemId == R.id.nav_settings) {
                finish();
                return true;
            } else if (itemId == R.id.nav_memories) {
                toast("Memories clicked");
            }
            return false;
        });
    }

    private void bindSwitch(View root, int iconRes, String title, String subtitle, String key, boolean defaultValue) {
        ((ImageView) root.findViewById(R.id.img_switch_icon)).setImageResource(iconRes);
        ((TextView) root.findViewById(R.id.txt_switch_title)).setText(title);
        ((TextView) root.findViewById(R.id.txt_switch_subtitle)).setText(subtitle);
        SwitchMaterial switchView = root.findViewById(R.id.switch_row_switch);
        switchView.setChecked(preferences.getBoolean(key, defaultValue));
        switchView.setOnCheckedChangeListener((buttonView, isChecked) -> preferences.putBoolean(key, isChecked));
    }

    private void bindAction(View root, int iconRes, String title, String subtitle) {
        ((ImageView) root.findViewById(R.id.img_action_icon)).setImageResource(iconRes);
        ((TextView) root.findViewById(R.id.txt_action_title)).setText(title);
        ((TextView) root.findViewById(R.id.txt_action_subtitle)).setText(subtitle);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
