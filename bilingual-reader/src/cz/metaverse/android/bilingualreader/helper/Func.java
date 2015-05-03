package cz.metaverse.android.bilingualreader.helper;

import java.io.File;

import android.os.Environment;

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

		private static final String EPUBS_UNPACK_DIR = "/BilingualReader/epubtemp/";


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
			return null;
		}
	}

}
