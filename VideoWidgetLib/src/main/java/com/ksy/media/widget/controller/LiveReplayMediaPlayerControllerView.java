package com.ksy.media.widget.controller;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.ksy.media.widget.data.MediaPlayerUtils;
import com.ksy.media.widget.ui.common.LiveAnchorDialog;
import com.ksy.media.widget.ui.common.MediaPlayerScreenSizePopupView;
import com.ksy.media.widget.ui.common.MediaPlayerVideoSeekBar;
import com.ksy.media.widget.ui.common.HeartLayout;
import com.ksy.media.widget.ui.common.HorizontalListView;
import com.ksy.media.widget.ui.common.LiveExitDialog;
import com.ksy.media.widget.ui.common.LivePersonDialog;
import com.ksy.media.widget.ui.livereplay.LiveReplayDialogAdapter;
import com.ksy.media.widget.ui.livereplay.LiveReplayDialogInfo;
import com.ksy.media.widget.ui.livereplay.LiveReplayHeadListAdapter;
import com.ksy.media.widget.util.IMediaPlayerControl;
import com.ksy.mediaPlayer.widget.R;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import android.os.Handler;
import android.graphics.Color;


public class LiveReplayMediaPlayerControllerView extends FrameLayout implements View.OnClickListener {

	private ImageView liveReplayHead;
	private TextView  loadingTextView;
	private TextView  netErrorTextView;
	private TextView  closeTextView;
	private TextView  reportTextView;

	private ListView listView;
	private List<LiveReplayDialogInfo> dialogList;
	private LiveReplayDialogAdapter dialogAdapter;
	private TextView noticeTextView;

	private ImageView liveReplayPerson;
	private TextView praiseCountTextView;
	private HorizontalListView mHorizontalList;
	private LiveReplayHeadListAdapter headListAdapter;
	private int praiseCount;

    private Button switchButton;
	private Button shareButton;
	private MediaPlayerVideoSeekBar mSeekBar;
	private TextView currentTimeTextView;
	private TextView lineTextView;
	private TextView totalTimeTextView;
	private ImageView mPlaybackImageView;

    private Context mContext;
	private Random mRandom = new Random();
	private Timer mTimer = new Timer();
	private HeartLayout mHeartLayout;
	private ImageView heartImageView;
	private boolean isSwitch;
	private boolean isListVisible;

	private Handler mHandler = new Handler();
	protected LayoutInflater mLiveReplayLayoutInflater;
	protected static final int LIVEREPLAY_MAX_VIDEO_PROGRESS = 1000;
	protected volatile boolean mVideoProgressTrackingTouch = false;
	protected  LiveReplayMediaPlayerController  mLiveReplayMediaPlayerController;
	private Timer seekTimer;
	private volatile boolean mSeekStarted = false;

