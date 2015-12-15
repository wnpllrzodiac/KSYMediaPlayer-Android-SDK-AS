package com.ksy.media.widget.controller;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.ksy.media.widget.ui.common.HeartLayout;
import com.ksy.media.widget.ui.common.HorizontalListView;
import com.ksy.media.widget.ui.common.LiveExitDialog;
import com.ksy.media.widget.ui.common.LivePersonDialog;
import com.ksy.media.widget.ui.live.LiveDialogAdapter;
import com.ksy.media.widget.ui.live.LiveDialogInfo;
import com.ksy.media.widget.ui.live.LiveHeadListAdapter;
import com.ksy.mediaPlayer.widget.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MediaPlayerLiveControllerView extends MediaPlayerBaseControllerView implements View.OnClickListener {

	private ImageView liveHead;
	private TextView  liveCloseTextView;
	private TextView  liveReportTextView;

	private ListView liveListView;
	private List<LiveDialogInfo> liveDialogList;
	private LiveDialogAdapter liveDialogAdapter;

	private ImageView livePerson;
	private HorizontalListView liveHorizontalList;
	private LiveHeadListAdapter liveHeadListAdapter;

    private Button liveSwitchButton;
	private Button liveShareButton;
	private EditText  liveEditText;

    private Context mContext;
	private Random mRandom = new Random();
	private Timer mTimer = new Timer();
	private HeartLayout liveHeartLayout;
	private ImageView liveImageView;
	private boolean isSwitch;
	private boolean isLiveListVisible;

	private Handler liveHandler = new Handler();

	public MediaPlayerLiveControllerView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
	}

	public MediaPlayerLiveControllerView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
	}

	public MediaPlayerLiveControllerView(Context context) {
		super(context);
		mContext = context;

		mLayoutInflater.inflate(R.layout.blue_media_player_controller_live, this);

		initViews();
		initListeners();
	}

	@Override
	protected void initViews() {

		liveEditText = (EditText) findViewById(R.id.video_comment_text);
		liveHead = (ImageView)findViewById(R.id.image_live_head);

		liveCloseTextView = (TextView) findViewById(R.id.title_text_close);
		liveReportTextView = (TextView) findViewById(R.id.title_text_report);

		liveListView = (ListView) findViewById(R.id.live_list);
		liveDialogList = new ArrayList<LiveDialogInfo>();
		liveDialogAdapter = new LiveDialogAdapter(liveDialogList, mContext);
		liveListView.setAdapter(liveDialogAdapter);

		liveHorizontalList = (HorizontalListView) findViewById(R.id.live_horizon);
		liveHeadListAdapter = new LiveHeadListAdapter(mContext);
		liveHorizontalList.setAdapter(liveHeadListAdapter);

		livePerson = (ImageView)findViewById(R.id.live_person_image);
		liveSwitchButton = (Button) findViewById(R.id.live_information_switch_bt);
		liveHeartLayout = (HeartLayout)findViewById(R.id.live_image_heart);
        liveImageView = (ImageView) findViewById(R.id.live_image_heart_bt);
		liveShareButton = (Button) findViewById(R.id.live_share_bt);


		liveHorizontalList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
				LivePersonDialog dialogPerson = new LivePersonDialog(mContext);
				dialogPerson.show();
			}
		});

		//heart
		mTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				liveHeartLayout.post(new Runnable() {
					@Override
					public void run() {
						liveHeartLayout.addHeart(randomColor());
					}
				});
			}
		}, 500, 500);

		mHandler.postDelayed(liveListHideRunnable, 5000);
	}

	//delay
	Runnable liveListHideRunnable = new Runnable() {
		@Override
		public void run() {
			liveListView.setVisibility(GONE);
		}
	};

	@Override
	protected void initListeners() {

		liveHead.setOnClickListener(this);
		liveCloseTextView.setOnClickListener(this);
		liveReportTextView.setOnClickListener(this);
		livePerson.setOnClickListener(this);
		liveSwitchButton.setOnClickListener(this);
		liveShareButton.setOnClickListener(this);
	}

	@Override
	void onTimerTicker() {

		long currentTime = mMediaPlayerController.getCurrentPosition();
		long durationTime = mMediaPlayerController.getDuration();

		/*if (durationTime > 0 && currentTime <= durationTime) {
			float percentage = ((float) currentTime) / durationTime;
			updateVideoProgress(percentage);
		}*/
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

	@Override
	public void onClick(View v) {

		int id = v.getId();

		if (id == liveHead.getId()) {
			LivePersonDialog dialogPerson = new LivePersonDialog(mContext);
			dialogPerson.show();

		} else if (id == liveCloseTextView.getId()) {
			LiveExitDialog dialog = new LiveExitDialog(mContext);
			dialog.show();

		} else if (id == liveReportTextView.getId()) {
			LiveExitDialog dialog = new LiveExitDialog(mContext);
			dialog.show();

		} else if (id == livePerson.getId()) {
			//person list button
			if (isLiveListVisible) {
				liveHorizontalList.setVisibility(VISIBLE);
				isLiveListVisible = false;
			} else {
				liveHorizontalList.setVisibility(GONE);
				isLiveListVisible = true;
			}

		} else if (id == liveShareButton.getId()) {
            //TODO

		} else if (id == liveSwitchButton.getId()) {
			if (isSwitch) {
//				liveListView.setVisibility(VISIBLE);
				livePerson.setVisibility(VISIBLE);
				liveHeartLayout.setVisibility(VISIBLE);
				liveHorizontalList.setVisibility(VISIBLE);
				liveShareButton.setVisibility(VISIBLE);
				liveEditText.setVisibility(VISIBLE);
				liveImageView.setVisibility(VISIBLE);
				liveSwitchButton.setText(getResources().getString(R.string.live_info_switch));
				isSwitch = false;

			} else {
//				liveListView.setVisibility(GONE);
				livePerson.setVisibility(GONE);
				liveHeartLayout.setVisibility(GONE);
				liveHorizontalList.setVisibility(GONE);
				liveShareButton.setVisibility(GONE);
				liveEditText.setVisibility(GONE);
				liveImageView.setVisibility(GONE);
				liveSwitchButton.setText(getResources().getString(R.string.live_info_quiet));
				isSwitch = true;
			}
		} else if (id == liveImageView.getId()) {
            //TODO
		}

	}

	private int randomColor() {
		return  Color.rgb(mRandom.nextInt(255), mRandom.nextInt(255), mRandom.nextInt(255));
	}

}
