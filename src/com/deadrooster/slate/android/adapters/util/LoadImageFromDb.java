package com.deadrooster.slate.android.adapters.util;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.SparseArray;
import android.widget.ImageView;

public class LoadImageFromDb extends AsyncTask<String, Integer, Bitmap> {

	private SparseArray<SparseArray<Bitmap>> imagesByPosition;
	private SparseArray<HashMap<Long, Bitmap>> imagesById;
	private int category;
	private int position;
	private long entryId;
	private byte[] thumbnailData;
	private final WeakReference<ImageView> imageViewReference;
	private Bitmap bitmap;

	public LoadImageFromDb(SparseArray<SparseArray<Bitmap>> imagesByPosition, SparseArray<HashMap<Long, Bitmap>> imagesById, int category, int position, long entryId, byte[] thumbnailData, ImageView viewThumbnail) {
		this.imagesByPosition = imagesByPosition;
		this.imagesById = imagesById;
		this.category = category;
		this.position = position;
		this.entryId = entryId;
		this.thumbnailData = thumbnailData;
		this.imageViewReference = new WeakReference<ImageView>(viewThumbnail);
	}

	@Override
	protected Bitmap doInBackground(String... params) {

		this.bitmap = BitmapFactory.decodeByteArray(this.thumbnailData, 0, this.thumbnailData.length);

		return this.bitmap;
	}

	@Override
	protected void onPostExecute(Bitmap bitmap) {
		super.onPostExecute(bitmap);
		ImageView imageView = this.imageViewReference.get();
		if (imageView != null) {
			imageView.setImageBitmap(this.bitmap);
			if (this.imagesByPosition != null) {
				if (this.imagesByPosition.get(category) == null) {
					this.imagesByPosition.put(category, new SparseArray<Bitmap>());
				}
				this.imagesByPosition.get(category).put(position, bitmap);
			}
			if (this.imagesById != null) {
				if (this.imagesById.get(category) == null) {
					this.imagesById.put(category, new HashMap<Long, Bitmap>());
				}
				this.imagesById.get(category).put(entryId, bitmap);
			}
		}
	}

}
