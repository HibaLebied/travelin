package com.example.travelin;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class ExploreDestinationAdapter
        extends RecyclerView.Adapter<ExploreDestinationAdapter.DestinationViewHolder> {

    private final Context context;
    private final OnDestinationClickListener listener;
    private final List<ExploreDestination> destinations = new ArrayList<>();

    public ExploreDestinationAdapter(
            Context context,
            OnDestinationClickListener listener
    ) {
        this.context = context;
        this.listener = listener;
    }

    public void setDestinations(List<ExploreDestination> values) {
        destinations.clear();
        if (values != null) {
            destinations.addAll(values);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DestinationViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_explore_destination, parent, false);
        return new DestinationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull DestinationViewHolder holder,
            int position
    ) {
        ExploreDestination destination = destinations.get(position);
        holder.name.setText(destination.getName());
        holder.country.setText(destination.getCountry());
        holder.places.setText(holder.itemView.getContext().getString(R.string.places_count, destination.getPlacesCount()));

        Glide.with(context)
                .load(TextUtils.isEmpty(destination.getImageUrl())
                        ? null
                        : destination.getImageUrl())
                .placeholder(R.drawable.placeholder_destination)
                .error(R.drawable.placeholder_destination)
                .centerCrop()
                .into(holder.image);

        Glide.with(context)
                .load(TextUtils.isEmpty(destination.getFlagUrl())
                        ? null
                        : destination.getFlagUrl())
                .placeholder(R.drawable.placeholder_flag)
                .error(R.drawable.placeholder_flag)
                .circleCrop()
                .into(holder.flag);

        holder.itemView.setOnClickListener(view -> listener.onDestinationClick(destination));
    }

    @Override
    public int getItemCount() {
        return destinations.size();
    }

    static class DestinationViewHolder extends RecyclerView.ViewHolder {
        final ImageView image;
        final ImageView flag;
        final TextView name;
        final TextView country;
        final TextView places;

        DestinationViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.img_destination);
            flag = itemView.findViewById(R.id.img_flag);
            name = itemView.findViewById(R.id.txt_destination_name);
            country = itemView.findViewById(R.id.txt_destination_country);
            places = itemView.findViewById(R.id.txt_destination_places);
        }
    }

    public interface OnDestinationClickListener {
        void onDestinationClick(ExploreDestination destination);
    }
}
