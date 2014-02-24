package com.deadrooster.slate.android.fragments;

import java.util.HashMap;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.deadrooster.slate.android.R;
import com.deadrooster.slate.android.activities.EntryDetailActivity;
import com.deadrooster.slate.android.activities.EntryListActivity;
import com.deadrooster.slate.android.adapters.util.ImageCacheById;
import com.deadrooster.slate.android.adapters.util.LoadImageFromDb;
import com.deadrooster.slate.android.adapters.util.LoadImageFromInternet;
import com.deadrooster.slate.android.model.Model.Entries;
import com.deadrooster.slate.android.provider.Uris;
import com.deadrooster.slate.android.util.Constants;
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

	private static final String STATE_SCROLL_X = "scroll_x";
	private static final String STATE_SCROLL_Y = "scroll_y";

	private static final String MIME_TYPE = "text/html";
	private static final String ENCODING = "utf-8";

	private long entryId = -1;
	private ScrollView scrollView;
	private float scrollX = -1;
	private float scrollY = -1;
	private TextView titleView;
	private TextView previewView;
	private TextView publicationDateView;
	private TextView authorView;
	private ImageView thumbnailView;
	private WebView webView;
	private String[] entryIdArg = new String[1];
	private Cursor c;

	private ViewTreeObserver vto;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public EntryDetailFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(Constants.TAG, "EntryDetailFragment - onCreate");

		// init loader
		initLoader();

		if (getArguments().containsKey(ARG_ITEM_ID)) {
			this.entryId = getArguments().getLong(ARG_ITEM_ID);
		}

		if (savedInstanceState != null) {
			if (savedInstanceState.getLong(ARG_ITEM_ID, -1) != -1) {
				this.entryId = savedInstanceState.getLong(ARG_ITEM_ID, -1);
			}
			if (savedInstanceState.getFloat(STATE_SCROLL_X, -1) != -1) {
				this.scrollX = savedInstanceState.getFloat(STATE_SCROLL_X);
			}
			if (savedInstanceState.getFloat(STATE_SCROLL_Y, -1) != -1) {
				this.scrollY = savedInstanceState.getFloat(STATE_SCROLL_Y);
			}
		}
	}

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		Log.d(Constants.TAG, "EntryDetailFragment - onCreateView");
		View rootView = inflater.inflate(R.layout.r_fragment_entry_detail, container, false);

		// set content layout and view
		this.scrollView = (ScrollView) rootView.findViewById(R.id.entry_scroll_id);
		this.vto = this.scrollView.getViewTreeObserver();
		this.vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				if (scrollX > -1 || scrollY > -1) {
					scrollView.scrollTo(Math.round(scrollX * getScrollViewHeight()), Math.round(scrollY * getScrollViewHeight()));
				}
				if (entryId > 0) {
					scrollView.setVisibility(View.VISIBLE);
				}
			}
		});
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

	@Override
	public void onSaveInstanceState(Bundle outState) {
		Log.d(Constants.TAG, "EntryDetailFragment - onSaveInstanceState");
		super.onSaveInstanceState(outState);
		outState.putLong(ARG_ITEM_ID, this.entryId);

		outState.putFloat(STATE_SCROLL_X, this.getRelativeScrollX());
		outState.putFloat(STATE_SCROLL_Y, this.getRelativeScrollY());
	}

	public void updateContent(long id) {
		if (id != this.entryId) {
			this.entryId = id;
			this.entryIdArg[0] = Long.toString(this.entryId);
			if (this.getRelativeScrollX() > 0 || this.getRelativeScrollY() > 0) {
				this.scrollX = 0;
				this.scrollY = 0;
			} else {
				this.scrollX = -1;
				this.scrollY = -1;
			}
		}
		getLoaderManager().restartLoader(0, null, this);
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

		}
	}

	private void loadImageViewData(byte[] data, String url, ImageView viewThumbnail, int category) {

		Bitmap thumbnail = null;
		HashMap<Long, Bitmap> categoryImages = ImageCacheById.getInstance().getImages().get(category);
		if (categoryImages != null) {
			thumbnail = categoryImages.get(this.entryId);
		}
		if (thumbnail == null) {
			if (data == null) {
				LoadImageFromInternet imageTask = new LoadImageFromInternet(getActivity(), category, this.entryId, url, viewThumbnail);
				imageTask.download();
			} else {
				LoadImageFromDb loadImageTask = new LoadImageFromDb(category, this.entryId, data, viewThumbnail);
				loadImageTask.execute();
			}
		} else {
			viewThumbnail.setImageBitmap(thumbnail);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		this.c = null;
	}

	private float getRelativeScrollX() {
		int absScrollY = this.scrollView.getScrollX();
		return (float) absScrollY / getScrollViewHeight();
	}

	private float getRelativeScrollY() {
		int absScrollY = this.scrollView.getScrollY();
		return (float) absScrollY / getScrollViewHeight();
	}

	private int getScrollViewHeight() {
		return this.scrollView.getChildAt(0).getHeight();
	}
}
