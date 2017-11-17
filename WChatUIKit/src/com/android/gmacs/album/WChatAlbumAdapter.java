package com.android.gmacs.album;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.gmacs.R;
import com.android.gmacs.logic.TalkLogic;
import com.android.gmacs.view.NetworkImageView;
import com.common.gmacs.core.GmacsConstant;
import com.common.gmacs.msg.data.IMImageMsg;
import com.common.gmacs.parse.contact.Group;
import com.common.gmacs.parse.contact.UserInfo;
import com.common.gmacs.parse.message.Message;
import com.common.gmacs.utils.GmacsEnvi;
import com.common.gmacs.utils.ImageUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.android.gmacs.album.AlbumConstant.MAX_IMAGE_AMOUNT_IN_ROW;
import static com.android.gmacs.album.AlbumConstant.MAX_ROW_AMOUNT_PER_GROUP;
import static com.android.gmacs.album.AlbumConstant.REQUEST_CODE_WCHAT_ALBUM_PREVIEW;
import static com.android.gmacs.album.WChatAlbumUtil.SUNDAY;
import static com.android.gmacs.album.WChatAlbumUtil.THIS_DAY_OF_WEEK;
import static com.android.gmacs.album.WChatAlbumUtil.THIS_MONTH;
import static com.android.gmacs.album.WChatAlbumUtil.THIS_WEEK;
import static com.android.gmacs.album.WChatAlbumUtil.THIS_YEAR;
import static com.android.gmacs.msg.view.IMImageMsgView.ImgResize;
import static com.android.gmacs.msg.view.IMImageMsgView.MinResize;


public class WChatAlbumAdapter extends BaseAdapter implements WChatAlbumImageLayout.OnImageClickListener {

    private final AtomicInteger mNextGeneratedId = new AtomicInteger(1);
    private final int ITEM_VIEW_TYPE_IMAGE = 0;
    private final int ITEM_VIEW_TYPE_TIMESTAMP = 1;
    private final int ITEM_VIEW_TYPE_GROUPINFO_TIMESTAMP = 2;
    private final int IMAGE_MAX_SIZE;
    private final int AVATAR_SIZE;
    private final int EXTERNAL_IMAGE_MAX_WIDTH;
    private final int TEXT_SIZE_NORMAL;
    private final int TEXT_SIZE_LARGE;
    private long mPreviousLaunchTimeMillis;
    private Context mContext;
    /**
     * 用于单相册分页，保留分页处的一行，会和拉到的下一页重新按照时间戳分组
     */
    private ArrayList<Message> mLatestMessageGroupList;
    /**
     * 行优先存储图片消息，只有在时间段不同或时间段内无更多消息时，会出现每行数量不足
     * {@link AlbumConstant#MAX_IMAGE_AMOUNT_IN_ROW}的情况，
     * 数据存储顺序和展示顺序保持一致
     */
    private ArrayList<ArrayList<Message>> mMessagesList;
    /**
     * 用于单相册，把会话及其中的图片消息、图片消息总数、会话信息视为msgGroupInfo，
     * 通过source和id组成talkId，把talkId和msgGroupInfo映射起来
     *
     * @see MsgGroupInfo
     */
    private HashMap<String, MsgGroupInfo> mMsgGroupInfoCache;
    /**
     * true表示正在展示单相册，反之为全相册
     */
    private boolean mSingleAlbumShowing;

