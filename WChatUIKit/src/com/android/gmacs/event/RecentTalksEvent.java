package com.android.gmacs.event;

import com.common.gmacs.parse.talk.Talk;

import java.util.List;

public class RecentTalksEvent {
    private List<Talk> mTalks;

    public RecentTalksEvent(List<Talk> mTalks) {
        this.mTalks = mTalks;
    }

    public List<Talk> getTalks() {
        return mTalks;
    }
}
