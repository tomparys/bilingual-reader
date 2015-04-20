package cz.metaverse.android.bilingualreader.selectionwebview;

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
	 * 	in our custom WebView (SelectionWebView).
	 *  
	 * @param selectedText	The text the user has selected in SelectionWebView.
	 */
	@JavascriptInterface
	public void receiveText(String selectedText) {
		
		// Put selected text into clipdata
		ClipboardManager clipboard = (ClipboardManager)
				mContext.getSystemService(Context.CLIPBOARD_SERVICE);
		ClipData clip = ClipData.newPlainText("simple text",selectedText);
		clipboard.setPrimaryClip(clip);

		// Gives the toast for selected text
		Toast.makeText(mContext, "Selected: " + selectedText, Toast.LENGTH_SHORT).show();
	}
}