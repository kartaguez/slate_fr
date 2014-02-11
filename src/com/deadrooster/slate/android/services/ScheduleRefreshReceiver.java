package com.deadrooster.slate.android.services;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;

import com.deadrooster.slate.android.preferences.Preferences;

public class ScheduleRefreshReceiver extends BroadcastReceiver {

	private static final int TIME_AFTER_BOOT = 300;
	private static final int REPEAT_TIME_SECONDS = 3600;
	private static final long SECOND_DURATION = 1000;

	@Override
	public void onReceive(Context context, Intent intent) {
		setStartSchedule(context, true);
	}

	public static boolean setScheduleAheadOfReboot(Context context) {

		SharedPreferences settings = context.getSharedPreferences(Preferences.PREFS_NAME, 0);
	    boolean scheduleIsSet = settings.getBoolean(Preferences.PREF_KEY_IS_SCHEDULE_SET, false);
	    int lastVersionUsed = settings.getInt(Preferences.PREF_KEY_LAST_VERSION_USED, -1);
	    int currentVersion = 0;
	    try {
	    	currentVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
			SharedPreferences.Editor editor = settings.edit();
		    editor.putInt(Preferences.PREF_KEY_LAST_VERSION_USED, currentVersion);
		    editor.commit();
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

	    if (!scheduleIsSet || currentVersion != lastVersionUsed) {
	    	setStartSchedule(context, false);
			SharedPreferences.Editor editor = settings.edit();
		    editor.putBoolean(Preferences.PREF_KEY_IS_SCHEDULE_SET, true);
		    editor.commit();
		    return true;
	    } else {
	    	return false;
	    }

	}

	private static void setStartSchedule(Context context, boolean isAfterBoot) {
		AlarmManager service = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(context, LaunchRefreshingBatchReceiver.class);
		PendingIntent pending = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);

		Calendar calendar = Calendar.getInstance();
		if (isAfterBoot) {
			calendar.add(Calendar.SECOND, TIME_AFTER_BOOT);
		} else {
			calendar.add(Calendar.SECOND, REPEAT_TIME_SECONDS);
		}

		service.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), REPEAT_TIME_SECONDS * SECOND_DURATION, pending);
		if (!isAfterBoot) {
			LaunchRefreshingBatchReceiver.launchRefreshingBatch(context);
		}
		
	}

}
