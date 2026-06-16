package com.example.travelin;

import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Outline;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripViewHolder> {
    private final List<Trip> trips;
    private final OnTripClickListener listener;

    public TripAdapter(List<Trip> trips) {
        this(trips, null);
    }

    public TripAdapter(List<Trip> trips, OnTripClickListener listener) {
        this.trips = new ArrayList<>(trips);
        this.listener = listener;
    }

    public void setTrips(List<Trip> newTrips) {
        trips.clear();
        trips.addAll(newTrips);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trip, parent, false);
        return new TripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        Trip trip = trips.get(position);
        holder.sectionText.setVisibility(trip.getSection() == null ? View.GONE : View.VISIBLE);
        if (trip.getSection() != null) {
            holder.sectionText.setText(trip.getSection());
        }
        holder.nameText.setText(trip.getName());
        holder.metaText.setText(trip.getDates() + "    " + trip.getLocations());
        holder.tripImage.setImageResource(trip.getImageResId());
        if ("VOYAGES PASSES".equals(trip.getSection()) || "PAST TRIPS".equals(trip.getSection())) {
            ColorMatrix matrix = new ColorMatrix();
            matrix.setSaturation(0f);
            holder.tripImage.setColorFilter(new ColorMatrixColorFilter(matrix));
            holder.tripImage.setAlpha(0.78f);
        } else {
            holder.tripImage.clearColorFilter();
            holder.tripImage.setAlpha(1f);
        }
        holder.tripCard.setClipToOutline(true);
        holder.tripCard.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                float radius = view.getResources().getDisplayMetrics().density * 18;
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });
        holder.tripCard.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTripClick(trip);
            }
        });
    }

    @Override
    public int getItemCount() {
        return trips.size();
    }

    static class TripViewHolder extends RecyclerView.ViewHolder {
        TextView sectionText;
        FrameLayout tripCard;
        ImageView tripImage;
        TextView nameText;
        TextView metaText;

        TripViewHolder(@NonNull View itemView) {
            super(itemView);
            sectionText = itemView.findViewById(R.id.txt_section);
            tripCard = itemView.findViewById(R.id.trip_card);
            tripImage = itemView.findViewById(R.id.img_trip);
            nameText = itemView.findViewById(R.id.txt_trip_name);
            metaText = itemView.findViewById(R.id.txt_trip_meta);
        }
    }

    public interface OnTripClickListener {
        void onTripClick(Trip trip);
    }
}
