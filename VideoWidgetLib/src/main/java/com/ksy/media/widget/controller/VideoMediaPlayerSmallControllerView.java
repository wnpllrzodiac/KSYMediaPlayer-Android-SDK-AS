package com.ksy.media.widget.controller;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.ksy.media.widget.data.MediaPlayMode;
import com.ksy.media.widget.data.MediaPlayerUtils;
import com.ksy.media.widget.ui.MediaPlayerVideoSeekBar;
import com.ksy.mediaPlayer.widget.R;

import org.w3c.dom.Text;

public class VideoMediaPlayerSmallControllerView extends MediaPlayerBaseControllerView implements View.OnClickListener {

    private RelativeLayout mControllerTopView;
    private RelativeLayout mBackLayout;
    private TextView mTitleTextView;

    private RelativeLayout mControllerBottomView;
    private MediaPlayerVideoSeekBar mSeekBar;
    private ImageView mPlaybackImageView;
    private ImageView mScreenModeImageView;
    private TextView mCurrentTimeTextView;
    private TextView mTotalTimeTextView;

    public VideoMediaPlayerSmallControllerView(Context context, AttributeSet attrs, int defStyle) {

        super(context, attrs, defStyle);
    }

    public VideoMediaPlayerSmallControllerView(Context context, AttributeSet attrs) {

        super(context, attrs);
    }

    public VideoMediaPlayerSmallControllerView(Context context) {
        super(context);
        mLayoutInflater.inflate(R.layout.video_blue_media_player_controller_small, this);

        initViews();
        initListeners();
    }

    @Override
    protected void initViews() {

        mControllerTopView = (RelativeLayout) findViewById(R.id.controller_top_layout);
        mBackLayout = (RelativeLayout) findViewById(R.id.back_layout);
        mTitleTextView = (TextView) findViewById(R.id.title_text_view);

        mControllerBottomView = (RelativeLayout) findViewById(R.id.controller_bottom_layout);
        mSeekBar = (MediaPlayerVideoSeekBar) findViewById(R.id.seekbar_video_progress);
        mPlaybackImageView = (ImageView) findViewById(R.id.video_playback_image_view);
        mScreenModeImageView = (ImageView) findViewById(R.id.video_fullscreen_image_view);
        mCurrentTimeTextView = (TextView) findViewById(R.id.video_small_current_time_tv);
        mTotalTimeTextView = (TextView) findViewById(R.id.video_small_duration_time_tv);
        mSeekBar.setMax(MAX_VIDEO_PROGRESS);
        mSeekBar.setProgress(0);

    }

    @Override
    protected void initListeners() {

        mBackLayout.setOnClickListener(this);
        mTitleTextView.setOnClickListener(this);
        mPlaybackImageView.setOnClickListener(this);
        mScreenModeImageView.setOnClickListener(this);
        mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                mVideoProgressTrackingTouch = false;

                int curProgress = seekBar.getProgress();
                int maxProgress = seekBar.getMax();

                if (curProgress >= 0 && curProgress <= maxProgress) {
                    float percentage = ((float) curProgress) / maxProgress;
                    int position = (int) (mMediaPlayerController.getDuration() * percentage);
                    mMediaPlayerController.seekTo(position);
                    // mMediaPlayerController.start();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

                mVideoProgressTrackingTouch = true;
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                if (fromUser) {
                    if (isShowing()) {
                        show();
                    }
                }

            }
        });

    }

    @Override
    void onTimerTicker() {

        long currentTime = mMediaPlayerController.getCurrentPosition();
        long durationTime = mMediaPlayerController.getDuration();

        if (durationTime > 0 && currentTime <= durationTime) {
            float percentage = ((float) currentTime) / durationTime;
            updateVideoProgress(percentage);
        }

    }

    @Override
    void onShow() {

        mControllerTopView.setVisibility(VISIBLE);
        mControllerBottomView.setVisibility(VISIBLE);
    }

    @Override
    void onHide() {

        mControllerTopView.setVisibility(INVISIBLE);
        mControllerBottomView.setVisibility(INVISIBLE);
    }

    public void updateVideoTitle(String title) {

        if (!TextUtils.isEmpty(title)) {
            mTitleTextView.setText(title);
        }
    }

    public void updateVideoProgress(float percentage) {

        if (percentage >= 0 && percentage <= 1) {
            int progress = (int) (percentage * mSeekBar.getMax());
            if (!mVideoProgressTrackingTouch)
                mSeekBar.setProgress(progress);

            long curTime = mMediaPlayerController.getCurrentPosition();
            long durTime = mMediaPlayerController.getDuration();

            if (durTime > 0 && curTime <= durTime) {
                mCurrentTimeTextView.setText(MediaPlayerUtils
                        .getVideoDisplayTime(curTime));
                mTotalTimeTextView.setText("/"
                        + MediaPlayerUtils.getVideoDisplayTime(durTime));
            }
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
        }

    }

}
