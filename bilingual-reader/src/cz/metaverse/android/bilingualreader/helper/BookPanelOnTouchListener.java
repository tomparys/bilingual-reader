package cz.metaverse.android.bilingualreader.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Toast;
import cz.metaverse.android.bilingualreader.R;
import cz.metaverse.android.bilingualreader.ReaderActivity;
import cz.metaverse.android.bilingualreader.dialog.PanelSizeDialog;
import cz.metaverse.android.bilingualreader.manager.PanelNavigator;
import cz.metaverse.android.bilingualreader.panel.BookPanel;
import cz.metaverse.android.bilingualreader.selectionwebview.SelectionWebView;

/**
 *
 *                                                  /===-_---~~~~~~~~~------____
 *                                                 |===-~___                _,-'
 *                  -==\\                         `//~\\   ~~~~`---.___.-~~
 *              ______-==|                         | |  \\           _-~`
 *        __--~~~  ,-/-==\\                        | |   `\        ,'
 *     _-~       /'    |  \\                      / /      \      /
 *   .'        /       |   \\                   /' /        \   /'
 *  /  ____  /         |    \`\.__/-~~ ~ \ _ _/'  /          \/'
 * /-'~    ~~~~~---__  |     ~-/~         ( )   /'        _--~`
 *                   \_|      /        _)   ;  ),   __--~~
 *                     '~~--_/      _-~/-  / \   '-~ \
 *                    {\__--_/}    / \\_>- )<__\      \
 *                    /'   (_/  _-~  | |__>--<__|      |
 *                   |0  0 _/) )-~     | |__>--<__|     |
 *                   / /~ ,_/       / /__>---<__/      |
 *                  o o _//        /-~_>---<__-~      /
 *                  (^(~          /~_>---<__-      _-~
 *                 ,/|           /__>--<__/     _-~
 *              ,//('(          |__>--<__|     /                  .----_
 *             ( ( '))          |__>--<__|    |                 /' _---_~\
 *          `-)) )) (           |__>--<__|    |               /'  /     ~\`\
 *         ,/,'//( (             \__>--<__\    \            /'  //        ||
 *       ,( ( ((, ))              ~-__>--<_~-_  ~--____---~' _/'/        /'
 *     `~/  )` ) ,/|                 ~-_~>--<_/-__       __-~ _/
 *   ._-~//( )/ )) `                    ~~-'_/_/ /~~~~~~~__--~
 *    ;'( ')/ ,)(                              ~~~~~~~~~~
 *   ' ') '( (/
 *     '   '  `
 *
 *                       HERE BE DRAGONS
 *
 *
 *
 * This class serves as a OnTouchListener for our BookPanel.
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
 * Be warned, editing this class is perilous. Editing one functionality can have
 * unforeseen adverse effects in other areas. Be sure to test your changes profusely.
 *
 *
 */
