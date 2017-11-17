package com.android.gmacs.utils;

import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;

import com.common.gmacs.utils.ToastUtil;

import java.io.UnsupportedEncodingException;

public class InputLengthFilter implements InputFilter {

    private final int maxLength; // 最大字节数
    private String toastText = "超出最大输入限制";

    public InputLengthFilter(int maxLength) {
        this.maxLength = maxLength;
    }

    public InputLengthFilter(String toastText, int maxLength) {
        this.maxLength = maxLength;
        this.toastText = toastText;
    }

    private int getBytesLength(String string) {
        int length = 0;
        try {
            length = string.getBytes("GB2312").length;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return length;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end,
                               Spanned dest, int dstart, int dend) {
        int destCount = getBytesLength(dest.toString());
        int sourceCount = getBytesLength(source.toString());
        if (destCount + sourceCount > maxLength) {
            if (!TextUtils.isEmpty(toastText)) {
                ToastUtil.showToast(toastText);
            }
            return "";
        } else if (source.toString().contains("\n")) {
            return "";
        } else {
            return source;
        }
    }

}
