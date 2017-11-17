package com.android.gmacs.event;

public class StarEvent {
    private String userId;
    private int userSource;
    private boolean star;

    public StarEvent(String userId, int userSource, boolean star) {
        this.userId = userId;
        this.userSource = userSource;
        this.star = star;
    }

    public String getUserId() {
        return userId;
    }

    public int getUserSource() {
        return userSource;
    }

    public boolean isStar() {
        return star;
    }
}
