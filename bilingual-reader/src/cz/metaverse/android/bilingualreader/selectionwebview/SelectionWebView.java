package cz.metaverse.android.bilingualreader.selectionwebview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ViewParent;
import android.webkit.WebSettings;
import android.webkit.WebView;
import cz.metaverse.android.bilingualreader.R;
import cz.metaverse.android.bilingualreader.ReaderActivity;

/**
 *
 * Custom extension of WebView that allows finding out what text has the user selected.
 * 	It accomplishes this through the use of javascript injection.
 *
 */
public class SelectionWebView extends WebView {
	private Context context;
	// For setting custom action bar
	private ActionMode mActionMode;
	private ActionMode.Callback mSelectActionModeCallback;
	private GestureDetector mDetector;


	/**
	 * Constructor in case of XML initialization.
	 * @param context		Activity context
	 * @param attributeSet	Set of attributes from the XML declaration
	 */
	@SuppressLint("SetJavaScriptEnabled") // Our opensource application has literally nothing to hide.
	public SelectionWebView(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);
		this.context = context;
		WebSettings webviewSettings = getSettings();
		webviewSettings.setJavaScriptEnabled(true);

		// Add JavaScript interface for communication between JS and Java code.
		//  Casting context into ReaderActivity, because it is needed there and the only thing
		//  that should ever own our WebView is an Activity.
		try {
			addJavascriptInterface(new WebAppInterface((ReaderActivity) context), "JSInterface");
		} catch (ClassCastException e) {
			Log.e("SelectionWebView class constructor",
					"SelectionWebView has been passed a Context that can't be cast " +
					"into an Activity, which is needed.");
		}
	}

	/**
	 * Constructor in case of programmatical initialization.
	 * @param context Activity context
	 */
	public SelectionWebView(Context context) {
		this(context, null);
	}

	/**
	 * This overrides the default action bar on long press and substitutes our own.
	 */
	@Override
	public ActionMode startActionMode(ActionMode.Callback callback) {
		ViewParent parent = getParent();
		if (parent == null) {
			return null;
		}

		// For lower Android versions than KitKat
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
			String name = callback.getClass().toString();
			if (name.contains("SelectActionModeCallback")) {
				mSelectActionModeCallback = callback;
				mDetector = new GestureDetector(context, new CustomOnGestureListener());
			}
		}

		// Start our custom ActionMode
		CustomActionModeCallback mActionModeCallback = new CustomActionModeCallback();
		return parent.startActionModeForChild(this, mActionModeCallback);
	}

	/**
	 * Activates JS code inside the WebView that will call WebAppInterface.receiveText method,
	 * 	it will pass along the menuItemId that the user has pressed on the ActionMode bar
	 * 	and it will send the text the user has selected.
	 * @param menuItemId	Id of the menu item pressed on the ActionMode bar
	 */
	@SuppressLint("NewApi") // The code checks the API version and uses the appropriate method.
	private void getSelectedDataAndActOnIt(int menuItemId) {

		// JS function that extracts the selected text
		// 	and sends it to our WebAppInterface.receiveText() method along with menuItemId.
		String js = ""
				+ "(function getSelectedText() {"
				+ 	"var txt;"
				+ 	"if (window.getSelection) {"
				+ 		"txt = window.getSelection().toString();"
				+ 	"} else if (window.document.getSelection) {"
				+ 		"txt = window.document.getSelection().toString();"
				+ 	"} else if (window.document.selection) {"
				+ 		"txt = window.document.selection.createRange().text;"
				+ 	"}"

				+ 	"JSInterface.receiveText(" + menuItemId + ", txt);"
				+ "})()";

		// Now we call the JS function (the invocation is SDK version dependent)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			evaluateJavascript("javascript:" + js, null);
		} else {
			loadUrl("javascript:" + js);
		}
	}

	/**
	 * Overriding onTouchEvent to plug in our gesture detector.
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// Send the event to our gesture detector
		// If it is implemented, there will be a return value
		if (mDetector != null) {
			mDetector.onTouchEvent(event);
		}
		// If the detected gesture is unimplemented, send it to the superclass
		return super.onTouchEvent(event);
	}



	// ============================================================================================
	//		Private inner classes for our custom ActionMode and custom GestureListener
	// ============================================================================================

	/**
	 *
	 * Inner class that handles our custom ActionMode.
	 *
	 */
	private class CustomActionModeCallback implements ActionMode.Callback {

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			mActionMode = mode;

			// Inflate our menu items
			mActionMode.getMenuInflater().inflate(R.menu.text_selection_menu, menu);
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false; // Indicates menu was not updated
		}

		/**
		 * Handles clicks on our ActionMode buttons.
		 */
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			// Send the id of the pressed menu item to the javascript that will pass it along
			// 	to WebAppInterface.receiveText method along with the selected text.
			getSelectedDataAndActOnIt(item.getItemId());

			// We want to leave the ActionMode open for the user to select multiple actions.
			//mode.finish();
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			// Checks the SDK version and uses the appropriate methods.
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				clearFocus();
			}else{
				 if (mSelectActionModeCallback != null) {
					 mSelectActionModeCallback.onDestroyActionMode(mode);
				 }
				 mActionMode = null;
			}
		}
	}


	/**
	 * Extending GestureDetector.SimpleOnGestureListener so we can inform ourselves that actionMode has ended.
	 */
	private class CustomOnGestureListener extends GestureDetector.SimpleOnGestureListener {
		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			if (mActionMode != null) {
				mActionMode.finish();
				return true;
			}
			return false;
		}
	}

}