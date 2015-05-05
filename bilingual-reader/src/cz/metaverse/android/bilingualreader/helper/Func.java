package cz.metaverse.android.bilingualreader.helper;

import java.io.File;

import android.content.res.Resources;
import android.os.Environment;
import android.util.TypedValue;

/**
 *
 * Helper class with static methods for various usage.
 *
 */
public class Func {

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
