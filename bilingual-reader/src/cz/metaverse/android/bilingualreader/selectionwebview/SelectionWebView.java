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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Canvas;
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
import cz.metaverse.android.bilingualreader.enums.BookPanelState;
import cz.metaverse.android.bilingualreader.enums.ScrollSyncMethod;
import cz.metaverse.android.bilingualreader.helper.Func;
import cz.metaverse.android.bilingualreader.manager.Governor;
import cz.metaverse.android.bilingualreader.manager.PanelHolder;
import cz.metaverse.android.bilingualreader.panel.BookPanel;

/**
 *
 * Custom extension of WebView that allows finding out what text has the user selected.
 * 	It accomplishes this through the use of JavaScript injection.
 *
 */
public class SelectionWebView extends WebView {

	private static final String LOG = "SelectionWebView";

	/* Interactivity with the outside. */
	private ReaderActivity readerActivity;
	private Governor governor;
	private PanelHolder panelHolder;
	private BookPanel bookPanel;

	// Whether this webView is in the top (0) or the bottom (1) panel.
	private Integer panelPosition;


	/* For setting custom Contextual Action Bar (CAB) */
	private ActionMode mActionMode;
	private ActionMode.Callback mSelectActionModeCallback;
	private GestureDetector mDetector;

	private boolean inActionMode = false;
	private boolean wasInActionMode = false;
	private long actionModeEndedAt;


	/* Scroll Sync */
	private ScrollSyncMethod scrollSyncMethod = ScrollSyncMethod.none;

	// Scroll Sync Offset
	private int scrollSyncOffset = 0;

	// Scroll Sync Ratio - used for ScrollSyncMethod.syncPoints
	private float scrollSyncRatio = 1;

	// The scrollY position when the user activated independent scrolling.
	// After independent scrolling ends, we use this to calculate the new Scroll Sync Offset.
	private int scrollYwhenScrollSyncWasPaused = 0;

	// If the user is currently scrolling this WebView (as opposed to the other one).
	private boolean userIsScrollingThisWebView = false;

	// When user is still interacting with this WebView, but he activated temporary independent scrolling.
	private boolean userPausedScrollSync = false;

	// Whether WebView should react to users touch events and scroll.
	// Used when a double-tap gesture is in progress in which case we do not want the WebView to scroll at all.
	private boolean doNotScrollThisWebView = false;


	/* Other */

	// The previous returned value from getContentHeight()
	// Used to figure out when the WebView content has finished rendering.
	int previousContentHeight = 0;



	// ============================================================================================
	//		Initialization
	// ============================================================================================

	/**
	 * Constructor in case of programmatic initialization - currently not used.
	 * @param readerActivity ReaderActivity context
	 */
	public SelectionWebView(Context readerActivity) {
		this(readerActivity, null);
	}

	/**
	 * Constructor in case of XML initialization.
	 * @param readerActivity  ReaderActivity context
	 * @param attributeSet  Set of attributes from the XML declaration
	 */
	@SuppressLint("SetJavaScriptEnabled") // Our open-source application has literally nothing to hide.
	public SelectionWebView(Context rActivity, AttributeSet attributeSet) {
		super(rActivity, attributeSet);
		this.readerActivity = (ReaderActivity) rActivity;
		governor = readerActivity.governor;

		WebSettings webviewSettings = getSettings();
		webviewSettings.setJavaScriptEnabled(true);

		// Add JavaScript interface for communication between JS and Java code.
		//  Casting context into ReaderActivity, because it is needed there and the only thing
		//  that should ever own our WebView is an Activity.
		try {
			addJavascriptInterface(new WebAppInterface(readerActivity), "JSInterface");
		} catch (ClassCastException e) {
			Log.e("SelectionWebView class constructor",
					"SelectionWebView has been passed a Context that can't be cast " +
					"into an Activity, which is needed.");
		}
	}


	/**
	 * Set the holder and position of the panel this WebView belongs to.
	 */
	public void setPanelHolderAndPosition(PanelHolder panelHolder, int position) {
		//Log.d(LOG, "setPanelHolderAndPosition");

		this.panelHolder = panelHolder;
		bookPanel = panelHolder.getBookPanel();
		panelPosition = position;
	}

	/**
	 * Update the position of the panel this WebView belongs to.
	 */
	public void updatePanelPosition(int pos) {
		panelPosition = pos;
	}



	// ============================================================================================
	//		Interaction with the outside
	// ============================================================================================

