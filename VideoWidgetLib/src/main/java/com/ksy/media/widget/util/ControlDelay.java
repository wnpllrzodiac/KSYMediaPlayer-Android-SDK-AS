package com.ksy.media.widget.util;

public class ControlDelay {

	private boolean isDelay;

	public boolean isDelay() {
		return isDelay;
	}

	public void setDelay(boolean isDelay) {
		this.isDelay = isDelay;
	}

	private static ControlDelay mInstance;
	private static Object mLockObject = new Object();

	public static ControlDelay getInstance() {
		if (null == mInstance) {
			synchronized (mLockObject) {
				if (null == mInstance) {
					mInstance = new ControlDelay();
				}
			}
		}
		return mInstance;
	}

}
