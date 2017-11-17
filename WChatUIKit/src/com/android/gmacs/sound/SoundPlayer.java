package com.android.gmacs.sound;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.os.PowerManager;
import android.text.TextUtils;

import com.common.gmacs.utils.GmacsConfig;
import com.common.gmacs.utils.GmacsEnvi;

public class SoundPlayer implements OnCompletionListener, OnErrorListener, MediaPlayer.OnPreparedListener {

    private static SoundPlayer mSoundPlayer;
    private MediaPlayer mPlayer;
    private AudioManager mAudioManager;
    private boolean isSpeakerphoneOn;

    /**
     * 用于记录播放的进度
     */
    private int currentPosition;

    /**
     * 主要用于聊天界面，标志哪行声音在被播放，-1表明播放的是普通不需要显示动画的声音动作
     */
    private long mCurrentPlayId = -2;
    private VoiceCompletion mCurrentListener;

    /**
     * 声音是否正在播放
     */
    private boolean isSoundPlaying;
    /**
     * 记录当前正在播放的文件的位置
     */
    private String currentAudioFilePath;

    private SoundPlayer() {
        isSpeakerphoneOn = (boolean) GmacsConfig.ClientConfig.getParam("isSpeakerphoneOn", true);
    }

    public static SoundPlayer getInstance() {
        if (mSoundPlayer == null) {
            synchronized (SoundPlayer.class) {
                if (mSoundPlayer == null) {
                    mSoundPlayer = new SoundPlayer();
                }
            }
        }
        return mSoundPlayer;
    }

    public boolean isSoundPlaying() {
        return isSoundPlaying;
    }

    public boolean isSpeakerphoneOn() {
        return isSpeakerphoneOn;
    }

