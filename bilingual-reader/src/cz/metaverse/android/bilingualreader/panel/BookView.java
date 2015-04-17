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

import cz.metaverse.android.bilingualreader.R;
import cz.metaverse.android.bilingualreader.R.id;
import cz.metaverse.android.bilingualreader.R.layout;
import cz.metaverse.android.bilingualreader.R.string;
import cz.metaverse.android.bilingualreader.helper.ViewStateEnum;
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

/**
 * 
 * An Extension of the SplitPanel Panel specialized in visualizing EPUB pages.
 *
 */
public class BookView extends SplitPanel {
	public ViewStateEnum enumState = ViewStateEnum.books;
	protected String viewedPage;
	protected WebView webView;
	protected float swipeOriginX, swipeOriginY;
	
	@Override
	public View onCreateView(LayoutInflater inflater,ViewGroup container,Bundle savedInstanceState)	{
		super.onCreateView(inflater, container, savedInstanceState);
		View v = inflater.inflate(R.layout.activity_book_view, container, false);
		return v;
	}
	
	/**
	 * onActivityCreated()
	 */
	@Override
    public void onActivityCreated(Bundle saved) {
		super.onActivityCreated(saved);
		webView = (WebView) getView().findViewById(R.id.Viewport);
		
		// Enable JavaScript for cool things to happen!
		webView.getSettings().setJavaScriptEnabled(true);
		
		// Set touch listener with the option to SWIPE pages.
		webView.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
	
				if (enumState == ViewStateEnum.books)
					// Call mothod that will evaluate the swipe and swipes the page if appropriate. 
					swipePage(v, event, 0);
								
				WebView view = (WebView) v;
				return view.onTouchEvent(event);
			}
		});

		// Set long-click listener - NOTE & LINK
		webView.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				// Create a message
				Message msg = new Message();
				msg.setTarget(new Handler() {
					// New handler for the message
					@Override
					public void handleMessage(Message msg) {
						super.handleMessage(msg);
						// Extract url from the message
						String url = msg.getData().getString(
								getString(R.string.url));
						// If url isn't empty, set note with the url into this SplitPanel/fragment.
						if (url != null)
							navigator.setNote(url, index);
					}
				});
				// Puts data about the last pressed object into the message.
				webView.requestFocusNodeHref(msg);
				
				return false;
			}
		});
		
		// Set a custom WebViewClient that has overwritten method for loading URLs. 
		webView.setWebViewClient(new WebViewClient() {
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
	 * Load page through URL path.
	 * @param path to load
	 */
	public void loadPage(String path)
	{
		viewedPage = path;
		if(created)
			webView.loadUrl(path);
	}
	
	/**
	 * Evaluates a swipe action and changes page if appropriate.
	 * @param v The WebView where the swipe took place
	 * @param event The MotionEvent of the swipe
	 * @param book TODO
	 */
	protected void swipePage(View v, MotionEvent event, int book) {
		// Analyze the motion event.
		int action = MotionEventCompat.getActionMasked(event);

		switch (action) {
		// Finger was just laid on the screen - save the original coordinates.
		case (MotionEvent.ACTION_DOWN):
			swipeOriginX = event.getX();
			swipeOriginY = event.getY();
			break;

		// Finger was just lifted - calculate what kind of swipe has it been.
		case (MotionEvent.ACTION_UP):
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
			break;
		}

	}
	
	/**
	 * Save state and content of the page.
	 */
	@Override
	public void saveState(Editor editor) {
		super.saveState(editor);
		editor.putString("state"+index, enumState.name());
		editor.putString("page"+index, viewedPage);
	}
	
	/**
	 * Load state and content of the page.
	 */
	@Override
	public void loadState(SharedPreferences preferences)
	{
		super.loadState(preferences);
		enumState = ViewStateEnum.valueOf(preferences.getString("state"+index, ViewStateEnum.books.name()));
		loadPage(preferences.getString("page"+index, ""));
	}
	
}
