package com.android.gmacs.msg.view;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.gmacs.R;
import com.android.gmacs.activity.GmacsWebViewActivity;
import com.android.gmacs.utils.GmacsUiUtil;
import com.common.gmacs.msg.IMMessage;
import com.common.gmacs.msg.data.IMTipMsg;
import com.common.gmacs.msg.format.Format;

public class IMTipMsgView extends IMMessageView {
    public IMTipMsgView(IMMessage mIMMessage) {
        super(mIMMessage);
    }

    @Override
    protected View initView(LayoutInflater inflater, ViewGroup parentView, int maxWidth) {
        mContentView = inflater.inflate(R.layout.gmacs_adapter_msg_content_tip, parentView, false);
        ((TextView) mContentView).setMovementMethod(LinkMovementMethod.getInstance());
        return mContentView;
    }

    @Override
    public void setDataForView(IMMessage imMessage) {
        super.setDataForView(imMessage);
        IMTipMsg imTipMsg = (IMTipMsg) mIMMessage;
        if (imTipMsg.spannableString == null) {
            if (imTipMsg.mText instanceof SpannableStringBuilder) {
                imTipMsg.spannableString = (SpannableStringBuilder) imTipMsg.mText;
                extractRichFormat(imTipMsg.spannableString);
            } else {
                imTipMsg.spannableString = new SpannableStringBuilder(imTipMsg.mText);
            }
        }
        ((TextView) mContentView).setText(imTipMsg.spannableString);
    }

    private void extractRichFormat(Spannable spannable) {
        Format[] formats = spannable.getSpans(0, spannable.length(), Format.class);
        if (formats != null) {
            for (final Format format : formats) {

                int startIndex = spannable.getSpanStart(format);
                int endIndex = spannable.getSpanEnd(format);

                ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.parseColor("#" + format.color));
                spannable.setSpan(colorSpan, startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                if (format.isBold) {
                    StyleSpan styleSpan = new StyleSpan(Typeface.BOLD);
                    spannable.setSpan(styleSpan, startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                ClickableSpan clickableSpan = new ClickableSpan() {
                    @Override
                    public void onClick(View view) {
                        Bundle bundle = new Bundle();
                        bundle.putString(GmacsWebViewActivity.EXTRA_URL, format.url);
                        String webViewTitle = getContentView().getContext().getResources().getString(R.string.webview_title);
                        bundle.putString(GmacsWebViewActivity.EXTRA_TITLE, webViewTitle);
                        GmacsUiUtil.startBrowserActivity(mChatActivity, bundle);
//                        Intent intent = new Intent();
//                        intent.setAction(Intent.ACTION_VIEW);
//                        intent.setData(Uri.parse(format.url));
//                        try {
//                            view.getContext().startActivity(intent);
//                        } catch (ActivityNotFoundException e) {
//                            try {
//                                intent = new Intent(mChatActivity, Class.forName(GmacsUiUtil.getBrowserClassName()));
//                                intent.putExtra(GmacsWebViewActivity.EXTRA_TITLE, getContentView().getContext().getResources().getString(R.string.webview_title));
//                                intent.putExtra(GmacsWebViewActivity.EXTRA_URL, format.url);
//                                mChatActivity.startActivity(intent);
//                            } catch (ClassNotFoundException e1) {
//                                e1.printStackTrace();
//                            }
//                        }
                    }

                    @Override
                    public void updateDrawState(TextPaint ds) {
                        ds.setUnderlineText(false);
                    }
                };
                spannable.setSpan(clickableSpan, startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }
}
