package com.deadrooster.slate.android.adapters.util;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import com.deadrooster.slate.android.util.Animations;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.SparseArray;
import android.widget.ImageView;

public class LoadImageFromDb extends AsyncTask<String, Integer, Bitmap> {

	private int category;
	private long entryId;
	private byte[] thumbnailData;
	private final WeakReference<ImageView> imageViewReference;
	private Bitmap bitmap;

	public LoadImageFromDb(int category, long entryId, byte[] thumbnailData, ImageView viewThumbnail) {
		this.category = category;
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
			imageView.startAnimation(Animations.fadeInAnim);
			SparseArray<HashMap<Long, Bitmap>> imageCacheById = ImageCacheById.getInstance().getImages();
			if (imageCacheById != null) {
				if (imageCacheById.get(category) == null) {
					imageCacheById.put(category, new HashMap<Long, Bitmap>());
				}
				imageCacheById.get(category).put(entryId, bitmap);
			}
		}
	}

}