public class BookPanelOnTouchListener
		implements OnTouchListener, GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {

	protected static final String LOG = "alfons";

	/* Variables for interconnectivity with the outside world. */
	private ReaderActivity activity;
	private PanelNavigator navigator;
	private BookPanel bookPanel;
	private SelectionWebView webView;

	/* Pre-calculated dimensions. */
	protected int screenWidth;
	protected int screenHeight;
	protected int quarterWidth;
	protected int quarterHeight;

	/*
	 * Fields for touch/gesture events.
	 */
	protected GestureDetectorCompat gestureDetector;

	/* For scrolling gesture events. */
	protected boolean scrollIsOrWasMultitouch;
	protected float scrollOriginX;
	protected float scrollOriginY;

	/* For DoubleTapEvent gestures. */
	private boolean isDoubleTapSwipe;
	private float doubleTapOriginX;
	private float doubleTapOriginY;
	private boolean doubleTapSwipeEscapedBounds;
	private float newPanelsWeight;
	private int doubleTapSwipe_contentStartsAtHeight;
	private int doubleTapSwipe_viewHeight;
	private int doubleTapSwipe_orientation;


	public BookPanelOnTouchListener(ReaderActivity readerActivity, PanelNavigator navigator,
			BookPanel bookPanel, SelectionWebView webView) {

		// Interconnectivity.
		this.activity = readerActivity;
		this.navigator = navigator;
		this.bookPanel = bookPanel;
		this.webView = webView;

		// Instantiate the gesture detector.
		gestureDetector = new GestureDetectorCompat(activity, this);
		gestureDetector.setOnDoubleTapListener(this);

		// Get the screen size.
		DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
		screenWidth = metrics.widthPixels;
		screenHeight = metrics.heightPixels;
		quarterWidth = (int) (screenWidth * 0.25);
		quarterHeight = (int) (screenHeight * 0.25);
	}


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

			// If the swipe was over 1/8 of the screen high and it was higher than wider.
			if (doubleTapSwipeEscapedBounds ||
					// Bounds for PORTRAIT orientation.
					(absDiffY * 2 > quarterHeight && absDiffY > absDiffX
							&& doubleTapSwipe_orientation == Configuration.ORIENTATION_PORTRAIT) ||
					// Bounds for LANDSCAPE orientation.
					(absDiffX * 2 > quarterWidth && absDiffX > absDiffY
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
				navigator.changePanelsWeight(newPanelsWeight);

				Log.d(LOG, "doubleTapSwipe " + newPanelsWeight);
				//+ " = (" + event.getRawY() + " - " + contentViewTop + ") / " + (height - contentViewTop));
			}
		}

		/*
		 * Handle ACTION_UP event for scrolling, because onScroll (almost?) never gets
		 * called with the last MotionEvent.ACTION_UP.
		 * Don't forget to mirror any changes made here in the onScroll() method as well, to be safe.
		 */
		if(MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_UP) {
			Log.d(LOG, "[" + bookPanel.getIndex() + "] OnTouchListener --> onTouch ACTION_UP");

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
		Log.d(LOG, "[" + bookPanel.getIndex() + "] onDown"); //: " + event.toString());

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
		if (navigator.isScrollSync()) {
			// The user laid finger in this WebView and may start scrolling it.
			//  Therefore send resumeScrollSync to both WebViews, the one that was active in User Scrolling
			//   will compute new offset and send it to the other one.
			//  Afterwards activate User Scrolling in this WebView and deactivate it in the sister WebView.
			webView.resumeScrollSync();
			webView.setUserIsScrolling(true);

			BookPanel otherBookPanel = activity.navigator.getSisterBookPanel(bookPanel.getIndex());
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
		Log.d(LOG, "[" + bookPanel.getIndex() + "] onFling"); //: " + event1.toString()+event2.toString());

		// Evaluation of the end of the scroll is handled directly in onTouch for consistent results.

		return true;
	}

	/**
	 * Gets called when the user scrolls, but not when he double taps and swipes/scrolls.
	 */
	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		//Log.d(LOG, "[" + bookPanel.getIndex() + "] onScroll"); //: " + e2.toString());

		/* Change from single to a Multi-touch event */
		if (e2.getPointerCount() > 1 && !scrollIsOrWasMultitouch) {
			Log.d(LOG, "[" + bookPanel.getIndex() + "] onScroll: Change from single to a Multitouch event");

			// Evaluate if the scroll section that has just ended constitutes some gesture.
			handleScrollEnd(e2);

			if (navigator.isScrollSync() && webView.isUserScrolling()) {
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
			Log.d(LOG, "[" + bookPanel.getIndex() + "] onScroll: Change from multi to a Singletouch event detected - doing nothing.");
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
			if (bookPanel.enumState == PanelViewState.books) {

				// Single touch scroll on a book - evaluate if the user wants to change chapters.
				if (diffX > quarterWidth && absDiffX > absDiffY) {
					// Next chapter - If the swipe was to the right, over 1/4 of the screen wide,
					//  and more broad than high.
					try {
						navigator.goToNextChapter(bookPanel.getIndex());
					} catch (Exception e) {
						activity.errorMessage(activity.getString(R.string.error_cannotTurnPage));
					}
				} else if (diffX < -quarterWidth && absDiffX > absDiffY) {
					// Previous chapter - If the swipe was to the left, over 1/4 of the screen wide,
					//  and more broad than high.
					try {
						navigator.goToPrevChapter(bookPanel.getIndex());
					} catch (Exception e) {
						activity.errorMessage(activity.getString(R.string.error_cannotTurnPage));
					}
				}
			}
		}

		/* Disabled because it was getting confused with two-finger scrolling gesture,
		 * and because this functionality is already covered by doubleTapSwipe.
		// Multi-touch scroll sideways:
		if (scrollIsMultitouch) {
			if (absDiffX > quarterWidth && absDiffX > absDiffY) {
				// Hide panel -
				//  If the swipe was over 1/4 of the screen wide, and more broad than high.
				Toast.makeText(ReaderActivity.debugContext, "Hiding panel - by multitouch scroll.", Toast.LENGTH_SHORT).show();
			}
		}*/
	}

	@Override
	public boolean onDoubleTap(MotionEvent event) {
		Log.d(LOG, "[" + bookPanel.getIndex() + "] onDoubleTap"); //: " + event.toString());

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
		Log.d(LOG, "[" + bookPanel.getIndex() + "] onDoubleTapEvent"); //: " + event.toString());

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

						// PORTRAIT: If the swipe was over 1/4 of the screen wide and it was wider than higher.
				if (	(absDiffX > quarterWidth && absDiffX > absDiffY
								&& doubleTapSwipe_orientation == Configuration.ORIENTATION_PORTRAIT) ||
						// LANDSCAPE: If the swipe was over 1/4 of the screen high and it was higher than wider.
						(absDiffY > quarterHeight && absDiffY > absDiffX
								&& doubleTapSwipe_orientation != Configuration.ORIENTATION_PORTRAIT)) {

					// Hide panel.
					Toast.makeText(ReaderActivity.debugContext, "Hiding panel - by DoubleTap swipe.", Toast.LENGTH_SHORT).show();
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
		Log.d(LOG,"[" + bookPanel.getIndex() + "] onSingleTapConfirmed"); //: " + event.toString());

		if (!webView.inSelectionActionMode()) {
			activity.switchFullscreen();
		}
		return true;
	}

	@Override
	public boolean onSingleTapUp(MotionEvent event) {
		Log.d(LOG,"[" + bookPanel.getIndex() + "] onSingleTapUp"); //: " + event.toString());
		return true;
	}

	@Override
	public void onLongPress(MotionEvent event) {
		Log.d(LOG, "[" + bookPanel.getIndex() + "] onLongPress"); //: " + event.toString());
	}

	@Override
	public void onShowPress(MotionEvent event) {
		Log.d(LOG, "[" + bookPanel.getIndex() + "] onShowPress"); //: " + event.toString());
	}

}
