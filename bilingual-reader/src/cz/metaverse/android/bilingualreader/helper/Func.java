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

import java.io.File;

import android.content.res.Resources;
import android.os.Environment;
import android.util.Log;
import android.util.TypedValue;

/**
 *
 * Helper class with static methods for various usage.
 *
 */
public class Func {

	private static final String LOG = "Func";

	/**
	 *
	 * Static class for obtaining important filesystem paths and directories.
	 *
	 */
	public static class Paths {

		private static final String EPUBS_SEARCH_DIR = "/_Tom/dip"; // TODO Change

		private static final String EPUBS_UNPACK_DIR = "/BilingualReader/epubtemp/"; // Trailing / needed

		private static final String EPUBS_SRS_EXPORT_DIR = "/BilingualReader/SRS-export";


		/**
		 * @return Directory file where to search for epubs in FileChooserActivity.
		 */
		public static File getEpubsSearchDir() {
			String root = Environment.getExternalStorageDirectory().toString();
			File ebooksDir = new File(root + EPUBS_SEARCH_DIR);

			if(ebooksDir.exists()) {
				return ebooksDir;
			} else {
				// If the directory doesn't exist, search the whole root directory.
				return Environment.getExternalStorageDirectory() ;
			}
		}

		/**
		 * @return Directory where the Epub class unpacks the ebooks.
		 */
		public static String getEpubsUnpackDir() {
			return Environment.getExternalStorageDirectory() + EPUBS_UNPACK_DIR;
		}

		/**
		 * @return Directory where the application exports SRS entries.
		 */
		public static String getSRSExportDir() {
			return Environment.getExternalStorageDirectory() + EPUBS_SRS_EXPORT_DIR;
		}
	}


	/**
	 * Converts device independent pixels (dp) to pixels (px).
	 * @param resources  Context.getResources() result.
	 * @param deviceIndependentPixels  Length in dp's
	 * @return  Length in pixels
	 */
	public static int dpToPix(Resources resources, int deviceIndependentPixels) {
		return Math.round(TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, deviceIndependentPixels, resources.getDisplayMetrics()));
	}

	/**
	 * Returns the closest number to the *value* in a range (min, max).
	 */
	public static int minMaxRange(int min, int value, int max) {
		if (value < min) {
			value = min;
		} else if (value > max) {
			value = max;
		}
		return value;
	}

	/**
	 * Returns filename from given file-path. I.e. removes everything before the last '/'.
	 */
	public static String fileNameFromPath(String filePath) {
		if (filePath == null) {
			return null;
		}

		// Remove everything before the last '/'
		String filename = filePath.substring(filePath.lastIndexOf("/") + 1);

		// Remove everything after and including the first '#' (AKA the Fragment Identifier).
		filename = removeFragmentIdentifier(filename);

		Log.d(LOG, LOG + ".fileNameFromPath: filePath: " + filePath);
		Log.d(LOG, LOG + ".fileNameFromPath: filename:" + filename);

		return filename;
	}

	/**
	 * Removes the Fragment Identifier, i.e. anything after and including the # symbol.
	 */
	public static String removeFragmentIdentifier(String filePath) {
		if (filePath == null) {
			return null;
		}

		// Remove everything after and including the first '#' (AKA the Fragment Identifier).
		int pos = filePath.indexOf("#");
		if (pos != -1) {
			filePath = filePath.substring(0, pos);
		}
		return filePath;
	}

}
