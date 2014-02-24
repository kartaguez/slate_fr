package com.deadrooster.slate.android.adapters;

import java.util.HashMap;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.deadrooster.slate.android.R;
import com.deadrooster.slate.android.adapters.util.ImageCacheById;
import com.deadrooster.slate.android.adapters.util.LoadImageFromDb;
import com.deadrooster.slate.android.adapters.util.LoadImageFromInternet;
import com.deadrooster.slate.android.adapters.util.ViewWrapper;
import com.deadrooster.slate.android.util.Constants;
import com.deadrooster.slate.android.util.DefaultImage;

public class EntryListAdapter extends SimpleCursorAdapter {

	private Context context = null;
	private int layout = 0;
	private Cursor c = null;
	private int[] to = null;

	public EntryListAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
		super(context, layout, c, from, to, flags);
		this.context = context;
		this.layout = layout;
		this.c = c;
		this.to = to;
	}

	@Override
	public View getView(int position, View convertView, android.view.ViewGroup parent) {
		
		View rowView = convertView;
		ViewWrapper wrapper = null;

		if (rowView == null) {
			LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			rowView = inflater.inflate(this.layout, null);
			wrapper = new ViewWrapper(rowView, this.to);
			rowView.setTag(wrapper);
		} else {
			wrapper = (ViewWrapper) rowView.getTag();
			if (wrapper.getImageTask() != null) {
				wrapper.getImageTask().cancel(false);
			}
			if (wrapper.getLoadImageTask() != null) {
				wrapper.getLoadImageTask().cancel(false);
			}
		}

		if (c != null) {
			c.moveToPosition(position);

			switch (this.layout) {
			case R.layout.r_row_entry_one_pane:
				TextView viewOnePaneTitle = (TextView) wrapper.getView(this.to[0]);
				TextView viewDescription = (TextView) wrapper.getView(this.to[1]);
				ImageView viewThumbnail = (ImageView) wrapper.getView(this.to[2]);
				viewOnePaneTitle.setText(this.c.getString(2));
				int category = this.c.getInt(1);
				viewDescription.setText(this.c.getString(3));
				loadImageViewData(position, wrapper, viewThumbnail, category);
				break;
			case R.layout.r_row_entry_two_pane:
				TextView viewTwoPaneTitle = (TextView) wrapper.getView(this.to[0]);
				viewTwoPaneTitle.setText(this.c.getString(2));
				break;
			default:
				break;
			}

		}

		return rowView;
	}

	private void loadImageViewData(int position, ViewWrapper wrapper, ImageView viewThumbnail, int category) {
		Bitmap thumbnail = null;
		long entryId = this.c.getLong(0);
		Log.d(Constants.TAG, "EntryListAdapter: loadImageViewData: " + entryId + ", " + category + ", " + position);
		HashMap<Long, Bitmap> categoryImages = ImageCacheById.getInstance().getImages().get(category);
		if (categoryImages != null) {
			thumbnail = categoryImages.get(entryId);
		}
		Log.d(Constants.TAG, "EntryListAdapter: loadImageViewData: categoryImages null: " + (categoryImages == null));
		if (thumbnail == null) {
			viewThumbnail.setImageBitmap(DefaultImage.getInstance(this.context).getImage());
			byte[] thumbnailData = this.c.getBlob(4);
			if (thumbnailData == null) {
				Log.d(Constants.TAG, "EntryListAdapter: loadImageViewData: no data from cache or DB available: load from the Internet: " + category + ", " + position);
				LoadImageFromInternet imageTask = new LoadImageFromInternet(this.context, category, entryId, c.getString(5), viewThumbnail);
				wrapper.setImageTask(imageTask);
				imageTask.download();
			} else {
				Log.d(Constants.TAG, "EntryListAdapter: loadImageViewData: load from DB: " + category + ", " + position);
				LoadImageFromDb loadImageTask = new LoadImageFromDb(category, entryId, thumbnailData, viewThumbnail);
				wrapper.setLoadImageTask(loadImageTask);
				loadImageTask.execute();
			}
		} else {
			Log.d(Constants.TAG, "EntryListAdapter: loadImageViewData: cache available: " + category + ", " + position);
			viewThumbnail.setImageBitmap(thumbnail);
		}

	}

	@Override
	public Cursor swapCursor(Cursor c) {
		this.c = c;
		return super.swapCursor(c);
	}

}
