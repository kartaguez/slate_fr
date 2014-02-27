package com.deadrooster.slate.android.activities;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.OperationApplicationException;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.deadrooster.slate.android.R;
import com.deadrooster.slate.android.adapters.util.Categories;
import com.deadrooster.slate.android.adapters.util.LoadNewDataTask;
import com.deadrooster.slate.android.fragments.EntryDetailPageFragment;
import com.deadrooster.slate.android.fragments.EntryDetailPaneFragment;
import com.deadrooster.slate.android.fragments.EntryListFragment;
import com.deadrooster.slate.android.ga.GAActivity;
import com.deadrooster.slate.android.model.Model.Entries;
import com.deadrooster.slate.android.preferences.Preferences;
import com.deadrooster.slate.android.provider.Provider;
import com.deadrooster.slate.android.provider.Uris;
import com.deadrooster.slate.android.services.NotifyRefreshRequestedService;
import com.deadrooster.slate.android.services.PerformRefreshService;
import com.deadrooster.slate.android.services.ScheduleRefreshReceiver;
import com.deadrooster.slate.android.tapstream.TapStreamImpl;
import com.deadrooster.slate.android.util.Callbacks;
import com.deadrooster.slate.android.util.Connectivity;
import com.deadrooster.slate.android.util.Constants;
import com.deadrooster.slate.android.util.SlateCalendar;
import com.tapstream.sdk.Config;
import com.tapstream.sdk.Tapstream;

public class EntryListActivity extends GAActivity implements Callbacks, ActionBar.OnNavigationListener {

	public static final String CURRENT_CATEGORY = "current_category";
	public static final String ACTIVATED_ITEM_ID = "activated_item_id";
	public static final String ENTRY_IDS = "entry_ids";

	public static final String[] PROJECTION = new String[] {Entries._ID, Entries.TITLE, Entries.DESCRIPTION, Entries.PREVIEW, Entries.THUMBNAIL_URL, Entries.THUMBNAIL_DATA, Entries.PUBLICATION_DATE, Entries.AUTHOR};
	public static final String SELECTION = "(" + Entries.CATEGORY + " = ?)";

	private static final SimpleDateFormat refreshDateTimeFormat = new SimpleDateFormat("HH:mm", Locale.FRANCE);

	private MenuItem refreshItem = null;
	private boolean twoPane = false;
	private int category = 0;
	private long activatedItemId = -1;
	private boolean refreshInProgressMustBeNotified = false;
	private long lastRefreshSuccessDate = -1;
	private long lastLoadedDataDate = -1;

	private PerformRefreshService performRefreshService = null;
	private ServiceConnection connection = new RefreshServiceConnection();
	private RefreshTriggeredReceiver refreshRequestedReceiver = new RefreshTriggeredReceiver();
	private RefreshCompletedReceiver refreshCompletedReceiver = new RefreshCompletedReceiver();

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		// init TapStream
		Config config = new Config();
		Tapstream.create(getApplication(), TapStreamImpl.ACCOUNT_NAME, TapStreamImpl.SDK_SECRET, config);

		// init layout
		setContentView(R.layout.r_activity_entry_list_one_pane);

		// set up the the action bar to show a dropdown list.
		setUpActionBar();

		// set refresh schedule
		ScheduleRefreshReceiver.setScheduleAfterFirstLaunch(this);

