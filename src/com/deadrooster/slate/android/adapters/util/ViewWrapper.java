package com.deadrooster.slate.android.adapters.util;

import android.util.SparseArray;
import android.view.View;

public class ViewWrapper {
	
	
	private View base;
	private SparseArray<View> views;
	private LoadImageFromInternet imageTask;
	private LoadImageFromDb loadImageTask;

	public ViewWrapper(View base, int[] viewIds) {
		this.base = base;
		generateViews(viewIds);
	}

	public View getBase() {
		return this.base;
	}
	
	public View getView(int viewId) {
		return this.views.get(viewId);
	}
	
	private void generateViews(int[] viewIds) {
		if (viewIds != null) {
			int nbViews = viewIds.length;
			views = new SparseArray<View>();
			for (int k = 0; k < nbViews; k++) {
				views.put(viewIds[k], this.base.findViewById(viewIds[k]));
			}
		}
	}

	public LoadImageFromInternet getImageTask() {
		return imageTask;
	}

	public LoadImageFromDb getLoadImageTask() {
		return loadImageTask;
	}

	public void setImageTask(LoadImageFromInternet imageTask) {
		this.imageTask = imageTask;
	}

	public void setLoadImageTask(LoadImageFromDb loadImageTask) {
		this.loadImageTask = loadImageTask;
	}

	
}