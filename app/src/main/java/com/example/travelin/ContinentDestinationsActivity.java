package com.example.travelin;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ContinentDestinationsActivity extends AppCompatActivity {
    public static final String EXTRA_CONTINENT_NAME = "continent_name";

    private ExploreRepository repository;
    private ExploreDestinationAdapter adapter;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private LinearLayout errorView;
    private TextView errorText;
    private String continentName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_continent_destinations);

        continentName = getIntent().getStringExtra(EXTRA_CONTINENT_NAME);
        repository = new ExploreRepository(this);

        ImageButton backButton = findViewById(R.id.btn_continent_back);
        TextView title = findViewById(R.id.txt_continent_title);
        progressBar = findViewById(R.id.progress_continent);
        recyclerView = findViewById(R.id.recycler_continent_destinations);
        errorView = findViewById(R.id.layout_continent_error);
        errorText = findViewById(R.id.txt_continent_error);
        Button retryButton = findViewById(R.id.btn_continent_retry);

        backButton.setOnClickListener(view -> finish());
        title.setText(continentName);
        adapter = new ExploreDestinationAdapter(
                this,
                destination -> startActivity(
                        DestinationDetailsActivity.createIntent(this, destination)
                )
        );
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        retryButton.setOnClickListener(view -> loadDestinations());
        loadDestinations();
    }

    private void loadDestinations() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);

        if (getSharedPreferences(ExploreRepository.PREFS_NAME, MODE_PRIVATE)
                .getBoolean(ExploreRepository.KEY_OFFLINE_MODE, false)) {
            Toast.makeText(this, "Mode hors ligne activé", Toast.LENGTH_SHORT).show();
        }

        repository.getExploreDestinations(new ExploreRepository.RepositoryCallback() {
            @Override
            public void onSuccess(List<ExploreDestination> destinations, boolean fromCache) {
                List<ExploreDestination> filtered = new ArrayList<>();
                for (ExploreDestination destination : destinations) {
                    if (continentName != null
                            && continentName.equalsIgnoreCase(destination.getContinent())) {
                        filtered.add(destination);
                    }
                }
                progressBar.setVisibility(View.GONE);
                if (filtered.isEmpty()) {
                    showError("Aucune destination pour ce continent");
                    return;
                }
                adapter.setDestinations(filtered);
                recyclerView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(String message) {
                progressBar.setVisibility(View.GONE);
                showError(message);
            }
        });
    }

    private void showError(String message) {
        recyclerView.setVisibility(View.GONE);
        errorText.setText(message);
        errorView.setVisibility(View.VISIBLE);
    }
}
