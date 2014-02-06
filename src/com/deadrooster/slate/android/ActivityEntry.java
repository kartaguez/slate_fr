package com.deadrooster.slate.android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.deadrooster.slate.android.adapters.util.LoadImageFromDb;
import com.deadrooster.slate.android.adapters.util.LoadImageFromInternet;
import com.deadrooster.slate.android.model.Model.Entries;
import com.deadrooster.slate.android.provider.Uris;
import com.deadrooster.slate.android.util.DefaultImage;

public class ActivityEntry extends Activity implements LoaderManager.LoaderCallbacks<Cursor> {

	public static final String[] PROJECTION = new String[] {Entries._ID, Entries.CATEGORY, Entries.TITLE, Entries.PREVIEW, Entries.DESCRIPTION, Entries.THUMBNAIL_URL, Entries.THUMBNAIL_DATA, Entries.PUBLICATION_DATE, Entries.AUTHOR};
	public static final String SELECTION = "((" + Entries._ID + " == ?))";

	private static final String MIME_TYPE = "text/html";
	private static final String ENCODING = "iso-8859-1";

	ScrollView scrollView;
	TextView titleView;
	TextView previewView;
	TextView publicationDateView;
	TextView authorView;
	ImageView thumbnailView;
	WebView webView;
	String[] entryIdArg = new String[1];
	Cursor c;

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// set content layout and view
		setContentView(R.layout.entry);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		this.scrollView = (ScrollView) findViewById(R.id.entry_scroll_id);
		this.titleView = (TextView) findViewById(R.id.entry_title_id);
		this.previewView = (TextView) findViewById(R.id.entry_preview_id);
		this.publicationDateView = (TextView) findViewById(R.id.entry_publication_date_id);
		this.authorView = (TextView) findViewById(R.id.entry_author_id);
		this.thumbnailView = (ImageView) findViewById(R.id.entry_thumbnail_id);
		this.webView = (WebView) findViewById(R.id.entry_webview_id);

		// video parameter
		this.webView.getSettings().setJavaScriptEnabled(true);
		this.webView.setPadding(0, 0, 0, 0);

		// retrieve entry is
		this.entryIdArg[0] = Long.toString(getIntent().getLongExtra(ActivityCategoryEntryList.EXTRA_ENTRY_ID, -1));

		// hide all views
		this.scrollView.setVisibility(View.GONE);

		// init loader
		initLoader();

	}

	@Override
	public boolean onNavigateUp() {
		finish();
		return true;
	}

	// loader methods
	private void initLoader() {
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(this, Uris.Entries.CONTENT_URI_ENTRIES, PROJECTION, SELECTION, this.entryIdArg, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
		this.c = c;
		this.c.moveToFirst();

		int categoryId = c.getInt(1);
		String title = ActivityCategoryEntryList.categories.get(categoryId)[1];
		this.getActionBar().setTitle(title);

		this.titleView.setText(c.getString(2));
		this.previewView.setText(c.getString(3));
		this.thumbnailView.setImageBitmap(DefaultImage.getInstance(this).getImage());

		this.publicationDateView.setText(c.getString(7));

		this.authorView.setText(c.getString(8));

		this.webView.loadDataWithBaseURL(null, c.getString(4), MIME_TYPE, ENCODING, null);
		loadImageViewData(c.getBlob(6), c.getString(5), this.thumbnailView, categoryId);
		this.scrollView.setVisibility(View.VISIBLE);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		this.c = null;
	}

	private void loadImageViewData(byte[] data, String url, ImageView viewThumbnail, int category) {

		if (data == null) {
			LoadImageFromInternet imageTask = new LoadImageFromInternet(this, null, category, -1, c.getLong(0), url, viewThumbnail);
			imageTask.download();
		} else {
			LoadImageFromDb loadImageTask = new LoadImageFromDb(null, category, -1, data, viewThumbnail);
			loadImageTask.execute();
		}
	}
}
