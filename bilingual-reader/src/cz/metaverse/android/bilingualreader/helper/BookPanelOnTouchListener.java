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

package cz.metaverse.android.bilingualreader.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import cz.metaverse.android.bilingualreader.ReaderActivity;
import cz.metaverse.android.bilingualreader.dialog.PanelSizeDialog;
import cz.metaverse.android.bilingualreader.enums.BookPanelState;
import cz.metaverse.android.bilingualreader.manager.Governor;
import cz.metaverse.android.bilingualreader.manager.PanelHolder;
import cz.metaverse.android.bilingualreader.panel.BookPanel;
import cz.metaverse.android.bilingualreader.selectionwebview.SelectionWebView;

/**
 *
 *                                                       /===-_---~~~~~~~~~------____
 *                                                      |===-~___                _,-'
 *                       -==\\                         `//~\\   ~~~~`---.___.-~~
 *                   ______-==|                         | |  \\           _-~`
 *             __--~~~  ,-/-==\\                        | |   `\        ,'
 *          _-~       /'    |  \\                      / /      \      /
 *        .'        /       |   \\                   /' /        \   /'
 *       /  ____  /         |    \`\.__/-~~ ~ \ _ _/'  /          \/'
 *      /-'~    ~~~~~---__  |     ~-/~         ( )   /'        _--~`
 *                        \_|      /        _)   ;  ),   __--~~
 *                          '~~--_/      _-~/-  / \   '-~ \
 *                         {\__--_/}    / \\_>- )<__\      \
 *                         /'   (_/  _-~  | |__>--<__|      |
 *                        |0  0 _/) )-~     | |__>--<__|     |
 *                        / /~ ,_/       / /__>---<__/      |
 *                       o o _//        /-~_>---<__-~      /
 *                       (^(~          /~_>---<__-      _-~
 *                      ,/|           /__>--<__/     _-~
 *                   ,//('(          |__>--<__|     /                  .----_
 *                  ( ( '))          |__>--<__|    |                 /' _---_~\
 *               `-)) )) (           |__>--<__|    |               /'  /     ~\`\
 *              ,/,'//( (             \__>--<__\    \            /'  //        ||
 *            ,( ( ((, ))              ~-__>--<_~-_  ~--____---~' _/'/        /'
 *          `~/  )` ) ,/|                 ~-_~>--<_/-__       __-~ _/
 *        ._-~//( )/ )) `                    ~~-'_/_/ /~~~~~~~__--~
 *         ;'( ')/ ,)(                              ~~~~~~~~~~
 *        ' ') '( (/
 *          '   '  `
 *
 *                                HERE BE DRAGONS
 *
 *
 *
 * Be warned, stranger, editing this class is perilous.
 * Editing one functionality can and will have unforeseen adverse effects in other areas.
 * Test your changes profusely.
 *
 *
 *
 * This class serves as a OnTouchListener for our BookPanel.
 *
 * It comprises of a complex logic that allows to discern several single and multitouch gestures:
 *
 *   - Single finger swipe up/down
 *   	- standard scrolling (implicit, not implemented here) with ScrollSync scrolling
 *        of the other panel, if ScrollSync is activated.
 *
 *   - Single finger swipe left/right - switch book chapter
 *
 *   - Two (or more) finger swipe up or down
 *      - standard scrolling with forced disabled ScrollSync, i.e. other panel won't never scroll,
 *        after the end of this scroll, the SelectionWebView is given notice to resume ScrollSync
 *        if appropriate and compute new offset/sync point. See SelectionWebView class.
 *
 *   - Double tap and swipe towards the side of the device (left/right in landscape, up/down in portrait)
 *      - Reappear the other panel if it is hidden, or hide this panel.
 *
 *   - Double tap and swipe towards or away from the center of the device
 *               (up/down in landscape, left/right in portrait)
 *      - Change the relative size of the two panels on the fly.
 *
 *
 */
