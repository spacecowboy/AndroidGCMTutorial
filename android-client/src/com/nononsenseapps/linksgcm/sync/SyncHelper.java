package com.nononsenseapps.linksgcm.sync;

import java.io.IOException;

import retrofit.RestAdapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableNotifiedException;
import com.nononsenseapps.linksgcm.database.ItemProvider;

public class SyncHelper {

	public static final String KEY_ACCOUNT = "key_account";
	public static final String SCOPE = "oauth2:https://www.googleapis.com/auth/userinfo.email";
	static final String TAG = "Links";

	public static LinksServer getRESTAdapter() {
		RestAdapter restAdapter = new RestAdapter.Builder().setServer(
				LinksServer.API_URL).build();
		return restAdapter.create(LinksServer.class);
	}

	public static String getSavedAccountName(final Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getString(SyncHelper.KEY_ACCOUNT, null);
	}

	public static String getAuthToken(final Context context) {
		final String accountName = getSavedAccountName(context);
		if (accountName == null || accountName.isEmpty()) {
			return null;
		}

		return getAuthToken(context, accountName);
	}

	/**
	 * Only use this in a background thread, i.e. the syncadapter.
	 */
	public static String getAuthToken(final Context context,
			final String accountName) {
		try {
			return "Bearer " + GoogleAuthUtil.getTokenWithNotification(context,
					accountName, SCOPE, null, ItemProvider.AUTHORITY, null);
		}
		catch (UserRecoverableNotifiedException userRecoverableException) {
			// Unable to authenticate, but the user can fix this.
			Log.e(TAG,
					"Could not fetch token: "
							+ userRecoverableException.getMessage());
		}
		catch (GoogleAuthException fatalException) {
			Log.e(TAG, "Unrecoverable error " + fatalException.getMessage());
		}
		catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}
		return null;
	}

	public static Account getAccount(final Context context,
			final String accountName) {
		final AccountManager manager = AccountManager.get(context);
		Account[] accounts = manager
				.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
		for (Account account : accounts) {
			if (account.name.equals(accountName)) {
				return account;
			}
		}
		return null;
	}

	public static void manualSync(final Context context) {
		final String email = getSavedAccountName(context);

		if (email != null) {
			// Set it syncable
			final Account account = getAccount(context, email);

			if (!ContentResolver.isSyncActive(account, ItemProvider.AUTHORITY)) {
				Bundle options = new Bundle();
				// This will force a sync regardless of what the setting is
				// in accounts manager. Only use it here where the user has
				// manually desired a sync to happen NOW.
				// options.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
				ContentResolver.requestSync(account, ItemProvider.AUTHORITY,
						options);
			}
		}
	}
}