    WChatAlbumAdapter(Context context, boolean singleAlbumShowing) {
        mContext = context;
        mSingleAlbumShowing = singleAlbumShowing;
        mMessagesList = new ArrayList<>();
        mMsgGroupInfoCache = new HashMap<>();
        AVATAR_SIZE = mContext.getResources().getDimensionPixelOffset(R.dimen.album_avatar_size);
        IMAGE_MAX_SIZE = (GmacsEnvi.screenWidth
                - mContext.getResources().getDimensionPixelOffset(R.dimen.album_image_padding)
                * (MAX_IMAGE_AMOUNT_IN_ROW - 1)) / MAX_IMAGE_AMOUNT_IN_ROW;
        // 弥补像素不能整除而出现的白边问题
        int internalImageAmount = MAX_IMAGE_AMOUNT_IN_ROW - 2;
        EXTERNAL_IMAGE_MAX_WIDTH = (mContext.getResources().getDisplayMetrics().widthPixels
                - (internalImageAmount * IMAGE_MAX_SIZE + (internalImageAmount + 1)
                * mContext.getResources().getDimensionPixelOffset(R.dimen.album_image_padding))) / 2;
        TEXT_SIZE_LARGE = mContext.getResources().getDimensionPixelSize(R.dimen.album_name);
        TEXT_SIZE_NORMAL = mContext.getResources().getDimensionPixelSize(R.dimen.album_timestamp);
    }

    @Override
    public int getCount() {
        return mMessagesList.size();
    }

