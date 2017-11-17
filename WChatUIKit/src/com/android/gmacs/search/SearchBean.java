package com.android.gmacs.search;

import android.text.SpannableStringBuilder;
import android.text.TextUtils;

import com.common.gmacs.core.ClientManager;
import com.common.gmacs.parse.contact.UserInfo;
import com.common.gmacs.parse.message.Message;
import com.common.gmacs.parse.search.Searchable;
import com.common.gmacs.parse.search.SearchedTalk;

import java.util.ArrayList;

public class SearchBean {
    Searchable searchable;
    SpannableStringBuilder hitSpannable;
    String hitWord;
    int hitDimension;
    String formattedTime;
    String senderName;

    private SearchBean(Searchable searchable, String hitWord, int hitDimension) {
        this.searchable = searchable;
        this.hitWord = hitWord;
        this.hitDimension = hitDimension;
    }

    public static SearchBean generateSearchBean(Searchable searchable, final String[] keywords) {
        SearchBean searchBean = null;
        if (searchable instanceof SearchDetailEntry) {
            String text = null;
            switch (((SearchDetailEntry) searchable).getSearchType()) {
                case ClientManager.GLOBAL_SEARCHTYPE_CONTACT:
                    text = "查看更多联系人";
                    break;
                case ClientManager.GLOBAL_SEARCHTYPE_GROUP:
                    text = "查看更多群聊";
                    break;
                case ClientManager.GLOBAL_SEARCHTYPE_HISTORY:
                    text = "查看更多聊天记录";
                    break;
            }
            if (text != null) {
                searchBean = new SearchBean(searchable, text, -1);
            }
        } else if (searchable instanceof SearchTitle) {
            String text = null;
            switch (((SearchTitle) searchable).getSearchType()) {
                case ClientManager.GLOBAL_SEARCHTYPE_CONTACT:
                    text = "联系人";
                    break;
                case ClientManager.GLOBAL_SEARCHTYPE_GROUP:
                    text = "群聊";
                    break;
                case ClientManager.GLOBAL_SEARCHTYPE_HISTORY:
                    text = "聊天记录";
                    break;
            }
            if (text != null) {
                searchBean = new SearchBean(searchable, text, -1);
            }
        } else if (searchable instanceof Message) {
            String text = ((Message) searchable).getMsgContent().getPlainText();
            if (containAllKeywords(text, keywords)) {
                searchBean = new SearchBean(searchable, text, 1);
            }
        } else if (searchable instanceof SearchedTalk) {
            String hitWord = null;
            int hitDimension = -1;
            ArrayList<Message> empty = new ArrayList<>();
            for (Message message : ((SearchedTalk) searchable).getMessageList()) {
                SearchBean bean = SearchBean.generateSearchBean(message, keywords);
                if (bean == null) {
                    empty.add(message);
                }
            }
            ((SearchedTalk) searchable).getMessageList().removeAll(empty);
            if (((SearchedTalk) searchable).getMessageList().size() == 1) {
                SearchBean bean = generateSearchBean(((SearchedTalk) searchable).getMessageList().get(0), keywords);
                if (bean != null) {
                    hitWord = bean.getHitWord();
                    hitDimension = 2;
                }
            } else if (((SearchedTalk) searchable).getMessageList().size() > 1) {
                hitDimension = 1;
                hitWord = String.valueOf(((SearchedTalk) searchable).getMessageList().size());
            }
            if (hitWord != null) {
                searchBean = new SearchBean(searchable, hitWord, hitDimension);
            }
        } else if (searchable instanceof UserInfo) {
            String hitWord = null;
            int hitDimension = -1;
            UserInfo userInfo = ((UserInfo) searchable);
            if (!TextUtils.isEmpty(userInfo.remark.remark_name)) {
                if (containAllKeywords(userInfo.remark.remark_name, keywords)) {
                    hitWord = userInfo.remark.remark_name;
                    hitDimension = 1;
                } else if (containAllKeywords(userInfo.name, keywords)) {
                    hitWord = userInfo.name;
                    hitDimension = 2;
                }
            } else if (containAllKeywords(userInfo.name, keywords)) {
                hitWord = userInfo.name;
                hitDimension = 1;
            }
            if (hitWord != null) {
                searchBean = new SearchBean(searchable, hitWord, hitDimension);
            }
        }
        return searchBean;
    }

    private static boolean containAllKeywords(String hitWord, String[] keywords) {
        if (!TextUtils.isEmpty(hitWord)) {
            for (String keyword : keywords) {
                if (!hitWord.toLowerCase().contains(keyword.toLowerCase())) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public static boolean filter(Message message, String keyword) {
        String text = message.getMsgContent().getPlainText();
        return text.contains(keyword);
    }

    public int getHitDimension() {
        return hitDimension;
    }

    public String getHitWord() {
        return hitWord;
    }

    public Searchable getSearchable() {
        return searchable;
    }

    public SpannableStringBuilder getHitSpannable() {
        return hitSpannable;
    }

    public void setHitSpannable(SpannableStringBuilder hitSpannable) {
        this.hitSpannable = hitSpannable;
    }

    public String getFormattedTime() {
        return formattedTime;
    }

    public void setFormattedTime(String formattedTime) {
        this.formattedTime = formattedTime;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
}
