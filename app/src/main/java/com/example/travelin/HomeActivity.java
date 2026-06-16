package com.example.travelin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class HomeActivity extends AppCompatActivity {
    public static final String EXTRA_OPEN_NAV_INDEX = "open_nav_index";
    private static final int NAV_ACTIVE_COLOR = Color.rgb(0, 158, 158);
    private static final int NAV_INACTIVE_COLOR = Color.rgb(111, 119, 136);

    private TripAdapter tripAdapter;
    private TripDao tripDao;
    private final List<Trip> allTrips = new ArrayList<>();
    private EditText searchInput;
    private TextView noTripsText;
    private FrameLayout rootContainer;
    private LinearLayout navigationBar;
    private FloatingActionButton addTripButton;
    private View notificationsContent;
    private View memoriesContent;
    private View activeMemoryShade;
    private View activeMemoryInfo;
    private Runnable pendingMemoryHide;
    private View explorerContent;
    private LinearLayout explorerBody;
    private ScrollView explorerScrollView;
    private ProgressBar explorerProgressBar;
    private LinearLayout explorerErrorView;
    private TextView explorerErrorText;
    private ExploreRepository exploreRepository;
    private ProfilePreferences profilePreferences;
    private View profileContent;
    private final List<ExploreDestination> explorerDestinations = new ArrayList<>();
    private LinearLayout[] navigationItems;
    private ImageView[] navigationIcons;
    private TextView[] navigationLabels;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        updateUserHeader();
        tripDao = new TripDao(this);
        searchInput = findViewById(R.id.input_search_trip);
        noTripsText = findViewById(R.id.txt_no_trips);
        exploreRepository = new ExploreRepository(this);
        profilePreferences = new ProfilePreferences(this);

        RecyclerView tripsRecyclerView = findViewById(R.id.recycler_trips);
        tripsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tripsRecyclerView.setNestedScrollingEnabled(true);
        tripAdapter = new TripAdapter(new ArrayList<>(), trip -> {
            Intent intent = new Intent(this, TripDetailActivity.class);
            intent.putExtra(TripDetailActivity.EXTRA_TRIP_ID, trip.getId());
            intent.putExtra(TripDetailActivity.EXTRA_TRIP_NAME, trip.getName());
            intent.putExtra(TripDetailActivity.EXTRA_TRIP_DATES, trip.getDates());
            intent.putExtra(TripDetailActivity.EXTRA_TRIP_IMAGE, trip.getImageResId());
            intent.putExtra(TripDetailActivity.EXTRA_HOTEL_PHONE, trip.getHotelPhone());
            startActivity(intent);
        });
        tripsRecyclerView.setAdapter(tripAdapter);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterTrips(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        replaceBottomNavigation();
        openRequestedNavigationItem(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        openRequestedNavigationItem(intent);
    }

    private void openRequestedNavigationItem(Intent intent) {
        if (intent == null || navigationIcons == null) {
            return;
        }
        int requestedIndex = intent.getIntExtra(EXTRA_OPEN_NAV_INDEX, 0);
        if (requestedIndex > 0 && requestedIndex < 4) {
            onNavigationItemClicked(requestedIndex);
        }
        intent.removeExtra(EXTRA_OPEN_NAV_INDEX);
    }

    private void replaceBottomNavigation() {
        BottomNavigationView oldNavigation = findViewById(R.id.bottom_navigation);
        FloatingActionButton oldAddButton = findViewById(R.id.fab_add_trip);
        addTripButton = oldAddButton;
        oldNavigation.setVisibility(View.GONE);
        oldAddButton.setVisibility(View.VISIBLE);
        FrameLayout.LayoutParams addButtonParams =
                (FrameLayout.LayoutParams) oldAddButton.getLayoutParams();
        addButtonParams.bottomMargin = dp(102);
        oldAddButton.setLayoutParams(addButtonParams);
        oldAddButton.setOnClickListener(view ->
                new TripTypeBottomSheetFragment().show(
                        getSupportFragmentManager(),
                        "TripTypeBottomSheet"
                ));

        ViewGroup content = findViewById(android.R.id.content);
        FrameLayout root = (FrameLayout) content.getChildAt(0);
        rootContainer = root;
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

        navigationItems = new LinearLayout[labels.length];
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

            navigationItems[index] = item;
            navigationIcons[index] = icon;
            navigationLabels[index] = label;
            navigationBar.addView(item, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        }

        selectNavigationItem(0);
    }

    private void onNavigationItemClicked(int index) {
        selectNavigationItem(index);
        if (index == 0) {
            showHomeContent();
            return;
        }
        if (index == 1) {
            showMemoriesContent();
        } else if (index == 2) {
            showExplorerContent();
        } else if (index == 3) {
            showNotificationsContent();
        } else {
            showProfileContent();
        }
    }

    private void showNotificationsContent() {
        hideSecondaryContent();
        if (notificationsContent == null) {
            notificationsContent = LayoutInflater.from(this)
                    .inflate(R.layout.activity_notifications, rootContainer, false);

            ImageButton backButton = notificationsContent.findViewById(R.id.btn_notifications_back);
            RecyclerView recyclerView = notificationsContent.findViewById(R.id.recycler_notifications);
            backButton.setOnClickListener(view -> {
                selectNavigationItem(0);
                showHomeContent();
            });
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(new NotificationAdapter(
                    NotificationsActivity.createNotifications(),
                    item -> Toast.makeText(this, item.getTitle(), Toast.LENGTH_SHORT).show()
            ));

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            params.bottomMargin = dp(83);
            rootContainer.addView(notificationsContent, params);
        } else {
            notificationsContent.setVisibility(View.VISIBLE);
        }

        addTripButton.setVisibility(View.GONE);
        navigationBar.bringToFront();
    }

    private void showProfileContent() {
        hideSecondaryContent();
        if (profileContent == null) {
            profileContent = LayoutInflater.from(this)
                    .inflate(R.layout.activity_settings, rootContainer, false);
            bindProfileContent();
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            params.bottomMargin = dp(83);
            rootContainer.addView(profileContent, params);
        } else {
            profileContent.setVisibility(View.VISIBLE);
            bindProfileData();
            bindProfileStats();
        }

        addTripButton.setVisibility(View.GONE);
        navigationBar.bringToFront();
    }

    private void bindProfileContent() {
        ImageButton backButton = profileContent.findViewById(R.id.btn_settings_back);
        backButton.setOnClickListener(view -> {
            selectNavigationItem(0);
            showHomeContent();
        });

        profileContent.findViewById(R.id.row_profile)
                .setOnClickListener(view -> startActivity(new Intent(this, PersonalInfoActivity.class)));
        profileContent.findViewById(R.id.btn_edit_photo)
                .setOnClickListener(view -> startActivity(new Intent(this, PersonalInfoActivity.class)));
        profileContent.findViewById(R.id.row_preferences)
                .setOnClickListener(view -> startActivity(new Intent(this, PreferencesActivity.class)));
        profileContent.findViewById(R.id.row_sync)
                .setOnClickListener(view -> startActivity(new Intent(this, SyncActivity.class)));
        profileContent.findViewById(R.id.row_help)
                .setOnClickListener(view -> startActivity(new Intent(this, HelpSupportActivity.class)));
        profileContent.findViewById(R.id.row_privacy)
                .setOnClickListener(view -> startActivity(new Intent(this, PrivacySecurityActivity.class)));
        profileContent.findViewById(R.id.row_logout)
                .setOnClickListener(view -> showLogoutSheet());

        bindProfileRow(profileContent.findViewById(R.id.row_profile),
                R.drawable.ic_settings_profile,
                "Informations personnelles",
                "Gérer vos informations");
        bindProfileRow(profileContent.findViewById(R.id.row_preferences),
                R.drawable.ic_settings_language,
                "Préférences",
                "Langue, devise, notifications");
        bindProfileRow(profileContent.findViewById(R.id.row_sync),
                R.drawable.ic_settings_cloud,
                "Données et synchronisation",
                "Sauvegarde et restauration");
        bindProfileRow(profileContent.findViewById(R.id.row_help),
                R.drawable.ic_settings_help,
                "Aide et support",
                "FAQ, contact, politiques");
        bindProfileRow(profileContent.findViewById(R.id.row_privacy),
                R.drawable.ic_settings_shield,
                "Confidentialité et sécurité",
                "Gérer vos données personnelles");
        bindProfileRow(profileContent.findViewById(R.id.row_logout),
                R.drawable.ic_settings_logout,
                "Déconnexion",
                "Se déconnecter de votre compte");

        bindProfileData();
        bindProfileStats();
    }

    private void bindProfileData() {
        ((TextView) profileContent.findViewById(R.id.txt_profile_name))
                .setText(profilePreferences.getFullName());
        ((TextView) profileContent.findViewById(R.id.txt_profile_email))
                .setText(profilePreferences.getEmail());
        ((TextView) profileContent.findViewById(R.id.txt_profile_city))
                .setText(profilePreferences.getCity());

        String photoUri = profilePreferences.getPhotoUri();
        ImageView avatar = profileContent.findViewById(R.id.img_profile_avatar);
        if (!TextUtils.isEmpty(photoUri)) {
            avatar.setImageURI(Uri.parse(photoUri));
        } else {
            avatar.setImageResource(R.drawable.ic_settings_profile);
        }
    }

    private void bindProfileStats() {
        ProfileStatsDao.ProfileStats stats = new ProfileStatsDao(this)
                .getStats(getConnectedUserId());
        bindProfileStat(profileContent.findViewById(R.id.stat_trips),
                R.drawable.ic_settings_trip,
                String.valueOf(stats.trips),
                "Voyages");
        bindProfileStat(profileContent.findViewById(R.id.stat_steps),
                R.drawable.ic_settings_location,
                String.valueOf(stats.steps),
                "Étapes");
        bindProfileStat(profileContent.findViewById(R.id.stat_photos),
                R.drawable.ic_settings_photo,
                String.valueOf(stats.photos),
                "Photos");
        bindProfileStat(profileContent.findViewById(R.id.stat_days),
                R.drawable.ic_settings_calendar,
                String.valueOf(stats.days),
                "Jours de voyage");
    }

    private void bindProfileStat(View root, int iconRes, String value, String label) {
        ((ImageView) root.findViewById(R.id.img_stat_icon)).setImageResource(iconRes);
        ((TextView) root.findViewById(R.id.txt_stats_trips)).setText(value);
        ((TextView) root.findViewById(R.id.txt_stat_label)).setText(label);
    }

    private void bindProfileRow(View root, int iconRes, String title, String subtitle) {
        ((ImageView) root.findViewById(R.id.img_row_icon)).setImageResource(iconRes);
        ((TextView) root.findViewById(R.id.txt_row_title)).setText(title);
        ((TextView) root.findViewById(R.id.txt_row_subtitle)).setText(subtitle);
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

    private void showMemoriesContent() {
        hideSecondaryContent();
        if (memoriesContent != null) {
            rootContainer.removeView(memoriesContent);
        }
        memoriesContent = createMemoriesView();
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        params.bottomMargin = dp(76);
        rootContainer.addView(memoriesContent, params);

        addTripButton.setVisibility(View.GONE);
        navigationBar.bringToFront();
    }

    private View createMemoriesView() {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(Color.WHITE);

        FrameLayout header = new FrameLayout(this);
        header.setPadding(dp(12), 0, dp(12), 0);
        header.setBackgroundColor(Color.WHITE);
        screen.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(68)
        ));

        ImageButton backButton = new ImageButton(this);
        backButton.setImageResource(R.drawable.ic_add_trip_back);
        backButton.setImageTintList(ColorStateList.valueOf(Color.rgb(7, 56, 68)));
        TypedValue ripple = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, ripple, true);
        backButton.setBackgroundResource(ripple.resourceId);
        backButton.setPadding(dp(12), dp(12), dp(12), dp(12));
        backButton.setContentDescription("Retour");
        backButton.setOnClickListener(view -> {
            selectNavigationItem(0);
            showHomeContent();
        });
        FrameLayout.LayoutParams backParams = new FrameLayout.LayoutParams(
                dp(48),
                dp(48),
                Gravity.START | Gravity.CENTER_VERTICAL
        );
        header.addView(backButton, backParams);

        TextView headerTitle = new TextView(this);
        headerTitle.setText("Memories");
        headerTitle.setTextColor(Color.rgb(7, 56, 68));
        headerTitle.setTextSize(23);
        headerTitle.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        headerTitle.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams titleParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
        );
        header.addView(headerTitle, titleParams);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Color.rgb(245, 245, 245));
        scrollView.setClipToPadding(false);
        screen.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(8), dp(18), dp(8), dp(16));
        scrollView.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView subtitle = new TextView(this);
        subtitle.setTextColor(Color.rgb(87, 99, 120));
        subtitle.setTextSize(16);
        subtitle.setPadding(dp(26), dp(6), dp(26), dp(22));
        content.addView(subtitle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        grid.setUseDefaultMargins(false);
        content.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        List<StepMemoryPhoto> memories = tripDao.getStepMemoryPhotos(getConnectedUserId());
        subtitle.setText(memories.size() == 1
                ? "1 photo souvenir"
                : memories.size() + " photos souvenirs");

        if (memories.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("Aucune photo souvenir pour le moment");
            emptyText.setTextColor(Color.rgb(87, 99, 120));
            emptyText.setTextSize(16);
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setPadding(dp(20), dp(50), dp(20), dp(50));
            content.addView(emptyText, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            return screen;
        }

        int initialPhotoCount = Math.min(9, memories.size());
        for (int index = 0; index < initialPhotoCount; index++) {
            grid.addView(createMemoryTile(memories.get(index)));
        }

        TextView loadMoreButton = new TextView(this);
        loadMoreButton.setText("Charger plus de photos");
        loadMoreButton.setTextColor(Color.WHITE);
        loadMoreButton.setTextSize(16);
        loadMoreButton.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        loadMoreButton.setGravity(Gravity.CENTER);
        loadMoreButton.setClickable(true);
        loadMoreButton.setFocusable(true);
        GradientDrawable loadMoreBackground = new GradientDrawable();
        loadMoreBackground.setColor(NAV_ACTIVE_COLOR);
        loadMoreBackground.setCornerRadius(dp(28));
        loadMoreButton.setBackground(loadMoreBackground);
        loadMoreButton.setElevation(dp(3));
        LinearLayout.LayoutParams loadMoreParams = new LinearLayout.LayoutParams(dp(226), dp(52));
        loadMoreParams.gravity = Gravity.CENTER_HORIZONTAL;
        loadMoreParams.setMargins(0, dp(38), 0, dp(30));
        content.addView(loadMoreButton, loadMoreParams);
        loadMoreButton.setVisibility(initialPhotoCount >= memories.size() ? View.GONE : View.VISIBLE);
        loadMoreButton.setOnClickListener(view -> {
            int currentCount = grid.getChildCount();
            int nextCount = Math.min(memories.size(), currentCount + 4);
            for (int index = currentCount; index < nextCount; index++) {
                grid.addView(createMemoryTile(memories.get(index)));
            }
            subtitle.setText(nextCount == 1 ? "1 photo souvenir" : nextCount + " photos souvenirs");
            if (nextCount >= memories.size()) {
                loadMoreButton.setVisibility(View.GONE);
            }
        });

        return screen;
    }

    private View createMemoryTile(StepMemoryPhoto memory) {
        String place = TextUtils.isEmpty(memory.getStepName())
                ? (TextUtils.isEmpty(memory.getTripName()) ? memory.getDestination() : memory.getTripName())
                : memory.getStepName();
        return createMemoryTile(Uri.parse(memory.getPhotoUri()), place, memory.getDate());
    }

    private View createMemoryTile(int imageResId, String place, String date) {
        FrameLayout tile = new FrameLayout(this);
        tile.setClickable(true);
        tile.setFocusable(true);

        ImageView image = new ImageView(this);
        image.setImageResource(imageResId);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setClickable(false);
        tile.addView(image, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        View shade = new View(this);
        shade.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.TRANSPARENT, Color.argb(185, 0, 0, 0)}
        ));
        shade.setVisibility(View.GONE);
        shade.setClickable(false);
        tile.addView(shade, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(16), 0, dp(12), dp(18));
        info.setVisibility(View.GONE);
        info.setClickable(false);

        TextView placeText = new TextView(this);
        placeText.setText(place);
        placeText.setTextColor(Color.WHITE);
        placeText.setTextSize(16);
        placeText.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        info.addView(placeText);

        TextView dateText = new TextView(this);
        dateText.setText(date);
        dateText.setTextColor(Color.WHITE);
        dateText.setTextSize(14);
        dateText.setPadding(0, dp(6), 0, 0);
        info.addView(dateText);

        FrameLayout.LayoutParams infoParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
        );
        tile.addView(info, infoParams);

        attachMemoryReveal(tile, shade, info);
        attachMemoryReveal(image, shade, info);
        attachMemoryReveal(shade, shade, info);
        attachMemoryReveal(info, shade, info);
        attachMemoryReveal(placeText, shade, info);
        attachMemoryReveal(dateText, shade, info);
        tile.setOnClickListener(view -> showMemoryInfo(shade, info));

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = dp(200);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(dp(2), dp(2), dp(2), dp(2));
        tile.setLayoutParams(params);
        return tile;
    }

    private View createMemoryTile(Uri imageUri, String place, String date) {
        FrameLayout tile = new FrameLayout(this);
        tile.setClickable(true);
        tile.setFocusable(true);

        ImageView image = new ImageView(this);
        image.setImageURI(imageUri);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setClickable(false);
        tile.addView(image, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        View shade = new View(this);
        shade.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.TRANSPARENT, Color.argb(185, 0, 0, 0)}
        ));
        shade.setVisibility(View.GONE);
        shade.setClickable(false);
        tile.addView(shade, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(16), 0, dp(12), dp(18));
        info.setVisibility(View.GONE);
        info.setClickable(false);

        TextView placeText = new TextView(this);
        placeText.setText(TextUtils.isEmpty(place) ? "Souvenir" : place);
        placeText.setTextColor(Color.WHITE);
        placeText.setTextSize(16);
        placeText.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        info.addView(placeText);

        TextView dateText = new TextView(this);
        dateText.setText(TextUtils.isEmpty(date) ? "Date a definir" : date);
        dateText.setTextColor(Color.WHITE);
        dateText.setTextSize(14);
        dateText.setPadding(0, dp(6), 0, 0);
        info.addView(dateText);

        FrameLayout.LayoutParams infoParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
        );
        tile.addView(info, infoParams);

        attachMemoryReveal(tile, shade, info);
        attachMemoryReveal(image, shade, info);
        attachMemoryReveal(shade, shade, info);
        attachMemoryReveal(info, shade, info);
        attachMemoryReveal(placeText, shade, info);
        attachMemoryReveal(dateText, shade, info);
        tile.setOnClickListener(view -> showMemoryInfo(shade, info));

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = dp(200);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(dp(2), dp(2), dp(2), dp(2));
        tile.setLayoutParams(params);
        return tile;
    }

    private void attachMemoryReveal(View target, View shade, View info) {
        View.OnHoverListener memoryHoverListener = (view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_HOVER_ENTER
                    || event.getAction() == MotionEvent.ACTION_HOVER_MOVE) {
                cancelPendingMemoryHide();
                showMemoryInfo(shade, info);
            } else if (event.getAction() == MotionEvent.ACTION_HOVER_EXIT
                    && activeMemoryShade == shade
                    && !isPointerInsideView(view, event)) {
                scheduleMemoryHide(shade);
            }
            return true;
        };
        target.setOnHoverListener(memoryHoverListener);
        target.setOnGenericMotionListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_HOVER_ENTER
                    || event.getAction() == MotionEvent.ACTION_HOVER_MOVE) {
                cancelPendingMemoryHide();
                showMemoryInfo(shade, info);
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_HOVER_EXIT && activeMemoryShade == shade) {
                scheduleMemoryHide(shade);
                return true;
            }
            return false;
        });
        target.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN
                    || event.getAction() == MotionEvent.ACTION_MOVE) {
                cancelPendingMemoryHide();
                showMemoryInfo(shade, info);
                return false;
            }
            if ((event.getAction() == MotionEvent.ACTION_UP
                    || event.getAction() == MotionEvent.ACTION_CANCEL)
                    && activeMemoryShade == shade
                    && !isPointerInsideView(view, event)) {
                scheduleMemoryHide(shade);
            }
            return false;
        });
    }

    private void showMemoryInfo(View shade, View info) {
        cancelPendingMemoryHide();
        if (activeMemoryShade != shade) {
            hideActiveMemoryInfo();
        }
        shade.setVisibility(View.VISIBLE);
        info.setVisibility(View.VISIBLE);
        activeMemoryShade = shade;
        activeMemoryInfo = info;
    }

    private void hideActiveMemoryInfo() {
        cancelPendingMemoryHide();
        if (activeMemoryShade != null) {
            activeMemoryShade.setVisibility(View.GONE);
        }
        if (activeMemoryInfo != null) {
            activeMemoryInfo.setVisibility(View.GONE);
        }
        activeMemoryShade = null;
        activeMemoryInfo = null;
    }

    private void scheduleMemoryHide(View shade) {
        cancelPendingMemoryHide();
        pendingMemoryHide = () -> {
            if (activeMemoryShade == shade) {
                hideActiveMemoryInfo();
            }
        };
        if (rootContainer != null) {
            rootContainer.postDelayed(pendingMemoryHide, 80);
        }
    }

    private void cancelPendingMemoryHide() {
        if (pendingMemoryHide != null && rootContainer != null) {
            rootContainer.removeCallbacks(pendingMemoryHide);
        }
        pendingMemoryHide = null;
    }

    private boolean isPointerInsideView(View view, MotionEvent event) {
        return event.getX() >= 0
                && event.getX() <= view.getWidth()
                && event.getY() >= 0
                && event.getY() <= view.getHeight();
    }

    private void showExplorerContent() {
        hideSecondaryContent();
        if (explorerContent == null) {
            explorerContent = createExplorerView();
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            params.bottomMargin = dp(83);
            rootContainer.addView(explorerContent, params);
        } else {
            explorerContent.setVisibility(View.VISIBLE);
        }
        addTripButton.setVisibility(View.GONE);
        navigationBar.bringToFront();
    }

    private View createExplorerView() {
        FrameLayout page = new FrameLayout(this);
        page.setBackgroundColor(Color.WHITE);

        explorerScrollView = new ScrollView(this);
        explorerScrollView.setFillViewport(true);
        explorerScrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        explorerScrollView.setVerticalScrollBarEnabled(false);
        explorerScrollView.setFadingEdgeLength(0);

        explorerBody = new LinearLayout(this);
        explorerBody.setOrientation(LinearLayout.VERTICAL);
        explorerBody.setPadding(dp(18), dp(10), dp(18), dp(28));
        explorerScrollView.addView(explorerBody);
        page.addView(explorerScrollView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        explorerProgressBar = new ProgressBar(this);
        page.addView(explorerProgressBar, new FrameLayout.LayoutParams(
                dp(48),
                dp(48),
                Gravity.CENTER
        ));

        explorerErrorView = new LinearLayout(this);
        explorerErrorView.setOrientation(LinearLayout.VERTICAL);
        explorerErrorView.setGravity(Gravity.CENTER);
        explorerErrorView.setPadding(dp(24), dp(24), dp(24), dp(24));
        explorerErrorText = createExplorerText(
                "Impossible de charger les destinations",
                16,
                Color.rgb(7, 56, 68),
                true
        );
        explorerErrorText.setGravity(Gravity.CENTER);
        explorerErrorView.addView(explorerErrorText);

        Button retryButton = new Button(this);
        retryButton.setText("Réessayer");
        retryButton.setTextColor(Color.WHITE);
        retryButton.setAllCaps(false);
        retryButton.setBackground(createRoundedBackground(NAV_ACTIVE_COLOR, 12));
        retryButton.setOnClickListener(view -> loadExploreDestinations());
        LinearLayout.LayoutParams retryParams = new LinearLayout.LayoutParams(dp(150), dp(52));
        retryParams.topMargin = dp(16);
        explorerErrorView.addView(retryButton, retryParams);
        explorerErrorView.setVisibility(View.GONE);
        page.addView(explorerErrorView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        loadExploreDestinations();
        return page;
    }

    private void renderExplorerContent() {
        explorerBody.removeAllViews();
        List<ExploreDestination> personalized = personalizeDestinations(explorerDestinations);
        List<ExploreDestination> featured = filterByCategory(personalized, "featured");
        List<ExploreDestination> popular = filterByCategory(personalized, "popular");
        List<ExploreDestination> trends = filterByCategory(personalized, "trend");

        LinearLayout topBar = new LinearLayout(this);
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        ImageButton backButton = new ImageButton(this);
        backButton.setImageResource(R.drawable.ic_add_trip_back);
        backButton.setScaleType(ImageView.ScaleType.CENTER);
        backButton.setPadding(dp(15), dp(15), dp(15), dp(15));
        backButton.setBackgroundColor(Color.TRANSPARENT);
        backButton.setContentDescription("Retour");
        backButton.setOnClickListener(view -> {
            selectNavigationItem(0);
            showHomeContent();
        });
        topBar.addView(backButton, new LinearLayout.LayoutParams(dp(54), dp(54)));

        TextView pageTitle = createExplorerText("Explorer", 23, Color.rgb(7, 56, 68), true);
        pageTitle.setGravity(Gravity.CENTER);
        topBar.addView(pageTitle, new LinearLayout.LayoutParams(0, dp(54), 1));

        Button refresh = new Button(this);
        refresh.setText("↻");
        refresh.setTextSize(23);
        refresh.setTextColor(NAV_ACTIVE_COLOR);
        refresh.setBackgroundColor(Color.TRANSPARENT);
        refresh.setOnClickListener(view -> loadExploreDestinations());
        topBar.addView(refresh, new LinearLayout.LayoutParams(dp(54), dp(54)));
        explorerBody.addView(topBar);

        if (!featured.isEmpty()) {
            explorerBody.addView(createSectionTitle("Pour vous"));
            HorizontalScrollView featuredScroll = createHorizontalScroll();
            LinearLayout featuredRow = createHorizontalRow();
            int featuredWidth = getResources().getDisplayMetrics().widthPixels - dp(100);
            for (ExploreDestination destination : featured) {
                featuredRow.addView(createFeaturedDestinationCard(destination, featuredWidth, dp(178)));
            }
            featuredScroll.addView(featuredRow);
            explorerBody.addView(featuredScroll);
        }

        if (!popular.isEmpty()) {
            explorerBody.addView(createSectionTitle("Populaires"));
            HorizontalScrollView popularScroll = createHorizontalScroll();
            LinearLayout popularRow = createHorizontalRow();
            for (ExploreDestination destination : popular) {
                popularRow.addView(createPopularDestinationItem(destination));
            }
            popularScroll.addView(popularRow);
            explorerBody.addView(popularScroll);
        }

        if (!trends.isEmpty()) {
            explorerBody.addView(createSectionTitle("Tendances du mois"));
            HorizontalScrollView trendingScroll = createHorizontalScroll();
            LinearLayout trendingPages = createHorizontalRow();
            int trendingPageWidth = getResources().getDisplayMetrics().widthPixels - dp(104);
            int pageCount = (trends.size() + 2) / 3;
            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                LinearLayout trendingPage = new LinearLayout(this);
                trendingPage.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams pageParams = new LinearLayout.LayoutParams(
                        trendingPageWidth,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                pageParams.rightMargin = dp(18);
                trendingPage.setLayoutParams(pageParams);

                int pageStart = pageIndex * 3;
                int pageEnd = Math.min(pageStart + 3, trends.size());
                for (int index = pageStart; index < pageEnd; index++) {
                    trendingPage.addView(createTrendingRow(trends.get(index)));
                }
                trendingPages.addView(trendingPage);
            }
            trendingScroll.addView(trendingPages);
            explorerBody.addView(trendingScroll);
        }

        Set<String> continents = new LinkedHashSet<>();
        for (ExploreDestination destination : explorerDestinations) {
            if (!TextUtils.isEmpty(destination.getContinent())) {
                continents.add(destination.getContinent());
            }
        }
        if (!continents.isEmpty()) {
            explorerBody.addView(createSectionTitle("Parcourir par continent"));
            LinearLayout grid = new LinearLayout(this);
            grid.setOrientation(LinearLayout.VERTICAL);
            int[] colors = {
                    Color.rgb(255, 126, 71),
                    Color.rgb(244, 84, 70),
                    Color.rgb(219, 112, 177),
                    Color.rgb(48, 169, 219),
                    Color.rgb(255, 91, 126),
                    Color.rgb(176, 174, 165)
            };
            int index = 0;
            LinearLayout row = null;
            for (String continent : continents) {
                if (index % 2 == 0) {
                    row = new LinearLayout(this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    rowParams.bottomMargin = dp(8);
                    grid.addView(row, rowParams);
                }
                row.addView(createContinentGridCard(continent, colors[index % colors.length], index));
                index++;
            }
            explorerBody.addView(grid);
        }
    }

    private TextView createSectionTitle(String title) {
        TextView view = createExplorerText(title, 18, Color.rgb(7, 56, 68), true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(14), 0, dp(10));
        view.setLayoutParams(params);
        return view;
    }

    private HorizontalScrollView createHorizontalScroll() {
        HorizontalScrollView scrollView = new HorizontalScrollView(this);
        scrollView.setHorizontalScrollBarEnabled(false);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        return scrollView;
    }

    private LinearLayout createHorizontalRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setClipToPadding(false);
        row.setPadding(0, 0, dp(12), 0);
        return row;
    }

    private View createFeaturedDestinationCard(
            ExploreDestination destination,
            int width,
            int height
    ) {
        FrameLayout card = new FrameLayout(this);
        card.setBackground(createRoundedBackground(Color.rgb(242, 246, 248), 18));
        card.setClipToOutline(true);
        card.setOnClickListener(view -> showDestinationDetails(destination));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.rightMargin = dp(14);
        card.setLayoutParams(params);

        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        loadDestinationImage(image, destination.getImageUrl());
        card.addView(image, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        View shade = new View(this);
        shade.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.TRANSPARENT, Color.argb(150, 0, 0, 0)}
        ));
        card.addView(shade, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        ImageView flag = new ImageView(this);
        flag.setScaleType(ImageView.ScaleType.CENTER_CROP);
        flag.setBackground(createRoundedBackground(Color.WHITE, 30));
        flag.setClipToOutline(true);
        loadFlagImage(flag, destination.getFlagUrl());
        FrameLayout.LayoutParams flagParams = new FrameLayout.LayoutParams(dp(34), dp(34));
        flagParams.leftMargin = dp(16);
        flagParams.topMargin = dp(14);
        card.addView(flag, flagParams);

        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        textBlock.setPadding(dp(18), 0, dp(18), dp(18));
        textBlock.setGravity(Gravity.BOTTOM);
        textBlock.addView(createExplorerText("POPULAIRES", 10, Color.WHITE, true));
        textBlock.addView(createExplorerText(destination.getName(), 23, Color.WHITE, true));
        textBlock.addView(createExplorerText(destination.getPlacesCount() + " lieux", 11, Color.WHITE, true));
        card.addView(textBlock, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        return card;
    }

    private View createPopularDestinationItem(ExploreDestination destination) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(dp(154), dp(176));
        itemParams.rightMargin = dp(12);
        item.setLayoutParams(itemParams);
        item.setOnClickListener(view -> showDestinationDetails(destination));

        FrameLayout imageCard = new FrameLayout(this);
        imageCard.setClipToOutline(true);
        imageCard.setBackground(createRoundedBackground(Color.WHITE, 16));

        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        loadDestinationImage(image, destination.getImageUrl());
        imageCard.addView(image, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        TextView name = createExplorerText(destination.getName(), 20, Color.WHITE, true);
        name.setShadowLayer(5, 0, 2, Color.BLACK);
        name.setGravity(Gravity.CENTER);
        name.setPadding(dp(8), dp(8), dp(8), dp(8));
        imageCard.addView(name, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        ImageView flag = new ImageView(this);
        flag.setScaleType(ImageView.ScaleType.CENTER_CROP);
        flag.setBackground(createRoundedBackground(Color.WHITE, 30));
        flag.setClipToOutline(true);
        loadFlagImage(flag, destination.getFlagUrl());
        FrameLayout.LayoutParams flagParams = new FrameLayout.LayoutParams(dp(32), dp(32));
        flagParams.gravity = Gravity.TOP | Gravity.START;
        flagParams.leftMargin = dp(9);
        flagParams.topMargin = dp(9);
        imageCard.addView(flag, flagParams);

        item.addView(imageCard, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(142)
        ));

        TextView count = createExplorerText(
                (destination.getPlacesCount() + " lieux").toUpperCase(),
                11,
                NAV_INACTIVE_COLOR,
                true
        );
        LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        countParams.topMargin = dp(7);
        item.addView(count, countParams);
        return item;
    }

    private View createTrendingRow(ExploreDestination destination) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));
        row.setOnClickListener(view -> showDestinationDetails(destination));

        FrameLayout imageContainer = new FrameLayout(this);
        imageContainer.setBackground(createRoundedBackground(Color.LTGRAY, 18));
        imageContainer.setClipToOutline(true);
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        loadDestinationImage(image, destination.getImageUrl());
        imageContainer.addView(image, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        row.addView(imageContainer, new LinearLayout.LayoutParams(dp(88), dp(88)));

        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        textBlock.setGravity(Gravity.CENTER_VERTICAL);
        textBlock.setPadding(dp(18), 0, 0, 0);
        textBlock.addView(createExplorerText(destination.getName(), 18, Color.rgb(7, 56, 68), true));
        TextView subtitle = createExplorerText(destination.getCountry(), 14, NAV_INACTIVE_COLOR, false);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        subtitleParams.topMargin = dp(3);
        textBlock.addView(subtitle, subtitleParams);
        row.addView(textBlock, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        return row;
    }

    private View createContinentGridCard(String continent, int accentColor, int position) {
        FrameLayout card = new FrameLayout(this);
        card.setBackground(createRoundedBackground(Color.rgb(247, 248, 250), 14));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(70), 1);
        if (position % 2 == 0) {
            params.rightMargin = dp(6);
        } else {
            params.leftMargin = dp(6);
        }
        card.setLayoutParams(params);

        TextView label = createExplorerText(continent, 14, Color.rgb(7, 56, 68), true);
        label.setGravity(Gravity.CENTER_VERTICAL);
        label.setPadding(dp(14), 0, dp(72), 0);
        card.addView(label, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        ContinentShapeView shape = new ContinentShapeView(accentColor, position);
        card.addView(shape, new FrameLayout.LayoutParams(dp(70), dp(70), Gravity.END));
        card.setOnClickListener(view -> {
            Intent intent = new Intent(this, ContinentDestinationsActivity.class);
            intent.putExtra(ContinentDestinationsActivity.EXTRA_CONTINENT_NAME, continent);
            startActivity(intent);
        });
        return card;
    }

    private class ContinentShapeView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int shapeIndex;

        ContinentShapeView(int color, int shapeIndex) {
            super(HomeActivity.this);
            this.shapeIndex = shapeIndex;
            paint.setColor(color);
            paint.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float width = getWidth();
            float height = getHeight();
            Path shape = new Path();
            switch (shapeIndex % 6) {
                case 0:
                    shape.moveTo(width * .42f, height * .08f);
                    shape.lineTo(width * .70f, height * .16f);
                    shape.lineTo(width * .83f, height * .38f);
                    shape.lineTo(width * .65f, height * .55f);
                    shape.lineTo(width * .58f, height * .88f);
                    shape.lineTo(width * .39f, height * .70f);
                    shape.lineTo(width * .25f, height * .42f);
                    break;
                case 1:
                    shape.moveTo(width * .18f, height * .22f);
                    shape.lineTo(width * .43f, height * .08f);
                    shape.lineTo(width * .76f, height * .18f);
                    shape.lineTo(width * .88f, height * .44f);
                    shape.lineTo(width * .70f, height * .60f);
                    shape.lineTo(width * .79f, height * .86f);
                    shape.lineTo(width * .48f, height * .76f);
                    shape.lineTo(width * .32f, height * .55f);
                    break;
                case 2:
                    shape.moveTo(width * .30f, height * .08f);
                    shape.lineTo(width * .66f, height * .18f);
                    shape.lineTo(width * .78f, height * .38f);
                    shape.lineTo(width * .61f, height * .58f);
                    shape.lineTo(width * .55f, height * .90f);
                    shape.lineTo(width * .35f, height * .66f);
                    shape.lineTo(width * .22f, height * .35f);
                    break;
                case 3:
                    shape.moveTo(width * .14f, height * .26f);
                    shape.lineTo(width * .42f, height * .10f);
                    shape.lineTo(width * .82f, height * .18f);
                    shape.lineTo(width * .90f, height * .44f);
                    shape.lineTo(width * .68f, height * .60f);
                    shape.lineTo(width * .48f, height * .88f);
                    shape.lineTo(width * .31f, height * .58f);
                    break;
                case 4:
                    shape.moveTo(width * .23f, height * .20f);
                    shape.lineTo(width * .48f, height * .08f);
                    shape.lineTo(width * .78f, height * .22f);
                    shape.lineTo(width * .70f, height * .47f);
                    shape.lineTo(width * .86f, height * .70f);
                    shape.lineTo(width * .50f, height * .86f);
                    shape.lineTo(width * .21f, height * .64f);
                    break;
                default:
                    shape.moveTo(width * .30f, height * .22f);
                    shape.lineTo(width * .58f, height * .12f);
                    shape.lineTo(width * .84f, height * .34f);
                    shape.lineTo(width * .76f, height * .70f);
                    shape.lineTo(width * .48f, height * .84f);
                    shape.lineTo(width * .20f, height * .60f);
                    break;
            }
            shape.close();
            canvas.drawPath(shape, paint);
        }
    }

    private void showDestinationDetails(ExploreDestination destination) {
        startActivity(DestinationDetailsActivity.createIntent(this, destination));
    }

    private void loadExploreDestinations() {
        explorerProgressBar.setVisibility(View.VISIBLE);
        explorerErrorView.setVisibility(View.GONE);
        explorerScrollView.setVisibility(View.INVISIBLE);

        SharedPreferences preferences = getSharedPreferences(ExploreRepository.PREFS_NAME, MODE_PRIVATE);
        if (preferences.getBoolean(ExploreRepository.KEY_OFFLINE_MODE, false)) {
            Toast.makeText(this, "Mode hors ligne activé", Toast.LENGTH_SHORT).show();
        }

        exploreRepository.getExploreDestinations(new ExploreRepository.RepositoryCallback() {
            @Override
            public void onSuccess(List<ExploreDestination> destinations, boolean fromCache) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                explorerDestinations.clear();
                explorerDestinations.addAll(destinations);
                explorerProgressBar.setVisibility(View.GONE);
                explorerErrorView.setVisibility(View.GONE);
                explorerScrollView.setVisibility(View.VISIBLE);
                renderExplorerContent();
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                explorerProgressBar.setVisibility(View.GONE);
                explorerScrollView.setVisibility(View.INVISIBLE);
                explorerErrorText.setText(message);
                explorerErrorView.setVisibility(View.VISIBLE);
            }
        });
    }

    private List<ExploreDestination> filterByCategory(List<ExploreDestination> destinations, String category) {
        List<ExploreDestination> filtered = new ArrayList<>();
        for (ExploreDestination destination : destinations) {
            if (category.equalsIgnoreCase(destination.getCategory())) {
                filtered.add(destination);
            }
        }
        return filtered;
    }

    private List<ExploreDestination> personalizeDestinations(List<ExploreDestination> destinations) {
        List<ExploreDestination> sorted = new ArrayList<>(destinations);
        SharedPreferences preferences = getSharedPreferences(ExploreRepository.PREFS_NAME, MODE_PRIVATE);
        String preferredTheme = preferences.getString(ExploreRepository.KEY_PREFERRED_THEME, "");
        preferences.getString(ExploreRepository.KEY_LANGUAGE, "fr");
        preferences.getString(ExploreRepository.KEY_CURRENCY, "EUR");
        if (!TextUtils.isEmpty(preferredTheme)) {
            Collections.sort(sorted, new Comparator<ExploreDestination>() {
                @Override
                public int compare(ExploreDestination first, ExploreDestination second) {
                    boolean firstMatches = preferredTheme.equalsIgnoreCase(first.getTheme());
                    boolean secondMatches = preferredTheme.equalsIgnoreCase(second.getTheme());
                    return Boolean.compare(!firstMatches, !secondMatches);
                }
            });
        }
        return sorted;
    }

    private void loadDestinationImage(ImageView imageView, String imageUrl) {
        Glide.with(this)
                .load(TextUtils.isEmpty(imageUrl) ? null : imageUrl)
                .placeholder(R.drawable.placeholder_destination)
                .error(R.drawable.placeholder_destination)
                .centerCrop()
                .into(imageView);
    }

    private void loadFlagImage(ImageView imageView, String flagUrl) {
        Glide.with(this)
                .load(TextUtils.isEmpty(flagUrl) ? null : flagUrl)
                .placeholder(R.drawable.placeholder_flag)
                .error(R.drawable.placeholder_flag)
                .circleCrop()
                .into(imageView);
    }

    private TextView createExplorerText(String text, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) {
            view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
        }
        return view;
    }

    private GradientDrawable createRoundedBackground(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private void hideSecondaryContent() {
        if (notificationsContent != null) {
            notificationsContent.setVisibility(View.GONE);
        }
        if (memoriesContent != null) {
            memoriesContent.setVisibility(View.GONE);
        }
        if (explorerContent != null) {
            explorerContent.setVisibility(View.GONE);
        }
        if (profileContent != null) {
            profileContent.setVisibility(View.GONE);
        }
        hideActiveMemoryInfo();
    }

    private void showHomeContent() {
        hideSecondaryContent();
        addTripButton.setVisibility(View.VISIBLE);
        addTripButton.bringToFront();
        navigationBar.bringToFront();
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

    @Override
    protected void onResume() {
        super.onResume();
        if (navigationIcons != null
                && (notificationsContent == null
                || notificationsContent.getVisibility() != View.VISIBLE)
                && (memoriesContent == null
                || memoriesContent.getVisibility() != View.VISIBLE)
                && (explorerContent == null
                || explorerContent.getVisibility() != View.VISIBLE)
                && (profileContent == null
                || profileContent.getVisibility() != View.VISIBLE)) {
            selectNavigationItem(0);
        }
        if (profileContent != null && profileContent.getVisibility() == View.VISIBLE) {
            bindProfileData();
            bindProfileStats();
            selectNavigationItem(4);
        }
        if (tripAdapter != null && tripDao != null) {
            List<Trip> trips = tripDao.getTripsForHome(getConnectedUserId());
            allTrips.clear();
            allTrips.addAll(trips.isEmpty() ? createTrips() : trips);
            filterTrips(searchInput == null ? "" : searchInput.getText().toString());
        }
    }

    private void updateUserHeader() {
        TextView greetingText = findViewById(R.id.txt_user_greeting);
        TextView initialsText = findViewById(R.id.txt_user_initials);

        String name = getConnectedUserName();
        greetingText.setText("Bonjour, " + name);
        initialsText.setText(getInitials(name));
    }

    private String getConnectedUserName() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return "Voyageur";
        }

        String displayName = user.getDisplayName();
        if (!TextUtils.isEmpty(displayName)) {
            return displayName.trim();
        }

        String email = user.getEmail();
        if (!TextUtils.isEmpty(email) && email.contains("@")) {
            return email.substring(0, email.indexOf("@")).trim();
        }

        return "Voyageur";
    }

    private String getConnectedUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user == null ? "guest" : user.getUid();
    }

    private String getInitials(String name) {
        if (TextUtils.isEmpty(name)) {
            return "T";
        }

        String[] parts = name.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                initials.append(Character.toUpperCase(part.charAt(0)));
            }
            if (initials.length() == 2) {
                break;
            }
        }

        return initials.length() == 0 ? "T" : initials.toString();
    }

    private List<Trip> createTrips() {
        List<Trip> trips = new ArrayList<>();
        trips.add(new Trip("A VENIR", "Paradis aux Maldives", "15 juin - 22 juin 2024", "5 lieux", R.drawable.travel_beach_bg));
        trips.add(new Trip(null, "Paris et Bruxelles", "5 aout - 18 aout 2024", "8 lieux", R.drawable.travel_balloons_bg));
        trips.add(new Trip(null, "Aventure dans les Alpes suisses", "10 septembre - 20 septembre 2024", "4 lieux", R.drawable.travel_beach_bg));
        trips.add(new Trip("VOYAGES PASSES", "Escapade a Venise", "12 mars - 19 mars 2024", "3 lieux", R.drawable.travel_balloons_bg));
        return trips;
    }

    private void filterTrips(String query) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        List<Trip> filtered = new ArrayList<>();
        for (Trip trip : allTrips) {
            if (TextUtils.isEmpty(normalizedQuery) || matchesTrip(trip, normalizedQuery)) {
                filtered.add(trip);
            }
        }
        applyFrenchSections(filtered);
        tripAdapter.setTrips(filtered);
        noTripsText.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private boolean matchesTrip(Trip trip, String query) {
        return contains(trip.getName(), query)
                || contains(trip.getDestination(), query)
                || contains(trip.getDates(), query)
                || contains(trip.getLocations(), query)
                || contains(trip.getHotelName(), query)
                || contains(trip.getHotelAddress(), query)
                || contains(trip.getNotes(), query);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private void applyFrenchSections(List<Trip> trips) {
        boolean hasUpcomingSection = false;
        boolean hasPastSection = false;
        for (Trip trip : trips) {
            boolean past = Trip.TYPE_PAST.equals(trip.getTripType())
                    || "VOYAGES PASSES".equals(trip.getSection())
                    || "PAST TRIPS".equals(trip.getSection());
            if (past) {
                trip.setSection(hasPastSection ? null : "VOYAGES PASSES");
                hasPastSection = true;
            } else {
                trip.setSection(hasUpcomingSection ? null : "A VENIR");
                hasUpcomingSection = true;
            }
        }
    }
}
