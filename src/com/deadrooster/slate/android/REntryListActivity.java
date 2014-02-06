package com.deadrooster.slate.android;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.deadrooster.slate.android.fragments.EntryDetailFragment;
import com.deadrooster.slate.android.fragments.EntryListFragment;
import com.deadrooster.slate.android.util.Callbacks;

public class REntryListActivity extends Activity implements Callbacks, ActionBar.OnNavigationListener {

	public static final String CURRENT_CATEGORY = "current_category";

	private static final String IS_REFRESHING = "is_refreshing";

	private MenuItem refreshItem = null;
	private boolean twoPane = false;
	private int category = 0;
	private boolean isRefreshing = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.r_activity_entry_list_one_pane);

		// set up the the action bar to show a dropdown list.
		setUpActionBar();
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(CURRENT_CATEGORY)) {
				this.category = savedInstanceState.getInt(CURRENT_CATEGORY);
			} else {
				this.category = 0;
			}
			if (savedInstanceState.containsKey(IS_REFRESHING)) {
				this.isRefreshing = savedInstanceState.getBoolean(IS_REFRESHING);
			} else {
				this.isRefreshing = false;
			}
		}

		if (this.isRefreshing) {
			startRotateRefreshIcon();
			((EntryListFragment) getFragmentManager().findFragmentById(R.id.entry_list_container)).refreshData();
		}

		if (findViewById(R.id.entry_detail_container) != null) {
			this.twoPane = true;
		}

		EntryListFragment fragment = (EntryListFragment) getFragmentManager().findFragmentById(R.id.entry_list_container);
		if (fragment == null) {
			fragment = new EntryListFragment();
			Bundle arguments = new Bundle();
			arguments.putBoolean(EntryListFragment.ARG_TWO_PANE, this.twoPane);
			fragment.setArguments(arguments);
			getFragmentManager().beginTransaction().replace(R.id.entry_list_container, fragment).commit();
		}

	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(CURRENT_CATEGORY, this.category);
		outState.putBoolean(IS_REFRESHING, this.isRefreshing);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		this.refreshItem = menu.getItem(0);
		resetRefreshActionView();
		if (this.isRefreshing) {
			startRotateRefreshIcon();
		}
		return true;
	}

	private void resetRefreshActionView() {
		this.refreshItem.setActionView(null);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case R.id.refresh:
			startRotateRefreshIcon();
			((EntryListFragment) getFragmentManager().findFragmentById(R.id.entry_list_container)).refreshData();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void setUpActionBar() {
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

		actionBar.setListNavigationCallbacks(
				new ArrayAdapter<String>(actionBar.getThemedContext(),
						android.R.layout.simple_list_item_1,
						android.R.id.text1, new String[] {
								getString(R.string.section_une),
								getString(R.string.section_france),
								getString(R.string.section_monde),
								getString(R.string.section_economie),
								getString(R.string.section_culture),
								getString(R.string.section_life),
								}), this);
	}

	@Override
	public boolean onNavigationItemSelected(int position, long itemId) {

		int formerCategory = this.category;
		this.category = position;

		if (this.category != formerCategory) {
			((EntryListFragment) getFragmentManager().findFragmentById(R.id.entry_list_container)).switchCategory(position);
		}

		return true;
	}

	public void stopRotateRefreshIcon() {

	    this.refreshItem.getActionView().clearAnimation();
	    resetRefreshActionView();
	    this.isRefreshing = false;
	}

	private void startRotateRefreshIcon() {

		this.isRefreshing = true;

		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		ImageView iv = (ImageView) inflater.inflate(R.layout.action_icon_refresh, null);

		Animation rotation = AnimationUtils.loadAnimation(this, R.anim.rotation_clockwise);
		rotation.setRepeatCount(Animation.INFINITE);
		iv.startAnimation(rotation);

		if (this.refreshItem != null) {
			this.refreshItem.setActionView(iv);
		}
	}

	/**
	 * Callback method from {@link EntryListFragment.Callbacks} indicating that
	 * the item with the given ID was selected.
	 */
	@Override
	public void onItemSelected(long id) {

		if (this.twoPane) {
			EntryDetailFragment fragment = (EntryDetailFragment) getFragmentManager().findFragmentById(R.id.entry_detail_container);
			if (fragment == null) {
				fragment = new EntryDetailFragment();
				Bundle arguments = new Bundle();
				arguments.putLong(EntryDetailFragment.ARG_ITEM_ID, id);
				fragment.setArguments(arguments);
				getFragmentManager().beginTransaction().replace(R.id.entry_detail_container, fragment).commitAllowingStateLoss();
			} else {
				fragment.updateContent(id);
			}
		} else {
			Intent detailIntent = new Intent(this, REntryDetailActivity.class);
			detailIntent.putExtra(CURRENT_CATEGORY, this.category);
			detailIntent.putExtra(EntryDetailFragment.ARG_ITEM_ID, id);
			startActivity(detailIntent);
		}
	}

	public boolean isTwoPane() {
		return twoPane;
	}

	public void setIsRefreshing(boolean isRefreshing) {
		this.isRefreshing = isRefreshing;
	}
}
