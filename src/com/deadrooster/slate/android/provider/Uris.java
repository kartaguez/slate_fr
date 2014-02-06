package com.deadrooster.slate.android.provider;

import android.net.Uri;

import com.deadrooster.slate.android.model.Model;

public class Uris {

	public static final String ALL = "*";
	public static final String ITEM = "#";
	public static final String _DISTINCT = "_distinct";

	public static class Entries {
		public static final Uri CONTENT_URI_ENTRIES = Uri.parse(Provider.CONTENT_URI + "/" + Model.Entries.TABLE_NAME + "/" + ALL);
		public static final Uri CONTENT_URI_ENTRIES_DISTINCT = Uri.parse(Provider.CONTENT_URI + "/" + Model.Entries.TABLE_NAME + _DISTINCT + "/" + ALL);
		public static final Uri CONTENT_URI_ENTRY_ID = Uri.parse(Provider.CONTENT_URI + "/" + Model.Entries.TABLE_NAME + "/" + ITEM);
	}
}
