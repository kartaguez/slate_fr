package com.deadrooster.slate.android;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.deadrooster.slate.android.adapters.util.ImageCacheById;
import com.deadrooster.slate.android.adapters.util.ImageCacheByPosition;
import com.deadrooster.slate.android.fragments.EntryDetailFragment;
import com.deadrooster.slate.android.fragments.EntryListFragment;
import com.deadrooster.slate.android.ga.GAActivity;
import com.deadrooster.slate.android.preferences.Preferences;
import com.deadrooster.slate.android.provider.Uris;
import com.deadrooster.slate.android.services.PerformRefreshService;
import com.deadrooster.slate.android.services.ScheduleReceiver;
import com.deadrooster.slate.android.services.TriggerRefreshService;
import com.deadrooster.slate.android.tapstream.TapStreamImpl;
import com.deadrooster.slate.android.util.Callbacks;
import com.deadrooster.slate.android.util.ParcelableBoolean;
import com.deadrooster.slate.android.util.SlateCalendar;
import com.tapstream.sdk.Config;
import com.tapstream.sdk.Tapstream;

public class EntryListActivity extends GAActivity implements Callbacks, ActionBar.OnNavigationListener {

	public static final String CURRENT_CATEGORY = "current_category";

	private static final String IS_REFRESHING = "is_refreshing";

	private static final SimpleDateFormat refreshDateTimeFormat = new SimpleDateFormat("HH:mm", Locale.FRANCE);

	public static SparseArray<String[]> categories;

	private MenuItem refreshItem = null;
	private boolean twoPane = false;
	private int category = 0;
	private boolean isRefreshing = false;
	private long lastRefreshSuccessDate = -1;

	private ServiceConnection connection = new RefreshServiceConnection();
	private RefreshTriggeredReceiver refreshRequestedReceiver = new RefreshTriggeredReceiver();
	private RefreshCompletedReceiver refreshCompletedReceiver = new RefreshCompletedReceiver();

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		// init TapStream
		Config config = new Config();
		Tapstream.create(getApplication(), TapStreamImpl.ACCOUNT_NAME, TapStreamImpl.SDK_SECRET, config);

		// init categories
		initCategories();

		// init layout
		setContentView(R.layout.r_activity_entry_list_one_pane);

		// set up the the action bar to show a dropdown list.
		setUpActionBar();
	
		// load state
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(CURRENT_CATEGORY)) {
				this.category = savedInstanceState.getInt(CURRENT_CATEGORY);
			}
			if (savedInstanceState.containsKey(IS_REFRESHING)) {
				this.isRefreshing = savedInstanceState.getBoolean(IS_REFRESHING);
			}
			// TODO : suppress refresh at launch
