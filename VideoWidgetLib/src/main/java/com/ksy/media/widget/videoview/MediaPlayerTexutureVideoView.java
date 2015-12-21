package com.ksy.media.widget.videoview;

import java.io.File;
import java.io.IOException;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewGroup.LayoutParams;

import com.ksy.media.widget.util.Constants;
import com.ksy.media.widget.util.IStop;
import com.ksy.media.widget.util.MD5Util;
import com.ksy.media.widget.util.VideoViewConfig;
import com.ksy.media.widget.util.IMediaPlayerControl;
import com.ksy.media.widget.util.IPowerStateListener;
import com.ksy.media.widget.controller.MediaPlayerBaseControllerView.MediaPlayerController;
import com.ksy.media.widget.ui.common.MediaPlayerMovieRatioView;
import com.ksy.media.widget.util.ScreenResolution;
import com.ksyun.media.player.IMediaPlayer;
import com.ksyun.media.player.KSYMediaPlayer;
import com.ksyun.media.player.MediaInfo;

import android.view.TextureView.SurfaceTextureListener;
import android.widget.Toast;

/**
 * Displays a video file. The VideoView class can load images from various
 * sources (such as resources or content providers), takes care of computing its
 * measurement from the video so that it can be used in any layout manager, and
 * provides various display options such as scaling and tinting.
 */
