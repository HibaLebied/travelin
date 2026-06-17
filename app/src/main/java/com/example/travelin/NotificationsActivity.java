package com.example.travelin;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
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

import java.util.List;

public class NotificationsActivity extends AppCompatActivity {
    private NotificationDao notificationDao;
    private NotificationAdapter adapter;
    private List<NotificationItem> notifications;
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            });


    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        notificationDao = new NotificationDao(this);
        ImageButton backButton = findViewById(R.id.btn_notifications_back);
        RecyclerView recyclerView = findViewById(R.id.recycler_notifications);

        backButton.setOnClickListener(v -> finish());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        notifications = createNotifications(this);
        adapter = new NotificationAdapter(notifications, item -> handleNotificationClick(this, item, adapter));
        recyclerView.setAdapter(adapter);

        NotificationHelper.createNotificationChannel(this);
        requestNotificationPermissionIfNeeded();
        handleExternalNotificationIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleExternalNotificationIntent(intent);
    }

    private void handleExternalNotificationIntent(Intent intent) {
        if (intent == null || !intent.hasExtra(NotificationHelper.EXTRA_NOTIFICATION_ID)) {
            return;
        }
        long notificationId = intent.getLongExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, 0);
        notificationDao.markAsRead(notificationId);
        reloadNotifications();
    }

    private void reloadNotifications() {
        notifications.clear();
        notifications.addAll(createNotifications(this));
        adapter.notifyDataSetChanged();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    static List<NotificationItem> createNotifications(Context context) {
        return new NotificationDao(context).getNotifications();
    }

    static void handleNotificationClick(Context context, NotificationItem item, NotificationAdapter adapter) {
        new NotificationDao(context).markAsRead(item.getId());
        item.setUnread(false);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        String type = item.getType();
        if (NotificationHelper.TYPE_DEPARTURE_TOMORROW.equals(type)
                || NotificationHelper.TYPE_TRIP_TODAY.equals(type)
                || NotificationHelper.TYPE_TRIP_FINISHED.equals(type)) {
            if (item.getRelatedId() > 0) {
                openTripDetail(context, item.getRelatedId());
            } else {
                Toast.makeText(context, item.getDescription(), Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (NotificationHelper.TYPE_STEP_TODAY.equals(type)
                || NotificationHelper.TYPE_ADD_STEP_PHOTOS.equals(type)) {
            if (item.getRelatedId() > 0) {
                openStepEditor(context, item.getRelatedId());
            } else {
                Toast.makeText(context, item.getDescription(), Toast.LENGTH_SHORT).show();
            }
            return;
        }

        Toast.makeText(context, item.getDescription(), Toast.LENGTH_SHORT).show();
    }

    private static void openTripDetail(Context context, long tripId) {
        Trip trip = new TripDao(context).getTripById(tripId);
        Intent intent = new Intent(context, TripDetailActivity.class);
        intent.putExtra(TripDetailActivity.EXTRA_TRIP_ID, tripId);
        if (trip != null) {
            intent.putExtra(TripDetailActivity.EXTRA_TRIP_NAME, trip.getName());
            intent.putExtra(TripDetailActivity.EXTRA_TRIP_DATES, trip.getDates());
            intent.putExtra(TripDetailActivity.EXTRA_TRIP_IMAGE, trip.getImageResId());
            intent.putExtra(TripDetailActivity.EXTRA_HOTEL_PHONE, trip.getHotelPhone());
        }
        context.startActivity(intent);
    }

    private static void openStepEditor(Context context, long stepId) {
        TripStep step = new TripDao(context).getStepById(stepId);
        if (step == null) {
            Toast.makeText(context, context.getString(R.string.step_not_found), Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(context, AddStepActivity.class);
        intent.putExtra(AddStepActivity.EXTRA_TRIP_ID, step.getTripId());
        intent.putExtra(AddStepActivity.EXTRA_STEP_ID, step.getId());
        intent.putExtra(AddStepActivity.EXTRA_STEP_NAME, step.getLocationName());
        intent.putExtra(AddStepActivity.EXTRA_STEP_DESCRIPTION, step.getDescription());
        intent.putExtra(AddStepActivity.EXTRA_STEP_DATE, step.getDate());
        intent.putExtra(AddStepActivity.EXTRA_STEP_TIME, step.getTime());
        if (step.hasCoordinates()) {
            intent.putExtra(AddStepActivity.EXTRA_STEP_LATITUDE, step.getLatitude());
            intent.putExtra(AddStepActivity.EXTRA_STEP_LONGITUDE, step.getLongitude());
        }
        context.startActivity(intent);
    }
}
