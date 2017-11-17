package com.android.gmacs.event;

import com.common.gmacs.parse.message.AcceptFriendMessage;

public class AddContactMsgEvent {

    private String contactId;
    private int contactSource;
    private String msgId;
    private AcceptFriendMessage acceptFriendMessage;

    public AddContactMsgEvent(String contactId, int contactSource, String msgId, AcceptFriendMessage acceptFriendMessage) {
        this.contactId = contactId;
        this.contactSource = contactSource;
        this.msgId = msgId;
        this.acceptFriendMessage = acceptFriendMessage;
    }

    public AcceptFriendMessage getAcceptFriendMessage() {
        return acceptFriendMessage;
    }

    public String getContactId() {
        return contactId;
    }

    public int getContactSource() {
        return contactSource;
    }

    public String getMsgId() {
        return msgId;
    }
}
