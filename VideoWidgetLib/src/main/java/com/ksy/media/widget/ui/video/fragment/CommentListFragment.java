package com.ksy.media.widget.ui.video.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ksy.mediaPlayer.widget.R;

import java.util.ArrayList;


public class CommentListFragment extends Fragment implements AbsListView.OnItemClickListener, AbsListView.OnScrollListener {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final int STATE_UP = 1;
    private static final int STATE_DOWN = 0;
    private String mParam1;
    private String mParam2;
    private boolean scrollFlag;
    private int lastVisibleItemPosition;
    private int currentState;
    private int lastState;
    private OnFragmentInteractionListener mListener;
    private ListView mListView;
    private CommentListAdapter mVideoAdapter;
    private ArrayList<CommentItem> items;
    private RelativeLayout view_container;
    private View commentLayout;

    public CommentListFragment() {
    }

    public static CommentListFragment newInstance(String param1, String param2) {
        CommentListFragment fragment = new CommentListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        items = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            CommentItem item = new CommentItem();
            item.setComment(getString(R.string.video_commment_item_comment));
            item.setTime(getString(R.string.video_comment_item_time));
            item.setUser(getString(R.string.video_comment_item_user));
            items.add(item);
        }
        mVideoAdapter = new CommentListAdapter(getActivity(), items);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_item, container, false);
        view_container = (RelativeLayout) view.findViewById(R.id.container);
        // Set the adapter
        mListView = (ListView) view.findViewById(R.id.video_comment_list);
        mListView.setAdapter(mVideoAdapter);
        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);
        mListView.setOnScrollListener(this);

        commentLayout = LayoutInflater.from(getActivity()).inflate(
                R.layout.video_pop_layout, null);
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (null != mListener) {
        }
    }

    public void setEmptyText(CharSequence emptyText) {
        View emptyView = mListView.getEmptyView();

        if (emptyView instanceof TextView) {
            ((TextView) emptyView).setText(emptyText);
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL || scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
            scrollFlag = true;
        } else {
            scrollFlag = false;
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        Log.d("eflake", "firstVisibleItem：：" + firstVisibleItem + ":visibleItemCount:" + visibleItemCount + ":totalItemCount:" + totalItemCount);
        if (scrollFlag) {
            if (firstVisibleItem > lastVisibleItemPosition) {
                //Up
                currentState = STATE_UP;
            }
            if (firstVisibleItem < lastVisibleItemPosition) {
                //Down
                currentState = STATE_DOWN;
            }
            if (lastState > currentState) {
                hideCommentLayout();
            } else if (lastState < currentState) {
                showCommentLayout();
            } else {

            }
            if (firstVisibleItem == lastVisibleItemPosition) {
                return;
            }
            lastVisibleItemPosition = firstVisibleItem;
            lastState = currentState;
        }
    }

    public interface OnFragmentInteractionListener {
        public void onFragmentInteraction(String id);
    }

    private void hideCommentLayout() {
        Log.d("ok", "hide");
        view_container.removeView(commentLayout);
    }

    private void showCommentLayout() {
        Log.d("ok", "show");
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, getResources().getDimensionPixelSize(R.dimen.video_comment_distance));
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        view_container.addView(commentLayout, params);
    }

}
