package com.android.gmacs.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.android.gmacs.event.RemarkEvent;
import com.common.gmacs.core.ContactsManager;
import com.common.gmacs.core.GmacsConstant;
import com.common.gmacs.parse.Pair;
import com.common.gmacs.parse.contact.Contact;
import com.common.gmacs.parse.contact.Group;
import com.common.gmacs.parse.contact.GroupMember;
import com.common.gmacs.parse.contact.Remark;
import com.common.gmacs.parse.contact.UserInfo;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;

public abstract class UserInfoBaseActivity extends BaseActivity implements ContactsManager.UserInfoChangeCb {

    protected String id;
    protected int source;
    protected UserInfo info;
    protected int maxCountFetchInfo = Integer.MAX_VALUE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        Intent intent = getIntent();
        if (intent != null) {
            id = intent.getStringExtra(GmacsConstant.EXTRA_USER_ID);
            source = intent.getIntExtra(GmacsConstant.EXTRA_USER_SOURCE, -1);
            if (!TextUtils.isEmpty(id) && source != -1) {
                ContactsManager.getInstance().registerUserInfoChange(id, source, this);
                ContactsManager.getInstance().getUserInfoAsync(id, source, new GetUserInfoCallBack(this));
            } else {
                finish();
            }
        }
    }

    @Override
    protected void initData() {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        if (!TextUtils.isEmpty(id) && source != -1) {
            ContactsManager.getInstance().unRegisterUserInfoChange(id, source, this);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGetUserInfo(Contact contact) {
        if (!isFinishing()) {
            if (info instanceof Contact) {
                if (TextUtils.equals(id, contact.getId()) && source == contact.getSource()) {
                    info = contact;
                    updateUI();
                }
            } else if (info instanceof Group) {
                for (GroupMember groupMember : ((Group) info).getMembers()) {
                    if (TextUtils.equals(contact.getId(), groupMember.getId())
                            && contact.getSource() == groupMember.getSource()) {
                        if (needToRefresh(groupMember, contact)) {
                            groupMember.updateFromContact(contact);
                            updateUI();
                        }
                        break;
                    }
                }
            }
        }
    }

    private boolean needToRefresh(GroupMember groupMember, Contact contact) {
        return !TextUtils.equals(groupMember.getName(), contact.name)
                || !TextUtils.equals(groupMember.getRemarkName(), contact.remark.remark_name)
                || !TextUtils.equals(groupMember.getAvatar(), contact.avatar)
                || !TextUtils.equals(groupMember.getRemarkSpell(), contact.remark.remark_spell)
                || !TextUtils.equals(groupMember.getNameSpell(), contact.nameSpell);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGetUserInfo(Group info) {
        if (!isFinishing()) {
            if (TextUtils.equals(id, info.getId()) && source == info.getSource()) {
                this.info = info;
                HashSet<Pair> users = null;
                int i = 0;
                for (GroupMember groupMember : info.getMembers()) {
                    if (++i > maxCountFetchInfo) {
                        break;
                    }
                    if (TextUtils.isEmpty(groupMember.getName())) {
                        if (users == null) {
                            users = new HashSet<>();
                        }
                        users.add(new Pair(groupMember.getId(), groupMember.getSource()));
                    }
                }
                if (users != null) {
                    ContactsManager.getInstance().getLocalUserInfoBatchAsync(users, new GetGroupMemberInfoBatchLocalCallBack(this));
                } else {
                    updateUI();
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRemark(RemarkEvent event) {
        if (!isFinishing()) {
            Remark remark = event.getRemark();
            if (info instanceof Contact) {
                if (id.equals(event.getUserId()) && source == event.getUserSource()) {
                    info.remark = remark;
                    updateUI();
                }
            } else if (info instanceof Group) {
                for (GroupMember groupMember : ((Group) info).getMembers()) {
                    if (event.getUserId().equals(groupMember.getId()) && event.getUserSource() == groupMember.getSource()) {
                        if (!TextUtils.equals(remark.remark_name, groupMember.getRemarkName())
                                || !TextUtils.equals(remark.remark_spell, groupMember.getRemarkSpell())) {
                            groupMember.setRemark(remark);
                            updateUI();
                        }
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void onUserInfoChanged(final UserInfo info) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (info instanceof Group) {
                    onGetUserInfo((Group) info);
                } else if (info instanceof Contact) {
                    onGetUserInfo((Contact) info);
                }
            }
        });

    }

    protected abstract void updateUI();

    private static class GetUserInfoCallBack implements ContactsManager.GetUserInfoCb {

        WeakReference<UserInfoBaseActivity> activityWeakReference;

        GetUserInfoCallBack(UserInfoBaseActivity activity) {
            activityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void done(int errorCode, String errorMessage, final UserInfo info) {
            if (errorCode == 0) {
                final UserInfoBaseActivity activity = activityWeakReference.get();
                if (activity != null && !activity.isFinishing()) {
                    activity.info = info;
                    if (info instanceof Group) {
                        HashSet<Pair> users = null;
                        int i = 0;
                        for (GroupMember groupMember : ((Group) info).getMembers()) {
                            if (++i > activity.maxCountFetchInfo) {
                                break;
                            }
                            if (TextUtils.isEmpty(groupMember.getName())) {
                                if (users == null) {
                                    users = new HashSet<>();
                                }
                                users.add(new Pair(groupMember.getId(), groupMember.getSource()));
                            }
                        }
                        if (users != null) {
                            ContactsManager.getInstance().getLocalUserInfoBatchAsync(users, new GetGroupMemberInfoBatchLocalCallBack(activity));
                        } else {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (!activity.isFinishing()) {
                                        activity.updateUI();
                                    }
                                }
                            });
                        }
                    } else {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!activity.isFinishing()) {
                                    activity.updateUI();
                                }
                            }
                        });
                    }
                }
            }
        }
    }

    private static class GetGroupMemberInfoBatchLocalCallBack implements ContactsManager.UserInfoBatchCb {

        WeakReference<UserInfoBaseActivity> activityWeakReference;

        GetGroupMemberInfoBatchLocalCallBack(UserInfoBaseActivity activity) {
            activityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void onGetUserInfoBatch(int errorCode, String errorMessage, final List<UserInfo> userInfoList) {
            final UserInfoBaseActivity activity = activityWeakReference.get();
            if (activity != null && !activity.isFinishing() && errorCode == 0) {
                HashSet<Pair> networkFetchUsers = null;
                if (userInfoList != null) {
                    int i = 0;
                    for (GroupMember groupMember : ((Group) activity.info).getMembers()) {
                        if (++i > activity.maxCountFetchInfo) {
                            break;
                        }
                        if (groupMember.getName() == null) {
                            for (UserInfo userInfo : userInfoList) {
                                if (TextUtils.equals(groupMember.getId(), userInfo.getId())
                                        && groupMember.getSource() == userInfo.getSource()) {
                                    groupMember.updateFromContact((Contact) userInfo);
                                    break;
                                }
                            }
                            if (groupMember.getName() == null) {
                                if (networkFetchUsers == null) {
                                    networkFetchUsers = new HashSet<>();
                                }
                                networkFetchUsers.add(new Pair(groupMember.getId(), groupMember.getSource()));
                            }
                        }
                    }
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!activity.isFinishing()) {
                                activity.updateUI();
                            }
                        }
                    });
                }
                if (networkFetchUsers != null) {
                    ContactsManager.getInstance().getLatestUserInfoBatchAsync(networkFetchUsers, new GetGroupMemberInfoBatchNetworkCallBack(activity));
                }
            }
        }
    }

    private static class GetGroupMemberInfoBatchNetworkCallBack implements ContactsManager.UserInfoBatchCb {

        WeakReference<UserInfoBaseActivity> activityWeakReference;

        GetGroupMemberInfoBatchNetworkCallBack(UserInfoBaseActivity activity) {
            activityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void onGetUserInfoBatch(int errorCode, String errorMessage, final List<UserInfo> userInfoList) {
            final UserInfoBaseActivity activity = activityWeakReference.get();
            if (activity != null && !activity.isFinishing() && errorCode == 0) {
                if (userInfoList != null && userInfoList.size() > 0) {
                    for (GroupMember groupMember : ((Group) activity.info).getMembers()) {
                        if (groupMember.getName() == null) {
                            for (UserInfo userInfo : userInfoList) {
                                if (TextUtils.equals(groupMember.getId(), userInfo.getId())
                                        && groupMember.getSource() == userInfo.getSource()) {
                                    groupMember.updateFromContact((Contact) userInfo);
                                    break;
                                }
                            }
                        }
                    }
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!activity.isFinishing()) {
                                activity.updateUI();
                            }
                        }
                    });
                }
            }
        }
    }
}
