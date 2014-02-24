package com.deadrooster.slate.android.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.deadrooster.slate.android.util.Connectivity;
import com.deadrooster.slate.android.util.Constants;

public class LaunchRefreshingBatchReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(Constants.TAG, "LaunchRefreshingBatchReceiver: onReceive");
		launchRefreshingBatch(context);
	}

	public static void launchRefreshingBatch(Context context) {
		Log.d(Constants.TAG, "LaunchRefreshingBatchReceiver: launchRefreshingBatch");

		if (Connectivity.isWifiConnected(context)) {
			Log.d(Constants.TAG, "LaunchRefreshingBatchReceiver: launchRefreshingBatch: go (WiFi connected)");

			Intent i = new Intent(context, NotifyRefreshRequestedService.class);
			context.startService(i);
	
			i = new Intent(context, PerformRefreshService.class);
			context.startService(i);
		} else {
			Log.d(Constants.TAG, "LaunchRefreshingBatchReceiver: launchRefreshingBatch: no go (WiFi not connected)");
		}
	}

}
