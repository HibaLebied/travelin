package com.example.travelin;

import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

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
        boolean pastTrip = isPastTrip(trip);
        applyTripImageStyle(holder.tripImage, pastTrip);
        RequestListener<Drawable> styleListener = new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                applyTripImageStyle(holder.tripImage, pastTrip);
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                holder.tripImage.post(() -> applyTripImageStyle(holder.tripImage, pastTrip));
                return false;
            }
        };
        if (!TextUtils.isEmpty(trip.getCoverPhotoPath())) {
            Glide.with(holder.tripImage.getContext())
                    .load(Uri.parse(trip.getCoverPhotoPath()))
                    .placeholder(trip.getImageResId())
                    .error(trip.getImageResId())
                    .centerCrop()
                    .listener(styleListener)
                    .into(holder.tripImage);
        } else {
            Glide.with(holder.tripImage.getContext())
                    .load(trip.getImageResId())
                    .centerCrop()
                    .listener(styleListener)
                    .into(holder.tripImage);
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

    private boolean isPastTrip(Trip trip) {
        return Trip.TYPE_PAST.equals(trip.getTripType())
                || "VOYAGES PASSES".equals(trip.getSection())
                || "PAST TRIPS".equals(trip.getSection());
    }

    private void applyTripImageStyle(ImageView imageView, boolean pastTrip) {
        if (pastTrip) {
            ColorMatrix matrix = new ColorMatrix();
            matrix.setSaturation(0f);
            imageView.setColorFilter(new ColorMatrixColorFilter(matrix));
            imageView.setAlpha(0.78f);
        } else {
            imageView.clearColorFilter();
            imageView.setAlpha(1f);
        }
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