public class MediaPlayerTexutureVideoView extends TextureView implements
        IMediaPlayerControl, IPowerStateListener, SurfaceTextureListener {

    private static final String TAG = MediaPlayerTexutureVideoView.class
            .getName();
    public volatile boolean isReleasing = false;
    public volatile boolean isNeedHandlerOpen = false;
    private Uri mUri;
    private long mDuration;
    private MediaInfo mMediaInfo;
    private String mUserAgent;
    private static final int LOW_LATENCY_NO = 0;
    private static final int LOW_LATENCY_DROP_AUDIO = 1;
    private static final int LOW_LATENCY_DROP_AUDIO_VIDEO = 2;
    private static final int STATE_ERROR = -1;
    private static final int STATE_IDLE = 0;
    private static final int STATE_PREPARING = 1;
    private static final int STATE_PREPARED = 2;
    public static final int STATE_PLAYING = 3;
    private static final int STATE_PAUSED = 4;
    private static final int STATE_PLAYBACK_COMPLETED = 5;
    private static final int STATE_SUSPEND = 6;
    private static final int STATE_RESUME = 7;
    private static final int STATE_SUSPEND_UNSUPPORTED = 8;

    public int mCurrentState = STATE_IDLE;
    private int mTargetState = STATE_IDLE;

    private int mVideoLayout = VIDEO_LAYOUT_SCALE;
    public static final int VIDEO_LAYOUT_ORIGIN = 0;
    public static final int VIDEO_LAYOUT_SCALE = 1;
    public static final int VIDEO_LAYOUT_STRETCH = 2;
    public static final int VIDEO_LAYOUT_ZOOM = 3;

    protected static final String KEY_SOUCE_IP = "source_ip";

    // private SurfaceHolder mSurfaceHolder = null;
    private SurfaceTexture mSurfaceTexture = null;
    private IMediaPlayer mMediaPlayer = null;
    private int mVideoWidth;
    private int mVideoHeight;
    private int mVideoSarNum;
    private int mVideoSarDen;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    //    private OnDebugInfoListener mOnDebugInfoListener;
    private IMediaPlayer.OnCompletionListener mOnCompletionListener;
    private IMediaPlayer.OnPreparedListener mOnPreparedListener;
    private IMediaPlayer.OnErrorListener mOnErrorListener;
    private IMediaPlayer.OnSeekCompleteListener mOnSeekCompleteListener;
    private IMediaPlayer.OnInfoListener mOnInfoListener;
    //    private OnDRMRequiredListener mOnDRMRequiredListener;
    private IMediaPlayer.OnBufferingUpdateListener mOnBufferingUpdateListener;
    //    private OnSurfaceListener mOnSurfaceListener;
    private MediaPlayerController mMediaPlayerController;
//    private OnNetSpeedListener mOnNetSpeedListener;

    private int mCurrentBufferPercentage;
    // private long mSeekWhenPrepared;
    private Context mContext;
    KSYMediaPlayer ksyMediaPlayer = null;

    private boolean mHasPrepared = false;
    private VideoViewConfig videoViewConfig = VideoViewConfig.getInstance();
    private IStop callBack;
    private Surface mSurface;
    private boolean misTexturePowetEvent;
    private boolean isOpening;
    private boolean mNeedUnlock;

    public MediaPlayerTexutureVideoView(Context context) {

        super(context);
        initVideoView(context);
    }

    public MediaPlayerTexutureVideoView(Context context, AttributeSet attrs) {

        this(context, attrs, 0);
        // LogClient.getInstance(context).put(LogRecord.getInstance()
        // .getUserDefinedJson(map, field, type));
    }

    public MediaPlayerTexutureVideoView(Context context, AttributeSet attrs,
                                        int defStyle) {

        super(context, attrs, defStyle);
        initVideoView(context);
    }

    // stop get flow timer
    public void setCallBack(IStop callBack) {
        this.callBack = callBack;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int width = getDefaultSize(mVideoWidth, widthMeasureSpec);
        int height = getDefaultSize(mVideoHeight, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    /**
     * @param layout
     * @Description 设置视频的大小
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    public void setVideoLayout(int layout) {

        Log.d(Constants.LOG_TAG, "SetVideoLayout ,Mode = " + layout);
        LayoutParams lp = getLayoutParams();
        Pair<Integer, Integer> res = ScreenResolution.getResolution(mContext);
        int windowWidth = res.first.intValue(), windowHeight = res.second
                .intValue();
        float windowRatio = windowWidth / (float) windowHeight;
        int sarNum = mVideoSarNum;
        int sarDen = mVideoSarDen;
        if (mVideoHeight > 0 && mVideoWidth > 0) {
            float videoRatio = ((float) (mVideoWidth)) / mVideoHeight;
            if (sarNum > 0 && sarDen > 0)
                videoRatio = videoRatio * sarNum / sarDen;
            mSurfaceHeight = mVideoHeight;
            mSurfaceWidth = mVideoWidth;

            if (layout == MediaPlayerMovieRatioView.MOVIE_RATIO_MODE_16_9) {
                // 16:9
                float target_ratio = 16.0f / 9.0f;
                float dh = windowHeight;
                float dw = windowWidth;
                if (windowRatio < target_ratio) {
                    dh = dw / target_ratio;
                } else {
                    dw = dh * target_ratio;
                }
                lp.width = (int) dw;
                lp.height = (int) dh;

            } else if (layout == MediaPlayerMovieRatioView.MOVIE_RATIO_MODE_4_3) {
                // 4:3
                float target_ratio = 4.0f / 3.0f;
                float source_height = windowHeight;
                float source_width = windowWidth;
                if (windowRatio < target_ratio) {
                    source_height = source_width / target_ratio;
                } else {
                    source_width = source_height * target_ratio;
                }
                lp.width = (int) source_width;
                lp.height = (int) source_height;
            }
            /*
             * else if (layout ==
			 * MediaPlayerMovieRatioView.MOVIE_RATIO_MODE_ORIGIN &&
			 * mSurfaceWidth < windowWidth && mSurfaceHeight < windowHeight) {
			 * // origin lp.width = (int) (mSurfaceHeight * videoRatio);
			 * lp.height = mSurfaceHeight; } else if (layout ==
			 * MediaPlayerMovieRatioView.MOVIE_RATIO_MODE_FULLSCREEN) { //
			 * fullscreen lp.width = (windowRatio < videoRatio) ? windowWidth :
			 * (int) (videoRatio * windowHeight); lp.height = (windowRatio >
			 * videoRatio) ? windowHeight : (int) (windowWidth / videoRatio); }
			 */

            setLayoutParams(lp);
            // getHolder().setFixedSize(mSurfaceWidth, mSurfaceHeight);
            getSurfaceTexture().setDefaultBufferSize(mSurfaceWidth,
                    mSurfaceHeight);

        }
        mVideoLayout = layout;
    }

    private void initVideoView(Context ctx) {

        mContext = ctx;
        mVideoWidth = 0;
        mVideoHeight = 0;
        mVideoSarNum = 0;
        mVideoSarDen = 0;
        // getHolder().addCallback(mSHCallback);
        setSurfaceTextureListener(this);
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
        mCurrentState = STATE_IDLE;
        mTargetState = STATE_IDLE;
        if (ctx instanceof Activity)
            ((Activity) ctx).setVolumeControlStream(AudioManager.STREAM_MUSIC);
        // handler = new ReleaseHandler();
    }

    public boolean isValid() {
        // return (mSurfaceHolder != null &&
        // mSurfaceHolder.getSurface().isValid());
        return (mSurfaceTexture != null);
    }

    public void setVideoPath(String path) {

        Log.i(Constants.LOG_TAG, "setVideoPath : path=" + path);
        setVideoURI(Uri.parse(path));
    }

    public void setVideoURI(Uri uri) {

        mUri = uri;
        openVideo();
        requestLayout();
        invalidate();
    }

    public void setUserAgent(String ua) {

        mUserAgent = ua;
    }

    public void stopPlayback() {

        Log.i(Constants.LOG_TAG, "on stop ");
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mCurrentState = STATE_IDLE;
            mTargetState = STATE_IDLE;
        }
    }

    private void openVideo() {
//        isOpening = true;
        Log.i(Constants.LOG_TAG, "openVideo");
        if (mUri == null) {
            return;
        }

        Intent i = new Intent("com.android.music.musicservicecommand");
        i.putExtra("command", "pause");
        mContext.sendBroadcast(i);
        // release(false);
        try {
            mDuration = -1;
            mCurrentBufferPercentage = 0;
            mMediaInfo = null;

            if (mUri != null) {
                /**
                 * cpu占用率每50秒取一次，用户可自定义; true为开启日志 开关，false关闭开关;
                 * waqu为用户key可以自定义的，需要自己传，只能是 字母（不分大小写），其他的都是无效的
                 */
                String timeSec = String.valueOf(System.currentTimeMillis() / 1000);
                String skSign = MD5Util.md5("sb56661c74aabc0df83d723a8d3eba69" + timeSec);
                ksyMediaPlayer = new KSYMediaPlayer.Builder(mContext.getApplicationContext()).setAppId("QYA0788DA337D2E0EC45").setAccessKey("a8b4dff4665f6e69ba6cbeb8ebadc9a3").setSecretKeySign(skSign).setTimeSec(timeSec).build();

//                ksyMediaPlayer
//                        .setAvOption(AvFormatOption_HttpDetectRangeSupport.Disable);
//                ksyMediaPlayer.setOverlayFormat(AvFourCC.SDL_FCC_RV32);
                // ksyMediaPlayer.setAvCodecOption("skip_loop_filter", "48");
//                ksyMediaPlayer.setFrameDrop(0);
//                ksyMediaPlayer
//                        .setBufferSize(IMediaPlayer.MEDIA_BUFFERSIZE_DEFAULT);
//                ksyMediaPlayer
//                        .setAnalyseDuration(IMediaPlayer.MEDIA_ANALYSE_DURATION_DEFAULT * 2);
//                ksyMediaPlayer.setTimeout(IMediaPlayer.MEDIA_TIME_OUT_DEFAULT);
                // 建议直播模式下启动低时延模式setLowDelayEnabled，缓冲时间大于start_drop_frame_threshold时开启，缓冲时间小于stop_drop_frame_threshold关闭
                Log.d(Constants.LOG_TAG, "controlDelay.isStream() "
                        + videoViewConfig.isStream());
                // 设置暂停状态下仍然缓存
//                ksyMediaPlayer.setCacheInPause(true);
                // 设置缓存路径
                ksyMediaPlayer.clearCachedFiles(new File(Environment
                        .getExternalStorageDirectory(), "ksy_cached_temp")
                        .getPath());
                ksyMediaPlayer.setCachedDir(new File(Environment
                        .getExternalStorageDirectory(), "ksy_cached_temp")
                        .getPath());
//                if (mUserAgent != null) {
//                    ksyMediaPlayer.setAvFormatOption("user_agent", mUserAgent);
//                }
            } else {
                Log.e(Constants.LOG_TAG, "mUri is null ");
            }

            mMediaPlayer = ksyMediaPlayer;
            mMediaPlayer.setOnPreparedListener(mPreparedListener);
            mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
            mMediaPlayer.setOnCompletionListener(mCompletionListener);
            mMediaPlayer.setOnErrorListener(mErrorListener);
            mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
//            mMediaPlayer.setOnNetSpeedUpdateListener(mNetSpeedListener);
            mMediaPlayer.setOnInfoListener(mInfoListener);
//            mMediaPlayer.setOnDRMRequiredListener(mDRMRequiredListener);
            mMediaPlayer.setOnSeekCompleteListener(mSeekCompleteListener);
//            mMediaPlayer.setOnDebugInfoListener(mDebugInfoListener);
            // For test add header
            // Map<String, String> headers = new HashMap<String, String>();
            // headers.put("User-Agent", "Android");
            // headers.put("User-Password", "Password");
            // if (mUri != null)
            // mMediaPlayer.setDataSource(mUri.toString(), headers);
            if (mUri != null) {
                mMediaPlayer.setDataSource(mUri.toString());
            }
            if (!misTexturePowetEvent) {
                if (mSurfaceTexture != null) {
                    mSurface = new Surface(mSurfaceTexture);
                } else {
                    mSurface = new Surface(getSurfaceTexture());
                }
            } else {
                misTexturePowetEvent = false;
            }
            mMediaPlayer.setSurface(mSurface);
            mMediaPlayer.setScreenOnWhilePlaying(true);
            mMediaPlayer.prepareAsync();
            if (mMediaPlayerController != null) {
                mMediaPlayerController.onVideoPreparing();
            }
            mCurrentState = STATE_PREPARING;
        } catch (IOException ex) {
            Log.e(TAG, "Unable to open content: " + mUri, ex);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer,
                    IMediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            return;
        } catch (IllegalArgumentException ex) {
            Log.e(TAG, "Unable to open content: " + mUri, ex);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer,
                    IMediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            return;
        }
    }

    IMediaPlayer.OnVideoSizeChangedListener mSizeChangedListener = new IMediaPlayer.OnVideoSizeChangedListener() {

        @Override
        public void onVideoSizeChanged(IMediaPlayer mp, int width, int height,
                                       int sarNum, int sarDen) {

            Log.d(Constants.LOG_TAG, "OnSizeChanged");
            mVideoWidth = mp.getVideoWidth();
            mVideoHeight = mp.getVideoHeight();
            mVideoSarNum = sarNum;
            mVideoSarDen = sarDen;
        }
    };

    IMediaPlayer.OnPreparedListener mPreparedListener = new IMediaPlayer.OnPreparedListener() {

        @Override
        public void onPrepared(IMediaPlayer mp) {
//            isOpening = false;
            Log.d(Constants.LOG_TAG, "OnPrepared");
            mHasPrepared = true;
            mCurrentState = STATE_PREPARED;
            mTargetState = STATE_PLAYING;

            if (mOnPreparedListener != null)
                mOnPreparedListener.onPrepared(mMediaPlayer);

            mVideoWidth = mp.getVideoWidth();
            mVideoHeight = mp.getVideoHeight();
            // For test source ip
//            Bundle bundle = mp.getMediaMeta();
//            String source_ip = bundle
//                    .getString(MediaPlayerTexutureVideoView.KEY_SOUCE_IP);
//            Log.d(Constants.LOG_TAG, "Source IP = " + source_ip);
        }
    };

    private final IMediaPlayer.OnCompletionListener mCompletionListener = new IMediaPlayer.OnCompletionListener() {

        @Override
        public void onCompletion(IMediaPlayer mp) {

            Log.d(Constants.LOG_TAG, "onCompletion");
            mCurrentState = STATE_PLAYBACK_COMPLETED;
            mTargetState = STATE_PLAYBACK_COMPLETED;

            if (mOnCompletionListener != null)
                mOnCompletionListener.onCompletion(mMediaPlayer);
        }
    };

    private final IMediaPlayer.OnErrorListener mErrorListener = new IMediaPlayer.OnErrorListener() {

        @Override
        public boolean onError(IMediaPlayer mp, int framework_err, int impl_err) {

            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;

			/* If an error handler has been supplied, use it and finish. */
            if (mOnErrorListener != null) {
                if (mOnErrorListener.onError(mMediaPlayer, framework_err,
                        impl_err)) {
                    return true;
                }
            }
            return true;

        }
    };

    private final IMediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener = new IMediaPlayer.OnBufferingUpdateListener() {

        @Override
        public void onBufferingUpdate(IMediaPlayer mp, int percent) {

            mCurrentBufferPercentage = percent;
            if (mOnBufferingUpdateListener != null)
                mOnBufferingUpdateListener.onBufferingUpdate(mp, percent);
        }
    };

