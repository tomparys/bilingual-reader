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
 * Class that abstracts our SRS database table.
 * Uses the singleton pattern - use static method getInstance().
 *
 */
public class SRSDatabaseTable {

	// For the singleton pattern - only one instance will be created.
	private static SRSDatabaseTable srsDatabaseTableInstance;

	// Logging tag
	private static final String LOG = "SRSDatabase";

	// The columns of the SRS table
	public static final String COL_WORD = "WORD";
	public static final String COL_TRANSLATION = "TRANSLATION";

	// Database info
	private static final String DATABASE_NAME = "BILINGUAL-READER";
	private static final String VIRTUAL_TABLE_NAME = "SRS"; // Full-Text Search table
	private static final int DATABASE_VERSION = 1;

	private final DatabaseOpenHelper databaseOpenHelper;

	/**
	 * Creates, if necessary, an instance of this class and returns it.
	 */
	public static SRSDatabaseTable getInstance(Context context) {
		if (srsDatabaseTableInstance == null) {
			srsDatabaseTableInstance = new SRSDatabaseTable(context);
		}
		return srsDatabaseTableInstance;
	}

	/**
	 * Private constructor - so the singleton pattern has to be used.
	 */
	private SRSDatabaseTable(Context context) {
		databaseOpenHelper = new DatabaseOpenHelper(context);
	}

	/**
	 * Adds one "SRS card" into the database table.
	 * @param word			The front of the card - the word or a phrase to be remembered
	 * @param definition	The back of the card - the word's translation or definition
	 */
	public void addWord(String word, String definition) {
		databaseOpenHelper.addWord(word, definition);
	}

	/**
	 * Searches the db table for matches with data in given columns.
	 * @param query		The beginning of the word user searches
	 * @param columns	Columns to be searched
	 * @return			Cursor to the results
	 */
	public Cursor getWordMatches(String query, String[] columns) {
	    String selection = COL_WORD + " MATCH ?";
	    String[] selectionArgs = new String[] {query + "*"};

	    return query(selection, selectionArgs, columns);
	}

	/**
	 * Returns the whole database table one row after another.
	 * @return			Cursor to the results
	 */
	public Cursor getAll() {
		return query(null, null, null);
	}

	/**
	 * General database query interface.
	 */
	private Cursor query(String selection, String[] selectionArgs, String[] columns) {
	    SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
	    builder.setTables(VIRTUAL_TABLE_NAME);

	    Cursor cursor = builder.query(databaseOpenHelper.getReadableDatabase(),
	            columns, selection, selectionArgs, null, null, null);

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
	 * The SQLiteOpenHelper inner class defines abstract methods that we override so that
	 *  our database table can be created and upgraded when necessary.
	 *
	 */
	private static class DatabaseOpenHelper extends SQLiteOpenHelper {

		private final Context helperContext;
		private SQLiteDatabase database;

		// SQL command that creates the database.
		private static final String FTS_TABLE_CREATE =
					"CREATE VIRTUAL TABLE " + VIRTUAL_TABLE_NAME +
					" USING fts3 (" + COL_WORD + ", " + COL_TRANSLATION + ")";


		DatabaseOpenHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			helperContext = context;
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			database = db;
			database.execSQL(FTS_TABLE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(LOG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + VIRTUAL_TABLE_NAME);
			onCreate(db);
		}

		/**
		 * Adds one "SRS card" into the database table.
		 * @param word			The front of the card - the word or a phrase to be remembered
		 * @param definition	The back of the card - the word's translation or definition
		 * @return				Id of the newly entered row in the table
		 */
		public long addWord(String word, String definition) {
		    ContentValues initialValues = new ContentValues();
		    initialValues.put(COL_WORD, word);
		    initialValues.put(COL_TRANSLATION, definition);

		    return getWritableDatabase().insert(VIRTUAL_TABLE_NAME, null, initialValues);
		}
	}
}
