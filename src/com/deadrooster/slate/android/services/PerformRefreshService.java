package com.deadrooster.slate.android.services;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import android.app.Service;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;

import com.deadrooster.slate.android.adapters.util.Categories;
import com.deadrooster.slate.android.adapters.util.ImageCacheById;
import com.deadrooster.slate.android.http.RSSFileFetcher;
import com.deadrooster.slate.android.model.Entry;
import com.deadrooster.slate.android.model.Model.Entries;
import com.deadrooster.slate.android.parser.SlateRSSParser;
import com.deadrooster.slate.android.preferences.Preferences;
import com.deadrooster.slate.android.provider.Provider;
import com.deadrooster.slate.android.provider.Uris;
import com.deadrooster.slate.android.util.Constants;
import com.deadrooster.slate.android.util.ParcelableBoolean;
import com.deadrooster.slate.android.util.RefreshCounter;
import com.deadrooster.slate.android.util.SlateCalendar;

public class PerformRefreshService extends Service {

	public static final String URL_RSS_UNE = "http://www.slate.fr/rss/android/une";
	public static final String URL_RSS_FRANCE = "http://www.slate.fr/rss/android/france";
	public static final String URL_RSS_MONDE = "http://www.slate.fr/rss/android/monde";
	public static final String URL_RSS_ECONOMIE = "http://www.slate.fr/rss/android/economie";
	public static final String URL_RSS_CULTURE = "http://www.slate.fr/rss/android/culture";
	public static final String URL_RSS_LIFE = "http://www.slate.fr/rss/android/life";

	public static final String NOTIFICATION = "com.deadrooster.slate.android.refresh.completed";
	public static final String IS_GLOBAL_SUCCESS = "is_global_success";

	public static final String[] PROJECTION = new String[] {Entries._ID, Entries.CATEGORY, Entries.TITLE, Entries.PREVIEW, Entries.THUMBNAIL_DATA, Entries.THUMBNAIL_URL};
	public static final String SELECTION = "((" + Entries.TITLE + " != '') and (" + Entries.CATEGORY + " = ?))";
	public static final String SELECTION_DELETE = "(" + Entries.CATEGORY + " = ?)";

	private final IBinder binder = new SlateBinder();

