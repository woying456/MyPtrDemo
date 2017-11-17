package com.android.gmacs.view;

import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.AttributeSet;

import com.android.gmacs.activity.GmacsChatActivity;
import com.android.gmacs.activity.SelectForUserAtActivity;
import com.common.gmacs.core.GmacsConstant;
import com.common.gmacs.parse.message.Message;
import com.common.gmacs.parse.talk.Talk;
import com.common.gmacs.parse.talk.TalkType;

import java.util.Arrays;


public class AtEditText extends android.support.v7.widget.AppCompatEditText {

    private SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();

    private GmacsChatActivity mActivity;
    private Talk talk;
    private boolean isGroupTalk;

    private int preSelection;
    private boolean textWatcherEnabled = true;
    private TextWatcher textWatcher;

    public AtEditText(Context context) {
        super(context);
    }

    public AtEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AtEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void addTextChangedListener(TextWatcher textWatcher) {
        this.textWatcher = textWatcher;
    }

    public void setChatActivity(GmacsChatActivity activity) {
        mActivity = activity;
    }

    public void setTalk(Talk talk) {
        this.talk = talk;
        isGroupTalk = TalkType.isGroupTalk(talk);
        setText("");
    }

    void disableTextWatcher(String text) {
        textWatcherEnabled = false;
        if (isGroupTalk) {
            spannableStringBuilder.clear();
            spannableStringBuilder.insert(0, text);
        }
    }

    public void insertAtText(boolean alreadyHasAt, String name, String id, int source, String realName) {
        if (isGroupTalk) {
            int insertStart = getSelectionStart();
            if (alreadyHasAt) { // append name after single '@'
                spannableStringBuilder.delete(insertStart - 1, insertStart--);
            }
            AtSpan atSpan = new AtSpan(id, source, realName);
            String atContent = "@" + name + " ";
            spannableStringBuilder.insert(insertStart, atContent);
            spannableStringBuilder.setSpan(atSpan, insertStart,
                    insertStart + atContent.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            textWatcherEnabled = false;
            setText(spannableStringBuilder);
            setSelection(insertStart + atContent.length());
        }
    }

    public String replaceWithRealNameToSend() {
        if (isGroupTalk) {
            AtSpan[] spans = getSpans(0, spannableStringBuilder.length());
            if (spans.length == 0) {
                return spannableStringBuilder.toString();
            } else {
                int start, end;
                for (int i = spans.length - 1; i >= 0; i--) {
                    start = spannableStringBuilder.getSpanStart(spans[i]);
                    end = spannableStringBuilder.getSpanEnd(spans[i]);
                    spannableStringBuilder.removeSpan(spans[i]);
                    spannableStringBuilder.replace(start, end, "@" + spans[i].realName + " ");
                    spannableStringBuilder.setSpan(new AtSpan(spans[i].id, spans[i].source, spans[i].realName),
                            start, start + ("@" + spans[i].realName + " ").length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                return spannableStringBuilder.toString();
            }
        } else {
            return getText().toString();
        }
    }

    public Message.AtInfo[] getAllAtInfo() {
        if (isGroupTalk) {
            AtSpan[] spans = getSpans(0, spannableStringBuilder.length());
            if (spans.length > 0) {
                Message.AtInfo[] atInfo = new Message.AtInfo[spans.length];
                for (int i = 0; i < atInfo.length; i++) {
                    atInfo[i] = new Message.AtInfo(
                            spans[i].id,
                            spans[i].source,
                            spannableStringBuilder.getSpanStart(spans[i]),
                            spannableStringBuilder.getSpanEnd(spans[i]));
                }
                Arrays.asList(atInfo, new Message.AtComparator());
                return atInfo;
            }
        }
        return null;
    }

    private AtSpan[] getSpans(int start, int end) {
        if (!isGroupTalk) {
            return new AtSpan[]{};
        }
        return spannableStringBuilder.getSpans(start, end, AtSpan.class);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        super.addTextChangedListener(new TextWatcher() {
            int selection;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (textWatcher != null) {
                    textWatcher.beforeTextChanged(s, start, count, after);
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isGroupTalk && textWatcherEnabled) {
                    textWatcherEnabled = false;
                    if (before > 0) { // deleting
                        AtSpan[] spans = getSpans(start, start + before);
                        if (spans.length > 0) {
                            AtSpan firstSpan = spans[0];
                            AtSpan lastSpan = spans[spans.length - 1];
                            int firstSpanStart = spannableStringBuilder.getSpanStart(firstSpan);
                            int lastSpanEnd = spannableStringBuilder.getSpanStart(lastSpan);
                            int tempStart = start;
                            int tempEnd = start + before;
                            if (firstSpanStart < tempStart) {
                                tempStart = firstSpanStart;
                                textWatcherEnabled = true;
                            }
                            if (lastSpanEnd > tempEnd) {
                                tempEnd = lastSpanEnd;
                                textWatcherEnabled = true;
                            }
                            spannableStringBuilder.delete(tempStart, tempEnd);
                            selection = tempStart;
                        } else { // there's no '@'
                            spannableStringBuilder.delete(start, start + before);
                        }
                    }
                    if (count > 0) { // inserting
                        int end = start + count;
                        selection = end;
                        String changingText = s.subSequence(start, end).toString();
                        if (changingText.startsWith("@") && changingText.length() == 1) { // '@' + ?
                            spannableStringBuilder.insert(start, changingText);
                            if (mActivity != null && talk != null) {
                                Intent intent = new Intent(mActivity, SelectForUserAtActivity.class);
                                intent.putExtra(GmacsConstant.EXTRA_USER_ID, talk.mTalkOtherUserId);
                                intent.putExtra(GmacsConstant.EXTRA_USER_SOURCE, talk.mTalkOtherUserSource);
                                intent.putExtra("title", "选择提醒的人");
                                mActivity.startActivityForResult(intent, GmacsChatActivity.REQUEST_AT_CODE);
                            }
                        } else {
                            spannableStringBuilder.insert(start, changingText);
                        }
                    }
                }
                if (textWatcher != null) {
                    textWatcher.onTextChanged(s, start, before, count);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isGroupTalk) {
                    if (textWatcherEnabled) {
                        textWatcherEnabled = false;
                        setText(spannableStringBuilder);
                        setSelection(selection);
                    } else {
                        textWatcherEnabled = true;
                    }
                }

                if (textWatcher != null) {
                    textWatcher.afterTextChanged(s);
                }
            }
        });
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        if (!isGroupTalk) {
            return;
        }

        if (!(selStart == selEnd && selStart == 0) && selStart != spannableStringBuilder.length()) {
            AtSpan[] spans = getSpans(selStart, selEnd);
            if (selStart == selEnd) {
                if (spans.length > 0) {
                    int spanStart = spannableStringBuilder.getSpanStart(spans[0]);
                    int spanEnd = spannableStringBuilder.getSpanEnd(spans[0]);
                    if (selStart > spanStart && selStart < spanEnd) {
                        setSelection(preSelection);
                        return;
                    }
                }
            } else {
                if (spans.length > 0) {
                    AtSpan firstSpan = spans[0];
                    AtSpan lastSpan = spans[spans.length - 1];
                    int firstSpanStart = spannableStringBuilder.getSpanStart(firstSpan);
                    int lastSpanEnd = spannableStringBuilder.getSpanEnd(lastSpan);
                    if (firstSpanStart < selStart) {
                        selStart = firstSpanStart;
                    }
                    if (lastSpanEnd > selEnd) {
                        selEnd = lastSpanEnd;
                    }
                    setSelection(selStart, selEnd);
                }
            }
        }
        preSelection = selStart;
    }

}
