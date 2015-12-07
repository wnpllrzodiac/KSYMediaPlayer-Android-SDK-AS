package com.ksy.media.player.db;

public class RecordResult {
		public StringBuffer idBuffer = new StringBuffer();
		public StringBuffer contentBuffer = new StringBuffer();

		public void release() {
			if (idBuffer != null) {
				idBuffer = null;
			}
			if (contentBuffer != null) {
				contentBuffer = null;
			}
		}
}
