package com.example.zhouying18.myptrdemo.adapter;

import java.util.List;

/**
 * @author stone
 * @date 16/8/10
 */
public interface AdapterOptions<T> {
    void addAll(List<T> list);
    void appendAll(List<T> list);
    void clear();
    void addToFront(List<T> list);
}
