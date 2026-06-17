package com.example.travelin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class TravelNotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }

        String type = intent.getStringExtra(NotificationHelper.EXTRA_TYPE);
        long relatedId = intent.getLongExtra(NotificationHelper.EXTRA_RELATED_ID, 0);

        if (NotificationHelper.TYPE_DEPARTURE_TOMORROW.equals(type)) {
            NotificationHelper.showNotification(
                    context,
                    context.getString(R.string.notification_departure_title),
                    context.getString(R.string.notification_departure_message),
                    NotificationHelper.TYPE_DEPARTURE_TOMORROW,
                    relatedId
            );
        } else if (NotificationHelper.TYPE_TRIP_TODAY.equals(type)) {
            NotificationHelper.showNotification(
                    context,
                    context.getString(R.string.notification_trip_today_title),
                    context.getString(R.string.notification_trip_today_message),
                    NotificationHelper.TYPE_TRIP_TODAY,
                    relatedId
            );
        } else if (NotificationHelper.TYPE_STEP_TODAY.equals(type)) {
            NotificationHelper.showNotification(
                    context,
                    context.getString(R.string.notification_step_today_title),
                    context.getString(R.string.notification_step_today_message),
                    NotificationHelper.TYPE_STEP_TODAY,
                    relatedId
            );
        } else if (NotificationHelper.TYPE_ADD_STEP_PHOTOS.equals(type)) {
            NotificationHelper.showNotification(
                    context,
                    context.getString(R.string.notification_add_photos_title),
                    context.getString(R.string.notification_add_photos_message),
                    NotificationHelper.TYPE_ADD_STEP_PHOTOS,
                    relatedId
            );
        } else if (NotificationHelper.TYPE_TRIP_FINISHED.equals(type)) {
            NotificationHelper.showNotification(
                    context,
                    context.getString(R.string.notification_finished_title),
                    context.getString(R.string.notification_finished_message),
                    NotificationHelper.TYPE_TRIP_FINISHED,
                    relatedId
            );
        }
    }
}
