package com.example.travelin;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

public class DestinationDetailsActivity extends AppCompatActivity {
    public static final String EXTRA_DESTINATION_ID = "destination_id";
    public static final String EXTRA_DESTINATION_NAME = "destination_name";
    public static final String EXTRA_COUNTRY = "country";
    public static final String EXTRA_PLACES_COUNT = "places_count";
    public static final String EXTRA_IMAGE_URL = "image_url";
    public static final String EXTRA_FLAG_URL = "flag_url";
    public static final String EXTRA_DESCRIPTION = "description";
    public static final String EXTRA_THEME = "theme";
    public static final String EXTRA_CONTINENT = "continent";

    public static Intent createIntent(Context context, ExploreDestination destination) {
        Intent intent = new Intent(context, DestinationDetailsActivity.class);
        intent.putExtra(EXTRA_DESTINATION_ID, destination.getId());
        intent.putExtra(EXTRA_DESTINATION_NAME, destination.getName());
        intent.putExtra(EXTRA_COUNTRY, destination.getCountry());
        intent.putExtra(EXTRA_PLACES_COUNT, destination.getPlacesCount());
        intent.putExtra(EXTRA_IMAGE_URL, destination.getImageUrl());
        intent.putExtra(EXTRA_FLAG_URL, destination.getFlagUrl());
        intent.putExtra(EXTRA_DESCRIPTION, destination.getDescription());
        intent.putExtra(EXTRA_THEME, destination.getTheme());
        intent.putExtra(EXTRA_CONTINENT, destination.getContinent());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_destination_details);

        Intent intent = getIntent();
        String name = intent.getStringExtra(EXTRA_DESTINATION_NAME);
        String country = intent.getStringExtra(EXTRA_COUNTRY);
        String imageUrl = intent.getStringExtra(EXTRA_IMAGE_URL);
        String flagUrl = intent.getStringExtra(EXTRA_FLAG_URL);
        String description = intent.getStringExtra(EXTRA_DESCRIPTION);
        String theme = intent.getStringExtra(EXTRA_THEME);
        String continent = intent.getStringExtra(EXTRA_CONTINENT);
        int placesCount = intent.getIntExtra(EXTRA_PLACES_COUNT, 0);

        ImageButton backButton = findViewById(R.id.btn_destination_back);
        ImageView image = findViewById(R.id.img_destination_cover);
        ImageView flag = findViewById(R.id.img_destination_flag);
        TextView nameText = findViewById(R.id.txt_destination_detail_name);
        TextView countryText = findViewById(R.id.txt_destination_detail_country);
        TextView placesText = findViewById(R.id.txt_destination_detail_places);
        TextView descriptionText = findViewById(R.id.txt_destination_description);
        TextView themeText = findViewById(R.id.txt_destination_theme);
        TextView continentText = findViewById(R.id.txt_destination_continent);
        MaterialButton planButton = findViewById(R.id.btn_plan_destination);

        backButton.setOnClickListener(view -> finish());
        nameText.setText(name);
        countryText.setText(country);
        placesText.setText(placesCount + " lieux");
        descriptionText.setText(description);
        themeText.setText("Thème : " + safeText(theme));
        continentText.setText("Continent : " + safeText(continent));

        Glide.with(this)
                .load(TextUtils.isEmpty(imageUrl) ? null : imageUrl)
                .placeholder(R.drawable.placeholder_destination)
                .error(R.drawable.placeholder_destination)
                .centerCrop()
                .into(image);
        Glide.with(this)
                .load(TextUtils.isEmpty(flagUrl) ? null : flagUrl)
                .placeholder(R.drawable.placeholder_flag)
                .error(R.drawable.placeholder_flag)
                .circleCrop()
                .into(flag);

        planButton.setOnClickListener(view -> {
            Intent addTripIntent = new Intent(this, AddTripActivity.class);
            addTripIntent.putExtra(AddTripActivity.EXTRA_TRIP_TYPE, Trip.TYPE_FUTURE);
            addTripIntent.putExtra(AddTripActivity.EXTRA_PREFILL_DESTINATION, name);
            addTripIntent.putExtra(AddTripActivity.EXTRA_PREFILL_COUNTRY, country);
            startActivity(addTripIntent);
        });
    }

    private String safeText(String value) {
        return TextUtils.isEmpty(value) ? "Non précisé" : value;
    }
}
