package com.android.gmacs.search;

import com.common.gmacs.parse.search.Searchable;

public class SearchDetailEntry implements Searchable {
    private int searchType;

    public SearchDetailEntry(int searchType) {
        this.searchType = searchType;
    }

    public int getSearchType() {
        return searchType;
    }
}
