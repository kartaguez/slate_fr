package com.deadrooster.slate.android.db;

import com.deadrooster.slate.android.model.Model;

public class DatabaseSchema {

	public static final String DROP_TABLE = "DROP TABLE IF EXISTS ";

	public static final String CREATE_TABLE_ENTRIES = "CREATE TABLE "
			+ Model.Entries.TABLE_NAME
			+ " ("
            + Model.Entries._ID + " INTEGER PRIMARY KEY, "
            + Model.Entries.TITLE + " TEXT NOT NULL, "
            + Model.Entries.CATEGORY + " INTEGER, "
            + Model.Entries.DESCRIPTION + " TEXT NOT NULL, "
            + Model.Entries.PREVIEW + " TEXT NOT NULL, "
            + Model.Entries.THUMBNAIL_URL + " TEXT NOT NULL, "
            + Model.Entries.THUMBNAIL_DATA + " BLOB, "
            + Model.Entries.PUBLICATION_DATE + " TEXT NOT NULL, "
            + Model.Entries.AUTHOR + " TEXT NOT NULL"
            + ");";

}