	/**
	 * Returns the WebView belonging to the sister panel of the one containing this WebView.
	 */
	public SelectionWebView getSisterWebView() {
		if (panelHolder != null) {
			//Log.v(LOG, "[" + panelPos + "] getSisterWebView");

			return panelHolder.getSisterWebView();
		}

		//Log.v(LOG, "[" + panelPos + "] getSisterWebView - null panelHolder");
		return null;
	}



	// ============================================================================================
	//		Scroll Sync - Inner methods
	// ============================================================================================

	/**
	 * Called when scroll position needs to be updated.
	 *
	 * We're hijacking it so that if the user is scrolling this WebView
	 * and if ScrollSync is enabled, we can scroll the other WebView as well.
	 */
	@Override
	public void computeScroll() {
		// If scrolling of this webView is currently disabled, intercept it and stop it.
		if (!doNotScrollThisWebView) {
			super.computeScroll();
		}

		// If Scroll Sync is active, the user is scrolling this WebView and his scrolling is not paused.
		if (governor.isScrollSync() && bookPanel.enumState == BookPanelState.books
				&& userIsScrollingThisWebView && !userPausedScrollSync) {

			// If sisterWebView exists.
			SelectionWebView sisterWV = getSisterWebView();
			if (sisterWV != null && sisterWV.bookPanel.enumState == BookPanelState.books) {

				// If maxScrollY is positive.
				int computedMaxScrollY = computeMaxScrollY();
				if (computedMaxScrollY != 0) {

					// Compute and set the corresponding scroll position of the other WebView.
					//  Variables need to be long, because the multiplication gets quite large!
					long scrollValue = 0;
					long sisterMaxScrollY = sisterWV.computeMaxScrollY();

					switch (scrollSyncMethod) {
					case none:
						break;

					// Offset - the webviews are synchronized on their % of scroll + offset pixels
					case proportional:
						// Because the position equation isn't symmetrical,
						// we have to compute them differently for each panel:
						if (panelPosition == 0) {
							scrollValue = (getScrollY() + scrollSyncOffset)
									* sisterMaxScrollY / computedMaxScrollY;
						} else {
							scrollValue = getScrollY()
									* sisterMaxScrollY / computedMaxScrollY - scrollSyncOffset;
						}

						/*Log.v(LOG, "[" + panelPosition + "] computeScroll:  from " + getScrollY()
								+ "  to " + scrollValue + "  offset " + scrollSyncOffset
								+ "   (maxScrollY " + computedMaxScrollY + "  destinationMaxScrollY "
								+ sisterWV.computeMaxScrollY() + ")"); /**/
						break;

					case linear:
						if (panelPosition == 0) {
							scrollValue = getScrollY() + scrollSyncOffset;
						} else {
							scrollValue = getScrollY() - scrollSyncOffset;
						}
						break;

					case syncPoints:
						if (panelPosition == 0) {
							scrollValue = Math.round(scrollSyncRatio * getScrollY() + scrollSyncOffset);

						} else {
							scrollValue = Math.round((float) (getScrollY() - scrollSyncOffset) / scrollSyncRatio);
						}
						break;
					}

					// Set the computed scroll to the sister webview if it falls between the
					// minimum and maximum scroll position, otherwise return min or max.
					sisterWV.setScrollY(Func.minMaxRange(0, (int) scrollValue, (int) sisterMaxScrollY));

					/*Log.v(LOG, "[" + panelPosition + "] computeScroll:  from " + getScrollY()
							+ "  to " + scrollValue + "  offset " + scrollSyncOffset + ")"); /**/
				}
			}
		}
	}

	/**
	 * For the user to be able to scroll both webviews in synchronized manner,
	 * both webviews have to be aware of the pertinent ScrollSyncMethod data.
	 */
	private void updateSisterWithNewScrollSyncData() {
		Log.d(LOG, LOG + ".updateSisterWithNewScrollSyncData[" + panelPosition + "]");

		/* Set corresponding ScrollSyncMethod data on the sister WebView. */
		SelectionWebView sisterWV = getSisterWebView();
		if (sisterWV != null) {
			switch (scrollSyncMethod) {
				case none:
					break;

				case syncPoints:
					sisterWV.scrollSyncRatio = scrollSyncRatio;
					// NO BREAK - continue!

				case proportional:
				case linear:
					sisterWV.scrollSyncOffset = scrollSyncOffset;
					break;
			}
		}
	}

