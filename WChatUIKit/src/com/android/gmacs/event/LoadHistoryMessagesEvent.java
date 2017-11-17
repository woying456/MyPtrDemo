package com.android.gmacs.event;

import com.common.gmacs.parse.message.Message;

import java.util.List;

public class LoadHistoryMessagesEvent {

    private List<Message> messages;

    public LoadHistoryMessagesEvent(List<Message> messages) {
        this.messages = messages;
    }

    public List<Message> getMessages() {
        return messages;
    }

}
