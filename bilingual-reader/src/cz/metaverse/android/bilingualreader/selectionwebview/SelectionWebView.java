package cz.metaverse.android.bilingualreader.selectionwebview;

import android.annotation.SuppressLint;
import android.content.Context;
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
import cz.metaverse.android.bilingualreader.helper.BookPanelState;
import cz.metaverse.android.bilingualreader.helper.ScrollSyncMethod;
import cz.metaverse.android.bilingualreader.manager.Governor;
import cz.metaverse.android.bilingualreader.manager.PanelHolder;
import cz.metaverse.android.bilingualreader.panel.BookPanel;

/**
 *
 * Custom extension of WebView that allows finding out what text has the user selected.
 * 	It accomplishes this through the use of javascript injection.
 *
 */
public class SelectionWebView extends WebView {

	private static final String LOG = "SelectionWebView";

	private ReaderActivity readerActivity;
	private Governor governor;
	private PanelHolder panelHolder;
	private BookPanel bookPanel;

	/* For setting custom Contextual Action Bar (CAB) */
	private ActionMode mActionMode;
	private ActionMode.Callback mSelectActionModeCallback;
	private GestureDetector mDetector;

	private boolean inActionMode = false;
	private boolean wasInActionMode = false;
	private long actionModeEndedAt;

	/* Scroll Sync */
	private ScrollSyncMethod scrollSyncMethod = ScrollSyncMethod.percentual_withOffset;
	private Integer panelPosition;
	private boolean userIsScrolling = false;
	// When user is still interacting with this WebView, but the sync scrolling is temporarily paused.
	private boolean userScrollingPaused = false;
	private boolean noScrollAtAll = false;

	// ScrollSync method offset
	private int scrollSyncOffset;
	private int scrollYwhenPaused;

	// The previous returned value from getContentHeight().
	int previousContentHeight = 0;