    @Override
    public ArrayList<Message> getItem(int position) {
        return mMessagesList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        List<Message> curMessages = getItem(position);
        if (position > 0) {
            String curTalkId = getTalkId(curMessages.get(0).getTalkOtherUserInfo());
            if (!getTalkId(getItem(position - 1).get(0).getTalkOtherUserInfo()).equals(curTalkId)) {
                return ITEM_VIEW_TYPE_GROUPINFO_TIMESTAMP;
            }

            List<Message> preMessages = getItem(position - 1);
            if (formatTimestamp(curMessages.get(0).mMsgUpdateTime)
                    .equals(formatTimestamp(preMessages.get(0).mMsgUpdateTime))) {
                return ITEM_VIEW_TYPE_IMAGE;
            } else {
                return ITEM_VIEW_TYPE_TIMESTAMP;
            }
        } else {
            if (!mSingleAlbumShowing) {
                return ITEM_VIEW_TYPE_GROUPINFO_TIMESTAMP;
            } else {
                return ITEM_VIEW_TYPE_TIMESTAMP;
            }
        }
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        int itemViewType = getItemViewType(position);
        if (convertView != null) {
            viewHolder = (ViewHolder) convertView.getTag();
        } else {
            viewHolder = new ViewHolder();
            viewHolder.images = new NetworkImageView[MAX_IMAGE_AMOUNT_IN_ROW];
            switch (itemViewType) {
                default:
                case ITEM_VIEW_TYPE_IMAGE:
                    convertView = LayoutInflater.from(mContext).inflate(R.layout.wchat_album_list_item_image, parent, false);
                    break;
                case ITEM_VIEW_TYPE_TIMESTAMP:
                    convertView = LayoutInflater.from(mContext).inflate(R.layout.wchat_album_list_item_timestamp, parent, false);
                    viewHolder.timestamp = (TextView) convertView.findViewById(R.id.album_timestamp);
                    break;
                case ITEM_VIEW_TYPE_GROUPINFO_TIMESTAMP:
                    convertView = LayoutInflater.from(mContext).inflate(R.layout.wchat_album_list_item_groupinfo_timestamp, parent, false);
                    viewHolder.avatar = (NetworkImageView) convertView.findViewById(R.id.album_avatar);
                    viewHolder.name = (TextView) convertView.findViewById(R.id.album_name);
                    viewHolder.timestamp = (TextView) convertView.findViewById(R.id.album_timestamp);
                    break;
            }
            inflateViewInCommon(viewHolder, convertView);
            for (int i = 0; i < AlbumConstant.MAX_IMAGE_AMOUNT_IN_ROW; i++) {
                if (i == 0 || i == AlbumConstant.MAX_IMAGE_AMOUNT_IN_ROW - 1) {
                    viewHolder.images[i].getLayoutParams().width = EXTERNAL_IMAGE_MAX_WIDTH;
                } else {
                    viewHolder.images[i].getLayoutParams().width = IMAGE_MAX_SIZE;
                }
                viewHolder.images[i].getLayoutParams().height = IMAGE_MAX_SIZE;
            }
            convertView.setTag(viewHolder);
        }
        viewHolder.imageContainer.setRowPosition(position);

        ArrayList<Message> messages = getItem(position);

        if (itemViewType != ITEM_VIEW_TYPE_IMAGE) {
            viewHolder.timestamp.setTextSize(TypedValue.COMPLEX_UNIT_PX, mSingleAlbumShowing ? TEXT_SIZE_LARGE : TEXT_SIZE_NORMAL);
            viewHolder.timestamp.setText(formatTimestamp(messages.get(0).mMsgUpdateTime));
            if (itemViewType == ITEM_VIEW_TYPE_GROUPINFO_TIMESTAMP) {
                UserInfo userInfo = mMsgGroupInfoCache.get(getTalkId(messages.get(0).getTalkOtherUserInfo())).userInfo;
                viewHolder.avatar.setDefaultImageResId(R.drawable.gmacs_ic_default_avatar);
                viewHolder.avatar.setErrorImageResId(R.drawable.gmacs_ic_default_avatar);

                if (userInfo == null) {
                    viewHolder.avatar.setImageUrl("");
                } else if (TextUtils.isEmpty(userInfo.getAvatar()) && userInfo instanceof Group) {
                    viewHolder.avatar.setImageUrls(TalkLogic.getInstance().getGroupTalkAvatar((Group) userInfo, AVATAR_SIZE));
                } else {
                    viewHolder.avatar.setImageUrl(ImageUtil.makeUpUrl(userInfo.avatar, AVATAR_SIZE, AVATAR_SIZE));
                }
                viewHolder.name.setText(userInfo == null ? "" : userInfo.getNameToShow());
            }
        }

        viewHolder.imageContainer.setLoadMorePosition(-1);
        // 未展示图片的数量
        if (!mSingleAlbumShowing) {
            viewHolder.loadMore.setVisibility(View.GONE);
            String talkId = getTalkId(messages.get(0).getTalkOtherUserInfo());
            MsgGroupInfo msgGroupInfo = mMsgGroupInfoCache.get(talkId);
            ArrayList<ArrayList<Message>> messagesList = msgGroupInfo.msgGroupList;
            if (messagesList != null && messagesList.indexOf(messages) + 1 >= MAX_ROW_AMOUNT_PER_GROUP) {
                int shownMsgCount = 0;
                for (ArrayList<Message> messages1 : messagesList) {
                    shownMsgCount += messages1.size();
                }
                if (msgGroupInfo.msgCount - shownMsgCount > 0) {
                    int anchor;
                    int messageCount = messages.size();
                    if (messageCount > 0) {
                        anchor = viewHolder.images[messageCount - 1].getId();
                        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) viewHolder.loadMore.getLayoutParams();
                        layoutParams.addRule(RelativeLayout.ALIGN_LEFT, anchor);
                        layoutParams.addRule(RelativeLayout.ALIGN_TOP, anchor);
                        layoutParams.addRule(RelativeLayout.ALIGN_RIGHT, anchor);
                        layoutParams.addRule(RelativeLayout.ALIGN_BOTTOM, anchor);
                        viewHolder.loadMore.setText(parent.getResources().getString(R.string.album_more_image, msgGroupInfo.msgCount - shownMsgCount));
                        viewHolder.loadMore.setVisibility(View.VISIBLE);
                        viewHolder.loadMore.setLayoutParams(layoutParams);
                        viewHolder.imageContainer.setLoadMorePosition(messageCount - 1);
                    }
                }
            }
        }

