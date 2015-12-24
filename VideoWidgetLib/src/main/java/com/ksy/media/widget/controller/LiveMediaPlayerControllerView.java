package com.ksy.media.widget.controller;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.ksy.media.widget.ui.common.HeartLayout;
import com.ksy.media.widget.ui.common.HorizontalListView;
import com.ksy.media.widget.ui.common.LiveAnchorDialog;
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

public class LiveMediaPlayerControllerView extends FrameLayout implements View.OnClickListener {

	private ImageView liveHead;
	private ImageView liveStateImage;
	private TextView timeTextView;
	private TextView  liveCloseTextView;
	private TextView  liveReportTextView;

	private ListView liveListView;
	private List<LiveDialogInfo> liveDialogList;
	private LiveDialogAdapter liveDialogAdapter;
	private TextView noticeTextViewLive;

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
    private TextView livePraiseCountTextView;
	private TextView livePersonCountTextView;
	private int livePraiseCount;

	private Handler liveHandler = new Handler();
	protected LayoutInflater mLiveLayoutInflater;


	public LiveMediaPlayerControllerView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
	}

	public LiveMediaPlayerControllerView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
	}

	public LiveMediaPlayerControllerView(Context context) {
		super(context);
		mContext = context;

		mLiveLayoutInflater = LayoutInflater.from(getContext());
		mLiveLayoutInflater.inflate(R.layout.blue_media_player_controller_live, this);

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

		liveEditText = (EditText) findViewById(R.id.video_comment_text);
		liveHead = (ImageView)findViewById(R.id.image_live_head);
		timeTextView = (TextView) findViewById(R.id.textViewTime);
		liveCloseTextView = (TextView) findViewById(R.id.title_text_close);
		liveReportTextView = (TextView) findViewById(R.id.title_text_report);
		livePersonCountTextView = (TextView) findViewById(R.id.live_person_count_textview);
        livePraiseCountTextView = (TextView) findViewById(R.id.live_praise_count_text);

		liveListView = (ListView) findViewById(R.id.live_list);
		liveDialogList = new ArrayList<LiveDialogInfo>();
		liveDialogAdapter = new LiveDialogAdapter(liveDialogList, mContext);
		liveListView.setAdapter(liveDialogAdapter);
		noticeTextViewLive = (TextView)findViewById(R.id.notice_text_live);

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
						livePraiseCount ++;
						livePraiseCountTextView.setText(String.valueOf(livePraiseCount));
					}
				});
			}
		}, 500, 300);

		liveHandler.postDelayed(liveListHideRunnable, 5000);
	}

	//delay
	Runnable liveListHideRunnable = new Runnable() {
		@Override
		public void run() {
			liveListView.setVisibility(GONE);
			noticeTextViewLive.setVisibility(GONE);
		}
	};


	protected void initListeners() {

		liveHead.setOnClickListener(this);
		liveCloseTextView.setOnClickListener(this);
		liveReportTextView.setOnClickListener(this);
		livePerson.setOnClickListener(this);
		liveSwitchButton.setOnClickListener(this);
		liveShareButton.setOnClickListener(this);
	}

//	@Override
	void onTimerTicker() {

//		long currentTime = mMediaPlayerController.getCurrentPosition();
//		long durationTime = mMediaPlayerController.getDuration();

		/*if (durationTime > 0 && currentTime <= durationTime) {
			float percentage = ((float) currentTime) / durationTime;
			updateVideoProgress(percentage);
		}*/
	}


	public void updateVideoTitle(String title) {
		if (!TextUtils.isEmpty(title)) {

		}
	}

	@Override
	public void onClick(View v) {

		int id = v.getId();

		if (id == liveHead.getId()) {
			LiveAnchorDialog dialogPerson = new LiveAnchorDialog(mContext);
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
				livePerson.setVisibility(VISIBLE);
				liveHeartLayout.setVisibility(VISIBLE);
				liveHorizontalList.setVisibility(VISIBLE);
				liveShareButton.setVisibility(VISIBLE);
				liveEditText.setVisibility(VISIBLE);
				liveImageView.setVisibility(VISIBLE);
				liveSwitchButton.setText(getResources().getString(R.string.live_info_switch));
				livePersonCountTextView.setVisibility(VISIBLE);
				livePraiseCountTextView.setVisibility(VISIBLE);

				isSwitch = false;

			} else {
				livePerson.setVisibility(GONE);
				liveHeartLayout.setVisibility(GONE);
				liveHorizontalList.setVisibility(GONE);
				liveShareButton.setVisibility(GONE);
				liveEditText.setVisibility(GONE);
				liveImageView.setVisibility(GONE);
				liveSwitchButton.setText(getResources().getString(R.string.live_info_quiet));
				livePersonCountTextView.setVisibility(GONE);
				livePraiseCountTextView.setVisibility(GONE);
				isSwitch = true;
			}
		} else if (id == liveImageView.getId()) {
            //TODO
			livePraiseCount ++;
			livePraiseCountTextView.setText(String.valueOf(livePraiseCount));
		}

	}

	private int randomColor() {
		return  Color.rgb(mRandom.nextInt(255), mRandom.nextInt(255), mRandom.nextInt(255));
	}

}