	/**
	 * Overriding this method so we can stop the WebView from scrolling at any time we want.
	 */
	@Override
	public boolean overScrollBy(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX,
			int scrollRangeY, int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {

		if (doNotScrollThisWebView) {
			// WebView will not scroll at all.
			return false;
		} else {
			// WebView will scroll normally.
			return super.overScrollBy(deltaX, deltaY, scrollX, scrollY, scrollRangeX,
					scrollRangeY, maxOverScrollX, maxOverScrollY, isTouchEvent);
		}
	}

	/**
	 * Returns the maximal scrollY position.
	 * If you set setScrollY() to this position, the document will be scrolled as far down as possible.
	 */
	public int computeMaxScrollY() {
		// More accurate then simple getContentHeight()
		// Deduced directly from the original code of WebView.computeMaxScrollY().
		return Math.max(computeVerticalScrollRange() - getHeight(), 0);
	}



	// ============================================================================================
	//		Scroll Sync - API for the outside
	// ============================================================================================

	/**
	 * Temporarily pause ScrollSync - user is using two-finger scroll for independent scrolling.
	 */
	public void pauseScrollSync() {
		// Pause only if we were really user scrolling and not paused.
		if (userIsScrollingThisWebView && !userPausedScrollSync) {
			Log.d(LOG, "SelectionWebView.[" + panelPosition + "] pauseScrollSync for now");

			userPausedScrollSync = true;

			switch (scrollSyncMethod) {
				case none:
					break;

				case proportional:
				case linear:
				case syncPoints:
					scrollYwhenScrollSyncWasPaused = getScrollY();
					break;
			}
		}
	}

	/**
	 * Resume scrolling from temporary pause - user stopped independent scrolling.
	 */
	public void resumeScrollSync() {
		// Make resume computations only if we were really user scrolling and paused.
		if (userIsScrollingThisWebView && userPausedScrollSync) {
			Log.d(LOG, "SelectionWebView.[" + panelPosition + "] resumeScrollSync went through");

			userPausedScrollSync = false;

			switch (scrollSyncMethod) {
			case none:
				break;

			case proportional:
				// Because the position equation isn't symmetrical,
				// we have to compute offset differently in each panel:
				if (panelPosition == 0) {

					scrollSyncOffset += scrollYwhenScrollSyncWasPaused - getScrollY();
				} else {
					// If maxScrollY is positive.
					int computedMaxScrollY = computeMaxScrollY();
					if (computedMaxScrollY != 0) {

						// If sisterWebView exists.
						SelectionWebView sisterWV = getSisterWebView();
						if (sisterWV != null) {

							scrollSyncOffset -= (scrollYwhenScrollSyncWasPaused - getScrollY())
									* sisterWV.computeMaxScrollY() / computedMaxScrollY;
						}
					}
				}
				break;


			case linear:
				if (panelPosition == 0) {
					scrollSyncOffset += scrollYwhenScrollSyncWasPaused - getScrollY();
				} else {
					scrollSyncOffset -= scrollYwhenScrollSyncWasPaused - getScrollY();
				}
				break;

			case syncPoints:
				if (panelPosition == 0) {
					scrollSyncOffset += (scrollYwhenScrollSyncWasPaused - getScrollY()) * scrollSyncRatio;
				} else {
					scrollSyncOffset -= scrollYwhenScrollSyncWasPaused - getScrollY();
				}
				break;
			}
			Log.d(LOG, LOG + ".[" + panelPosition + "] new offset: " + scrollSyncOffset);

			updateSisterWithNewScrollSyncData();
		}
	}

	/**
	 * Set whether the user is scrolling this WebView or not at this moment.
	 */
	public void setUserIsScrolling(boolean isScrolling) {

		if (!governor.isScrollSync() || !isScrolling) {
			// If ScrollSync isn't active, or if we want to disable user scrolling:
			userIsScrollingThisWebView = false;
		}
		else {
			// If ScrollSync is active and we want to activate user scrolling:
			BookPanel thisPanel = panelHolder.getBookPanel();
			BookPanel sisterPanel = panelHolder.getSisterBookPanel();

			if (thisPanel != null && thisPanel.enumState == BookPanelState.books
					&& sisterPanel != null && sisterPanel.enumState == BookPanelState.books) {

				// If both opened panels are showing books, we can start user scrolling.
				userIsScrollingThisWebView = true;
			} else {
				userIsScrollingThisWebView = false;
			}
		}

		// Always reset Paused status.
		userPausedScrollSync = false;
	}

	/**
	 * Returns whether the user is scrolling this view or not.
	 */
	public boolean isUserScrolling() {
		return userIsScrollingThisWebView;
	}

	/**
	 * Set whether the WebView will react to scrolling or not scroll at all.
	 */
	public void setDoNotScrollThisWebView(boolean doNotScrollThisWebView) {
		//Log.d(LOG, "[" + panelPos + "] noScrollAtAll " + doNotScrollThisWebView);

		this.doNotScrollThisWebView = doNotScrollThisWebView;
	}

	/**
	 * Reset ScrollSync into a given ScrollSync method.
	 * Used mainly with Method.proportional to start scroll sync for the first time.
	 */
	public void resetScrollSync(ScrollSyncMethod method) {
		resetScrollSync();
		setScrollSyncMethod(method);
	}

	/**
	 * Reset ScrollSync - for instance when new chapter is opened.
	 */
	public void resetScrollSync() {
		Log.d(LOG, LOG + ".resetScrollSync");

		// Set none ScrollSync as default, because it's most useful.
		scrollSyncMethod = ScrollSyncMethod.none;
		scrollSyncOffset = 0;
		scrollSyncRatio = 1;
	}

	/**
	 * Sets the appropriate ScrollSync data (but doesn't activate ScrollSync).
	 */
	public void initializeScrollSyncData(ScrollSyncMethod method, float floatOffset, float ratio) {
		setScrollSyncMethod(method);
		setScrollSyncOffsetFromFloat(floatOffset);
		setScrollSyncRatio(ratio);
	}

	/**
	 * Sets the appropriate ScrollSync data (but doesn't activate ScrollSync).
	 */
	public void initializeScrollSyncData(ScrollSyncMethod method, int offset, float ratio) {
		setScrollSyncMethod(method);
		setScrollSyncOffset(offset);
		setScrollSyncRatio(ratio);
	}

	/**
	 * Returns whether the scrollSync
	 */
	public boolean areScrollSyncDataCongruentWithSister() {
		SelectionWebView sister = getSisterWebView();

		// If the ScrollSyncMethods are not null and equal to each other.
		if (scrollSyncMethod != null && scrollSyncMethod.equals(sister.scrollSyncMethod)) {

			switch (scrollSyncMethod) {
			case none:
				return false;

			case proportional:
			case linear:
				if (scrollSyncOffset == sister.scrollSyncOffset) {
					return true;
				}
				break;

			case syncPoints:
				if (scrollSyncOffset == sister.scrollSyncOffset
						&& scrollSyncRatio == sister.scrollSyncRatio) {
					return true;
				}
				break;
			}
		}
		return false;
	}



	// ============================================================================================
	//		Scroll Sync - Load and save state
	// ============================================================================================

	/**
	 * Returns the currently active Scroll Sync Method.
	 */
	public ScrollSyncMethod getScrollSyncMethod() {
		return scrollSyncMethod;
	}

	/**
	 * Sets the Scroll Sync Method.
	 */
	public void setScrollSyncMethod(ScrollSyncMethod method) {
		scrollSyncMethod = method;

		if (scrollSyncMethod == null) {
			scrollSyncMethod = ScrollSyncMethod.none;
		}
	}
	/**
	 * Sets the Scroll Sync Method from String.
	 */
	private void setScrollSyncMethod(String method) {
		setScrollSyncMethod(ScrollSyncMethod.fromString(method));
	}

	public float getScrollSyncRatio() {
		return scrollSyncRatio;
	}

	private void setScrollSyncRatio(float ratio) {
		scrollSyncRatio = ratio;
	}

	/**
	 * Given the actual integer offset, returns the offset as a fraction of the computeMaxScrollY area.
	 */
	public float getScrollSyncOffsetAsFloat() {
		return (float) scrollSyncOffset / (float) computeMaxScrollY();
	}

	/**
	 * Given the offset as a fraction of the computeMaxScrollY area, sets the actual integer offset.
	 */
	private void setScrollSyncOffsetFromFloat(float floatOffset) {
		scrollSyncOffset = Math.round(floatOffset * computeMaxScrollY());
	}

	/**
	 * Set the offset directly from int to int.
	 */
	private void setScrollSyncOffset(int offset) {
		scrollSyncOffset = offset;
	}

	/**
	 * Loads the ScollSync variables from preferences.
	 */
	public void loadStateWhenContentRendered() {
		SharedPreferences preferences = readerActivity.getPreferences(Context.MODE_PRIVATE);

		// Load ScrollSync data
		String prefSSMethod = preferences.getString("SelectionWV_scrollSyncMethod"+panelPosition, null);
		float prefSSOffset = preferences.getFloat("SelectionWV_scrollSyncOffset"+panelPosition, 0f);
		float prefSSRatio = preferences.getFloat("SelectionWV_scrollSyncRatio"+panelPosition, 1f);

		setScrollSyncMethod(prefSSMethod);
		setScrollSyncOffsetFromFloat(prefSSOffset);
		setScrollSyncRatio(prefSSRatio);

		Log.v(LOG, "SelectionWebView.loadStateWhenContentRendered"
				+ ", scrollSyncMethod: " + scrollSyncMethod
				+ ", scrollSyncOffset: " + scrollSyncOffset
				+ ", scrollSyncRatio: " + scrollSyncRatio);
	}

	/**
	 * Saves the ScrollSync variables, such as offset.
	 */
	public void saveState(Editor editor) {
		// Resumes scroll sync if it was currently paused, to compute the current offset.
		resumeScrollSync();

		// Save ScrollSync data
		editor.putString("SelectionWV_scrollSyncMethod"+panelPosition, scrollSyncMethod.toString());
		editor.putFloat("SelectionWV_scrollSyncOffset"+panelPosition, getScrollSyncOffsetAsFloat());
		editor.putFloat("SelectionWV_scrollSyncRatio"+panelPosition, getScrollSyncRatio());

		Log.v(LOG, "SelectionWebView.saveState"
				+ ", scrollSyncMethod: " + scrollSyncMethod
				+ ", scrollSyncOffset: " + scrollSyncOffset
				+ ", scrollSyncRatio: " + scrollSyncRatio);
	}



	// ============================================================================================
	//		Misc
	// ============================================================================================

	/**
	 * Overriding onDraw is necessary, because it is a tested method (See ParagraphPositionsWebView)
	 *  of a chance to figure out when the WebView has finished rendering its contents and we can
	 *  obtain a new non-zero getContentHeight() value.
	 *
	 * Warning: getContentHeight either returns 0 when content isn't yet rendered,
	 *  or a non-zero value that is >= the height of the WebView display area.
	 * Therefore if user opens two pages that are shorter than the WebView display area after each other,
	 *  we will NOT get notified that the content has finished rendering.
	 * This is, however, not an issue, because in such short pages, loading the scroll position
	 *   from preferences makes no sense anyway.
	 */
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (previousContentHeight != getContentHeight() && getProgress() == 100) {
			if (bookPanel != null) {
				previousContentHeight = getContentHeight();

				bookPanel.onFinishedRenderingContent();
			}
		}
	}



