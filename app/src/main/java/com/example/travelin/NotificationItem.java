package com.example.travelin;

public class NotificationItem {
    private final long id;
    private final String section;
    private final String title;
    private final String description;
    private final String date;
    private final int iconResId;
    private boolean unread;
    private final String type;
    private final long relatedId;

    public NotificationItem(String section, String title, String description, String date, int iconResId, boolean unread) {
        this(0, section, title, description, date, iconResId, unread, "", 0);
    }

    public NotificationItem(long id, String section, String title, String description, String date,
                            int iconResId, boolean unread, String type, long relatedId) {
        this.id = id;
        this.section = section;
        this.title = title;
        this.description = description;
        this.date = date;
        this.iconResId = iconResId;
        this.unread = unread;
        this.type = type;
        this.relatedId = relatedId;
    }

    public long getId() {
        return id;
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

    public void setUnread(boolean unread) {
        this.unread = unread;
    }

    public String getType() {
        return type;
    }

    public long getRelatedId() {
        return relatedId;
    }
}
