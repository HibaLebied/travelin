package com.example.travelin;

import android.app.DatePickerDialog;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddStepActivity extends AppCompatActivity {
    public static final String EXTRA_TRIP_ID = "extra_trip_id";
    public static final String EXTRA_STEP_ID = "extra_step_id";
    public static final String EXTRA_STEP_NAME = "extra_step_name";
    public static final String EXTRA_STEP_DESCRIPTION = "extra_step_description";
    public static final String EXTRA_STEP_DATE = "extra_step_date";
    public static final String EXTRA_STEP_TIME = "extra_step_time";
    public static final String EXTRA_STEP_LATITUDE = "extra_step_latitude";
    public static final String EXTRA_STEP_LONGITUDE = "extra_step_longitude";
    private static final String MAP_VIEW_BUNDLE_KEY = "map_view_bundle";
    private static final LatLng DEFAULT_MAP_LOCATION = new LatLng(48.8584, 2.2945);
    private static final float DEFAULT_MAP_ZOOM = 12f;
    private static final float SELECTED_MAP_ZOOM = 15f;
    private static final int MIN_LOCATION_QUERY_LENGTH = 3;
    private static final int MAX_GEOCODER_RESULTS = 5;
    private static final long LOCATION_SEARCH_DELAY_MS = 700L;

    private AutoCompleteTextView locationInput;
    private EditText descriptionInput;
    private EditText dateInput;
    private EditText timeInput;
    private TextView photoTitleText;
    private MapView mapView;
    private View mapHint;
    private GoogleMap googleMap;
    private LatLng selectedLatLng;
    private ArrayAdapter<String> locationSuggestionsAdapter;
    private final List<Address> locationSuggestions = new ArrayList<>();
    private final ExecutorService geocodingExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private int locationSearchRequestId = 0;
    private boolean isUpdatingLocationText = false;
    private final List<Uri> selectedPhotoUris = new ArrayList<>();
    private Uri pendingCameraPhotoUri;
    private long tripId;
    private long stepId;
    private final Runnable delayedLocationSearch = () ->
            searchLocationSuggestions(locationInput.getText().toString().trim(), false);
    private final ActivityResultLauncher<String[]> photoPicker =
            registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(), uris -> {
                if (uris == null || uris.isEmpty()) {
                    return;
                }
                for (Uri uri : uris) {
                    if (!selectedPhotoUris.contains(uri)) {
                        selectedPhotoUris.add(uri);
                    }
                    try {
                        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException ignored) {
                    }
                }
                updatePhotoSummary();
            });
    private final ActivityResultLauncher<Uri> cameraCapture =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && pendingCameraPhotoUri != null) {
                    selectedPhotoUris.add(pendingCameraPhotoUri);
                    updatePhotoSummary();
                } else {
                    pendingCameraPhotoUri = null;
                    Toast.makeText(this, "Photo non enregistree", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_step);

        tripId = getIntent().getLongExtra(EXTRA_TRIP_ID, 0);
        stepId = getIntent().getLongExtra(EXTRA_STEP_ID, 0);
        locationInput = findViewById(R.id.input_location_name);
        descriptionInput = findViewById(R.id.input_description);
        dateInput = findViewById(R.id.input_date);
        timeInput = findViewById(R.id.input_time);
        photoTitleText = findViewById(R.id.txt_upload_photos);
        mapView = findViewById(R.id.map_view);
        mapHint = findViewById(R.id.map_hint);
        locationSuggestionsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        locationInput.setAdapter(locationSuggestionsAdapter);
        locationInput.setThreshold(MIN_LOCATION_QUERY_LENGTH);

        ImageButton backButton = findViewById(R.id.btn_add_step_back);
        backButton.setOnClickListener(v -> finish());

        dateInput.setFocusable(false);
        dateInput.setOnClickListener(v -> showDatePicker());
        timeInput.setFocusable(false);
        timeInput.setOnClickListener(v -> showTimePicker());
        bindStepForEditing();
        locationInput.setOnEditorActionListener((v, actionId, event) -> {
            boolean isDoneAction = actionId == EditorInfo.IME_ACTION_DONE;
            boolean isEnterKey = event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_UP;
            if (isDoneAction || isEnterKey) {
                geocodeLocationFromInput();
                return true;
            }
            return false;
        });
        locationInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isUpdatingLocationText) {
                    return;
                }
                mainHandler.removeCallbacks(delayedLocationSearch);
                String query = s.toString().trim();
                if (query.length() < MIN_LOCATION_QUERY_LENGTH) {
                    clearLocationSuggestions();
                    return;
                }
                mainHandler.postDelayed(delayedLocationSearch, LOCATION_SEARCH_DELAY_MS);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        locationInput.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < locationSuggestions.size()) {
                Address address = locationSuggestions.get(position);
                String displayName = getAddressDisplayName(address, locationInput.getText().toString().trim());
                selectAddress(address, displayName);
            }
        });

        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
        }
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(map -> {
            googleMap = map;
            googleMap.getUiSettings().setZoomControlsEnabled(true);
            googleMap.getUiSettings().setMapToolbarEnabled(false);
            if (selectedLatLng == null) {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_MAP_LOCATION, DEFAULT_MAP_ZOOM));
            } else {
                selectMapLocation(selectedLatLng, locationInput.getText().toString().trim(), false);
            }
            googleMap.setOnMapClickListener(this::selectMapLocation);
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
        findViewById(R.id.photo_upload_card).setOnClickListener(v -> showPhotoOptions());
        findViewById(R.id.btn_save_step).setOnClickListener(v -> saveStep());
    }

    private void bindStepForEditing() {
        if (stepId <= 0) {
            return;
        }

        String name = getIntent().getStringExtra(EXTRA_STEP_NAME);
        String description = getIntent().getStringExtra(EXTRA_STEP_DESCRIPTION);
        String date = getIntent().getStringExtra(EXTRA_STEP_DATE);
        String time = getIntent().getStringExtra(EXTRA_STEP_TIME);
        double latitude = getIntent().getDoubleExtra(EXTRA_STEP_LATITUDE, Double.NaN);
        double longitude = getIntent().getDoubleExtra(EXTRA_STEP_LONGITUDE, Double.NaN);

        isUpdatingLocationText = true;
        locationInput.setText(TextUtils.isEmpty(name) ? "" : name);
        locationInput.setSelection(locationInput.getText().length());
        isUpdatingLocationText = false;
        descriptionInput.setText(TextUtils.isEmpty(description) ? "" : description);
        dateInput.setText(TextUtils.isEmpty(date) ? "" : date);
        timeInput.setText(TextUtils.isEmpty(time) ? "" : time);

        if (!Double.isNaN(latitude) && !Double.isNaN(longitude)) {
            selectedLatLng = new LatLng(latitude, longitude);
        }
    }

    private void geocodeLocationFromInput() {
        String query = locationInput.getText().toString().trim();
        if (TextUtils.isEmpty(query)) {
            locationInput.setError("Entrez un nom de lieu");
            return;
        }

        if (googleMap == null) {
            Toast.makeText(this, "La carte n'est pas encore prete", Toast.LENGTH_SHORT).show();
            return;
        }

        searchLocationSuggestions(query, true);
    }

    private void searchLocationSuggestions(String query, boolean autoSelectFirstResult) {
        if (query.length() < MIN_LOCATION_QUERY_LENGTH) {
            return;
        }

        int requestId = ++locationSearchRequestId;
        geocodingExecutor.execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocationName(query, MAX_GEOCODER_RESULTS);
                mainHandler.post(() -> {
                    if (requestId == locationSearchRequestId) {
                        handleGeocodingResult(query, addresses, autoSelectFirstResult);
                    }
                });
            } catch (IOException e) {
                mainHandler.post(() -> {
                    if (requestId == locationSearchRequestId) {
                        Toast.makeText(this, "Erreur reseau pendant la recherche du lieu", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (IllegalArgumentException e) {
                mainHandler.post(() -> {
                    if (requestId == locationSearchRequestId) {
                        locationInput.setError("Nom du lieu invalide");
                    }
                });
            }
        });
    }

    private void handleGeocodingResult(String query, List<Address> addresses, boolean autoSelectFirstResult) {
        if (addresses == null || addresses.isEmpty()) {
            clearLocationSuggestions();
            if (autoSelectFirstResult) {
                Toast.makeText(this, "Aucun lieu trouve pour : " + query, Toast.LENGTH_LONG).show();
            }
            return;
        }

        locationSuggestions.clear();
        locationSuggestions.addAll(addresses);
        locationSuggestionsAdapter.clear();
        for (Address address : addresses) {
            locationSuggestionsAdapter.add(getAddressDisplayName(address, query));
        }
        locationSuggestionsAdapter.notifyDataSetChanged();
        if (locationInput.hasFocus() && !locationInput.isPopupShowing()) {
            locationInput.showDropDown();
        }

        if (autoSelectFirstResult) {
            Address address = addresses.get(0);
            selectAddress(address, getAddressDisplayName(address, query));
        }
    }

    private void selectAddress(Address address, String displayName) {
        LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
        selectMapLocation(latLng, displayName, true);
    }

    private String getAddressDisplayName(Address address, String fallback) {
        String displayName = address.getAddressLine(0);
        return TextUtils.isEmpty(displayName) ? fallback : displayName;
    }

    private void clearLocationSuggestions() {
        locationSuggestions.clear();
        locationSuggestionsAdapter.clear();
        locationSuggestionsAdapter.notifyDataSetChanged();
    }

    private void selectMapLocation(LatLng latLng) {
        selectMapLocation(latLng, "Lieu choisi", false);
    }

    private void selectMapLocation(LatLng latLng, String markerTitle, boolean updateLocationText) {
        selectedLatLng = latLng;
        mapHint.setVisibility(View.GONE);
        googleMap.clear();
        googleMap.addMarker(new MarkerOptions().position(latLng).title(markerTitle));
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, SELECTED_MAP_ZOOM));

        if (updateLocationText) {
            isUpdatingLocationText = true;
            locationInput.setText(markerTitle);
            locationInput.setSelection(locationInput.getText().length());
            locationInput.dismissDropDown();
            isUpdatingLocationText = false;
        } else if (TextUtils.isEmpty(locationInput.getText().toString().trim())) {
            locationInput.setText(String.format(Locale.US, "%.5f, %.5f", latLng.latitude, latLng.longitude));
        }
    }

    private void showPhotoOptions() {
        new AlertDialog.Builder(this)
                .setTitle("Ajouter des photos")
                .setItems(new CharSequence[]{"Importer des photos", "Prendre une photo"}, (dialog, which) -> {
                    if (which == 0) {
                        openPhotoPicker();
                    } else {
                        openCamera();
                    }
                })
                .show();
    }

    private void openPhotoPicker() {
        photoPicker.launch(new String[]{"image/*"});
    }

    private void openCamera() {
        try {
            File photoFile = createCameraPhotoFile();
            pendingCameraPhotoUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    photoFile
            );
            cameraCapture.launch(pendingCameraPhotoUri);
        } catch (IOException e) {
            Toast.makeText(this, "Impossible de preparer la camera", Toast.LENGTH_SHORT).show();
        }
    }

    private File createCameraPhotoFile() throws IOException {
        File photosDir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "step_photos");
        if (!photosDir.exists() && !photosDir.mkdirs()) {
            throw new IOException("Cannot create photo directory");
        }
        return File.createTempFile("step_photo_", ".jpg", photosDir);
    }

    private void updatePhotoSummary() {
        int count = selectedPhotoUris.size();
        photoTitleText.setText(count == 1 ? "1 photo selectionnee" : count + " photos selectionnees");
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) ->
                dateInput.setText(String.format(Locale.FRANCE, "%02d/%02d/%04d", dayOfMonth, month + 1, year)),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        new TimePickerDialog(this, (view, hourOfDay, minute) ->
                timeInput.setText(String.format(Locale.FRANCE, "%02d:%02d", hourOfDay, minute)),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true).show();
    }

    private void saveStep() {
        String location = locationInput.getText().toString().trim();
        if (TextUtils.isEmpty(location)) {
            locationInput.setError("Lieu obligatoire");
            return;
        }
        if (selectedLatLng == null) {
            Toast.makeText(this, "Selectionnez un lieu sur la carte", Toast.LENGTH_LONG).show();
            return;
        }

        if (tripId <= 0) {
            Toast.makeText(this, "Ce voyage doit etre enregistre avant d'ajouter une etape", Toast.LENGTH_LONG).show();
            return;
        }

        TripDao tripDao = new TripDao(this);
        long savedStepId;
        if (stepId > 0) {
            int updatedRows = tripDao.updateStep(
                    stepId,
                    location,
                    descriptionInput.getText().toString().trim(),
                    dateInput.getText().toString().trim(),
                    timeInput.getText().toString().trim(),
                    selectedLatLng.latitude,
                    selectedLatLng.longitude
            );
            if (updatedRows <= 0) {
                Toast.makeText(this, "Impossible de modifier l'etape", Toast.LENGTH_SHORT).show();
                return;
            }
            savedStepId = stepId;
        } else {
            savedStepId = tripDao.insertStep(
                    tripId,
                    location,
                    descriptionInput.getText().toString().trim(),
                    dateInput.getText().toString().trim(),
                    timeInput.getText().toString().trim(),
                    selectedLatLng.latitude,
                    selectedLatLng.longitude
            );
            if (savedStepId == -1) {
                Toast.makeText(this, "Impossible d'enregistrer l'etape", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        for (Uri uri : selectedPhotoUris) {
            tripDao.insertStepPhoto(savedStepId, uri.toString());
        }

        Toast.makeText(this, stepId > 0 ? "Etape modifiee" : "Etape enregistree", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
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
    }

    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mapView.onDestroy();
        mainHandler.removeCallbacks(delayedLocationSearch);
        geocodingExecutor.shutdownNow();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
