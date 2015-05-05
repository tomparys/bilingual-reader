package cz.metaverse.android.bilingualreader.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

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
	private static final String LOG = "BookPageDatabaseTable";

	// The columns of the database table
	public static final String COL_ROWID = "_id";
	public static final String COL_BOOK_FILENAME = "BOOK_FILENAME";
	public static final String COL_BOOK_TITLE = "BOOK_TITLE";
	public static final String COL_PAGE_FILENAME = "PAGE_FILENAME";
	public static final String COL_PAGE_RELATIVE_PATH = "PAGE_RELATIVE_PATH";
	public static final String COL_SCROLL_Y = "SCROLL_Y";
	public static final String COL_SCROLLSYNC_OFFSET = "SCROLLSYNC_OFFSET";
	public static final String COL_PARAGRAPHS_COUNT = "PARAGRAPHS_COUNT";
	public static final String COL_LAST_OPENED = "LAST_OPENED";

	// Array of all available columns to be used for results.
	public static final String [] allColumns = new String[] {COL_ROWID,
			COL_BOOK_FILENAME, COL_BOOK_TITLE, COL_PAGE_FILENAME, COL_PAGE_RELATIVE_PATH, COL_SCROLL_Y,
			COL_SCROLLSYNC_OFFSET, COL_PARAGRAPHS_COUNT, COL_LAST_OPENED};
	public static final String [] dataColumns = new String[] {
			COL_BOOK_FILENAME, COL_BOOK_TITLE, COL_PAGE_FILENAME, COL_PAGE_RELATIVE_PATH, COL_SCROLL_Y,
			COL_SCROLLSYNC_OFFSET, COL_PARAGRAPHS_COUNT, COL_LAST_OPENED};

	// Database info
	private static final String DATABASE_NAME = "BILINGUAL_READER";
	private static final String TABLE_NAME = "BOOKPAGE";
	private static final int DATABASE_VERSION = 3;

	// The class does most of the interaction with the database.
	private final DatabaseOpenHelper dbOpenHelper;
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
		dbOpenHelper = new DatabaseOpenHelper(context);
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

		return dbOpenHelper.getWritableDatabase().insert(TABLE_NAME, null, initialValues);
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

		if (id != null) {
			String where = COL_ROWID + " = ?";
			String[] whereArgs = new String[] {"" + id};

			return dbOpenHelper.getWritableDatabase().update(TABLE_NAME, editValues, where, whereArgs);
		} else {
			return dbOpenHelper.getWritableDatabase().insert(TABLE_NAME, null, editValues);
		}

	}

	/**
	 * Delete a BookPage entry from the db.
	 */
	public long deleteBookPage(long id) {
		String where = COL_ROWID + " = ?";
		String[] whereArgs = new String[] {"" + id};

		return dbOpenHelper.getWritableDatabase().delete(TABLE_NAME, where, whereArgs);
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
			Log.w(LOG, "   Warning!   BookPageDatabaseTable returned more than 1 results for these values "
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

		Cursor cursor = builder.query(dbOpenHelper.getReadableDatabase(),
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

		public float getScrollSyncOffset() {
			return cursor.getFloat(cursor.getColumnIndex(COL_SCROLLSYNC_OFFSET));
		}

		public int getParagraphsCount() {
			return cursor.getInt(cursor.getColumnIndex(COL_PARAGRAPHS_COUNT));
		}

		public long getLastOpened() {
			return cursor.getLong(cursor.getColumnIndex(COL_LAST_OPENED));
		}
	}


	/**
	 *
	 * The SQLiteOpenHelper inner class defines abstract methods that we override so that
	 *  our database table can be created and upgraded when necessary.
	 *
	 */
	private static class DatabaseOpenHelper extends SQLiteOpenHelper {

		private SQLiteDatabase database;

		// SQL command that creates the database.
		private static final String TABLE_CREATE = "CREATE TABLE " + TABLE_NAME +" ("
				+ COL_ROWID + " integer primary key autoincrement, "
				+ COL_BOOK_FILENAME + " text not null, "
				+ COL_BOOK_TITLE + " text, "
				+ COL_PAGE_FILENAME + " text not null, "
				+ COL_PAGE_RELATIVE_PATH + " text not null, "
				+ COL_SCROLL_Y + " real, "
				+ COL_SCROLLSYNC_OFFSET + " real, "
				+ COL_PARAGRAPHS_COUNT + " integer, "
				+ COL_LAST_OPENED + " integer)";


		DatabaseOpenHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			database = db;
			database.execSQL(TABLE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(LOG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
			onCreate(db);
		}
	}
}
