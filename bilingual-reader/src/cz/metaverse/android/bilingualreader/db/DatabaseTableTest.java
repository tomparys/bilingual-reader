package cz.metaverse.android.bilingualreader.db;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import cz.metaverse.android.bilingualreader.db.BookPageDB.BookPage;

/**
 *
 * This class was used for debugging purposes.
 * I'm leaving it here in case some changes to the database need to be debugged in the future.
 *
 */
public class DatabaseTableTest {

	// Logging tag
	private static final String LOG = "DatabaseTableTest";

	public static void testBookPageDatabaseTable(Context context) {
		String[] test = new String[] {"Beard Facts", "Ways To Get", "The Science of Dogs"};

		BookPageDB bookPageDbT = BookPageDB.getInstance(context);
		String[] cols = BookPageDB.dataColumns;
		String[] values = new String[] {"book hugo", "title alfons", "page Pathy",
				"600", "300", "15", "" + System.currentTimeMillis()};
		Log.d(LOG, "New row added: " + bookPageDbT.insertBookPage(cols, values));

		values = new String[] {"comparison", "operators", "including",
				"800", "600", "30", "" + System.currentTimeMillis()};
		Log.d(LOG, "New row added: " + bookPageDbT.insertBookPage(cols, values));

		values = new String[] {test[0], test[1], test[2],
				"999", "666", "33", "" + System.currentTimeMillis()};
		Log.d(LOG, "New row added: " + bookPageDbT.insertBookPage(cols, values));

		values = new String[] {"you’re passing in a static", "name and a", "primary key",
				"100", "150", "20", "" + System.currentTimeMillis()};
		Log.d(LOG, "New row added: " + bookPageDbT.insertBookPage(cols, values));


		Cursor res = bookPageDbT.getAll();
		if (res == null) {
			Log.d(LOG, "getAll() result null");
		} else {
			do {
				Log.d(LOG, "getAll() result: " + res.getInt(0) + ", " + res.getFloat(4));
			} while (res.moveToNext());
		}

		BookPage bp = bookPageDbT.findBookPage(test[0], test[1], test[2]);
		if (res == null) {
			Log.d(LOG, "findBookPage() result null");
		} else {
			Log.d(LOG, "findBookPage(): " + bp.getId() + ", " +  + bp.getScrollY() + ", "
					+ bp.getScrollSyncOffset() + ", " + bp.getParagraphsCount() + ", "
					+ bp.getLastOpened() + ".");
		}
	}
}
