package com.deadrooster.slate.android.activities;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.ViewGroup;

import com.deadrooster.slate.android.R;
import com.deadrooster.slate.android.adapters.util.Categories;
import com.deadrooster.slate.android.fragments.EntryDetailPagerFragment;

public class EntryDetailPagerActivity extends Activity {

	public static final String ACTIVATED_POSITION = "activated_position";

	private EntryPagerAdapter adapter;
	private ViewPager pager;

	private int activatedPosition;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.r_activity_entry_detail_pager);

		// Show the Up button in the action bar.
		getActionBar().setDisplayHomeAsUpEnabled(true);

		// Set title
		int category = getIntent().getIntExtra(EntryListActivity.CURRENT_CATEGORY, 0);
		String title = Categories.getInstance(this).getCategories().get(category)[1];
		this.getActionBar().setTitle(title);

		long[] entryIds = getIntent().getLongArrayExtra(EntryListActivity.ENTRY_IDS);
		if (savedInstanceState != null && savedInstanceState.containsKey(ACTIVATED_POSITION)) {
			this.activatedPosition = savedInstanceState.getInt(ACTIVATED_POSITION);
		} else {
			this.activatedPosition = getIntent().getIntExtra(EntryDetailPagerFragment.ARG_ITEM_POSITION, 0);
		}

		this.adapter = new EntryPagerAdapter(getFragmentManager(), entryIds, activatedPosition);

		this.pager = (ViewPager) findViewById(R.id.entry_detail_pager_id);
		this.pager.setAdapter(this.adapter);
		this.pager.setCurrentItem(activatedPosition);

		OnPageChangeListener onPageChangeListener = new OnPageChangeListener() {
			private EntryDetailPagerFragment lastFragment;
			private EntryDetailPagerFragment currentFragment;
			
			@Override
			public void onPageSelected(int position) {
				activatedPosition = position;
				if (currentFragment == null && adapter.getFirstPageSelected() != -1) {
					lastFragment = adapter.getFragments().get(adapter.getFirstPageSelected());
					adapter.setFirstPageSelected(-1);
				} else {
					lastFragment = currentFragment;
				}
				
				if (lastFragment != null) {
					lastFragment.resetScrollX();
					lastFragment.resetScrollY();
				}
				if (adapter != null && adapter.getFragments() != null && adapter.getFragments().get(position) != null) {
					currentFragment = adapter.getFragments().get(position);
				}
				
			}

			@Override
			public void onPageScrollStateChanged(int arg0) {
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {			
			}
		};
		this.pager.setOnPageChangeListener(onPageChangeListener);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt(ACTIVATED_POSITION, this.activatedPosition);
		super.onSaveInstanceState(outState);
	}

	public class EntryPagerAdapter extends FragmentStatePagerAdapter {

		private int firstPageSelected = -1;
		private SparseArray<EntryDetailPagerFragment> fragments = new SparseArray<EntryDetailPagerFragment>();
		private long[] entryIds;
		private int num;

		public EntryPagerAdapter(FragmentManager fm, long[] entryIds, int firstPageSelected) {
			super(fm);
			this.firstPageSelected = firstPageSelected;
			this.entryIds = entryIds;
			if(this.entryIds == null) {
				this.num = 0;
			} else {
				this.num = this.entryIds.length;
			}
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			EntryDetailPagerFragment fragment = (EntryDetailPagerFragment) super.instantiateItem(container, position);
			this.fragments.put(position, fragment);
			return fragment;
		}

		@Override
		public Fragment getItem(int position) {
			EntryDetailPagerFragment fragment = EntryDetailPagerFragment.newInstance(this.entryIds[position], this.num, position);
			this.fragments.put(position, fragment);
			return fragment;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			super.destroyItem(container, position, object);
			this.fragments.delete(position);
		}

		public EntryDetailPagerFragment getFragment(int position) {
			EntryDetailPagerFragment fragment = null;
			if (this.fragments != null) {
				fragment = this.fragments.get(position);
			}
			return fragment;
		}

		@Override
		public int getCount() {

			int count = 0;
			if (this.entryIds != null) {
				count = this.entryIds.length;
			}
			return count;
		}

		public int getFirstPageSelected() {
			return firstPageSelected;
		}

		public void setFirstPageSelected(int firstPageSelected) {
			this.firstPageSelected = firstPageSelected;
		}

		public SparseArray<EntryDetailPagerFragment> getFragments() {
			return fragments;
		}

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    case android.R.id.home:
	    	finish();
	        return true;
	    }
	    return super.onOptionsItemSelected(item);
	}

}
