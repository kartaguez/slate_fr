package com.deadrooster.slate.android.adapters.util;

import java.util.HashMap;

import com.deadrooster.slate.android.util.Constants;

import android.graphics.Bitmap;
import android.util.Log;
import android.util.SparseArray;

public class ImageCacheById {

	public SparseArray<HashMap<Long, Bitmap>> images;
	private static ImageCacheById instance;

	private ImageCacheById() {
	}

	public static ImageCacheById getInstance() {
		if (ImageCacheById.instance == null) {
			ImageCacheById.instance = new ImageCacheById();
			ImageCacheById.instance.images = new SparseArray<HashMap<Long, Bitmap>>();
		}

		return ImageCacheById.instance;
	}

	public SparseArray<HashMap<Long, Bitmap>> getImages() {
		return this.images;
	}

	public void clear(int category) {
		Log.d(Constants.TAG, "ImageCacheById: clear category: " + category);
		if (this.images.get(category) != null) {
			this.images.get(category).clear();
			this.images.remove(category);
		}
	}
}
