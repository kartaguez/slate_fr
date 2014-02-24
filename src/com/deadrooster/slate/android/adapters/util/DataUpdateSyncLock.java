package com.deadrooster.slate.android.adapters.util;

public class DataUpdateSyncLock {

	private static DataUpdateSyncLock instance = null;

	private boolean dataBeingSwapped = false;

	private DataUpdateSyncLock() {
	}

	public static DataUpdateSyncLock getInstance() {
		if (instance == null) {
			synchronized (DataUpdateSyncLock.class) {
				if (instance == null) {
					instance = new DataUpdateSyncLock();
				}
				
			}
		}
		return instance;
	}

	public boolean areDataBeingSwapped() {
		return dataBeingSwapped;
	}

	public void setDataBeingSwapped(boolean dataBeingSwapped) {
		this.dataBeingSwapped = dataBeingSwapped;
	}

	
}
