package com.example.zhouying18.myptrdemo.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.zhouying18.myptrdemo.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhouying18 on 2017/5/26.
 */

public class LoadMoreAdapter extends BaseAdapter<String, LoadMoreAdapter.MyViewHolder> {

    public LoadMoreAdapter(Context context, RecyclerView recyclerView) {
        super(context, recyclerView);
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = View.inflate(parent.getContext(), R.layout.activity_list_item, null);
        return new MyViewHolder(view);
    }

    @Override
    protected List<String> initData() {
        return new ArrayList<>();
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position, String item) {
        holder.textView.setText(item);
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public MyViewHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView.findViewById(R.id.text1);
        }
    }

}
