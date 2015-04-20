package cz.metaverse.android.bilingualreader.selectionwebview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.ActionMode.Callback;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ViewParent;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;
import cz.metaverse.android.bilingualreader.R;
import cz.metaverse.android.bilingualreader.ReaderActivity;

public class SelectionWebView extends WebView {
	private Context context;
	
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
		// add JavaScript interface for copy
		addJavascriptInterface(new WebAppInterface(context), "JSInterface");
	}

	/**
	 * Constructor in case of programmatical initialization.
	 * @param context Activity context
	 */
	public SelectionWebView(Context context) {
		this(context, null);
	}
	
	// setting custom action bar
	private ActionMode mActionMode;
	private ActionMode.Callback mSelectActionModeCallback;
	private GestureDetector mDetector;
	
	// this will over ride the default action bar on long press
	@Override
	public ActionMode startActionMode(Callback callback) {
		ViewParent parent = getParent();
		if (parent == null) {
			return null;
		}
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
			String name = callback.getClass().toString();
			if (name.contains("SelectActionModeCallback")) {
				mSelectActionModeCallback = callback;
				mDetector = new GestureDetector(context,
						new CustomGestureListener());
			}
		}
		CustomActionModeCallback mActionModeCallback = new CustomActionModeCallback();
		return parent.startActionModeForChild(this, mActionModeCallback);
	}
	
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
			return false; 
		}
	
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			// TODO items need to be implemented
			switch (item.getItemId()) {
			case R.id.copy_menu_item:
				getSelectedData();
				Toast.makeText(ReaderActivity.debugContext, "TODO copy", Toast.LENGTH_SHORT).show();
				break;
			case R.id.share_menu_item:
				getSelectedData();
				Toast.makeText(ReaderActivity.debugContext, "TODO share", Toast.LENGTH_SHORT).show();
				break;
			case R.id.dictionary_menu_item:
				getSelectedData();
				Toast.makeText(ReaderActivity.debugContext, "TODO Dictionary search", Toast.LENGTH_SHORT).show();
				break;
			case R.id.srs_menu_item:
				getSelectedData();
				Toast.makeText(ReaderActivity.debugContext, "TODO Add to SRS", Toast.LENGTH_SHORT).show();
				break;
			default:
				mode.finish();
				return false;
			}
			
			mode.finish();
			return true;
		}
		@Override
		public void onDestroyActionMode(ActionMode mode) {
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
	@SuppressLint("NewApi") // The code checks the API version and uses the appropriate method.
	private void getSelectedData(){
	
		String js= "(function getSelectedText() {"+
				"var txt;"+
				"if (window.getSelection) {"+
					"txt = window.getSelection().toString();"+
				"} else if (window.document.getSelection) {"+
					"txt = window.document.getSelection().toString();"+
				"} else if (window.document.selection) {"+
					"txt = window.document.selection.createRange().text;"+
				"}"+
				"JSInterface.getText(txt);"+
			  "})()";
		// calling the js function
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			evaluateJavascript("javascript:"+js, null);
		}else{
			loadUrl("javascript:"+js);
		}
	}
	
	private class CustomGestureListener extends GestureDetector.SimpleOnGestureListener {
		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			if (mActionMode != null) {
				mActionMode.finish();
				return true;
			}
			return false;
		}
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// Send the event to our gesture detector
		// If it is implemented, there will be a return value
		if(mDetector !=null)
			mDetector.onTouchEvent(event);
		// If the detected gesture is unimplemented, send it to the superclass
		return super.onTouchEvent(event);
	}
}