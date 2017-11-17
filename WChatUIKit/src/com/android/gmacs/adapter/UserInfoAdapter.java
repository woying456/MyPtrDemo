package com.android.gmacs.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.android.gmacs.R;
import com.android.gmacs.view.NetworkImageView;
import com.android.gmacs.view.PinnedHeaderListView;
import com.common.gmacs.parse.contact.UserInfo;
import com.common.gmacs.utils.ImageUtil;

import java.util.ArrayList;
import java.util.List;

import static com.android.gmacs.view.NetworkImageView.IMG_RESIZE;

public class UserInfoAdapter extends BaseAdapter implements PinnedHeaderListView.PinnedHeaderListAdapter {

    private List<UserInfo> userInfoList = new ArrayList<>();
    private Context mContext;

    public UserInfoAdapter(Context context, List<UserInfo> userInfoList) {
        mContext = context;
        this.userInfoList = userInfoList;
    }

    @Override
    public int getCount() {
        return userInfoList.size();
    }

    @Override
    public Object getItem(int position) {
        return userInfoList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.gmacs_contact_list_item, parent, false);
            viewHolder.contactAvatar = (NetworkImageView) convertView.findViewById(R.id.iv_avatar);
            viewHolder.contactName = (TextView) convertView.findViewById(R.id.tv_contact_name);
            viewHolder.separator = (TextView) convertView.findViewById(R.id.tv_separator);
            viewHolder.contactItemLine = convertView.findViewById(R.id.contact_item_line);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        UserInfo userInfo = userInfoList.get(position);
        viewHolder.contactName.setText(userInfo.getNameToShow());

        viewHolder.contactAvatar
                .setDefaultImageResId(R.drawable.gmacs_ic_default_avatar)
                .setErrorImageResId(R.drawable.gmacs_ic_default_avatar)
                .setImageUrl(ImageUtil.makeUpUrl(userInfo.getAvatar(), IMG_RESIZE, IMG_RESIZE));

        viewHolder.separator.setVisibility(View.VISIBLE);
        //全部联系人
        String currentStr = userInfo.getFirstLetter();// 当前联系人
        String previousStr = (position - 1) >= 0 ? userInfoList.get(position - 1).getFirstLetter() : "";// 上一个联系人
        if (position == 0 || !currentStr.equals(previousStr)) {
            viewHolder.separator.setText(currentStr);
            viewHolder.separator.setVisibility(View.VISIBLE);//不同字母再次分组
        } else {
            viewHolder.separator.setVisibility(View.GONE);//同一个字母
        }
        if (!currentStr.equals(previousStr)) {
            viewHolder.contactItemLine.setVisibility(View.GONE);
        } else {
            viewHolder.contactItemLine.setVisibility(View.VISIBLE);
        }
        return convertView;
    }


    /**
     * @return 滑动时判断头部字母listview
     */
    @Override
    public int getPinnedHeaderState(int position) {
        if (userInfoList.size() == 0) {
            return PINNED_HEADER_GONE;
        }
        UserInfo currentContact = userInfoList.get(position);
        String currentStr = currentContact.getFirstLetter();// 当前联系人
        String nextStr = (position + 1) < userInfoList.size() ? userInfoList.get(position + 1).getFirstLetter() : "";// 下一个联系人
        if (!currentStr.equals(nextStr)) {
            return PINNED_HEADER_PUSHED_UP;//挤压效果
        } else {
            return PINNED_HEADER_VISIBLE;
        }
    }

    /**
     * @param header   pinned header view.
     * @param position position of the first visible list item.
     * @param alpha    配置头部listview标题显示内容
     */
    @Override
    public void configurePinnedHeader(View header, int position, int alpha) {
        UserInfo curContact = userInfoList.get(position);
        String title = curContact.getFirstLetter();
        ((TextView) header.findViewById(R.id.tv_separator)).setText(title);
    }

    private class ViewHolder {
        NetworkImageView contactAvatar;
        TextView contactName;
        TextView separator;
        View contactItemLine;
    }
}
