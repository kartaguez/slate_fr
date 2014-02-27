package com.deadrooster.slate.android.provider;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import com.deadrooster.slate.android.db.DatabaseHelper;
import com.deadrooster.slate.android.model.Model;

public class Provider extends ContentProvider {

	public static final String SCHEME = "content://";
	public static final String AUTHORITY = "com.deadrooster.slate.android";
	public static final Uri CONTENT_URI = Uri.parse(SCHEME + AUTHORITY);

	private static final UriMatcher uriMatcher;
	private static final HashMap<String, String> entriesPM;

	private DatabaseHelper databaseHelper;

	@Override
	public boolean onCreate() {
		this.databaseHelper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(
			Uri uri,
			String[] projection,
			String selection,
			String[] selectionArgs,
			String sortOrder) {

		// Set querybuilder
		SQLiteQueryBuilder qb = null;

		switch (uriMatcher.match(uri)) {

			case UriMatchingCodes.Entries.ENTRIES:
	
				qb = new SQLiteQueryBuilder();
				qb.setTables(Model.Entries.TABLE_NAME);
				qb.setProjectionMap(entriesPM);
				break;

			case UriMatchingCodes.Entries.ENTRIES_DISTINCT:
				
				qb = new SQLiteQueryBuilder();
				qb.setTables(Model.Entries.TABLE_NAME);
				qb.setProjectionMap(entriesPM);
				qb.setDistinct(true);
				break;

			case UriMatchingCodes.Entries.ENTRIES_TEMP:
				
				qb = new SQLiteQueryBuilder();
				qb.setTables(Model.Entries.TABLE_TEMP_NAME);
				qb.setProjectionMap(entriesPM);
				break;

			case UriMatchingCodes.Entries.ENTRIES_TEMP_DISTINCT:
				
				qb = new SQLiteQueryBuilder();
				qb.setTables(Model.Entries.TABLE_TEMP_NAME);
				qb.setProjectionMap(entriesPM);
				qb.setDistinct(true);
				break;

			default:
				throw new IllegalArgumentException(UriMatchingCodes.ERR_UNKNOWN_URI + " :" + uri);
		}

		// Set order by
		String orderBy = null;
		if (TextUtils.isEmpty(sortOrder)) {
			orderBy = Model.Entries.DEFAULT_ORDER_BY;
		} else {
			orderBy = sortOrder;
		}

		// Open database
		SQLiteDatabase db = databaseHelper.getReadableDatabase();

		// Generate cursor
		Cursor c = null;
		if (qb != null) {
			c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
		}

		// Set URI watch
		c.setNotificationUri(getContext().getContentResolver(), uri);

		// Return
		return c;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {

		String table = null;
		int uriMatchCode = uriMatcher.match(uri);

		switch (uriMatchCode) {
			case UriMatchingCodes.Entries.ENTRIES:
				table = Model.Entries.TABLE_NAME;
				break;
			case UriMatchingCodes.Entries.ENTRIES_TEMP:
				table = Model.Entries.TABLE_TEMP_NAME;
				break;
			default:
				throw new IllegalArgumentException(UriMatchingCodes.ERR_UNKNOWN_URI + uri);
		}

		// Clean content values
		ContentValues values;
		if (initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}

		if (!values.containsKey(Model.Entries.TITLE)) {
			values.put(Model.Entries.TITLE, "");
		}
		if (!values.containsKey(Model.Entries.DESCRIPTION)) {
			values.put(Model.Entries.DESCRIPTION, "");
		}
		if (!values.containsKey(Model.Entries.PREVIEW)) {
			values.put(Model.Entries.PREVIEW, "");
		}
		if (!values.containsKey(Model.Entries.THUMBNAIL_URL)) {
			values.put(Model.Entries.THUMBNAIL_URL, "");
		}
		if (!values.containsKey(Model.Entries.PUBLICATION_DATE)) {
			values.put(Model.Entries.PUBLICATION_DATE, "");
		}
		if (!values.containsKey(Model.Entries.AUTHOR)) {
			values.put(Model.Entries.AUTHOR, "");
		}

		// Open database
		SQLiteDatabase db = databaseHelper.getWritableDatabase();

		// Insert data
		long rowId = db.insert(table, null, values);

		// Generate uri to return
		if (rowId > 0) {
			Uri insertedObjectUri = null;
			switch (uriMatchCode) {
				case UriMatchingCodes.Entries.ENTRIES:
					insertedObjectUri = ContentUris.withAppendedId(Uris.Entries.CONTENT_URI_ENTRY_ID, rowId);
					return insertedObjectUri;
				case UriMatchingCodes.Entries.ENTRIES_TEMP:
					insertedObjectUri = ContentUris.withAppendedId(Uris.Entries.CONTENT_URI_ENTRY_TEMP_ID, rowId);
					return insertedObjectUri;
				default:
					throw new IllegalArgumentException(UriMatchingCodes.ERR_UNKNOWN_URI + uri);
			}
		} else {
			throw new IllegalArgumentException(Errors.URI_INSERT_FAILED + uri);
		}
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {

		SQLiteDatabase db = databaseHelper.getWritableDatabase();

		int count = 0;

		switch (uriMatcher.match(uri)) {
			case UriMatchingCodes.Entries.ENTRIES :
				count = db.delete(Model.Entries.TABLE_NAME, selection, selectionArgs);
				break;
			case UriMatchingCodes.Entries.ENTRIES_TEMP :
				count = db.delete(Model.Entries.TABLE_TEMP_NAME, selection, selectionArgs);
				break;
			default:
				throw new IllegalArgumentException(UriMatchingCodes.ERR_UNKNOWN_URI + uri);
		}

		return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

		SQLiteDatabase db = databaseHelper.getWritableDatabase();

		int count = 0;

		switch (uriMatcher.match(uri)) {
			case UriMatchingCodes.Entries.ENTRY_ID :
				count = db.update(Model.Entries.TABLE_NAME, values, selection, selectionArgs);
				break;
			case UriMatchingCodes.Entries.ENTRY_TEMP_ID :
				count = db.update(Model.Entries.TABLE_TEMP_NAME, values, selection, selectionArgs);
				break;
			default:
				throw new IllegalArgumentException(UriMatchingCodes.ERR_UNKNOWN_URI + uri);
		}

		return count;
		
	}

	public static class Errors {
		public static final String URI_INSERT_FAILED = "Failed to insert row into "; 
	}

	static {

		// Init uriMather
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

		uriMatcher.addURI(AUTHORITY, Model.Entries.TABLE_NAME + "/" + Uris.ITEM, UriMatchingCodes.Entries.ENTRY_ID);
		uriMatcher.addURI(AUTHORITY, Model.Entries.TABLE_NAME + "/" + Uris.ALL, UriMatchingCodes.Entries.ENTRIES);
		uriMatcher.addURI(AUTHORITY, Model.Entries.TABLE_NAME + Uris._DISTINCT + "/" + Uris.ALL, UriMatchingCodes.Entries.ENTRIES_DISTINCT);

		uriMatcher.addURI(AUTHORITY, Model.Entries.TABLE_TEMP_NAME + "/" + Uris.ITEM, UriMatchingCodes.Entries.ENTRY_TEMP_ID);
		uriMatcher.addURI(AUTHORITY, Model.Entries.TABLE_TEMP_NAME + "/" + Uris.ALL, UriMatchingCodes.Entries.ENTRIES_TEMP);
		uriMatcher.addURI(AUTHORITY, Model.Entries.TABLE_TEMP_NAME + Uris._DISTINCT + "/" + Uris.ALL, UriMatchingCodes.Entries.ENTRIES_TEMP_DISTINCT);

		// Init EntriesPM
		entriesPM = new HashMap<String, String>();
		entriesPM.put(Model.Entries._ID, Model.Entries._ID);
		entriesPM.put(Model.Entries.TITLE, Model.Entries.TITLE);
		entriesPM.put(Model.Entries.CATEGORY, Model.Entries.CATEGORY);
		entriesPM.put(Model.Entries.DESCRIPTION, Model.Entries.DESCRIPTION);
		entriesPM.put(Model.Entries.PREVIEW, Model.Entries.PREVIEW);
		entriesPM.put(Model.Entries.THUMBNAIL_URL, Model.Entries.THUMBNAIL_URL);
		entriesPM.put(Model.Entries.THUMBNAIL_DATA, Model.Entries.THUMBNAIL_DATA);
		entriesPM.put(Model.Entries.PUBLICATION_DATE, Model.Entries.PUBLICATION_DATE);
		entriesPM.put(Model.Entries.AUTHOR, Model.Entries.AUTHOR);
		entriesPM.put("initial",  "substr("+ Model.Entries.TITLE + ", 1, 1)");
	}
}
