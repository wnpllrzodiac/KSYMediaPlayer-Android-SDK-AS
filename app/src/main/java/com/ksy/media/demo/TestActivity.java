package com.ksy.media.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class TestActivity extends Activity{
	private Button tsetButton;
	private Button delayButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_test);
		
		//低延迟模式
		tsetButton = (Button)findViewById(R.id.bt_low_delay);
		tsetButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(TestActivity.this, VideoPlayerActivity.class);
				intent.putExtra("is_delay", true);
				startActivity(intent);			
			}
		});
		
		//延迟模式
		delayButton = (Button)findViewById(R.id.bt_delay);
		delayButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(TestActivity.this, VideoPlayerActivity.class);
				intent.putExtra("is_delay", false);
				startActivity(intent);	
			}
		});
		
	}
	
	
	@Override
	protected void onPause() {
		super.onPause();
	}
	
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
}