	public LiveReplayMediaPlayerControllerView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
	}

	public LiveReplayMediaPlayerControllerView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
	}

	public LiveReplayMediaPlayerControllerView(Context context) {
		super(context);
		mContext = context;

		mLiveReplayLayoutInflater = LayoutInflater.from(getContext());
		mLiveReplayLayoutInflater.inflate(R.layout.blue_media_player_controller_live_replay, this);

		initViews();
		initListeners();
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		initViews();
		initListeners();
	}

	protected void initViews() {

		liveReplayHead = (ImageView)findViewById(R.id.image_live_replay_head);
		loadingTextView = (TextView) findViewById(R.id.text_live_replay);
		netErrorTextView = (TextView) findViewById(R.id.textViewNetError);
		closeTextView = (TextView) findViewById(R.id.title_text_close);
		reportTextView = (TextView) findViewById(R.id.title_text_report);
		praiseCountTextView = (TextView) findViewById(R.id.praise_count_text);

		listView = (ListView) findViewById(R.id.live_list);
		//load data
		dialogList = new ArrayList<LiveReplayDialogInfo>();
		dialogAdapter = new LiveReplayDialogAdapter(dialogList, mContext);
		listView.setAdapter(dialogAdapter);
		noticeTextView = (TextView)findViewById(R.id.notice_text);

		mHorizontalList = (HorizontalListView) findViewById(R.id.live_replay_horizon);
		headListAdapter = new LiveReplayHeadListAdapter(mContext);
		mHorizontalList.setAdapter(headListAdapter);

		mHorizontalList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
				LivePersonDialog dialogPerson = new LivePersonDialog(mContext);
				dialogPerson.show();
			}
		});

		liveReplayPerson = (ImageView)findViewById(R.id.live_replay_person_image);
		switchButton = (Button) findViewById(R.id.live_replay_information_switch);
		mHeartLayout = (HeartLayout)findViewById(R.id.live_replay_image_heart);
		heartImageView = (ImageView) findViewById(R.id.image_heart);
		shareButton = (Button) findViewById(R.id.live_replay_share_bt);

		mSeekBar = (MediaPlayerVideoSeekBar) findViewById(R.id.seekbar_video_progress);
		mPlaybackImageView = (ImageView) findViewById(R.id.video_playback_image_view);
		currentTimeTextView = (TextView) findViewById(R.id.textViewCurrentTime);
		lineTextView = (TextView) findViewById(R.id.textViewLine);
		totalTimeTextView = (TextView) findViewById(R.id.textViewTotalTime);
		mSeekBar.setMax(LIVEREPLAY_MAX_VIDEO_PROGRESS);
		mSeekBar.setProgress(0);

		//heart
		mTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				mHeartLayout.post(new Runnable() {
					@Override
					public void run() {
						mHeartLayout.addHeart(randomColor());
					}
				});
			}
		}, 500, 300);

		seekTimer = new Timer();
		seekTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				mHandler.postDelayed(seekRefreshRunnable, 200);
			}
		}, 200, 1000);

		mHandler.postDelayed(listHideRunnable, 5000);

	}

    //delay
	Runnable listHideRunnable = new Runnable() {
		@Override
		public void run() {
			listView.setVisibility(GONE);
			noticeTextView.setVisibility(GONE);
		}
	};

	Runnable seekRefreshRunnable = new Runnable() {
		@Override
		public void run() {
			onTimerTicker();
		}
	};


	protected void initListeners() {

		liveReplayHead.setOnClickListener(this);
		closeTextView.setOnClickListener(this);
		reportTextView.setOnClickListener(this);

		liveReplayPerson.setOnClickListener(this);
		switchButton.setOnClickListener(this);
		shareButton.setOnClickListener(this);
		mPlaybackImageView.setOnClickListener(this);

		mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				mVideoProgressTrackingTouch = false;

				int curProgress = seekBar.getProgress();
				int maxProgress = seekBar.getMax();

				if (curProgress >= 0 && curProgress <= maxProgress) {
					float percentage = ((float) curProgress) / maxProgress;
					int position = (int) (mLiveReplayMediaPlayerController.getDuration() * percentage);
					mLiveReplayMediaPlayerController.seekTo(position);
					// mMediaPlayerController.start();
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				mVideoProgressTrackingTouch = true;
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				/*if (fromUser) {
					if (isShowing()) {
						show();
					}
				}*/
			}
		});

	}

	public boolean isShowing() {
		if (getVisibility() == View.VISIBLE) {
			return true;
		}
		return false;
	}

	private void onTimerTicker() {

		long currentTime = mLiveReplayMediaPlayerController.getCurrentPosition();
		long durationTime = mLiveReplayMediaPlayerController.getDuration();

		if (durationTime > 0 && currentTime <= durationTime) {
			float percentage = ((float) currentTime) / durationTime;
			updateVideoProgress(percentage);
		}
	}

	public void updateVideoTitle(String title) {
		if (!TextUtils.isEmpty(title)) {
		}
	}

	public void updateVideoProgress(float percentage) {

		if (percentage >= 0 && percentage <= 1) {
			int progress = (int) (percentage * mSeekBar.getMax());
			if (!mVideoProgressTrackingTouch) {
				mSeekBar.setProgress(progress);
			}

			long curTime = mLiveReplayMediaPlayerController.getCurrentPosition();
			long durTime = mLiveReplayMediaPlayerController.getDuration();

			if (durTime > 0 && curTime <= durTime) {
				currentTimeTextView.setText(MediaPlayerUtils
						.getVideoDisplayTime(curTime));
				totalTimeTextView.setText(MediaPlayerUtils.getVideoDisplayTime(durTime));
			}
		}
	}

	public void updateVideoPlaybackState(boolean isStart) {
		// 播放中
		if (isStart) {
			loadingTextView.setText("回放中");
			mPlaybackImageView.setImageResource(R.drawable.blue_ksy_pause);

			if (mLiveReplayMediaPlayerController.canPause()) {
				mPlaybackImageView.setEnabled(true);
			} else {
				mPlaybackImageView.setEnabled(false);
			}
		}
		// 未播放
		else {

			mPlaybackImageView.setImageResource(R.drawable.blue_ksy_play);
			if (mLiveReplayMediaPlayerController.canStart()) {
				mPlaybackImageView.setEnabled(true);
			} else {
				mPlaybackImageView.setEnabled(false);
			}
		}
	}

	@Override
	public void onClick(View v) {

		int id = v.getId();

		if (id == liveReplayHead.getId()) {
			LiveAnchorDialog dialogPerson = new LiveAnchorDialog(mContext);
			dialogPerson.show();

		} else if (id == mPlaybackImageView.getId()) {
			if (mLiveReplayMediaPlayerController.isPlaying()) {
				mLiveReplayMediaPlayerController.pause();
//				show(0);
			} else if (!mLiveReplayMediaPlayerController.isPlaying()) {
				mLiveReplayMediaPlayerController.start();
//				show();
			}
		} else if (id == closeTextView.getId()) {
			LiveExitDialog dialog = new LiveExitDialog(mContext);
			dialog.show();

		} else if (id == reportTextView.getId()) {
			LiveExitDialog dialog = new LiveExitDialog(mContext);
			dialog.show();

		} else if (id == liveReplayPerson.getId()) {
			//person list button
			if (isListVisible) {
				mHorizontalList.setVisibility(VISIBLE);
				isListVisible = false;
			} else {
				mHorizontalList.setVisibility(GONE);
				isListVisible = true;
			}

		} else if (id == shareButton.getId()) {
			//TODO
			praiseCount++;
			praiseCountTextView.setText(String.valueOf(praiseCount));

		} else if (id == switchButton.getId()) {
			if (isSwitch) {
//				listView.setVisibility(VISIBLE);
				currentTimeTextView.setVisibility(VISIBLE);
				lineTextView.setVisibility(VISIBLE);
				totalTimeTextView.setVisibility(VISIBLE);
				liveReplayPerson.setVisibility(VISIBLE);
				mHeartLayout.setVisibility(VISIBLE);
				mHorizontalList.setVisibility(VISIBLE);
				mPlaybackImageView.setVisibility(VISIBLE);
				mSeekBar.setVisibility(VISIBLE);
				shareButton.setVisibility(VISIBLE);
				heartImageView.setVisibility(VISIBLE);
				isSwitch = false;

			} else {
//				listView.setVisibility(GONE);
				currentTimeTextView.setVisibility(GONE);
				lineTextView.setVisibility(GONE);
				totalTimeTextView.setVisibility(GONE);
				liveReplayPerson.setVisibility(GONE);
				mHeartLayout.setVisibility(GONE);
				mHorizontalList.setVisibility(GONE);
				mPlaybackImageView.setVisibility(GONE);
				mSeekBar.setVisibility(GONE);
				shareButton.setVisibility(GONE);
				heartImageView.setVisibility(GONE);
				isSwitch = true;
			  }
			} else if (id == heartImageView.getId()) {
			  //TODO
		}

	}

	public void setMediaPlayerController(LiveReplayMediaPlayerController mediaPlayerController) {
		mLiveReplayMediaPlayerController = mediaPlayerController;
	}

	private int randomColor() {
		return  Color.rgb(mRandom.nextInt(255), mRandom.nextInt(255), mRandom.nextInt(255));
	}

	public interface LiveReplayMediaPlayerController extends IMediaPlayerControl {

		boolean supportQuality();

		boolean supportVolume();

		boolean playVideo(String url);

		int getPlayMode();

		void onRequestPlayMode(int requestPlayMode);

		void onBackPress(int playMode);

		void onControllerShow(int playMode);

		void onControllerHide(int playMode);

		void onRequestLockMode(boolean lockMode);

		void onVideoPreparing();

		void onMovieRatioChange(int screenSize);

		void onMoviePlayRatioUp();

		void onMoviePlayRatioDown();

	}

}
