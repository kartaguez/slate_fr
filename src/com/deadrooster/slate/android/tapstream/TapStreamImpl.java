package com.deadrooster.slate.android.tapstream;

import java.util.HashMap;
import java.util.Iterator;

import com.tapstream.sdk.Event;
import com.tapstream.sdk.Tapstream;

public class TapStreamImpl {

	// TapStream secrets
	public static final String ACCOUNT_NAME = "slate";
	public static final String SDK_SECRET = "hr86go_eQoiAgjZPkCuEZg";

	// Events
	public static class Events {
		public static final String REFRESH = "refresh";
	} 

	public static void fireEvent(String eventName) {
		Event e = new Event(eventName, false);
		Tapstream.getInstance().fireEvent(e);
	}

	public static void fireEvent(String eventName, HashMap<String, Object> params) {
		Event e = new Event(eventName, false);
		if (params != null) {
			Iterator<String> keys = params.keySet().iterator();
			while (keys.hasNext()) {
				String key = keys.next();
				Object value = params.get(key);
				e.addPair(key, value);
			}
		}
		Tapstream.getInstance().fireEvent(e);
	}
}
