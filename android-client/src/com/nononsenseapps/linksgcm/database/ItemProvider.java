package com.nononsenseapps.linksgcm.database;

import java.util.ArrayList;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

public class ItemProvider extends ContentProvider {
	public static final String AUTHORITY = "com.nononsenseapps.linksgcm.database.AUTHORITY";
	public static final String SCHEME = "content://";

	private static final UriMatcher sURIMatcher = new UriMatcher(
			UriMatcher.NO_MATCH);
	static {
		LinkItem.addMatcherUris(sURIMatcher);
	}

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// Setup some common parsing and stuff
		final String table;
		final ContentValues values = new ContentValues();
		final ArrayList<String> args = new ArrayList<String>();
		if (selectionArgs != null) {
			for (String arg : selectionArgs) {
				args.add(arg);
			}
		}
		final StringBuilder sb = new StringBuilder();
		if (selection != null && !selection.isEmpty()) {
			sb.append("(").append(selection).append(")");
		}

		// Configure table and args depending on uri
		switch (sURIMatcher.match(uri)) {
		case LinkItem.BASEITEMCODE:
			table = LinkItem.TABLE_NAME;
			if (selection != null && !selection.isEmpty()) {
				sb.append(" AND ");
			}
			sb.append(LinkItem.COL_ID + " IS ?");
			args.add(uri.getLastPathSegment());
			values.put(LinkItem.COL_DELETED, 1);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// Write to DB
		final SQLiteDatabase db = DatabaseHandler.getInstance(getContext())
				.getWritableDatabase();
		final String[] argArray = new String[args.size()];
		final int result = db.update(table, values, sb.toString(),
				args.toArray(argArray));

		if (result > 0) {
			// Support upload sync
			getContext().getContentResolver().notifyChange(uri, null, true);
		}
		return result;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		final Uri result;
		final String table;
		final DBItem item; // Just used for getting final URI

		// Configure table and args depending on uri
		switch (sURIMatcher.match(uri)) {
		case LinkItem.BASEURICODE:
			table = LinkItem.TABLE_NAME;
			if (!values.containsKey(LinkItem.COL_SHA)) {
				values.put(LinkItem.COL_SHA, LinkIDGenerator.generateID());
			}

			item = new LinkItem();
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// Write to DB
		final SQLiteDatabase db = DatabaseHandler.getInstance(getContext())
				.getWritableDatabase();
		final long id = db.insert(table, null, values);

		if (id > 0) {
			item.setId(id);
			result = item.getUri();
			// Support upload sync
			getContext().getContentResolver().notifyChange(uri, null, true);
		}
		else {
			result = null;
		}

		return result;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO: Implement this to handle requests to update one or more rows.
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public String getType(Uri uri) {
		switch (sURIMatcher.match(uri)) {

		case LinkItem.BASEITEMCODE:
			return LinkItem.TYPE_ITEM;
		case LinkItem.BASEURICODE:
			return LinkItem.TYPE_DIR;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] args, String sortOrder) {
		Cursor result = null;
		final long id;
		final DatabaseHandler handler = DatabaseHandler
				.getInstance(getContext());

		switch (sURIMatcher.match(uri)) {

		case LinkItem.BASEITEMCODE:
			id = Long.parseLong(uri.getLastPathSegment());
			result = handler.getLinkItemCursor(id);
			result.setNotificationUri(getContext().getContentResolver(), uri);
			break;
		case LinkItem.BASEURICODE:
			result = handler.getAllLinkItemsCursor(selection, args, sortOrder);
			result.setNotificationUri(getContext().getContentResolver(), uri);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		return result;
	}
}
