package com.example.travelin;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;

public class GetStartedActivity extends Activity {
    private static final String PREFS_NAME = "travelin_prefs";
    private static final String KEY_LANGUAGE = "language";
    private static final String LANG_EN = "en";
    private static final String LANG_FR = "fr";

    private TextView languageText;
    private TextView titleText;
    private Button startButton;


    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_started);

        languageText = findViewById(R.id.txt_language);
        titleText = findViewById(R.id.txt_get_started_title);
        startButton = findViewById(R.id.btn_start_journey);

        applyLanguage(getSavedLanguage());

        languageText.setOnClickListener(v -> showLanguageMenu());
        startButton.setOnClickListener(v ->
                startActivity(new Intent(GetStartedActivity.this, SignInActivity.class)));
    }

    private void showLanguageMenu() {
        PopupMenu menu = new PopupMenu(this, languageText);
        menu.getMenu().add(getString(R.string.english));
        menu.getMenu().add(getString(R.string.french));
        menu.setOnMenuItemClickListener(item -> {
            String selected = item.getTitle().toString();
            if (getString(R.string.french).equals(selected)) {
                saveLanguage(LANG_FR);
            } else {
                saveLanguage(LANG_EN);
            }
            recreate();
            return true;
        });
        menu.show();
    }

    private void applyLanguage(String language) {
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        if (LANG_FR.equals(language)) {
            languageText.setText(getString(R.string.language_french_short));
            titleText.setText(getString(R.string.get_started_title));
            startButton.setText(getString(R.string.journey_starts_here));
        } else {
            languageText.setText(getString(R.string.language_english_short));
            titleText.setText("Ready to explore\nbeyond borders?");
            startButton.setText("Your journey starts here");
        }
    }

    private String getSavedLanguage() {
        return LocaleHelper.getLanguage(this);
    }

    private void saveLanguage(String language) {
        LocaleHelper.setLanguage(this, language);
    }
}
