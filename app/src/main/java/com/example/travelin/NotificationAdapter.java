package com.example.travelin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {
    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationItem item);
    }

    private final List<NotificationItem> notifications;
    private final OnNotificationClickListener listener;

    public NotificationAdapter(List<NotificationItem> notifications, OnNotificationClickListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationItem item = notifications.get(position);
        holder.section.setVisibility(item.getSection() == null ? View.GONE : View.VISIBLE);
        holder.section.setText(item.getSection());
        holder.title.setText(item.getTitle());
        holder.description.setText(item.getDescription());
        holder.date.setText(item.getDate());
        holder.icon.setImageResource(item.getIconResId());
        holder.unreadDot.setVisibility(item.isUnread() ? View.VISIBLE : View.INVISIBLE);
        holder.itemView.setOnClickListener(v -> listener.onNotificationClick(item));
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        final TextView section;
        final ImageView icon;
        final TextView title;
        final TextView description;
        final TextView date;
        final View unreadDot;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            section = itemView.findViewById(R.id.txt_notification_section);
            icon = itemView.findViewById(R.id.img_notification_icon);
            title = itemView.findViewById(R.id.txt_notification_title);
            description = itemView.findViewById(R.id.txt_notification_description);
            date = itemView.findViewById(R.id.txt_notification_date);
            unreadDot = itemView.findViewById(R.id.notification_unread_dot);
        }
    }
}
