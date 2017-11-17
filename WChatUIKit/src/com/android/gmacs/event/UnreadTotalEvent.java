package com.android.gmacs.event;

public class UnreadTotalEvent {
    private int total;

    public UnreadTotalEvent(int total) {
        this.total = total;
    }

    public int getTotal() {
        return total;
    }
}
