package com.android.gmacs.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.gmacs.R;
import com.android.gmacs.activity.GmacsBrandServiceListActivity;
import com.android.gmacs.activity.GmacsNewFriendsActivity;
import com.android.gmacs.adapter.UserInfoAdapter;
import com.android.gmacs.event.ContactsEvent;
import com.android.gmacs.event.FriendUnreadCountEvent;
import com.android.gmacs.event.RemarkEvent;
import com.android.gmacs.logic.ContactLogic;
import com.android.gmacs.logic.MessageLogic;
import com.android.gmacs.utils.GmacsUiUtil;
import com.android.gmacs.view.FastLetterIndexView;
import com.android.gmacs.view.GmacsDialog;
import com.android.gmacs.view.PinnedHeaderListView;
import com.common.gmacs.core.Gmacs;
import com.common.gmacs.core.GmacsConstant;
import com.common.gmacs.parse.contact.UserInfo;
import com.common.gmacs.utils.GLog;
import com.common.gmacs.utils.GmacsEnvi;
import com.common.gmacs.utils.StringUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;


public class ContactListFragment extends BaseFragment implements AdapterView.OnItemClickListener, View.OnClickListener {

    protected PinnedHeaderListView mLvContactList;
    private FastLetterIndexView mFastLetterIndexView;
    private TextView mTvToastIndex, mTvNoContact;
    private UserInfoAdapter mUserInfoAdapter;
    private List<UserInfo> contacts = new ArrayList<>();
    private AdapterView.OnItemClickListener contactListListener;
    private View mNewFriendsHeaderView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void setContentView() {
        this.layoutResID = R.layout.gmacs_contact_list;
    }

    @Override
    protected void initView() {
        mLvContactList = (PinnedHeaderListView) getView().findViewById(R.id.pinnedheaderlistview_contacts);
        mFastLetterIndexView = (FastLetterIndexView) getView().findViewById(R.id.fastLetterIndexView);
        mTvToastIndex = (TextView) getView().findViewById(R.id.tv_toast_index);
        mTvNoContact = (TextView) getView().findViewById(R.id.tv_no_contact);
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        mLvContactList.setPinnedHeaderView(inflater.inflate(R.layout.gmacs_item_list_separators, mLvContactList, false));
        mLvContactList.setEnabledPinnedHeaderDynamicAlphaEffect(true);
        mLvContactList.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (mLvContactList != null) {
                    mLvContactList.onPinnedHeaderScroll(firstVisibleItem - mLvContactList.getHeaderViewsCount());
                }
            }
        });

        if (contactListListener != null) {
            mLvContactList.setOnItemClickListener(contactListListener);
        } else {
            mLvContactList.setOnItemClickListener(this);
        }

        View mHeadView = setHeaderView();
        if (mHeadView != null) {
            mLvContactList.addHeaderView(mHeadView);
        }
        mNewFriendsHeaderView = inflater.inflate(R.layout.gmacs_new_friends, null);
        mLvContactList.addHeaderView(mNewFriendsHeaderView);
        mNewFriendsHeaderView.setOnClickListener(this);