	private RefreshCounter counter = null;
	private ArrayList<HttpTask> httpTasks = new ArrayList<HttpTask>();
	private ArrayList<ParseTask> parseTasks = new ArrayList<ParseTask>();
	private SparseArray<List<Entry>> newEntries = new SparseArray<List<Entry>>();
	private boolean globalSuccess = false;
	private SparseArray<ParcelableBoolean> successes = new SparseArray<ParcelableBoolean>();
	private boolean refreshInProgress = false;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		if (!this.refreshInProgress) {
			Log.d(Constants.TAG, "PerformRefreshService: refresh started");

			this.refreshInProgress = true;
			// init refresh counter
			this.globalSuccess = false;

			if (this.counter != null) {
				this.counter.deactivate();
			}

			int nbCategories = Categories.getInstance(this).getCategories().size();
			if (nbCategories == 0) {
				notifyRefreshDone();
			} else {
				RefreshCounter counter = new RefreshCounter(nbCategories);
				this.counter = counter;
		
				// fetch rss file
				HttpTask task = null;
				
				for (int i = 0; i < nbCategories; i++) {
					task = new HttpTask(i, counter);
					this.httpTasks.add(task);
					task.execute();
					counter.incrementRefresh();
				}
			}
		} else {
			Log.d(Constants.TAG, "PerformRefreshService: refresh not started (already in progress)");
		}
		return Service.START_NOT_STICKY;
	}

	public boolean isRefreshInProgress() {
		return this.refreshInProgress;
	}

	private void notifyRefreshDone() {

		if (this.globalSuccess) {
			saveLastRefreshDate();
		}

		this.refreshInProgress = false;

    	if (this.successes != null) {
	    	for (int i = 0; i < this.successes.size(); i++) {
	    		int curCategory = this.successes.keyAt(i);
	    		if (this.successes.get(curCategory).getBool()) {
	    			ImageCacheById.getInstance().clear(curCategory);
	    		}
	    	}
    	}

		Log.d(Constants.TAG, "PerformRefreshService: notifyRefreshDone");
		Intent i = new Intent(NOTIFICATION);
		i.putExtra(PerformRefreshService.IS_GLOBAL_SUCCESS, this.globalSuccess);
		sendBroadcast(i);

	}

	private void saveLastRefreshDate() {
		Log.d(Constants.TAG, "PerformRefreshService: saveLastRefreshDate");

		long lastRefreshSuccessDate = new SlateCalendar().getTimeInMillis();
		SharedPreferences settings = getSharedPreferences(Preferences.PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
	    editor.putLong(Preferences.PREF_KEY_LAST_REFRESH_DATE, lastRefreshSuccessDate);
	    editor.commit();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return this.binder;
	}

	// private Binder
	public class SlateBinder extends Binder {

		public PerformRefreshService getService() {
			return PerformRefreshService.this;
		}
	}

	private void parseRssFile(String rssFileContent, int category, RefreshCounter counter) {
		Log.d(Constants.TAG, "PerformRefreshService: parseRssFile");
		// parse rss file
		ParseTask task = new ParseTask(category, counter);
		this.parseTasks.add(task);
		task.execute(rssFileContent);
	}

	private void updateEntriesDB(ArrayList<ContentProviderOperation> ops, List<Entry> result, int category) {
		Log.d(Constants.TAG, "PerformRefreshService: updateEntriesDB");

		// empty table
		ops.add(ContentProviderOperation
				.newDelete(Uris.Entries.CONTENT_URI_ENTRIES_TEMP)
				.withSelection(SELECTION_DELETE, new String[] {Integer.toString(category)})
				.build());

		// generate content value
		for (Entry entry : result) {
			ContentValues values = new ContentValues();
			values.put(Entries.TITLE, entry.getTitle());
			values.put(Entries.CATEGORY, category);
			values.put(Entries.DESCRIPTION, entry.getContent());
			values.put(Entries.PREVIEW, entry.getPreview());
			values.put(Entries.THUMBNAIL_URL, entry.getThumbnailUrl());
			values.put(Entries.PUBLICATION_DATE, entry.getPublicationDate());
			values.put(Entries.AUTHOR, entry.getAuthor());
			ops.add(ContentProviderOperation
				.newInsert(Uris.Entries.CONTENT_URI_ENTRIES_TEMP)
				.withValues(values)
				.build());
		}

	}

	// AsyncTask for retrieving rss file.
	private class HttpTask extends AsyncTask<Void, Integer, String> {

		private int category;
		private RefreshCounter counter;

		public HttpTask(int category, RefreshCounter counter) {
			super();
			this.category = category;
			this.counter = counter;
		}

		@Override
		protected String doInBackground(Void... params) {
			Log.d(Constants.TAG, "PerformRefreshService: HttpTask.doInBackground");

			String rssFileContent = null;
			String url = Categories.getInstance(PerformRefreshService.this).getCategories().get(category)[2];

			if (url != null) {
				RSSFileFetcher fetcher = RSSFileFetcher.getInstance();
				try {
					rssFileContent = fetcher.fetch(url);
				} catch (MalformedURLException e1) {
					e1.printStackTrace();
				}
			}

			return rssFileContent;
		}

		@Override
		protected void onPostExecute(String result) {
			Log.d(Constants.TAG, "PerformRefreshService: HttpTask.onPostExecute");
			super.onPostExecute(result);

			if (result != null) {
				httpTasks.remove(this);
				parseRssFile(result, this.category, this.counter);
			} else {
				synchronized (this.counter) {
					this.counter.decrementRefresh();
					successes.put(this.category, new ParcelableBoolean(false));
					if (this.counter.isLast() && !globalSuccess){
						notifyRefreshDone();
					}
				}
			}
		}

	}

	// AsyncTask for parsing rss file.
	private class ParseTask extends AsyncTask<String, Integer, List<Entry>> {

		private int category;
		private RefreshCounter counter;

		public ParseTask(int category, RefreshCounter counter) {
			super();
			this.category = category;
			this.counter = counter;
		}

		@Override
		protected List<Entry> doInBackground(String... rssFileContent) {
			Log.d(Constants.TAG, "ParseTask: HttpTask.doInBackground");
			List<Entry> entries = null;

			try {
				SlateRSSParser parser = new SlateRSSParser();
				entries = parser.parse(rssFileContent[0], this.category);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return entries;

		}

		@Override
		protected void onPostExecute(List<Entry> result) {
			Log.d(Constants.TAG, "ParseTask: HttpTask.onPostExecute");
			super.onPostExecute(result);
			parseTasks.remove(this);
			if (result != null) {
				globalSuccess = true;
				successes.put(this.category, new ParcelableBoolean(true));
				newEntries.put(this.category, result);
			} else {
				successes.put(this.category, new ParcelableBoolean(false));
			}
				
			synchronized (this.counter) {
				this.counter.decrementRefresh();
				if (this.counter.isLast()) {
					UpdateDBEntriesTask updateDBEntriesTask = new UpdateDBEntriesTask();
					updateDBEntriesTask.execute();
				}
			}
		}

	}

	// AsyncTask for updating data.
	private class UpdateDBEntriesTask extends AsyncTask<String, Integer, Boolean> {

		@Override
		protected Boolean doInBackground(String... rssFileContent) {
			Log.d(Constants.TAG, "UpdateDBEntriesTask: doInBackground");

			ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
			for (int i = 0; i < newEntries.size(); i++) {
				int key = newEntries.keyAt(i);
				if (successes.get(key).getBool()) {
					updateEntriesDB(ops, newEntries.get(key), key);
				}
			}
			try {
				getContentResolver().applyBatch(Provider.AUTHORITY, ops);
			} catch (RemoteException e) {
				e.printStackTrace();
			} catch (OperationApplicationException e) {
				e.printStackTrace();
			}
			newEntries.clear();

			return Boolean.TRUE;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			Log.d(Constants.TAG, "UpdateDBEntriesTask: onPostExecute");
			notifyRefreshDone();
		}

	}

}
