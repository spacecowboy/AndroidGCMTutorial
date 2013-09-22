## Basic app
__SHA1__: todo

This is not a tutorial in making apps in general, so the starting point
is a working local-only app. No synchronization or network connectivity
is implemented at all. The app simply stores a list of links in its local
database. A user can share a link from anywhere and select 'Add to Links'
to add links easily and quickly.

TODO: Pictures here

## Adding a SyncAdapter
__SHA1__: todo

Synchronization needs to be done on a background thread. One could use an
AsyncTask, but we are going to go all the way and a SyncAdapter here instead.
Why? A SyncAdapter handles all the syncing for you. There is no need to
request a sync manually, you set a period and you're done. Even better,
a SyncAdapter respects the user's global sync setting. So if the user has
turned off sync, our app will respect that.

Setting up a SyncAdapter is fairly well covered in the docs so I won't go
too far into specifics there. What needs to be clarified are the bits that
make it work with the user's Google account.

Note below that I specify _"com.google"_ as the account type. This means
that our app will show up in the global sync settings under the Google
account. The authority is the same as specified in the ContentProvider.

__syncadapter.xml:__
```xml
<?xml version="1.0" encoding="utf-8"?>
<!--
  Important to use my own authority
  also specify that we want to use standard google account as the type
  also we want to be able to upload etc...
-->

<sync-adapter xmlns:android="http://schemas.android.com/apk/res/android"
    android:contentAuthority="com.nononsenseapps.linksgcm.database.AUTHORITY"
    android:accountType="com.google"
    android:supportsUploading="true"
    android:userVisible="true"
/>
```

The SyncAdapter itself is very simple, here is the onPerform method
as the rest is just short boilerplate:
```java
@Override
	public void onPerformSync(Account account, Bundle extras, String authority,
			ContentProviderClient provider, SyncResult syncResult) {
		try {
			// Need to get an access token first
			final String token = SyncHelper.getAuthToken(getContext(),
					account.name);

			if (token == null) {
				Log.e(TAG, "Token was null. Aborting sync");
				// Sync is rescheduled by SyncHelper
				return;
			}

			// token should be good. Transmit
			final LinksServer server = SyncHelper.getRESTAdapter();
			DatabaseHandler db = DatabaseHandler.getInstance(getContext());

			// Upload stuff
			for (LinkItem item : db.getAllLinkItems(LinkItem.COL_SYNCED
					+ " IS 0 OR " + LinkItem.COL_DELETED + " IS 1", null, null)) {
				if (item.deleted != 0) {
					// Delete the item
					server.deleteLink(token, item.sha);
					syncResult.stats.numDeletes++;
					db.deleteItem(item);
				}
				else {
					server.addLink(token, item);
					syncResult.stats.numInserts++;
					item.synced = 1;
					db.putItem(item);
				}
			}

			// Download stuff
			// Check if we synced before
			final String lastSync = PreferenceManager
					.getDefaultSharedPreferences(getContext()).getString(
							KEY_LASTSYNC, null);

			final LinkItems items;
			if (lastSync != null && !lastSync.isEmpty()) {
				items = server.listLinks(token, "true", lastSync);
			}
			else {
				items = server.listLinks(token, "false", null);
			}

			if (items != null && items.links != null) {
				for (LinkItem item : items.links) {
					if (item.deleted == 0) {
						db.putItem(item);
					}
					else {
						db.deleteItem(item);
					}
				}
			}

			// Save sync timestamp
			PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
					.putString(KEY_LASTSYNC, items.latestTimestamp).commit();
		}
		catch (RetrofitError e) {
			Log.e(TAG, e.getResponse().toString());
			// An HTTP error was encountered.
			switch (e.getResponse().getStatus()) {
			case 401: // Unauthorized
				syncResult.stats.numAuthExceptions++;
				break;
			case 404: // No such item, should never happen, programming error
			case 415: // Not proper body, programming error
			case 400: // Didn't specify url, programming error
				syncResult.databaseError = true;
				break;
			default: // Default is to consider it a networking problem
				syncResult.stats.numIoExceptions++;
				break;
			}
		}
	}
```

The general idea is as follows:
1. Get an access token
2. Upload new items and deletions from the client
3. Download new items and deletions from the server (if we have synced before, only fetch items newer than last time)
4. Save the timestamp from this sync for next time

The syncing model is simple because the app doesn't really have the idea
of updates. There is no way to update individual entries, only add new
ones or delete them. Hence we avoid the problem of sync conflicts
entirely. If your use case involves updating things, you'll have to
consider some kind of conflict resolution.

Let's have look at the SyncHelper class next. That's where the
access token is retrieved:

__SyncHelper.java:__
```java
public class SyncHelper {

	public static final String KEY_ACCOUNT = "key_account";
	public static final String SCOPE = "oauth2:https://www.googleapis.com/auth/userinfo.profile";
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
			return GoogleAuthUtil.getTokenWithNotification(context,
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
```

_accountName_ is the e-mail address of the user. If you're confused,
focus entirely on the _getAuthToken_ method. It returns an access token
if the user has/will authorized the app, and null if the user declined.
This is supposed to be used in the SyncAdapter, and what happens the first
time if unauthorized is that a notification will appear. If clicked,
the user gets a question if he/she wants to authorize the app to
access the profile information. That means we can potentially access
name, gender, birthday and a profile photo but we don't care about
those.

The actual network communication is handled by the excellent
[Retrofit](http://square.github.io/retrofit/) library. It basically
does for Java what Bottle did for the server app. Observe:

__LinksServer.java:__
```java
public interface LinksServer {

	public static final String API_URL = "http://192.168.1.17:5500";

	public static class LinkItems {
		String latestTimestamp;
		List<LinkItem> links;
	}

	public static class Dummy {
		// Methods must have return type
	}

	@GET("/links")
	LinkItems listLinks(@Header("Bearer") String token,
			@Query("showDeleted") String showDeleted,
			@Query("timestampMin") String timestampMin);

	@GET("/links/{sha}")
	LinkItem getLink(@Header("Bearer") String token, @Path("sha") String sha);

	@DELETE("/links/{sha}")
	Dummy deleteLink(@Header("Bearer") String token, @Path("sha") String sha);

	@POST("/links")
	LinkItem addLink(@Header("Bearer") String token, @Body LinkItem item);
}
```

You define an interface, and the library takes care of building an
actual object that talks to the server. Note that because the database
object _LinkItem_ has public fields, we can use it directly in this
interface. This is seriously __ALL__ the code required to talk
with a rest server. Notice also that the definitions match those
in the server.

That was __IT__. There are a few additional convenience classes and such
that I included to make the app more user friendly but this is all
that takes place behind the scenes. The sync only happens at fixed times
though. Either the user hits sync inside the app, or once a day. Next
up will be getting GCM up and running so we can get real time push
updates going.

## Adding GCM
todo
