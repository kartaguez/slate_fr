package com.deadrooster.slate.android.util;

import android.os.Parcel;
import android.os.Parcelable;

public class ParcelableBoolean implements Parcelable {

	private boolean bool = false;

	public ParcelableBoolean(boolean bool) {
		super();
		this.bool = bool;
	}

	public ParcelableBoolean(Parcel in) {
		boolean[] data = new boolean[1];

		in.readBooleanArray(data);
		this.bool = data[0];
	}

	public boolean getBool() {
		return bool;
	}

	public static final Parcelable.Creator<ParcelableBoolean> CREATOR = new Creator<ParcelableBoolean>() {

		@Override
		public ParcelableBoolean createFromParcel(Parcel source) {
			return new ParcelableBoolean(source);
		}

		@Override
		public ParcelableBoolean[] newArray(int size) {
			return new ParcelableBoolean[size];
		}
	};


	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeBooleanArray(new boolean[] {this.bool});
	}

}
