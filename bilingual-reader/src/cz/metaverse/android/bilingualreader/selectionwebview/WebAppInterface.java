package cz.metaverse.android.bilingualreader.selectionwebview;

import cz.metaverse.android.bilingualreader.R;
import cz.metaverse.android.bilingualreader.ReaderActivity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

/**
 * 
 * Class that facilitates one-way communication between JavaScript in the displayed webpage
 * 	and our Java application.
 *
 */
public class WebAppInterface {
	Context mContext;
	
	WebAppInterface(Context c) {
		mContext = c;
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
			ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
			ClipData clip = ClipData.newPlainText("simple text", selectedText);
			clipboard.setPrimaryClip(clip);
			break;
			
		case R.id.share_menu_item:
			Toast.makeText(ReaderActivity.debugContext, "TODO share: " + selectedText, Toast.LENGTH_SHORT).show();
			break;
			
		case R.id.dictionary_menu_item:
			Toast.makeText(ReaderActivity.debugContext, "TODO Dictionary search: " + selectedText, Toast.LENGTH_SHORT).show();
			break;
			
		case R.id.srs_menu_item:
			Toast.makeText(ReaderActivity.debugContext, "TODO Add to SRS: " + selectedText, Toast.LENGTH_SHORT).show();
			break;
		}
	}
}