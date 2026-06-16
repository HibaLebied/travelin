package com.example.travelin;

public class NotificationItem {
    private final String section;
    private final String title;
    private final String description;
    private final String date;
    private final int iconResId;
    private final boolean unread;

    public NotificationItem(String section, String title, String description, String date, int iconResId, boolean unread) {
        this.section = section;
        this.title = title;
        this.description = description;
        this.date = date;
        this.iconResId = iconResId;
        this.unread = unread;
    }

    public String getSection() {
        return section;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getDate() {
        return date;
    }

    public int getIconResId() {
        return iconResId;
    }

    public boolean isUnread() {
        return unread;
    }
}
