/*
The MIT License (MIT)

Copyright (c) 2013, V. Giacometti, M. Giuriato, B. Petrantuono

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

package cz.metaverse.android.bilingualreader.panel;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import cz.metaverse.android.bilingualreader.R;
import cz.metaverse.android.bilingualreader.ReaderActivity;
import cz.metaverse.android.bilingualreader.helper.PanelViewState;
import cz.metaverse.android.bilingualreader.selectionwebview.SelectionWebView;

/**
 *
 * An Extension of the SplitPanel Panel specialized in visualizing EPUB pages.
 *
 */
public class BookPanel extends SplitPanel
		implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {

	private ReaderActivity activity;

	// Information about the content
	public PanelViewState enumState = PanelViewState.books;
	protected String displayedPage;
	protected String displayedData;

	// Position within the page loaded from before
	protected Integer loadPositionX, loadPositionY;

	// Our customized WebView
	protected SelectionWebView webView;

	/* Fields for touch/gesture events. */
	protected static final String DEBUG_TAG = "alfons";
	protected GestureDetectorCompat gestureDetector;

	// For scrolling gesture events.
	protected boolean scrollIsMultitouch = false;
	protected float scrollOriginX;
	protected float scrollOriginY;

	// For DoubleTapEvent gestures.
	private float doubleTapOriginX;
	private float doubleTapOriginY;



	@Override
	public View onCreateView(LayoutInflater inflater,ViewGroup container,Bundle savedInstanceState)	{
		super.onCreateView(inflater, container, savedInstanceState);
		View v = inflater.inflate(R.layout.panel_book, container, false);
		return v;
	}

	/**
	 * onActivityCreated()
	 */
	@SuppressLint("SetJavaScriptEnabled") // Our opensource application has literally nothing to hide.
	@Override
	public void onActivityCreated(Bundle saved) {
		super.onActivityCreated(saved);

		// Our panels are designed to work strictly with our ReaderActivity.
		activity = (ReaderActivity) getActivity();

		// Find our customized web view that will server as our viewport
		webView = (SelectionWebView) getView().findViewById(R.id.Viewport);
		webView.setPanelIndex(index);
		// Enable JavaScript for cool things to happen!
		webView.getSettings().setJavaScriptEnabled(true);

		// Instantiate the gesture detector.
		gestureDetector = new GestureDetectorCompat(getActivity(), this);
		gestureDetector.setOnDoubleTapListener(this);

		// Set touch listener with the option to SWIPE pages.
		webView.setOnTouchListener(new OnTouchListener() {
			@SuppressLint("ClickableViewAccessibility") // The "performClick" method is launched inside the
			@Override									// handleWebViewTouch method, which eclipse fails to realize.
			public boolean onTouch(View view, MotionEvent motionEvent) {
				// Call a method that will evaluate the touch/swipe.
				handleWebViewTouch(view, motionEvent);

				return view.onTouchEvent(motionEvent);
			}
		});

		// Set long-click listener:
		//  If the user long-clicks on any URL link we open it in the other panel.
		webView.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				// Create a message and a method that will serve as its target.
				// The target method will look for URL in the messages data,
				//  and if there is one, it dispatches it to the PanelsNavigator
				//  to open it as a Note in the other panel.
				Message msg = new Message();
				msg.setTarget(new Handler() {
					@Override
					public void handleMessage(Message msg) {
						super.handleMessage(msg);

						// Extract url from the message
						String url = msg.getData().getString(getString(R.string.url));

						// If url isn't empty, set note with the url into this SplitPanel/fragment.
						if (url != null) {
							navigator.setNote(url, index);
						}
					}
				});

				// Puts data about the last pressed object into the message and dispatches it.
				//  If the user didn't click on anything, message is not dispatched.
				webView.requestFocusNodeHref(msg);

				return false;
			}
		});

		// Set a custom WebViewClient that has overwritten method for loading URLs.
		webView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				// Set book page through the navigator if possible.
				try {
					navigator.setBookPage(url, index);
				} catch (Exception e) {
					errorMessage(getString(R.string.error_LoadPage));
				}
				return true;
			}
		});

		// Load the page.
		if (enumState == PanelViewState.metadata) {
			loadData(displayedData, displayedPage);
		} else {
			loadPage(displayedPage);
		}
	}

	/**
	 * Returns the URL of the currently displayed page.
	 */
	public String getViewedPage() {
		return displayedPage;
	}

	/**
	 * Returns this panel's SelectionWebView.
	 */
	public SelectionWebView getWebView() {
		return webView;
	}

	/**
	 * Load page through URL path.
	 * @param path to load
	 */
	public void loadPage(String path) {
		displayedPage = path;

		if(created) {
			webView.loadUrl(path);

			// Load position from before if this is a page opening from before.
			if (loadPositionX != null && loadPositionY != null) {
				webView.setScrollX(loadPositionX);
				webView.setScrollY(loadPositionY);
				loadPositionX = null;
				loadPositionY = null;
			}
		}
	}

	/**
	 * Loads text data into the WebView.
	 * @param data 		String data to display
	 * @param baseUrl	URL of any file from the associated epub to get proper encoding from it.
	 */
	public void loadData(String data, String baseUrl) {
		displayedPage = baseUrl;
		displayedData = data;

		if (created) {
			webView.loadDataWithBaseURL(baseUrl, data,
					getActivity().getApplicationContext().getResources().getString(R.string.textOrHTML),
					null, null);
		}
	}

	/**
	 * Save state and content of the page.
	 */
	@Override
	public void saveState(Editor editor) {
		super.saveState(editor);
		if (enumState != null) {
			editor.putString("state"+index, enumState.name());
		}
		if (displayedPage != null) {
			editor.putString("displayedPage"+index, displayedPage);
		}
		if (enumState == PanelViewState.metadata) {
			editor.putString("displayedData"+index, displayedData);
		}

		// Save the position within the page.
		if (webView != null) {
			editor.putInt("positionX"+index, webView.getScrollX());
			editor.putInt("positionY"+index, webView.getScrollY());
		}
	}

	/**
	 * Load state and content of the page.
	 */
	@Override
	public void loadState(SharedPreferences preferences)
	{
		super.loadState(preferences);
		try {
			enumState = PanelViewState.valueOf(preferences.getString("state"+index, PanelViewState.books.name()));
		} catch (IllegalArgumentException e) {
			enumState = PanelViewState.books;
		}

		String page = preferences.getString("displayedPage"+index, "");

		if (enumState == PanelViewState.metadata) {
			loadData(preferences.getString("displayedData"+index, ""), page);
		} else {
			loadPage(page);
		}

		// Load the position within the page from before to be used when webView is instantiated.
		loadPositionX = preferences.getInt("positionX"+index, 0);
		loadPositionY = preferences.getInt("positionY"+index, 0);
	}



	// ============================================================================================
	//		Touches and Gesture Detector
	// ============================================================================================

	/**
	 * Evaluates the touch/swipe event and changes page if appropriate.
	 * @param view The WebView where the swipe took place
	 * @param event The MotionEvent of the swipe
	 */
	protected void handleWebViewTouch(View view, MotionEvent event) {
		// Provide data to the GestureDetector.
		gestureDetector.onTouchEvent(event);

		view.performClick(); // Android system mandates we pass the baton to the onClick listener now.
	}

	/**
	 * First finger was put on the screen.
	 */
	@Override
	public boolean onDown(MotionEvent event) {
		Log.d(DEBUG_TAG,"onDown"); //: " + event.toString());

		scrollOriginX = event.getX();
		scrollOriginY = event.getY();

		if (navigator.isScrollSync()) {
			// The user laid finger in this WebView and may start scrolling it.
			// Therefore activate User Scrolling in this WebView and deactivate it in the sister WebView.
			webView.setUserIsScrolling(true);

			BookPanel otherBookPanel = activity.navigator.getSisterBookPanel(index);
			if (otherBookPanel != null) {
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
		Log.d(DEBUG_TAG, "onFling"); //: " + event1.toString()+event2.toString());

		// Evaluate if the scroll that has just ended constitutes some gesture.
		handleScrollEnd(e2);

		return true;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		//Log.d(DEBUG_TAG, "onScroll"); //: " + e1.toString()+e2.toString());

		if (e2.getPointerCount() > 1 && !scrollIsMultitouch) {
			/* Change from single to a Multitouch event */
			// Evaluate if the scroll section that has just ended constitutes some gesture.
			handleScrollEnd(e2);

			// Start a new scroll section.
			scrollIsMultitouch = true;
			scrollOriginX = e2.getX();
			scrollOriginY = e2.getY();

		    Log.d(DEBUG_TAG,"Change from single to a Multitouch event");
		}
		else if (e2.getPointerCount() <= 1 && scrollIsMultitouch) {
			/* Change from multi to a Singletouch event */
			// Evaluate if the scroll section that has just ended constitutes some gesture.
			handleScrollEnd(e2);

			// Start a new scroll section.
			scrollIsMultitouch = false;
			scrollOriginX = e2.getX();
			scrollOriginY = e2.getY();

		    Log.d(DEBUG_TAG,"Change from multi to a Singletouch event");
		}

		if (MotionEventCompat.getActionMasked(e2) == MotionEvent.ACTION_UP) {
			// Evaluate if the scroll that has just ended constitutes some gesture.
			handleScrollEnd(e2);
		}
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

		if (!scrollIsMultitouch) {
			// Scroll is single touch - do nothing if we're not displaying a book.
			if (enumState == PanelViewState.books) {
				// Single touch scroll on a book - evaluate if the user wants to change chapters.
				if (diffX > quarterWidth && absDiffX > absDiffY) {
					// Next chapter - If the swipe was to the right, over 1/4 of the screen wide,
					//  and more broad than high.
					try {
						navigator.goToNextChapter(index);
					} catch (Exception e) {
						errorMessage(getString(R.string.error_cannotTurnPage));
					}
				} else if (diffX < -quarterWidth && absDiffX > absDiffY) {
					// Previous chapter - If the swipe was to the left, over 1/4 of the screen wide,
					//  and more broad than high.
					try {
						navigator.goToPrevChapter(index);
					} catch (Exception e) {
						errorMessage(getString(R.string.error_cannotTurnPage));
					}
				}
			} else {
				// Multitouch scroll
				if (absDiffX > quarterWidth && absDiffX > absDiffY) {
					// Hide panel -
					//  If the swipe was over 1/4 of the screen wide, and more broad than high.
					Toast.makeText(ReaderActivity.debugContext, "Hiding panel - by multitouch scroll.", Toast.LENGTH_SHORT).show();
				}
			}
		}

		scrollIsMultitouch = false;
	}

	/**
	 * onSingleTapConfirmed - Activate/deactivate immersive fullscreen mode.
	 */

	@Override
	public boolean onDoubleTap(MotionEvent event) {
		Log.d(DEBUG_TAG, "onDoubleTap"); //: " + event.toString());

		doubleTapOriginX = event.getX();
		doubleTapOriginY = event.getY();
		return true;
	}

	/**
	 * Called when the user double taps and then swipes his finger away on the second tap.
	 */
	@Override
	public boolean onDoubleTapEvent(MotionEvent event) {
		//Log.d(DEBUG_TAG, "onDoubleTapEvent"); //: " + event.toString());

		// If the finger has finally risen.
		if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_UP) {
			Log.d(DEBUG_TAG, "onDoubleTapEvent ACTION_UP"); //: " + event.toString());

			float absDiffX = Math.abs(doubleTapOriginX - event.getX());
			float absDiffY = Math.abs(doubleTapOriginY - event.getY());

			if (absDiffX > quarterWidth || absDiffY > quarterHeight) {
				// Hide panel -
				//  If the swipe was over 1/4 of the screen wide or over 1/4 of the screen high.
				Toast.makeText(ReaderActivity.debugContext, "Hiding panel - by DoubleTap swipe.", Toast.LENGTH_SHORT).show();
			}
		}

		return true;
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent event) {
		Log.d(DEBUG_TAG,"onSingleTapConfirmed"); //: " + event.toString());

		if (!webView.inSelectionActionMode()) {
			activity.switchFullscreen();
		}
		return true;
	}

	@Override
	public boolean onSingleTapUp(MotionEvent event) {
		Log.d(DEBUG_TAG,"onSingleTapUp"); //: " + event.toString());
		return true;
	}

	@Override
	public void onLongPress(MotionEvent event) {
		Log.d(DEBUG_TAG, "onLongPress"); //: " + event.toString());
	}

	@Override
	public void onShowPress(MotionEvent event) {
		Log.d(DEBUG_TAG, "onShowPress"); //: " + event.toString());
	}

}
