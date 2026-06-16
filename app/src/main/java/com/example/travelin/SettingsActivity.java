package com.example.travelin;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsActivity extends AppCompatActivity {
    private static final int NAV_ACTIVE_COLOR = Color.rgb(0, 158, 158);
    private static final int NAV_INACTIVE_COLOR = Color.rgb(111, 119, 136);
    private ProfilePreferences profilePreferences;
    private LinearLayout navigationBar;
    private ImageView[] navigationIcons;
    private TextView[] navigationLabels;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        profilePreferences = new ProfilePreferences(this);

        findViewById(R.id.btn_settings_back).setOnClickListener(v -> finish());
        setupCustomBottomNavigation();
        setupRows();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindProfile();
        bindStats();
    }

    private void bindProfile() {
        ((TextView) findViewById(R.id.txt_profile_name)).setText(profilePreferences.getFullName());
        ((TextView) findViewById(R.id.txt_profile_email)).setText(profilePreferences.getEmail());
        ((TextView) findViewById(R.id.txt_profile_city)).setText(profilePreferences.getCity());

        String photoUri = profilePreferences.getPhotoUri();
        ImageView avatar = findViewById(R.id.img_profile_avatar);
        if (!TextUtils.isEmpty(photoUri)) {
            avatar.setImageURI(Uri.parse(photoUri));
        } else {
            avatar.setImageResource(R.drawable.ic_settings_profile);
        }
    }

    private void bindStats() {
        ProfileStatsDao.ProfileStats stats = new ProfileStatsDao(this).getStats(getConnectedUserId());
        bindStat(findViewById(R.id.stat_trips), R.drawable.ic_settings_trip, String.valueOf(stats.trips), "Voyages");
        bindStat(findViewById(R.id.stat_steps), R.drawable.ic_settings_location, String.valueOf(stats.steps), "Étapes");
        bindStat(findViewById(R.id.stat_photos), R.drawable.ic_settings_photo, String.valueOf(stats.photos), "Photos");
        bindStat(findViewById(R.id.stat_days), R.drawable.ic_settings_calendar, String.valueOf(stats.days), "Jours de voyage");
    }

    private void setupRows() {
        bindRow(findViewById(R.id.row_profile), R.drawable.ic_settings_profile, "Informations personnelles", "Gérer vos informations");
        bindRow(findViewById(R.id.row_preferences), R.drawable.ic_settings_language, "Préférences", "Langue, devise, notifications");
        bindRow(findViewById(R.id.row_sync), R.drawable.ic_settings_cloud, "Données et synchronisation", "Sauvegarde et restauration");
        bindRow(findViewById(R.id.row_help), R.drawable.ic_settings_help, "Aide et support", "FAQ, contact, politiques");
        bindRow(findViewById(R.id.row_privacy), R.drawable.ic_settings_shield, "Confidentialité et sécurité", "Gérer vos données personnelles");
        bindRow(findViewById(R.id.row_logout), R.drawable.ic_settings_logout, "Déconnexion", "Se déconnecter de votre compte");
        findViewById(R.id.row_profile).setOnClickListener(v -> startActivity(new Intent(this, PersonalInfoActivity.class)));
        findViewById(R.id.btn_edit_photo).setOnClickListener(v -> startActivity(new Intent(this, PersonalInfoActivity.class)));
        findViewById(R.id.row_preferences).setOnClickListener(v -> startActivity(new Intent(this, PreferencesActivity.class)));
        findViewById(R.id.row_sync).setOnClickListener(v -> startActivity(new Intent(this, SyncActivity.class)));
        findViewById(R.id.row_help).setOnClickListener(v -> startActivity(new Intent(this, HelpSupportActivity.class)));
        findViewById(R.id.row_privacy).setOnClickListener(v -> startActivity(new Intent(this, PrivacySecurityActivity.class)));
        findViewById(R.id.row_logout).setOnClickListener(v -> showLogoutSheet());
    }

    private void bindStat(View root, int iconRes, String value, String label) {
        ((ImageView) root.findViewById(R.id.img_stat_icon)).setImageResource(iconRes);
        ((TextView) root.findViewById(R.id.txt_stats_trips)).setText(value);
        ((TextView) root.findViewById(R.id.txt_stat_label)).setText(label);
    }

    private void bindRow(View root, int iconRes, String title, String subtitle) {
        ((ImageView) root.findViewById(R.id.img_row_icon)).setImageResource(iconRes);
        ((TextView) root.findViewById(R.id.txt_row_title)).setText(title);
        ((TextView) root.findViewById(R.id.txt_row_subtitle)).setText(subtitle);
    }

    private void setupCustomBottomNavigation() {
        ViewGroup content = findViewById(android.R.id.content);
        FrameLayout root = (FrameLayout) content.getChildAt(0);
        navigationBar = new LinearLayout(this);
        navigationBar.setOrientation(LinearLayout.HORIZONTAL);
        navigationBar.setGravity(Gravity.CENTER);
        navigationBar.setPadding(dp(8), dp(5), dp(8), dp(5));
        navigationBar.setElevation(0);
        navigationBar.setTranslationZ(0);
        navigationBar.setStateListAnimator(null);
        navigationBar.setOutlineProvider(null);

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(22));
        navigationBar.setBackground(background);

        FrameLayout.LayoutParams barParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(76),
                Gravity.BOTTOM
        );
        barParams.setMargins(dp(8), 0, dp(8), dp(7));
        root.addView(navigationBar, barParams);

        String[] labels = {"Accueil", "Memories", "Explorer", "Notifications", "Profil"};
        int[] icons = {
                R.drawable.nav_home,
                R.drawable.nav_memories,
                R.drawable.nav_explorer,
                R.drawable.nav_notification,
                R.drawable.nav_profile
        };

        navigationIcons = new ImageView[labels.length];
        navigationLabels = new TextView[labels.length];
        for (int index = 0; index < labels.length; index++) {
            final int selectedIndex = index;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setClickable(true);
            item.setFocusable(true);
            item.setOnClickListener(view -> onNavigationItemClicked(selectedIndex));

            ImageView icon = new ImageView(this);
            icon.setImageResource(icons[index]);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            item.addView(icon, new LinearLayout.LayoutParams(dp(27), dp(27)));

            TextView label = new TextView(this);
            label.setText(labels[index]);
            label.setTextSize(index == 3 ? 9 : 10);
            label.setGravity(Gravity.CENTER);
            label.setSingleLine(true);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            labelParams.topMargin = dp(3);
            item.addView(label, labelParams);

            navigationIcons[index] = icon;
            navigationLabels[index] = label;
            navigationBar.addView(item, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        }
        selectNavigationItem(4);
        navigationBar.bringToFront();
    }

    private void onNavigationItemClicked(int index) {
        if (index == 4) {
            selectNavigationItem(4);
            return;
        }
        Intent intent = new Intent(this, HomeActivity.class);
        intent.putExtra(HomeActivity.EXTRA_OPEN_NAV_INDEX, index);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void selectNavigationItem(int selectedIndex) {
        for (int index = 0; index < navigationIcons.length; index++) {
            int color = index == selectedIndex ? NAV_ACTIVE_COLOR : NAV_INACTIVE_COLOR;
            navigationIcons[index].setImageTintList(ColorStateList.valueOf(color));
            navigationLabels[index].setTextColor(color);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void showLogoutSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_logout, null);
        dialog.setContentView(view);
        view.findViewById(R.id.btn_logout_cancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btn_logout_confirm).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            profilePreferences.setLoggedOut();
            Toast.makeText(this, "Déconnexion réussie", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, SignInActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            dialog.dismiss();
        });
        dialog.show();
    }

    private String getConnectedUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user == null ? "guest" : user.getUid();
    }
}
