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
import com.deadrooster.slate.android.adapters.util.ScrolledFragment;
import com.deadrooster.slate.android.model.Model.Entries;
import com.deadrooster.slate.android.provider.Uris;
import com.deadrooster.slate.android.util.Constants;
import com.deadrooster.slate.android.util.DefaultImage;

/**
 * A fragment representing a single Entry detail screen. This fragment is either
 * contained in a {@link EntryListActivity} in two-pane mode (on tablets) or a
 * {@link EntryDetailActivity} on handsets.
 */
public class EntryDetailPagerFragment extends Fragment implements ScrolledFragment, LoaderManager.LoaderCallbacks<Cursor> {
	/**
	 * The fragment argument representing the item ID that this fragment
	 * represents.
	 */
	public static final String ARG_ITEM_ID = "item_id";
	public static final String ARG_ITEM_POSITION = "item_position";
	public static final String ARG_NUM = "num";

	public static final String[] PROJECTION = new String[] {Entries._ID, Entries.CATEGORY, Entries.TITLE, Entries.PREVIEW, Entries.DESCRIPTION, Entries.THUMBNAIL_URL, Entries.THUMBNAIL_DATA, Entries.PUBLICATION_DATE, Entries.AUTHOR};
	public static final String SELECTION = "((" + Entries._ID + " == ?))";

	private static HashMap<Long, Float> scrollXTable = new HashMap<Long, Float>();
	private static HashMap<Long, Float> scrollYTable = new HashMap<Long, Float>();

	private static final String MIME_TYPE = "text/html";
	private static final String ENCODING = "utf-8";

	private long entryId;
	private int num;
	private int position;
	private ScrollView scrollView;
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
	public EntryDetailPagerFragment() {
	}

	public static EntryDetailPagerFragment newInstance(long id, int num, int position) {
		Log.d(Constants.TAG, "EntryDetailPagerFragment: newInstance: position: " + position);
		EntryDetailPagerFragment fragment = new EntryDetailPagerFragment();
		Bundle args = new Bundle();
		args.putLong(ARG_ITEM_ID, id);
		args.putInt(ARG_NUM, num);
		args.putInt(ARG_ITEM_POSITION, position);
		fragment.setArguments(args);

		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// init loader
		initLoader();

		if (getArguments() != null) {
			if (getArguments().containsKey(ARG_ITEM_ID)) {
				this.entryId = getArguments().getLong(ARG_ITEM_ID);
			} else {
				this.entryId = 0;
			}
			if (getArguments().containsKey(ARG_NUM)) {
				this.num = getArguments().getInt(ARG_NUM);
			} else {
				this.num = 0;
			}
			if (getArguments().containsKey(ARG_ITEM_POSITION)) {
				this.position = getArguments().getInt(ARG_ITEM_POSITION);
			} else {
				this.position = 0;
			}
		}
		Log.d(Constants.TAG, "EntryDetailPagerFragment: onCreate: entryId: " + this.entryId + ", num: " + this.num + ", position: " + this.position);

	}

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		Log.d(Constants.TAG, "EntryDetailPagerFragment: onCreateView");

		// set content layout and view
		View rootView = inflater.inflate(R.layout.r_fragment_entry_detail, container, false);
		this.scrollView = (ScrollView) rootView.findViewById(R.id.entry_scroll_id);
		this.vto = this.scrollView.getViewTreeObserver();
		this.vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {

				float curScrollX = -1;
				if (scrollXTable.get(entryId) != null) {
					curScrollX = scrollXTable.get(entryId).floatValue();
				}
				float curScrollY = -1;
				if (scrollYTable.get(entryId) != null) {
					Log.d(Constants.TAG, "EntryDetailPagerFragment: set scrollY: entryId: " + entryId + ", scrollY: " + scrollYTable.get(entryId).intValue());
					curScrollY = scrollYTable.get(entryId).floatValue();
				}
				if (curScrollX > 0 || curScrollY > 0) {
					Log.d(Constants.TAG, "EntryDetailPagerFragment: onCreateView: scroll: position: " + position + ", scrollY: " + curScrollY);
					scrollView.scrollTo(Math.round(curScrollX * getScrollViewHeight()), Math.round(curScrollY * getScrollViewHeight()));
				}
				scrollView.setVisibility(View.VISIBLE);
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
		if (this.isVisible()) {
			Log.d(Constants.TAG, "EntryDetailPagerFragment: onSaveInstanceState: save scrollY: entryId: " + entryId + ", scrollY: " + this.scrollView.getScrollY());
			super.onSaveInstanceState(outState);
			scrollXTable.put(this.entryId, getRelativeScrollX());
			scrollYTable.put(this.entryId, getRelativeScrollY());
		}
	}

	public void updateContent(long id) {
		this.entryId = id;
		this.entryIdArg[0] = Long.toString(this.entryId);
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

	// util
	public static void resetScrollPositions() {
		scrollXTable.clear();
		scrollYTable.clear();
	}

	@Override
	public void setScrollX(float scrollX) {
		scrollXTable.put(this.entryId, Float.valueOf(scrollX));
	}

	@Override
	public void setScrollY(float scrollY) {
		scrollYTable.put(this.entryId, Float.valueOf(scrollY));
	}

	public void resetScrollX() {
		scrollXTable.remove(entryId);
		this.scrollView.setScrollX(0);
	}

	public void resetScrollY() {
		scrollYTable.remove(entryId);
		this.scrollView.setScrollY(0);
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
