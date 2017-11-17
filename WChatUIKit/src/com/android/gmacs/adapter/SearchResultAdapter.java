package com.android.gmacs.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.android.gmacs.R;
import com.android.gmacs.logic.TalkLogic;
import com.android.gmacs.search.SearchBean;
import com.android.gmacs.search.SearchDetailEntry;
import com.android.gmacs.search.SearchResultWrapper;
import com.android.gmacs.search.SearchTitle;
import com.android.gmacs.utils.HighLightTextUtil;
import com.android.gmacs.view.NetworkImageView;
import com.common.gmacs.core.ClientManager;
import com.common.gmacs.parse.contact.Contact;
import com.common.gmacs.parse.contact.Group;
import com.common.gmacs.parse.contact.UserInfo;
import com.common.gmacs.parse.search.Searchable;
import com.common.gmacs.parse.search.SearchedTalk;
import com.common.gmacs.parse.talk.Talk;
import com.common.gmacs.utils.GmacsEnvi;
import com.common.gmacs.utils.ImageUtil;

import java.util.ArrayList;

import static com.android.gmacs.view.NetworkImageView.IMG_RESIZE;

public class SearchResultAdapter extends BaseAdapter {
    private static final int ITEM_TYPE_TITLE = 0;
    private static final int ITEM_TYPE_ENTRY = 1;
    private static final int ITEM_TYPE_RESULT = 2;
    private final HighLightHelper highLightHelper = new HighLightHelper(GmacsEnvi.screenWidth
            - GmacsEnvi.appContext.getResources().getDimensionPixelOffset(R.dimen.avatar_conversation_list)
            - GmacsEnvi.appContext.getResources().getDimensionPixelOffset(R.dimen.conversation_list_margin_left)
            - GmacsEnvi.appContext.getResources().getDimensionPixelOffset(R.dimen.conversation_list_avatar_margin_right), Color.parseColor("#14c2f4"));
    private ArrayList<SearchBean> searchBeans;
    private LayoutInflater inflater;
    private String[] keywords;

    public SearchResultAdapter(Context context, int searchType, SearchResultWrapper searchResult) {
        inflater = LayoutInflater.from(context);
        setSearchTypeAndComposeSearchBean(searchType, searchResult);
    }

    public void setSearchTypeAndComposeSearchBean(int searchType, SearchResultWrapper searchResult) {
        if (searchBeans == null) {
            searchBeans = new ArrayList<>();
        } else {
            searchBeans.clear();
        }
        if (searchResult != null) {
            switch (searchType) {
                case ClientManager.GLOBAL_SEARCHTYPE_ALL:
                    int maxCountPerType = 3;
                    addSearchBeans(searchResult.contacts, maxCountPerType, ClientManager.GLOBAL_SEARCHTYPE_CONTACT);
                    addSearchBeans(searchResult.groups, maxCountPerType, ClientManager.GLOBAL_SEARCHTYPE_GROUP);
                    addSearchBeans(searchResult.searchedTalks, maxCountPerType, ClientManager.GLOBAL_SEARCHTYPE_HISTORY);
                    break;
                case ClientManager.GLOBAL_SEARCHTYPE_CONTACT:
                    addSearchBeans(searchResult.contacts, Integer.MAX_VALUE, searchType);
                    break;
                case ClientManager.GLOBAL_SEARCHTYPE_GROUP:
                    addSearchBeans(searchResult.groups, Integer.MAX_VALUE, searchType);
                    break;
                case ClientManager.GLOBAL_SEARCHTYPE_HISTORY:
                    addSearchBeans(searchResult.searchedTalks, Integer.MAX_VALUE, searchType);
                    break;
            }
            keywords = searchResult.keywords;
        }
    }

    private void addSearchBeans(ArrayList<SearchBean> beans, int maxCount, int searchType) {
        int size = beans.size();
        if (size > 0) {
            searchBeans.add(SearchBean.generateSearchBean(new SearchTitle(searchType), keywords));
            if (size <= maxCount) {
                searchBeans.addAll(beans);
            } else {
                for (int i = 0; i < maxCount; i++) {
                    searchBeans.add(beans.get(i));
                }
                searchBeans.add(SearchBean.generateSearchBean(new SearchDetailEntry(searchType), keywords));
            }
        }
    }