	// ============================================================================================
	//		Text selection ActionMode / Custom Action Bar
	// ============================================================================================

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
				mDetector = new GestureDetector(readerActivity, new CustomOnGestureListener());
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
	 * Overriding onTouchEvent to plug in our gesture detector,
	 * so that our custom ActionMode works on older Android versions as well.
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// Send the event to our gesture detector
		// If it is implemented, there will be a return value
		if (mDetector != null) {
			mDetector.onTouchEvent(event);
		}

		performClick(); // Android system mandates we pass the baton to the onClick listener now.

		// If the detected gesture is unimplemented, send it to the superclass
		return super.onTouchEvent(event);
	}

	/**
	 * Android sends out warning unless we override this method.
	 */
	@Override
	public boolean performClick() {
		// Calls the super implementation, which generates an AccessibilityEvent
		// and calls the onClick() listener on the view, if any.
		return super.performClick();
	}

	/**
	 * Returns whether ActionMode is currently active or not.
	 */
	public boolean inSelectionActionMode() {
		if (inActionMode) {
			return true;
		} else {
			// If this is the first time someone asked inSelectionActionMode() after ActionMode ended,
			// i.e. if this is the first time someone did a single tap on the screen after AM ended.
			if (wasInActionMode) {
				wasInActionMode = false;

				// If the ActionMode ended less than a second ago, say that ActionMode is active
				// so the recipient knows the click they're evaluating highly probably served to close
				// the ActionMode, and therefore should not be used to switch Fullscreen.
				return System.currentTimeMillis() - actionModeEndedAt < 1000;
			} else
				return false;
		}
	}



	// ============================================================================================
	//		Helper private inner classes for our custom ActionMode (and its custom GestureListener)
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
			inActionMode = true;

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
			inActionMode = false;
			wasInActionMode = true;
			actionModeEndedAt = System.currentTimeMillis();

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
		public boolean onSingleTapConfirmed(MotionEvent e) {
			if (mActionMode != null) {
				mActionMode.finish();

				inActionMode = false;
				wasInActionMode = true;
				actionModeEndedAt = System.currentTimeMillis();

				return true;
			}
			return false;
		}
	}
}
