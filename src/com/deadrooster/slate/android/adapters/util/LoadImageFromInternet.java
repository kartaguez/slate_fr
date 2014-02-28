package com.deadrooster.slate.android.adapters.util;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.SparseArray;
import android.widget.ImageView;

import com.deadrooster.slate.android.util.DefaultImage;

public class LoadImageFromInternet extends AsyncTask<String, Integer, Bitmap> {

	private static Context context;
	private int category;
	private long entryId;
	private String url;
	private final WeakReference<ImageView> imageViewReference;

	public LoadImageFromInternet(Context context, int category, long entryId, String url, ImageView imageView) {
		if (LoadImageFromInternet.context == null) {
			synchronized(LoadImageFromInternet.class) {
				if (LoadImageFromInternet.context == null) {
					LoadImageFromInternet.context = context;
				}
			}
		}
		this.category = category;
		this.entryId = entryId;
		this.url = url;
		this.imageViewReference = new WeakReference<ImageView>(imageView);
	}

	// download the thumbnail
	public void download() {

	     if (cancelPotentialDownload(this.url, this.imageViewReference.get())) {
	         DownloadedDrawable downloadedDrawable = new DownloadedDrawable(this);
	         this.imageViewReference.get().setImageDrawable(downloadedDrawable);
	         this.execute(url);
	     }

	}

	@Override
	protected Bitmap doInBackground(String... url) {

		Bitmap bitmap = null;

		try {
			bitmap = downloadBitmap(this.url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		return bitmap;
	}

	@Override
	protected void onPostExecute(Bitmap bitmap) {
		super.onPostExecute(bitmap);

		if (isCancelled()) {
			bitmap = null;
		}

		if (imageViewReference != null && bitmap != null) {
			ImageView imageView = imageViewReference.get();
			LoadImageFromInternet task = getImageTask(imageView);
			if (this == task) {
				imageView.setImageBitmap(bitmap);
				SparseArray<HashMap<Long, Bitmap>> imageCacheById = ImageCacheById.getInstance().getImages();
				if (imageCacheById != null) {
					if (imageCacheById.get(category) == null) {
						imageCacheById.put(category, new HashMap<Long, Bitmap>());
					}
					imageCacheById.get(category).put(entryId, bitmap);
				}
				updateEntryBitmap(bitmap);
			}
		}
	}

	private Bitmap downloadBitmap(String urlString) throws MalformedURLException {

		Bitmap bitmap = null;

		URL url = new URL(urlString);
		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) url.openConnection();
			bitmap = BitmapFactory.decodeStream(connection.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}

		return bitmap;
	}

	private static boolean cancelPotentialDownload(String url, ImageView imageView) {
		LoadImageFromInternet bitmapDownloaderTask = getImageTask(imageView);

	    if (bitmapDownloaderTask != null) {
	        String bitmapUrl = bitmapDownloaderTask.url;
	        if ((bitmapUrl == null) || (!bitmapUrl.equals(url))) {
	            bitmapDownloaderTask.cancel(true);
	        } else {
	            return false;
	        }
	    }
	    return true;
	}

	private static LoadImageFromInternet getImageTask(ImageView imageView) {
	    if (imageView != null) {
	        Drawable drawable = imageView.getDrawable();
	        if (drawable instanceof DownloadedDrawable) {
	            DownloadedDrawable downloadedDrawable = (DownloadedDrawable) drawable;
	            return downloadedDrawable.getBitmapDownloaderTask();
	        }
	    }
	    return null;
	}

	private void updateEntryBitmap(Bitmap bitmap) {

		SaveEntryImageTask updateTask = new SaveEntryImageTask(LoadImageFromInternet.context, this.entryId, bitmap);
		updateTask.execute();
	}

	static class DownloadedDrawable extends BitmapDrawable {
	    private final WeakReference<LoadImageFromInternet> bitmapDownloaderTaskReference;

	    public DownloadedDrawable(LoadImageFromInternet imageTask) {
	    	super(context.getResources(), DefaultImage.getInstance(context).getImage());
	        bitmapDownloaderTaskReference = new WeakReference<LoadImageFromInternet>(imageTask);
	    }

	    public LoadImageFromInternet getBitmapDownloaderTask() {
	        return bitmapDownloaderTaskReference.get();
	    }
	}

}
