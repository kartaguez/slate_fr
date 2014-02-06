package com.deadrooster.slate.android;

import android.app.Activity;
import android.os.Bundle;

import com.deadrooster.slate.android.fragments.EntryDetailFragment;

/**
 * An activity representing a single Entry detail screen. This activity is only
 * used on handset devices. On tablet-size devices, item details are presented
 * side-by-side with a list of items in a {@link REntryListActivity}.
 * <p>
 * This activity is mostly just a 'shell' activity containing nothing more than
 * a {@link EntryDetailFragment}.
 */
public class REntryDetailActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.r_activity_entry_detail);

		// Show the Up button in the action bar.
		getActionBar().setDisplayHomeAsUpEnabled(true);

		// Set title
		int category = getIntent().getIntExtra(REntryListActivity.CURRENT_CATEGORY, 0);
		String title = ActivityCategoryEntryList.categories.get(category)[1];
		this.getActionBar().setTitle(title);

		if (savedInstanceState == null) {
			// Create the detail fragment and add it to the activity
			// using a fragment transaction.
			Bundle arguments = new Bundle();
			arguments.putLong(EntryDetailFragment.ARG_ITEM_ID, getIntent().getLongExtra(EntryDetailFragment.ARG_ITEM_ID, -1));
			EntryDetailFragment fragment = new EntryDetailFragment();
			fragment.setArguments(arguments);
			getFragmentManager().beginTransaction().replace(R.id.entry_detail_container, fragment).commit();
		}
	}

	@Override
	public boolean onNavigateUp() {
		finish();
		return true;
	}
}
