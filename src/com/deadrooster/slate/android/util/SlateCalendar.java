package com.deadrooster.slate.android.util;

import java.util.Calendar;
import java.util.GregorianCalendar;


public class SlateCalendar extends GregorianCalendar {

	private static final long serialVersionUID = -4131121417448096302L;

    public static boolean isSameDay(Calendar cal1, Calendar cal2) {
        if (cal1 == null || cal2 == null) {
            throw new IllegalArgumentException("The dates must not be null");
        }
        return (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
    }

    public boolean isToday() {

		Calendar today = Calendar.getInstance();
		today.setTimeInMillis(System.currentTimeMillis());

		return isSameDay(this, today);
		
	}

	public boolean isYesterday() {

		Calendar yesterday = Calendar.getInstance();
		yesterday.setTimeInMillis(System.currentTimeMillis());
		yesterday.add(Calendar.DAY_OF_YEAR, -1);

		return isSameDay(this, yesterday);
		
	}

}
