package com.example.travelin;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public final class NotificationHelper {
    public static final String CHANNEL_ID = "travelin_notifications";
    public static final String CHANNEL_NAME = "Travelin Notifications";
    public static final String EXTRA_NOTIFICATION_ID = "notification_id";
    public static final String EXTRA_TYPE = "type";
    public static final String EXTRA_RELATED_ID = "related_id";
    public static final String TYPE_DEPARTURE_TOMORROW = "departure_tomorrow";
    public static final String TYPE_TRIP_TODAY = "trip_today";
    public static final String TYPE_STEP_TODAY = "step_today";
    public static final String TYPE_ADD_STEP_PHOTOS = "add_step_photos";
    public static final String TYPE_TRIP_FINISHED = "trip_finished";

    private NotificationHelper() {
    }

    public static long showNotification(Context context, String title, String message, String type, long relatedId) {
        if (TYPE_DEPARTURE_TOMORROW.equals(type) && !areDepartureRemindersEnabled(context)) {
            return -1;
        }

        long notificationId = new NotificationDao(context).insertNotification(title, message, type, relatedId);
        if (!areNotificationsEnabled(context)) {
            return notificationId;
        }

        createNotificationChannel(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return notificationId;
        }

        Intent intent = new Intent(context, NotificationsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);
        intent.putExtra(EXTRA_TYPE, type);
        intent.putExtra(EXTRA_RELATED_ID, relatedId);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(largeIcon)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setColor(ContextCompat.getColor(context, R.color.primary))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat.from(context).notify((int) notificationId, builder.build());
        return notificationId;
    }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                NotificationChannel existingChannel = manager.getNotificationChannel(CHANNEL_ID);
                if (existingChannel != null && existingChannel.getImportance() < NotificationManager.IMPORTANCE_HIGH) {
                    manager.deleteNotificationChannel(CHANNEL_ID);
                }
            }
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(context.getString(R.string.notification_channel_description));
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static boolean areNotificationsEnabled(Context context) {
        ProfilePreferences preferences = new ProfilePreferences(context);
        return preferences.getBoolean("notifications_enabled", preferences.getBoolean("notifications", true));
    }

    public static boolean areDepartureRemindersEnabled(Context context) {
        ProfilePreferences preferences = new ProfilePreferences(context);
        return preferences.getBoolean("departure_reminders_enabled", preferences.getBoolean("departure_reminders", true));
    }

    public static void cancelAllNotifications(Context context) {
        NotificationManagerCompat.from(context).cancelAll();
    }

}
