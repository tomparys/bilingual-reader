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
public class DatabaseManager extends SQLiteOpenHelper {

	// Logging tag
	private static final String LOG = "DatabaseOpenHelper";

	private final Context helperContext;
	private SQLiteDatabase database;

	// Database info
	private static final String DATABASE_NAME = "BILINGUAL_READER";
	private static final int DATABASE_VERSION = 7;


	DatabaseManager(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		helperContext = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		database = db;
		database.execSQL(SRSDB.FTS_TABLE_CREATE);
		database.execSQL(BookDB.TABLE_CREATE);
		database.execSQL(BookPageDB.TABLE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(LOG, "Upgrading database from version " + oldVersion + " to "
				+ newVersion + ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS " + SRSDB.VIRTUAL_TABLE_NAME);
		db.execSQL("DROP TABLE IF EXISTS " + BookDB.TABLE_NAME);
		db.execSQL("DROP TABLE IF EXISTS " + BookPageDB.TABLE_NAME);
		onCreate(db);
	}
}