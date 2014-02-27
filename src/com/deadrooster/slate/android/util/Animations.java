package com.deadrooster.slate.android.util;

import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

public class Animations {

	public static Animation fadeInAnim = null;

	static {
		fadeInAnim = new AlphaAnimation(0.0f, 1.0f);
		fadeInAnim.setDuration(200);
		fadeInAnim.setZAdjustment(Animation.ZORDER_BOTTOM);
	}
}