//        View mGroupListEntryHeaderView = inflater.inflate(R.layout.gmacs_group_entry_item, null);
//        mLvContactList.addHeaderView(mGroupListEntryHeaderView);
        mUserInfoAdapter = new UserInfoAdapter(getActivity(), contacts);
        mLvContactList.setAdapter(mUserInfoAdapter);

        mLvContactList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final int realPosition = position - mLvContactList.getHeaderViewsCount();
                if (contacts == null || realPosition >= contacts.size() || position < mLvContactList.getHeaderViewsCount()) {
                    return false;
                }
                final GmacsDialog.Builder dialog = new GmacsDialog.Builder(view.getContext(), GmacsDialog.Builder.DIALOG_TYPE_LIST_NO_BUTTON);
                dialog.initDialog(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        switch (position) {
                            case 0:
                                ContactLogic.getInstance().delContact(contacts.get(realPosition).getId(), contacts.get(realPosition).getSource());
                                dialog.dismiss();
                        }
                    }
                }).setListTexts(new String[]{getString(R.string.delete_contact)}).create().show();
                return true;
            }
        });

        //右侧字母滑动事件
        mFastLetterIndexView.setOnTouchLetterListener(new FastLetterIndexView.OnTouchLetterListener() {

            @Override
            public void onTouchLetter(MotionEvent event, int index, String letterIndex) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mTvToastIndex.setVisibility(View.VISIBLE);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        mTvToastIndex.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mTvToastIndex.setVisibility(View.GONE);
                            }
                        }, 500);
                        break;
                }
                if (View.VISIBLE == mTvToastIndex.getVisibility()) {//显示中间检索大字母
                    mTvToastIndex.setText(letterIndex);
                }

                /*
                 * 检索字母条与首字母相对应
                 */
                for (int i = 0; i < contacts.size(); i++) {
                    UserInfo contact = contacts.get(i);

                    String mNameSpell;
                    if (!TextUtils.isEmpty(contact.remark.remark_spell)) {
                        mNameSpell = contact.remark.remark_spell;
                    } else {
                        mNameSpell = contact.getNameSpell();
                    }

                    if (StringUtil.getAlpha(mNameSpell).equals(letterIndex)) {
                        mLvContactList.setSelection(i + mLvContactList.getHeaderViewsCount());
                        break;
                    }
                }

            }
        });

    }

    @Override
    protected void initData() {
        ContactLogic.getInstance().getContacts();
        MessageLogic.getInstance().getUnreadFriendCount();
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        int realPosition = position - mLvContactList.getHeaderViewsCount();
        if (realPosition < 0 || realPosition >= contacts.size()) {
            return;
        }
        try {
            Intent intent = new Intent();
            intent.setClass(getActivity(), Class.forName(GmacsUiUtil.getContactDetailActivityClassName()));
            intent.putExtra(GmacsConstant.EXTRA_USER_ID, contacts.get(realPosition).getId());
            intent.putExtra(GmacsConstant.EXTRA_TALK_TYPE, Gmacs.TalkType.TALKTYPE_NORMAL.getValue());
            intent.putExtra(GmacsConstant.EXTRA_USER_SOURCE, contacts.get(realPosition).getSource());
            intent.putExtra(GmacsConstant.EXTRA_DEVICE_ID, contacts.get(realPosition).getDeviceId());
            startActivity(intent);
        } catch (ClassNotFoundException ignored) {
        }
    }

    /**
     * 判断列表是否有数据
     */
    private void isHavePerson() {
        GLog.d(TAG, "contacts.size:" + contacts.size());
        if (contacts.size() > 0) {
            mTvNoContact.setVisibility(View.GONE);
            mFastLetterIndexView.setVisibility(View.VISIBLE);
            mLvContactList.getLayoutParams().height = RelativeLayout.LayoutParams.MATCH_PARENT;
            mLvContactList.requestLayout();
        } else {
            mLvContactList.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
            mLvContactList.requestLayout();
            mTvNoContact.setVisibility(View.VISIBLE);
            mFastLetterIndexView.setVisibility(View.GONE);
        }
    }

    protected View setHeaderView() {//子类设置头部视图
        return null;
    }

    /**
     * 跳转至公众号列表
     */
    protected void gotoGmacsBrandServiceListActivity() {
        startActivity(new Intent(getActivity(), GmacsBrandServiceListActivity.class));
    }

    /**
     * 供宿主程序监听联系人列表点击事件
     *
     * @param listener
     */
    public void onContactListItemClick(AdapterView.OnItemClickListener listener) {
        this.contactListListener = listener;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onContactListChanged(ContactsEvent event) {
        contacts.clear();
        if (event.getContactList() != null) {
            contacts.addAll(event.getContactList());
        }
        isHavePerson();
        mUserInfoAdapter.notifyDataSetChanged();
        ArrayList<String> lettersArray = new ArrayList<>();
        String current = "";
        for (UserInfo contact : contacts) {
            String temp = contact.getFirstLetter();
            if (!current.equals(temp)) {
                current = temp;
                lettersArray.add(current);
            }
        }

        int maxHeight = (int) (GmacsEnvi.screenHeight - getResources().getDimension(R.dimen.titlebar_height));
        mFastLetterIndexView.setMaxDisplayHeight(maxHeight);
        mFastLetterIndexView.setLetter(lettersArray);
    }

    /**
     * 接收消息
     *
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRemark(RemarkEvent event) {
        ContactLogic.getInstance().getContacts();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFriendUnreadCount(FriendUnreadCountEvent event) {
        long friendCount = event.getFriendCount();
        TextView mFriendCountView = (TextView) mNewFriendsHeaderView.findViewById(R.id.tv_new_friends_request);
        if (friendCount > 99) {
            mFriendCountView.setVisibility(View.VISIBLE);
            mFriendCountView.setText("99+");
            mFriendCountView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 8);
        } else if (friendCount <= 0) {
            mFriendCountView.setVisibility(View.GONE);
        } else {
            mFriendCountView.setVisibility(View.VISIBLE);
            mFriendCountView.setText(String.valueOf(friendCount));
            mFriendCountView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mNewFriendsHeaderView) {
            startActivity(new Intent(getActivity(), GmacsNewFriendsActivity.class));
        }
    }
}
