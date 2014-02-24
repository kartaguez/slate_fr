package com.deadrooster.slate.android.adapters.util;

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;

import com.deadrooster.slate.android.R;
import com.deadrooster.slate.android.services.PerformRefreshService;
import com.deadrooster.slate.android.util.Constants;

public class Categories {

	private static Categories instance;
	private static Context context; 
	private static SparseArray<String[]> categories;

	private Categories(){
	}

	public static Categories getInstance(Context context) {
		if (instance == null) {
			synchronized (Categories.class) {
				if (instance == null) {
					instance = new Categories();
					Categories.context = context.getApplicationContext();
					instance.initCategories();
				}
			}
		}
		return instance;
	}

	public SparseArray<String[]> getCategories() {
		initCategories();
		return categories;
	}

	private void initCategories() {
		if (categories.size() == 0) {
			Log.d(Constants.TAG, "PerformRefreshService: init categories");
			categories.put(0, new String[] {"une", context.getString(R.string.section_une), PerformRefreshService.URL_RSS_UNE});
			categories.put(1, new String[] {"france", context.getString(R.string.section_france), PerformRefreshService.URL_RSS_FRANCE});
			categories.put(2, new String[] {"monde", context.getString(R.string.section_monde), PerformRefreshService.URL_RSS_MONDE});
			categories.put(3, new String[] {"economie", context.getString(R.string.section_economie), PerformRefreshService.URL_RSS_ECONOMIE});
			categories.put(4, new String[] {"culture", context.getString(R.string.section_culture), PerformRefreshService.URL_RSS_CULTURE});
			categories.put(5, new String[] {"life", context.getString(R.string.section_life), PerformRefreshService.URL_RSS_LIFE});
		}
	}

	static {
		categories = new SparseArray<String[]>();
	}
}
