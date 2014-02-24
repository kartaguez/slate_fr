package com.deadrooster.slate.android.util;

public class RefreshCounter {

	private int countRunningRefresh;
	private int maxRunningRefresh;
	private boolean atLeastOneStopped;
	private boolean active;

	public RefreshCounter(int max) {
		super();
		this.countRunningRefresh = 0;
		this.maxRunningRefresh = max;
		this.atLeastOneStopped = false;
		this.active = true;
	}

	public void incrementRefresh() {
		this.countRunningRefresh++;
	}

	public void decrementRefresh() {
		this.countRunningRefresh--;
		this.atLeastOneStopped = true;
	}

	public boolean isLast() {

		boolean ret = false;

		if (this.active && this.countRunningRefresh == 0) {
			if (maxRunningRefresh == 1) {
				ret = true;
			} else if (this.maxRunningRefresh > 1 && this.atLeastOneStopped) {
				ret = true;
			}
		}

		return ret;
	}

	public void deactivate() {
		this.active = false;
	}

	public boolean hasAtLeastOneStopped() {
		return this.atLeastOneStopped;
	}

}
