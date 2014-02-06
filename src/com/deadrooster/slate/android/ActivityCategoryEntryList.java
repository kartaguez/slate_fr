package com.deadrooster.slate.android;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import android.app.ActionBar;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import com.deadrooster.slate.android.adapters.EntryListAdapter;
import com.deadrooster.slate.android.http.RSSFileFetcher;
import com.deadrooster.slate.android.model.Entry;
import com.deadrooster.slate.android.model.Model.Entries;
import com.deadrooster.slate.android.parser.SlateRSSParser;
import com.deadrooster.slate.android.provider.Uris;
import com.deadrooster.slate.android.util.RefreshCounter;

public class ActivityCategoryEntryList extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor>, ActionBar.OnNavigationListener {

	public static final String URL_RSS_UNE = "http://www.slate.fr/rss/android/une";
	public static final String URL_RSS_FRANCE = "http://www.slate.fr/rss/android/france";
	public static final String URL_RSS_MONDE = "http://www.slate.fr/rss/android/monde";
	public static final String URL_RSS_ECONOMIE = "http://www.slate.fr/rss/android/economie";
	public static final String URL_RSS_CULTURE = "http://www.slate.fr/rss/android/culture";
	public static final String URL_RSS_LIFE = "http://www.slate.fr/rss/android/life";

	public static final String[] PROJECTION = new String[] {Entries._ID, Entries.CATEGORY, Entries.TITLE, Entries.PREVIEW, Entries.THUMBNAIL_DATA, Entries.THUMBNAIL_URL};
	public static final String SELECTION = "((" + Entries.TITLE + " != '') and (" + Entries.CATEGORY + " = ?))";
	public static final String SELECTION_DELETE = "(" + Entries.CATEGORY + " = ?)";

	public static final String EXTRA_ENTRY_ID = "entry_id";

	public static SparseArray<String[]> categories;
	private int category = 0;
	private EntryListAdapter adapter = null;
	private RefreshCounter counter = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// set layout
		setContentView(R.layout.category_entry_list);
		getListView().setEmptyView(findViewById(R.id.empty));

		// set up the the action bar to show a dropdown list.
		initCategories();
		setUpActionBar();
		this.category = 0;

		// init adapter
		initAdapter();

		// init loader
		initLoader();

	}

	private void initCategories() {
		ActivityCategoryEntryList.categories.put(0, new String[] {"une", getString(R.string.section_une)});
		ActivityCategoryEntryList.categories.put(1, new String[] {"france", getString(R.string.section_france)});
		ActivityCategoryEntryList.categories.put(2, new String[] {"monde", getString(R.string.section_monde)});
		ActivityCategoryEntryList.categories.put(3, new String[] {"economie", getString(R.string.section_economie)});
		ActivityCategoryEntryList.categories.put(4, new String[] {"culture", getString(R.string.section_culture)});
		ActivityCategoryEntryList.categories.put(5, new String[] {"life", getString(R.string.section_life)});
	}

	private void setUpActionBar() {
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

		// Set up the dropdown list navigation in the action bar.
		actionBar.setListNavigationCallbacks(
		// Specify a SpinnerAdapter to populate the dropdown list.
				new ArrayAdapter<String>(actionBar.getThemedContext(),
						android.R.layout.simple_list_item_1,
						android.R.id.text1, new String[] {
								getString(R.string.section_une),
								getString(R.string.section_france),
								getString(R.string.section_monde),
								getString(R.string.section_economie),
								getString(R.string.section_culture),
								getString(R.string.section_life),
								}), this);
	}

	@Override
	public boolean onNavigationItemSelected(int position, long itemId) {

		int formerCategory = this.category;
		this.category = position;

		if (this.category != formerCategory) {
			switchCategory();
		}

		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case R.id.refresh:
			startRotateIcon(item);
			refreshData(item);
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void startRotateIcon(MenuItem item) {
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		ImageView iv = (ImageView) inflater.inflate(R.layout.action_icon_refresh, null);

		Animation rotation = AnimationUtils.loadAnimation(this, R.anim.rotation_clockwise);
		rotation.setRepeatCount(Animation.INFINITE);
		iv.startAnimation(rotation);

		item.setActionView(iv);
	}

	private void stopRotateIcon(MenuItem item) {
	    item.getActionView().clearAnimation();
	    item.setActionView(null);
	}

	// events
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Intent i = new Intent(this, ActivityEntry.class);
		i.putExtra(ActivityCategoryEntryList.EXTRA_ENTRY_ID, id);
		startActivityForResult(i, 0);
	}
	
	// load data
	private void initAdapter() {

		String[] fromColumns = new String[] {Entries.TITLE, Entries.PREVIEW, Entries.THUMBNAIL_DATA, Entries.THUMBNAIL_URL};
		int[] toViews = new int[] {R.id.entry_title, R.id.entry_preview, R.id.entry_thumbnail};

		this.adapter = new EntryListAdapter(this, R.layout.row_entry, null, fromColumns, toViews, 0);
		setListAdapter(this.adapter);
	}

	private void initLoader() {
		getLoaderManager().initLoader(0, null, this);
	}

	private void refreshData(MenuItem item) {

		// init refresh counter
		if (this.counter != null) {
			this.counter.deactivate();
		}
		RefreshCounter counter = new RefreshCounter(6);
		this.counter = counter;

		// fetch rss file
		HttpTask task = null;
		for (int i = 0; i < 6; i++) {
			task = new HttpTask(item, i, counter);
			task.execute();
			counter.incrementRefresh();
		}

	}

	private void switchCategory() {
		// load stored data
		getLoaderManager().restartLoader(0, null, ActivityCategoryEntryList.this);
	}

	private void parseRssFile(String rssFileContent, int category, MenuItem item, RefreshCounter counter) {
		// parse rss file
		ParseTask task = new ParseTask(category, item, counter);
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

		private MenuItem item;
		private int category;
		private RefreshCounter counter;

		public HttpTask(MenuItem item, int category, RefreshCounter counter) {
			super();
			this.item = item;
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
				if (adapter != null) {
					adapter.clearCache(this.category);
				}
				parseRssFile(result, this.category, this.item, this.counter);
			} else {
				synchronized (this.counter) {
					this.counter.decrementRefresh();
					if (this.counter.isLast()){
						stopRotateIcon(this.item);
					}
				}
			}
		}

	}

	// AsyncTask for parsing rss file.
	private class ParseTask extends AsyncTask<String, Integer, List<Entry>> {

		private MenuItem item;
		private int category;
		private RefreshCounter counter;

		public ParseTask(int category, MenuItem item, RefreshCounter counter) {
			super();
			this.item = item;
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
			updateEntriesDB(result, this.category);
			synchronized (this.counter) {
				this.counter.decrementRefresh();
				if (this.counter.isLast()){
					stopRotateIcon(this.item);
				}
			}
		}

	}

	// loader methods
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(this, Uris.Entries.CONTENT_URI_ENTRIES, PROJECTION, SELECTION, new String[] {Integer.toString(this.category)}, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
		this.adapter.swapCursor(c);
		
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		this.adapter.swapCursor(null);
	}

	static {
		ActivityCategoryEntryList.categories = new SparseArray<String[]>();
	}

}
