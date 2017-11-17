package com.android.gmacs.event;

import java.util.List;

public class WChatAlbumImagesDeletedEvent {

    private List<Long> deletedLocalIdList;
    private String userId;
    private int userSource;

    public WChatAlbumImagesDeletedEvent(List<Long> deletedLocalIdList, String userId, int userSource) {
        this.deletedLocalIdList = deletedLocalIdList;
        this.userId = userId;
        this.userSource = userSource;
    }

    public List<Long> getDeletedLocalIdList() {
        return deletedLocalIdList;
    }

    public String getUserId() {
        return userId;
    }

    public int getUserSource() {
        return userSource;
    }
}