    public void setSpeakerphoneOn(final boolean on) {
        try {
            if (mAudioManager == null) {
                mAudioManager = (AudioManager) GmacsEnvi.appContext.getSystemService(Context.AUDIO_SERVICE);
            }
            if (mPlayer != null && mPlayer.isPlaying()) {
                currentPosition = mPlayer.getCurrentPosition();

                mPlayer.stop();
                mPlayer.release();
                mPlayer = null;
                // 为了解决小米5无法切换的问题
                mPlayer = new MediaPlayer();
                mPlayer.setWakeMode(GmacsEnvi.appContext, PowerManager.PARTIAL_WAKE_LOCK);
                mPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);

                mPlayer.reset();
                mPlayer.setDataSource(currentAudioFilePath);
                mPlayer.setVolume(1.0f, 1.0f);
                mPlayer.setLooping(false);

                mPlayer.setOnErrorListener(this);
                mPlayer.setOnCompletionListener(this);

                mPlayer.prepareAsync();

                mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        isSoundPlaying = true;
                        mAudioManager.setSpeakerphoneOn(on);
                        if (currentPosition > 0) {
                            mp.seekTo(currentPosition);
                        }
                        mp.start();

                        currentPosition = 0;
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveAudioMessageRoute(boolean isSpeakerphoneOn) {
        this.isSpeakerphoneOn = isSpeakerphoneOn;
        GmacsConfig.ClientConfig.setParam("isSpeakerphoneOn", isSpeakerphoneOn);
    }

    public boolean isWiredHeadsetOn() {
        if (mAudioManager == null) {
            mAudioManager = (AudioManager) GmacsEnvi.appContext.getSystemService(Context.AUDIO_SERVICE);
        }
        return mAudioManager.isWiredHeadsetOn();
    }

    /**
     * 用在普通的播放声音，播放声音时不需要动画效果
     *
     * @param fileName
     */
    private void startPlaying(String fileName) {
        if (mAudioManager == null) {
            mAudioManager = (AudioManager) GmacsEnvi.appContext.getSystemService(Context.AUDIO_SERVICE);
        }

        /*
          记录当前正在播放的文件的位置，切换听筒或者是扬声器的时候会用到
         */
        currentAudioFilePath = fileName;

        try {
            if (mPlayer == null) {
                mPlayer = new MediaPlayer();
                mPlayer.setWakeMode(GmacsEnvi.appContext, PowerManager.PARTIAL_WAKE_LOCK);
                mPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
            }

            mPlayer.reset();
            mPlayer.setDataSource(fileName);
            mPlayer.setVolume(1.0f, 1.0f);
            mPlayer.setLooping(false);

            /*
              播放的时候声音模式以配置文件的配置为准
             */
            boolean defaultMode = (boolean) GmacsConfig.ClientConfig.getParam("isSpeakerphoneOn", true);
            mAudioManager.setSpeakerphoneOn(defaultMode);

            mPlayer.setOnErrorListener(this);
            mPlayer.setOnCompletionListener(this);
            mPlayer.prepareAsync();
            mPlayer.setOnPreparedListener(this);
        } catch (Exception e) {
            notifyFinalState(false);
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        isSoundPlaying = true;

        if (currentPosition > 0) {
            mp.seekTo(currentPosition);
        }
        mp.start();

        currentPosition = 0;
    }

    private void stopPlay() {
        if (mPlayer != null) {
            if (mPlayer.isPlaying()) {
                mPlayer.stop();

                currentPosition = 0;
                currentAudioFilePath = null;
            }
        }
    }

    /**
     * 停止声音播放，并且把动画停掉
     */
    public void stopPlayAndAnimation() {
        if (mPlayer != null) {
            if (mPlayer.isPlaying()) {
                mPlayer.stop();
            }
        }
        notifyFinalState(false);
    }

    private void notifyFinalState(boolean isNormal) {
        currentAudioFilePath = null;
        currentPosition = 0;

        mCurrentPlayId = -2;
        isSoundPlaying = false;
        if (mCurrentListener != null) {
            mCurrentListener.onCompletion(mPlayer, isNormal);
        }
        if (mAudioManager == null) {
            mAudioManager = (AudioManager) GmacsEnvi.appContext.getSystemService(Context.AUDIO_SERVICE);
        }
        mAudioManager.abandonAudioFocus(null);
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
    }

    public void startPlaying(Uri uri) {
        if (uri != null) {
            if (mAudioManager == null) {
                mAudioManager = (AudioManager) GmacsEnvi.appContext.getSystemService(Context.AUDIO_SERVICE);
            }

            try {
                if (mPlayer == null) {
                    mPlayer = new MediaPlayer();
                }
                mPlayer.reset();
                mPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
                mPlayer.setDataSource(GmacsEnvi.appContext, uri);
                mPlayer.setVolume(1.0f, 1.0f);
                mPlayer.setLooping(false);
                mPlayer.prepareAsync();
                mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        mp.start();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 用在聊天界面，播放声音，需要动画效果
     *
     * @param fileName
     * @param listener position 列表中哪行声音被播放
     */
    public void startPlaying(String fileName, VoiceCompletion listener, long msgLocalId) {
        if (TextUtils.isEmpty(fileName)) {
            mCurrentPlayId = -2;
            listener.onCompletion(mPlayer, false);
            return;
        }

        // 说明当前行正在播放，停掉
        if (msgLocalId == mCurrentPlayId) {
            stopPlay();
            notifyFinalState(false);
            return;
        } else {
            if (isSoundPlaying) {
                stopPlay();
                notifyFinalState(false);
            }
        }
        mCurrentListener = listener;
        mCurrentPlayId = msgLocalId;
        startPlaying(fileName);
    }

    public void autoStartPlaying(String fileName, VoiceCompletion listener, long msgId) {
        if (TextUtils.isEmpty(fileName)) {
            mCurrentPlayId = -2;
            listener.onCompletion(mPlayer, false);
            return;
        }
        mCurrentListener = listener;
        mCurrentPlayId = msgId;
        startPlaying(fileName);
    }


    @Override
    public void onCompletion(MediaPlayer mp) {
        notifyFinalState(true);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        if (mp.isPlaying()) {
            mp.stop();
        }
        notifyFinalState(false);
        return false;
    }

    public void destroy() {
        if (mPlayer != null) {
            if (mPlayer.isPlaying()) {
                mPlayer.stop();
            }
            mPlayer.release();
            mPlayer = null;

            currentPosition = 0;
            currentAudioFilePath = null;
        }
        mAudioManager = null;
        mCurrentPlayId = -2;
        mCurrentListener = null;
        isSoundPlaying = false;
    }

    /**
     * 聊天界面正在播放声音索引值，-1表示没有
     *
     * @return
     */
    public long currentPlayId() {
        return mCurrentPlayId;
    }

    public interface VoiceCompletion {
        void onCompletion(MediaPlayer mp, boolean isNormal);
    }
}
