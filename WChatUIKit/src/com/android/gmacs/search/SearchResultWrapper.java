package com.android.gmacs.search;

import com.common.gmacs.parse.contact.Contact;
import com.common.gmacs.parse.contact.Group;
import com.common.gmacs.parse.search.SearchResult;
import com.common.gmacs.parse.search.SearchedTalk;

import java.util.ArrayList;
import java.util.Arrays;

public class SearchResultWrapper {
    public ArrayList<SearchBean> groups;
    public ArrayList<SearchBean> contacts;
    public ArrayList<SearchBean> searchedTalks;
    public String[] keywords;

    public SearchResultWrapper(String[] keywords, SearchResult searchResult) {
        groups = new ArrayList<>();
        contacts = new ArrayList<>();
        searchedTalks = new ArrayList<>();
        if (searchResult != null) {
            for (Group group : searchResult.groups) {
                SearchBean searchBean = SearchBean.generateSearchBean(group, keywords);
                if (searchBean != null) {
                    groups.add(searchBean);
                }
            }
            for (Contact contact : searchResult.contacts) {
                SearchBean searchBean = SearchBean.generateSearchBean(contact, keywords);
                if (searchBean != null) {
                    contacts.add(searchBean);
                }
            }
            for (SearchedTalk s : searchResult.searchedTalks) {
                SearchBean searchBean = SearchBean.generateSearchBean(s, keywords);
                if (searchBean != null) {
                    searchedTalks.add(searchBean);
                }
            }
        }
        this.keywords = keywords;
    }

    @Override
    public String toString() {
        return "SearchResultWrapper{" +
                "groups=" + groups +
                ", contacts=" + contacts +
                ", searchedTalks=" + searchedTalks +
                ", keywords=" + Arrays.toString(keywords) +
                '}';
    }
}
