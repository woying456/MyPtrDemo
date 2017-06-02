package com.example.zhouying18.myptrdemo.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * @author stone
 * @date 16/8/10
 *
 * Adapter鸡肋
 *
 */
public abstract class BaseAdapter<T, VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH>
        implements AdapterOptions<T> {

    private final int TAG_KEY_ONCLICKLISTENER = 10;
    private final int TAG_KEY_ONLONGCLICKLISTENER = 11;

    private WeakReference<RecyclerView> mRef;

    protected Context                 mContext;
    protected LayoutInflater          mInflater;
    protected List<T>                 mList;
    private   OnItemClickListener     mOnItemClickListener;
    private   OnItemLongClickListener mOnItemLongClickListener;

    public BaseAdapter(Context context, RecyclerView recyclerView) {
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        mList = initData();
        mRef = new WeakReference<RecyclerView>(recyclerView);
    }

    public void setOnItemClickListener(OnItemClickListener<T> onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener<T> onItemLongClickListener) {
        mOnItemLongClickListener = onItemLongClickListener;
    }

    protected abstract List<T> initData();

    @Override
    public void onBindViewHolder(VH holder, int position) {
        setItemClickEvent(holder, position);
        setItemLongClickEvent(holder, position);

        onBindViewHolder(holder, position, mList.get(position));
    }

    private void setItemLongClickEvent(VH holder, int position) {
        if(mOnItemLongClickListener != null) {
            Object tag = holder.itemView.getTag(TAG_KEY_ONLONGCLICKLISTENER);
            OnLongClickListener<T> longClickListener;

            if(tag != null) { //复用OnLongClickListener
                longClickListener = (OnLongClickListener<T>)tag;
                longClickListener.resetData(holder.itemView, position, getItemId(position), mList.get(position));
            } else {
                longClickListener = new OnLongClickListener<>(mRef.get(), holder.itemView, position, getItemId(position), mList.get(position), mOnItemLongClickListener);
            }

            holder.itemView.setOnLongClickListener(longClickListener);
        }
    }

    private void setItemClickEvent(VH holder ,int position) {
        if(mOnItemClickListener != null) {
            Object tag = holder.itemView.getTag(TAG_KEY_ONCLICKLISTENER);
            OnClicklistener<T> clicklistener;

            if(tag != null) { //复用OnClickListener
                clicklistener = (OnClicklistener<T>)tag;
                clicklistener.resetData(holder.itemView, position, getItemId(position), mList.get(position));
            } else {
                clicklistener = new OnClicklistener<>(mRef.get(), holder.itemView, position, getItemId(position), mList.get(position), mOnItemClickListener);
            }

            holder.itemView.setOnClickListener(clicklistener);
        }
    }

    public abstract void onBindViewHolder(VH holder, int position, T item);

    @Override
    public int getItemCount() {
        return mList.size();
    }

    @Override
    public void addAll(List<T> list) {
        mList.clear();
        mList.addAll(list);
        notifyDataSetChanged();
    }

    @Override
    public void appendAll(List<T> list) {
        mList.addAll(list);
        notifyDataSetChanged();
    }

    @Override
    public void clear() {
        mList.clear();
        notifyDataSetChanged();
    }

    @Override
    public void addToFront(List<T> list) {
        mList.addAll(0, list);
        notifyDataSetChanged();
    }

    public interface OnItemClickListener<T> {
        void onItemClick(RecyclerView recyclerView, View itemView, int position, long id, T data);
    }

    public interface OnItemLongClickListener<T> {
        void onItemLongClick(RecyclerView recyclerView, View itemView, int position, long id, T data);
    }

    private static class OnClicklistener<T> implements View.OnClickListener {
        RecyclerView mRecyclerView;
        View         mItemView;
        int          mPosition;
        long         mId;
        T            mData;

        OnItemClickListener mOnItemClickListener;

        public OnClicklistener(RecyclerView recyclerView, View itemView, int position, long id, T data, OnItemClickListener<T> onItemLongClickListener) {
            mRecyclerView = recyclerView;
            mItemView = itemView;
            mPosition = position;
            mId = id;
            mData = data;
            mOnItemClickListener = onItemLongClickListener;
        }

        @Override
        public void onClick(View v) {
            mOnItemClickListener.onItemClick(mRecyclerView, mItemView, mPosition, mId, mData);
        }

        void resetData(View itemView, int position, long id, T data) {
            mItemView = itemView;
            mPosition = position;
            mId = id;
            mData = data;
        }
    }

    private static class OnLongClickListener<T> implements View.OnLongClickListener {
        RecyclerView mRecyclerView;
        View         mItemView;
        int          mPosition;
        long         mId;
        T            mData;

        OnItemLongClickListener mOnItemLongClickListener;

        public OnLongClickListener(RecyclerView recyclerView, View itemView, int position, long id, T data, OnItemLongClickListener<T> onItemLongClickListener) {
            mRecyclerView = recyclerView;
            mItemView = itemView;
            mPosition = position;
            mId = id;
            mData = data;
            mOnItemLongClickListener = onItemLongClickListener;
        }

        @Override
        public boolean onLongClick(View v) {
            mOnItemLongClickListener.onItemLongClick(mRecyclerView, mItemView, mPosition, mId, mData);
            return true;
        }

        void resetData(View itemView, int position, long id, T data) {
            mItemView = itemView;
            mPosition = position;
            mId = id;
            mData = data;
        }
    }
}
