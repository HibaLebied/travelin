package com.example.travelin;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SyncActivity extends AppCompatActivity {
    private ProfilePreferences preferences;
    private TextView lastSyncText;
    private MaterialButton syncButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);
        preferences = new ProfilePreferences(this);
        lastSyncText = findViewById(R.id.txt_last_sync);
        syncButton = findViewById(R.id.btn_start_sync);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        SwitchMaterial autoSync = findViewById(R.id.switch_auto_sync);
        autoSync.setChecked(preferences.getBoolean("auto_sync", true));
        autoSync.setOnCheckedChangeListener((buttonView, isChecked) -> preferences.putBoolean("auto_sync", isChecked));
        lastSyncText.setText(preferences.getString("last_sync", "Aujourd'hui à 08:42"));
        bindAction(findViewById(R.id.row_export), R.drawable.ic_settings_export, "Exporter mes données", "Télécharger une copie de vos données");
        bindAction(findViewById(R.id.row_restore), R.drawable.ic_settings_cloud, "Restaurer une sauvegarde", "Restaurer vos données depuis le cloud");
        bindAction(findViewById(R.id.row_cache), R.drawable.ic_settings_trash, "Vider le cache", "Libérer de l’espace sur votre appareil");
        findViewById(R.id.row_export).setOnClickListener(v -> toast("Export de données simulé"));
        findViewById(R.id.row_restore).setOnClickListener(v -> toast("Restauration simulée"));
        findViewById(R.id.row_cache).setOnClickListener(v -> toast("Cache vidé"));
        syncButton.setOnClickListener(v -> startSync());
    }

    private void startSync() {
        syncButton.setEnabled(false);
        syncButton.setText("Synchronisation...");
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            String value = "Aujourd'hui à " + new SimpleDateFormat("HH:mm", Locale.FRANCE).format(new Date());
            preferences.putString("last_sync", value);
            lastSyncText.setText(value);
            syncButton.setEnabled(true);
            syncButton.setText("Lancer la synchronisation");
            Toast.makeText(this, "Synchronisation terminée", Toast.LENGTH_SHORT).show();
        }, 2000);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void bindAction(View root, int iconRes, String title, String subtitle) {
        ((ImageView) root.findViewById(R.id.img_action_icon)).setImageResource(iconRes);
        ((TextView) root.findViewById(R.id.txt_action_title)).setText(title);
        ((TextView) root.findViewById(R.id.txt_action_subtitle)).setText(subtitle);
    }
}
