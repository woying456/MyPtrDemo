package com.android.gmacs.event;

import com.common.gmacs.parse.message.Message;

public class UpdateInsertChatMsgEvent {
    private Message message;

    public UpdateInsertChatMsgEvent(Message message) {
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }
}
