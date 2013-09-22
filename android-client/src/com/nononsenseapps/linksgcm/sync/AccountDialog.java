package com.nononsenseapps.linksgcm.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.nononsenseapps.linksgcm.MainActivity;
import com.nononsenseapps.linksgcm.R;

/**
 * A copy of AccountDialog in SyncPrefs, but extending from support library
 * fragment.
 * 
 * In addition, a successful account choice will trigger an immediate sync.
 * 
 */
public class AccountDialog extends DialogFragment {

	@Override
	public Dialog onCreateDialog(Bundle args) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.select_account);
		final Account[] accounts = AccountManager.get(getActivity())
				.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
		final int size = accounts.length;
		String[] names = new String[size];
		for (int i = 0; i < size; i++) {
			names[i] = accounts[i].name;
		}
		// Could add a clear alternative here
		builder.setItems(names, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				// Stuff to do when the account is selected by the user
				accountSelected(accounts[which]);
			}
		});
		return builder.create();
	}

	/**
	 * Called from the activity, since that one builds the dialog
	 * 
	 * @param account
	 */
	public void accountSelected(Account account) {
		if (account != null) {
			//new TokenGetter().execute(account.name);
			new GetTokenTask((MainActivity) getActivity(), account.name, SyncHelper.SCOPE).execute();
		}
	}
}