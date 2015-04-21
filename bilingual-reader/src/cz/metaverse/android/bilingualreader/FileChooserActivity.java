/*
The MIT License (MIT)

Copyright (c) 2013, V. Giacometti, M. Giuriato, B. Petrantuono

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

package cz.metaverse.android.bilingualreader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 *
 * Activity which offers a list of epubs the user can open.
 *
 */
public class FileChooserActivity extends Activity {

	static List<File> epubs;
	static List<String> names;
	ArrayAdapter<String> arrayAdapter;
	static File selected;
	boolean firstTime;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.file_chooser_layout);

		// Sets the app icon as clickable.
		getActionBar().setDisplayHomeAsUpEnabled(true);

		// If either names or epubs isn't initialized, create empty lists for them.
		boolean populateList = false;
		if (names == null || epubs == null) {
			names = new ArrayList<String>();
			epubs = new ArrayList<File>();
			populateList = true;
		}

		// Prepare the ListView for data
		ListView list = (ListView) findViewById(R.id.fileListView);
		arrayAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, names);

		// On click listener
		list.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> listView, View itemView,
					int position, long itemId) {

				// End this Activity by sending the Intent result with the epub absolute path.
				selected = epubs.get(position);
				Intent resultIntent = new Intent();
				resultIntent.putExtra("bpath", selected.getAbsolutePath());
				setResult(Activity.RESULT_OK, resultIntent);
				finish();
			}
		});

		// Activate the list
		list.setAdapter(arrayAdapter);

		// Populate list of epubs if needed
		if (epubs == null || epubs.size() == 0 || populateList) {
			new FindEpubsTask().execute(getEpubSearchDirectory());
		}
	}

	/**
	 * @return Directory file where to search for epubs.
	 */
	private File getEpubSearchDirectory() {
		// Opens the so far hard-coded directory where to look for epubs.
		String root = Environment.getExternalStorageDirectory().toString();
		File ebooksDir = new File(root+getString(R.string.epubs_search_directory));

		if(ebooksDir.exists()) {
			return ebooksDir;
		} else {
			// If the directory doesn't exist, return the root dir.
			return Environment.getExternalStorageDirectory() ;
		}
	}

	/**
	 * Displays one more book into the list.
	 * @param newEpub new epub file to display
	 */
	public void addEpub(File newEpub) {
		epubs.add(newEpub);
		names.add(fileName(newEpub));
		arrayAdapter.notifyDataSetChanged();
	}


	/**
	 * Returns file name of a file
	 * @param file
	 * @return file name
	 */
	private String fileName(File file) {
		return file.getName().replace(".epub", "");
	}

	/**
	 * Refreshes the epub list
	 */
	private void refreshList() {
		names.clear();
		epubs.clear();

		new FindEpubsTask().execute(getEpubSearchDirectory());
	}

	/**
	 * Opens menu in case the user clicks it.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.file_chooser, menu);
		return true;
	}

	/**
	 * Menu item selected.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		// The App icon in upper left corner
		case android.R.id.home:
			finish();
			return true;

		// The update icon/menu item
		case R.id.update:
			refreshList();
			return true;

		default:
			return super.onOptionsItemSelected(item);

		}
	}


	/**
	 *
	 * AsyncTask instance for loading epubs in the background while progress dialog circles in foreground.
	 *
	 */
	private class FindEpubsTask extends AsyncTask<File, File, Boolean> {
		private ProgressDialog progressDialog;

		@Override
		protected void onPreExecute() {
			// Start ProgressDialog
			progressDialog = ProgressDialog.show(FileChooserActivity.this, "",
					FileChooserActivity.this.getText(R.string.searching_for_epubs), true, false);

		};

		@Override
	    protected Boolean doInBackground(File... directory) {
			// Start computing the list in the background.
	    	epubList(directory[0]);
	    	return true;
	    }

		/**
		 * Recursively returns a lsit of epub files in a given dir.
		 * @param dir to search
		 * @return list of epub Files
		 */
		private List<File> epubList(File dir) {
			// Check if the AsyncTask has been cancelled, if so, end.
			if (isCancelled()) {
				return null;
			}

			List<File> res = new ArrayList<File>();
			if (dir.isDirectory()) {
				File[] f = dir.listFiles();
				if (f != null) {
					for (int i = 0; i < f.length; i++) {
						if (f[i].isDirectory()) {
							// Directory: Recursive call with the new directory as parameter.
							res.addAll(epubList(f[i]));
						} else {
							// File: check if it's .epub, if so, add to results.
							// 	TODO: check with mimetype, not with filename extension
							String lowerCasedName = f[i].getName().toLowerCase();
							if (lowerCasedName.endsWith(".epub")) {
								res.add(f[i]);
								// Send this file back to the UI thread to be displayed.
								publishProgress(f[i]);
							}
						}
					}
				}
			}
			return res;
		}

		@Override
	    protected void onProgressUpdate(File... file) {
			// Send this file back to the UI thread to be displayed.
			addEpub(file[0]);
	    }

		@Override
	    protected void onPostExecute(Boolean result) {
	        // Stop the ProgressDialog
			progressDialog.dismiss();
	    }

	}
}
