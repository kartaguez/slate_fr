package com.deadrooster.slate.android.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.deadrooster.slate.android.R;

public class DefaultImage {

	private static DefaultImage instance;
	private static Bitmap image;

	private DefaultImage() {
	};

	public static DefaultImage getInstance(Context context) {
		if (DefaultImage.instance == null) {
			DefaultImage.instance = new DefaultImage();
			DefaultImage.image = BitmapFactory.decodeResource(context.getResources(), R.drawable.default_image);
		}
		return DefaultImage.instance;
	}

	public Bitmap getImage() {
		return DefaultImage.image;
	}

}
