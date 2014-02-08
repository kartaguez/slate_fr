package com.deadrooster.slate.android.services;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.deadrooster.slate.android.preferences.Preferences;

public class ScheduleReceiver extends BroadcastReceiver {

	private static final int TIME_AFTER_INSTALL = 0;
	private static final int TIME_AFTER_BOOT = 300;
	private static final long REPEAT_TIME_SECONDS = 7200;
	private static final long SECOND_DURATION = 1000;

	@Override
	public void onReceive(Context context, Intent intent) {
		setStartSchedule(context, true);
	}

	public static void setScheduleAheadOfReboot(Context context) {

		SharedPreferences settings = context.getSharedPreferences(Preferences.PREFS_NAME, 0);
	    boolean scheduleIsSet = settings.getBoolean(Preferences.PREF_KEY_IS_SCHEDULE_SET, false);
	    if (!scheduleIsSet) {
	    	setStartSchedule(context, false);
			SharedPreferences.Editor editor = settings.edit();
		    editor.putBoolean(Preferences.PREF_KEY_IS_SCHEDULE_SET, true);
		    editor.commit();
	    }

	}

	private static void setStartSchedule(Context context, boolean isAfterBoot) {
		AlarmManager service = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(context, ScheduledTriggerRefreshReceiver.class);
		PendingIntent pending = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);

		Calendar calendar = Calendar.getInstance();
		if (isAfterBoot) {
			calendar.add(Calendar.SECOND, TIME_AFTER_BOOT);
		} else {
			calendar.add(Calendar.SECOND, TIME_AFTER_INSTALL);
		}

		service.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), REPEAT_TIME_SECONDS * SECOND_DURATION, pending);
	}

}
