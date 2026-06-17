package com.example.travelin;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;

import java.util.Locale;

public final class LocaleHelper {
    public static final String PREFS_NAME = "travelin_prefs";
    public static final String KEY_LANGUAGE = "language";
    public static final String LANG_EN = "en";
    public static final String LANG_FR = "fr";

    private LocaleHelper() {
    }

    public static Context wrap(Context context) {
        String language = getLanguage(context);
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale);
            configuration.setLayoutDirection(locale);
            return context.createConfigurationContext(configuration);
        }

        configuration.locale = locale;
        configuration.setLayoutDirection(locale);
        context.getResources().updateConfiguration(configuration, context.getResources().getDisplayMetrics());
        return context;
    }

    public static String getLanguage(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return preferences.getString(KEY_LANGUAGE, LANG_FR);
    }

    public static void setLanguage(Context context, String language) {
        String normalized = LANG_EN.equals(language) ? LANG_EN : LANG_FR;
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LANGUAGE, normalized)
                .apply();
    }
}
