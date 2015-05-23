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

package cz.metaverse.android.bilingualreader.helper;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import cz.metaverse.android.bilingualreader.R;

/**
*
* Helper class for important infotexts that are dispersed throughout the application,
* along with "Don't Show this again" checkbox.
*
* Here we store the different types of the infotexts, the values of "don't show again" settings,
* and match the infotexts with their messages.
*
*/
public class DontShowAgain {

	/* The various available info boxes. */
	public static final int CHAPTER_SYNC = 0;
	public static final int SCROLL_SYNC = 1;
	public static final int RECENTLY_OPENED_BOOKS = 2;
	public static final int READER_ACTIVITY = 3;
	public static final int METADATA_OR_TOC = 4;

	public static final int TEXTS_COUNT = 5;

	// Returns an empty array with the appropriate length.
	private static boolean[] getEmpty() {
		return new boolean[] {false, false, false, false, false};
	}

	// Logging
	private static final String LOG = "DontShowAgain";

	// Store of the actual values.
	private boolean[] values;


	/* Singleton pattern. */
	private static DontShowAgain selfInstance;

	public static DontShowAgain getInstance() {
		if (selfInstance == null) {
			selfInstance = new DontShowAgain();
		}
		return selfInstance;
	}

	private DontShowAgain() {}


	/**
	 * Returns corresponding info string resource.
	 */
	public static int getMessageResource(int which) {
		switch (which) {
		case RECENTLY_OPENED_BOOKS:
		default:
			return R.string.infobox_recently_opened_books;
		case READER_ACTIVITY:
			return R.string.infobox_reader_activity;
		case METADATA_OR_TOC:
			return R.string.infobox_metadata_or_toc;
		case CHAPTER_SYNC:
			return R.string.infobox_chapter_sync;
		case SCROLL_SYNC:
			return R.string.infobox_scroll_sync;
		}
	}

	/**
	 * Sets the given field into the desired position.
	 */
	public void set(int which, boolean how) {
		if (0 <= which && which < values.length) {
			Log.d(LOG, LOG + ".set(" + which + ", " + how + ")");

			values[which] = how;

			Log.d(LOG, LOG + ".set: " + values[which]);
		}
	}

	/**
	 * Returns the desired field.
	 */
	public boolean get(int which) {
		if (0 <= which && which < values.length) {
			Log.d(LOG, LOG + ".get(" + which + "): " + values[which]);
			return values[which];
		}
		return false;
	}

	/**
	 * Save state.
	 * @param editor	SharedPreferences editor instance
	 */
	public void saveState(Editor editor) {
		Log.d(LOG, LOG + ".saveState");

		for (int i = 0; i < values.length; i++) {
			editor.putBoolean("dontDisplayAgain_" + i, values[i]);
		}
	}

	/**
	 * Load state.
	 * @param preferences  SharedPreferences instance
	 * @param creatingActivity  Whether or not the activity is just being created.
	 * @return				successfulness
	 */
	public boolean loadState(SharedPreferences preferences, boolean creatingActivity) {
		Log.d(LOG, LOG + ".loadState");

		if (values == null) {
			values = getEmpty();
			for (int i = 0; i < values.length; i++) {
				values[i] = preferences.getBoolean("dontDisplayAgain_" + i, false);
			}
		}
		//values = getEmpty(); // debug: resets values
		return true;
	}
}
