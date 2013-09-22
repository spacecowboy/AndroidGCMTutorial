package com.nononsenseapps.linksgcm;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.nononsenseapps.linksgcm.sync.AccountDialog;
import com.nononsenseapps.linksgcm.sync.GetTokenTask;
import com.nononsenseapps.linksgcm.sync.SyncHelper;

import android.os.Bundle;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.view.Menu;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		getFragmentManager().beginTransaction()
				.add(R.id.mainContent, new LinkFragment()).commit();

		if (null == SyncHelper.getSavedAccountName(this)) {
			final Account[] accounts = AccountManager.get(this)
					.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);

			if (accounts.length == 1) {
				new GetTokenTask(this, accounts[0].name, SyncHelper.SCOPE)
						.execute();
			}
			else if (accounts.length > 1) {
				DialogFragment dialog = new AccountDialog();
				dialog.show(getFragmentManager(), "account_dialog");
			}
		}
	}

	/**
	 * This method is a hook for background threads and async tasks that need to
	 * launch a dialog. It does this by launching a runnable under the UI
	 * thread.
	 */
	public void showErrorDialog(final int code) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Dialog d = GooglePlayServicesUtil
						.getErrorDialog(
								code,
								MainActivity.this,
								GetTokenTask.REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
				d.show();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		// getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
