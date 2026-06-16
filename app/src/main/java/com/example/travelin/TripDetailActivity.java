package com.example.travelin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class TripDetailActivity extends AppCompatActivity {
    public static final String EXTRA_TRIP_ID = "extra_trip_id";
    public static final String EXTRA_TRIP_NAME = "extra_trip_name";
    public static final String EXTRA_TRIP_DATES = "extra_trip_dates";
    public static final String EXTRA_TRIP_IMAGE = "extra_trip_image";
    public static final String EXTRA_HOTEL_PHONE = "extra_hotel_phone";

    private long tripId;
    private String tripName;
    private String hotelPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_detail);

        tripId = getIntent().getLongExtra(EXTRA_TRIP_ID, 0);
        tripName = getIntent().getStringExtra(EXTRA_TRIP_NAME);
        String tripDates = getIntent().getStringExtra(EXTRA_TRIP_DATES);
        hotelPhone = getIntent().getStringExtra(EXTRA_HOTEL_PHONE);
        int imageRes = getIntent().getIntExtra(EXTRA_TRIP_IMAGE, R.drawable.travel_beach_bg);

        ImageView heroImage = findViewById(R.id.img_detail_hero);
        TextView titleText = findViewById(R.id.txt_detail_title);
        TextView datesText = findViewById(R.id.txt_detail_dates);
        heroImage.setImageResource(imageRes);
        titleText.setText(TextUtils.isEmpty(tripName) ? "Voyage" : tripName);
        datesText.setText(TextUtils.isEmpty(tripDates) ? "Dates à définir" : tripDates);

        ImageButton backButton = findViewById(R.id.btn_detail_back);
        backButton.setOnClickListener(v -> finish());
        findViewById(R.id.btn_add_step).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddStepActivity.class);
            intent.putExtra(AddStepActivity.EXTRA_TRIP_ID, tripId);
            startActivity(intent);
        });
        findViewById(R.id.btn_share_trip).setOnClickListener(v -> shareTrip());
        findViewById(R.id.btn_alert_trip).setOnClickListener(v ->
                Toast.makeText(this, "Rappel de voyage activé", Toast.LENGTH_SHORT).show());
        findViewById(R.id.card_call_hotel).setOnClickListener(v -> callHotel());
        findViewById(R.id.card_share_sms).setOnClickListener(v -> shareTrip());
    }

    private void shareTrip() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, "Mon voyage : " + (TextUtils.isEmpty(tripName) ? "Travelin" : tripName));
        startActivity(Intent.createChooser(intent, "Partager le voyage"));
    }

    private void callHotel() {
        if (TextUtils.isEmpty(hotelPhone)) {
            Toast.makeText(this, "Aucun téléphone d'hôtel enregistré", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + hotelPhone)));
    }
}