        for (int i = 0; i < AlbumConstant.MAX_IMAGE_AMOUNT_IN_ROW; i++) {
            viewHolder.images[i].setVisibility(View.GONE);
        }
        IMImageMsg imImageMsg;
        for (int i = 0; i < messages.size() && i < AlbumConstant.MAX_IMAGE_AMOUNT_IN_ROW; i++) {
            imImageMsg = (IMImageMsg) (messages.get(i).getMsgContent());
            viewHolder.images[i].setDefaultImageResId(R.drawable.wchat_ic_album_image_loading);
            viewHolder.images[i].setErrorImageResId(R.drawable.wchat_ic_album_image_error);
            viewHolder.images[i].setViewHeight(IMAGE_MAX_SIZE);
            viewHolder.images[i].setViewWidth(IMAGE_MAX_SIZE);
            if (!TextUtils.isEmpty(imImageMsg.mLocalUrl)) {
                viewHolder.images[i].setImageUrl(imImageMsg.mLocalUrl);
            } else if (imImageMsg.mUrl.startsWith("/")) {
                viewHolder.images[i].setImageUrl(imImageMsg.mUrl);
            } else {
                int[] scaleSize = ImageUtil.getScaleSize(imImageMsg.mWidth, imImageMsg.mHeight, ImgResize, ImgResize, MinResize, MinResize);
                viewHolder.images[i].setImageUrl(ImageUtil.makeUpUrl(imImageMsg.mUrl, scaleSize[3], scaleSize[2]));
            }
            viewHolder.images[i].setVisibility(View.VISIBLE);
        }

        // 弥补像素不能整除带来的白边问题
        RelativeLayout.LayoutParams layoutParams =
                (RelativeLayout.LayoutParams) viewHolder.images[MAX_IMAGE_AMOUNT_IN_ROW - 1].getLayoutParams();
        if (messages.size() == MAX_IMAGE_AMOUNT_IN_ROW) {
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        } else {
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
        }

