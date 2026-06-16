package com.example.travelin;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class TripReminderScheduler {
    private static final SimpleDateFormat TRIP_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat STEP_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE);

    private TripReminderScheduler() {
    }

    public static void scheduleTripNotifications(Context context, long tripId, String startDate, String endDate) {
        if (tripId <= 0 || TextUtils.isEmpty(startDate)) {
            return;
        }
        scheduleTripNotification(context, tripId, startDate, -1, 9, NotificationHelper.TYPE_DEPARTURE_TOMORROW);
        scheduleTripNotification(context, tripId, startDate, 0, 8, NotificationHelper.TYPE_TRIP_TODAY);
        if (!TextUtils.isEmpty(endDate)) {
            scheduleTripNotification(context, tripId, endDate, 0, 18, NotificationHelper.TYPE_TRIP_FINISHED);
        }
    }

    public static void scheduleStepNotifications(Context context, long stepId, String stepDate) {
        if (stepId <= 0 || TextUtils.isEmpty(stepDate)) {
            return;
        }
        scheduleStepNotification(context, stepId, stepDate, 8, NotificationHelper.TYPE_STEP_TODAY);
        scheduleStepNotification(context, stepId, stepDate, 19, NotificationHelper.TYPE_ADD_STEP_PHOTOS);
    }

    private static void scheduleTripNotification(Context context, long tripId, String dateValue, int dayOffset,
                                                 int hour, String type) {
        Date date = parseTripDate(dateValue);
        if (date == null) {
            return;
        }
        Calendar trigger = Calendar.getInstance();
        trigger.setTime(date);
        trigger.add(Calendar.DAY_OF_YEAR, dayOffset);
        schedule(context, type, tripId, trigger, hour, (int) (tripId * 100 + Math.abs(dayOffset * 10) + hour));
    }

    private static void scheduleStepNotification(Context context, long stepId, String dateValue, int hour, String type) {
        Date date = parseStepDate(dateValue);
        if (date == null) {
            return;
        }
        Calendar trigger = Calendar.getInstance();
        trigger.setTime(date);
        schedule(context, type, stepId, trigger, hour, (int) (stepId * 100 + hour));
    }

    private static void schedule(Context context, String type, long relatedId, Calendar trigger, int hour, int requestCode) {
        trigger.set(Calendar.HOUR_OF_DAY, hour);
        trigger.set(Calendar.MINUTE, 0);
        trigger.set(Calendar.SECOND, 0);
        trigger.set(Calendar.MILLISECOND, 0);

        long triggerTime = trigger.getTimeInMillis();
        if (triggerTime < System.currentTimeMillis()) {
            if (isToday(trigger)) {
                fireNow(context, type, relatedId);
            }
            return;
        }

        Intent intent = new Intent(context, TravelNotificationReceiver.class);
        intent.putExtra(NotificationHelper.EXTRA_TYPE, type);
        intent.putExtra(NotificationHelper.EXTRA_RELATED_ID, relatedId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }

    private static void fireNow(Context context, String type, long relatedId) {
        Intent intent = new Intent(context, TravelNotificationReceiver.class);
        intent.putExtra(NotificationHelper.EXTRA_TYPE, type);
        intent.putExtra(NotificationHelper.EXTRA_RELATED_ID, relatedId);
        new TravelNotificationReceiver().onReceive(context, intent);
    }

    private static boolean isToday(Calendar calendar) {
        Calendar today = Calendar.getInstance();
        return today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)
                && today.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR);
    }

    private static Date parseTripDate(String value) {
        try {
            return TRIP_DATE_FORMAT.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    private static Date parseStepDate(String value) {
        try {
            return STEP_DATE_FORMAT.parse(value);
        } catch (Exception e) {
            return null;
        }
    }
}
