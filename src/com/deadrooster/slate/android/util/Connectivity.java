package com.deadrooster.slate.android.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class Connectivity {

	public static boolean isWifiConnected(Context context) {

		ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		return info.isConnected();
	}

}
