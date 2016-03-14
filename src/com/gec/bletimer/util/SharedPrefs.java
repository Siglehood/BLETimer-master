package com.gec.bletimer.util;

import android.content.Context;

public class SharedPrefs {
	private static final String SHARED_PREFS_NAME = "ble_timer_shared";

	public static String getSharedPrefsStr(Context context, String key) {
		return context.getSharedPreferences(SHARED_PREFS_NAME, 0).getString(key, null);
	}

	public static int getSharedPrefsInt(Context context, String key) {
		return context.getSharedPreferences(SHARED_PREFS_NAME, 0).getInt(key, 0);
	}

	public static boolean getSharedPrefsBool(Context context, String key) {
		return context.getSharedPreferences(SHARED_PREFS_NAME, 0).getBoolean(key, false);
	}

	public static void putSharedPrefs(Context context, String key, String value) {
		context.getSharedPreferences(SHARED_PREFS_NAME, 0).edit().putString(key, value).commit();
	}

	public static void putSharedPrefs(Context context, String key, int value) {
		context.getSharedPreferences(SHARED_PREFS_NAME, 0).edit().putInt(key, value).commit();
	}

	public static void putSharedPrefs(Context context, String key, boolean value) {
		context.getSharedPreferences(SHARED_PREFS_NAME, 0).edit().putBoolean(key, value).commit();
	}

	public static void clearSharedPrefs(Context context) {
		context.getSharedPreferences(SHARED_PREFS_NAME, 0).edit().clear().commit();
	}
}
