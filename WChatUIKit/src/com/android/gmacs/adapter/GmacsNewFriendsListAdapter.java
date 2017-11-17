package com.android.gmacs.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.android.gmacs.R;
import com.android.gmacs.logic.ContactLogic;
import com.android.gmacs.view.NetworkImageView;
import com.common.gmacs.msg.IMMessage;
import com.common.gmacs.msg.data.IMReqFriendMsg;
import com.common.gmacs.parse.message.Message;
import com.common.gmacs.utils.ImageUtil;

import java.util.ArrayList;

import static com.android.gmacs.view.NetworkImageView.IMG_RESIZE;


public class GmacsNewFriendsListAdapter extends BaseAdapter implements View.OnClickListener {

    private ArrayList<Message> mRequestArray;
    private LayoutInflater mLayoutInflater;
    public GmacsNewFriendsListAdapter(Context context, ArrayList<Message> messages) {
        mLayoutInflater = LayoutInflater.from(context);
        mRequestArray = messages;
    }

    public void addNewMsgToTop(Message message) {
        mRequestArray.add(0, message);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mRequestArray.size();
    }

    @Override
    public Message getItem(int position) {
        return mRequestArray.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        Message message = mRequestArray.get(position);
        IMMessage imReqFriendMsg = message.getMsgContent();
        if (!(imReqFriendMsg instanceof IMReqFriendMsg)) {
            convertView = new View(mLayoutInflater.getContext());
            return convertView;
        }
        ViewHolder viewHolder;
        if (convertView != null && convertView.getTag() != null) {
            viewHolder = (ViewHolder) convertView.getTag();
        } else {
            convertView = mLayoutInflater.inflate(R.layout.gmacs_new_friends_list, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.avatar = (NetworkImageView) convertView.findViewById(R.id.iv_avatar);
            viewHolder.name = (TextView) convertView.findViewById(R.id.tv_name);
            viewHolder.accept = (TextView) convertView.findViewById(R.id.tv_accept);
            convertView.setTag(viewHolder);
        }
        viewHolder.avatar
                .setDefaultImageResId(R.drawable.gmacs_ic_default_avatar)
                .setErrorImageResId(R.drawable.gmacs_ic_default_avatar)
                .setImageUrl(ImageUtil.makeUpUrl(((IMReqFriendMsg)imReqFriendMsg).reqUrl, IMG_RESIZE, IMG_RESIZE));
        boolean isReqFriendAccepted = ((IMReqFriendMsg)imReqFriendMsg).acceptTime != 0;
        viewHolder.name.setText(((IMReqFriendMsg)imReqFriendMsg).reqName);
        viewHolder.accept.setText(isReqFriendAccepted ? R.string.new_friends_accepted : R.string.new_friends_request);
        viewHolder.accept.setTextColor(isReqFriendAccepted ? Color.parseColor("#353535") : Color.WHITE);
        viewHolder.accept.setBackgroundResource(isReqFriendAccepted ? 0 : R.drawable.gmacs_bg_new_friends_list_accept);
        viewHolder.accept.setTag(message);
        if (isReqFriendAccepted) {
            viewHolder.accept.setOnClickListener(null);
        } else {
            viewHolder.accept.setOnClickListener(this);
        }
        return convertView;
    }

    @Override
    public void onClick(View v) {
        Message msg = (Message) v.getTag();
        if (msg != null) {
            IMReqFriendMsg imReqFriendMsg = (IMReqFriendMsg) msg.getMsgContent();
            ContactLogic.getInstance().acceptContact(msg, imReqFriendMsg.reqId, imReqFriendMsg.reqSource,
                    String.valueOf(msg.mMsgId));
        }
    }

    private final class ViewHolder {
        NetworkImageView avatar;
        TextView name;
        TextView accept;
    }
}