//		} else {
//			this.isRefreshing = true;
		}

		// load required fragments
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

		// set refresh schedule
		ScheduleReceiver.setScheduleAheadOfReboot(this);

	}

	@Override
	public void onStart() {
		super.onStart();

		// refresh if needed
		if (this.isRefreshing) {
			launchRefreshBatch(false);
		}
	}

	@Override
	public void onPause() {
		stopRotatingRefreshIcon();

		// with services
		unbindService(connection);

		unregisterReceiver(this.refreshRequestedReceiver);
		unregisterReceiver(this.refreshCompletedReceiver);

		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();

		// with services
		registerReceiver(refreshRequestedReceiver, new IntentFilter(TriggerRefreshService.NOTIFICATION));
		registerReceiver(refreshCompletedReceiver, new IntentFilter(PerformRefreshService.NOTIFICATION));

		Intent i = new Intent(this, PerformRefreshService.class);
		bindService(i, this.connection, Context.BIND_AUTO_CREATE);

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
		this.refreshItem = menu.getItem(1);
		if (this.isRefreshing) {
			startRotateRefreshIcon();
		}
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		updateLastRefreshDate(menu);
		return super.onPrepareOptionsMenu(menu);
	}

	private void updateLastRefreshDate(Menu menu) {

		MenuItem lastRefreshDateItem = menu.getItem(0);
		TextView actionLastRefreshTime = (TextView) lastRefreshDateItem.getActionView();

		loadLastRefreshDate();

		if (this.lastRefreshSuccessDate == -1) {
			actionLastRefreshTime.setText("-");
		} else {
			SlateCalendar calendar = new SlateCalendar();
			calendar.setTimeInMillis(this.lastRefreshSuccessDate);

			String time = null;
			if (calendar.isToday()) {
				time = getTimeAsString(calendar);
			} else if (calendar.isYesterday()) {
				time = getResources().getString(R.string.date_prefix_yesterday) + getTimeAsString(calendar);
			} else {
//				DateTime lastRefreshDate = new DateTime(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND), calendar.get(Calendar.MILLISECOND));
				DateTime lastRefreshDate = new DateTime(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH), 0, 0, 0, 0);
				DateTime today = new DateTime();
				Period period = new Period(lastRefreshDate, today);

				PeriodFormatter formatter = new PeriodFormatterBuilder()
					.appendYears().appendSuffix(getResources().getString(R.string.time_span_years))
					.appendMonths().appendSuffix(getResources().getString(R.string.time_span_months))
					.appendWeeks().appendSuffix(getResources().getString(R.string.time_span_weeks))
					.appendDays().appendSuffix(getResources().getString(R.string.time_span_days))
/*					.appendMinutes().appendSuffix("Z")
					.appendSeconds().appendSuffix("S")*/
				    .printZeroNever()
				    .toFormatter();

				String cleanTimeSpan = null;
				String rawTimeSpan = formatter.print(period);
				int yearsIndex = rawTimeSpan.indexOf(getResources().getString(R.string.time_span_years));
				int monthsIndex = rawTimeSpan.indexOf(getResources().getString(R.string.time_span_months));
				int weeksIndex = rawTimeSpan.indexOf(getResources().getString(R.string.time_span_weeks));
				int daysIndex = rawTimeSpan.indexOf(getResources().getString(R.string.time_span_days));

/*				int minIndex = rawTimeSpan.indexOf("Z");
				int secIndex = rawTimeSpan.indexOf("S");*/

				int nb = 0;
				String unit = null;
				if (yearsIndex > -1) {
					nb = Integer.parseInt(rawTimeSpan.substring(0, yearsIndex));
					if (nb > 1) {
						unit = getResources().getString(R.string.time_ago_year_plur);
					} else {
						unit = getResources().getString(R.string.time_ago_year_sing);
					}
				} else if (monthsIndex > -1) {
					nb = Integer.parseInt(rawTimeSpan.substring(0, monthsIndex));
					if (nb > 1) {
						unit = getResources().getString(R.string.time_ago_month_plur);
					} else {
						unit = getResources().getString(R.string.time_ago_month_sing);
					}
				} else if (weeksIndex > -1) {
					nb = Integer.parseInt(rawTimeSpan.substring(0, weeksIndex));
					if (nb > 1) {
						unit = getResources().getString(R.string.time_ago_week_plur);
					} else {
						unit = getResources().getString(R.string.time_ago_week_sing);
					}
				} else {
					nb = Integer.parseInt(rawTimeSpan.substring(0, daysIndex));
					if (nb > 1) {
						unit = getResources().getString(R.string.time_ago_day_plur);
					} else {
						unit = getResources().getString(R.string.time_ago_day_sing);
					}
/*				} else if (minIndex > -1) {
					nb = Integer.parseInt(rawTimeSpan.substring(0, minIndex));
					unit = nb + " min. ";
					nb = Integer.parseInt(rawTimeSpan.substring(minIndex + 1, secIndex));
					unit = unit + nb + " sec.";
				} else {
					nb = Integer.parseInt(rawTimeSpan.substring(0, secIndex));
					unit = " sec.";*/
				}
				cleanTimeSpan = nb + " " + unit;
				time = String.format(getResources().getString(R.string.date_prefix_days_ago), cleanTimeSpan);
			}

			actionLastRefreshTime.setText(time);

		}
	}

	private String getTimeAsString(SlateCalendar calendar) {
		return getResources().getString(R.string.date_prefix_hour) + refreshDateTimeFormat.format(calendar.getTime());
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case R.id.refresh:
			launchRefreshBatch(false);
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void launchRefreshBatch(boolean alreadyLaunchedInBackground) {
		fireRefreshEvent();
		startRotateRefreshIcon();

		if (!alreadyLaunchedInBackground) {
			Intent i = new Intent(this, PerformRefreshService.class);
			startService(i);
		}
	}

	private void loadLastRefreshDate() {
		SharedPreferences settings = getSharedPreferences(Preferences.PREFS_NAME, 0);
	    this.lastRefreshSuccessDate = settings.getLong(Preferences.PREF_KEY_LAST_REFRESH_DATE, -1);
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
			Intent detailIntent = new Intent(this, EntryDetailActivity.class);
			detailIntent.putExtra(CURRENT_CATEGORY, this.category);
			detailIntent.putExtra(EntryDetailFragment.ARG_ITEM_ID, id);
			startActivity(detailIntent);
		}
	}

	// init
	private void initCategories() {
		EntryListActivity.categories.put(0, new String[] {"une", getString(R.string.section_une)});
		EntryListActivity.categories.put(1, new String[] {"france", getString(R.string.section_france)});
		EntryListActivity.categories.put(2, new String[] {"monde", getString(R.string.section_monde)});
		EntryListActivity.categories.put(3, new String[] {"economie", getString(R.string.section_economie)});
		EntryListActivity.categories.put(4, new String[] {"culture", getString(R.string.section_culture)});
		EntryListActivity.categories.put(5, new String[] {"life", getString(R.string.section_life)});
	}

	// action bar
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

	private void startRotateRefreshIcon() {

		this.isRefreshing = true;

		stopRotatingRefreshIcon();

		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		ImageView iv = (ImageView) inflater.inflate(R.layout.action_menu_refresh, null);

		Animation rotation = AnimationUtils.loadAnimation(this, R.anim.rotation_clockwise);
		rotation.setRepeatCount(Animation.INFINITE);
		iv.startAnimation(rotation);

		if (this.refreshItem != null) {
			this.refreshItem.setActionView(iv);
		}
	}

	private void stopRotatingRefreshIcon() {
		if (this.refreshItem != null) {
			if (this.refreshItem.getActionView() != null) {
				this.refreshItem.getActionView().clearAnimation();
				this.refreshItem.setActionView(null);
			}
		}
	}


	public void finalizeRefreshBatch(boolean isSuccess) {

		finalizeRefreshBatch(isSuccess, null);
	}

	public void finalizeRefreshBatch(boolean isSuccess, SparseArray<ParcelableBoolean> categorySuccesses) {

		this.isRefreshing = false;
		stopRotatingRefreshIcon();

	    if (isSuccess) {

	    	if (categorySuccesses != null) {
		    	for (int i = 0; i < categorySuccesses.size(); i++) {
		    		int curCategory = categorySuccesses.keyAt(i);
		    		if (categorySuccesses.get(curCategory).getBool()) {
		    			ImageCacheByPosition.getInstance().clear(curCategory);
		    			ImageCacheById.getInstance().clear(curCategory);
		    		}
		    	}
	    	}
			getContentResolver().notifyChange(Uris.Entries.CONTENT_URI_ENTRIES_DISTINCT, null);
			getContentResolver().notifyChange(Uris.Entries.CONTENT_URI_ENTRIES, null);
	    }

	    invalidateOptionsMenu();
	}

	// TapStream methods
	private void fireRefreshEvent() {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(CURRENT_CATEGORY, Integer.valueOf(this.category));
		TapStreamImpl.fireEvent(TapStreamImpl.Events.REFRESH, params);
	}

	// getters
	public boolean isTwoPane() {
		return twoPane;
	}

	public void setIsRefreshing(boolean isRefreshing) {
		this.isRefreshing = isRefreshing;
	}

	// private classes
	private class RefreshServiceConnection implements ServiceConnection {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			connection = null;
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder rawBinder) {
		}

	};

	private class RefreshTriggeredReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			launchRefreshBatch(true);
		}
		
	}

	private class RefreshCompletedReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {

			boolean isRefreshSuccess = intent.getBooleanExtra(PerformRefreshService.REFRESH_SUCCESSFUL, false);
			Bundle bundle = intent.getExtras();
			SparseArray<ParcelableBoolean> categorySuccesses = bundle.getSparseParcelableArray(PerformRefreshService.REFRESH_SUCCESSFUL_PER_CATEGORY);
			finalizeRefreshBatch(isRefreshSuccess, categorySuccesses);

		}
		
	}

	static {
		EntryListActivity.categories = new SparseArray<String[]>();
	}
}
