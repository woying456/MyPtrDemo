package com.android.gmacs.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.android.gmacs.R;
import com.android.gmacs.view.NetworkImageView;
import com.common.gmacs.parse.pubcontact.PublicContactInfo;
import com.common.gmacs.utils.ImageUtil;

import java.util.ArrayList;
import java.util.List;

import static com.android.gmacs.view.NetworkImageView.IMG_RESIZE;

public class GmacsBrandServiceAdapter extends BaseAdapter {

    private Context mContext;
    private List<PublicContactInfo> publicContactInfoList = new ArrayList<>();

    public GmacsBrandServiceAdapter(Context context, List<PublicContactInfo> publicContactInfoList) {
        this.mContext = context;
        this.publicContactInfoList = publicContactInfoList;
    }

    @Override
    public int getCount() {
        return publicContactInfoList.size();
    }

    @Override
    public Object getItem(int position) {
        return publicContactInfoList.get(position);
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
            viewHolder.contactName = (TextView) convertView.findViewById(R.id.tv_contact_name);
            viewHolder.contactAvatar = (NetworkImageView) convertView.findViewById(R.id.iv_avatar);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.contactAvatar
                .setDefaultImageResId(R.drawable.gmacs_ic_default_avatar)
                .setErrorImageResId(R.drawable.gmacs_ic_default_avatar)
                .setImageUrl(ImageUtil.makeUpUrl(publicContactInfoList.get(position).getAvatar(), IMG_RESIZE, IMG_RESIZE));
        viewHolder.contactName.setText(publicContactInfoList.get(position).getUser_name());
        return convertView;
    }

    private class ViewHolder {
        NetworkImageView contactAvatar;
        TextView contactName;
    }
}
