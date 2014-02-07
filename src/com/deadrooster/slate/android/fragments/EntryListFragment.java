package com.deadrooster.slate.android.fragments;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.deadrooster.slate.android.EntryListActivity;
import com.deadrooster.slate.android.R;
import com.deadrooster.slate.android.adapters.EntryListAdapter;
import com.deadrooster.slate.android.http.RSSFileFetcher;
import com.deadrooster.slate.android.model.Entry;
import com.deadrooster.slate.android.model.Model.Entries;
import com.deadrooster.slate.android.parser.SlateRSSParser;
import com.deadrooster.slate.android.provider.Uris;
import com.deadrooster.slate.android.util.Callbacks;
import com.deadrooster.slate.android.util.RefreshCounter;

public class EntryListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

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
	public static final String ARG_TWO_PANE = "two_pane";

	private static final String STATE_ACTIVATED_POSITION = "activated_position";

	private static Callbacks dummyCallbacks = new Callbacks() {
		@Override
		public void onItemSelected(long id) {
		}
	};

	private Context context = null;
	private Callbacks callbacks = null;
	private int activatedPosition = ListView.INVALID_POSITION;
	private int category = 0;
	private EntryListAdapter adapter = null;
	private RefreshCounter counter = null;
	private boolean twoPane = false;
	private boolean isActivable = true;

	private ArrayList<HttpTask> httpTasks = new ArrayList<HttpTask>();
	private ArrayList<ParseTask> parseTasks = new ArrayList<ParseTask>();
	private SparseArray<List<Entry>> newEntries = new SparseArray<List<Entry>>();

	public EntryListFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.context = getActivity();
		if (this.callbacks == null) {
			this.callbacks = dummyCallbacks;
		}

		// set up the the action bar to show a dropdown list.
		this.category = 0;

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.r_fragment_entry_list, container, false);
		return rootView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		getListView().setEmptyView(view.findViewById(R.id.empty));
		this.setActivateOnItemClick(true);

		if (getArguments().containsKey(ARG_TWO_PANE)) {
			this.twoPane = getArguments().getBoolean(ARG_TWO_PANE);
		}

		// init adapter
		initAdapter();

		// init loader
		initLoader();

		// Restore the previously serialized activated item position.
		if (this.twoPane) {
			if (savedInstanceState != null && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
				setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
			} else {
				setActivatedPosition(0);
			}
		}

	}

	// init
	private void initAdapter() {

		String[] fromColumns = new String[] {Entries.TITLE, Entries.PREVIEW, Entries.THUMBNAIL_DATA, Entries.THUMBNAIL_URL};
		int layoutId = R.layout.r_row_entry_one_pane;
		int[] toViews = new int[] {R.id.entry_title, R.id.entry_preview, R.id.entry_thumbnail};

		if (this.twoPane) {
			layoutId = R.layout.r_row_entry_two_pane;
			toViews = new int[] {R.id.entry_title};
		}

		this.adapter = new EntryListAdapter(getActivity(), layoutId, null, fromColumns, toViews, 0);
		setListAdapter(this.adapter);
	}

	private void initLoader() {
		getLoaderManager().initLoader(0, null, this);
	}

	public void switchCategory(int category) {
		this.isActivable = false;
		this.category = category;
		this.activatedPosition = 0;
		getListView().setSelection(0);
		getLoaderManager().restartLoader(0, null, this);
	}

	public void refreshData() {

		// init refresh counter
		if (this.counter != null) {
			this.counter.deactivate();
		}
		RefreshCounter counter = new RefreshCounter(6);
		this.counter = counter;

		// fetch rss file
		HttpTask task = null;
		for (int i = 0; i < EntryListActivity.categories.size(); i++) {
			task = new HttpTask(i, counter);
			this.httpTasks.add(task);
			task.execute();
			counter.incrementRefresh();
		}

	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// Activities containing this fragment must implement its callbacks.
		if (!(activity instanceof Callbacks)) {
			throw new IllegalStateException(
					"Activity must implement fragment's callbacks.");
		}

		callbacks = (Callbacks) activity;
	}

	@Override
	public void onDetach() {
		super.onDetach();
		for (HttpTask task : this.httpTasks) {
			task.cancel(true);
		}
		for (ParseTask task : this.parseTasks) {
			task.cancel(true);
		}
		this.callbacks = dummyCallbacks;
	}

	@Override
	public void onListItemClick(ListView listView, View view, int position, long id) {
		super.onListItemClick(listView, view, position, id);
		setActivatedPosition(position);
		this.callbacks.onItemSelected(id);
	}

	private void selectActivatedPosition() {
		if (isActivable) {
			callbackOnItemSelected(getListView().getItemIdAtPosition(this.activatedPosition));
		}
	}

	private void callbackOnItemSelected(long id) {
		callbacks.onItemSelected(id);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (activatedPosition != ListView.INVALID_POSITION) {
			outState.putInt(STATE_ACTIVATED_POSITION, activatedPosition);
		}
	}

	/**
	 * Turns on activate-on-click mode. When this mode is on, list items will be
	 * given the 'activated' state when touched.
	 */
	public void setActivateOnItemClick(boolean activateOnItemClick) {
		getListView().setChoiceMode(activateOnItemClick ? ListView.CHOICE_MODE_SINGLE : ListView.CHOICE_MODE_NONE);
	}

	private void setActivatedPosition(int position) {
		if (position == ListView.INVALID_POSITION) {
			getListView().setItemChecked(activatedPosition, false);
		} else {
			getListView().setItemChecked(position, true);
		}

		activatedPosition = position;
	}

	private void notifyRefreshDone(boolean isSuccess) {

		if (callbacks instanceof EntryListActivity) {
			((EntryListActivity) callbacks).finalizeRefreshBatch(isSuccess);
		}
	}

	private void parseRssFile(String rssFileContent, int category, RefreshCounter counter) {
		// parse rss file
		ParseTask task = new ParseTask(category, counter);
		this.parseTasks.add(task);
		task.execute(rssFileContent);
	}

	private void updateEntriesDB(List<Entry> result, int category) {

		if (this.context != null) {

			ContentResolver cr = this.context.getContentResolver();
	
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
				this.context.getContentResolver().insert(Uris.Entries.CONTENT_URI_ENTRIES, values);
			}
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
				if (adapter != null) {
					adapter.clearCache(this.category);
				}
				httpTasks.remove(this);
				parseRssFile(result, this.category, this.counter);
			} else {
				synchronized (this.counter) {
					this.counter.decrementRefresh();
					if (this.counter.isLast()){
						notifyRefreshDone(false);
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
				if (this.counter.isLast()){
					notifyRefreshDone(true);
					for (int i = 0; i < newEntries.size(); i++) {
						int key = newEntries.keyAt(i);
						updateEntriesDB(newEntries.get(key), key);
					}
					newEntries.clear();
				}
			}
		}

	}

	// loader methods
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(getActivity(), Uris.Entries.CONTENT_URI_ENTRIES, PROJECTION, SELECTION, new String[] {Integer.toString(this.category)}, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
		this.adapter.swapCursor(c);
		this.isActivable = true;
		if (this.twoPane) {
			selectActivatedPosition();
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		this.adapter.swapCursor(null);
	}

	public EntryListAdapter getAdapter() {
		return adapter;
	}

}
