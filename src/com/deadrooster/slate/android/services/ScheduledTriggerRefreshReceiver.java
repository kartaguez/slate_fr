package com.deadrooster.slate.android.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ScheduledTriggerRefreshReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Intent i = new Intent(context, TriggerRefreshService.class);
		context.startService(i);

		i = new Intent(context, PerformRefreshService.class);
		context.startService(i);
	}

}
