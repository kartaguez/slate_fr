package com.deadrooster.slate.android.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.deadrooster.slate.android.util.Connectivity;

public class LaunchRefreshingBatchReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {

		launchRefreshingBatch(context);
	}

	public static void launchRefreshingBatch(Context context) {
		if (Connectivity.isWifiConnected(context)) {

			Intent i = new Intent(context, NotifyRefreshRequestedService.class);
			context.startService(i);
	
			i = new Intent(context, PerformRefreshService.class);
			context.startService(i);
		}
	}

}
