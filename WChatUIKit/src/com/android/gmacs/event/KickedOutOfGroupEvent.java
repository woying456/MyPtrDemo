package com.android.gmacs.event;

import com.common.gmacs.parse.command.KickedOutOfGroupCommand;

public class KickedOutOfGroupEvent {

    private KickedOutOfGroupCommand command;

    public KickedOutOfGroupEvent(KickedOutOfGroupCommand command) {
        this.command = command;
    }

    public String getOperatorName() {
        return command.getOperatorName();
    }

    public String getOperatorId() {
        return command.getOperatorId();
    }

    public int getOperatorSource() {
        return command.getOperatorSource();
    }

    public String getOperatedGroupId() {
        return command.getOperatedGroupId();
    }

    public int getOperatedGroupSource() {
        return command.getOperatedGroupSource();
    }

}
