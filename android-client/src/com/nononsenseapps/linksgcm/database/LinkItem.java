package com.nononsenseapps.linksgcm.database;

import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

/**
 * Represents Link in the database.
 *
 */
public class LinkItem extends DBItem {
    public static final String TABLE_NAME = "Link";

    public static Uri URI() {
        return Uri.withAppendedPath(
            Uri.parse(ItemProvider.SCHEME
                      + ItemProvider.AUTHORITY), TABLE_NAME);
    }

    // Column names
    public static final String COL__ID = "_id";
    public static final String COL_SHA = "sha";
    public static final String COL_URL = "url";
    public static final String COL_TIMESTAMP = "timestamp";
    public static final String COL_DELETED = "deleted";
    public static final String COL_SYNCED = "synced";

    // For database projection so order is consistent
    public static final String[] FIELDS = { COL__ID, COL_SHA, COL_URL, COL_TIMESTAMP, COL_DELETED, COL_SYNCED };

    public long _id = -1;
    public String sha;
    public String url;
    public String timestamp = null;
    public long deleted = 0;
    public long synced = 0;

    public static final int BASEURICODE = 0x3b109c7;
    public static final int BASEITEMCODE = 0x87a22b7;

    public static void addMatcherUris(UriMatcher sURIMatcher) {
        sURIMatcher.addURI(ItemProvider.AUTHORITY, TABLE_NAME, BASEURICODE);
        sURIMatcher.addURI(ItemProvider.AUTHORITY, TABLE_NAME + "/#", BASEITEMCODE);
    }

    public static final String TYPE_DIR = "vnd.android.cursor.dir/vnd.example." + TABLE_NAME;
    public static final String TYPE_ITEM = "vnd.android.cursor.item/vnd.example." + TABLE_NAME;

    public LinkItem() {
        super();
    }

    public LinkItem(final Cursor cursor) {
        super();
        // Projection expected to match FIELDS array
        this._id = cursor.getLong(0);
        this.sha = cursor.getString(1);
        this.url = cursor.getString(2);
        this.timestamp = cursor.getString(3);
        this.deleted = cursor.getLong(4);
        this.synced = cursor.getLong(5);
    }

    public ContentValues getContent() {
        ContentValues values = new ContentValues();
        
        values.put(COL_SHA, sha);
        values.put(COL_URL, url);
        if (timestamp != null) values.put(COL_TIMESTAMP, timestamp);
        values.put(COL_DELETED, deleted);
        values.put(COL_SYNCED, synced);

        return values;
    }

    public String getTableName() {
        return TABLE_NAME;
    }

    public String[] getFields() {
        return FIELDS;
    }

    public long getId() {
        return _id;
    }

    public void setId(final long id) {
        _id = id;
    }

    public static final String CREATE_TABLE =
"CREATE TABLE Link"
+"  (_id INTEGER PRIMARY KEY,"
+"  sha TEXT NOT NULL,"
+"  url TEXT NOT NULL,"
+"  timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
+"  deleted INTEGER NOT NULL DEFAULT 0,"
+"  synced INTEGER NOT NULL DEFAULT 0,"
+""
+"  UNIQUE (url) ON CONFLICT IGNORE,"
+"  UNIQUE (sha) ON CONFLICT IGNORE)";
}
