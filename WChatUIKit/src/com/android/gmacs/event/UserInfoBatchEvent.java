package com.android.gmacs.event;

import com.common.gmacs.parse.contact.UserInfo;

import java.util.List;

public class UserInfoBatchEvent {
    private List<UserInfo> userInfoList;

    public UserInfoBatchEvent(List<UserInfo> userInfoList) {
        this.userInfoList = userInfoList;
    }

    public List<UserInfo> getUserInfoList() {
        return userInfoList;
    }
}
