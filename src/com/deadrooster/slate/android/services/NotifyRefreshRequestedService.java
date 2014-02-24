package com.deadrooster.slate.android.services;

import com.deadrooster.slate.android.util.Constants;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class NotifyRefreshRequestedService extends Service {

	public static final String NOTIFICATION = "com.deadrooster.slate.android.refresh.trigger";

	private final IBinder binder = new RefreshTriggerBinder();

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(Constants.TAG, "NotifyRefreshRequestedService: start");

		notifyRefreshTrigger();
		return Service.START_NOT_STICKY;
	}

	private void notifyRefreshTrigger() {

		Intent i = new Intent(NOTIFICATION);
		sendBroadcast(i);

	}

	@Override
	public IBinder onBind(Intent intent) {
		return this.binder;
	}

	// private Binder
	public class RefreshTriggerBinder extends Binder {

		public NotifyRefreshRequestedService getService() {
			return NotifyRefreshRequestedService.this;
		}
	}

}
