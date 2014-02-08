package com.deadrooster.slate.android.fragments;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.deadrooster.slate.android.R;
import com.deadrooster.slate.android.adapters.EntryListAdapter;
import com.deadrooster.slate.android.model.Model.Entries;
import com.deadrooster.slate.android.provider.Uris;
import com.deadrooster.slate.android.util.Callbacks;

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

	private Callbacks callbacks = null;
	private int activatedPosition = ListView.INVALID_POSITION;
	private int category = 0;
	private EntryListAdapter adapter = null;
	private boolean twoPane = false;
	private boolean isActivable = true;

	public EntryListFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

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
			setActivatedPosition(this.activatedPosition);
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
			getListView().setItemChecked(activatedPosition, true);
		} else {
			getListView().setItemChecked(position, true);
		}

		activatedPosition = position;
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
