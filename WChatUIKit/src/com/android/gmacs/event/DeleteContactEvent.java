package com.android.gmacs.event;

public class DeleteContactEvent {
    private String userId;
    private int userSource;

    public DeleteContactEvent(String userId, int userSource) {
        this.userId = userId;
        this.userSource = userSource;
    }

    public String getUserId() {
        return userId;
    }

    public int getUserSource() {
        return userSource;
    }
}
