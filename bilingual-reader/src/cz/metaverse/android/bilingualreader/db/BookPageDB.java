/*
Bilingual Reader for Android

Copyright (c) 2015 Tomáš Orsava

This program is free software: you can redistribute it and/or modify it under the terms
of the GNU General Public License as published by the Free Software Foundation, either
version 3 of the License, or any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this
program (see the LICENSE.html file). If not, see http://www.gnu.org/licenses/.

Contact: gpl at orsava.cz
 */

package cz.metaverse.android.bilingualreader.db;

import java.util.Arrays;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;
import cz.metaverse.android.bilingualreader.enums.ScrollSyncMethod;

/**
 *
 * Class that abstracts our Book Page database table.
 * Uses the singleton pattern - use static method getInstance().
 *
 * The columns COL_BOOK_FILENAME and COL_BOOK_TITLE serve together to uniquely identify a given
 * book, and then COL_PAGE_FILENAME uniquely identifies given page in the book.
 *
 */
public class BookPageDB {

	// For the singleton pattern - only one instance will be created.
	private static BookPageDB bookPageDBInstance;

	// Logging tag
	private static final String LOG = "BookPageDB";

	// The columns of the database table
	public static final String COL_ROWID = "_id";
	public static final String COL_BOOK_FILENAME = "BOOK_FILENAME";
	public static final String COL_BOOK_TITLE = "BOOK_TITLE";
	public static final String COL_PAGE_FILENAME = "PAGE_FILENAME";
	public static final String COL_PAGE_RELATIVE_PATH = "PAGE_RELATIVE_PATH";
	public static final String COL_SCROLL_Y = "SCROLL_Y";
	public static final String COL_SCROLLSYNC_METHOD = "SCROLLSYNC_METHOD";
	public static final String COL_SCROLLSYNC_OFFSET = "SCROLLSYNC_OFFSET";
	public static final String COL_SCROLLSYNC_RATIO = "SCROLLSYNC_RATIO";
	public static final String COL_PARAGRAPHS_COUNT = "PARAGRAPHS_COUNT";
	public static final String COL_LAST_OPENED = "LAST_OPENED";

	// Array of all available columns to be used for results.
	public static final String [] allColumns = new String[] {COL_ROWID,
			COL_BOOK_FILENAME, COL_BOOK_TITLE, COL_PAGE_FILENAME, COL_PAGE_RELATIVE_PATH, COL_SCROLL_Y,
			COL_SCROLLSYNC_METHOD, COL_SCROLLSYNC_OFFSET, COL_SCROLLSYNC_RATIO,
			COL_PARAGRAPHS_COUNT, COL_LAST_OPENED};

	/* Database table */
	protected static final String TABLE_NAME = "BOOKPAGE";
	// SQL command that creates the database.
	protected static final String TABLE_CREATE = "CREATE TABLE " + TABLE_NAME +" ("
			+ COL_ROWID + " integer primary key autoincrement, "
			+ COL_BOOK_FILENAME + " text not null, "
			+ COL_BOOK_TITLE + " text, "
			+ COL_PAGE_FILENAME + " text not null, "
			+ COL_PAGE_RELATIVE_PATH + " text not null, "
			+ COL_SCROLL_Y + " real, "
			+ COL_SCROLLSYNC_METHOD + " text, "
			+ COL_SCROLLSYNC_OFFSET + " real, "
			+ COL_SCROLLSYNC_RATIO + " real, "
			+ COL_PARAGRAPHS_COUNT + " integer, "
			+ COL_LAST_OPENED + " integer)";

	// The class does most of the interaction with the database.
	private final DatabaseManager dbManager;
	private final Context savedContext;


	/**
	 * Creates, if necessary, an instance of this class and returns it.
	 */
	public static BookPageDB getInstance(Context context) {
		//Log.d("hugo", context + " " + bookPageDBInstance.savedContext);

		if (bookPageDBInstance == null || bookPageDBInstance.savedContext == null
				|| !context.equals(bookPageDBInstance.savedContext)) {
			bookPageDBInstance = new BookPageDB(context);
		}
		return bookPageDBInstance;
	}

	/**
	 * Private constructor - so the singleton pattern has to be used.
	 */
	private BookPageDB(Context context) {
		dbManager = new DatabaseManager(context);
		savedContext = context;
	}

	/**
	 * Insert a new BookPage into the DB.
	 * @param cols    String array with the columns that you want to fill in.
	 * @param values  String array with the values for the specified columns.
	 * @return        id of the new entry row in the database.
	 */
	public long insertBookPage(final String[] cols, final String[] values) {
		ContentValues initialValues = new ContentValues();
		for (int i = 0; i < cols.length && i < values.length; i++) {
			initialValues.put(cols[i], values[i]);
		}

		return dbManager.getWritableDatabase().insert(TABLE_NAME, null, initialValues);
	}

