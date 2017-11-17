package com.android.gmacs.emoji;

public class EmojiManager {
    private volatile static EmojiManager instance;
    private IEmojiParser mEmojiParser;

    /**
     * EmojiManager的单例入口
     *
     * @return EmojiManager单例对象
     */
    public static EmojiManager getInstance() {
        if (null == instance) {
            synchronized (EmojiManager.class) {
                if (null == instance) {
                    instance = new EmojiManager();
                }
            }
        }
        return instance;
    }

    private EmojiManager() {
    }

    /**
     * 设置emoji解析器
     *
     * @param emojiPaser emoji解析器
     */
    public void setEmojiPaser(IEmojiParser emojiPaser) {
        mEmojiParser = emojiPaser;
    }

    /**
     * 获取emoji解析器
     *
     * @return IEmojiParser emoji解析器
     */
    public IEmojiParser getEmojiParser() {
        return mEmojiParser;
    }
}
