package com.android.gmacs.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.gmacs.R;
import com.android.gmacs.adapter.SearchResultAdapter;
import com.android.gmacs.logic.TalkLogic;
import com.android.gmacs.search.SearchBean;
import com.android.gmacs.search.SearchDetailEntry;
import com.android.gmacs.search.SearchResultWrapper;
import com.android.gmacs.utils.GmacsUiUtil;
import com.common.gmacs.core.ClientManager;
import com.common.gmacs.core.Gmacs;
import com.common.gmacs.parse.Pair;
import com.common.gmacs.parse.contact.Contact;
import com.common.gmacs.parse.contact.Group;
import com.common.gmacs.parse.contact.GroupMember;
import com.common.gmacs.parse.message.Message;
import com.common.gmacs.parse.search.SearchResult;
import com.common.gmacs.parse.search.Searchable;
import com.common.gmacs.parse.search.SearchedTalk;
import com.common.gmacs.parse.talk.Talk;
import com.common.gmacs.utils.GmacsUtils;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

public class GlobalSearchActivity extends BaseActivity implements AdapterView.OnItemClickListener, View.OnClickListener, TextWatcher {

    private EditText searchBar;
    private ListView resultContainer;
    private ImageView searchBack;
    private TextView emptyView;
    private ImageView clearAll;
    //    private View functionPane;
    private int searchType = ClientManager.GLOBAL_SEARCHTYPE_ALL;
    private SearchResultAdapter adapter;
    private SearchResultWrapper globalResult;
    private String globalKeyword;
    private int selectionPosition;
    private int fromTop;
    private String[] keywords;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowNoTitle(true);
        setBackEnable(false);
        setContentView(R.layout.gmacs_activity_global_search);
    }

    @Override
    protected void initView() {
        searchBar = (EditText) findViewById(R.id.et_search_bar);
        resultContainer = (ListView) findViewById(R.id.detail_search_result);
        emptyView = (TextView) findViewById(R.id.empty_view);
        searchBack = (ImageView) findViewById(R.id.search_back);
        clearAll = (ImageView) findViewById(R.id.iv_clear_all);
        searchBar.addTextChangedListener(this);
        resultContainer.setOnItemClickListener(this);
        searchBack.setOnClickListener(this);
        clearAll.setOnClickListener(this);
        findViewById(R.id.tv_search_cancel).setOnClickListener(this);
//        functionPane = findViewById(R.id.search_function_pane);
//        functionPane.setOnClickListener(this);
        searchBar.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (!TextUtils.isEmpty(searchBar.getText().toString().trim())
                        && actionId == EditorInfo.IME_ACTION_SEARCH) {
                    GmacsUtils.hideSoftInputMethod(v);
                }
                return true;
            }
        });
    }

    private void search() {
        if (searchBar != null) {
            String keyString = searchBar.getText().toString().trim();
            if (searchType == ClientManager.GLOBAL_SEARCHTYPE_ALL) {
                globalKeyword = keyString;
            }
            if (TextUtils.isEmpty(keyString)) {
                keywords = null;
                clearAll.setVisibility(View.GONE);
                resultContainer.setEmptyView(null);
                emptyView.setVisibility(View.GONE);
                refreshUI(null);
//                functionPane.setVisibility(View.VISIBLE);
            } else {
                String[] keywords = keyString.split(" ");
                HashSet<String> keywordSet = new HashSet<>();
                Collections.addAll(keywordSet, keywords);
                keywords = keywordSet.toArray(new String[keywordSet.size()]);
                if (!Arrays.equals(keywords, this.keywords)) {
                    this.keywords = keywords;
                    clearAll.setVisibility(View.VISIBLE);
                    long maxHistoryPerTalk = 100;
                    ClientManager.globalSearch(keywords[0], searchType, maxHistoryPerTalk, new SearchResultCallBack(this, keywords));
                }
//                functionPane.setVisibility(View.GONE);
            }
        }
    }

    private void onSearchResult(final SearchResultWrapper result) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (Arrays.equals(result.keywords, keywords)) {
                    if (searchType == ClientManager.GLOBAL_SEARCHTYPE_ALL) {
                        globalResult = result;
                    }
                    emptyView.setText("无结果");
                    resultContainer.setEmptyView(emptyView);
                    refreshUI(result);
                }
            }
        });
    }

    private void refreshUI(SearchResultWrapper result) {
        if (searchType == ClientManager.GLOBAL_SEARCHTYPE_ALL) {
            searchBack.setVisibility(View.GONE);
            searchBar.setHint("搜索");
        } else {
            searchBack.setVisibility(View.VISIBLE);
            switch (searchType) {
                case ClientManager.GLOBAL_SEARCHTYPE_CONTACT:
                    searchBar.setHint("查找联系人");
                    break;
                case ClientManager.GLOBAL_SEARCHTYPE_GROUP:
                    searchBar.setHint("查找群聊");
                    break;
                case ClientManager.GLOBAL_SEARCHTYPE_HISTORY:
                    searchBar.setHint("查找聊天记录");
                    break;
            }
        }
        if (adapter == null) {
            adapter = new SearchResultAdapter(GlobalSearchActivity.this, searchType, result);
            resultContainer.setAdapter(adapter);
        } else {
            adapter.setSearchTypeAndComposeSearchBean(searchType, result);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void initData() {
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        SearchBean searchBean = (SearchBean) adapter.getItem(position);
        if (searchBean != null) {
            Searchable searchable = searchBean.getSearchable();
            Intent intent;
            if (searchable instanceof Group) {
                intent = GmacsUiUtil.createToChatActivity(this, Gmacs.TalkType.TALKTYPE_GROUP.getValue(), ((Group) searchable).getId(), ((Group) searchable).getSource());
                startActivity(intent);
            } else if (searchable instanceof Contact) {
                intent = GmacsUiUtil.createToChatActivity(this, Gmacs.TalkType.TALKTYPE_NORMAL.getValue(), ((Contact) searchable).getId(), ((Contact) searchable).getSource());
                startActivity(intent);
            } else if (searchable instanceof SearchedTalk) {
                if (searchBean.getHitDimension() == 2) {
                    Talk talk = ((SearchedTalk) searchable).getTalk();
                    Message message = ((SearchedTalk) searchable).getMessageList().get(0);
                    intent = GmacsUiUtil.createToChatActivity(this, talk.mTalkType, talk.mTalkOtherUserId, talk.mTalkOtherUserSource, message.mLocalId);
                    startActivity(intent);
                } else if (searchBean.getHitDimension() == 1) {
                    String keyword = searchBar.getText().toString().trim();
                    SearchedTalkDetailActivity.start(this, keyword, (SearchedTalk) searchable);
                }
            } else if (searchable instanceof SearchDetailEntry) {
                searchType = ((SearchDetailEntry) searchable).getSearchType();
                selectionPosition = resultContainer.getFirstVisiblePosition() + 1;
                fromTop = resultContainer.getChildAt(1).getTop();
                refreshUI(globalResult);
                resultContainer.setSelection(0);
                GmacsUtils.hideSoftInputMethod(view.getWindowToken());
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.search_back) {
            searchType = ClientManager.GLOBAL_SEARCHTYPE_ALL;
            refreshUI(globalResult);
            resultContainer.setSelectionFromTop(selectionPosition, fromTop);
            searchBar.removeTextChangedListener(this);
            searchBar.setText(globalKeyword);
            searchBar.setSelection(globalKeyword.length());
            searchBar.addTextChangedListener(this);
        } else if (id == R.id.tv_search_cancel) {
            finish();
        } else if (id == R.id.iv_clear_all) {
            searchBar.setText(null);
        }
//        else if (id == search_function_pane) {
//            startActivity(new Intent(this, WChatAlbumsPreviewActivity.class));
//        }
    }

    @Override
    public void onBackPressed() {
        if (searchBack.getVisibility() == View.VISIBLE) {
            onClick(searchBack);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        search();
    }

    private static class SearchResultCallBack implements ClientManager.SearchResultCb {
        WeakReference<GlobalSearchActivity> activityWeakReference;
        String[] keywords;

        SearchResultCallBack(GlobalSearchActivity activity, String[] keywords) {
            activityWeakReference = new WeakReference<>(activity);
            this.keywords = keywords;
        }

        @Override
        public void done(int errorCode, String errorMessage, SearchResult searchResult) {
            GlobalSearchActivity activity = activityWeakReference.get();
            if (activity != null && !activity.isFinishing() && Arrays.equals(keywords, activity.keywords)) {
                SearchResultWrapper result = new SearchResultWrapper(keywords, searchResult);
                for (SearchBean bean : result.searchedTalks) {
                    SearchedTalk searchedTalk = (SearchedTalk) bean.getSearchable();
                    Talk talk = searchedTalk.getTalk();
                    if (talk.mTalkOtherUserInfo instanceof Group) {
                        Group group = (Group) talk.mTalkOtherUserInfo;
                        HashSet<Pair> users = new HashSet<>();
                        for (int i = 0; i < group.getMembers().size() && i < TalkLogic.MAX_GROUP_MEMBER_COUNT; i++) {
                            GroupMember member = group.getMembers().get(i);
                            users.add(new Pair(member.getId(), member.getSource()));
                        }
                        for (Message message : searchedTalk.getMessageList()) {
                            if (message.isShowSenderName() && !message.mIsSelfSendMsg) {
                                users.add(new Pair(message.mSenderInfo.mUserId, message.mSenderInfo.mUserSource));
                            }
                        }
                        TalkLogic.getInstance().fillGroupMemberInfoFromLocal(group, users);

                        talk.mTalkOtherName = TalkLogic.getInstance().getGroupTalkName(group, TalkLogic.MAX_GROUP_MEMBER_COUNT);

                        if (bean.getHitDimension() == 2) {
                            Message message = searchedTalk.getMessageList().get(0);
                            if (!message.mIsSelfSendMsg && message.isShowSenderName()) {
                                for (GroupMember member : ((Group) talk.mTalkOtherUserInfo).getMembers()) {
                                    if (TextUtils.equals(member.getId(), message.mSenderInfo.mUserId)
                                            && member.getSource() == message.mSenderInfo.mUserSource) {
                                        bean.setSenderName(member.getNameToShow());
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                for (SearchBean bean : result.groups) {
                    Group group = (Group) bean.getSearchable();
                    HashSet<Pair> users = new HashSet<>();
                    for (int i = 0; i < group.getMembers().size() && i < 4; i++) {
                        GroupMember member = group.getMembers().get(i);
                        users.add(new Pair(member.getId(), member.getSource()));
                    }
                    TalkLogic.getInstance().fillGroupMemberInfoFromLocal(group, users);
                }

                activity.onSearchResult(result);
            }
        }
    }
}
