package com.deadrooster.slate.android.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StartReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Intent service = new Intent(context, RefreshService.class);
		context.startService(service);
	}

}
