package com.example.zhouying18.myptrdemo;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.andview.refreshview.XRefreshView;
import com.example.zhouying18.myptrdemo.adapter.LoadMoreAdapter;

import java.util.ArrayList;

import in.srain.cube.views.ptr.util.TimeUtil;

/**
 * Created by zhouying18 on 2017/6/5.
 */

public class LoadMoreActivity2 extends AppCompatActivity {
    private RecyclerView          recyclerView;
    private LoadMoreAdapter adapter;
    private XRefreshView xRefreshView;
    private int               mLoadCount = 0;
    private ArrayList<String> datas      = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_more2);
        initView();
        initData();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void initView() {
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        dividerItemDecoration.setDrawable(ContextCompat.getDrawable(this, R.drawable.common_item_divider));
        recyclerView.addItemDecoration(dividerItemDecoration);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        adapter = new LoadMoreAdapter(this, recyclerView);
        recyclerView.setAdapter(adapter);

        xRefreshView = (XRefreshView) findViewById(R.id.xRefreshView);
        xRefreshView.setRefreshTime(TimeUtil.getCurrentDateTimeShortString());
        //设置刷新完成以后，headerview固定的时间
        xRefreshView.setPinnedTime(1000);
        xRefreshView.setMoveForHorizontal(true);
        xRefreshView.setPullLoadEnable(true);
        xRefreshView.setAutoLoadMore(false);
//        adapter.setCustomLoadMoreView(new XRefreshViewFooter(this));
        xRefreshView.enableReleaseToLoadMore(true);
        xRefreshView.enableRecyclerViewPullUp(true);
        xRefreshView.enablePullUpWhenLoadCompleted(true);
        xRefreshView.setPinnedContent(true);
        xRefreshView.setXRefreshViewListener(new XRefreshView.SimpleXRefreshListener() {

            @Override
            public void onRefresh(boolean isPullDown) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        xRefreshView.setRefreshTime(TimeUtil.getCurrentDateTimeShortString());
                        datas.clear();
                        setDatas(0);
                        adapter.addAll(datas);
                        xRefreshView.stopRefresh();
                        xRefreshView.setLoadComplete(false);
                    }
                }, 500);
            }

            @Override
            public void onLoadMore(boolean isSilence) {
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        mLoadCount++;
                        if (mLoadCount >= 3) {//模拟没有更多数据的情况
                            xRefreshView.setLoadComplete(true);
                        } else {
                            // 刷新完成必须调用此方法停止加载
                            int size = datas.size();
                            setDatas(size);
                            adapter.addAll(datas);
                            xRefreshView.stopLoadMore(true);
                            //当数据加载失败 不需要隐藏footerview时，可以调用以下方法，传入false，不传默认为true
                            // 同时在Footerview的onStateFinish(boolean hideFooter)，可以在hideFooter为false时，显示数据加载失败的ui
//                            xRefreshView1.stopLoadMore(false);
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
