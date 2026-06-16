package com.example.travelin;

import android.Manifest;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    showTestNotificationOnce();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        ImageButton backButton = findViewById(R.id.btn_notifications_back);
        RecyclerView recyclerView = findViewById(R.id.recycler_notifications);

        backButton.setOnClickListener(v -> finish());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new NotificationAdapter(createNotifications(), item ->
                Toast.makeText(this, item.getTitle(), Toast.LENGTH_SHORT).show()));

        NotificationHelper.createNotificationChannel(this);
        requestNotificationPermissionIfNeeded();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            showTestNotificationOnce();
        }
    }

    private void showTestNotificationOnce() {
        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0) {
            return;
        }
        boolean sent = getPreferences(MODE_PRIVATE).getBoolean("trip_reminder_test_sent", false);
        if (!sent) {
            NotificationHelper.showTripReminderNotification(this);
            getPreferences(MODE_PRIVATE).edit().putBoolean("trip_reminder_test_sent", true).apply();
        }
    }

    static List<NotificationItem> createNotifications() {
        List<NotificationItem> items = new ArrayList<>();
        items.add(new NotificationItem(
                "Aujourd'hui",
                "Prochain voyage",
                "Votre voyage à Marrakech commence dans 2 jours.",
                "09:30",
                R.drawable.ic_notification_trip,
                true
        ));
        items.add(new NotificationItem(
                null,
                "Rappel",
                "N'oubliez pas de préparer vos documents avant le départ.",
                "08:15",
                R.drawable.ic_notification_bell,
                true
        ));
        items.add(new NotificationItem(
                null,
                "Nouvelle étape",
                "Ajoutez vos étapes prévues pour mieux organiser votre voyage.",
                "Hier",
                R.drawable.ic_notification_location,
                true
        ));
        items.add(new NotificationItem(
                null,
                "Synchronisation terminée",
                "Vos données ont été synchronisées avec succès.",
                "Hier",
                R.drawable.ic_notification_sync,
                true
        ));
        items.add(new NotificationItem(
                "27 mai 2024",
                "Carte d'embarquement disponible",
                "Votre carte d'embarquement pour Paris est maintenant disponible.",
                "27 mai",
                R.drawable.ic_notification_ticket,
                true
        ));
        items.add(new NotificationItem(
                "25 mai 2024",
                "Mise à jour de l'itinéraire",
                "Le programme de votre voyage à Lisbonne a été mis à jour.",
                "25 mai",
                R.drawable.ic_notification_info,
                true
        ));
        return items;
    }
}
