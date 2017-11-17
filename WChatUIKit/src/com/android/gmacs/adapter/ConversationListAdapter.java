package com.android.gmacs.adapter;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.gmacs.R;
import com.android.gmacs.emoji.EmojiManager;
import com.android.gmacs.emoji.IEmojiParser;
import com.android.gmacs.logic.TalkLogic;
import com.android.gmacs.view.NetworkImageView;
import com.common.gmacs.core.GmacsConstant;
import com.common.gmacs.msg.IMMessage;
import com.common.gmacs.msg.MsgContentType;
import com.common.gmacs.msg.data.IMGroupNotificationMsg;
import com.common.gmacs.parse.contact.GmacsUser;
import com.common.gmacs.parse.contact.Group;
import com.common.gmacs.parse.message.Message;
import com.common.gmacs.parse.talk.Talk;
import com.common.gmacs.parse.talk.TalkType;
import com.common.gmacs.utils.ImageUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

import static com.android.gmacs.view.NetworkImageView.IMG_RESIZE;

public class ConversationListAdapter extends BaseAdapter {

    protected ArrayList<Talk> talkList;
    private LayoutInflater li;
    private SimpleDateFormat mSimpleDateFormat = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance();

    public ConversationListAdapter(Context context, ArrayList<Talk> talkList) {
        li = LayoutInflater.from(context);
        this.talkList = talkList;
    }

