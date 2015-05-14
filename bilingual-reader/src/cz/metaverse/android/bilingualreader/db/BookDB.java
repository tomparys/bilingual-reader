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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

/**
 *
 * Class that abstracts our SRS database table.
 * Uses the singleton pattern - use static method getInstance().
 *
 * This SQLite database table uses the FTS3 Extension to facilitate faster Full-Text Search.
 * 	More info at: www.sqlite.org/fts3.html
 *
 */
public class BookDB {

	// For the singleton pattern - only one instance will be created.
	private static BookDB bookDBInstance;

	// Logging tag
	private static final String LOG = "BookDB";

	// The columns of the SRS table
	public static final String COL_ROWID = "_id";
	public static final String COL_BOOK_FILENAME = "BOOK_FILENAME";
	public static final String COL_BOOK_TITLE = "BOOK_TITLE";
	public static final String COL_BOOK_FILE_PATH = "BOOK_FILE_PATH";
	public static final String COL_LAST_OPENED = "LAST_OPENED";

	// Array of all available columns to be used for results.
	private static final String [] allColumns = new String[] {
		COL_ROWID, COL_BOOK_FILENAME, COL_BOOK_TITLE, COL_BOOK_FILE_PATH, COL_LAST_OPENED};

	/* Database table */
	protected static final String TABLE_NAME = "BOOKS"; // We're creating a virtual SQLite FTS3 table
	// SQL command that creates the database.
	protected static final String TABLE_CREATE = "CREATE TABLE " + TABLE_NAME +" ("
			+ COL_ROWID + " integer primary key autoincrement, "
			+ COL_BOOK_FILENAME + " text not null, "
			+ COL_BOOK_TITLE + " text, "
			+ COL_BOOK_FILE_PATH + " text, "
			+ COL_LAST_OPENED + " integer)";

	// The class does most of the interaction with the database.
	private final DatabaseManager dbManager;

	// The active sort order that will be applied to returned results.
	private String chosenSortOrder;

	/**
	 * Creates, if necessary, an instance of this class and returns it.
	 */
	public static BookDB getInstance(Context context) {
		if (bookDBInstance == null) {
			bookDBInstance = new BookDB(context);

			bookDBInstance.sortAlphabetically(false);
		}
		return bookDBInstance;
	}

	/**
	 * Private constructor - so the singleton pattern has to be used.
	 */
	private BookDB(Context context) {
		dbManager = new DatabaseManager(context);
	}

	/**
	 * Sets whether this db class will return results sorted alphabetically or by last added.
	 */
	public void sortAlphabetically(boolean alphabetically) {
		if (alphabetically) {
			chosenSortOrder = COL_BOOK_TITLE + " ASC";  // Alphabetical sorting.
		} else {
			chosenSortOrder = COL_LAST_OPENED + " DESC"; // Last added item will be first.
		}
	}


	public long replaceBook(String filename, String title, String filePath, long timestamp) {
		ContentValues values = new ContentValues();
		values.put(COL_BOOK_FILENAME, filename);
		values.put(COL_BOOK_TITLE, title);
		values.put(COL_BOOK_FILE_PATH, filePath);
		values.put(COL_LAST_OPENED, timestamp);

		Long bookId = findBookId(filename, title);
		if (bookId != null) {
			String where = COL_ROWID + " = ?";
			String[] whereArgs = new String[] {"" + bookId};

			return dbManager.getWritableDatabase().update(TABLE_NAME, values, where, whereArgs);
		} else {
			return dbManager.getWritableDatabase().insert(TABLE_NAME, null, values);
		}
	}

	public Long findBookId(String filename, String title) {
		if (filename == null || title == null) {
			Log.w(LOG, "   Warning!   findBook() was passed null variable(s).");
			return null;
		}

		String selection = COL_BOOK_FILENAME + " = ? AND " + COL_BOOK_TITLE + " = ?";
		String[] selectionArgs = new String[] {filename, title};

		Log.d(LOG, LOG + ".findBookId: selectionArgs: " + selectionArgs[0] + "; " + selectionArgs[1]);

		Cursor cur = query(selection, selectionArgs, allColumns);
		if (cur == null) {
			return null;
		} else if (cur.getCount() > 1) {
			Log.w(LOG, "   Warning!   BookDB returned more than 1 results for these values "
					+ "(filename: " + filename + ", title: " + title + ")!");
		}

		// Return the ID
		return cur.getLong(0);
	}

	/**
	 * Deletes a book entry from DB.
	 */
	public long deleteCard(long id) {
		String where = COL_ROWID + " = ?";
		String[] whereArgs = new String[] {"" + id};

		return dbManager.getWritableDatabase().delete(TABLE_NAME, where, whereArgs);
	}

	/**
	 * Deletes several book entries from DB.
	 */
	public void deleteCards(long[] ids) {
		for (long id : ids) {
			deleteCard(id);
		}
	}

	/**
	 * Searches for a given text in columns bookTitle and bookFilename.
	 *
	 * @param query		String to be searched for.
	 * @return			Cursor to the results
	 */
	public Cursor getMatches(String query) {
		String selection = COL_BOOK_TITLE + " LIKE ? OR " + COL_BOOK_FILENAME + " LIKE ?";
		String[] selectionArgs = new String[] {"%" + query + "%", "%" + query + "%"};

		return query(selection, selectionArgs, allColumns);
	}

	/**
	 * Returns the whole database table one row after another.
	 * @return			Cursor to the results
	 */
	public Cursor getAll() {
		return query(null, null, allColumns);
	}

	/**
	 * General database query interface.
	 */
	private Cursor query(String selection, String[] selectionArgs, String[] columns) {
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		builder.setTables(TABLE_NAME);

		Cursor cursor = builder.query(dbManager.getReadableDatabase(),
				columns, selection, selectionArgs, null, null, chosenSortOrder);

		if (cursor == null) {
			return null;
		} else if (!cursor.moveToFirst()) {
			cursor.close();
			return null;
		}
		return cursor;
	}
}
