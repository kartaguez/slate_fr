package com.deadrooster.slate.android.fragments;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.deadrooster.slate.android.R;
import com.deadrooster.slate.android.activities.EntryListActivity;
import com.deadrooster.slate.android.adapters.EntryListAdapter;
import com.deadrooster.slate.android.adapters.util.LoadNewDataTask;
import com.deadrooster.slate.android.model.Model.Entries;
import com.deadrooster.slate.android.provider.Uris;
import com.deadrooster.slate.android.util.Callbacks;
import com.deadrooster.slate.android.util.Constants;

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
		public void onItemSelected(long id, int position) {
		}
	};

	private Callbacks callbacks = null;
	private int activatedPosition = ListView.INVALID_POSITION;
	private int category = 0;
	private EntryListAdapter adapter = null;
	private CursorLoader cursorLoader = null;
	private boolean twoPane = false;
	private boolean isActivable = true;
	private long[] entryIds;

	private DataSwappingIsOverReceiver dataSwappingIsOverReceiver = new DataSwappingIsOverReceiver();
	private boolean isWaitingForSwappingEnd = false;

	public EntryListFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(Constants.TAG, "EntryListFragment: onCreate");
		super.onCreate(savedInstanceState);

		if (this.callbacks == null) {
			this.callbacks = dummyCallbacks;
		}

		// set up the the action bar to show a dropdown list.
		this.category = 0;
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(EntryListActivity.CURRENT_CATEGORY)) {
				this.category = savedInstanceState.getInt(EntryListActivity.CURRENT_CATEGORY);
			}
		}

	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		Log.d(Constants.TAG, "EntryListFragment: onActivityCreated");
		super.onActivityCreated(savedInstanceState);
		// init adapter
		initAdapter();

		// init loader
		initLoader();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(Constants.TAG, "EntryListFragment: onCreateView");
		View rootView = inflater.inflate(R.layout.r_fragment_entry_list, container, false);
		return rootView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		Log.d(Constants.TAG, "EntryListFragment: onViewCreated");
		super.onViewCreated(view, savedInstanceState);

		getListView().setEmptyView(view.findViewById(R.id.empty));
		this.setActivateOnItemClick(true);

		if (getArguments().containsKey(ARG_TWO_PANE)) {
			this.twoPane = getArguments().getBoolean(ARG_TWO_PANE);
		}

		// Restore the previously serialized activated item position.
		if (this.twoPane) {
			if (savedInstanceState != null && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
				setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
			} else {
				setActivatedPosition(0);
			}
		}

	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		Log.d(Constants.TAG, "EntryListFragment: onSaveInstanceState");
		super.onSaveInstanceState(outState);
		if (activatedPosition != ListView.INVALID_POSITION) {
			outState.putInt(STATE_ACTIVATED_POSITION, activatedPosition);
		}
		outState.putInt(EntryListActivity.CURRENT_CATEGORY, this.category);
	}

	@Override
	public void onAttach(Activity activity) {
		Log.d(Constants.TAG, "EntryListFragment: onAttach");
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
		Log.d(Constants.TAG, "EntryListFragment: onDetach");
		super.onDetach();
		this.callbacks = dummyCallbacks;
	}

	@Override
	public void onResume() {
		Log.d(Constants.TAG, "EntryListFragment: onResume");
		super.onResume();
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(this.dataSwappingIsOverReceiver, new IntentFilter(LoadNewDataTask.IN_PROGRESS));
	}

	@Override
	public void onPause() {
		Log.d(Constants.TAG, "EntryListFragment: onPause");
		super.onPause();
		this.isWaitingForSwappingEnd = false;
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(this.dataSwappingIsOverReceiver);
	}

	// init
	private void initAdapter() {
		Log.d(Constants.TAG, "EntryListFragment: initAdapter");
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
		Log.d(Constants.TAG, "EntryListFragment: initLoader");
		getLoaderManager().initLoader(0, null, this);
	}

	public void switchCategory(int category) {
		Log.d(Constants.TAG, "EntryListFragment: switchCategory to " + category);
		this.isActivable = false;
		this.category = category;
		this.activatedPosition = 0;
		getListView().setSelection(0);
		Log.d(Constants.TAG, "EntryListFragment: restartLoader");
		getLoaderManager().restartLoader(0, null, this);
	}

	@Override
	public void onListItemClick(ListView listView, View view, int position, long id) {
		Log.d(Constants.TAG, "EntryListFragment: onListItemClick: position: " + position + ", id: " + id);
		super.onListItemClick(listView, view, position, id);
		setActivatedPosition(position);
		this.callbacks.onItemSelected(getListView().getItemIdAtPosition(this.activatedPosition), this.activatedPosition);
	}

	private void propagateItemClick() {
		this.callbacks.onItemSelected(getListView().getItemIdAtPosition(this.activatedPosition), this.activatedPosition);
	}

	private void selectActivatedPosition() {
		if (isActivable) {
			setActivatedPosition(this.activatedPosition);
			callbackOnItemSelected(getListView().getItemIdAtPosition(this.activatedPosition), this.activatedPosition);
		}
	}

	private void callbackOnItemSelected(long id, int position) {
		callbacks.onItemSelected(id, position);
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
			getListView().setItemChecked(this.activatedPosition, true);
		} else {
			getListView().setItemChecked(position, true);
		}

		this.activatedPosition = position;
	}

	// loader methods
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Log.d(Constants.TAG, "EntryListFragment: onCreateLoader");
		this.cursorLoader = new CursorLoader(getActivity(), Uris.Entries.CONTENT_URI_ENTRIES, PROJECTION, SELECTION, new String[] {Integer.toString(this.category)}, null);
		return this.cursorLoader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
		Log.d(Constants.TAG, "EntryListFragment: onLoadFinished");
		loadEntryIds(c);
		this.adapter.swapCursor(c);
		this.isActivable = true;
		if (this.twoPane) {
			selectActivatedPosition();
		}
	}

	private void loadEntryIds(Cursor c) {
		int nbEntries = c.getCount();
		this.entryIds = new long[nbEntries];
		while (c.moveToNext()) {
			this.entryIds[c.getPosition()] = c.getLong(0);
		}
		c.moveToPosition(-1);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		Log.d(Constants.TAG, "EntryListFragment: onLoaderReset");
		this.adapter.swapCursor(null);
	}

	private class DataSwappingIsOverReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(Constants.TAG, "EntryListActivity: RefreshCompletedReceiver received");
			if (isWaitingForSwappingEnd) {
				isWaitingForSwappingEnd = false;
				propagateItemClick();
			}
		}
		
	}

	// getters
	public long[] getEntryIds() {
		return entryIds;
	}

}