    /**
     * Format time.<br>Format message time on your own rule by overriding this method.</br>
     *
     * @param messageTime
     * @return
     */
    protected String messageTimeFormat(long messageTime) {
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

    private String messageTimeFormat(Talk talk) {
        if (null == talk.mUpdateTimeStyle) {
            talk.mUpdateTimeStyle = messageTimeFormat(talk.getTalkUpdateTime());
        }
        return talk.mUpdateTimeStyle;
    }


    /**
     * 用以子类重写该方法，根据不同性别获取默认头像
     *
     * @return
     */
    protected int defaultAvatarRes(int gender) {
        return R.drawable.gmacs_ic_default_avatar;
    }

    @Override
    public int getCount() {
        return talkList == null ? 0 : talkList.size();
    }

    @Override
    public Object getItem(int position) {
        return talkList == null ? null : talkList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder vh;
        if (convertView == null) {
            convertView = li.inflate(R.layout.gmacs_conversation_list_item, null);
            vh = new ViewHolder();
            vh.userAvatar = (NetworkImageView) convertView.findViewById(R.id.iv_avatar);
            vh.name = (TextView) convertView.findViewById(R.id.tv_conversation_name);
            vh.msgStatus = (ImageView) convertView.findViewById(R.id.iv_conversation_msg_status);
            vh.msgText = (TextView) convertView.findViewById(R.id.tv_conversation_msg_text);
            vh.msgTime = (TextView) convertView.findViewById(R.id.tv_conversation_msg_time);
            vh.msgCount = (TextView) convertView.findViewById(R.id.tv_conversation_msg_count);
            vh.silentIcon = (ImageView) convertView.findViewById(R.id.iv_silent);
            vh.silentMsgCount = (ImageView) convertView.findViewById(R.id.iv_conversation_silent_msg_dot);
            vh.divider = convertView.findViewById(R.id.v_conversation_divider);
            convertView.setTag(vh);
        } else {
            vh = (ViewHolder) convertView.getTag();
        }

        final Talk talk = talkList.get(position);

        if (TalkType.isGroupTalk(talk)) {
            vh.userAvatar.setDefaultImageResId(R.drawable.gmacs_ic_groups_entry)
                    .setErrorImageResId(R.drawable.gmacs_ic_groups_entry);
            if (TextUtils.isEmpty(talk.getOtherAvatar()) && talk.mTalkOtherUserInfo instanceof Group) {
                vh.userAvatar.setImageUrls(TalkLogic.getInstance().getGroupTalkAvatar((Group) talk.mTalkOtherUserInfo, IMG_RESIZE));
            } else {
                vh.userAvatar.setImageUrl(ImageUtil.makeUpUrl(talk.getOtherAvatar(), IMG_RESIZE, IMG_RESIZE));
            }
        } else {
            vh.userAvatar.setDefaultImageResId(defaultAvatarRes(talk.getOtherGender()))
                    .setErrorImageResId(defaultAvatarRes(talk.getOtherGender()))
                    .setImageUrl(ImageUtil.makeUpUrl(talk.getOtherAvatar(), IMG_RESIZE, IMG_RESIZE));
        }


        vh.name.setText(talk.getOtherName());

        // 消息发送状态
        int sendStateImgId = getSendStateImageSrcId(talk);
        if (sendStateImgId != -1) {
            vh.msgStatus.setVisibility(ImageView.VISIBLE);
            vh.msgStatus.setImageResource(sendStateImgId);
        } else {
            vh.msgStatus.setVisibility(ImageView.GONE);
        }

        vh.msgText.setText(getLastMessageContent(talk));

        vh.msgTime.setText(messageTimeFormat(talk));

        boolean isSilent = false;
        boolean isStickPost = false;
        if (talk.mTalkOtherUserInfo instanceof Group) {
            isSilent = ((Group) talk.mTalkOtherUserInfo).isSilent();
            isStickPost = ((Group) talk.mTalkOtherUserInfo).isStickPost();
        }


        if (isSilent) {
            vh.silentIcon.setVisibility(View.VISIBLE);
            vh.msgCount.setVisibility(View.GONE);
            if (talk.mNoReadMsgCount > 0) {
                vh.silentMsgCount.setVisibility(View.VISIBLE);
            } else {
                vh.silentMsgCount.setVisibility(View.GONE);
            }
        } else {
            vh.silentIcon.setVisibility(View.GONE);
            vh.silentMsgCount.setVisibility(View.GONE);
            // 未读消息数
            if (talk.mNoReadMsgCount > 99) {
                vh.msgCount.setText("99+");
                vh.msgCount.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 8);
                vh.msgCount.setVisibility(TextView.VISIBLE);
            } else if (talk.mNoReadMsgCount <= 0) {
                vh.msgCount.setVisibility(TextView.GONE);
            } else {
                vh.msgCount.setText(String.valueOf(talk.mNoReadMsgCount));
                vh.msgCount.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
                vh.msgCount.setVisibility(TextView.VISIBLE);
            }
        }

        if (isStickPost) {
            convertView.setBackgroundResource(R.drawable.gmacs_bg_conversation_list_item_stick_post);
        } else {
            convertView.setBackgroundResource(R.drawable.gmacs_bg_conversation_list_item);
        }

        if (position == talkList.size() - 1) {
            vh.divider.setBackgroundResource(R.color.transparent);
        } else {
            vh.divider.setBackgroundResource(R.color.conversation_list_divider);
        }
        return convertView;
    }

