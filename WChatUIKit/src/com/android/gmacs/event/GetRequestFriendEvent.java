package com.android.gmacs.event;

import com.common.gmacs.parse.talk.Talk;

public class GetRequestFriendEvent {

    private Talk talk;

    public GetRequestFriendEvent(Talk talk) {
        this.talk = talk;
    }

    public Talk getTalk() {
        return talk;
    }

}
