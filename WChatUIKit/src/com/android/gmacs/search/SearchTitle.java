package com.android.gmacs.search;

import com.common.gmacs.parse.search.Searchable;

public class SearchTitle implements Searchable {
    private int searchType;
    public SearchTitle(int searchType) {
        this.searchType = searchType;
    }

    public int getSearchType() {
        return searchType;
    }
}
