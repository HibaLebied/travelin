package com.example.travelin;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AddTripActivity extends AppCompatActivity {
    public static final String EXTRA_TRIP_TYPE = "trip_type";
    public static final String EXTRA_PREFILL_DESTINATION = "prefill_destination";
    public static final String EXTRA_PREFILL_COUNTRY = "prefill_country";
    private static final SimpleDateFormat STORAGE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat DISPLAY_FORMAT = new SimpleDateFormat("dd MMMM yyyy", Locale.FRENCH);

    private String tripType;
    private Calendar startCalendar;
    private Calendar endCalendar;
    private TextView startDateText;
    private TextView endDateText;
    private TextView nightsText;
    private LinearLayout fieldsContainer;
    private LinearLayout actionsContainer;
    private EditText destinationEditText;
    private EditText tripNameEditText;
    private EditText hotelNameEditText;
    private EditText hotelAddressEditText;
    private EditText hotelPhoneEditText;
    private EditText notesEditText;
    private ImageView coverImageView;
    private String selectedCoverPhotoUri;
    private TripDao tripDao;
    private final ActivityResultLauncher<String[]> coverPhotoPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) {
                    return;
                }
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                selectedCoverPhotoUri = uri.toString();
                coverImageView.setImageURI(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_trip);

        tripType = getIntent().getStringExtra(EXTRA_TRIP_TYPE);
        if (!Trip.TYPE_CURRENT.equals(tripType) && !Trip.TYPE_PAST.equals(tripType)) {
            tripType = Trip.TYPE_FUTURE;
        }

        tripDao = new TripDao(this);
        bindViews();
        configureDates();
        buildDynamicForm();
        applyPrefilledDestination();
    }

    private void bindViews() {
        ImageButton backButton = findViewById(R.id.btn_back);
        FrameLayout coverPhoto = findViewById(R.id.cover_photo);
        coverImageView = findViewById(R.id.img_cover_photo);
        startDateText = findViewById(R.id.txt_start_date);
        endDateText = findViewById(R.id.txt_end_date);
        nightsText = findViewById(R.id.txt_nights);
        fieldsContainer = findViewById(R.id.container_fields);
        actionsContainer = findViewById(R.id.container_actions);
        MaterialButton saveButton = findViewById(R.id.btn_save_trip);

        backButton.setOnClickListener(v -> finish());
        coverPhoto.setOnClickListener(v -> showPhotoMenu());
        findViewById(R.id.box_start_date).setOnClickListener(v -> showDatePicker(true));
        findViewById(R.id.box_end_date).setOnClickListener(v -> showDatePicker(false));
        saveButton.setOnClickListener(v -> saveTrip());
    }

    private void configureDates() {
        if (Trip.TYPE_CURRENT.equals(tripType)) {
            startCalendar = Calendar.getInstance();
        }
        updateDateCard();
    }

    private void buildDynamicForm() {
        fieldsContainer.removeAllViews();
        actionsContainer.removeAllViews();

        if (Trip.TYPE_CURRENT.equals(tripType)) {
            destinationEditText = addField(R.drawable.ic_field_destination, "Destination actuelle", "Ex. : Marrakech, Maroc", false);
            hotelNameEditText = addField(R.drawable.ic_field_hotel, "Hotel actuel", "Ex. : Hotel Name", false);
            hotelAddressEditText = addField(R.drawable.ic_field_hotel_address, "Adresse de l'hotel", "Ex. : 12 Rue Mohammed V, Marrakech", false);
            hotelPhoneEditText = addField(R.drawable.ic_field_phone, "Telephone de l'hotel", "Ex. : +212 6XX XXX XXX", false);
            notesEditText = addField(R.drawable.ic_field_notes, "Notes", "Ajouter des notes...", true);
            addActionButton(R.drawable.ic_action_add_step, "Ajouter une etape\nmaintenant", v -> saveTripAndOpenStep());
            addActionButton(R.drawable.ic_action_take_photo, "Prendre une photo", v -> openCoverPhotoPicker());
        } else if (Trip.TYPE_PAST.equals(tripType)) {
            destinationEditText = addField(R.drawable.ic_field_destination, "Destination", "Ex. : Paris, France", false);
            tripNameEditText = addField(R.drawable.ic_field_trip_name, "Nom du voyage", "Ex. : Voyage a Paris", false);
            notesEditText = addField(R.drawable.ic_field_notes, "Notes souvenirs", "Decrivez vos souvenirs...", true);
            addActionButton(R.drawable.ic_action_photos_memories, "Ajouter des photos\nsouvenirs", v -> openCoverPhotoPicker());
            addActionButton(R.drawable.ic_action_visited_steps, "Ajouter des etapes\nvisitees", v -> saveTripAndOpenStep());
        } else {
            destinationEditText = addField(R.drawable.ic_field_destination, "Destination", "Ex. : Istanbul, Turquie", false);
            tripNameEditText = addField(R.drawable.ic_field_trip_name, "Nom du voyage", "Ex. : Vacances d'ete 2024", false);
            hotelNameEditText = addField(R.drawable.ic_field_hotel, "Nom de l'hotel", "Ex. : Hotel Name", false);
            hotelAddressEditText = addField(R.drawable.ic_field_hotel_address, "Adresse de l'hotel", "Ex. : 12 Rue Mohammed V, Marrakech", false);
            hotelPhoneEditText = addField(R.drawable.ic_field_phone, "Telephone de l'hotel", "Ex. : +90 532 123 45 67", false);
            notesEditText = addField(R.drawable.ic_field_notes, "Notes", "Ajouter des notes...", true);
            addWideActionButton(R.drawable.ic_add_trip_flight_route_image, "Ajouter des etapes prevues", v -> saveTripAndOpenStep());
        }
    }

    private void applyPrefilledDestination() {
        String destination = getIntent().getStringExtra(EXTRA_PREFILL_DESTINATION);
        if (destinationEditText != null && !TextUtils.isEmpty(destination)) {
            destinationEditText.setText(destination);
            destinationEditText.setSelection(destinationEditText.length());
        }
    }

    private EditText addField(int iconResId, String label, String hint, boolean multiline) {
        View field = LayoutInflater.from(this).inflate(R.layout.item_add_trip_field, fieldsContainer, false);
        ImageView icon = field.findViewById(R.id.img_field_icon);
        TextView labelText = field.findViewById(R.id.txt_field_label);
        EditText editText = field.findViewById(R.id.edit_field_value);
        LinearLayout fieldRoot = field.findViewById(R.id.field_root);
        LinearLayout fieldContent = field.findViewById(R.id.field_content);
        icon.setImageResource(iconResId);
        labelText.setText(label);
        editText.setHint(hint);
        if (multiline) {
            field.getLayoutParams().height = getResources().getDimensionPixelSize(R.dimen.add_trip_notes_height);
            fieldRoot.setGravity(android.view.Gravity.TOP);
            fieldRoot.setPadding(dpToPx(14), dpToPx(16), dpToPx(14), dpToPx(12));
            fieldContent.setGravity(android.view.Gravity.TOP);
            editText.setMinLines(2);
            editText.setMaxLines(3);
            editText.setGravity(android.view.Gravity.TOP);
        }
        fieldsContainer.addView(field);
        return editText;
    }

    private void addActionButton(int iconResId, String text, View.OnClickListener listener) {
        LinearLayout button = new LinearLayout(this);
        button.setBackgroundResource(R.drawable.add_trip_action_background);
        button.setGravity(android.view.Gravity.CENTER_VERTICAL);
        button.setOrientation(LinearLayout.HORIZONTAL);
        button.setPadding(dpToPx(10), 0, dpToPx(10), 0);
        button.setClickable(true);
        button.setFocusable(true);
        button.setOnClickListener(listener);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconResId);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        button.addView(icon, new LinearLayout.LayoutParams(dpToPx(32), dpToPx(32)));

        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(getColor(R.color.primary));
        label.setTextSize(13);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        label.setGravity(android.view.Gravity.CENTER);
        label.setMaxLines(2);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        labelParams.setMargins(dpToPx(6), 0, 0, 0);
        button.addView(label, labelParams);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, getResources().getDimensionPixelSize(R.dimen.add_trip_button_height), 1f);
        int gap = dpToPx(6);
        params.setMargins(actionsContainer.getChildCount() == 0 ? 0 : gap, 0, actionsContainer.getChildCount() == 0 ? gap : 0, 0);
        actionsContainer.addView(button, params);
    }

    private void addWideActionButton(int iconResId, String text, View.OnClickListener listener) {
        LinearLayout button = new LinearLayout(this);
        button.setBackgroundResource(R.drawable.add_trip_wide_action_background);
        button.setGravity(android.view.Gravity.CENTER_VERTICAL);
        button.setOrientation(LinearLayout.HORIZONTAL);
        button.setPadding(dpToPx(18), 0, dpToPx(16), 0);
        button.setClickable(true);
        button.setFocusable(true);
        button.setOnClickListener(listener);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconResId);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        button.addView(icon, new LinearLayout.LayoutParams(dpToPx(34), dpToPx(34)));

        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(getColor(R.color.primary));
        label.setTextSize(15);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        label.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        labelParams.setMargins(dpToPx(10), 0, dpToPx(10), 0);
        button.addView(label, labelParams);

        ImageView arrow = new ImageView(this);
        arrow.setImageResource(R.drawable.ic_add_trip_chevron_right);
        arrow.setColorFilter(getColor(R.color.primary));
        button.addView(arrow, new LinearLayout.LayoutParams(dpToPx(24), dpToPx(24)));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getResources().getDimensionPixelSize(R.dimen.add_trip_button_height));
        actionsContainer.addView(button, params);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void showPhotoMenu() {
        String[] options = {"Galerie", "Prendre une photo", "Annuler"};
        new AlertDialog.Builder(this)
                .setItems(options, (dialog, which) -> {
                    if (which == 2) {
                        dialog.dismiss();
                    } else {
                        openCoverPhotoPicker();
                    }
                })
                .show();
    }

    private void openCoverPhotoPicker() {
        coverPhotoPicker.launch(new String[]{"image/*"});
    }

    private void showDatePicker(boolean isStartDate) {
        Calendar initialCalendar = isStartDate
                ? (startCalendar == null ? Calendar.getInstance() : startCalendar)
                : (endCalendar == null ? Calendar.getInstance() : endCalendar);
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (DatePicker view, int year, int month, int dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth, 0, 0, 0);
                    selected.set(Calendar.MILLISECOND, 0);
                    if (isStartDate) {
                        startCalendar = selected;
                        if (endCalendar != null && endCalendar.before(startCalendar)) {
                            endCalendar = null;
                        }
                    } else {
                        endCalendar = selected;
                    }
                    updateDateCard();
                },
                initialCalendar.get(Calendar.YEAR),
                initialCalendar.get(Calendar.MONTH),
                initialCalendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void updateDateCard() {
        startDateText.setText(startCalendar == null ? "Date debut" : DISPLAY_FORMAT.format(startCalendar.getTime()));
        if (endCalendar == null) {
            endDateText.setText(Trip.TYPE_CURRENT.equals(tripType) ? "Facultatif" : "Date fin");
        } else {
            endDateText.setText(DISPLAY_FORMAT.format(endCalendar.getTime()));
        }

        if (startCalendar != null && endCalendar != null) {
            long diff = endCalendar.getTimeInMillis() - startCalendar.getTimeInMillis();
            long nights = Math.max(0, TimeUnit.MILLISECONDS.toDays(diff));
            nightsText.setText(nights + (nights > 1 ? " nuits" : " nuit"));
            nightsText.setVisibility(View.VISIBLE);
        } else {
            nightsText.setVisibility(View.GONE);
        }
    }

    private void saveTrip() {
        if (!validateRequiredFields()) {
            return;
        }

        long id = insertTripFromForm();
        if (id == -1) {
            Toast.makeText(this, "Impossible d'enregistrer le voyage", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Voyage enregistre", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void saveTripAndOpenStep() {
        if (!validateRequiredFields()) {
            return;
        }

        long id = insertTripFromForm();
        if (id == -1) {
            Toast.makeText(this, "Impossible d'enregistrer le voyage", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, AddStepActivity.class);
        intent.putExtra(AddStepActivity.EXTRA_TRIP_ID, id);
        startActivity(intent);
        finish();
    }

    private long insertTripFromForm() {
        Trip trip = new Trip();
        trip.setUserId(getConnectedUserId());
        trip.setTripType(tripType);
        trip.setDestination(valueOf(destinationEditText));
        trip.setName(valueOf(tripNameEditText));
        if (TextUtils.isEmpty(trip.getName())) {
            trip.setName(trip.getDestination());
        }
        trip.setStartDate(startCalendar == null ? null : STORAGE_FORMAT.format(startCalendar.getTime()));
        trip.setEndDate(endCalendar == null ? null : STORAGE_FORMAT.format(endCalendar.getTime()));
        trip.setHotelName(valueOf(hotelNameEditText));
        trip.setHotelAddress(valueOf(hotelAddressEditText));
        trip.setHotelPhone(valueOf(hotelPhoneEditText));
        trip.setNotes(valueOf(notesEditText));
        trip.setCoverPhotoPath(selectedCoverPhotoUri);
        trip.setCreatedAt(String.valueOf(new Date().getTime()));

        return tripDao.insertTrip(trip);
    }

    private boolean validateRequiredFields() {
        if (isEmpty(destinationEditText)) {
            destinationEditText.setError("Champ obligatoire");
            return false;
        }

        if ((Trip.TYPE_FUTURE.equals(tripType) || Trip.TYPE_PAST.equals(tripType)) && isEmpty(tripNameEditText)) {
            tripNameEditText.setError("Champ obligatoire");
            return false;
        }

        if (!Trip.TYPE_CURRENT.equals(tripType) && startCalendar == null) {
            Toast.makeText(this, "Choisissez la date de debut", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (Trip.TYPE_PAST.equals(tripType) && endCalendar == null) {
            Toast.makeText(this, "Choisissez la date de fin", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (startCalendar != null && endCalendar != null && endCalendar.before(startCalendar)) {
            Toast.makeText(this, "La date de fin doit etre apres la date de debut", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private boolean isEmpty(EditText editText) {
        return editText == null || TextUtils.isEmpty(editText.getText().toString().trim());
    }

    private String valueOf(EditText editText) {
        return editText == null ? null : editText.getText().toString().trim();
    }

    private String getConnectedUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user == null ? "guest" : user.getUid();
    }
}