        return convertView;
    }

    @Override
    public void onImageClick(int rowPosition, int colPosition, int x, int y, boolean isLoadMoreClicked) {
        Intent intent;
        if (!isLoadMoreClicked) {
            if (colPosition >= mMessagesList.get(rowPosition).size()) {
                return;
            }

            if (SystemClock.uptimeMillis() - mPreviousLaunchTimeMillis < 500) {
                return;
            }
            mPreviousLaunchTimeMillis = SystemClock.uptimeMillis();

            intent = new Intent();
            Message message = mMessagesList.get(rowPosition).get(colPosition);
            Message.MessageUserInfo messageUserInfo = message.getTalkOtherUserInfo();
            intent.putExtra(GmacsConstant.EXTRA_USER_ID, messageUserInfo.mUserId);
            intent.putExtra(GmacsConstant.EXTRA_USER_SOURCE, messageUserInfo.mUserSource);
            if (!mSingleAlbumShowing) {
                intent.putExtra(AlbumConstant.BEGIN_LOCAL_ID, message.mLocalId);
                intent.putExtra(AlbumConstant.IMAGE_LOCAL_ID, message.mLocalId);
                intent.putExtra(AlbumConstant.IMAGE_COUNT, 1);
            } else {
                int msgCount = 0;
                for (ArrayList<Message> messages : mMessagesList) {
                    msgCount += messages.size();
                }
                intent.putExtra(AlbumConstant.IMAGE_COUNT, msgCount);
                intent.putExtra(AlbumConstant.IMAGE_LOCAL_ID, message.mLocalId);
                intent.putExtra(AlbumConstant.BEGIN_LOCAL_ID, mMessagesList.get(0).get(0).mLocalId);
            }
            intent.putExtra("x", x);
            intent.putExtra("y", y);
            intent.putExtra("width", IMAGE_MAX_SIZE);
            intent.putExtra("height", IMAGE_MAX_SIZE);
            intent.putExtra(AlbumConstant.LAUNCHED_FROM_ALBUM, true);

            intent.setClass(mContext, GmacsImageActivity.class);
            if (mContext instanceof WChatAlbumsPreviewActivity) {
                ((WChatAlbumsPreviewActivity) mContext).startActivityForResult(intent,
                        REQUEST_CODE_WCHAT_ALBUM_PREVIEW);
            } else if (mContext instanceof WChatAlbumBrowserActivity) {
                ((WChatAlbumBrowserActivity) mContext).startActivityForResult(intent,
                        AlbumConstant.REQUEST_CODE_WCHAT_ALBUM_BROWSER);
            }
        } else {
            IMImageMsg imMessage = (IMImageMsg) mMessagesList.get(rowPosition).get(colPosition).getMsgContent();
            Message.MessageUserInfo messageUserInfo = imMessage.message.getTalkOtherUserInfo();

            intent = new Intent(mContext, WChatAlbumBrowserActivity.class);
            intent.putExtra(GmacsConstant.EXTRA_USER_ID, messageUserInfo.mUserId);
            intent.putExtra(GmacsConstant.EXTRA_USER_SOURCE, messageUserInfo.mUserSource);
            intent.putExtra(AlbumConstant.ALBUM_TITLE, mMsgGroupInfoCache.get(getTalkId(messageUserInfo)).userInfo.name);
            mContext.startActivity(intent);
        }
    }

    void addNewMsgGroupsToAdapter(List<MsgGroupInfo> msgGroupInfoList, int index) {
        if (mSingleAlbumShowing) {
            return;
        }

        for (MsgGroupInfo msgGroupInfo : msgGroupInfoList) {
            if (mMsgGroupInfoCache.get(msgGroupInfo.talkId) != null && index > -1) {
                mMessagesList.addAll(index, msgGroupInfo.msgGroupList);
            } else {
                mMessagesList.addAll(msgGroupInfo.msgGroupList);
            }

            mMsgGroupInfoCache.put(msgGroupInfo.talkId, msgGroupInfo);
        }

        notifyDataSetChanged();
    }

    void addNewMsgGroupsToAdapter(ArrayList<Message> newAllMsgGroups) {
        if (!mSingleAlbumShowing) {
            return;
        }

        if (newAllMsgGroups == null) {
            if (mLatestMessageGroupList != null) {
                mMessagesList.remove(mLatestMessageGroupList);
                notifyDataSetChanged();
                mMessagesList.add(mLatestMessageGroupList);
            } else {
                mMessagesList.clear();
            }
            notifyDataSetChanged();
            return;
        }

        ArrayList<ArrayList<Message>> resultList = new ArrayList<>();
        if (mLatestMessageGroupList != null) {
            mMessagesList.remove(mLatestMessageGroupList);
            mLatestMessageGroupList.addAll(newAllMsgGroups);
            WChatAlbumUtil.split(true, mLatestMessageGroupList, resultList);
        } else {
            WChatAlbumUtil.split(true, newAllMsgGroups, resultList);
        }
        mLatestMessageGroupList = resultList.get(resultList.size() - 1);

        mMessagesList.addAll(resultList);
        notifyDataSetChanged();
    }

    int removeMsgFromAdapter(String otherId, int otherSource) {
        if (mSingleAlbumShowing) {
            return -1;
        }

        int loopCount = 0;
        int matchedStartIndex = -1;
        int matchedEndIndex = -1;

        for (int i = 0; i < mMessagesList.size(); i++) {
            Message.MessageUserInfo messageUserInfo = mMessagesList.get(i).get(0).getTalkOtherUserInfo();
            if (messageUserInfo.mUserId.equals(otherId) && messageUserInfo.mUserSource == otherSource) {
                loopCount++;
                if (matchedStartIndex == -1) {
                    matchedStartIndex = i;
                }
                matchedEndIndex = i;
            } else if (loopCount > 0) {
                break;
            }
        }
        for (int i = matchedStartIndex; i <= matchedEndIndex; i++) {
            mMessagesList.remove(matchedStartIndex);
        }

        return matchedStartIndex;
    }

    void removeMsgFromAdapter(long localId) {
        if (!mSingleAlbumShowing) {
            return;
        }

        ArrayList<Message> changedList = new ArrayList<>();
        for (int i = 0; i < mMessagesList.size(); i++) {
            ArrayList<Message> messageGroup = mMessagesList.get(i);
            for (int j = 0; j < messageGroup.size(); j++) {
                Message temp = messageGroup.get(j);
                if (temp.mLocalId != localId) {
                    changedList.add(temp);
                }
            }
        }

        mMessagesList.clear();
        WChatAlbumUtil.split(mSingleAlbumShowing, changedList, mMessagesList);
        if (mMessagesList.size() > 0) {
            mLatestMessageGroupList = mMessagesList.get(mMessagesList.size() - 1);
        } else {
            mLatestMessageGroupList = null;
        }
        if (mContext instanceof WChatAlbumBrowserActivity) {
            ((WChatAlbumBrowserActivity) mContext).fetchNewPage(true);
        }
    }

    private int generateViewId() {
        for (; ; ) {
            final int result = mNextGeneratedId.get();
            int newValue = result + 1;
            if (newValue > 0x00FFFFFF) newValue = 1;
            if (mNextGeneratedId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }

    private void inflateViewInCommon(ViewHolder viewHolder, View convertView) {
        viewHolder.imageContainer = (WChatAlbumImageLayout) convertView.findViewById(R.id.album_image_container);
        int margin = mContext.getResources().getDimensionPixelOffset(R.dimen.album_image_padding);
        int previousResId = 0;
        for (int i = 0; i < AlbumConstant.MAX_IMAGE_AMOUNT_IN_ROW; i++) {
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(0, 0);
            viewHolder.images[i] = new NetworkImageView(mContext);
            viewHolder.images[i].setScaleType(ImageView.ScaleType.CENTER_CROP);
            if (i > 0) {
                layoutParams.setMargins(margin, margin, 0, 0);
            } else {
                layoutParams.setMargins(0, margin, 0, 0);
            }
            if (i > 0) {
                layoutParams.addRule(RelativeLayout.RIGHT_OF, previousResId);
            }
            viewHolder.images[i].setId(previousResId = generateViewId());
            viewHolder.imageContainer.addView(viewHolder.images[i], i, layoutParams);
        }
        viewHolder.loadMore = (TextView) convertView.findViewById(R.id.album_load_more);
        ((WChatAlbumImageLayout) convertView.findViewById(R.id.album_image_container)).setOnImageClickListener(this);
    }

    private String formatTimestamp(long imageTimestamp) {
        WChatAlbumUtil.setTimeInMillis(imageTimestamp);
        int imageYear = WChatAlbumUtil.getYear();
        int imageMonth = WChatAlbumUtil.getMonth();
        int imageWeek = WChatAlbumUtil.getWeekOfMonth();
        int imageDayOfWeek = WChatAlbumUtil.getDayOfWeek();

        if (THIS_YEAR == imageYear && THIS_MONTH == imageMonth) {
            if (THIS_WEEK == imageWeek) {
                if (imageDayOfWeek == SUNDAY) { // 上周日
                    return "本月";
                } else {
                    return "本周";
                }
            } else if (THIS_WEEK == imageWeek + 1) {
                if (THIS_DAY_OF_WEEK == SUNDAY) { // 本周日
                    return "本周";
                } else {
                    return "本月";
                }
            } else {
                return "本月";
            }
        }
        return imageYear + "年" + imageMonth + "月";
    }

    private String getTalkId(Message.MessageUserInfo msgUserInfo) {
        return msgUserInfo.mUserSource + "-" + msgUserInfo.mUserId;
    }

    static class MsgGroupInfo {
        ArrayList<ArrayList<Message>> msgGroupList;
        UserInfo userInfo;
        String talkId;
        int msgCount;

        MsgGroupInfo(ArrayList<ArrayList<Message>> msgGroupList, UserInfo userInfo, String talkId, int msgCount) {
            this.msgGroupList = msgGroupList;
            this.userInfo = userInfo;
            this.talkId = talkId;
            this.msgCount = msgCount;
        }
    }

    private static class ViewHolder {
        TextView name, timestamp, loadMore;
        NetworkImageView avatar;
        NetworkImageView[] images;
        WChatAlbumImageLayout imageContainer;
    }

}
