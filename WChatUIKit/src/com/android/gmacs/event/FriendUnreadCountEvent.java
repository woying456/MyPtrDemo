package com.android.gmacs.event;

public class FriendUnreadCountEvent {
    private long friendCount;

    public FriendUnreadCountEvent(long friendCount) {
        this.friendCount = friendCount;
    }

    public long getFriendCount() {
        return friendCount;
    }

}