		// load state
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(CURRENT_CATEGORY)) {
				this.category = savedInstanceState.getInt(CURRENT_CATEGORY);
			}
			if (savedInstanceState.containsKey(ACTIVATED_ITEM_ID)) {
				this.activatedItemId = savedInstanceState.getLong(ACTIVATED_ITEM_ID);
			}
		}
		else if (Connectivity.isWifiConnected(this) || isFirstLaunchEver()) {
			refreshInProgressMustBeNotified = true;
			invalidateOptionsMenu();
		}

		// load required fragments
		if (findViewById(R.id.entry_detail_container) != null) {
			this.twoPane = true;
		}
		EntryListFragment fragment = (EntryListFragment) getFragmentManager().findFragmentById(R.id.entry_list_container);
		if (fragment == null) {
			Log.d(Constants.TAG, "EntryListActivity: EntryListFragment created");
			fragment = new EntryListFragment();
			Bundle arguments = new Bundle();
			arguments.putBoolean(EntryListFragment.ARG_TWO_PANE, this.twoPane);
			arguments.putInt(CURRENT_CATEGORY, this.category);
			fragment.setArguments(arguments);
			getFragmentManager().beginTransaction().replace(R.id.entry_list_container, fragment).commit();
		} else {
			Log.d(Constants.TAG, "EntryListActivity: EntryListFragment not created");
		}

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(Constants.TAG, "EntryListActivity: onActivityResult");
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onStart() {
		Log.d(Constants.TAG, "EntryListActivity: onStart");
		super.onStart();
	}

	@Override
	protected void onResume() {
		Log.d(Constants.TAG, "EntryListActivity: onResume");
		super.onResume();

		// with services
		registerReceiver(this.refreshRequestedReceiver, new IntentFilter(NotifyRefreshRequestedService.NOTIFICATION));
		registerReceiver(this.refreshCompletedReceiver, new IntentFilter(PerformRefreshService.NOTIFICATION));

		if (this.performRefreshService == null) {
			Intent i = new Intent(this, PerformRefreshService.class);
			bindService(i, this.connection, Context.BIND_AUTO_CREATE);
		}

		if (this.refreshInProgressMustBeNotified) {
			launchRefreshActions(false, false);
		}

		loadLastRefreshDate();
		boolean swapAndLoadNeeded = checkDataSwapAndLoadNeeded();
		if (swapAndLoadNeeded) {
			Log.d(Constants.TAG, "EntryListActivity: data swap and load needed");
			swapAndLoadData();
		} else {
			Log.d(Constants.TAG, "EntryListActivity: data swap and load not needed");
		}

		// redraw options menu
		Log.d(Constants.TAG, "EntryListActivity: onResume: invalidateOptionsMenu");
		invalidateOptionsMenu();

	}

	public void onRestoreInstanceState(Bundle outState) {
		Log.d(Constants.TAG, "EntryListActivity: onRestoreInstanceState");
		if (outState != null) {
			if (outState.containsKey(CURRENT_CATEGORY)) {
				this.category = outState.getInt(CURRENT_CATEGORY);
			}
			if (outState.containsKey(ACTIVATED_ITEM_ID)) {
				this.activatedItemId = outState.getLong(ACTIVATED_ITEM_ID);
			}
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		Log.d(Constants.TAG, "EntryListActivity: onSaveInstanceState");
		super.onSaveInstanceState(outState);
		outState.putInt(CURRENT_CATEGORY, this.category);
		outState.putLong(ACTIVATED_ITEM_ID, this.activatedItemId);
	}

	@Override
	public void onPause() {
		Log.d(Constants.TAG, "EntryListActivity: onPause");
		stopRotatingRefreshIcon();

		// with services
		if (this.performRefreshService != null) {
			unbindService(this.connection);
			this.performRefreshService = null;
		}

		unregisterReceiver(this.refreshRequestedReceiver);
		unregisterReceiver(this.refreshCompletedReceiver);

		super.onPause();
	}

	@Override
	public void onStop() {
		Log.d(Constants.TAG, "EntryListActivity: onStop");
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		Log.d(Constants.TAG, "EntryListActivity: onDestroy");
		super.onDestroy();
	}

	/**
	 * Callback method from {@link EntryListFragment.Callbacks} indicating that
	 * the item with the given ID was selected.
	 */
	@Override
	public void onItemSelected(long id, int position) {
		Log.d(Constants.TAG, "EntryListActivity: onItemSelected: position: " + position + ", id: " + id);
		if (this.twoPane) {
			EntryDetailPaneFragment fragment = (EntryDetailPaneFragment) getFragmentManager().findFragmentById(R.id.entry_detail_container);
			if (fragment == null) {
				fragment = new EntryDetailPaneFragment();
				Bundle arguments = new Bundle();
				arguments.putLong(EntryDetailPaneFragment.ARG_ITEM_ID, id);
				fragment.setArguments(arguments);
				FragmentTransaction ft = getFragmentManager().beginTransaction();
				ft.setCustomAnimations(android.R.animator.fade_in, 0);
				ft.replace(R.id.entry_detail_container, fragment).commitAllowingStateLoss();
			} else {
				if (this.activatedItemId != id) {
					this.activatedItemId = id;
					fragment.updateContent(id);
				}
			}
		} else {
			EntryListFragment fragment = (EntryListFragment) getFragmentManager().findFragmentById(R.id.entry_list_container);
			long[] entryIds = fragment.getEntryIds();
			
			Intent detailIntent = new Intent(this, EntryDetailPagerActivity.class);
			detailIntent.putExtra(CURRENT_CATEGORY, this.category);
			detailIntent.putExtra(ENTRY_IDS, entryIds);
			detailIntent.putExtra(EntryDetailPageFragment.ARG_NUM, entryIds.length);
			detailIntent.putExtra(EntryDetailPageFragment.ARG_ITEM_POSITION, position);
			EntryDetailPageFragment.resetScrollPositions();
			Log.d(Constants.TAG, "EntryListActivity: onItemSelected: CURRENT_CATEGORY: " + this.category + ", ARG_NUM: " + entryIds.length + ", ARG_ITEM_POSITION: "  + position);
			startActivityForResult(detailIntent, 0);
			overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
		}
	}

	// options menu
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case R.id.refresh:
			launchRefreshActions(false, true);
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		this.refreshItem = menu.getItem(1);
		if (this.refreshInProgressMustBeNotified) {
			Log.d(Constants.TAG, "EntryListActivity: onCreateOptionsMenu: refreshInProgressMustBeNotified: true");
			startRotatingRefreshIcon();
		} else {
			Log.d(Constants.TAG, "EntryListActivity: onCreateOptionsMenu: refreshInProgressMustBeNotified: false");
		}
		Log.d(Constants.TAG, "EntryListActivity: OptionsMenu created");
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		updateLastRefreshDate(menu);
		getActionBar().setSelectedNavigationItem(this.category);
		Log.d(Constants.TAG, "EntryListActivity: OptionsMenu prepared");
		return super.onPrepareOptionsMenu(menu);
	}

	private void updateLastRefreshDate(Menu menu) {
		Log.d(Constants.TAG, "EntryListActivity: updateLastRefreshDate");
		MenuItem lastRefreshDateItem = menu.getItem(0);
		TextView actionLastRefreshTime = (TextView) lastRefreshDateItem.getActionView();

		if (this.lastRefreshSuccessDate != -1) {
			SlateCalendar calendar = new SlateCalendar();
			calendar.setTimeInMillis(this.lastRefreshSuccessDate);

			String time = null;
			if (calendar.isToday()) {
				time = getTimeAsString(calendar);
			} else if (calendar.isYesterday()) {
				time = getResources().getString(R.string.date_prefix_yesterday) + getTimeAsString(calendar);
			} else {
				DateTime lastRefreshDate = new DateTime(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH), 0, 0, 0, 0);
				DateTime today = new DateTime();
				Period period = new Period(lastRefreshDate, today);

				PeriodFormatter formatter = new PeriodFormatterBuilder()
					.appendYears().appendSuffix(getResources().getString(R.string.time_span_years))
					.appendMonths().appendSuffix(getResources().getString(R.string.time_span_months))
					.appendWeeks().appendSuffix(getResources().getString(R.string.time_span_weeks))
					.appendDays().appendSuffix(getResources().getString(R.string.time_span_days))
				    .printZeroNever()
				    .toFormatter();

				String cleanTimeSpan = null;
				String rawTimeSpan = formatter.print(period);
				int yearsIndex = rawTimeSpan.indexOf(getResources().getString(R.string.time_span_years));
				int monthsIndex = rawTimeSpan.indexOf(getResources().getString(R.string.time_span_months));
				int weeksIndex = rawTimeSpan.indexOf(getResources().getString(R.string.time_span_weeks));
				int daysIndex = rawTimeSpan.indexOf(getResources().getString(R.string.time_span_days));

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
				}
				cleanTimeSpan = nb + " " + unit;
				time = String.format(getResources().getString(R.string.date_prefix_days_ago), cleanTimeSpan);
			}

			actionLastRefreshTime.setText(time);

		}
	}

	private void loadLastRefreshDate() {
		Log.d(Constants.TAG, "EntryListActivity: loadLastRefreshDate");

		SharedPreferences settings = getSharedPreferences(Preferences.PREFS_NAME, 0);
	    this.lastRefreshSuccessDate = settings.getLong(Preferences.PREF_KEY_LAST_REFRESH_DATE, -1);
	}

	// data
	private boolean checkDataSwapAndLoadNeeded() {
		Log.d(Constants.TAG, "EntryListActivity: checkDataReloadNeeded");

		boolean ret = false;
		SharedPreferences settings = getSharedPreferences(Preferences.PREFS_NAME, 0);
	    this.lastLoadedDataDate = settings.getLong(Preferences.PREF_KEY_LAST_LOADED_DATA_DATE, -1);

	    if (this.lastLoadedDataDate < this.lastRefreshSuccessDate) {
	    	ret = true;
	    }

	    return ret;
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

	private void startRotatingRefreshIcon() {

		stopRotatingRefreshIcon();

		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		ImageView iv = (ImageView) inflater.inflate(R.layout.action_menu_refresh, null);

		Animation rotation = AnimationUtils.loadAnimation(this, R.anim.rotation_clockwise);
		rotation.setRepeatCount(Animation.INFINITE);
		iv.startAnimation(rotation);

		if (this.refreshItem != null) {
			Log.d(Constants.TAG, "EntryListActivity: startRotateRefreshIcon");
			this.refreshItem.setActionView(iv);
		} else {
			Log.d(Constants.TAG, "EntryListActivity: cannot startRotateRefreshIcon: item null");
		}
	}

	private void stopRotatingRefreshIcon() {
		Log.d(Constants.TAG, "EntryListActivity: stopRotatingRefreshIcon");
		this.refreshInProgressMustBeNotified = false;
		if (this.refreshItem != null) {
			if (this.refreshItem.getActionView() != null) {
				this.refreshItem.getActionView().clearAnimation();
				this.refreshItem.setActionView(null);
			}
		}
	}

	// refresh actions
	private void launchRefreshActions(boolean alreadyLaunchedInBackground, boolean handleRefreshIcon) {
		Log.d(Constants.TAG, "EntryListActivity: launchRefreshBatch: " + alreadyLaunchedInBackground + ", " + handleRefreshIcon);
		fireRefreshEvent();
		if (handleRefreshIcon) {
			startRotatingRefreshIcon();
		}

		if (!alreadyLaunchedInBackground) {
			Intent i = new Intent(this, PerformRefreshService.class);
			startService(i);
		}
	}

	public void finalizeRefreshBatch() {
		Log.d(Constants.TAG, "EntryListActivity: finalizeRefreshBatch");
		stopRotatingRefreshIcon();

	    invalidateOptionsMenu();
	}

	// copy newly fetched data into the table used for display
	private void swapAndLoadData() {

    	Log.d(Constants.TAG, "EntryListActivity: swapAndLoadData: update last loaded data date");
    	SharedPreferences settings = getSharedPreferences(Preferences.PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
	    editor.putLong(Preferences.PREF_KEY_LAST_LOADED_DATA_DATE, this.lastRefreshSuccessDate);
	    editor.commit();

		Log.d(Constants.TAG, "EntryListActivity: swapAndLoadData: swap data");
		// TODO JGU #1: swap in background or UI thread
		LoadNewDataTask loadNewDataTask = new LoadNewDataTask(this);
		loadNewDataTask.execute();
//		executeSwapping();
		///JGU #1
	}

	// TODO JGU #1
	@SuppressWarnings("unused")
	///JGU #1
	private void executeSwapping() {
		ContentResolver cr = getContentResolver();
		Cursor c = null;
		ContentValues values = null;

		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

		int nbCategories = Categories.getInstance(this).getCategories().size();
		for (int i = 0; i < nbCategories; i++) {
			ops.add(ContentProviderOperation
					.newDelete(Uris.Entries.CONTENT_URI_ENTRIES)
					.withSelection(PerformRefreshService.SELECTION_DELETE, new String[] {Integer.toString(i)})
					.build());
			c = cr.query(Uris.Entries.CONTENT_URI_ENTRIES_TEMP, EntryListActivity.PROJECTION, EntryListActivity.SELECTION, new String[] {Integer.toString(i)}, null);

			while (c.moveToNext()) {
				values = new ContentValues();
				values.put(Entries.CATEGORY, i);
				values.put(Entries.TITLE, c.getString(1));
				values.put(Entries.DESCRIPTION, c.getString(2));
				values.put(Entries.PREVIEW, c.getString(3));
				values.put(Entries.THUMBNAIL_URL, c.getString(4));
				values.put(Entries.PUBLICATION_DATE, c.getString(6));
				values.put(Entries.AUTHOR, c.getString(7));
				ops.add(ContentProviderOperation
						.newInsert(Uris.Entries.CONTENT_URI_ENTRIES)
						.withValues(values)
						.build());
			}
		}
		if (c != null) {
			c.close();
		}
		try {
			cr.applyBatch(Provider.AUTHORITY, ops);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (OperationApplicationException e) {
			e.printStackTrace();
		}
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

	private boolean isFirstLaunchEver() {
		SharedPreferences settings = getSharedPreferences(Preferences.PREFS_NAME, 0);
	    boolean hasAlreadyBeenLaunched = settings.getBoolean(Preferences.PREF_KEY_HAS_ALREADY_BEEN_LAUNCHED, false);

	    if (!hasAlreadyBeenLaunched) {
			SharedPreferences.Editor editor = settings.edit();
		    editor.putBoolean(Preferences.PREF_KEY_HAS_ALREADY_BEEN_LAUNCHED, true);
		    editor.commit();
		}

	    return !hasAlreadyBeenLaunched;
	}

	private String getTimeAsString(SlateCalendar calendar) {
		return getResources().getString(R.string.date_prefix_hour) + refreshDateTimeFormat.format(calendar.getTime());
	}

	// private classes
	private class RefreshServiceConnection implements ServiceConnection {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.d(Constants.TAG, "EntryListActivity: RefreshServiceConnection: onServiceDisconnected");
			performRefreshService = null;
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder rawBinder) {
			Log.d(Constants.TAG, "EntryListActivity: RefreshServiceConnection: onServiceConnected");
			performRefreshService = ((PerformRefreshService.SlateBinder) rawBinder).getService();
			if (performRefreshService.isRefreshInProgress()) {
				Log.d(Constants.TAG, "EntryListActivity: RefreshServiceConnection:: refresh in progress: start rotating icon");
				refreshInProgressMustBeNotified = true;
				invalidateOptionsMenu();
				launchRefreshActions(true, false);
			} else {
				Log.d(Constants.TAG, "EntryListActivity: RefreshServiceConnection:: no refresh in progress");
			}
		}

	};

	private class RefreshTriggeredReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(Constants.TAG, "EntryListActivity: RefreshTriggeredReceiver received");
			launchRefreshActions(true, true);
		}
		
	}

	private class RefreshCompletedReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(Constants.TAG, "EntryListActivity: RefreshCompletedReceiver received");
			boolean isGlobalSuccess = intent.getBooleanExtra(PerformRefreshService.IS_GLOBAL_SUCCESS, false);
			if (isGlobalSuccess) {
				loadLastRefreshDate();
				swapAndLoadData();
			}
			finalizeRefreshBatch();
		}
		
	}

}
