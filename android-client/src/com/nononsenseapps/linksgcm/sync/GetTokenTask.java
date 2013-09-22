/*
 * Copyright 2012 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.nononsenseapps.linksgcm.sync;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.nononsenseapps.linksgcm.MainActivity;
import com.nononsenseapps.linksgcm.database.ItemProvider;

import android.accounts.Account;
import android.content.ContentResolver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;

/**
 * Display personalized greeting. This class contains boilerplate code to
 * consume the token but isn't integral to getting the tokens.
 */
public class GetTokenTask extends AsyncTask<Void, Void, String> {
	private static final String TAG = "TokenInfoTask";

	public static final int REQUEST_CODE_RECOVER_FROM_AUTH_ERROR = 1001;
	public static final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR = 1002;

	protected String mScope;
	protected String mEmail;
	protected MainActivity mActivity;

	public GetTokenTask(MainActivity activity, String email, String scope) {
		this.mActivity = activity;
		this.mScope = scope;
		this.mEmail = email;
	}

	@Override
	protected String doInBackground(Void... params) {
		String token = null;

		try {
			token = fetchToken();
		}
		catch (IOException e) {
			onError(e.getMessage(), e);
		}

		Log.d(TAG, "Token: " + token);

		if (token != null) {
			PreferenceManager.getDefaultSharedPreferences(mActivity).edit()
					.putString(SyncHelper.KEY_ACCOUNT, mEmail).commit();

			// Set it syncable
			final Account account = SyncHelper.getAccount(mActivity, mEmail);
			ContentResolver.setIsSyncable(account, ItemProvider.AUTHORITY, 1);
			ContentResolver.setSyncAutomatically(account,
					ItemProvider.AUTHORITY, true);
			// Set sync frequency
			// Set periodic syncing to once every day
			ContentResolver.addPeriodicSync(account, ItemProvider.AUTHORITY,
					new Bundle(), 60 * 60 * 24);

			// And trigger an immediate sync
			// Don't start a new sync if one is already going
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

		return token;
	}

	protected void onError(String msg, Exception e) {
		if (e != null) {
			Log.e(TAG, "Exception: ", e);
		}
	}

	/**
	 * Get a authentication token if one is not available. If the error is not
	 * recoverable then it displays the error message on parent activity.
	 */
	protected String fetchToken() throws IOException {
		try {
			return GoogleAuthUtil.getToken(mActivity, mEmail, mScope);
		}
		catch (GooglePlayServicesAvailabilityException playEx) {
			// GooglePlayServices.apk is either old, disabled, or not present.
			mActivity.showErrorDialog(playEx.getConnectionStatusCode());
		}
		catch (UserRecoverableAuthException userRecoverableException) {
			// Unable to authenticate, but the user can fix this.
			// Forward the user to the appropriate activity.
			mActivity.startActivityForResult(
					userRecoverableException.getIntent(),
					REQUEST_CODE_RECOVER_FROM_AUTH_ERROR);
		}
		catch (GoogleAuthException fatalException) {
			onError("Unrecoverable error " + fatalException.getMessage(),
					fatalException);
		}
		return null;
	}
}
