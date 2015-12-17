package com.ksy.media.widget.controller;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ksy.media.widget.data.MediaPlayMode;
import com.ksy.mediaPlayer.widget.R;

public class StreamMediaPlayerSmallControllerView extends MediaPlayerBaseControllerView implements View.OnClickListener {

    private Context mContext;
    private RelativeLayout mControllerTopView;
    private RelativeLayout mBackLayout;
    private TextView mTitleTextView;
    private RelativeLayout mControllerBottomView;
    private ImageView mPlaybackImageView;
    private ImageView mScreenModeImageView;
    private ImageView mSettingView;
    private PopupWindow mPopupWindow;
    private View stream_share_tv;
    private View stream_alarm_tv;
    private View stream_setting_tv;
    private View view;

    public StreamMediaPlayerSmallControllerView(Context context, AttributeSet attrs, int defStyle) {

        super(context, attrs, defStyle);
    }

    public StreamMediaPlayerSmallControllerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public StreamMediaPlayerSmallControllerView(Context context) {
        super(context);
        mContext = context;
        mLayoutInflater.inflate(R.layout.stream_blue_media_player_controller_small, this);

        initViews();
        initListeners();
    }

    @Override
    protected void initViews() {
        mControllerTopView = (RelativeLayout) findViewById(R.id.controller_top_layout);
        mBackLayout = (RelativeLayout) findViewById(R.id.back_layout);
        mTitleTextView = (TextView) findViewById(R.id.title_text_view);
        mSettingView = (ImageView) findViewById(R.id.stream_setting);
        mControllerBottomView = (RelativeLayout) findViewById(R.id.controller_bottom_layout);
        mPlaybackImageView = (ImageView) findViewById(R.id.video_playback_image_view);
        mScreenModeImageView = (ImageView) findViewById(R.id.video_fullscreen_image_view);
        View view = LayoutInflater.from(mContext).inflate(R.layout.stream_small_pop, null);
        stream_share_tv = view.findViewById(R.id.stream_share_tv);
        stream_alarm_tv = view.findViewById(R.id.stream_alarm_tv);
        stream_setting_tv = view.findViewById(R.id.stream_setting_tv);
        mPopupWindow = new PopupWindow(view, getResources().getDimensionPixelSize(R.dimen.stream_pop_width), getResources().getDimensionPixelOffset(R.dimen.stream_pop_height));
        mPopupWindow.setFocusable(true);
        mPopupWindow.setTouchable(true);
        mPopupWindow.setBackgroundDrawable(new BitmapDrawable());
    }

    @Override
    protected void initListeners() {
        mSettingView.setOnClickListener(this);
        mBackLayout.setOnClickListener(this);
        mTitleTextView.setOnClickListener(this);
        mPlaybackImageView.setOnClickListener(this);
        mScreenModeImageView.setOnClickListener(this);

    }

    @Override
    void onTimerTicker() {
        Log.d("eflake", "onTimerTicker");
    }

    @Override
    void onShow() {
        Log.d("eflake", "onShow");
        mControllerTopView.setVisibility(VISIBLE);
        mControllerBottomView.setVisibility(VISIBLE);
    }

    @Override
    void onHide() {
        Log.d("eflake", "onHide");
        mControllerTopView.setVisibility(View.GONE);
        mControllerBottomView.setVisibility(View.GONE);
        hidePopWindow();
    }

    public void updateVideoTitle(String title) {

        if (!TextUtils.isEmpty(title)) {
            mTitleTextView.setText(title);
        }
    }


    public void updateVideoPlaybackState(boolean isStart) {

        // 播放中
        if (isStart) {

            mPlaybackImageView.setImageResource(R.drawable.blue_ksy_pause);

            if (mMediaPlayerController.canPause()) {
                mPlaybackImageView.setEnabled(true);
            } else {
                mPlaybackImageView.setEnabled(false);
            }
        }
        // 未播放
        else {
            mPlaybackImageView.setImageResource(R.drawable.blue_ksy_play);
            if (mMediaPlayerController.canStart()) {
                mPlaybackImageView.setEnabled(true);
            } else {
                mPlaybackImageView.setEnabled(false);
            }
        }
    }

    @Override
    public void onClick(View v) {

        int id = v.getId();

        if (id == mBackLayout.getId() || id == mTitleTextView.getId()) {

            mMediaPlayerController.onBackPress(MediaPlayMode.PLAYMODE_WINDOW);

        } else if (id == mPlaybackImageView.getId()) {

            if (mMediaPlayerController.isPlaying()) {
                mMediaPlayerController.pause();
                show(0);
            } else if (!mMediaPlayerController.isPlaying()) {
                mMediaPlayerController.start();
                show();
            }

        } else if (id == mScreenModeImageView.getId()) {
            mMediaPlayerController.onRequestPlayMode(MediaPlayMode.PLAYMODE_FULLSCREEN);
        } else if (id == mSettingView.getId()) {
            showPopWindow();
        } else if (id == stream_alarm_tv.getId() || id == stream_setting_tv.getId() || id == stream_share_tv.getId()) {
            hidePopWindow();
        }

    }

    private void showPopWindow() {
        if (!mPopupWindow.isShowing()) {
            mPopupWindow.showAsDropDown(mSettingView,0,getResources().getDimensionPixelSize(R.dimen.stream_pop_offset));
            stream_share_tv.setOnClickListener(this);
            stream_alarm_tv.setOnClickListener(this);
            stream_setting_tv.setOnClickListener(this);
            stopTimerTicker();
        }
    }

    private void hidePopWindow() {
        if (mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
        }
    }

}
