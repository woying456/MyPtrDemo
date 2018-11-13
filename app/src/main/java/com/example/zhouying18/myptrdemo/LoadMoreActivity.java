package com.example.zhouying18.myptrdemo;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.example.zhouying18.myptrdemo.adapter.LoadMoreAdapter;

import java.util.ArrayList;

import in.srain.cube.views.ptr.PtrClassicFrameLayout;
import in.srain.cube.views.ptr.PtrDefaultHandler2;
import in.srain.cube.views.ptr.PtrFrameLayout;
import in.srain.cube.views.ptr.util.TimeUtil;

public class LoadMoreActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PtrClassicFrameLayout ptrFrameLayout;
    private LoadMoreAdapter adapter;
    private ArrayList<String> datas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_more);
        initView();
        initData();
        initEvents();
    }

    private void initEvents() {
        ptrFrameLayout.setPtrHandler(new PtrDefaultHandler2() {
            @Override
            public void onRefreshBegin(PtrFrameLayout frame) {
                ptrFrameLayout.setRefreshTime(TimeUtil.getCurrentDateTimeShortString());
                datas.clear();
                setDatas(0);
                adapter.addAll(datas);
                frame.refreshComplete();
            }

            @Override
            public void onLoadMoreBegin(PtrFrameLayout frame) {
                int size = datas.size();
                setDatas(size);
                Log.d("zhouying18", "onLoadMoreBegin: hh");
                adapter.addAll(datas);
                frame.refreshComplete();
            }
        });
    }

    private void initData() {
        setDatas(0);
        adapter.addAll(datas);
    }

    private void setDatas(int beginNum) {
        for (int i = 0; i < 20; i++) {
            datas.add("此条目是第" + (beginNum + i) + "条");
        }
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
        ptrFrameLayout = (PtrClassicFrameLayout) findViewById(R.id.ptrFramelayout);
        ptrFrameLayout.setFooterLineEnable(false);
        ptrFrameLayout.setMode(PtrFrameLayout.Mode.BOTH);
        ptrFrameLayout.setRefreshTime(TimeUtil.getCurrentDateTimeShortString());
    }
}