public class BookPanelOnTouchListener
		implements OnTouchListener, GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {

	/*
	 * Threshold constants for supported gestures.
	 * All values are in device independent pixels (dp).
	 *
	 * For illustration, here are some numbers for typical screen *widths*:
	 *  - 320dp: a typical phone screen (240x320 ldpi, 320x480 mdpi, 480x800 hdpi, etc).
	 *  - 480dp: a phablet (480x800 mdpi).
	 *  - 600dp: a 7” tablet (600x1024 mdpi).
	 *  - 720dp: a 10” tablet (720x1280 mdpi, 800x1280 mdpi, etc).
	 */
	// Change Panel Size in Real Time threshold - both for up/down (portrait) and left/right (landscape mode)
	private static final int THRESHOLD_SWIPE_UP_OR_DOWN_CHANGE_PANEL_SIZE = 20; // dp
	private static final int THRESHOLD_SWIPE_LEFT_RIGHT_CHANGE_PANEL_SIZE = 20; // dp

	// Switch chapter threshold - swipe left/right
	private static final int THRESHOLD_SWIPE_LEFT_RIGHT_SWITCH_CHAPTER = 120; // dp

	// Hide/reappear panel threshold - both for left/right (portrait) and up/down (landscape mode)
	private static final int THRESHOLD_SWIPE_LEFT_RIGHT_HIDE_PANEL = 120; // dp
	private static final int THRESHOLD_SWIPE_UP_OR_DOWN_HIDE_PANEL = 120; // dp

	/* Threshold fiels - calculated pixel values from the dp constants above. */
	private final int threshold_swipe_left_right_switch_chapter_px;
	private final int threshold_swipe_left_right_hide_panel_px;
	private final int threshold_swipe_up_or_down_hide_panel_px;
	private final int threshold_swipe_up_or_down_change_panel_size_px;
	private final int threshold_swipe_left_right_change_panel_size_px;


	/* Logging */
	protected static final String LOG = "alfons";

	/* Variables for interconnectivity with the outside world. */
	private ReaderActivity activity;
	private Governor governor;
	private PanelHolder panelHolder;
	private BookPanel bookPanel;
	private SelectionWebView webView;
	private int panelPosition;

	/*
	 * Fields for touch/gesture events.
	 */
	protected GestureDetectorCompat gestureDetector;

	// For scrolling gesture events.
	protected boolean scrollIsOrWasMultitouch;
	protected float scrollOriginX;
	protected float scrollOriginY;

	// For DoubleTapEvent gestures.
	private boolean isDoubleTapSwipe;
	private float doubleTapOriginX;
	private float doubleTapOriginY;
	private boolean doubleTapSwipeEscapedBounds;
	private float newPanelsWeight;
	private int doubleTapSwipe_contentStartsAtHeight;
	private int doubleTapSwipe_viewHeight;
	private int doubleTapSwipe_orientation;

	// For fullscreen.
	private boolean justClickedOnUrlLink = false;


	public BookPanelOnTouchListener(ReaderActivity readerActivity, Governor governor,
			PanelHolder panelHolder, BookPanel bookPanel, SelectionWebView webView, int position) {

		// Interconnectivity.
		this.activity = readerActivity;
		this.governor = governor;
		this.panelHolder = panelHolder;
		this.bookPanel = bookPanel;
		this.webView = webView;
		this.panelPosition = position;

		// Instantiate the gesture detector.
		gestureDetector = new GestureDetectorCompat(activity, this);
		gestureDetector.setOnDoubleTapListener(this);

		/* Compute the threshold limits in pixels */
		Resources r = activity.getResources();

		// Change Panel Size in Real Time threshold - both for up/down (portrait) and left/right (landscape mode)
		threshold_swipe_up_or_down_change_panel_size_px = Func.dpToPix(r, THRESHOLD_SWIPE_UP_OR_DOWN_CHANGE_PANEL_SIZE);
		threshold_swipe_left_right_change_panel_size_px = Func.dpToPix(r, THRESHOLD_SWIPE_LEFT_RIGHT_CHANGE_PANEL_SIZE);

		// Switch chapter threshold - swipe left/right
		threshold_swipe_left_right_switch_chapter_px = Func.dpToPix(r, THRESHOLD_SWIPE_LEFT_RIGHT_SWITCH_CHAPTER);

		// Hide/reappear panel threshold - both for left/right (portrait) and up/down (landscape mode)
		threshold_swipe_left_right_hide_panel_px = Func.dpToPix(r, THRESHOLD_SWIPE_LEFT_RIGHT_HIDE_PANEL);
		threshold_swipe_up_or_down_hide_panel_px = Func.dpToPix(r, THRESHOLD_SWIPE_UP_OR_DOWN_HIDE_PANEL);
	}

	/**
	 * Update the position of the panel.
	 */
	public void updatePanelPosition(int position) {
		panelPosition = position;
	}

	/**
	 * Called when our custom WebViewClient that listens to our WebView detects that the user just
	 * clicked on a URL link.
	 * In that case we will ignore the next onSingleTapConfirmed, and won't switch to/from fullscreen.
	 */
	public void setJustClickedOnUrlLink() {
		justClickedOnUrlLink = true;
	}


	// ============================================================================================
	//		onTouch
	// ============================================================================================

	/**
	 * The entry method for any touch-related event.
	 * @param view The WebView where the swipe took place
	 * @param event The MotionEvent of the swipe
	 */
	@Override
	public boolean onTouch(View view, MotionEvent event) {
		// Provide data to the GestureDetector.
		gestureDetector.onTouchEvent(event);

		/*
		 * Handle Double Tap Swipe in real time.
		 *  This handling isn't in onDoubleTapEvent() because that method has a limit how many times
		 *  it gets called after which it isn't called at all until the user lifts the finger in which
		 *  case it gets called one last time. That is not workable for on-the-fly resizing of panels.
		 */
		if (isDoubleTapSwipe) {
			float absDiffX = Math.abs(doubleTapOriginX - event.getX());
			float absDiffY = Math.abs(doubleTapOriginY - event.getY());

			// If the swipe was over predefined threshold value high and it was higher than wider,
			// or if the swipe was over predefined threshold value wide and it was wider than higher.
			if (doubleTapSwipeEscapedBounds ||
					// Bounds for PORTRAIT orientation.
					(absDiffY > threshold_swipe_up_or_down_change_panel_size_px && absDiffY > absDiffX
							&& doubleTapSwipe_orientation == Configuration.ORIENTATION_PORTRAIT) ||
					// Bounds for LANDSCAPE orientation.
					(absDiffX > threshold_swipe_left_right_change_panel_size_px && absDiffX > absDiffY
							&& doubleTapSwipe_orientation != Configuration.ORIENTATION_PORTRAIT)) {

				if (!doubleTapSwipeEscapedBounds) {
					// This is the first time doubleTapSwipe escaped it's bounds
					// - we're officially in the set-panels-size mode.
					doubleTapSwipeEscapedBounds = true;

					// Find out and save the relevant dimensions of our view/display
					Window window = activity.getWindow();

					if(doubleTapSwipe_orientation == Configuration.ORIENTATION_PORTRAIT) {
						doubleTapSwipe_contentStartsAtHeight = window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
						doubleTapSwipe_viewHeight = window.getDecorView().getHeight();
					} else {
						doubleTapSwipe_contentStartsAtHeight = window.findViewById(Window.ID_ANDROID_CONTENT).getLeft();
						doubleTapSwipe_viewHeight = window.getDecorView().getWidth();
					}
				}

				// Compute the panels weight
				float useCoordinate = (doubleTapSwipe_orientation == Configuration.ORIENTATION_PORTRAIT)
						? event.getRawY() : event.getRawX();

				newPanelsWeight = (useCoordinate - doubleTapSwipe_contentStartsAtHeight)
						/ (doubleTapSwipe_viewHeight - doubleTapSwipe_contentStartsAtHeight);

				// If the weight is close to 0.5, let it stick to it.
				if (Math.abs(0.5 - newPanelsWeight) < 0.05) {
					newPanelsWeight = 0.5f;
				}

				// Change relative panel size on the fly.
				governor.changePanelsWeight(newPanelsWeight);

				//Log.v(LOG, "[" + panelPosition + "] doubleTapSwipe " + newPanelsWeight);
				//+ " = (" + event.getRawY() + " - " + contentViewTop + ") / " + (height - contentViewTop));
			}
		}

		/*
		 * Handle ACTION_UP event for scrolling, because onScroll (almost?) never gets
		 * called with the last MotionEvent.ACTION_UP.
		 * Don't forget to mirror any changes made here in the onScroll() method as well, to be safe.
		 */
		if(MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_UP) {
			Log.d(LOG, "[" + panelPosition + "] OnTouchListener --> onTouch ACTION_UP");

			// Only if it's not DoubleTapSwipe, that is handled separately.
			if (!isDoubleTapSwipe) {
				// Evaluate if the scroll that has just ended constitutes some gesture.
				handleScrollEnd(event);
			}
		}

		view.performClick(); // Android system mandates we pass the baton to the onClick listener now.

		return view.onTouchEvent(event);
	}


	// ============================================================================================
	//		Gesture Detector hooks
	// ============================================================================================

	/**
	 * First finger was put on the screen.
	 */
	@Override
	public boolean onDown(MotionEvent event) {
		Log.d(LOG, "[" + panelPosition + "] onDown"); //: " + event.toString());

		/*
		 * Reset all variables from handling of the last touch gesture
		 * and re-initiate them for this new touch gesture handling.
		 *
		 * Warning: When doing double-tap, after the second tap, all variables get re-initiated here for
		 *  the second time. There's no good way around this, but it also doesn't cause any issues so far.
		 */
		// Reset fields dealing with scroll.
		scrollIsOrWasMultitouch = false;
		scrollOriginX = event.getX();
		scrollOriginY = event.getY();

		// Reset fields dealing with double tap swipe.
		isDoubleTapSwipe = false;
		doubleTapSwipeEscapedBounds = false;
		webView.setNoScrollAtAll(false);  // (Re)activate WebView scrolling.
		doubleTapSwipe_contentStartsAtHeight = 0; // Will be recomputed upon need, and with current values
		doubleTapSwipe_viewHeight = 0;            // instead of saved ones that need to be updated upon change.
		doubleTapSwipe_orientation = activity.getResources().getConfiguration().orientation;


		// If ScrollSync is active:
		if (governor.isScrollSync()) {
			// The user laid finger in this WebView and may start scrolling it.
			//  Therefore send resumeScrollSync to both WebViews, the one that was active in User Scrolling
			//   will compute new offset and send it to the other one.
			//  Afterwards activate User Scrolling in this WebView and deactivate it in the sister WebView.
			webView.resumeScrollSync();
			webView.setUserIsScrolling(true);

			BookPanel otherBookPanel = panelHolder.getSisterBookPanel();
			if (otherBookPanel != null) {
				otherBookPanel.getWebView().resumeScrollSync();
				otherBookPanel.getWebView().setUserIsScrolling(false);

				// onDown() gesture stops the fling in this WebView,
				//  therefore we do the same for the sister WebView.
				otherBookPanel.getWebView().flingScroll(0, 0);
			}
		}

		return true;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		Log.d(LOG, "[" + panelPosition + "] onFling"); //: " + event1.toString()+event2.toString());

		// Evaluation of the end of the scroll is handled directly in onTouch for consistent results.

		return true;
	}

	/**
	 * Gets called when the user scrolls, but not when he double taps and swipes/scrolls.
	 */
	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		//Log.d(LOG, "[" + panelPosition + "] onScroll"); //: " + e2.toString());

		/* Change from single to a Multi-touch event */
		if (e2.getPointerCount() > 1 && !scrollIsOrWasMultitouch) {
			Log.d(LOG, "[" + panelPosition + "] onScroll: Change from single to a Multitouch event");

			// Evaluate if the scroll section that has just ended constitutes some gesture.
			handleScrollEnd(e2);

			if (governor.isScrollSync() && webView.isUserScrolling()) {
				// Pause ScrollSync, user is setting a scrolling offset.
				webView.pauseScrollSync();
			}

			// Start a new scroll section.
			scrollIsOrWasMultitouch = true;
			scrollOriginX = e2.getX();
			scrollOriginY = e2.getY();
		}
		/* Change from multi to a Single-touch event */
		else if (e2.getPointerCount() <= 1 && scrollIsOrWasMultitouch) {
			// Do nothing.
			Log.d(LOG, "[" + panelPosition + "] onScroll: Change from multi to a Singletouch event detected - doing nothing.");
		}

		// Evaluation of the end of the scroll is handled directly in onTouch for consistent results,
		//  because the last onScroll call with MotionEvent.ACTION_UP (almost?) never happens.
		//  Also, this solution unifies handling with scrolls that end up in onFling.

		return true;
	}

	/**
	 * Handles the ending of a scroll gesture.
	 * @param e2  The last MotionEvent of the scroll or of the scroll section.
	 */
	private void handleScrollEnd(MotionEvent e2) {
		float diffX = scrollOriginX - e2.getX();
		float diffY = scrollOriginY - e2.getY();
		float absDiffX = Math.abs(diffX);
		float absDiffY = Math.abs(diffY);

		// If this scroll has never been multi-touch or doubleTapSwipe:
		if (!scrollIsOrWasMultitouch && !isDoubleTapSwipe) {
			// If the WebView is displaying a book (no sense in switching chapters of metadata or Toc):
			if (bookPanel.enumState == BookPanelState.books) {

				// Single touch scroll on a book - evaluate if the user wants to change chapters.
				if (diffX > threshold_swipe_left_right_switch_chapter_px && absDiffX > absDiffY) {
					// Next chapter - If the swipe was to the right, over the predefined threshold,
					//  and more broad than high.
					panelHolder.changeChapter(true);
				} else if (diffX < -threshold_swipe_left_right_switch_chapter_px && absDiffX > absDiffY) {
					// Previous chapter - If the swipe was to the left, over the predefined threshold,
					//  and more broad than high.
					panelHolder.changeChapter(false);
				}
			}
		}

		/* Disabled because it was getting confused with two-finger scrolling gesture,
		 * and because this functionality is already covered by doubleTapSwipe.
		// Multi-touch scroll sideways:
		if (scrollIsMultitouch) {
			if (absDiffX > quarterWidth && absDiffX > absDiffY) {
				// Hide or reappear panel -
				//  If the swipe was over 1/4 of the screen wide, and more broad than high.
				Toast.makeText(ReaderActivity.debugContext, "Hiding panel - by multitouch scroll.", Toast.LENGTH_SHORT).show();
			}
		}*/
	}

	@Override
	public boolean onDoubleTap(MotionEvent event) {
		Log.d(LOG, "[" + panelPosition + "] onDoubleTap"); //: " + event.toString());

		doubleTapOriginX = event.getX();
		doubleTapOriginY = event.getY();
		return true;
	}

	/**
	 * Called when the user double taps and then swipes his finger away on the second tap.
	 *
	 * Warning: Gets called before the second onDown() call, which resets values used here.
	 * But the subsequent call of this method can repair the damage.
	 */
	@Override
	public boolean onDoubleTapEvent(MotionEvent event) {
		//Log.v(LOG, "[" + panelPosition + "] onDoubleTapEvent"); //: " + event.toString());

		// This cannot be moved to onDoubleTap, because another onDown comes after it that wipes it out.
		if (!isDoubleTapSwipe) {
			isDoubleTapSwipe = true;
			webView.setNoScrollAtAll(true);
		}

		/*
		 * Detecting Double tap + swipe up/down was moved directly to onTouch(),
		 *  because this method has a limit how many times it gets called after which it isn't called
		 *  at all until the user lifts the finger in which case it gets called one last time.
		 *  That is not workable for on-the-fly resizing of panels.
		 */

		// If the finger has finally risen.
		if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_UP) {
			//Log.d(DEBUG_TAG, "onDoubleTapEvent ACTION_UP"); //: " + event.toString());

			// If doubleTapSwipe escaped its vertical bounds and up/down sliding begun changing
			// the relative size of panels on the fly. The user has settled on a relative size he wants,
			// and our job now is to save this preference into PanelSizeDialog for further use in the dialog.
			if (doubleTapSwipeEscapedBounds) {
				// Save the newly set panels weight into preferences so PanelSizeDialog can acess it.
				SharedPreferences preferences = activity.getPreferences(Context.MODE_PRIVATE);
				PanelSizeDialog.saveSeekBarValue(preferences, newPanelsWeight);
			}
			else {
				// DoubleTapSwipe never escaped its bounds and therefore
				// we will evaluate if it constituted a swipe to the side.
				float absDiffX = Math.abs(doubleTapOriginX - event.getX());
				float absDiffY = Math.abs(doubleTapOriginY - event.getY());

						// PORTRAIT: If the swipe was over the threshold wide and it was wider than higher.
				if (	(absDiffX > threshold_swipe_left_right_hide_panel_px && absDiffX > absDiffY
								&& doubleTapSwipe_orientation == Configuration.ORIENTATION_PORTRAIT) ||
						// LANDSCAPE: If the swipe was over the threshold high and it was higher than wider.
						(absDiffY > threshold_swipe_up_or_down_hide_panel_px && absDiffY > absDiffX
								&& doubleTapSwipe_orientation != Configuration.ORIENTATION_PORTRAIT)) {

					// Hide or reappear panel.
					panelHolder.hideOrReappearPanel();
				}
			}
		}

		return true;
	}

	/**
	 * onSingleTapConfirmed - Activate/deactivate immersive fullscreen mode.
	 */
	@Override
	public boolean onSingleTapConfirmed(MotionEvent event) {
		Log.d(LOG,"[" + panelPosition + "] onSingleTapConfirmed"); //: " + event.toString());

		// The user just clicked on a URL link, therefore do nothing with this click.
		if (justClickedOnUrlLink) {
			justClickedOnUrlLink = false;
		}
		// If we aren't in selection mode, switch fullscreen modes.
		else if (!webView.inSelectionActionMode()) {
			activity.switchFullscreen();
		}
		return true;
	}

	@Override
	public boolean onSingleTapUp(MotionEvent event) {
		Log.d(LOG,"[" + panelPosition + "] onSingleTapUp"); //: " + event.toString());
		return true;
	}

	@Override
	public void onLongPress(MotionEvent event) {
		Log.d(LOG, "[" + panelPosition + "] onLongPress"); //: " + event.toString());
	}

	@Override
	public void onShowPress(MotionEvent event) {
		Log.d(LOG, "[" + panelPosition + "] onShowPress"); //: " + event.toString());
	}

}
