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


This file incorporates work covered by the following copyright and permission notice:


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
import cz.metaverse.android.bilingualreader.db.BookPageDB;
import cz.metaverse.android.bilingualreader.db.BookPageDB.BookPage;
import cz.metaverse.android.bilingualreader.enums.BookPanelState;
import cz.metaverse.android.bilingualreader.helper.BookPanelOnTouchListener;
import cz.metaverse.android.bilingualreader.helper.Func;
import cz.metaverse.android.bilingualreader.manager.Governor;
import cz.metaverse.android.bilingualreader.manager.PanelHolder;
import cz.metaverse.android.bilingualreader.selectionwebview.SelectionWebView;

/**
 *
 * An Extension of the SplitPanel Panel specialized in visualizing EPUB pages.
 *
 */
public class BookPanel extends SplitPanel {

	private static final String LOG = "BookPanel";

	private ReaderActivity activity;

	// Prepared static variables for interaction with the DB. DB columns to be updated.
	private static final String[] colsUpdate = new String[] {BookPageDB.COL_SCROLL_Y,
			BookPageDB.COL_SCROLLSYNC_OFFSET, BookPageDB.COL_LAST_OPENED};

	private static final String[] colsInsert = new String[] {BookPageDB.COL_BOOK_FILENAME,
			BookPageDB.COL_BOOK_TITLE, BookPageDB.COL_PAGE_FILENAME, BookPageDB.COL_PAGE_RELATIVE_PATH,
			BookPageDB.COL_SCROLL_Y, BookPageDB.COL_SCROLLSYNC_OFFSET, BookPageDB.COL_LAST_OPENED};

	// Information about the content
	public BookPanelState enumState = BookPanelState.books;
	protected String displayedPage;
	protected String displayedData;

	// Position within the page and ScrollSync offset loaded from before.
	private Float loadPositionY;
	private Float loadScrollSyncOffset;

	// Fields concerning communication with the DB
	private Long loadedFromBookPageRowId;
	private String[] displayedBookPageKey;

	// Whether the current page has been fully rendered.
	private boolean finishedRenderingContent = false;

	// Our customized WebView and its onTouchListener
	protected SelectionWebView webView;
	private BookPanelOnTouchListener onTouchListener;


	/**
	 * Constructor for our BookPanel.
	 * @param governor  The Governor of our application.
	 * @param panelHolder  PanelHolder instance that's holding our panel.
	 * @param position  Position of this panel.
	 */
	public BookPanel(Governor governor, PanelHolder panelHolder, int position) {
		super(governor, panelHolder, position); // Invokes changePosition(position)

		Log.d(LOG, "BookPanel.new BookPanel (note. constructor)");
	}

