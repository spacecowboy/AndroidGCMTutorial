package com.nononsenseapps.linksgcm;

import com.nononsenseapps.linksgcm.database.LinkItem;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class AddLinkService extends IntentService {

	private static final String ACTION_ADD = "com.nononsenseapps.linksgcm.action.ADD";
	private static final String EXTRA_LINK = "com.nononsenseapps.linksgcm.extra.LINK";

	/**
	 * Starts this service to perform action Foo with the given parameters. If
	 * the service is already performing a task this action will be queued.
	 * 
	 * @see IntentService
	 */
	public static void addLink(Context context, String uri) {
		Intent intent = new Intent(context, AddLinkService.class);
		intent.setAction(ACTION_ADD).putExtra(EXTRA_LINK, uri);
		context.startService(intent);
	}

	public AddLinkService() {
		super("AddLinkService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null) {
			final String action = intent.getAction();
			if (ACTION_ADD.equals(action)) {
				addLink(intent.getStringExtra(EXTRA_LINK));
			}
		}
	}

	private void addLink(final String uri) {
		if (uri == null || uri.isEmpty()) {
			return;
		}
		final ContentValues values = new ContentValues();
		values.put(LinkItem.COL_URL, uri);
		getContentResolver().insert(LinkItem.URI(), values);
	}
}
