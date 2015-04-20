package cz.metaverse.android.bilingualreader.selectionwebview;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.webkit.JavascriptInterface;
import android.widget.Toast;
import cz.metaverse.android.bilingualreader.R;
import cz.metaverse.android.bilingualreader.ReaderActivity;
import cz.metaverse.android.bilingualreader.db.SRSDatabaseTable;
import cz.metaverse.android.bilingualreader.dialog.AddToSRSDialog;

/**
 *
 * Class that facilitates one-way communication between JavaScript in the displayed webpage
 * 	and our Java application.
 *
 */
public class WebAppInterface {
	Activity activity;

	WebAppInterface(Activity activity) {
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

		// TODO items need to be implemented
		switch (menuItemId) {

		case R.id.copy_menu_item:
			// Put the selected text into the clipboard
			ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
			ClipData clip = ClipData.newPlainText("simple text", selectedText);
			clipboard.setPrimaryClip(clip);
			break;

		case R.id.share_menu_item:
			Intent sharingIntent = new Intent(Intent.ACTION_SEND);
			sharingIntent.setType("text/plain");
			sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, selectedText);
			activity.startActivity(Intent.createChooser(sharingIntent,
		    		activity.getString(R.string.share_menu_headline)));
			break;

		case R.id.dictionary_menu_item:
			Toast.makeText(ReaderActivity.debugContext, "TODO Dictionary search: " + selectedText, Toast.LENGTH_SHORT).show();

			// TEMP: Trying getting the contents of the database.
			SRSDatabaseTable db = SRSDatabaseTable.getInstance(activity);
			Cursor cursor = db.getAll();

			if (cursor == null) {
				Toast.makeText(ReaderActivity.debugContext, "No results", Toast.LENGTH_SHORT).show();
			} else {
				do {
					Toast.makeText(ReaderActivity.debugContext,
							"Found: " + cursor.getString(0) + " " + cursor.getString(1),
							Toast.LENGTH_SHORT).show();
				} while (cursor.moveToNext());
			}

			break;

		case R.id.srs_menu_item:
			new AddToSRSDialog(selectedText).show(activity.getFragmentManager(), "add_to_srs_dialog");
			break;
		}
	}
}