package com.android.gmacs.emoji;

import android.text.Spannable;

import java.util.List;

public interface IEmojiParser<T> {
    void replaceAllEmoji(Spannable str, int dp);
    List<List<T>> getEmojiPages();
}
