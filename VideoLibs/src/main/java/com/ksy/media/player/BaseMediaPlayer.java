package com.ksy.media.player;

import android.util.Log;
import com.ksy.media.player.exception.Ks3ClientException;
import com.ksy.media.player.log.LogClient;
import com.ksy.media.player.log.LogRecord;
import com.ksy.media.player.util.Constants;

/**
 * 
 *   Common IMediaPlayer implement
 */
public abstract class BaseMediaPlayer implements IMediaPlayer {
    private OnPreparedListener mOnPreparedListener;
    private OnCompletionListener mOnCompletionListener;
    private OnBufferingUpdateListener mOnBufferingUpdateListener;
    private OnSeekCompleteListener mOnSeekCompleteListener;
    private OnVideoSizeChangedListener mOnVideoSizeChangedListener;
    private OnErrorListener mOnErrorListener;
    private OnInfoListener mOnInfoListener;
    private OnDRMRequiredListener mOnDRMRequiredListener;
    private OnDebugInfoListener mOnDebugInfoListener;
    private OnNetSpeedListener mOnNetSpeedListener;
    
    public static LogRecord logRecord = LogRecord.getInstance();
    public static LogClient logClient;
    
    public final void setOnPreparedListener(OnPreparedListener listener) {
        mOnPreparedListener = listener;
    }

    public final void setOnCompletionListener(OnCompletionListener listener) {
        mOnCompletionListener = listener;
    }

    public final void setOnDebugInfoListener(OnDebugInfoListener listener) {
    	mOnDebugInfoListener = listener;
    }
    
    public final void setOnBufferingUpdateListener(
            OnBufferingUpdateListener listener) {
        mOnBufferingUpdateListener = listener;
    }

    public final void setOnNetSpeedUpdateListener(
    		OnNetSpeedListener listener) {
    	mOnNetSpeedListener = listener;
    }
    
    public final void setOnSeekCompleteListener(OnSeekCompleteListener listener) {
        mOnSeekCompleteListener = listener;
    }

    public final void setOnVideoSizeChangedListener(
            OnVideoSizeChangedListener listener) {
        mOnVideoSizeChangedListener = listener;
    }

    public final void setOnErrorListener(OnErrorListener listener) {
        mOnErrorListener = listener;
    }

    public final void setOnInfoListener(OnInfoListener listener) {
        mOnInfoListener = listener;
    }
    
    public final void setOnDRMRequiredListener(OnDRMRequiredListener listener){
    	mOnDRMRequiredListener = listener;
    }

    
    public void resetListeners() {
        mOnPreparedListener = null;
        mOnBufferingUpdateListener = null;
        mOnCompletionListener = null;
        mOnSeekCompleteListener = null;
        mOnVideoSizeChangedListener = null;
        mOnErrorListener = null;
        mOnInfoListener = null;
        mOnDRMRequiredListener = null;
        mOnDebugInfoListener = null;
        mOnNetSpeedListener = null;
    }

    public void attachListeners(IMediaPlayer mp) {
        mp.setOnPreparedListener(mOnPreparedListener);
        mp.setOnBufferingUpdateListener(mOnBufferingUpdateListener);
        mp.setOnCompletionListener(mOnCompletionListener);
        mp.setOnSeekCompleteListener(mOnSeekCompleteListener);
        mp.setOnVideoSizeChangedListener(mOnVideoSizeChangedListener);
        mp.setOnErrorListener(mOnErrorListener);
        mp.setOnInfoListener(mOnInfoListener);
        mp.setOnDRMRequiredListener(mOnDRMRequiredListener);
        mp.setOnDebugInfoListener(mOnDebugInfoListener);
        mp.setOnNetSpeedUpdateListener(mOnNetSpeedListener);
    }

    protected final void notifyOnPrepared() {
        if (mOnPreparedListener != null)
            mOnPreparedListener.onPrepared(this);
    }

    protected final void notifyOnCompletion() {
        if (mOnCompletionListener != null)
            mOnCompletionListener.onCompletion(this);
    }

    protected final void notifyOnBufferingUpdate(int percent) {
        if (mOnBufferingUpdateListener != null)
            mOnBufferingUpdateListener.onBufferingUpdate(this, percent);
    }
    
    protected final void notifyOnNetSpeedUpdate(int arg1, int arg2) {
    	if (mOnNetSpeedListener != null) {
    		mOnNetSpeedListener.onNetSpeedUpdate(this, arg1, arg2);
    	}
    }

    protected final void notifyOnDebugInfo(int type,int arg1,int arg2) {
        if (mOnDebugInfoListener != null)
            mOnDebugInfoListener.onDebugInfo(this,type,arg1,arg2);
    }
    
    protected final void notifyOnSeekComplete() {
        if (mOnSeekCompleteListener != null)
            mOnSeekCompleteListener.onSeekComplete(this);
        
        logRecord.setSeekStatus("ok");
        logRecord.setSeekMessage("SeekComplete success");
        
        if (logClient.mSwitch) {
            
            try {
//				Log.d(Constants.LOG_TAG, "seekend =" + logRecord.getSeekEndJson());
            	logRecord.setDate(logClient.getInstance().logGetData.currentTimeGmt());
				LogClient.getInstance().put(logRecord.getSeekEndJson());
				
			} catch (Ks3ClientException e) {
				e.printStackTrace();
				Log.e(Constants.LOG_TAG, "BaseMediaPlayer e = " + e);
			}
        }
    }

    protected final void notifyOnVideoSizeChanged(int width, int height,
            int sarNum, int sarDen) {
        if (mOnVideoSizeChangedListener != null)
            mOnVideoSizeChangedListener.onVideoSizeChanged(this, width, height,
                    sarNum, sarDen);
    }


    protected final boolean notifyOnError(int what, int extra) {
        if (mOnErrorListener != null) {
            return mOnErrorListener.onError(this, what, extra);
        }
        
        return false;
    }

    protected final boolean notifyOnInfo(int what, int extra) {
        if (mOnInfoListener != null)
            return mOnInfoListener.onInfo(this, what, extra);
        return false;
    }
    
    protected final void notifyOnDRMRequired(int what, int extra,String version) {
        if (mOnDRMRequiredListener != null)
           mOnDRMRequiredListener.OnDRMRequired(this, what, extra,version);
    }
}
