package com.deadrooster.slate.android.adapters.util;

import java.lang.ref.WeakReference;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.SparseArray;
import android.widget.ImageView;

public class LoadImageFromDb extends AsyncTask<String, Integer, Bitmap> {

	private SparseArray<SparseArray<Bitmap>> images;
	private int category;
	private int position;
	private byte[] thumbnailData;
	private final WeakReference<ImageView> imageViewReference;
	private Bitmap bitmap;

	public LoadImageFromDb(SparseArray<SparseArray<Bitmap>> images, int category, int position, byte[] thumbnailData, ImageView viewThumbnail) {
		this.images = images;
		this.category = category;
		this.position = position;
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
			if (this.images != null) {
				if (this.images.get(category) == null) {
					this.images.put(category, new SparseArray<Bitmap>());
				}
				this.images.get(category).put(position, bitmap);
			}
		}
	}

}
