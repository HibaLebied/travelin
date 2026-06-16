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
                    "Voyage bientôt",
                    "Votre voyage commence demain. Préparez vos affaires.",
                    NotificationHelper.TYPE_DEPARTURE_TOMORROW,
                    relatedId
            );
        } else if (NotificationHelper.TYPE_TRIP_TODAY.equals(type)) {
            NotificationHelper.showNotification(
                    context,
                    "Bon voyage !",
                    "Votre voyage commence aujourd’hui. Consultez vos étapes.",
                    NotificationHelper.TYPE_TRIP_TODAY,
                    relatedId
            );
        } else if (NotificationHelper.TYPE_STEP_TODAY.equals(type)) {
            NotificationHelper.showNotification(
                    context,
                    "Étape prévue",
                    "Vous avez une étape prévue aujourd’hui.",
                    NotificationHelper.TYPE_STEP_TODAY,
                    relatedId
            );
        } else if (NotificationHelper.TYPE_ADD_STEP_PHOTOS.equals(type)) {
            NotificationHelper.showNotification(
                    context,
                    "Ajoutez vos souvenirs",
                    "N’oubliez pas d’ajouter les photos de cette étape.",
                    NotificationHelper.TYPE_ADD_STEP_PHOTOS,
                    relatedId
            );
        } else if (NotificationHelper.TYPE_TRIP_FINISHED.equals(type)) {
            NotificationHelper.showNotification(
                    context,
                    "Voyage terminé",
                    "Votre voyage est terminé. Préparez votre récapitulatif.",
                    NotificationHelper.TYPE_TRIP_FINISHED,
                    relatedId
            );
        }
    }
}
