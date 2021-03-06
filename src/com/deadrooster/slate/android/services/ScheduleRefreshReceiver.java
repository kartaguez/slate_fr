package com.deadrooster.slate.android.services;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import com.deadrooster.slate.android.preferences.Preferences;
import com.deadrooster.slate.android.util.Constants;

public class ScheduleRefreshReceiver extends BroadcastReceiver {

	private static final int TIME_AFTER_BOOT = 300;
	private static final int REPEAT_TIME_SECONDS = 3600;
	private static final long SECOND_DURATION = 1000;

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(Constants.TAG, "ScheduleRefreshReceiver: onReceive");

		setStartSchedule(context, true);
	}

	public static boolean setScheduleAfterFirstLaunch(Context context) {
		Log.d(Constants.TAG, "ScheduleRefreshReceiver: setScheduleAfterFirstLaunch");

		SharedPreferences settings = context.getSharedPreferences(Preferences.PREFS_NAME, 0);
	    boolean scheduleIsSet = settings.getBoolean(Preferences.PREF_KEY_SCHEDULE_IS_SET, false);
	    int lastVersionUsed = settings.getInt(Preferences.PREF_KEY_LAST_VERSION_USED, -1);
	    int currentVersion = 0;
	    try {
	    	currentVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

	    if (!scheduleIsSet || currentVersion != lastVersionUsed) {
			SharedPreferences.Editor editor = settings.edit();
		    editor.putInt(Preferences.PREF_KEY_LAST_VERSION_USED, currentVersion);
	    	setStartSchedule(context, false);
		    editor.putBoolean(Preferences.PREF_KEY_SCHEDULE_IS_SET, true);
		    editor.commit();
		    return true;
	    } else {
	    	Log.d(Constants.TAG, "ScheduleRefreshReceiver: no need to set schedule");
	    	return false;
	    }

	}

	private static void setStartSchedule(Context context, boolean isAfterBoot) {
		Log.d(Constants.TAG, "ScheduleRefreshReceiver: setStartSchedule: isAfterBoot = " + isAfterBoot);

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

		SharedPreferences settings = context.getSharedPreferences(Preferences.PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
	    editor.putBoolean(Preferences.PREF_KEY_SCHEDULE_IS_SET, true);
	    editor.commit();

	}

}
