package com.android.gmacs.event;

import com.common.gmacs.parse.contact.Remark;

public class RemarkEvent {
    private String userId;
    private int userSource;
    private Remark remark;

    public RemarkEvent(String userId, int userSource, Remark remark) {
        this.userId = userId;
        this.userSource = userSource;
        this.remark = remark;
    }

    public int getUserSource() {
        return userSource;
    }

    public String getUserId() {
        return userId;
    }

    public Remark getRemark() {
        return remark;
    }

}
