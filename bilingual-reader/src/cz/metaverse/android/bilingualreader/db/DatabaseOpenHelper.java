package cz.metaverse.android.bilingualreader.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
*
* The SQLiteOpenHelper inner class defines abstract methods that we override so that
*  our database table can be created and upgraded when necessary.
*
*/
public class DatabaseOpenHelper extends SQLiteOpenHelper {

	// Logging tag
	private static final String LOG = "DatabaseOpenHelper";

	private final Context helperContext;
	private SQLiteDatabase database;

	// Database info
	private static final String DATABASE_NAME = "BILINGUAL_READER";
	private static final int DATABASE_VERSION = 5;


	DatabaseOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		helperContext = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		database = db;
		database.execSQL(SRSDatabaseTable.FTS_TABLE_CREATE);
		database.execSQL(BookDB.FTS_TABLE_CREATE);
		database.execSQL(BookPageDB.TABLE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(LOG, "Upgrading database from version " + oldVersion + " to "
				+ newVersion + ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS " + SRSDatabaseTable.VIRTUAL_TABLE_NAME);
		db.execSQL("DROP TABLE IF EXISTS " + BookDB.VIRTUAL_TABLE_NAME);
		db.execSQL("DROP TABLE IF EXISTS " + BookPageDB.TABLE_NAME);
		onCreate(db);
	}
}