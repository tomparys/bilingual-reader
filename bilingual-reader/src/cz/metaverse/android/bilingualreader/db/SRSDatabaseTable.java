package cz.metaverse.android.bilingualreader.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
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
public class SRSDatabaseTable {

	// For the singleton pattern - only one instance will be created.
	private static SRSDatabaseTable srsDatabaseTableInstance;

	// Logging tag
	private static final String LOG = "SRSDatabase";

	// The columns of the SRS table
	public static final String COL_WORD = "WORD";
	public static final String COL_DEFINITION = "DEFINITION";

	// The implicit rowid column that gets automatically created for the FTS table
	public static final String COL_ROWID = "rowid";
	// Column that returns the automatically generated row _id necessary for CursorLoader to operate.
	public static final String COL_HELP_ID = COL_ROWID + " AS _id";

	// Array of all available columns to be used for results.
	private static final String [] allColumns = new String[] {COL_HELP_ID, COL_WORD, COL_DEFINITION};

	// Database info
	private static final String DATABASE_NAME = "BILINGUAL-READER";
	private static final String VIRTUAL_TABLE_NAME = "SRS"; // We're creating a virtual SQLite FTS3 table
	private static final int DATABASE_VERSION = 1;

	// The class does most of the interaction with the database.
	private final DatabaseOpenHelper databaseOpenHelper;

	// The active sort order that will be applied to returned results.
	private String chosenSortOrder;

	/**
	 * Creates, if necessary, an instance of this class and returns it.
	 */
	public static SRSDatabaseTable getInstance(Context context) {
		if (srsDatabaseTableInstance == null) {
			srsDatabaseTableInstance = new SRSDatabaseTable(context);

			srsDatabaseTableInstance.sortAlphabetically(false);
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
	 * Sets whether this db class will return results sorted alphabetically or by last added.
	 */
	public void sortAlphabetically(boolean alphabetically) {
		if (alphabetically) {
			chosenSortOrder = COL_WORD + " ASC";  // Alphabetical sorting.
		} else {
			chosenSortOrder = COL_ROWID + " DESC"; // Last added item will be first.
		}
	}


	/**
	 * Adds one "SRS card" into the database table.
	 * @param word			The front of the card - the word or a phrase to be remembered
	 * @param definition	The back of the card - the word's translation or definition
	 */
	public void addCard(String word, String definition) {
		databaseOpenHelper.addCard(word, definition);
	}

	/**
	 * Edits an existing SRS card.
	 */
	public void editCard(long id, String word, String definition) {
		databaseOpenHelper.editCard(id, word, definition);
	}

	/**
	 * Deletes an SRS card.
	 */
	public void deleteCard(long id) {
		databaseOpenHelper.deleteCard(id);
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
	 * Searches the db table for words that contain the *query* prefix in the column COL_WORD.
	 * @param query		Word prefix to search for
	 * @return			Cursor to the results
	 */
	public Cursor getWordMatches(String query) {
		String selection = COL_WORD + " MATCH ?";
		String[] selectionArgs = new String[] {query + "*"};

		return query(selection, selectionArgs, allColumns);
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

		Cursor cursor = builder.query(databaseOpenHelper.getReadableDatabase(),
				columns, selection, selectionArgs, null, null, chosenSortOrder);

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
					" USING fts3 (" + COL_WORD + ", " + COL_DEFINITION + ")";


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
		public long addCard(String word, String definition) {
			ContentValues initialValues = new ContentValues();
			initialValues.put(COL_WORD, word);
			initialValues.put(COL_DEFINITION, definition);

			return getWritableDatabase().insert(VIRTUAL_TABLE_NAME, null, initialValues);
		}

		/**
		 * Updates a card in the database with new data on the card.
		 * @param id  Rowid of the card we're updating
		 */
		public long editCard(long id, String word, String definition) {
			ContentValues newValues = new ContentValues();
			newValues.put(COL_WORD, word);
			newValues.put(COL_DEFINITION, definition);

			String where = "rowid = ?";
			String[] whereArgs = new String[] {"" + id};

			return getWritableDatabase().update(VIRTUAL_TABLE_NAME, newValues, where, whereArgs);
		}

		/**
		 * Wipes a given card from the database.
		 * @param id  Rowid of the banished card
		 */
		public long deleteCard(long id) {
			String where = "rowid = ?";
			String[] whereArgs = new String[] {"" + id};

			return getWritableDatabase().delete(VIRTUAL_TABLE_NAME, where, whereArgs);
		}
	}
}
