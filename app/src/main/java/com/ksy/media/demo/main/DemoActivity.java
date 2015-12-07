package com.ksy.media.demo.main;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import com.ksy.media.demo.R;

import java.util.ArrayList;

public class DemoActivity extends AppCompatActivity {

    private RecyclerView mRecycleView;
    private ArrayList<DemoContent> demoList;
    private DemoListAdapter mAdapter;
    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupDemoContent();
        setupViews();
    }

    private void setupDemoContent() {
        demoList = new ArrayList<>();
        demoList.add(new DemoContent("PhoneLive"));
        demoList.add(new DemoContent("PhoneLive"));
        demoList.add(new DemoContent("PhoneLive"));
    }

    private void setupViews() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if (mToolbar != null) {
//            mToolbar.setNavigationIcon(getResources().getDrawable(R.mipmap.ksy_logo));
            setSupportActionBar(mToolbar);
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Handle your drawable state here
                }
            });
        }
        mRecycleView = (RecyclerView) findViewById(R.id.demo_list);
//        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(DemoActivity.this);
//        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
//        GridLayoutManager gridLayoutManager = new GridLayoutManager(DemoActivity.this,2);
        StaggeredGridLayoutManager staggeredGridLayoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        mRecycleView.setLayoutManager(staggeredGridLayoutManager);
        mAdapter = new DemoListAdapter(DemoActivity.this, demoList);
        mRecycleView.setAdapter(mAdapter);
        mRecycleView.addItemDecoration(new DemoListItemSpaceDecoration(getResources().getDimensionPixelSize(R.dimen.demo_card_hori_margin)));
//      mRecycleView.setItemAnimator(new SlideInOutBottomItemAnimator(mRecycleView));
        mAdapter.setDemoListClickListener(new DemoListAdapter.DemoListClickListener() {
            @Override
            public void onDemoListClicked(int position, DemoContent demoContent) {
                Toast.makeText(DemoActivity.this, "demo=" + demoContent.name, Toast.LENGTH_SHORT).show();
            }
        });
    }

}
