package com.deadrooster.slate.android.services;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ScheduleReceiver extends BroadcastReceiver {

	private static final int TIME_AFTER_BOOT = 300;
	private static final long REPEAT_TIME = 1000 * 3600;

	@Override
	public void onReceive(Context context, Intent intent) {

		AlarmManager service = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(context, StartReceiver.class);
		PendingIntent pending = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);

		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.SECOND, TIME_AFTER_BOOT);

		service.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), REPEAT_TIME, pending);
	}

}
