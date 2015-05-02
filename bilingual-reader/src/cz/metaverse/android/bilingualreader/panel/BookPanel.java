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
import android.util.Log;
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
import cz.metaverse.android.bilingualreader.manager.PanelHolder;
import cz.metaverse.android.bilingualreader.manager.PanelNavigator;
import cz.metaverse.android.bilingualreader.selectionwebview.SelectionWebView;

/**
 *
 * An Extension of the SplitPanel Panel specialized in visualizing EPUB pages.
 *
 */
public class BookPanel extends SplitPanel {

	private static final String LOG = "hugo";

	private ReaderActivity activity;

	// Information about the content
	public PanelViewState enumState = PanelViewState.books;
	protected String displayedPage;
	protected String displayedData;

	// Position within the page loaded from before
	protected Integer loadPositionX, loadPositionY;

	// Our customized WebView and its onTouchListener
	protected SelectionWebView webView;
	private BookPanelOnTouchListener onTouchListener;


	/**
	 * Constructor for our BookPanel.
	 * @param navigator  The Governor of our application.
	 * @param panelHolder  PanelHolder instance that's holding our panel.
	 * @param position  Position of this panel.
	 */
	public BookPanel(PanelNavigator navigator, PanelHolder panelHolder, int position) {
		super(navigator, panelHolder, position); // Invokes changePosition(position)
		Log.d(LOG, "New BookPanel (note. constructor)");
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)	{
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
		Log.d(LOG, "BookPanel onActivityCreated");
		super.onActivityCreated(saved);

		// Our panels are designed to work strictly with our ReaderActivity.
		activity = (ReaderActivity) getActivity();

		// Find our customized web view that will server as our viewport
		webView = (SelectionWebView) getView().findViewById(R.id.Viewport);
		webView.setPanelHolderAndPosition(panelHolder, panelPosition);
		// Enable JavaScript for cool things to happen!
		webView.getSettings().setJavaScriptEnabled(true);

		onTouchListener = new BookPanelOnTouchListener(activity, navigator, panelHolder, this, webView, panelPosition);
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
							panelHolder.setNote(url);
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
					panelHolder.setBookPage(url);
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

		Log.d(LOG, "BookPanel onActivityCreated finished");
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
	 * Update the position of the panel.
	 */
	@Override
	public void updatePosition(int position) {
		super.updatePosition(position);

		// Pass the new index onto classes that work with it.
		if (webView != null) {
			webView.updatePanelPosition(position);
			onTouchListener.updatePanelPosition(position);
		}
	}


	// ============================================================================================
	//		Display page or HTML data
	// ============================================================================================

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


	// ============================================================================================
	//		Save and load state to preferences
	// ============================================================================================

	/**
	 * Save state and content of the page.
	 */
	@Override
	public void saveState(Editor editor) {
		super.saveState(editor);
		if (enumState != null) {
			editor.putString("state"+panelPosition, enumState.name());
		}
		if (displayedPage != null) {
			editor.putString("displayedPage"+panelPosition, displayedPage);
		}
		if (enumState == PanelViewState.metadata) {
			editor.putString("displayedData"+panelPosition, displayedData);
		}

		// Save the position within the page.
		if (webView != null) {
			editor.putInt("positionX"+panelPosition, webView.getScrollX());
			editor.putInt("positionY"+panelPosition, webView.getScrollY());
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
			enumState = PanelViewState.valueOf(preferences.getString("state"+panelPosition, PanelViewState.books.name()));
		} catch (IllegalArgumentException e) {
			enumState = PanelViewState.books;
		}

		// If this is one of the BookPanelStates that get closed by pressing the back button,
		// inform navigator of that fact so it can close it properly.
		if (enumState == PanelViewState.notes || enumState == PanelViewState.metadata) {
			navigator.notesDisplayedLastIn = panelHolder;
		}

		String page = preferences.getString("displayedPage"+panelPosition, "");

		if (enumState == PanelViewState.metadata) {
			loadData(preferences.getString("displayedData"+panelPosition, ""), page);
		} else {
			loadPage(page);
		}

		// Load the position within the page from before to be used when webView is instantiated.
		loadPositionX = preferences.getInt("positionX"+panelPosition, 0);
		loadPositionY = preferences.getInt("positionY"+panelPosition, 0);
	}

}
