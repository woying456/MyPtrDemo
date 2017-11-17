package com.android.gmacs.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.gmacs.R;
import com.android.gmacs.adapter.SearchTalkDetailAdapter;
import com.android.gmacs.search.SearchBean;
import com.android.gmacs.utils.GmacsUiUtil;
import com.common.gmacs.parse.message.Message;
import com.common.gmacs.parse.search.SearchedTalk;
import com.common.gmacs.parse.talk.Talk;
import com.common.gmacs.utils.GmacsUtils;


public class SearchedTalkDetailActivity extends BaseActivity implements AdapterView.OnItemClickListener {

    public static SearchedTalk sSearchedTalk;
    private static String sKeyword;

    private ListView resultContainer;
    private Talk talk;

    public static void start(Context context, String keyword, SearchedTalk searchedTalk) {
        sSearchedTalk = searchedTalk;
        sKeyword = keyword;
        context.startActivity(new Intent(context, SearchedTalkDetailActivity.class));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wchat_list_no_divider_layout);
    }

    @Override
    protected void initView() {
        resultContainer = (ListView) findViewById(R.id.wchat_list_layout);
    }

    @Override
    protected void initData() {
        if (sSearchedTalk != null) {
            talk = sSearchedTalk.getTalk();
            setTitle(talk.getOtherName());
            String[] keywords = sKeyword.split(" ");
            SearchTalkDetailAdapter adapter = new SearchTalkDetailAdapter(this, sSearchedTalk, keywords);
            TextView title = new TextView(this);
            title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            title.setTextColor(Color.parseColor("#9b9b9b"));
            title.setPadding(GmacsUtils.dipToPixel(10), GmacsUtils.dipToPixel(14.5f), 0, GmacsUtils.dipToPixel(7));
            title.setBackgroundColor(Color.parseColor("#f3f3f7"));
            title.setText(getString(R.string.wchat_searched_talk_detail_title, sSearchedTalk.getMessageList().size(), sKeyword));
            resultContainer.addHeaderView(title);
            resultContainer.setAdapter(adapter);
            resultContainer.setOnItemClickListener(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sSearchedTalk = null;
        sKeyword = null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        SearchBean searchBean = (SearchBean) parent.getAdapter().getItem(position);
        if (searchBean != null) {
            Message message = (Message) searchBean.getSearchable();
            Intent intent = GmacsUiUtil.createToChatActivity(this, talk.mTalkType, talk.mTalkOtherUserId, talk.mTalkOtherUserSource, message.mLocalId);
            startActivity(intent);
        }
    }
}