    @Override
    public int getItemViewType(int position) {
        Searchable searchable = searchBeans.get(position).getSearchable();
        if (searchable instanceof SearchTitle) {
            return ITEM_TYPE_TITLE;
        } else if (searchable instanceof SearchDetailEntry) {
            return ITEM_TYPE_ENTRY;
        } else {
            return ITEM_TYPE_RESULT;
        }
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public int getCount() {
        return searchBeans.size();
    }

    @Override
    public Object getItem(int position) {
        return searchBeans.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        final SearchBean searchBean = searchBeans.get(position);
        Searchable searchable = searchBean.getSearchable();
        int viewType;
        if (searchable instanceof SearchTitle) {
            viewType = ITEM_TYPE_TITLE;
        } else if (searchable instanceof SearchDetailEntry) {
            viewType = ITEM_TYPE_ENTRY;
        } else {
            viewType = ITEM_TYPE_RESULT;
        }
        if (convertView == null) {
            viewHolder = new ViewHolder();
            switch (viewType) {
                case ITEM_TYPE_TITLE:
                    convertView = inflater.inflate(R.layout.wchat_search_title, parent, false);
                    viewHolder.title = (TextView) convertView.findViewById(R.id.search_title);
                    viewHolder.gap = convertView.findViewById(R.id.search_gap);
                    break;
                case ITEM_TYPE_ENTRY:
                    convertView = inflater.inflate(R.layout.wchat_search_detail_entry, parent, false);
                    viewHolder.title = (TextView) convertView;
                    break;
                case ITEM_TYPE_RESULT:
                default:
                    convertView = inflater.inflate(R.layout.wchat_search_result, parent, false);
                    viewHolder.title = (TextView) convertView.findViewById(R.id.search_title);
                    viewHolder.detail = (TextView) convertView.findViewById(R.id.search_detail);
                    viewHolder.avatar = (NetworkImageView) convertView.findViewById(R.id.search_avatar);
                    viewHolder.divider = convertView.findViewById(R.id.search_divider);
                    break;
            }
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        switch (viewType) {
            case ITEM_TYPE_TITLE:
                viewHolder.title.setText(searchBean.getHitWord());
                if (position == 0) {
                    viewHolder.gap.setVisibility(View.GONE);
                } else {
                    viewHolder.gap.setVisibility(View.VISIBLE);
                }
                break;
            case ITEM_TYPE_ENTRY:
                viewHolder.title.setText(searchBean.getHitWord());
                break;
            case ITEM_TYPE_RESULT:
            default:
                UserInfo userInfo = null;
                if (searchable instanceof UserInfo) {
                    userInfo = (UserInfo) searchable;
                    if (searchBean.getHitDimension() == 1) {
                        highLightHelper.highLightText(keywords, searchBean, viewHolder.title, null);
                        viewHolder.detail.setVisibility(View.GONE);
                    } else {
                        viewHolder.title.setText(userInfo.getNameToShow());
                        highLightHelper.highLightText(keywords, searchBean, viewHolder.detail, "名称");
                        viewHolder.detail.setVisibility(View.VISIBLE);
                    }
                } else if (searchable instanceof SearchedTalk) {
                    Talk talk = ((SearchedTalk) searchable).getTalk();
                    userInfo = talk.mTalkOtherUserInfo;
                    viewHolder.title.setText(talk.getOtherName());
                    viewHolder.detail.setVisibility(View.VISIBLE);
                    if (searchBean.getHitDimension() == 1) {
                        viewHolder.detail.setText(inflater.getContext().getString(R.string.wchat_search_history_count, searchBean.getHitWord()));
                    } else {
                        highLightHelper.highLightText(keywords, searchBean, viewHolder.detail, searchBean.getSenderName());
                    }
                }
                if (userInfo instanceof Group) {
                    viewHolder.avatar.setTag(userInfo);
                    if (TextUtils.isEmpty(userInfo.avatar)) {
                        String[] urls = TalkLogic.getInstance().getGroupTalkAvatar((Group) userInfo, IMG_RESIZE);
                        viewHolder.avatar.setDefaultImageResId(R.drawable.gmacs_ic_groups_entry)
                                .setImageUrls(urls);
                    } else {
                        viewHolder.avatar.setDefaultImageResId(R.drawable.gmacs_ic_groups_entry)
                                .setErrorImageResId(R.drawable.gmacs_ic_groups_entry)
                                .setImageUrl(ImageUtil.makeUpUrl(userInfo.avatar, IMG_RESIZE, IMG_RESIZE));
                    }
                } else if (userInfo instanceof Contact) {
                    viewHolder.avatar.setTag(null);
                    viewHolder.avatar.setDefaultImageResId(R.drawable.gmacs_ic_default_avatar)
                            .setErrorImageResId(R.drawable.gmacs_ic_default_avatar)
                            .setImageUrl(ImageUtil.makeUpUrl(userInfo.avatar, IMG_RESIZE, IMG_RESIZE));
                }
                break;
        }
        return convertView;
    }

    static class HighLightHelper {
        private int maxWidth;
        private int color;

        HighLightHelper(int maxWidth, @ColorInt int color) {
            this.maxWidth = maxWidth;
            this.color = color;
        }

        void highLightText(String[] keywords, SearchBean searchBean, TextView tv, String prefix) {
            if (searchBean.getHitSpannable() != null) {
                tv.setText(searchBean.getHitSpannable());
            } else {
                float prefixLength = 0;
                if (!TextUtils.isEmpty(prefix)) {
                    prefix = prefix + "：";
                    prefixLength = tv.getPaint().measureText(prefix);
                }
                SpannableStringBuilder spannableString = HighLightTextUtil.highlightText(keywords, searchBean.getHitWord(), tv, maxWidth - prefixLength, color);
                if (!TextUtils.isEmpty(prefix)) {
                    spannableString.insert(0, prefix);
                }
                searchBean.setHitSpannable(spannableString);
                tv.setText(spannableString);
            }
        }
    }

    private class ViewHolder {
        TextView title;
        NetworkImageView avatar;
        TextView detail;
        View gap;
        View divider;
    }
}
