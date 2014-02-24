package com.deadrooster.slate.android.model;

import android.provider.BaseColumns;

public class Model {

	public static class Entries implements BaseColumns {

		private Entries() {}

		// Table name
		public static final String TABLE_NAME = "entries";
		public static final String TABLE_TEMP_NAME = "entries_temp";

		// Columns
		public static final String CATEGORY = "category";
		public static final String TITLE = "title";
		public static final String DESCRIPTION = "description";
		public static final String PREVIEW = "preview";
		public static final String THUMBNAIL_URL = "thumbnail_url";
		public static final String THUMBNAIL_DATA = "thumbnail_data";
		public static final String PUBLICATION_DATE = "publication_date";
		public static final String AUTHOR = "author";

		// Default sort order
		public static final String DEFAULT_ORDER_BY = _ID + " ASC";

		public static class Tags {
			public static final String CHANNEL = "channel";
			public static final String ITEM = "item";
			public static final String TITLE = "title";
			public static final String DESCRIPTION = "description";
			public static final String DEK = "media:dek";
			public static final String THUMBNAIL = "media:thumbnail";
			public static final String PUBDATE = "pubDate";
			public static final String AUTHOR = "author";
		}

	}

}
