package com.example.travelin;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

public class TripDetailActivity extends AppCompatActivity {
    public static final String EXTRA_TRIP_ID = "extra_trip_id";
    public static final String EXTRA_TRIP_NAME = "extra_trip_name";
    public static final String EXTRA_TRIP_DATES = "extra_trip_dates";
    public static final String EXTRA_TRIP_IMAGE = "extra_trip_image";
    public static final String EXTRA_HOTEL_PHONE = "extra_hotel_phone";
    private static final String MAP_VIEW_BUNDLE_KEY = "trip_detail_map_view_bundle";
    private static final float STEP_MAP_ZOOM = 13f;

    private long tripId;
    private String tripName;
    private String hotelPhone;
    private TripDao tripDao;
    private MapView mapView;
    private GoogleMap googleMap;
    private TextView noMapStepsText;
    private LinearLayout detailContentContainer;
    private int planningTitleIndex = -1;
    private int quickActionsTitleIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_detail);

        tripId = getIntent().getLongExtra(EXTRA_TRIP_ID, 0);
        tripName = getIntent().getStringExtra(EXTRA_TRIP_NAME);
        String tripDates = getIntent().getStringExtra(EXTRA_TRIP_DATES);
        hotelPhone = getIntent().getStringExtra(EXTRA_HOTEL_PHONE);
        int imageRes = getIntent().getIntExtra(EXTRA_TRIP_IMAGE, R.drawable.travel_beach_bg);
        tripDao = new TripDao(this);

        ImageView heroImage = findViewById(R.id.img_detail_hero);
        TextView titleText = findViewById(R.id.txt_detail_title);
        TextView datesText = findViewById(R.id.txt_detail_dates);
        mapView = findViewById(R.id.detail_map_view);
        noMapStepsText = findViewById(R.id.txt_no_map_steps);
        detailContentContainer = findViewById(R.id.detail_content_container);
        cachePlanningSectionIndexes();

        heroImage.setImageResource(imageRes);
        titleText.setText(TextUtils.isEmpty(tripName) ? "Voyage" : tripName);
        datesText.setText(TextUtils.isEmpty(tripDates) ? "Dates a definir" : tripDates);

        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
        }
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(map -> {
            googleMap = map;
            googleMap.getUiSettings().setZoomControlsEnabled(true);
            googleMap.getUiSettings().setMapToolbarEnabled(false);
            renderStepMarkers();
        });
        mapView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
            }
            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.getParent().requestDisallowInterceptTouchEvent(false);
            }
            return false;
        });

        ImageButton backButton = findViewById(R.id.btn_detail_back);
        backButton.setOnClickListener(v -> finish());
        findViewById(R.id.btn_add_step).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddStepActivity.class);
            intent.putExtra(AddStepActivity.EXTRA_TRIP_ID, tripId);
            startActivity(intent);
        });
        findViewById(R.id.btn_share_trip).setOnClickListener(v -> shareTrip());
        findViewById(R.id.btn_alert_trip).setOnClickListener(v ->
                Toast.makeText(this, "Rappel de voyage active", Toast.LENGTH_SHORT).show());
        findViewById(R.id.card_call_hotel).setOnClickListener(v -> callHotel());
        findViewById(R.id.card_share_sms).setOnClickListener(v -> shareTrip());
    }

    private void renderStepMarkers() {
        if (googleMap == null || tripDao == null) {
            return;
        }

        googleMap.clear();
        List<TripStep> steps = tripDao.getStepsForTrip(tripId);
        LatLng firstStepPosition = null;
        int markerCount = 0;

        for (TripStep step : steps) {
            if (!step.hasCoordinates()) {
                continue;
            }
            LatLng position = new LatLng(step.getLatitude(), step.getLongitude());
            googleMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(step.getLocationName()));
            if (firstStepPosition == null) {
                firstStepPosition = position;
            }
            markerCount++;
        }

        noMapStepsText.setVisibility(markerCount == 0 ? View.VISIBLE : View.GONE);
        if (firstStepPosition != null) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstStepPosition, STEP_MAP_ZOOM));
        }
    }

    private void renderPlanningSteps() {
        if (detailContentContainer == null || tripDao == null) {
            return;
        }
        if (planningTitleIndex == -1 || quickActionsTitleIndex == -1) {
            cachePlanningSectionIndexes();
        }
        if (planningTitleIndex == -1 || quickActionsTitleIndex == -1) {
            return;
        }

        while (quickActionsTitleIndex - planningTitleIndex > 1) {
            detailContentContainer.removeViewAt(planningTitleIndex + 1);
            quickActionsTitleIndex--;
        }

        List<TripStep> steps = tripDao.getStepsForTrip(tripId);
        if (steps.isEmpty()) {
            detailContentContainer.addView(createEmptyPlanningView(), planningTitleIndex + 1);
            quickActionsTitleIndex++;
            return;
        }

        int insertIndex = planningTitleIndex + 1;
        for (TripStep step : steps) {
            detailContentContainer.addView(createPlanningStepView(step), insertIndex);
            insertIndex++;
            quickActionsTitleIndex++;
        }
    }

    private void cachePlanningSectionIndexes() {
        if (detailContentContainer == null) {
            return;
        }
        planningTitleIndex = -1;
        quickActionsTitleIndex = -1;
        for (int i = 0; i < detailContentContainer.getChildCount(); i++) {
            View child = detailContentContainer.getChildAt(i);
            if (child instanceof TextView) {
                CharSequence text = ((TextView) child).getText();
                if ("Planning du voyage".contentEquals(text)) {
                    planningTitleIndex = i;
                } else if ("Quick Actions".contentEquals(text)) {
                    quickActionsTitleIndex = i;
                }
            }
        }
    }

    private View createEmptyPlanningView() {
        TextView textView = new TextView(this);
        textView.setText("Aucune étape ajoutée");
        textView.setTextColor(0xFF29435C);
        textView.setTextSize(16);
        textView.setGravity(Gravity.CENTER);
        textView.setBackgroundResource(R.drawable.detail_card_background);
        textView.setPadding(dp(18), dp(22), dp(18), dp(22));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(16);
        textView.setLayoutParams(params);
        return textView;
    }

    private View createPlanningStepView(TripStep step) {
        LinearLayout card = new LinearLayout(this);
        card.setBackgroundResource(R.drawable.detail_card_background);
        card.setClickable(true);
        card.setElevation(dp(3));
        card.setFocusable(true);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setOnClickListener(v -> openStepEditor(step));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.topMargin = dp(16);
        card.setLayoutParams(cardParams);

        FrameLayout iconBubble = new FrameLayout(this);
        iconBubble.setBackgroundResource(R.drawable.detail_icon_bubble);
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_detail_pin);
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(dp(28), dp(28), Gravity.CENTER);
        iconBubble.addView(icon, iconParams);
        card.addView(iconBubble, new LinearLayout.LayoutParams(dp(54), dp(54)));

        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textBlockParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        );
        textBlockParams.leftMargin = dp(18);
        card.addView(textBlock, textBlockParams);

        TextView title = new TextView(this);
        title.setText(TextUtils.isEmpty(step.getLocationName()) ? "Étape" : step.getLocationName());
        title.setTextColor(0xFF071D2B);
        title.setTextSize(20);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        textBlock.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView dateTime = new TextView(this);
        dateTime.setText(formatStepDateTime(step));
        dateTime.setTextColor(0xFF6B7C93);
        dateTime.setTextSize(14);
        dateTime.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_detail_clock, 0, 0, 0);
        dateTime.setCompoundDrawablePadding(dp(7));
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        dateParams.topMargin = dp(8);
        textBlock.addView(dateTime, dateParams);

        if (!TextUtils.isEmpty(step.getDescription())) {
            TextView description = new TextView(this);
            description.setText(step.getDescription());
            description.setTextColor(0xFF29435C);
            description.setTextSize(15);
            LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            descriptionParams.topMargin = dp(12);
            textBlock.addView(description, descriptionParams);
        }

        return card;
    }

    private void openStepEditor(TripStep step) {
        Intent intent = new Intent(this, AddStepActivity.class);
        intent.putExtra(AddStepActivity.EXTRA_TRIP_ID, tripId);
        intent.putExtra(AddStepActivity.EXTRA_STEP_ID, step.getId());
        intent.putExtra(AddStepActivity.EXTRA_STEP_NAME, step.getLocationName());
        intent.putExtra(AddStepActivity.EXTRA_STEP_DESCRIPTION, step.getDescription());
        intent.putExtra(AddStepActivity.EXTRA_STEP_DATE, step.getDate());
        intent.putExtra(AddStepActivity.EXTRA_STEP_TIME, step.getTime());
        if (step.hasCoordinates()) {
            intent.putExtra(AddStepActivity.EXTRA_STEP_LATITUDE, step.getLatitude());
            intent.putExtra(AddStepActivity.EXTRA_STEP_LONGITUDE, step.getLongitude());
        }
        startActivity(intent);
    }

    private String formatStepDateTime(TripStep step) {
        String date = step.getDate();
        String time = step.getTime();
        if (!TextUtils.isEmpty(date) && !TextUtils.isEmpty(time)) {
            return date + ", " + time;
        }
        if (!TextUtils.isEmpty(date)) {
            return date;
        }
        if (!TextUtils.isEmpty(time)) {
            return time;
        }
        return "Date a definir";
    }

    private void shareTrip() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, "Mon voyage : " + (TextUtils.isEmpty(tripName) ? "Travelin" : tripName));
        startActivity(Intent.createChooser(intent, "Partager le voyage"));
    }

    private void callHotel() {
        if (TextUtils.isEmpty(hotelPhone)) {
            Toast.makeText(this, "Aucun telephone d'hotel enregistre", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + hotelPhone)));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle mapViewBundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapViewBundle);
        }
        mapView.onSaveInstanceState(mapViewBundle);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        renderStepMarkers();
        renderPlanningSteps();
    }

    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
