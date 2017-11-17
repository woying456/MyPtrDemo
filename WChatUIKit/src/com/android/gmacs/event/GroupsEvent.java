package com.android.gmacs.event;

import com.common.gmacs.parse.contact.Group;

import java.util.List;


public class GroupsEvent {
    private List<Group> groups;

    public GroupsEvent(List<Group> groups) {
        this.groups = groups;
    }

    public List<Group> getGroups() {
        return groups;
    }
}
