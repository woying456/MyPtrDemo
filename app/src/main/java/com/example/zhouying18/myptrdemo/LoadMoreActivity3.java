package com.example.zhouying18.myptrdemo;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;

import com.example.zhouying18.myptrdemo.adapter.LoadMoreAdapter;
import com.jcodecraeer.xrecyclerview.XRecyclerView;

import java.util.ArrayList;

import in.srain.cube.views.ptr.util.TimeUtil;

/**
 * Created by zhouying18 on 2017/6/5.
 */

public class LoadMoreActivity3 extends AppCompatActivity {
    private XRecyclerView          recyclerView;
    private LoadMoreAdapter adapter;
    private int               mLoadCount = 0;
    private ArrayList<String> datas      = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_more3);
        initView();
        initData();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void initView() {
        recyclerView = (XRecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setRefreshTime(TimeUtil.getCurrentDateTimeShortString());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        dividerItemDecoration.setDrawable(ContextCompat.getDrawable(this, R.drawable.common_item_divider));
        recyclerView.addItemDecoration(dividerItemDecoration);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        adapter = new LoadMoreAdapter(this, recyclerView);
        recyclerView.setAdapter(adapter);

        recyclerView.setLoadingListener(new XRecyclerView.LoadingListener() {
            @Override
            public void onRefresh() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        recyclerView.refreshComplete();
                        recyclerView.setRefreshTime(TimeUtil.getCurrentDateTimeShortString());
                        datas.clear();
                        setDatas(0);
                        adapter.addAll(datas);
                    }
                }, 500);
            }

            @Override
            public void onLoadMore() {
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        recyclerView.loadMoreComplete();
                        mLoadCount++;
                        if (mLoadCount >= 3) {//模拟没有更多数据的情况
                            recyclerView.setNoMore(true);
                        } else {
                            int size = datas.size();
                            setDatas(size);
                            adapter.addAll(datas);
                        }
                    }
                }, 1000);
            }
        });
    }

    private void initData() {
        setDatas(0);
        adapter.addAll(datas);
    }

    private void setDatas(int beginNum) {
        for (int i = 0; i < 30; i++) {
            datas.add("此条目是第" + (beginNum + i) + "条");
        }
    }
}
