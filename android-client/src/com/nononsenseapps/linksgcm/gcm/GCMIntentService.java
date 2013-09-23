package com.nononsenseapps.linksgcm.gcm;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.nononsenseapps.linksgcm.database.DatabaseHandler;
import com.nononsenseapps.linksgcm.database.LinkItem;
import com.nononsenseapps.linksgcm.sync.SyncHelper;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class GCMIntentService extends IntentService {
	public GCMIntentService() {
		super("GCMIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle extras = intent.getExtras();
		GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
		// The getMessageType() intent parameter must be the intent you received
		// in your BroadcastReceiver.
		String messageType = gcm.getMessageType(intent);

		if (!extras.isEmpty()) { // has effect of unparcelling Bundle
			/*
			 * Filter messages based on message type. Since it is likely that
			 * GCM will be extended in the future with new message types, just
			 * ignore any message types you're not interested in, or that you
			 * don't recognize.
			 */

			// If it's a regular GCM message, do some work.
			if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
				// Write link to database
				final LinkItem link = new LinkItem();
				link.sha = extras.getString("sha");
				link.timestamp = extras.getString("timestamp");
				link.url = extras.getString("url");
				link.synced = 1;
				if (Boolean.parseBoolean(extras.getString("deleted", "false"))) {
					link.deleted = 1;
				}

				if (link.deleted == 0) {
					DatabaseHandler.getInstance(this).putItem(link);
				}
				else {
					DatabaseHandler.getInstance(this).deleteItem(link);
				}

				Log.i("linksgcm", "Received: " + extras.toString()
						+ ", deleted: " + link.deleted);
			}
			else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED
					.equals(messageType)) {
				// We reached the limit of 100 queued messages. Request a full
				// sync
				SyncHelper.requestSync(this);
			}
		}
		// Release the wake lock provided by the WakefulBroadcastReceiver.
		GCMReceiver.completeWakefulIntent(intent);
	}
}
