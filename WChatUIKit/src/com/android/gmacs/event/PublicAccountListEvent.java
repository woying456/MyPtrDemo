package com.android.gmacs.event;

import com.common.gmacs.parse.pubcontact.PublicContactInfo;

import java.util.List;

public class PublicAccountListEvent {
    private List<PublicContactInfo> publicContactInfos;

    public PublicAccountListEvent(List<PublicContactInfo> publicContactInfos) {
        this.publicContactInfos = publicContactInfos;
    }

    public List<PublicContactInfo> getPublicContactInfos() {
        return publicContactInfos;
    }
}
