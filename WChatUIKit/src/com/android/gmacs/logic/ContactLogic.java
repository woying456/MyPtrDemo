package com.android.gmacs.logic;

import com.android.gmacs.R;
import com.android.gmacs.event.AddContactMsgEvent;
import com.android.gmacs.event.ContactReportEvent;
import com.android.gmacs.event.ContactsEvent;
import com.android.gmacs.event.DeleteContactEvent;
import com.android.gmacs.event.GroupsEvent;
import com.android.gmacs.event.PublicAccountListEvent;
import com.android.gmacs.event.RemarkEvent;
import com.android.gmacs.event.StarEvent;
import com.common.gmacs.core.ClientManager;
import com.common.gmacs.core.ContactsManager;
import com.common.gmacs.core.GroupManager;
import com.common.gmacs.core.MessageManager;
import com.common.gmacs.core.RecentTalkManager;
import com.common.gmacs.msg.data.IMReqFriendMsg;
import com.common.gmacs.parse.Pair;
import com.common.gmacs.parse.contact.Contact;
import com.common.gmacs.parse.contact.GmacsUser;
import com.common.gmacs.parse.contact.Group;
import com.common.gmacs.parse.contact.Remark;
import com.common.gmacs.parse.contact.UserInfo;
import com.common.gmacs.parse.message.AcceptFriendMessage;
import com.common.gmacs.parse.message.GmacsUserInfo;
import com.common.gmacs.parse.message.Message;
import com.common.gmacs.parse.pubcontact.PAFunctionConfig;
import com.common.gmacs.parse.pubcontact.PublicContactInfo;
import com.common.gmacs.utils.GmacsEnvi;

import org.greenrobot.eventbus.EventBus;

import java.util.HashSet;
import java.util.List;

public class ContactLogic extends BaseLogic implements ContactsManager.ContactsChangeCb, ContactsManager.UserInfoChangeCb {

    private volatile static ContactLogic instance;

    private ContactLogic() {
    }

    public static ContactLogic getInstance() {
        if (null == instance) {
            synchronized (ContactLogic.class) {
                if (null == instance) {
                    instance = new ContactLogic();
                }
            }
        }
        return instance;
    }

    @Override
    public void init() {
        ContactsManager.getInstance().regContactsChangeCb(this);
    }

    /**
     * 获取联系人
     */
    public void getContacts() {
        ContactsManager.getInstance().getContactsAsync(new ContactsManager.GetContactsCb() {
            @Override
            public void done(int errorCode, String errorMessage, List<Contact> contacts, List<Contact> stars) {
                if (errorCode != 0) {
                    EventBus.getDefault().post(errorMessage);
                } else {
                    EventBus.getDefault().post(new ContactsEvent(contacts, stars));
                }
            }
        });
    }

    /**
     * 获取群列表
     */
    public void getGroups() {
        GroupManager.getGroupsAsync(new GroupManager.GetGroupsCb() {
            @Override
            public void done(int errorCode, String errorMessage, List<Group> groups) {
                if (errorCode != 0) {
                    EventBus.getDefault().post(errorMessage);
                } else {
                    EventBus.getDefault().post(new GroupsEvent(groups));
                }
            }
        });
    }

    /**
     * 删除联系人
     */
    public void delContact(final String contactId, final int contactSource) {
        ContactsManager.getInstance().delContactAsync(contactId, contactSource, new ClientManager.CallBack() {
            @Override
            public void done(int errorCode, String errorMessage) {
                if (errorCode != 0) {
                    EventBus.getDefault().post(errorMessage);
                } else {
                    RecentTalkManager.getInstance().deleteTalkByIdAsync(contactId, contactSource, null);
                    EventBus.getDefault().post(new DeleteContactEvent(contactId, contactSource));
                }
            }
        });
    }

    /**
     * 请求添加联系人
     */
    public void requestContact(String userId, int userSource, String text, String extra) {
        ContactsManager.getInstance().requestFriend(userId, userSource, text, extra, new ClientManager.CallBack() {
            @Override
            public void done(int errorCode, String errorMessage) {
                if (errorCode == 0) {
                    EventBus.getDefault().post(GmacsEnvi.appContext.getString(R.string.add_contact_sent));
                } else if (errorCode == 41110) {
                    EventBus.getDefault().post(GmacsEnvi.appContext.getString(R.string.add_contact_limit));
                } else if (errorCode == 41111) {
                    EventBus.getDefault().post(GmacsEnvi.appContext.getString(R.string.add_contact_already));
                } else if (errorCode == 41102) {
                    EventBus.getDefault().post(GmacsEnvi.appContext.getString(R.string.new_friends_self_black));
                } else if (errorCode == 41103) {
                    EventBus.getDefault().post(GmacsEnvi.appContext.getString(R.string.new_friends_other_black));
                } else {
                    EventBus.getDefault().post(errorMessage);
                }
            }
        });
    }

