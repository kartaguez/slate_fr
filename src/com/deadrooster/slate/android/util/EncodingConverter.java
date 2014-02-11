package com.deadrooster.slate.android.util;

import java.io.UnsupportedEncodingException;

public class EncodingConverter {

	public static final String toUTF8(String latin) {
		String utf8 = null;
		try {
			utf8 =  new String(latin.getBytes("UTF-8"), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return utf8;
	}

	public static final String readAsUTF8(String latin) {
		String utf8 = null;
		try {
			utf8 =  new String(latin.getBytes("ISO-8859-1"), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return utf8;
	}

}
