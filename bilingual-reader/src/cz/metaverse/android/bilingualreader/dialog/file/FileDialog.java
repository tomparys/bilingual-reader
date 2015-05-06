/*
COPYRIGHT

This file contains code taken from the Android-File-Dialog project, (c) twig, that can be found at:
    https://github.com/twig/Android-File-Dialog

Released under the BSD 3-Clause License under the personal permission of the author:
    https://github.com/twig/Android-File-Dialog/issues/1
    http://opensource.org/licenses/BSD-3-Clause
 */

package cz.metaverse.android.bilingualreader.dialog.file;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import cz.metaverse.android.bilingualreader.R;
import cz.metaverse.android.bilingualreader.RecentlyOpenedFilesActivity;

public class FileDialog extends DialogFragment {

	private static final String LOG = "FileDialog";

	private static final String ITEM_KEY = "key";
	private static final String ITEM_IMAGE = "image";

	public static final String PATH_ROOT = "/";
	public static final String PATH_SDCARD = Environment.getExternalStorageDirectory().getAbsolutePath();

	private FileDialogOptions options;

    // Option for filtering files by extension
    private static final String FILTER_FILES_BY_EXTENSION = "epub";

	// TODO: This needs a cleanup
	private AlertDialog dialog;
	private ListView listview;

	private List<String> path;

	private String parentPath;
	private String currentPath = PATH_ROOT;

	private File selectedFile;
	private final HashMap<String, Integer> lastPositions = new HashMap<String, Integer>();

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setRetainInstance(true); -- bug in the API, this closes the dialog upon orientation change.

		// Restore the state in case of an orientation change.
		//   -- no need to restore it, android doesn't destroy the Dialog upon orientation change.
		/*if (savedInstanceState != null) {
			this.currentPath = savedInstanceState.getString("currentPath");
			listview.onRestoreInstanceState(savedInstanceState.getParcelable("listview"));
		}*/

		RelativeLayout layout = (RelativeLayout) getActivity().getLayoutInflater().inflate(R.layout.file_dialog_main, null);

