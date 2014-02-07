package com.deadrooster.slate.android.fragments;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.deadrooster.slate.android.R;
import com.deadrooster.slate.android.EntryDetailActivity;
import com.deadrooster.slate.android.EntryListActivity;
import com.deadrooster.slate.android.adapters.util.LoadImageFromDb;
import com.deadrooster.slate.android.adapters.util.LoadImageFromInternet;
import com.deadrooster.slate.android.model.Model.Entries;
import com.deadrooster.slate.android.provider.Uris;
import com.deadrooster.slate.android.util.DefaultImage;

/**
 * A fragment representing a single Entry detail screen. This fragment is either
 * contained in a {@link EntryListActivity} in two-pane mode (on tablets) or a
 * {@link EntryDetailActivity} on handsets.
 */
public class EntryDetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
	/**
	 * The fragment argument representing the item ID that this fragment
	 * represents.
	 */
	public static final String ARG_ITEM_ID = "item_id";

	public static final String[] PROJECTION = new String[] {Entries._ID, Entries.CATEGORY, Entries.TITLE, Entries.PREVIEW, Entries.DESCRIPTION, Entries.THUMBNAIL_URL, Entries.THUMBNAIL_DATA, Entries.PUBLICATION_DATE, Entries.AUTHOR};
	public static final String SELECTION = "((" + Entries._ID + " == ?))";

	private static final String MIME_TYPE = "text/html";
	private static final String ENCODING = "iso-8859-1";

	private long entryId;
	ScrollView scrollView;
	TextView titleView;
	TextView previewView;
	TextView publicationDateView;
	TextView authorView;
	ImageView thumbnailView;
	WebView webView;
	String[] entryIdArg = new String[1];
	Cursor c;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public EntryDetailFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// init loader
		initLoader();

		if (getArguments().containsKey(ARG_ITEM_ID)) {
			this.entryId = getArguments().getLong(ARG_ITEM_ID);
		}
	}

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.r_fragment_entry_detail, container, false);

		// set content layout and view
		this.scrollView = (ScrollView) rootView.findViewById(R.id.entry_scroll_id);
		this.titleView = (TextView) rootView.findViewById(R.id.entry_title_id);
		this.previewView = (TextView) rootView.findViewById(R.id.entry_preview_id);
		this.publicationDateView = (TextView) rootView.findViewById(R.id.entry_publication_date_id);
		this.authorView = (TextView) rootView.findViewById(R.id.entry_author_id);
		this.thumbnailView = (ImageView) rootView.findViewById(R.id.entry_thumbnail_id);
		this.webView = (WebView) rootView.findViewById(R.id.entry_webview_id);

		// video parameter
		this.webView.getSettings().setJavaScriptEnabled(true);
		this.webView.setPadding(0, 0, 0, 0);

		// retrieve entry is
		this.entryIdArg[0] = Long.toString(this.entryId);

		// hide all views
		this.scrollView.setVisibility(View.GONE);

		return rootView;
	}

	public void updateContent(long id) {
		this.entryId = id;
		this.entryIdArg[0] = Long.toString(this.entryId);
		getLoaderManager().restartLoader(0, null, this);
		this.scrollView.setScrollY(0);
	}

	// loader methods
	private void initLoader() {
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(getActivity(), Uris.Entries.CONTENT_URI_ENTRIES, PROJECTION, SELECTION, this.entryIdArg, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
		this.c = c;
		if (this.c.moveToFirst()) {;

			int categoryId = c.getInt(1);
	
			this.titleView.setText(c.getString(2));
			this.previewView.setText(c.getString(3));
			this.thumbnailView.setImageBitmap(DefaultImage.getInstance(this.getActivity()).getImage());
	
			this.publicationDateView.setText(c.getString(7));
	
			this.authorView.setText(c.getString(8));
	
			this.webView.loadDataWithBaseURL(null, c.getString(4), MIME_TYPE, ENCODING, null);
			loadImageViewData(c.getBlob(6), c.getString(5), this.thumbnailView, categoryId);
			this.scrollView.setVisibility(View.VISIBLE);
		}
	}

	private void loadImageViewData(byte[] data, String url, ImageView viewThumbnail, int category) {

		if (data == null) {
			LoadImageFromInternet imageTask = new LoadImageFromInternet(getActivity(), null, category, -1, c.getLong(0), url, viewThumbnail);
			imageTask.download();
		} else {
			LoadImageFromDb loadImageTask = new LoadImageFromDb(null, category, -1, data, viewThumbnail);
			loadImageTask.execute();
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		this.c = null;
	}

}
