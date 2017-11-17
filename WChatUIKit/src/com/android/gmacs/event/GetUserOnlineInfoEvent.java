package com.android.gmacs.event;

import com.common.gmacs.parse.contact.UserOnlineInfo;


public class GetUserOnlineInfoEvent {
    private String userId;
    private int userSource;
    private UserOnlineInfo userOnlineInfo;

    public GetUserOnlineInfoEvent(String userId, int userSource, UserOnlineInfo userOnlineInfo) {
        this.userId = userId;
        this.userSource = userSource;
        this.userOnlineInfo = userOnlineInfo;
    }

    public String getUserId() {
        return userId;
    }

    public int getUserSource() {
        return userSource;
    }

    public UserOnlineInfo getUserOnlineInfo() {
        return userOnlineInfo;
    }
}