		listview = (ListView) layout.findViewById(android.R.id.list);
		listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> lv, View v, int pos, long id) {
				handleListItemClick(v, pos, id);
			}
		});


		// Read options
		options = new FileDialogOptions();

		// Load the last path from preferences so we open the dialog in same directory.
		SharedPreferences preferences = getActivity().getPreferences(Context.MODE_PRIVATE);
		String lastPath = preferences.getString(getString(R.string.pref_file_dialog_last_path), null);
		if (lastPath != null && !lastPath.isEmpty()) {
			options.currentPath = lastPath;
		}

		dialog = new AlertDialog.Builder(getActivity())
			.setTitle("Select file")
			.setView(layout)
			.setCancelable(false)
			.setOnKeyListener(new DialogInterface.OnKeyListener() {
				@Override
				public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
					if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
						if (!currentPath.equals(PATH_ROOT) && !currentPath.equals(PATH_SDCARD)) {
							getDir(parentPath);
						}
						else {
							dismiss();
						}
					}

					return true;
				}
			})
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override public void onClick(DialogInterface dialog, int which) {
					if (options.selectFolderMode) {
						// TODO me: maybe you should return the dir here? Well, I'm not using this anyhow.
						returnSelection(null);
						return;
					}

					dismiss();
				}
			})
			.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
				@Override public void onClick(DialogInterface dialog, int which) {
					dismiss();
				}
			})
			.create();



		// Try to restore current path after screen rotation
		if (savedInstanceState != null) {
		    getDir(savedInstanceState.getString("currentPath"));
		    // TODO: restore scroll position also
		}
		// New instance of FileDialog
		else {
		    File file = new File(options.currentPath);

		    if (file.isDirectory() && file.exists()) {
		        getDir(options.currentPath);
		    }
		    else {
		        getDir(PATH_SDCARD);
		    }
		}


		dialog.setTitle(replaceSDCardPath(currentPath));

		return dialog;
	}


	private void getDir(String dirPath) {

		boolean useAutoSelection = dirPath.length() < currentPath.length();

		Integer position = lastPositions.get(parentPath);

		getDirImpl(dirPath);

		if (position != null && useAutoSelection) {
			listview.setSelection(position);
		}

	}

	private void getDirImpl(final String dirPath) {
		currentPath = dirPath;

		path = new ArrayList<String>();
		ArrayList<HashMap<String, Object>> mList = new ArrayList<HashMap<String, Object>>();

		File f = new File(currentPath);
		File[] files = f.listFiles();

		// Null if file is not a directory
		if (files == null) {
			currentPath = PATH_SDCARD;
			f = new File(currentPath);
			files = f.listFiles();
		}

		// Sort files by alphabet and ignore casing
		Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File lhs, File rhs) {
                return lhs.getPath().compareToIgnoreCase(rhs.getPath());
            }
		});

	    dialog.setTitle(replaceSDCardPath(currentPath));

		// Add the "Back to SD card" line.
        if (!currentPath.equals(PATH_SDCARD)) {
            boolean mounted = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);

            if (mounted) {
                addItem(mList, getString(R.string.Back_to_SD_card), this.options.iconSDCard);
                path.add(PATH_SDCARD);
            }
        }

        // Add the ".." line
		if (!currentPath.equals(PATH_ROOT)) {
			addItem(mList, getString(R.string.parent_folder_line), this.options.iconUp);
			path.add(f.getParent());
			parentPath = f.getParent();
		}

		ArrayList<File> listDirs = new ArrayList<File>();
		ArrayList<File> listFiles = new ArrayList<File>();

		for (File file : files) {
			if (file.isDirectory()) {
				listDirs.add(file);
			}
			// Only add files if we're not in folder mode
			else if (!options.selectFolderMode) {

				// Filter files by extension
				if (FILTER_FILES_BY_EXTENSION != null) {
					String filePath = file.getPath();
					int extPosition = filePath.lastIndexOf(".");
					if (extPosition != -1) {
						String extension = filePath.substring(extPosition + 1).toLowerCase(Locale.US);
						//Log.d(LOG, LOG + "; extension: " + extension);

						if (extension != null && extension.equals(FILTER_FILES_BY_EXTENSION)) {
							listFiles.add(file);
						}
					}

				} else {
					listFiles.add(file);
				}
			}
		}

		// Add directories
		for (File dir : listDirs) {
		    path.add(dir.getPath());
			addItem(mList, dir.getName(), this.options.iconFolder);
		}

		// Add files
		for (File file : listFiles) {
		    path.add(file.getPath());
			addItem(mList, file.getName(), this.options.iconFile);
		}

		SimpleAdapter fileList = new SimpleAdapter(getActivity(), mList,
            R.layout.file_dialog_row,
            new String[] { ITEM_KEY, ITEM_IMAGE },
            new int[] { R.id.fdrowtext, R.id.fdrowimage }
        );

		fileList.notifyDataSetChanged();

		listview.setAdapter(fileList);
	}

	/**
	 * If the path contains the path of the SDcard, replace it with the string SDcard.
	 */
	private CharSequence replaceSDCardPath(String currentPath) {
		return currentPath.replace(PATH_SDCARD, getActivity().getString(R.string.SD_card));
	}


	private void addItem(ArrayList<HashMap<String, Object>> mList, String fileName, int imageId) {
		HashMap<String, Object> item = new HashMap<String, Object>();
		item.put(ITEM_KEY, fileName);
		item.put(ITEM_IMAGE, imageId);
		mList.add(item);
	}


	protected void handleListItemClick(View v, int position, long id) {
		File file = new File(path.get(position));

		if (!file.exists()) {
		    new AlertDialog.Builder(getActivity())
                .setTitle("Does not exist.")
                .setMessage(file.getName())
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
		    return;
		}

		if (file.isDirectory()) {
			if (file.canRead()) {
			    // Save the scroll position so users don't get confused when they come back
				lastPositions.put(currentPath, listview.getFirstVisiblePosition());
				getDir(path.get(position));
			}
			else {
				new AlertDialog.Builder(getActivity())
					.setTitle("[" + file.getName() + "] " + getText(R.string.cant_read_folder))
					.setPositiveButton("OK", null)
					.show();
			}
		}
		else {
			selectedFile = file;
			v.setSelected(true);

			if (selectedFile != null) {
			    returnSelection(selectedFile.getPath());
			}
		}
	}

	/**
	 * This is where we do the thing we want with the selected file.
	 * @param filePath
	 */
	private void returnSelection(String filePath) {
	    this.options.currentPath = currentPath;
	    this.options.selectedFile = filePath;

		// Save the current path to preferences so next time we open the dialog in same directory.
		Editor prefEditor = getActivity().getPreferences(Context.MODE_PRIVATE).edit();
		prefEditor.putString(getString(R.string.pref_file_dialog_last_path), currentPath);
		prefEditor.commit();

		// Send the filePath back to the activity.
	    ((RecentlyOpenedFilesActivity) getActivity()).returnPathAndFinish(filePath);

	    dismiss();
	}

	/**
	 * Remember the information when the screen is just about to be rotated.
	 */
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putString("currentPath", this.currentPath);
		outState.putParcelable("listview", listview.onSaveInstanceState());
	}
}