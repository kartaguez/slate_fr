package com.deadrooster.slate.android.adapters.util;

import java.util.ArrayList;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.deadrooster.slate.android.activities.EntryListActivity;
import com.deadrooster.slate.android.model.Model.Entries;
import com.deadrooster.slate.android.provider.Provider;
import com.deadrooster.slate.android.provider.Uris;
import com.deadrooster.slate.android.services.PerformRefreshService;
import com.deadrooster.slate.android.util.Constants;

public class LoadNewDataTask extends AsyncTask<String, Void, Boolean> {

	public static final String IN_PROGRESS = "in_progress";

	private static Context context;

	public LoadNewDataTask(Context context) {
		if (LoadNewDataTask.context == null) {
			synchronized(LoadNewDataTask.class) {
				if (LoadNewDataTask.context == null) {
					LoadNewDataTask.context = context;
				}
			}
		}
	}

	@Override
	protected Boolean doInBackground(String... params) {
		Log.d(Constants.TAG, "LoadNewDataTask: doInBackground: start");

		DataUpdateSyncLock.getInstance().setDataBeingSwapped(true);

		ContentResolver cr = LoadNewDataTask.context.getContentResolver();
		Cursor c = null;
		ContentValues values = null;

		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

		int nbCategories = Categories.getInstance(context).getCategories().size();
		for (int i = 0; i < nbCategories; i++) {
			ops.add(ContentProviderOperation
					.newDelete(Uris.Entries.CONTENT_URI_ENTRIES)
					.withSelection(PerformRefreshService.SELECTION_DELETE, new String[] {Integer.toString(i)})
					.build());
			c = LoadNewDataTask.context.getContentResolver().query(Uris.Entries.CONTENT_URI_ENTRIES_TEMP, EntryListActivity.PROJECTION, EntryListActivity.SELECTION, new String[] {Integer.toString(i)}, null);

			while (c.moveToNext()) {
				values = new ContentValues();
				values.put(Entries.CATEGORY, i);
				values.put(Entries.TITLE, c.getString(1));
				values.put(Entries.DESCRIPTION, c.getString(2));
				values.put(Entries.PREVIEW, c.getString(3));
				values.put(Entries.THUMBNAIL_URL, c.getString(4));
				values.put(Entries.PUBLICATION_DATE, c.getString(6));
				values.put(Entries.AUTHOR, c.getString(7));
				ops.add(ContentProviderOperation
						.newInsert(Uris.Entries.CONTENT_URI_ENTRIES)
						.withValues(values)
						.build());
			}
		}
		if (c != null) {
			c.close();
		}
		try {
			cr.applyBatch(Provider.AUTHORITY, ops);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (OperationApplicationException e) {
			e.printStackTrace();
		}

		DataUpdateSyncLock.getInstance().setDataBeingSwapped(false);
		sendBroadcast(false);
		Log.d(Constants.TAG, "LoadNewDataTask: doInBackground: done");
		return Boolean.TRUE;
	}
		
	@Override
	protected void onPostExecute(Boolean result) {
		Log.d(Constants.TAG, "LoadNewDataTask: onPostExecute");
		super.onPostExecute(result);
		LoadNewDataTask.context.getContentResolver().notifyChange(Uris.Entries.CONTENT_URI_ENTRIES_DISTINCT, null);
		LoadNewDataTask.context.getContentResolver().notifyChange(Uris.Entries.CONTENT_URI_ENTRIES, null);
	}

	private void sendBroadcast(boolean isInProgress) {
		Intent intent = new Intent(IN_PROGRESS);
		intent.putExtra(IN_PROGRESS, isInProgress);
		LocalBroadcastManager.getInstance(LoadNewDataTask.context).sendBroadcast(intent);
	}
}