    /**
     * 接受添加联系人请求
     */
    public void acceptContact(final Message message, final String userId, final int userSource, final String msgId) {
        ContactsManager.getInstance().acceptFriend(userId, userSource, msgId, new ContactsManager.AcceptFriendCb() {
            @Override
            public void onAcceptFriend(int errorCode, String errorMessage, AcceptFriendMessage acceptMessage) {
                if (errorCode == 0 || errorCode == 41111) {
                    IMReqFriendMsg imReqFriendMsg = (IMReqFriendMsg) message.getMsgContent();
                    imReqFriendMsg.acceptTime = acceptMessage.acceptTime;
                    MessageManager.getInstance().updateMessage(message, null);
                    EventBus.getDefault().post(new AddContactMsgEvent(userId, userSource, msgId, acceptMessage));
                } else if (errorCode == 41112) {
                    EventBus.getDefault().post(GmacsEnvi.appContext.getString(R.string.new_friends_accepted));
                } else if (errorCode == 41102) {
                    EventBus.getDefault().post(GmacsEnvi.appContext.getString(R.string.new_friends_self_black));
                } else if (errorCode == 41103) {
                    EventBus.getDefault().post(GmacsEnvi.appContext.getString(R.string.new_friends_other_black));
                } else {
                    EventBus.getDefault().post(errorMessage);
                }
            }
        });
    }

    /**
     * 星标用户
     */
    public void star(final String contactId, final int contactSource) {
        ContactsManager.getInstance().starAsync(contactId, contactSource, new ClientManager.CallBack() {
            @Override
            public void done(int errorCode, String errorMessage) {
                if (errorCode == 0) {
                    EventBus.getDefault().post(new StarEvent(contactId, contactSource, true));
                } else {
                    EventBus.getDefault().post(errorMessage);
                }
            }
        });
    }

    /**
     * 取消星标用户
     */
    public void unStar(final String contactId, final int contactSource) {
        ContactsManager.getInstance().unStarAsync(contactId, contactSource, new ClientManager.CallBack() {
            @Override
            public void done(int errorCode, String errorMessage) {
                if (errorCode == 0) {
                    EventBus.getDefault().post(new StarEvent(contactId, contactSource, false));
                } else {
                    EventBus.getDefault().post(errorMessage);
                }
            }
        });
    }

    /**
     * 获取联系人信息
     *
     * @param userId
     * @param userSource
     */
    public void getUserInfo(final String userId, final int userSource) {
        ContactsManager.getInstance().getUserInfoAsync(userId, userSource, new ContactsManager.GetUserInfoCb() {
            @Override
            public void done(int errorCode, String errorMessage, UserInfo userInfo) {
                if (errorCode == 0 && userInfo != null) {
                    EventBus.getDefault().post(userInfo);
                    if (userId.equals(GmacsUser.getInstance().getUserId()) && userSource == GmacsUser.getInstance().getSource()) {
                        GmacsUserInfo gmacsUserInfo = GmacsUserInfo.getUserInfoFromContact(userInfo);
                        if (gmacsUserInfo != null) {
                            ClientManager.getInstance().setGmacsUserInfo(gmacsUserInfo);
                        }
                    }
                } else {
                    EventBus.getDefault().post(errorMessage);
                }
            }
        });
    }

    /**
     * 从本地获取联系人信息
     *
     * @param userId
     * @param userSource
     */
    public void getLocalUserInfo(String userId, int userSource) {
        HashSet<Pair> pairs = new HashSet<>();
        pairs.add(new Pair(userId, userSource));
        ContactsManager.getInstance().getLocalUserInfoBatchAsync(pairs, new ContactsManager.UserInfoBatchCb() {
            @Override
            public void onGetUserInfoBatch(int errorCode, String errorMessage, List<UserInfo> userInfoList) {
                if (errorCode == 0) {
                    if (userInfoList != null && userInfoList.size() > 0) {
                        EventBus.getDefault().post(userInfoList.get(0));
                    }
                } else {
                    EventBus.getDefault().post(errorMessage);
                }
            }
        });
    }

