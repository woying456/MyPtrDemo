package com.android.gmacs.adapter;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.android.gmacs.R;
import com.android.gmacs.logic.TalkLogic;
import com.android.gmacs.search.SearchBean;
import com.android.gmacs.view.NetworkImageView;
import com.common.gmacs.parse.contact.Group;
import com.common.gmacs.parse.message.Message;
import com.common.gmacs.parse.search.SearchedTalk;
import com.common.gmacs.parse.talk.Talk;
import com.common.gmacs.utils.GmacsEnvi;
import com.common.gmacs.utils.ImageUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

import static com.android.gmacs.view.NetworkImageView.IMG_RESIZE;

public class SearchTalkDetailAdapter extends BaseAdapter {
    private Talk talk;
    private ArrayList<SearchBean> messageList;
    private LayoutInflater inflater;
    private String[] urls;
    private String[] keywords;

    private SimpleDateFormat mSimpleDateFormat = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance();

    private SearchResultAdapter.HighLightHelper highLightHelper = new SearchResultAdapter.HighLightHelper(GmacsEnvi.screenWidth
            - GmacsEnvi.appContext.getResources().getDimensionPixelOffset(R.dimen.avatar_conversation_list)
            - GmacsEnvi.appContext.getResources().getDimensionPixelOffset(R.dimen.conversation_list_margin_left)
            - GmacsEnvi.appContext.getResources().getDimensionPixelOffset(R.dimen.conversation_list_avatar_margin_right)
            - GmacsEnvi.appContext.getResources().getDimensionPixelOffset(R.dimen.conversation_list_time_margin_right), Color.parseColor("#14c2f4"));

    public SearchTalkDetailAdapter(Context context, SearchedTalk searchedTalk, String[] keywords) {
        inflater = LayoutInflater.from(context);
        talk = searchedTalk.getTalk();
        messageList = new ArrayList<>();
        for (Message message : searchedTalk.getMessageList()) {
            SearchBean searchBean = SearchBean.generateSearchBean(message, keywords);
            if (searchBean != null) {
                if (talk.mTalkOtherUserInfo instanceof Group && !message.mIsSelfSendMsg && message.isShowSenderName()) {
                    searchBean.setSenderName(TalkLogic.getInstance().getGroupMessageSenderName((Group) talk.mTalkOtherUserInfo, message));
                }
                messageList.add(searchBean);
            }
        }
        if (talk.mTalkOtherUserInfo instanceof Group && TextUtils.isEmpty(talk.getOtherAvatar())) {
            urls = TalkLogic.getInstance().getGroupTalkAvatar((Group) talk.mTalkOtherUserInfo, IMG_RESIZE);
        }

        this.keywords = keywords;
    }

    @Override
    public int getCount() {
        return messageList.size();
    }

    @Override
    public Object getItem(int position) {
        return messageList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder vh;
        final SearchBean searchBean = (SearchBean) getItem(position);
        if (convertView == null) {
            vh = new ViewHolder();
            convertView = inflater.inflate(R.layout.gmacs_conversation_list_item, parent, false);
            convertView.setBackgroundResource(R.drawable.gmacs_bg_conversation_list_item);
            vh.userAvatar = (NetworkImageView) convertView.findViewById(R.id.iv_avatar);
            vh.name = (TextView) convertView.findViewById(R.id.tv_conversation_name);
            vh.msgText = (TextView) convertView.findViewById(R.id.tv_conversation_msg_text);
            vh.msgTime = (TextView) convertView.findViewById(R.id.tv_conversation_msg_time);
            vh.divider = convertView.findViewById(R.id.v_conversation_divider);
            convertView.setTag(vh);
        } else {
            vh = (ViewHolder) convertView.getTag();
        }
        if (talk.mTalkOtherUserInfo instanceof Group && urls != null) {
            vh.userAvatar.setDefaultImageResId(R.drawable.gmacs_ic_groups_entry)
                    .setErrorImageResId(R.drawable.gmacs_ic_default_avatar)
                    .setImageUrls(urls);
        } else {
            vh.userAvatar.setDefaultImageResId(R.drawable.gmacs_ic_default_avatar)
                    .setErrorImageResId(R.drawable.gmacs_ic_default_avatar)
                    .setImageUrl(ImageUtil.makeUpUrl(talk.getOtherAvatar(), IMG_RESIZE, IMG_RESIZE));
        }

        vh.name.setText(talk.getOtherName());

        highLightHelper.highLightText(keywords, searchBean, vh.msgText, searchBean.getSenderName());

        vh.msgTime.setText(messageTimeFormat(searchBean));

        if (position == messageList.size() - 1) {
            vh.divider.setBackgroundResource(R.color.transparent);
        } else {
            vh.divider.setBackgroundResource(R.color.conversation_list_divider);
        }
        return convertView;
    }

    private String messageTimeFormat(SearchBean searchBean) {
        if (null == searchBean.getFormattedTime()) {
            searchBean.setFormattedTime(messageTimeFormat(((Message) (searchBean.getSearchable())).mMsgUpdateTime));
        }
        return searchBean.getFormattedTime();
    }

    private String messageTimeFormat(long messageTime) {
        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentDate = calendar.get(Calendar.DAY_OF_YEAR);
        int currentWeek = calendar.get(Calendar.WEEK_OF_MONTH) - 1;

        calendar.setTimeInMillis(messageTime);
        calendar.setTimeZone(TimeZone.getTimeZone("GMT+8"));

        int messageYear = calendar.get(Calendar.YEAR);
        int messageMonth = calendar.get(Calendar.MONTH);
        int messageDate = calendar.get(Calendar.DAY_OF_YEAR);
        int messageWeek = calendar.get(Calendar.WEEK_OF_MONTH) - 1;

        String formattedTime;
        if (currentYear == messageYear) {
            int delta = currentDate - messageDate;
            if (delta == 0) {
                mSimpleDateFormat.applyPattern("HH:mm");
                formattedTime = mSimpleDateFormat.format(calendar.getTime());
            } else {
                if (delta == 1) {
                    formattedTime = "昨天";
                } else if ((messageWeek == currentWeek || currentMonth - messageMonth == 1) && delta < 7) {
                    String[] weekOfDays = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
                    int week = calendar.get(Calendar.DAY_OF_WEEK) - 1;
                    formattedTime = weekOfDays[week >= 0 ? week : 0];
                } else {
                    mSimpleDateFormat.applyPattern("MM-dd");
                    formattedTime = mSimpleDateFormat.format(calendar.getTime());
                }
            }
        } else {
            mSimpleDateFormat.applyPattern("yyyy-MM-dd");
            formattedTime = mSimpleDateFormat.format(calendar.getTime());
        }
        return formattedTime;
    }

    private class ViewHolder {
        NetworkImageView userAvatar;
        TextView name, msgText, msgTime;
        View divider;
    }

}