//    private final OnNetSpeedListener mNetSpeedListener = new OnNetSpeedListener() {
//
//        @Override
//        public void onNetSpeedUpdate(IMediaPlayer mp, int arg1, int arg2) {
//
//            if (mOnNetSpeedListener != null) {
//                mOnNetSpeedListener.onNetSpeedUpdate(mp, arg1, arg2);
//            }
//        }
//    };

    private final IMediaPlayer.OnInfoListener mInfoListener = new IMediaPlayer.OnInfoListener() {

        @Override
        public boolean onInfo(IMediaPlayer mp, int what, int extra) {

            if (mOnInfoListener != null) {
                mOnInfoListener.onInfo(mp, what, extra);
            }
            return true;
        }
    };

//    private final OnDRMRequiredListener mDRMRequiredListener = new OnDRMRequiredListener() {
//
//        @Override
//        public void OnDRMRequired(IMediaPlayer mp, int what, int extra,
//                                  String version) {
//
//            if (mOnDRMRequiredListener != null) {
//                mOnDRMRequiredListener.OnDRMRequired(mp, what, extra, version);
//            }
//        }
//
//    };
//
//    private OnDebugInfoListener mDebugInfoListener = new OnDebugInfoListener() {
//
//        @Override
//        public void onDebugInfo(IMediaPlayer mp, int type, int arg1, int arg2) {
//            if (mOnDebugInfoListener != null) {
//                mOnDebugInfoListener.onDebugInfo(mp, type, arg1, arg2);
//            }
//        }
//    };

    private final IMediaPlayer.OnSeekCompleteListener mSeekCompleteListener = new IMediaPlayer.OnSeekCompleteListener() {

        @Override
        public void onSeekComplete(IMediaPlayer mp) {

            Log.d(Constants.LOG_TAG, "onSeekComplete");
            if (mOnSeekCompleteListener != null)
                mOnSeekCompleteListener.onSeekComplete(mp);
        }
    };

    public void setMediaPlayerController(
            MediaPlayerController mediaPlayerController) {

        mMediaPlayerController = mediaPlayerController;
    }
