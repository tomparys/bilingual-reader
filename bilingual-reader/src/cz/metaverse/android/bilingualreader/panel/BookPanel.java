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
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import cz.metaverse.android.bilingualreader.R;
import cz.metaverse.android.bilingualreader.ReaderActivity;
import cz.metaverse.android.bilingualreader.helper.BookPanelOnTouchListener;
import cz.metaverse.android.bilingualreader.helper.PanelViewState;
import cz.metaverse.android.bilingualreader.selectionwebview.SelectionWebView;

/**
 *
 * An Extension of the SplitPanel Panel specialized in visualizing EPUB pages.
 *
 */
public class BookPanel extends SplitPanel {

	private ReaderActivity activity;

	// Information about the content
	public PanelViewState enumState = PanelViewState.books;
	protected String displayedPage;
	protected String displayedData;

	// Position within the page loaded from before
	protected Integer loadPositionX, loadPositionY;

	// Our customized WebView
	protected SelectionWebView webView;


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

		// Set our custom complex OnTouchListener class that will handle
		// the multitude of supported touch and multi-touch gestures for our BookPanel.
		BookPanelOnTouchListener onTouchListener =
				new BookPanelOnTouchListener(activity, navigator, this, webView);
		webView.setOnTouchListener(onTouchListener);

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
			webView.resetScrollSync();

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
			webView.resetScrollSync();
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

}