    /**
     * 从网络获取联系人信息
     *
     * @param userId
     * @param userSource
     */
    public void getLatestUserInfo(String userId, int userSource) {
        HashSet<Pair> pairs = new HashSet<>();
        pairs.add(new Pair(userId, userSource));
        ContactsManager.getInstance().getLatestUserInfoBatchAsync(pairs, new ContactsManager.UserInfoBatchCb() {
            @Override
            public void onGetUserInfoBatch(int errorCode, String errorMessage, List<UserInfo> userInfoList) {
                if (errorCode == 0) {
                    if (userInfoList != null && userInfoList.size() > 0) {
                        EventBus.getDefault().post(userInfoList.get(0));
                    }
                } else {
                    EventBus.getDefault().post(errorMessage);
                }
            }
        });
    }

    /**
     * 联系人姓名备注
     *
     * @param targetId
     * @param targetSource
     * @param guestName
     * @param postData
     */
    public void remark(final String targetId, final int targetSource, final boolean isFriend, String guestName, final Remark postData) {
        ContactsManager.getInstance().remarkAsync(targetId, targetSource, guestName, postData, new ClientManager.CallBack() {
            @Override
            public void done(int errorCode, String errorMessage) {
                if (errorCode == 0) {
                    Remark finalPostData;
                    if (postData == null) {
                        finalPostData = new Remark();
                    } else {
                        finalPostData = postData;
                    }
                    EventBus.getDefault().post(new RemarkEvent(targetId, targetSource, finalPostData));
                    if (!isFriend) {
                        getLatestUserInfo(targetId, targetSource);
                    }
                } else {
                    EventBus.getDefault().post(errorMessage);
                }
            }

        });
    }

    /**
     * 获取公众号列表
     */
    public void getPublicAccount(int targetSource) {
        ContactsManager.getInstance().getPublicAccountListAsync(targetSource, new ContactsManager.GetPublicAccountListCb() {
            @Override
            public void done(int errorCode, String errorMessage, List<PublicContactInfo> pubs) {
                if (errorCode == 0) {
                    EventBus.getDefault().post(new PublicAccountListEvent(pubs));
                } else {
                    EventBus.getDefault().post(errorMessage);
                }
            }
        });
    }

    /**
     * 获取公众号底部菜单
     */
    public void getPAFunctionConfig(String targetId, int targetSource) {
        ContactsManager.getInstance().getPAFunctionConfAsync(targetId, targetSource, new ContactsManager.GetPAFunctionConfCb() {
            @Override
            public void done(int errorCode, String errorMessage, String menuData, String targetId, int targetSource) {
                if (errorCode == 0) {
                    EventBus.getDefault().post(PAFunctionConfig.buildPAFunctionConfig(menuData));
                } else {
                    EventBus.getDefault().post(errorMessage);
                }
            }
        });
    }

    /**
     * 举报联系人
     *
     * @param reportId
     * @param reportSource
     * @param reportInfo
     */
    public void reportUser(final String reportId, int reportSource, String reportInfo) {
        ContactsManager.getInstance().reportUserAsync(reportId, reportSource, reportInfo, new ContactsManager.ReportUserCb() {
            @Override
            public void done(int errorCode, String errorMessage, boolean result, String text, String reportId, int reportSource) {
                if (errorCode == 0) {
                    EventBus.getDefault().post(new ContactReportEvent());
                } else {
                    EventBus.getDefault().post(errorMessage);
                }
            }
        });
    }

    @Override
    public void destroy() {
        EventBus.getDefault().post(new ContactsEvent(null, null));
    }

    @Override
    public void onContactsChanged(List<Contact> contacts, List<Contact> stars) {
        EventBus.getDefault().post(new ContactsEvent(contacts, stars));
    }

    @Override
    public void onUserInfoChanged(UserInfo info) {
        EventBus.getDefault().post(info);
        GmacsUserInfo gmacsUserInfo = GmacsUserInfo.getUserInfoFromContact(info);
        if (gmacsUserInfo != null) {
            ClientManager.getInstance().setGmacsUserInfo(gmacsUserInfo);
        }
    }
}