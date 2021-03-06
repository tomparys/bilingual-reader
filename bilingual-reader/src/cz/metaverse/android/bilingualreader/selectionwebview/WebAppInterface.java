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

package cz.metaverse.android.bilingualreader.selectionwebview;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;
import cz.metaverse.android.bilingualreader.R;
import cz.metaverse.android.bilingualreader.ReaderActivity;
import cz.metaverse.android.bilingualreader.dialog.AddToSRSDialog;
import cz.metaverse.android.bilingualreader.helper.Dictionary;

/**
 *
 * Class that facilitates one-way communication between JavaScript in the displayed webpage
 * 	and our Java application.
 *
 */
public class WebAppInterface {

	private static final String LOG = "WebAppInterface";

	ReaderActivity activity;

	WebAppInterface(ReaderActivity activity) {
		this.activity = activity;
	}

	/**
	 * The method JavaScript calls and sends it the text the user has selected
	 * 	in our custom WebView (SelectionWebView) along with the menuItemId that the user has clicked.
	 *
	 * @param menuItemId	Id of the menu item on the ActionMode bar the user has clicked
	 * @param selectedText	The text the user has selected in SelectionWebView
	 */
	@JavascriptInterface
	public void receiveText(int menuItemId, String selectedText) {
		switch (menuItemId) {

		// Copy
		case R.id.copy_menu_item:
			// Put the selected text into the clipboard
			ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
			ClipData clip = ClipData.newPlainText("simple text", selectedText);
			clipboard.setPrimaryClip(clip);
			Toast.makeText(activity, R.string.Copied, Toast.LENGTH_SHORT).show();
			break;

		// Share
		case R.id.share_menu_item:
			Intent sharingIntent = new Intent(Intent.ACTION_SEND);
			sharingIntent.setType("text/plain");
			sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, selectedText);
			activity.startActivity(Intent.createChooser(sharingIntent,
		    		activity.getString(R.string.share_menu_headline)));
			break;

		// Open dictionary
		case R.id.dictionary_menu_item:
			Log.d(LOG, LOG + " - Dictionary menu item clicked.");

			if (!Dictionary.openDefault(activity, selectedText)) {
				Log.d(LOG, LOG + " - No default dictionary found.");

				Toast.makeText(activity, R.string.Set_default_dictionary, Toast.LENGTH_SHORT).show();
				activity.openSettings();
			}
			break;

		// Add to SRS
		case R.id.srs_menu_item:
			new AddToSRSDialog(selectedText).show(activity.getFragmentManager(), "add_to_srs_dialog");
			break;

		// Settings
		case R.id.settings_menu_item:
			activity.openSettings();
			break;
		}
	}
}