    private SpannableStringBuilder getLastMessageContent(Talk talk) {
        if (null == talk.mLastMessageStyle) {
            SpannableStringBuilder result = null;
            if (!TextUtils.isEmpty(talk.mDraftBoxMsg) && !talk.mHasAtMsg) {
                result = new SpannableStringBuilder(talk.mDraftBoxMsg);
                result.insert(0, "[草稿] ");
                result.setSpan(new ForegroundColorSpan(Color.parseColor("#DE3132")),
                        0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                Message lastMessage = talk.getLastMessage();
                if (lastMessage != null) {
                    IMMessage imMessage = lastMessage.getMsgContent();
                    ForegroundColorSpan fColorSpan;
                    switch (imMessage.showType) {
                        case MsgContentType.TYPE_TEXT:
                            result = new SpannableStringBuilder(imMessage.getPlainText());
                            IEmojiParser emojiParser = EmojiManager.getInstance().getEmojiParser();
                            if (emojiParser != null) {
                                emojiParser.replaceAllEmoji(result, 14);
                            }
                            break;
                        case MsgContentType.TYPE_IMAGE:
                        case MsgContentType.TYPE_LOCATION:
                        case MsgContentType.TYPE_UNIVERSAL_CARD1:
                        case MsgContentType.TYPE_UNIVERSAL_CARD2:
                        case MsgContentType.TYPE_UNIVERSAL_CARD3:
                        case MsgContentType.TYPE_UNIVERSAL_CARD4:
                            result = new SpannableStringBuilder(imMessage.getPlainText());
                            fColorSpan = new ForegroundColorSpan(Color.parseColor("#808080"));
                            result.setSpan(fColorSpan, 0, result.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            break;
                        case MsgContentType.TYPE_AUDIO:
                        case MsgContentType.TYPE_CALL:
                            result = new SpannableStringBuilder(imMessage.getPlainText());
                            if (!lastMessage.mIsSelfSendMsg &&
                                    lastMessage.getMsgPlayStatus() == GmacsConstant.MSG_NOT_PLAYED) {
                                fColorSpan = new ForegroundColorSpan(Color.RED);
                            } else {
                                fColorSpan = new ForegroundColorSpan(Color.parseColor("#808080"));
                            }
                            result.setSpan(fColorSpan, 0, result.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            break;
                        case MsgContentType.TYPE_TIP:
                        case MsgContentType.TYPE_GROUP_NOTIFICATION:
                        case MsgContentType.TYPE_REQ_FRIEND:
                            if (imMessage instanceof IMGroupNotificationMsg) {
                                result = new SpannableStringBuilder(imMessage.getPlainText().replace("user" + GmacsUser.getInstance().getUserId(), "你"));
                            } else {
                                result = new SpannableStringBuilder(imMessage.getPlainText());
                            }
                            break;
                    }
                    if (result != null && TalkType.isGroupTalk(talk)) {
                        if (lastMessage.atInfoArray != null) {
                            for (Message.AtInfo atInfo : lastMessage.atInfoArray) {
                                if (atInfo.getNameToShow() != null && atInfo.startPosition >= 0 && atInfo.endPosition <= result.length() && atInfo.startPosition < atInfo.endPosition) {
                                    result.replace(atInfo.startPosition, atInfo.endPosition, "@" + atInfo.getNameToShow() + " ");
//                                    result.setSpan(
//                                            new BackgroundColorSpan(Color.CYAN),
//                                            atInfo.startPosition,
//                                            atInfo.startPosition + atInfo.getNameToShow().length() + 2,
//                                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                }
                            }
                        }
                        if (imMessage.isShowSenderName()) {
                            if (!imMessage.message.mIsSelfSendMsg) {
                                if (!TextUtils.isEmpty(talk.getLastMessageSenderName())) {
                                    String senderName = talk.getLastMessageSenderName() + "：";
                                    result.insert(0, senderName);
                                }
                            }
                        }
                        if (talk.mTalkOtherUserInfo instanceof Group
                                && ((Group) talk.mTalkOtherUserInfo).isSilent()
                                && talk.mNoReadMsgCount > 1) {
                            result.insert(0, "[" + talk.mNoReadMsgCount + "条]");
                        }
                        if (talk.mHasAtMsg) {
                            result.insert(0, "[有人@我] ");
                            result.setSpan(new ForegroundColorSpan(Color.parseColor("#DE3132")),
                                    0, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }
                }
            }
            talk.mLastMessageStyle = result;
        }
        return talk.mLastMessageStyle;
    }

    private int getSendStateImageSrcId(Talk talk) {
        if (!TextUtils.isEmpty(talk.mDraftBoxMsg)) {
            return -1;
        } else if (talk.getLastMessage() == null) {
            return -1;
        } else if (talk.getLastMessage().mIsSelfSendMsg) {
            if (talk.getLastMessage().isMsgSending()) {
                return R.drawable.gmacs_ic_msg_sending_state;
            } else if (talk.getLastMessage().isMsgSendFailed()) {
                return R.drawable.gmacs_ic_msg_sended_failed;
            }
        }
        return -1;
    }

    private class ViewHolder {
        NetworkImageView userAvatar;
        ImageView msgStatus, silentIcon, silentMsgCount;
        TextView name, msgText, msgTime, msgCount;
        View divider;
    }

}