	/**
	 * Update an existing BookPage in the DB.
	 * @param id      id of the entry to update.
	 * @param cols    String array with the columns that you want to edit.
	 * @param values  String array with the values for the specified columns.
	 * @return        id of the entry row in the database.
	 */
	public long updateBookPage(Long id, final String[] cols, final String[] values) {
		ContentValues editValues = new ContentValues();
		for (int i = 0; i < cols.length && i < values.length; i++) {
			editValues.put(cols[i], values[i]);
		}

		Log.d(LOG, "id: " + id);

		if (id == null) {
			Log.w(LOG, "   Warning!   BookPageDB.updateBookPage() launched with NULL id! "
					+ "{values: " + Arrays.asList(values) + "}!");
			return -1;
		} else {
			String where = COL_ROWID + " = ?";
			String[] whereArgs = new String[] {"" + id};

			return dbManager.getWritableDatabase().update(TABLE_NAME, editValues, where, whereArgs);
		}

	}

	/**
	 * Delete a BookPage entry from the db.
	 */
	public long deleteBookPage(long id) {
		String where = COL_ROWID + " = ?";
		String[] whereArgs = new String[] {"" + id};

		return dbManager.getWritableDatabase().delete(TABLE_NAME, where, whereArgs);
	}

	/**
	 * Find a BookPage entry in the database given the 3 columns that uniquely identify it.
	 * @param bookFilename
	 * @param bookTitle
	 * @param pageFilename
	 * @return  BookPage object containing the result data.
	 */
	public BookPage findBookPage(String bookFilename, String bookTitle, String pageFilename) {
		if (bookFilename == null || bookTitle == null || pageFilename == null) {
			Log.w(LOG, "   Warning!   findBookPage() was passed null variable(s).");
			return null;
		}

		String selection = COL_BOOK_FILENAME + " = ? AND " + COL_BOOK_TITLE + " = ? AND "
				+ COL_PAGE_FILENAME + " = ?";
		String[] selectionArgs = new String[] {bookFilename, bookTitle, pageFilename};

		Cursor cur = query(selection, selectionArgs, allColumns, null);
		if (cur == null) {
			return null;
		} else if (cur.getCount() > 1) {
			Log.w(LOG, "   Warning!   BookPageDB returned more than 1 results for these values "
					+ "(bookFilename: " + bookFilename + ", bookTitle: " + bookTitle
					+ ", pageFilename: " + pageFilename + ")!");
		}
		return new BookPage(cur);
	}

	/**
	 * Find the last inserted/updated BookPage entry of a given book in the database.
	 * @param bookFilename
	 * @param bookTitle
	 * @return  BookPage object containing the result data.
	 */
	public BookPage findLatestBookPage(String bookFilename, String bookTitle) {
		if (bookFilename == null || bookTitle == null) {
			Log.w(LOG, "   Warning!   findLatestBookPage() was passed null variable(s).");
			return null;
		}

		String selection = COL_BOOK_FILENAME + " = ? AND " + COL_BOOK_TITLE + " = ?";
		String[] selectionArgs = new String[] {bookFilename, bookTitle};
		String sortOrder = COL_LAST_OPENED + " DESC";

		Cursor cur = query(selection, selectionArgs, allColumns, sortOrder);
		if (cur == null) {
			return null;
		}
		return new BookPage(cur);
	}


	/**
	 * Returns the whole database table one row after another.
	 * @return			Cursor to the results
	 */
	public Cursor getAll() {
		return query(null, null, allColumns, null);
	}

	/**
	 * General database query interface.
	 */
	private Cursor query(String selection, String[] selectionArgs, String[] columns, String sortOrder) {
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		builder.setTables(TABLE_NAME);

		Cursor cursor = builder.query(dbManager.getReadableDatabase(),
				columns, selection, selectionArgs, null, null, sortOrder);

		if (cursor == null) {
			return null;
		} else if (!cursor.moveToFirst()) {
			cursor.close();
			return null;
		}
		return cursor;
	}


	/**
	 *
	 * A shell object to access the results of DB queries easier.
	 *
	 */
	public class BookPage {
		private Cursor cursor;

		public BookPage(Cursor cursor) {
			this.cursor = cursor;
		}

		public long getId() {
			return cursor.getLong(cursor.getColumnIndex(COL_ROWID));
		}

		public String getPageRelativePath() {
			return cursor.getString(cursor.getColumnIndex(COL_PAGE_RELATIVE_PATH));
		}

		public float getScrollY() {
			return cursor.getFloat(cursor.getColumnIndex(COL_SCROLL_Y));
		}

		public ScrollSyncMethod getScrollSyncMethod() {
			return ScrollSyncMethod.fromString(
					cursor.getString(cursor.getColumnIndex(COL_SCROLLSYNC_METHOD)));
		}

		public float getScrollSyncOffset() {
			return cursor.getFloat(cursor.getColumnIndex(COL_SCROLLSYNC_OFFSET));
		}

		public float getScrollSyncRatio() {
			return cursor.getFloat(cursor.getColumnIndex(COL_SCROLLSYNC_RATIO));
		}

		public int getParagraphsCount() {
			return cursor.getInt(cursor.getColumnIndex(COL_PARAGRAPHS_COUNT));
		}

		public long getLastOpened() {
			return cursor.getLong(cursor.getColumnIndex(COL_LAST_OPENED));
		}
	}
}