	/**
	 * Constructor in case of XML initialization.
	 * @param readerActivity  ReaderActivity context
	 * @param attributeSet  Set of attributes from the XML declaration
	 */
	@SuppressLint("SetJavaScriptEnabled") // Our opensource application has literally nothing to hide.
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
	 * Constructor in case of programmatic initialization.
	 * @param readerActivity ReaderActivity context
	 */
	public SelectionWebView(Context readerActivity) {
		this(readerActivity, null);
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
	 * Overriding onTouchEvent to plug in our gesture detector.
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
	//		Synchronized Scrolling
	// ============================================================================================

	/**
	 * Called when scroll position needs to be updated.
	 *
	 * If the user is scrolling this WebView and if scroll sync is enabled, scrolls the other WebView.
	 */
	@Override
	public void computeScroll() {
		if (!noScrollAtAll) {
			super.computeScroll();
		}

		// If Scroll Sync is active, the user is scrolling this WebView and his scrolling is not paused.
		if (governor.isScrollSync() && userIsScrolling && !userScrollingPaused) {

			// If maxScrollY is positive.
			int computedMaxScrollY = computeMaxScrollY();
			if (computedMaxScrollY != 0) {

				// If sisterWebView exists.
				SelectionWebView sisterWV = getSisterWebView();
				if (sisterWV != null) {

					// Compute and set the corresponding scroll position of the other WebView.
					int scrollValue = 0;
					switch (scrollSyncMethod) {

					// Offset - the webviews are synchronized on their % of scroll + offset pixels
					case percentual_withOffset:
						// Because the position equation isn't symmetrical,
						// we have to compute them differently for each panel:
						if (panelPosition == 0) {
							scrollValue = (getScrollY() - scrollSyncOffset)
									* sisterWV.computeMaxScrollY() / computedMaxScrollY;
						} else {
							scrollValue = getScrollY()
									* sisterWV.computeMaxScrollY() / computedMaxScrollY - scrollSyncOffset;
						}

						/*Log.v(LOG, "[" + panelPos + "] computeScroll:  from " + getScrollY()
								+ "  to " + scrollValue + "  offset " + scrollSyncOffset
								+ "   (maxScrollY " + computedMaxScrollY + "  destinationMaxScrollY "
								+ sisterWV.computeMaxScrollY() + ")"); /**/
						break;

					default:
						break;
					}

					// Set the computed scroll to the sister webview.
					sisterWV.setScrollY(scrollValue);
				}
			}
		}
	}

	/**
	 * Overriding this method so we can stop the WebView from scrolling at any time we want.
	 */
	@Override
	public boolean overScrollBy(int deltaX, int deltaY, int scrollX, int scrollY,
			int scrollRangeX, int scrollRangeY, int maxOverScrollX,
			int maxOverScrollY, boolean isTouchEvent) {

		if (noScrollAtAll) {
			// WebView will not scroll at all.
			return false;
		} else {
			// WebView will scroll normally.
			return super.overScrollBy(deltaX, deltaY, scrollX, scrollY, scrollRangeX, scrollRangeY,
					maxOverScrollX, maxOverScrollY, isTouchEvent);
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

	/**
	 * Temporarily pause ScrollSync - user is using two-finger scroll for independent scrolling.
	 */
	public void pauseScrollSync() {
		// Pause only if we were really user scrolling and not paused.
		if (userIsScrolling && !userScrollingPaused) {
			//Log.d(LOG, "[" + panelPos + "] pauseScrollSync for now");

			userScrollingPaused = true;

			switch (scrollSyncMethod) {

			case percentual_withOffset:
				scrollYwhenPaused = getScrollY();
				break;

			default:
				break;
			}
		}
	}

	/**
	 * Resume scrolling from temporary pause - user stopped independent scrolling.
	 */
	public void resumeScrollSync() {
		// Make resume computations only if we were really user scrolling and paused.
		if (userIsScrolling && userScrollingPaused) {
			//Log.d(LOG, "[" + panelPos + "] resumeScrollSync went through");

			userScrollingPaused = false;

			switch (scrollSyncMethod) {

			case percentual_withOffset:
				// Because the position equation isn't symmetrical,
				// we have to compute offset differently in each panel:
				if (panelPosition == 0) {
					scrollSyncOffset += getScrollY() - scrollYwhenPaused;
				} else {
					// If maxScrollY is positive.
					int computedMaxScrollY = computeMaxScrollY();
					if (computedMaxScrollY != 0) {

						// If sisterWebView exists.
						SelectionWebView sisterWV = getSisterWebView();
						if (sisterWV != null) {

							scrollSyncOffset += (getScrollY() - scrollYwhenPaused)
									* sisterWV.computeMaxScrollY() / computedMaxScrollY;
						}
					}
				}
				//Log.d(LOG, "[" + panelPos + "] new offset: " + scrollSyncOffset);
				break;

			default:
				break;
			}

			setCorrespondingScrollSyncDataOnSisterWebView();
		}
	}

	/**
	 * Reset ScrollSync - for instance when new chapter is opened.
	 */
	public void resetScrollSync() {
		switch (scrollSyncMethod) {

		case percentual_withOffset:
			scrollSyncOffset = 0;
			break;

		default:
			break;
		}

		setCorrespondingScrollSyncDataOnSisterWebView();
	}

	/**
	 * For the user to be able to scroll both webviews in synchronized manner,
	 * both webviews have to be aware of the pertinent ScrollSyncMethod data.
	 */
	private void setCorrespondingScrollSyncDataOnSisterWebView() {
		/* Set corresponding ScrollSyncMethod data on the sister WebView. */
		SelectionWebView sisterWV = getSisterWebView();
		if (sisterWV != null) {

			switch (scrollSyncMethod) {

			case percentual_withOffset:
				sisterWV.setCorrespondingScrollSyncOffset(scrollSyncOffset);
				break;

			default:
				break;
			}
		}
	}

	/**
	 * Sets corresponding data for the ScrollSyncMethod offset method.
	 */
	public void setCorrespondingScrollSyncOffset(int offset) {
		//Log.d(LOG, "[" + panelPos + "] received new offset: " + offset);
		scrollSyncOffset = -offset;
	}

	/**
	 * Set whether the WebView will react to scrolling or not scroll at all.
	 */
	public void setNoScrollAtAll(boolean noScrollAtAll) {
		//Log.d(LOG, "[" + panelPos + "] noScrollAtAll " + noScrollAtAll);

		this.noScrollAtAll = noScrollAtAll;
	}

	/**
	 * Set whether the user is scrolling this WebView or not at this moment.
	 */
	public void setUserIsScrolling(boolean isScrolling) {

		if (!governor.isScrollSync() || !isScrolling) {
			// If ScrollSync isn't active, or if we want to disable user scrolling:
			userIsScrolling = false;
		}
		else {
			// If ScrollSync is active and we want to activate user scrolling:
			BookPanel thisPanel = panelHolder.getBookPanel();
			BookPanel sisterPanel = panelHolder.getSisterBookPanel();

			if (thisPanel != null && thisPanel.enumState == BookPanelState.books
					&& sisterPanel != null && sisterPanel.enumState == BookPanelState.books) {

				// If both opened panels are showing books, we can start user scrolling.
				userIsScrolling = true;
			} else {
				userIsScrolling = false;
			}
		}

		// Always reset Paused status.
		userScrollingPaused = false;
	}

	/**
	 * Returns whether the user is scrolling this view or not.
	 */
	public boolean isUserScrolling() {
		return userIsScrolling;
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