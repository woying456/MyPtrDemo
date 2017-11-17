package com.android.gmacs.event;

import com.common.gmacs.parse.contact.Contact;

import java.util.List;

public class ContactsEvent {
    private List<Contact> contactList;
    private List<Contact> stars;

    public ContactsEvent(List<Contact> contactList, List<Contact> stars) {
        this.contactList = contactList;
        this.stars = stars;
    }

    public List<Contact> getContactList() {
        return contactList;
    }

    public List<Contact> getStars() {
        return stars;
    }
}