	/**
	 * Checks whether there are any problems with this instance, if for example the Android system
	 * didn't close any important fields that would result in NullPointerExceptions.
	 * @return true if everything appears to be sound
	 */
	@Override
	public boolean selfCheck(boolean creatingActivity) {
		// If the ReaderActivity is being recreated due to a runtime change (e.g. switch to/from landscape),
		// than this BookPanel has persisted and is still open. But since we need this BookPage to act
		// like it's just being created, we have to set finishedRenderingContent=false.
		// Otherwise it would screw up in loadData() and loadPage() where it would null out
		// loadPositionY and loadScrollSyncOffset, which are to be nulled out only after switching to a new
		// page, not after opening the first, due to how loading these values in loadState() works.
		//   - For more information see comment inside loadPage().
		if (creatingActivity) {
			finishedRenderingContent = false;
		}

		boolean ok = super.selfCheck(creatingActivity) && webView != null && onTouchListener != null && enumState != null;

		Log.d(LOG, "BookPanel.selfCheck - " + ok);
		return ok;
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
		//Log.d(LOG, "BookPanel onActivityCreated");
		super.onActivityCreated(saved);

		// Our panels are designed to work strictly with our ReaderActivity.
		activity = (ReaderActivity) getActivity();

		// Find our customized web view that will server as our viewport
		webView = (SelectionWebView) getView().findViewById(R.id.Viewport);
		webView.setPanelHolderAndPosition(panelHolder, panelPosition);
		// Enable JavaScript for cool things to happen!
		webView.getSettings().setJavaScriptEnabled(true);

		onTouchListener = new BookPanelOnTouchListener(activity, governor, panelHolder, this, webView, panelPosition);
		webView.setOnTouchListener(onTouchListener);

		// Set a custom WebViewClient that has overwritten method for loading URLs.
		webView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				// Set book page through the governor if possible.
				try {
					// Inform our onTouchListener to ignore this click, since it was ment
					// to click on this URL link.
					onTouchListener.setJustClickedOnUrlLink();

					panelHolder.setBookPage(url);
				} catch (Exception e) {
					errorMessage(getString(R.string.error_LoadPage));
				}
				return true;
			}
		});

		// Set long-click listener:
		//  If the user long-clicks on any URL link we open it in the other panel.
		webView.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				// Create a message and a method that will serve as its target.
				// The target method will look for URL in the messages data,
				//  and if there is one, it dispatches it to the Governor
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

		// Load the page.
		if (enumState == BookPanelState.metadata) {
			loadData(displayedData, displayedPage);
		} else {
			loadPage(displayedPage);
		}

		//Log.d(LOG, "BookPanel onActivityCreated finished");
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
		Log.d(LOG, LOG + ".loadData");

		// For explanation see the same section in loadPage().
		if (finishedRenderingContent) {
			loadPositionY = null;
			loadScrollSyncOffset = null;
			Log.d(LOG, "nulling load* variables");
		}

		saveBookPageToDb();

		// This needs to be set only *after* calling saveBookPageToDb().
		finishedRenderingContent = false;

		/* Save the data to be loaded and display them if possible. */
		displayedPage = baseUrl;
		displayedData = data;

		if (created) {
			webView.loadDataWithBaseURL(baseUrl, data,
					getActivity().getString(R.string.textOrHTML), null, null);
		}
	}

	/**
	 * Load page through URL path.
	 * @param path to load
	 */
	public void loadPage(String path) {
		Log.d(LOG, LOG + ".loadPage, created: " + created);

		// We have to null these fields here, because onFinishedRenderingContent() might very rarely get
		// called twice, first time with wrong content height, and nulling the variables then would mean
		// that when it gets called for the second time, it wouldn't do anything. Now when it gets called
		// for the second time, it recalculates the values and corrects the previous mistake.
		if (finishedRenderingContent) {
			loadPositionY = null;
			loadScrollSyncOffset = null;
			Log.d(LOG, "nulling load* variables");
		}

		saveBookPageToDb();

		// Save the page to be displayed and display it if possible.
		displayedPage = path;
		if(created) {
			webView.loadUrl(path);
		}

		loadBookPageFromDb(null);

		// This needs to be set only after calling both saveBookPageToDb() and loadBookPageFromDb().
		finishedRenderingContent = false;
	}


	// ============================================================================================
	//		Save and load state to preferences
	// ============================================================================================

	/**
	 * Saves scroll position and ScrollSync offset into the BookPage database.
	 */
	public void saveBookPageToDb() {
		// Only save when there is something to save and if it's a book page.
		if (displayedPage != null && webView != null && finishedRenderingContent
				&& displayedBookPageKey != null) {

			// Compute the values to be saved.
			float scrollY = getPositionYAsFloat();
			float scrollSyncOffset = webView.getScrollSyncOffsetAsFloat();

			// Obtain database access.
			BookPageDB bookPageDB = BookPageDB.getInstance(governor.getActivity());

			if (loadedFromBookPageRowId != null) {
				// Update the existing BookPage entry in the DB with new values.
				String[] values = new String[] {
						"" + scrollY, "" + scrollSyncOffset, "" + System.currentTimeMillis()};
				bookPageDB.updateBookPage(loadedFromBookPageRowId, colsUpdate, values);
			}
			else {
				// Insert a brand new BookPage entry into the DB.
				String[] values = new String[] {
						displayedBookPageKey[0], displayedBookPageKey[1], displayedBookPageKey[2],
						panelHolder.getBook().getRelativePathFromAbsolute(displayedPage),
						"" + scrollY, "" + scrollSyncOffset, "" + System.currentTimeMillis()};

				bookPageDB.insertBookPage(colsInsert, values);
			}

			Log.d(LOG, String.format("%s: Saved to DB (id: %s, ScrollY: %s, ScrollSyncOffset: %s, +time)",
					LOG, loadedFromBookPageRowId, scrollY, scrollSyncOffset));
			//Toast.makeText(ReaderActivity.debugContext, "Saved page to DB", Toast.LENGTH_SHORT).show();

			displayedBookPageKey = null;
			loadedFromBookPageRowId = null;
		}
	}

	/**
	 * Loads scroll position and ScrollSync offset from the BookPage database.
	 * @param latestBookPage BookPage entry to load data from, or NULL so the method contacts database itself.
	 */
	public void loadBookPageFromDb(BookPage latestBookPage) {

		// Only prepare displayedBookPageKey if enumState=books, because when saveBookPageToDb() gets
		// called, the enumState will be already changed to the new one. This way, we can test
		// whether displayedBookPageKey!=null to achieve the same effect.
		if (enumState == BookPanelState.books) {

			// Compute the unique identifier of a book page: (bookFilename, bookTitle, pageFilename).
			displayedBookPageKey = new String[] {
					Func.fileNameFromPath(panelHolder.getBook().getFilePath()),
					panelHolder.getBook().getTitle(),
					Func.fileNameFromPath(displayedPage)};
		}

		// Load page only if this isn't the first time we're opening any page in this run of the app,
		// in which case we're loading the data from preferences, and loading from DB would be redundant.
		if ((latestBookPage != null || finishedRenderingContent) && enumState == BookPanelState.books) {

			BookPage bookPage;
			if (latestBookPage != null) {
				bookPage = latestBookPage;
			} else {
				// Load the BookPage from database if it wasn't provided.
				BookPageDB bookPageDB = BookPageDB.getInstance(governor.getActivity());
				bookPage = bookPageDB.findBookPage(
					displayedBookPageKey[0], displayedBookPageKey[1], displayedBookPageKey[2]);
			}

			// If the BookPage was found in the database, load the data to be used later!
			if (bookPage != null) {
				loadedFromBookPageRowId = bookPage.getId();
				loadPositionY = bookPage.getScrollY();
				loadScrollSyncOffset = bookPage.getScrollSyncOffset();

				Log.d(LOG, String.format("%s: Loaded from DB (id: %s, ScrollY: %s, ScrollSyncOffset: %s)",
						LOG, loadedFromBookPageRowId, loadPositionY, loadScrollSyncOffset));
			}

			/*Toast.makeText(ReaderActivity.debugContext, "Loaded page from DB: "
					+ (bookPage != null ? "found" : "not found"), Toast.LENGTH_SHORT).show(); /**/
			/*Log.v(LOG, String.format("%s: Searching DB for BookPage with these values: (%s, %s, %s)", LOG,
					Func.fileNameFromPath(panelHolder.getBook().getFilePath()),
					panelHolder.getBook().getTitle(),
					Func.fileNameFromPath(displayedPageFilename))); /**/
		}
	}

	/**
	 * Returns the scroll position as a fraction of the ContentHeight.
	 * Used when saving to the preferences or the database. Because next time, the page might be opened
	 * in a different sized WebView, so absolute values would be no good.
	 */
	private float getPositionYAsFloat() {
		return (float) webView.getScrollY() / (float) webView.getContentHeight();
	}

	/**
	 * This method gets called by our SelectionWebView when the WebView has definitively finished
	 * rendering its contents and getContentHeight() returns a new non-zero value.
	 *
	 * Warning: May not get called when two pages shorter than the WebView display area get opened after
	 *  each other. For details see SelectionWebView.onDraw() javadoc.
	 * This is, however, not an issue, because in such short pages, loading the scroll position
	 *  from preferences makes no sense anyway.
	 *
	 * Warning 2!: This method might very rarely get called twice, first time with a little premature
	 *  (shorter OR longer) getContentHeight(), and the second time with the correct getContentHeight().
	 * Thus it is important to do not null the load* fields here, so that in case the method gets called
	 *  the second time, it can recalculate the values and get it right!
	 * Nulling of load* values was therefore moved to loadPage/loadData when the user opens another page.
	 */
	public void onFinishedRenderingContent() {
		finishedRenderingContent = true;

		Log.d(LOG, "BookPanel.onFinishedRenderingContent, loadY: " + loadPositionY
				+ ", contentHeight: " + webView.getContentHeight());

		// Load position from before if this is a page opening from before.
		if (loadPositionY != null) {
			webView.setScrollY(Math.round(loadPositionY * webView.getContentHeight()));
		}

		// Load the scroll sync offset, etc.
		if (loadScrollSyncOffset != null) {
			webView.setScrollSyncOffsetFromFloat(loadScrollSyncOffset);
		}
	}

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
		if (enumState == BookPanelState.metadata) {
			editor.putString("displayedData"+panelPosition, displayedData);
		}

		// Save the position within the page.
		if (webView != null) {
			editor.putInt("positionX"+panelPosition, webView.getScrollX());
			editor.putFloat("positionY"+panelPosition,
					getPositionYAsFloat());
		}

		// Load the scroll sync offset, etc.
		if (webView != null) {
			webView.saveState(editor);
		}
	}

	/**
	 * Load the position within the page from before to be used when webView is instantiated.
	 */
	public void loadScrollPosition(SharedPreferences preferences) {
		loadPositionY = preferences.getFloat("positionY"+panelPosition, 0f);
	}

	/**
	 * Load state and content of the page.
	 */
	@Override
	public void loadState(SharedPreferences preferences) {
		Log.d(LOG, "BookPanel.loadState"); //loadPosX: " + loadPositionX + ", loadPosY: " + loadPositionY

		super.loadState(preferences);
		try {
			enumState = BookPanelState.valueOf(preferences.getString("state"+panelPosition, BookPanelState.books.name()));
		} catch (IllegalArgumentException e) {
			enumState = BookPanelState.books;
		}

		// If this is one of the BookPanelStates that get closed by pressing the back button,
		// inform governor of that fact so it can close it properly.
		if (enumState == BookPanelState.notes || enumState == BookPanelState.metadata) {
			governor.notesDisplayedLastIn = panelHolder;
		}

		String page = preferences.getString("displayedPage"+panelPosition, "");

		if (enumState == BookPanelState.metadata) {
			loadData(preferences.getString("displayedData"+panelPosition, ""), page);
		} else {
			loadPage(page);
		}

		loadScrollPosition(preferences);
	}

	/**
	 * Activity calls this when a runtime change happens in case we need to do something about it.
	 * Examples: Screen orientation changed, entered/exited fullscreen, etc.
	 */
	public void onRuntimeChange() {
		Log.d(LOG, LOG + ".onOrientationChanged");

		// Save the scroll variables so that if content is going to get re-rendered,
		// we load the proper values.
		loadPositionY = getPositionYAsFloat();
		loadScrollSyncOffset = webView.getScrollSyncOffsetAsFloat();
	}

}