//
//    public void setOnDebugInfoListener(OnDebugInfoListener l) {
//        mOnDebugInfoListener = l;
//    }

    public void setOnPreparedListener(IMediaPlayer.OnPreparedListener l) {

        mOnPreparedListener = l;
    }

    public void setOnCompletionListener(IMediaPlayer.OnCompletionListener l) {

        mOnCompletionListener = l;
    }

    public void setOnErrorListener(IMediaPlayer.OnErrorListener l) {

        mOnErrorListener = l;
    }

    public void setOnBufferingUpdateListener(IMediaPlayer.OnBufferingUpdateListener l) {

        mOnBufferingUpdateListener = l;
    }

//    public void setOnSpeedListener(OnNetSpeedListener l) {
//        mOnNetSpeedListener = l;
//    }

    public void setOnSeekCompleteListener(IMediaPlayer.OnSeekCompleteListener l) {

        mOnSeekCompleteListener = l;
    }

    public void setOnInfoListener(IMediaPlayer.OnInfoListener l) {

        mOnInfoListener = l;
    }

//    public void setOnDRMRequiredListener(OnDRMRequiredListener l) {
//
//        mOnDRMRequiredListener = l;
//    }

//    public void setOnSurfaceListener(OnSurfaceListener l) {
//
//        mOnSurfaceListener = l;
//    }

    private boolean mIsDismiss;
    private KeyguardManager km;
    private KeyguardLock mKeyguardLock;
    private boolean isAppShowing;
    private boolean isDestroyed = true;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    protected void release(final boolean cleartargetstate) {
        long current = System.currentTimeMillis();
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
            mCurrentState = STATE_IDLE;
            if (cleartargetstate)
                mTargetState = STATE_IDLE;
        }
        Log.e(Constants.LOG_TAG,
                "videoview release cost :"
                        + String.valueOf(System.currentTimeMillis() - current));
        Toast.makeText(
                mContext.getApplicationContext(),
                "videoview release cost :"
                        + String.valueOf(System.currentTimeMillis() - current),
                Toast.LENGTH_SHORT).show();

    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        return false;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {

        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        boolean isKeyCodeSupported = keyCode != KeyEvent.KEYCODE_BACK
                && keyCode != KeyEvent.KEYCODE_VOLUME_UP
                && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN
                && keyCode != KeyEvent.KEYCODE_MENU
                && keyCode != KeyEvent.KEYCODE_CALL
                && keyCode != KeyEvent.KEYCODE_ENDCALL;
        if (keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
            mIsDismiss = true;
        }
        if (isInPlaybackState() && isKeyCodeSupported) {
            if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK
                    || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                    || keyCode == KeyEvent.KEYCODE_SPACE) {
                if (mMediaPlayer.isPlaying()) {
                    pause();

                } else {
                    start();

                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                    && mMediaPlayer.isPlaying()) {
                pause();
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void start() {

        Log.i(Constants.LOG_TAG, "start , =========================="
                + isInPlaybackState());
        if (isInPlaybackState()) {
            mMediaPlayer.start();
            mCurrentState = STATE_PLAYING;
            if (mMediaPlayerController != null)
                mMediaPlayerController.onPlay();
        }
        mTargetState = STATE_PLAYING;
    }

    @Override
    public void pause() {

        if (isInPlaybackState()) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mCurrentState = STATE_PAUSED;
                if (mMediaPlayerController != null)
                    mMediaPlayerController.onPause();
            }
        }
        mTargetState = STATE_PAUSED;
    }

    public void resume() {

        Log.e(Constants.LOG_TAG, "video view resume");
        if (mSurfaceTexture == null && mCurrentState == STATE_SUSPEND) {
            mTargetState = STATE_RESUME;
        } else if (mCurrentState == STATE_SUSPEND_UNSUPPORTED) {
            openVideo();
        }
    }

    @Override
    public int getDuration() {

        if (isInPlaybackState()) {
            if (mDuration > 0)
                return (int) mDuration;
            mDuration = mMediaPlayer.getDuration();
            return (int) mDuration;
        }
        mDuration = -1;
        return (int) mDuration;
    }

    public MediaInfo getMediaInfo() {

        if (isInPlaybackState()) {
            if (mMediaInfo == null) {
                mMediaInfo = mMediaPlayer.getMediaInfo();
            }
            return mMediaInfo;
        }

        mMediaInfo = null;
        return mMediaInfo;
    }

    @Override
    public int getCurrentPosition() {

        if (isInPlaybackState()) {
            long position = mMediaPlayer.getCurrentPosition();
            return (int) position;
        }
        return 0;
    }

    @Override
    public void seekTo(long msec) {

        Log.e(Constants.LOG_TAG, "seek called=========");
        if (isInPlaybackState())
            mMediaPlayer.seekTo(msec);
    }

    @Override
    public boolean isPlaying() {

        return isInPlaybackState() && mMediaPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage() {

        if (mMediaPlayer != null)
            return mCurrentBufferPercentage;
        return 0;
    }

    public int getVideoWidth() {

        return mVideoWidth;
    }

    public int getVideoHeight() {

        return mVideoHeight;
    }

    protected boolean isInPlaybackState() {

        return (mMediaPlayer != null && mCurrentState != STATE_ERROR
                && mCurrentState != STATE_IDLE && mCurrentState != STATE_PREPARING);
    }

    @Override
    public boolean canPause() {

        if (isPlaying())
            return true;
        return false;
    }

    @Override
    public boolean canSeekBackward() {

        if (this.getDuration() > 0)
            return true;
        return false;
    }

    @Override
    public boolean canSeekForward() {

        if (this.getDuration() > 0)
            return true;
        return false;
    }

    @Override
    public boolean canStart() {

        return isInPlaybackState();
    }

    @Override
    public void onPlay() {

    }

    @Override
    public void onPause() {

    }

    // P1 Added Interface
//    public void setAudioAmplify(float ratio) {

//        mMediaPlayer.setAudioAmplify(ratio);
//    }

//    public void setVideoRate(float rate) {
//
//        mMediaPlayer.setVideoRate(rate);
//    }
//
//    public void getCurrentFrame(Bitmap bitmap) {
//
//        mMediaPlayer.getCurrentFrame(bitmap);
//    }

//    public void setBufferSize(int size) {
//
//        mMediaPlayer.setBufferSize(size);
//    }
//
//    public void setAnalyseDuration(int duration) {
//
//        mMediaPlayer.setAnalyseDuration(duration);
//    }

    // P2 Added Interface
//    public void setDRMKey(String version, String key) {
//
//        mMediaPlayer.setDRMKey(version, key);
//    }

    @Override
    public void onPowerState(int state) {
        Log.d(Constants.LOG_TAG, "onPowerState :" + state);
        switch (state) {
            case Constants.POWER_OFF:
                Log.d("eflake", "POWER_OFF");
                misTexturePowetEvent = true;
                switch (videoViewConfig.getInterruptMode()) {
                    case VideoViewConfig.INTERRUPT_MODE_RELEASE_CREATE:
                        release(true);
                        break;
                    case VideoViewConfig.INTERRUPT_MODE_PAUSE_RESUME:
                        pause();
                        break;
                    case VideoViewConfig.INTERRUPT_MODE_STAY_PLAYING:
                        break;
                }
                break;
            case Constants.POWER_ON:
                if (isKeyGuard()) {
                    Log.d("eflake", "isKeyGuard");
                    mNeedUnlock = true;
                } else {
                    Log.d("eflake", "POWER_ON");
                    switch (videoViewConfig.getInterruptMode()) {
                        case VideoViewConfig.INTERRUPT_MODE_RELEASE_CREATE:
                            openVideo();
                            break;
                        case VideoViewConfig.INTERRUPT_MODE_PAUSE_RESUME:
                            start();
                            break;
                        case VideoViewConfig.INTERRUPT_MODE_STAY_PLAYING:
                            break;
                    }
                }
                break;
            case Constants.USER_PRESENT:
                Log.d("eflake", "USER_PRESENT");
                if (isAppShowing && mNeedUnlock) {
                    Log.d("eflake", "isKeyGuard");
                    mNeedUnlock = false;
                    switch (videoViewConfig.getInterruptMode()) {
                        case VideoViewConfig.INTERRUPT_MODE_RELEASE_CREATE:
//                            if (!isOpening){
                            openVideo();
//                            }
                            break;
                        case VideoViewConfig.INTERRUPT_MODE_PAUSE_RESUME:
                            start();
                            break;
                        case VideoViewConfig.INTERRUPT_MODE_STAY_PLAYING:
                            break;
                    }
                }
                break;
            case Constants.APP_SHOWN:
                isAppShowing = true;
                break;
            case Constants.APP_HIDEN:
                isAppShowing = false;
                break;
            default:
                break;
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public boolean isKeyGuard() {
        km = (KeyguardManager) mContext
                .getSystemService(Context.KEYGUARD_SERVICE);
        if (km.isKeyguardSecure() || km.isKeyguardLocked()) {
            Log.d(Constants.LOG_TAG, "locked");
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width,
                                          int height) {
        mSurfaceWidth = width;
        mSurfaceHeight = height;
        mSurfaceTexture = surface;
        switch (videoViewConfig.getInterruptMode()) {
            case VideoViewConfig.INTERRUPT_MODE_RELEASE_CREATE:
                Log.i("eflake", "surfaceCreated  openVideo in video view");
                openVideo();
                break;
            case VideoViewConfig.INTERRUPT_MODE_PAUSE_RESUME:
                if (mMediaPlayer != null) {
                    mMediaPlayer.setSurface(new Surface(mSurfaceTexture));
                    start();
                }
                break;
            case VideoViewConfig.INTERRUPT_MODE_STAY_PLAYING:
                break;
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width,
                                            int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.i("eflake", "surfaceDestroyed release");
        switch (videoViewConfig.getInterruptMode()) {
            case VideoViewConfig.INTERRUPT_MODE_RELEASE_CREATE:
                release(true);
                break;
            case VideoViewConfig.INTERRUPT_MODE_PAUSE_RESUME:
                pause();
                break;
            case VideoViewConfig.INTERRUPT_MODE_STAY_PLAYING:
                break;
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

}
