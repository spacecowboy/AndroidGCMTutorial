package com.nononsenseapps.linksgcm.gcm;

import java.io.IOException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.nononsenseapps.linksgcm.sync.LinksServer;
import com.nononsenseapps.linksgcm.sync.SyncHelper;
import com.nononsenseapps.linksgcm.sync.LinksServer.RegId;

public class GCMHelper {
	private static final String KEY_REGID = "KEY_REGID";
	private static final String KEY_APP_VERSION = "KEY_APP_VERSION";

	private static boolean isPlayServicesAvailable(final Context context) {
		int resultCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(context);
		return resultCode == ConnectionResult.SUCCESS;
	}

	/**
	 * @return Application's version code from the {@code PackageManager}.
	 */
	private static int getAppVersion(final Context context) {
		try {
			PackageInfo packageInfo = context.getPackageManager()
					.getPackageInfo(context.getPackageName(), 0);
			return packageInfo.versionCode;
		}
		catch (NameNotFoundException e) {
			// should never happen
			throw new RuntimeException("Could not get package name: " + e);
		}
	}

	private static void storeRegistrationId(final Context context,
			final String regid) {
		PreferenceManager.getDefaultSharedPreferences(context).edit()
				.putString(KEY_REGID, regid)
				.putInt(KEY_APP_VERSION, getAppVersion(context)).commit();
	}

	public static String getSavedRegistrationId(final Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getString(KEY_REGID, "");
	}

	private static String getRegistrationId(final Context context) {
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		final String regid = prefs.getString(KEY_REGID, "");

		if (regid.isEmpty()) {
			return "";
		}
		// Check if app was updated; if so, it must clear the registration ID
		// since the existing regID is not guaranteed to work with the new
		// app version.
		int registeredVersion = prefs
				.getInt(KEY_APP_VERSION, Integer.MIN_VALUE);
		int currentVersion = getAppVersion(context);
		if (registeredVersion != currentVersion) {
			return "";
		}

		return regid;
	}

	/**
	 * Handle registrations. If already registered, returns.
	 */
	public static void registerIfNotAlreadyDone(final Context context) {
		if (!isPlayServicesAvailable(context)) {
			return;
		}

		final String regid = getRegistrationId(context);
		if (regid.isEmpty()) {
			registerForGCM(context);
		}
	}

	private static void registerForGCM(final Context context) {
		try {
			GoogleCloudMessaging gcm = GoogleCloudMessaging
					.getInstance(context);

			final String regid = gcm.register(GCMConfig.SENDER_ID);

			if (sendRegistrationIdToBackend(context, regid)) {

				// Persist the regID - no need to register again.
				storeRegistrationId(context, regid);
			}
		}
		catch (IOException ex) {
			// If there is an error, don't just keep trying to register.
			// Require the user to click a button again, or perform
			// exponential back-off.
		}
	}

	private static boolean sendRegistrationIdToBackend(final Context context,
			final String regid) {
		// Need to get an access token first
		final String token = SyncHelper.getAuthToken(context,
				SyncHelper.getSavedAccountName(context));

		if (token == null) {
			return false;
		}

		// token should be good. Transmit
		final LinksServer server = SyncHelper.getRESTAdapter();
		final RegId item = new RegId();
		item.regid = regid;
		server.registerGCM(token, item);

		return true;
	}
}
