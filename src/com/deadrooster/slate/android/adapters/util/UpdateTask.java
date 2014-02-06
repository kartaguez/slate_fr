package com.deadrooster.slate.android.adapters.util;

import java.io.ByteArrayOutputStream;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;

import com.deadrooster.slate.android.model.Model.Entries;
import com.deadrooster.slate.android.provider.Uris;

public class UpdateTask extends AsyncTask<String, Integer, Integer> {

	private static Context context;
	private long entryId;
	private Bitmap bitmap;

	public UpdateTask(Context context, long entryId, Bitmap bitmap) {
		if (UpdateTask.context == null) {
			synchronized(UpdateTask.class) {
				if (UpdateTask.context == null) {
					UpdateTask.context = context;
				}
			}
		}
		this.entryId = entryId;
		this.bitmap = bitmap;
	}

	@Override
	protected Integer doInBackground(String... params) {

		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		this.bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
		byte[] data = stream.toByteArray();

		ContentValues contentValues = new ContentValues();
		contentValues.put(Entries.THUMBNAIL_DATA, data);

		final Uri uri = ContentUris.withAppendedId(Uris.Entries.CONTENT_URI_ENTRY_ID, entryId);
		int id = UpdateTask.context.getContentResolver().update(uri, contentValues, Entries._ID + " = " + entryId, null);

		return Integer.valueOf(id);
	}

}
