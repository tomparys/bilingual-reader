package cz.metaverse.android.bilingualreader.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.text.TextUtils;
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
	public static final String COL_BOOK_FILENAME = "BOOK_FILENAME";
	public static final String COL_BOOK_TITLE = "BOOK_TITLE";
	public static final String COL_BOOK_FILE_PATH = "BOOK_FILE_PATH";
	public static final String COL_LAST_OPENED = "LAST_OPENED";

	// The implicit rowid column that gets automatically created for the FTS table
	public static final String COL_ROWID = "rowid";
	// Column that returns the automatically generated row _id necessary for CursorLoader to operate.
	public static final String COL_HELP_ID = COL_ROWID + " AS _id";

	// Array of all available columns to be used for results.
	private static final String [] allColumns = new String[] {
		COL_HELP_ID, COL_BOOK_FILENAME, COL_BOOK_TITLE, COL_BOOK_FILE_PATH, COL_LAST_OPENED};

	/* Database table */
	protected static final String VIRTUAL_TABLE_NAME = "BOOKS"; // We're creating a virtual SQLite FTS3 table
	// SQL command that creates the database.
	protected static final String FTS_TABLE_CREATE =
				"CREATE VIRTUAL TABLE " + VIRTUAL_TABLE_NAME +
				" USING fts3 (" + COL_BOOK_FILENAME + ", " + COL_BOOK_TITLE + ", " +
					COL_BOOK_FILE_PATH + ", " + COL_LAST_OPENED + ")";

	// The class does most of the interaction with the database.
	private final DatabaseOpenHelper dbOpenHelper;

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
		dbOpenHelper = new DatabaseOpenHelper(context);
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

			return dbOpenHelper.getWritableDatabase().update(VIRTUAL_TABLE_NAME, values, where, whereArgs);
		} else {
			return dbOpenHelper.getWritableDatabase().insert(VIRTUAL_TABLE_NAME, null, values);
		}
	}

	public Long findBookId(String filename, String title) {
		if (filename == null || title == null) {
			Log.w(LOG, "   Warning!   findBook() was passed null variable(s).");
			return null;
		}

		String selection = VIRTUAL_TABLE_NAME + " MATCH ?";
		String match = COL_BOOK_FILENAME + ": \"" + filename + "\" " + COL_BOOK_TITLE + ": \"" + title + "\"";
		String[] selectionArgs = new String[] {match};

		Log.d(LOG, LOG + ".findBookId: match: " + match);

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
	 * Deletes an SRS card.
	 */
	public long deleteCard(long id) {
		String where = COL_ROWID + " = ?";
		String[] whereArgs = new String[] {"" + id};

		return dbOpenHelper.getWritableDatabase().delete(VIRTUAL_TABLE_NAME, where, whereArgs);
	}

	/**
	 * Deletes several SRS cards at once.
	 */
	public void deleteCards(long[] ids) {
		for (long id : ids) {
			deleteCard(id);
		}
	}

	/**
	 * 1. Splits the query into *words* separated by whitespace.
	 * 2. Searches the database for any row that contains words that contain *words* as prefixes.
	 *
	 * E.g. query = "alb hu" will match a row that has
	 *  "Mighty Albrecht" in one column and "Happy Hugo" in another.
	 *
	 * @param query		List of word-prefixes to be searched for.
	 * @return			Cursor to the results
	 */
	public Cursor getMatches(String query) {
		String[] words = query.split("\\s+");
		String match = TextUtils.join("* ", words) + "*";

		String selection = VIRTUAL_TABLE_NAME + " MATCH ?";
		String[] selectionArgs = new String[] {match};

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
		builder.setTables(VIRTUAL_TABLE_NAME);

		Cursor cursor = builder.query(dbOpenHelper.getReadableDatabase(),
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
