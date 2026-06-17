package com.example.travelin;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfilePreferences {
    private static final String PREFS_NAME = "travelin_profile";
    private final Context context;
    private final SharedPreferences preferences;
    private final SharedPreferences appPreferences;

    public ProfilePreferences(Context context) {
        this.context = context;
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        appPreferences = context.getSharedPreferences(ExploreRepository.PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getFullName() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && !TextUtils.isEmpty(user.getDisplayName())) {
            return user.getDisplayName();
        }
        String saved = preferences.getString("full_name", "");
        if (!TextUtils.isEmpty(saved)) {
            return saved;
        }
        String email = getEmail();
        if (!TextUtils.isEmpty(email) && email.contains("@")) {
            return email.substring(0, email.indexOf('@'));
        }
        return "";
    }

    public String getEmail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && !TextUtils.isEmpty(user.getEmail())) {
            return user.getEmail();
        }
        String saved = preferences.getString("email", "");
        if (!TextUtils.isEmpty(saved)) {
            return saved;
        }
        return "sarah.benali@email.com";
    }

    public String getPhone() {
        String phone = preferences.getString("phone", "");
        if ("+212 6 12 34 56 78".equals(phone)) {
            return "";
        }
        return phone;
    }

    public String getCity() {
        return preferences.getString("city", "Casablanca, Maroc");
    }

    public String getBirthDate() {
        String birthDate = preferences.getString("birth_date", "");
        if ("12 mars 1999".equals(birthDate)) {
            return "";
        }
        return birthDate;
    }

    public String getPhotoUri() {
        return preferences.getString("photo_uri", "");
    }

    public void saveProfile(String fullName, String email, String phone, String city, String birthDate, String photoUri) {
        preferences.edit()
                .putString("full_name", fullName)
                .putString("email", email)
                .putString("phone", phone)
                .putString("city", city)
                .putString("birth_date", birthDate)
                .putString("photo_uri", photoUri)
                .apply();
    }

    public String getLanguage() {
        String language = appPreferences.getString(
                ExploreRepository.KEY_LANGUAGE,
                preferences.getString(ExploreRepository.KEY_LANGUAGE, LocaleHelper.LANG_FR)
        );
        return LocaleHelper.LANG_EN.equals(language)
                ? context.getString(R.string.english)
                : context.getString(R.string.french);
    }

    public String getCurrency() {
        return appPreferences.getString(
                ExploreRepository.KEY_CURRENCY,
                preferences.getString(ExploreRepository.KEY_CURRENCY, "MAD (DH)")
        );
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        if (ExploreRepository.KEY_OFFLINE_MODE.equals(key)) {
            return appPreferences.getBoolean(key, preferences.getBoolean(key, defaultValue));
        }
        return preferences.getBoolean(key, defaultValue);
    }

    public String getString(String key, String defaultValue) {
        if (ExploreRepository.KEY_LANGUAGE.equals(key)
                || ExploreRepository.KEY_CURRENCY.equals(key)
                || ExploreRepository.KEY_PREFERRED_THEME.equals(key)) {
            return appPreferences.getString(key, preferences.getString(key, defaultValue));
        }
        return preferences.getString(key, defaultValue);
    }

    public void putString(String key, String value) {
        if (ExploreRepository.KEY_LANGUAGE.equals(key)) {
            value = (LocaleHelper.LANG_EN.equals(value) || "English".equals(value))
                    ? LocaleHelper.LANG_EN
                    : LocaleHelper.LANG_FR;
            LocaleHelper.setLanguage(context, value);
        }
        preferences.edit().putString(key, value).apply();
        if (ExploreRepository.KEY_LANGUAGE.equals(key)
                || ExploreRepository.KEY_CURRENCY.equals(key)
                || ExploreRepository.KEY_PREFERRED_THEME.equals(key)) {
            appPreferences.edit().putString(key, value).apply();
        }
    }

    public void putBoolean(String key, boolean value) {
        preferences.edit().putBoolean(key, value).apply();
        if (ExploreRepository.KEY_OFFLINE_MODE.equals(key)) {
            appPreferences.edit().putBoolean(key, value).apply();
        }
    }

    public void setLoggedOut() {
        preferences.edit().putBoolean("isLoggedIn", false).apply();
        context.getSharedPreferences("travelin_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("isLoggedIn", false)
                .apply();
    }
}
