package com.ksy.media.widget.controller;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import com.ksy.media.widget.ui.common.MediaPlayerVideoSeekBar;
import com.ksy.media.widget.ui.common.HeartLayout;
import com.ksy.media.widget.ui.common.HorizontalListView;
import com.ksy.media.widget.ui.common.LiveExitDialog;
import com.ksy.media.widget.ui.common.LivePersonDialog;
import com.ksy.media.widget.ui.livereplay.LiveReplayDialogAdapter;
import com.ksy.media.widget.ui.livereplay.LiveReplayDialogInfo;
import com.ksy.media.widget.ui.livereplay.LiveReplayHeadListAdapter;
import com.ksy.mediaPlayer.widget.R;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import android.os.Handler;
import android.graphics.Color;

public class LiveReplayMediaPlayerControllerView extends MediaPlayerBaseControllerView implements View.OnClickListener {

	private ImageView liveReplayHead;
	private TextView  loadingTextView;
	private TextView  closeTextView;
	private TextView  reportTextView;

	private ListView listView;
	private List<LiveReplayDialogInfo> dialogList;
	private LiveReplayDialogAdapter dialogAdapter;

	private ImageView liveReplayPerson;
	private HorizontalListView mHorizontalList;
	private LiveReplayHeadListAdapter headListAdapter;

    private Button switchButton;
	private Button shareButton;
	private MediaPlayerVideoSeekBar mSeekBar;
	private ImageView mPlaybackImageView;

    private Context mContext;
	private Random mRandom = new Random();
	private Timer mTimer = new Timer();
	private HeartLayout mHeartLayout;
	private ImageView heartImageView;
	private boolean isSwitch;
	private boolean isListVisible;

	private Timer listHide = new Timer();
	private Handler mHandler = new Handler();

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

		mLayoutInflater.inflate(R.layout.blue_media_player_controller_live_replay, this);

		initViews();
		initListeners();
	}

	@Override
	protected void initViews() {

		liveReplayHead = (ImageView)findViewById(R.id.image_live_replay_head);
		loadingTextView = (TextView) findViewById(R.id.text_live_replay);
		closeTextView = (TextView) findViewById(R.id.title_text_close);
		reportTextView = (TextView) findViewById(R.id.title_text_report);

		listView = (ListView) findViewById(R.id.live_list);
		//load data
		dialogList = new ArrayList<LiveReplayDialogInfo>();
		dialogAdapter = new LiveReplayDialogAdapter(dialogList, mContext);
		listView.setAdapter(dialogAdapter);

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

		mSeekBar.setMax(MAX_VIDEO_PROGRESS);
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
		}, 500, 500);

		mHandler.postDelayed(listHideRunnable, 5000);
	}

    //delay
	Runnable listHideRunnable = new Runnable() {
		@Override
		public void run() {
			listView.setVisibility(GONE);
		}
	};

	@Override
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
//		mControllerTopView.setVisibility(VISIBLE);
//		mControllerBottomView.setVisibility(VISIBLE);
	}

	@Override
	void onHide() {
//		mControllerTopView.setVisibility(INVISIBLE);
//		mControllerBottomView.setVisibility(INVISIBLE);
	}

	public void updateVideoTitle(String title) {
		if (!TextUtils.isEmpty(title)) {
		}
	}

	public void updateVideoProgress(float percentage) {

		if (percentage >= 0 && percentage <= 1) {
			int progress = (int) (percentage * mSeekBar.getMax());
			if (!mVideoProgressTrackingTouch)
				mSeekBar.setProgress(progress);
		}
	}

	public void updateVideoPlaybackState(boolean isStart) {
		// 播放中
		if (isStart) {
			loadingTextView.setText("回放中");
			mPlaybackImageView.setImageResource(R.drawable.blue_ksy_pause);

			if (mMediaPlayerController.canPause()) {
				mPlaybackImageView.setEnabled(true);
			} else {
				mPlaybackImageView.setEnabled(false);
			}
		}
		// 未播放
		else {
//			loadingTextView.setText("加载中");
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

		if (id == liveReplayHead.getId()) {
			LivePersonDialog dialogPerson = new LivePersonDialog(mContext);
			dialogPerson.show();

		} else if (id == mPlaybackImageView.getId()) {
			if (mMediaPlayerController.isPlaying()) {
				mMediaPlayerController.pause();
				show(0);
			} else if (!mMediaPlayerController.isPlaying()) {
				mMediaPlayerController.start();
				show();
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

		} else if (id == switchButton.getId()) {
			if (isSwitch) {
//				listView.setVisibility(VISIBLE);
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

	private int randomColor() {
		return  Color.rgb(mRandom.nextInt(255), mRandom.nextInt(255), mRandom.nextInt(255));
	}

}
