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
import android.support.v4.view.MotionEventCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import cz.metaverse.android.bilingualreader.R;
import cz.metaverse.android.bilingualreader.helper.PanelViewStateEnum;
import cz.metaverse.android.bilingualreader.selectionwebview.SelectionWebView;

/**
 *
 * An Extension of the SplitPanel Panel specialized in visualizing EPUB pages.
 *
 */
public class BookPanel extends SplitPanel {
	public PanelViewStateEnum enumState = PanelViewStateEnum.books;
	protected String viewedPage;
	protected SelectionWebView webView;
	protected float swipeOriginX, swipeOriginY;
	// Position within the page loaded from before
	protected Integer loadPositionX, loadPositionY;

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

		// Find our customized web view that will server as our viewport
		webView = (SelectionWebView) getView().findViewById(R.id.Viewport);

		// Enable JavaScript for cool things to happen!
		webView.getSettings().setJavaScriptEnabled(true);

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

		// Set long-click listener - NOTE & LINK
		webView.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {

				// Create a message and its handler
				Message msg = new Message();

				msg.setTarget(new Handler() {

					// A handler for the message
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
		loadPage(viewedPage);
	}

	/**
	 * Returns the URL of the currently displayed page.
	 */
	public String getViewedPage() {
		return viewedPage;
	}

	/**
	 * Load page through URL path.
	 * @param path to load
	 */
	public void loadPage(String path)
	{
		viewedPage = path;
		if(created) {

			// TODO Estimate scroll position of each paragraph for proper synchronized scrolling.

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
	 * Evaluates the touch/swipe event and changes page if appropriate.
	 * @param v The WebView where the swipe took place
	 * @param event The MotionEvent of the swipe
	 */
	protected void handleWebViewTouch(View v, MotionEvent event) {
		// Analyze the motion event.
		int action = MotionEventCompat.getActionMasked(event);

		switch (action) {
		// Finger was just laid on the screen - save the original coordinates.
		case (MotionEvent.ACTION_DOWN):
	        if (enumState == PanelViewStateEnum.books) {
				swipeOriginX = event.getX();
				swipeOriginY = event.getY();
	        }
			break;

		// Finger was just lifted - calculate what kind of swipe has it been.
		case (MotionEvent.ACTION_UP):
	        if (enumState == PanelViewStateEnum.books) {
				int quarterWidth = (int) (screenWidth * 0.25); // A quarter of the screen's width
				float diffX = swipeOriginX - event.getX();
				float diffY = swipeOriginY - event.getY();
				float absDiffX = Math.abs(diffX);
				float absDiffY = Math.abs(diffY);

				if ((diffX > quarterWidth) && (absDiffX > absDiffY)) {
					// If swipe was to the left and over 1/4 of the screen wide,
					// 		and swipe was more broad than high
					try {
						navigator.goToNextChapter(index);
					} catch (Exception e) {
						errorMessage(getString(R.string.error_cannotTurnPage));
					}
				} else if ((diffX < -quarterWidth) && (absDiffX > absDiffY)) {
					// If swipe was to the right and over 1/4 of the screen wide,
					// 		and swipe was more broad than high
					try {
						navigator.goToPrevChapter(index);
					} catch (Exception e) {
						errorMessage(getString(R.string.error_cannotTurnPage));
					}
				}
	        }

	        v.performClick(); // Android system mandates we pass the baton to the onClick listener now.
			break;
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
		if (viewedPage != null) {
			editor.putString("page"+index, viewedPage);
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
		enumState = PanelViewStateEnum.valueOf(preferences.getString("state"+index, PanelViewStateEnum.books.name()));
		loadPage(preferences.getString("page"+index, ""));

		// Load the position within the page from before to be used when webView is instantiated.
		loadPositionX = preferences.getInt("positionX"+index, 0);
		loadPositionY = preferences.getInt("positionY"+index, 0);
	}

}
