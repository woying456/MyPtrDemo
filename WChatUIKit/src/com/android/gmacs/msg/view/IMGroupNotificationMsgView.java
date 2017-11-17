package com.android.gmacs.msg.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.gmacs.R;
import com.common.gmacs.core.ClientManager;
import com.common.gmacs.msg.IMMessage;
import com.common.gmacs.msg.data.IMGroupNotificationMsg;
import com.common.gmacs.parse.contact.GmacsUser;

import static com.common.gmacs.msg.data.IMGroupNotificationMsg.CHANGE_GROUP_NAME;
import static com.common.gmacs.msg.data.IMGroupNotificationMsg.JOIN_INVITE;
import static com.common.gmacs.msg.data.IMGroupNotificationMsg.RM_FROM_GROUP;
import static com.common.gmacs.msg.data.IMGroupNotificationMsg.TRANFER_GROUP_OWNER;


public class IMGroupNotificationMsgView extends IMMessageView {

    private static final String YOU = "你";
    private static final String SEPARATOR = "、";

    public IMGroupNotificationMsgView(IMMessage mIMMessage) {
        super(mIMMessage);
    }

    @Override
    protected View initView(LayoutInflater inflater, ViewGroup parentView, int maxWidth) {
        mContentView = inflater.inflate(R.layout.gmacs_adapter_msg_content_tip, parentView, false);
        return mContentView;
    }

    @Override
    public void setDataForView(IMMessage imMessage) {
        super.setDataForView(imMessage);
        IMGroupNotificationMsg groupNotificationMsg = (IMGroupNotificationMsg) mIMMessage;
        switch (groupNotificationMsg.operationType) {
            case CHANGE_GROUP_NAME:
                if (GmacsUser.getInstance().getUserId().equals(groupNotificationMsg.operator[0])) {
                    groupNotificationMsg.text = groupNotificationMsg.text.replaceFirst(groupNotificationMsg.operator[2], YOU);
                }
                break;
            case JOIN_INVITE:
                if (GmacsUser.getInstance().getUserId().equals(groupNotificationMsg.operator[0])) {
                    groupNotificationMsg.text = groupNotificationMsg.text.replaceFirst(groupNotificationMsg.operator[2], YOU);
                } else if (ClientManager.getInstance().getGmacsUserInfo().userName.equals(groupNotificationMsg.operator[2])) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(groupNotificationMsg.operator[2]).append(" 邀请 ");
                    boolean foundTheSameName = false;
                    if (groupNotificationMsg.targets != null && groupNotificationMsg.targets.size() >= 1) {
                        if (!groupNotificationMsg.targets.get(0)[2].equals(groupNotificationMsg.operator[2])) {
                            stringBuilder.append(groupNotificationMsg.targets.get(0)[2]);
                        } else {
                            foundTheSameName = true;
                            stringBuilder.append(YOU);
                        }
                        for (int i = 1; i < groupNotificationMsg.targets.size(); i++) {
                            if (!groupNotificationMsg.targets.get(i)[2].equals(groupNotificationMsg.operator[2]) || foundTheSameName) {
                                stringBuilder.append(SEPARATOR).append(groupNotificationMsg.targets.get(i)[2]);
                            } else {
                                foundTheSameName = true;
                                stringBuilder.append(SEPARATOR).append(YOU);
                            }
                        }
                        groupNotificationMsg.text = stringBuilder.append("加入群").toString();
                    }
                } else {
                    if (groupNotificationMsg.targets != null) {
                        for (String[] target : groupNotificationMsg.targets) {
                            if (GmacsUser.getInstance().getUserId().equals(target[0])) {
                                groupNotificationMsg.text = groupNotificationMsg.text.replaceFirst(ClientManager.getInstance().getGmacsUserInfo().userName, YOU);
                                break;
                            }
                        }
                    }
                }
                break;
            case RM_FROM_GROUP:
                if (GmacsUser.getInstance().getUserId().equals(groupNotificationMsg.operator[0])) {
                    int offset;
                    if ((offset = groupNotificationMsg.text.lastIndexOf(ClientManager.getInstance().getGmacsUserInfo().userName)) != -1) {
                        StringBuilder stringBuilder = new StringBuilder(groupNotificationMsg.text);
                        groupNotificationMsg.text = stringBuilder.replace(offset,
                                offset + ClientManager.getInstance().getGmacsUserInfo().userName.length(), YOU).toString();
                    }
                }
                break;
            case TRANFER_GROUP_OWNER:
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < groupNotificationMsg.targets.size(); i++) {
                    final String[] t = groupNotificationMsg.targets.get(i);// {id, source, name}
                    if (GmacsUser.getInstance().getUserId().equals(t[0]) && GmacsUser.getInstance().getSource() == Integer.parseInt(t[1])) {
                        sb.append(YOU).append(SEPARATOR);
                    } else {
                        sb.append(t[2]).append(SEPARATOR);
//                            GroupMember gm = mAdapter.groupMemberInfoCache.get(Talk.getTalkId(Integer.parseInt(t[1]), t[0]));
//                            if (null != gm) {
//                                sb.append(gm.getNameToShow()).append(SEPARATOR);
//                            } else {
//                                sb.append(t[2]).append(SEPARATOR);
//                            }
                    }
                }
                groupNotificationMsg.text = sb.substring(0, sb.length() - 1) + "已成为群主";
                break;
        }
        ((TextView) mContentView).setText(groupNotificationMsg.text);
    }
}
