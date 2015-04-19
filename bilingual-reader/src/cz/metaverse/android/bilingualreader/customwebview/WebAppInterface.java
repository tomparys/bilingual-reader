package cz.metaverse.android.bilingualreader.customwebview;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

public class WebAppInterface {
	Context mContext;
	
	WebAppInterface(Context c) {
	    mContext = c;
	}
	
	@JavascriptInterface
	public void getText(String text) {
	    // put selected text into clipdata
	    ClipboardManager clipboard = (ClipboardManager)
	            mContext.getSystemService(Context.CLIPBOARD_SERVICE);
	    ClipData clip = ClipData.newPlainText("simple text",text);
	    clipboard.setPrimaryClip(clip);
	    // gives the toast for selected text
	    Toast.makeText(mContext, "Selected: " + text, Toast.LENGTH_SHORT).show();
	}
}