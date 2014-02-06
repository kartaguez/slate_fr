package com.deadrooster.slate.android.ga;

import android.app.Activity;

import com.google.analytics.tracking.android.EasyTracker;

public class GAActivity extends Activity {

	  @Override
	  public void onStart() {
	    super.onStart();
	    EasyTracker.getInstance(this).activityStart(this);  // Add this method.
	  }

	  @Override
	  public void onStop() {
	    super.onStop();
	    EasyTracker.getInstance(this).activityStop(this);  // Add this method.
	  }

}
