package com.android.gmacs.msg.view;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import com.android.gmacs.R;
import com.android.gmacs.activity.GmacsWebViewActivity;
import com.android.gmacs.emoji.EmojiManager;
import com.android.gmacs.emoji.IEmojiParser;
import com.android.gmacs.utils.GmacsUiUtil;
import com.android.gmacs.view.GmacsDialog;
import com.common.gmacs.core.ClientManager;
import com.common.gmacs.msg.IMMessage;
import com.common.gmacs.msg.data.IMTextMsg;
import com.common.gmacs.msg.format.Format;
import com.common.gmacs.parse.contact.GroupMember;
import com.common.gmacs.parse.message.Message;
import com.common.gmacs.utils.CopyPasteContent;
import com.common.gmacs.utils.StringUtil;
import com.common.gmacs.utils.ToastUtil;

import java.util.regex.Matcher;

public class IMTextMsgView extends IMMessageView {

    private TextView msgContentTv;

    private boolean mIsShowClickEvent = true;

    public IMTextMsgView(IMMessage mIMMessage) {
        super(mIMMessage);
    }

    @Override
    protected View initView(LayoutInflater inflater, ViewGroup parentView, int maxWidth) {
        //自己发送消息文本展现的样式
        if (mIMMessage.message.mIsSelfSendMsg) {
            mContentView = inflater.inflate(R.layout.gmacs_adapter_msg_content_right_text, parentView, false);
            msgContentTv = (TextView) mContentView;
        } else {
            mContentView = inflater.inflate(R.layout.gmacs_adapter_msg_content_left_text, parentView, false);
            msgContentTv = (TextView) mContentView;
        }
        msgContentTv.setMaxWidth(maxWidth * 4 / 5);
        msgContentTv.setMovementMethod(LinkMovementMethod.getInstance());
        msgContentTv.setOnLongClickListener(new View.OnLongClickListener() {//长按复制粘贴
            @Override
            public boolean onLongClick(View v) {
                mIsShowClickEvent = false;
                final GmacsDialog.Builder dialog = new GmacsDialog.Builder(getContentView().getContext(), GmacsDialog.Builder.DIALOG_TYPE_LIST_NO_BUTTON);
                dialog.initDialog(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        switch (position) {
                            case 0:// 复制消息
                                String copyContent;
                                if (((IMTextMsg) mIMMessage).spannableString != null) {
                                    copyContent = ((IMTextMsg) mIMMessage).spannableString.toString();
                                } else {
                                    copyContent = mIMMessage.getPlainText();
                                }
                                CopyPasteContent.copy(copyContent, mChatActivity);
                                ToastUtil.showToast(R.string.copied);
                                break;
                            case 1:// 删除消息
                                deleteIMMessageView();
                                dialog.dismiss();
                                break;
                            default:
                                break;
                        }
                        dialog.dismiss();
                    }
                }).setListTexts(new String[]{mChatActivity.getString(R.string.copy_message), mChatActivity.getString(R.string.delete_message)}).create().show();
                return true;
            }
        });
        return mContentView;
    }

    @Override
    public void setDataForView(IMMessage imMessage) {
        super.setDataForView(imMessage);
        IMTextMsg textMsg = (IMTextMsg) mIMMessage;
        if (textMsg.spannableString == null) {
            if (textMsg.mMsg instanceof SpannableStringBuilder) {
                textMsg.spannableString = (SpannableStringBuilder) textMsg.mMsg;
                extractRichFormat(textMsg.spannableString);
            } else {
                textMsg.spannableString = new SpannableStringBuilder(textMsg.mMsg);
            }
            extractUrlAndPhoneNumber(textMsg.spannableString);
            extractAtInfo(textMsg);
            extractEmoji(textMsg);
        }
        msgContentTv.setText(textMsg.spannableString);
    }

    private void extractEmoji(IMTextMsg textMsg) {
        IEmojiParser iEmojiParser = EmojiManager.getInstance().getEmojiParser();
        if (iEmojiParser != null) {
            iEmojiParser.replaceAllEmoji(textMsg.spannableString, 20);
        }
    }

    private void extractAtInfo(IMTextMsg textMsg) {
        if (mIMMessage.message.atInfoArray != null) {
            for (Message.AtInfo atInfo : textMsg.message.atInfoArray) {
                String name = "";
                if (atInfo.userSource >= 10000) {
                    name = "@所有人 ";
                } else if (ClientManager.getInstance().getGmacsUserInfo().userId.equals(atInfo.userId)
                        && ClientManager.getInstance().getGmacsUserInfo().userSource == atInfo.userSource) {
                    name = "@" + ClientManager.getInstance().getGmacsUserInfo().userName + " ";
                } else {
                    GroupMember groupMember = mChatActivity.getUserInfoFromCache(atInfo.userSource, atInfo.userId);
                    if (groupMember != null) {
                        name = "@" + groupMember.getNameToShow() + " ";
                    }
                }
                if (atInfo.startPosition >= 0 && atInfo.endPosition <= textMsg.spannableString.length() && atInfo.startPosition < atInfo.endPosition) {
                    if (!TextUtils.isEmpty(name)) {
                        textMsg.spannableString.replace(atInfo.startPosition, atInfo.endPosition, name);
                    }
                }
            }
        }
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
                        if (mIsShowClickEvent) {
                            openBrowser(mChatActivity, format.url);
//                            Intent intent = new Intent();
//                            intent.setAction(Intent.ACTION_VIEW);
//                            intent.setData(Uri.parse(format.url));
//                            try {
//                                view.getContext().startActivity(intent);
//                            } catch (ActivityNotFoundException e) {
//                                try {
//                                    intent = new Intent(mChatActivity, Class.forName(GmacsUiUtil.getBrowserClassName()));
//                                    intent.putExtra(GmacsWebViewActivity.EXTRA_TITLE, getContentView().getContext().getResources().getString(R.string.webview_title));
//                                    intent.putExtra(GmacsWebViewActivity.EXTRA_URL, format.url);
//                                    mChatActivity.startActivity(intent);
//                                } catch (ClassNotFoundException e1) {
//                                    e1.printStackTrace();
//                                }
//                            }
                        } else {
                            mIsShowClickEvent = true;
                        }
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

    private void openBrowser(Activity activity, String url) {
        Bundle bundle = new Bundle();
        bundle.putString(GmacsWebViewActivity.EXTRA_URL, url);
        String webViewTitle = getContentView().getContext().getResources().getString(R.string.webview_title);
        bundle.putString(GmacsWebViewActivity.EXTRA_TITLE, webViewTitle);
        GmacsUiUtil.startBrowserActivity(mChatActivity, bundle);
    }

    private void extractUrlAndPhoneNumber(Spannable spannable) {
        final IMTextMsg textMsg = (IMTextMsg) mIMMessage;
        // 匹配网络地址
        Matcher m = StringUtil.getUrlPattern().matcher(textMsg.mMsg);
        if (m.find()) {
            final String url = textMsg.mMsg.toString().substring(m.start(), m.end());
            spannable.setSpan(new ClickableSpan() {
                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    if (textMsg.message.mIsSelfSendMsg) {
                        ds.setColor(getContentView().getContext().getResources().getColor(R.color.chat_right_super_link));
                    } else {
                        ds.setColor(getContentView().getContext().getResources().getColor(R.color.chat_left_super_link));
                    }
                }

                // 在onClick方法中可以编写单击链接时要执行的动作
                @Override
                public void onClick(View widget) {
                    if (mIsShowClickEvent) {
                        openBrowser(mChatActivity, url);
//                        Intent intent;
//                        try {
//                            intent = new Intent();
//                            intent.setAction(Intent.ACTION_VIEW);
//                            intent.setData(Uri.parse(url));
//                            mChatActivity.startActivity(intent);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                            try {
//                                intent = new Intent(mChatActivity, Class.forName(GmacsUiUtil.getBrowserClassName()));
//                                intent.putExtra(GmacsWebViewActivity.EXTRA_TITLE, getContentView().getContext().getResources().getString(R.string.webview_title));
//                                intent.putExtra(GmacsWebViewActivity.EXTRA_URL, url);
//                                mChatActivity.startActivity(intent);
//                            } catch (ClassNotFoundException cnfe) {
//                                cnfe.printStackTrace();
//                            }
//                        }
                    } else {
                        mIsShowClickEvent = true;
                    }
                }
            }, m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // 匹配电话号码
        m = StringUtil.getPhonePattern().matcher(textMsg.mMsg);
        while (m.find()) {
            final String subNumbers = m.group();
            if (StringUtil.getNumberPattern().matcher(subNumbers).matches()) {
                spannable.setSpan(new ClickableSpan() {
                    @Override
                    public void updateDrawState(TextPaint ds) {
                        super.updateDrawState(ds);
                        if (textMsg.message.mIsSelfSendMsg) {
                            ds.setColor(getContentView().getContext().getResources().getColor(R.color.chat_right_super_link));
                        } else {
                            ds.setColor(getContentView().getContext().getResources().getColor(R.color.chat_left_super_link));
                        }
                        ds.setUnderlineText(false);
                    }

                    // 在onClick方法中可以编写单击链接时要执行的动作
                    @Override
                    public void onClick(View widget) {
                        if (mIsShowClickEvent) {
                            final GmacsDialog.Builder dialog = new GmacsDialog.Builder(getContentView().getContext(), GmacsDialog.Builder.DIALOG_TYPE_LIST_NO_BUTTON);
                            dialog.initDialog(new AdapterView.OnItemClickListener() {

                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    switch (position) {
                                        case 0:
                                            // 呼叫,如果之有一个可用的号码，这直接使用这个号码拨出
                                            mChatActivity.startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + subNumbers)));
                                            break;
                                        case 1:
                                            CopyPasteContent.copy(subNumbers, mChatActivity);
                                            ToastUtil.showToast(R.string.copied);
                                            break;
                                        default:
                                            break;
                                    }
                                    dialog.dismiss();
                                }
                            }).setListTexts(new String[]{mChatActivity.getString(R.string.call), mChatActivity.getString(R.string.copy)}).create().show();

                        } else {
                            mIsShowClickEvent = true;
                        }
                    }
                }, m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }
}
