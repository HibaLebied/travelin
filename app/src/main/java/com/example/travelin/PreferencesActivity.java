package com.example.travelin;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class PreferencesActivity extends AppCompatActivity {
    private ProfilePreferences preferences;
    private TextView languageValue;
    private TextView currencyValue;
    private SwitchMaterial notificationsSwitch;
    private SwitchMaterial remindersSwitch;


    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);
        preferences = new ProfilePreferences(this);
        languageValue = findViewById(R.id.txt_language_value);
        currencyValue = findViewById(R.id.txt_currency_value);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.row_language).setOnClickListener(v -> chooseLanguage());
        findViewById(R.id.row_currency).setOnClickListener(v ->
                choose(ExploreRepository.KEY_CURRENCY, getString(R.string.currency), new String[]{"MAD (DH)", "EUR (€)", "USD ($)"}, currencyValue));

        notificationsSwitch = bindSwitch(
                findViewById(R.id.row_notifications),
                R.drawable.ic_settings_notifications,
                "Notifications",
                "Recevoir des notifications sur vos voyages",
                "notifications",
                true
        );
        remindersSwitch = bindSwitch(
                findViewById(R.id.row_reminders),
                R.drawable.ic_settings_calendar,
                "Rappels avant départ",
                "Recevoir des rappels avant vos voyages",
                "departure_reminders",
                true
        );
        bindSwitch(
                findViewById(R.id.row_offline),
                R.drawable.ic_settings_offline,
                "Mode hors ligne",
                "Accéder à vos données sans connexion",
                ExploreRepository.KEY_OFFLINE_MODE,
                false
        );

        updateReminderAvailability(notificationsSwitch.isChecked());
        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.putBoolean("notifications", isChecked);
            updateReminderAvailability(isChecked);
            if (!isChecked) {
                NotificationHelper.cancelAllNotifications(this);
                Toast.makeText(this, "Notifications désactivées dans toute l'application", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_save_preferences).setOnClickListener(v -> {
            Toast.makeText(this, getString(R.string.preferences_saved), Toast.LENGTH_SHORT).show();
            finish();
        });
        bindValues();
    }

    private void bindValues() {
        languageValue.setText(preferences.getLanguage());
        currencyValue.setText(preferences.getCurrency());
    }

    private void choose(String key, String title, String[] items, TextView target) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(items, (dialog, which) -> {
                    preferences.putString(key, items[which]);
                    target.setText(items[which]);
                })
                .show();
    }

    private void chooseLanguage() {
        String[] items = {getString(R.string.french), getString(R.string.english)};
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.language))
                .setItems(items, (dialog, which) -> {
                    preferences.putString(
                            ExploreRepository.KEY_LANGUAGE,
                            which == 1 ? LocaleHelper.LANG_EN : LocaleHelper.LANG_FR
                    );
                    recreate();
                })
                .show();
    }

    private SwitchMaterial bindSwitch(View root, int iconRes, String title, String subtitle, String key, boolean defaultValue) {
        ((ImageView) root.findViewById(R.id.img_switch_icon)).setImageResource(iconRes);
        ((TextView) root.findViewById(R.id.txt_switch_title)).setText(title);
        ((TextView) root.findViewById(R.id.txt_switch_subtitle)).setText(subtitle);
        SwitchMaterial switchView = root.findViewById(R.id.switch_row_switch);
        switchView.setChecked(preferences.getBoolean(key, defaultValue));
        switchView.setOnCheckedChangeListener((buttonView, isChecked) -> preferences.putBoolean(key, isChecked));
        return switchView;
    }

    private void updateReminderAvailability(boolean notificationsEnabled) {
        remindersSwitch.setEnabled(notificationsEnabled);
        remindersSwitch.setAlpha(notificationsEnabled ? 1f : 0.45f);
    }
}
