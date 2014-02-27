package com.deadrooster.slate.android.util;


/**
 * A callback interface that all activities containing this fragment must
 * implement. This mechanism allows activities to be notified of item
 * selections.
 */
public interface Callbacks {
	/**
	 * Callback for when an item has been selected.
	 */
	public void onItemSelected(long id, int position);

}