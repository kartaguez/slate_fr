package com.deadrooster.slate.android.services;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.SparseArray;

import com.deadrooster.slate.android.http.RSSFileFetcher;
import com.deadrooster.slate.android.model.Entry;
import com.deadrooster.slate.android.model.Model.Entries;
import com.deadrooster.slate.android.parser.SlateRSSParser;
import com.deadrooster.slate.android.provider.Uris;
import com.deadrooster.slate.android.util.ParcelableBoolean;
import com.deadrooster.slate.android.util.RefreshCounter;

public class RefreshService extends Service {

	public static final String URL_RSS_UNE = "http://www.slate.fr/rss/android/une";
	public static final String URL_RSS_FRANCE = "http://www.slate.fr/rss/android/france";
	public static final String URL_RSS_MONDE = "http://www.slate.fr/rss/android/monde";
	public static final String URL_RSS_ECONOMIE = "http://www.slate.fr/rss/android/economie";
	public static final String URL_RSS_CULTURE = "http://www.slate.fr/rss/android/culture";
	public static final String URL_RSS_LIFE = "http://www.slate.fr/rss/android/life";

	public static final String NOTIFICATION = "com.deadrooster.slate.android.notification";
	public static final String REFRESH_SUCCESSFUL = "refresh_successful";
	public static final String REFRESH_SUCCESSFUL_PER_CATEGORY = "refresh_successful_per_category";

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

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		// init refresh counter
		this.globalSuccess = false;
		if (this.counter != null) {
			this.counter.deactivate();
		}
		RefreshCounter counter = new RefreshCounter(6);
		this.counter = counter;

		// fetch rss file
		HttpTask task = null;
		for (int i = 0; i < 6; i++) {
			task = new HttpTask(i, counter);
			this.httpTasks.add(task);
			task.execute();
			counter.incrementRefresh();
		}
		return Service.START_NOT_STICKY;
	}

	private void notifyRefreshDone() {

		Intent i = new Intent(NOTIFICATION);
		Bundle bundle = new Bundle();
		bundle.putSparseParcelableArray(RefreshService.REFRESH_SUCCESSFUL_PER_CATEGORY, this.successes);
		bundle.putBoolean(RefreshService.REFRESH_SUCCESSFUL, this.globalSuccess);
		i.putExtras(bundle);
		sendBroadcast(i);

	}

	@Override
	public IBinder onBind(Intent intent) {
		return this.binder;
	}

	// private Binder
	public class SlateBinder extends Binder {

		public RefreshService getService() {
			return RefreshService.this;
		}
	}

	private void parseRssFile(String rssFileContent, int category, RefreshCounter counter) {
		// parse rss file
		ParseTask task = new ParseTask(category, counter);
		this.parseTasks.add(task);
		task.execute(rssFileContent);
	}

	private void updateEntriesDB(List<Entry> result, int category) {


		ContentResolver cr = getContentResolver();

		// empty table
		cr.delete(Uris.Entries.CONTENT_URI_ENTRIES, SELECTION_DELETE, new String[] {Integer.toString(category)});

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
			getContentResolver().insert(Uris.Entries.CONTENT_URI_ENTRIES, values);
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

			String rssFileContent = null;

			String url = null;
			switch (category) {
			case 0:
				url = URL_RSS_UNE;
				break;
			case 1:
				url = URL_RSS_FRANCE;
				break;
			case 2:
				url = URL_RSS_MONDE;
				break;
			case 3:
				url = URL_RSS_ECONOMIE;
				break;
			case 4:
				url = URL_RSS_CULTURE;
				break;
			case 5:
				url = URL_RSS_LIFE;
				break;
			default:
				break;
			}

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
			super.onPostExecute(result);

			if (result != null) {
				httpTasks.remove(this);
				parseRssFile(result, this.category, this.counter);
			} else {
				synchronized (this.counter) {
					this.counter.decrementRefresh();
					successes.put(this.category, new ParcelableBoolean(false));
					if (this.counter.isLast()){
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
			super.onPostExecute(result);
			parseTasks.remove(this);
			newEntries.put(this.category ,result);
			synchronized (this.counter) {
				this.counter.decrementRefresh();
				globalSuccess = true;
				successes.put(this.category, new ParcelableBoolean(true));
				if (this.counter.isLast()) {
					notifyRefreshDone();
					for (int i = 0; i < newEntries.size(); i++) {
						int key = newEntries.keyAt(i);
						updateEntriesDB(newEntries.get(key), key);
					}
					newEntries.clear();
				}
			}
		}

	}

}
