package com.android.gmacs.emoji;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.android.gmacs.R;

public class FaceLinearLayout extends LinearLayout {

    private EmojiLayoutBuilder emojiLayoutBuilder;

    public FaceLinearLayout(Context context) {
        super(context);
    }

    public FaceLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FaceLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setMessageEditView(EditText messageEditText) {
        emojiLayoutBuilder.setMessageEditView(messageEditText);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        RelativeLayout contentView = (RelativeLayout) findViewById(R.id.face_layout);
        emojiLayoutBuilder = new EmojiLayoutBuilder();
        View staticEmojiLayout = emojiLayoutBuilder.getEmojiLayout();
        contentView.removeAllViews();
        contentView.addView(staticEmojiLayout, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    public boolean faceViewShown() {
        return getVisibility() == View.VISIBLE;
    }

    public void hidden() {
        setVisibility(View.GONE);
    }

    public void show() {
        setVisibility(View.VISIBLE);
    }
}
