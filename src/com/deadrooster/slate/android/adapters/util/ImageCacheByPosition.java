package com.deadrooster.slate.android.adapters.util;

import android.graphics.Bitmap;
import android.util.SparseArray;

public class ImageCacheByPosition {

	public SparseArray<SparseArray<Bitmap>> images;
	private static ImageCacheByPosition instance;

	private ImageCacheByPosition() {
	}

	public static ImageCacheByPosition getInstance() {
		if (ImageCacheByPosition.instance == null) {
			ImageCacheByPosition.instance = new ImageCacheByPosition();
			ImageCacheByPosition.instance.images = new SparseArray<SparseArray<Bitmap>>();
		}

		return ImageCacheByPosition.instance;
	}

	public SparseArray<SparseArray<Bitmap>> getImages() {
		return this.images;
	}

	public void clear(int category) {
		if (this.images.get(category) != null) {
			this.images.get(category).clear();
			this.images.remove(category);
		}
	}
}
